package com.symphony.bdk.integrationtest.context.pod;

import com.symphony.api.pod.api.UserApi;
import com.symphony.api.pod.api.UsersApi;
import com.symphony.api.pod.client.ApiClient;
import com.symphony.api.pod.model.UserV2;
import com.symphony.api.pod.model.V2UserAttributes;
import com.symphony.api.pod.model.V2UserKeyRequest;
import com.symphony.bdk.integrationtest.auth.api.AuthApi;
import com.symphony.bdk.integrationtest.auth.api.AuthInternalApi;
import com.symphony.bdk.integrationtest.auth.model.AuthTokens;
import com.symphony.bdk.integrationtest.auth.model.RSAKeyModel;
import com.symphony.bdk.integrationtest.auth.model.RSAKeyType;
import com.symphony.bdk.integrationtest.common.Role;
import com.symphony.bdk.integrationtest.common.UserCreationRequest;
import com.symphony.bdk.integrationtest.common.UsersInternalApiUtils;
import com.symphony.bdk.integrationtest.exception.ApiException;
import com.symphony.security.exceptions.SymphonyEncryptionException;
import com.symphony.security.exceptions.SymphonyInputException;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Getter
public class Pod {
  private static final Logger LOGGER = LoggerFactory.getLogger(Pod.class);

  private static final List<Role> PLAT_AUT_BOT_ROLES =
      Arrays.asList(new Role("USER_PROVISIONING"), new Role("CONTENT_MANAGEMENT"));
  private static final String TEMPORARY_AUTHORIZATION_REASON = "BDK-integration-tests";
  private static final String TEMPORARY_AUTHORIZATION_JUSTIFICATION = "Tests";
  private static final String SAVE_RSA_KEY_ACTION = "SAVE";
  private static final String RSA_PRIVATE_KEY_FILE = "rsa/%s-private.pem";
  private static final String RSA_PUBLIC_KEY_FILE = "rsa/%s-public.pem";

  private final String podName;
  private final Boolean ephemeral;
  private final String adminUsername;
  private final String adminPassword;
  private final String apiAdminUsername;
  private final String url;
  private final String podBaseUrl;
  private final String agentBaseUrl;
  private final String sessionAuthUrl;
  private final String keyAuthUrl;
  private final String loginUrl;
  private final String relayUrl;
  private AuthTokens adminTokens;
  private AuthTokens apiAdminTokens;

  public Pod(String name, Boolean isEphemeral, PodConfigAdminInfo adminConfig,
      PodConfigUrlInfo urlConfig, String apiAdminUsername) {
    this.podName = name;
    this.ephemeral = isEphemeral;

    //TODO: Add default admin username/password if not provided in adminConfig
    this.adminUsername = adminConfig.getAdminUsername();
    this.adminPassword = adminConfig.getAdminPassword();
    this.apiAdminUsername = apiAdminUsername;

    this.url = urlConfig.getUrl();
    this.podBaseUrl = urlConfig.getPodUrl();
    this.agentBaseUrl = urlConfig.getAgentUrl();
    this.sessionAuthUrl = urlConfig.getSessionAuthUrl();
    this.keyAuthUrl = urlConfig.getKeyAuthUrl();
    this.loginUrl = urlConfig.getLoginUrl();
    this.relayUrl = urlConfig.getRelayUrl();
  }

  public void authenticateAdmin()
      throws UnsupportedEncodingException, SymphonyInputException, ApiException,
      SymphonyEncryptionException {
    LOGGER.info("Authenticating Admin User account: {}", this.adminUsername);
    AuthInternalApi authInternalApi = new AuthInternalApi(this.url);
    this.adminTokens =
        authInternalApi.authenticateUsernamePassword(this.adminUsername, this.adminPassword);
  }

  public void authenticateApiAdminIfNeeded()
      throws GeneralSecurityException, IOException, __login.api.package_.client.ApiException {
    LOGGER.info("Authenticating Api Admin Service account: {}", this.apiAdminUsername);
    if (apiAdminTokens == null) {
      RSAKeyModel rsaKeys = new RSAKeyModel();
      rsaKeys.loadKeyFromFile(String.format(RSA_PRIVATE_KEY_FILE, this.apiAdminUsername),
          RSAKeyType.PRIVATE);

      AuthApi authApi = new AuthApi(getSessionAuthUrl(), getKeyAuthUrl());
      apiAdminTokens =
          authApi.authenticateApiAdminForIntegrationTests(this, rsaKeys.getFormattedPrivateKey());
    }
  }

  public boolean isApiAdminServiceAccountExist(String serviceAccountName)
      throws com.symphony.api.pod.client.ApiException {
    ApiClient apiClient = new ApiClient().setBasePath(getPodBaseUrl());
    UsersApi usersApi = new UsersApi(apiClient);
    UserV2 adminServiceAccountUser = usersApi.v2UserGet(this.adminTokens
        .getSessionToken(), null, null, serviceAccountName, true);

    if (adminServiceAccountUser != null) {
      LOGGER.info("Api Admin Service account {} already exists", serviceAccountName);
    } else {
      LOGGER.info("Api Admin Service account {} does not exist", serviceAccountName);
    }
    return adminServiceAccountUser != null;
  }

  public Long createApiAdminServiceAccount(String apiAdminUsername) throws ApiException {
    LOGGER.info("Creating Api Admin Service account {}", apiAdminUsername);
    UsersInternalApiUtils usersInternalApiUtils = new UsersInternalApiUtils(this.getUrl());
    UserCreationRequest userCreationRequest =
        this.buildApiAdminUserCreationRequest(apiAdminUsername);

    Map<String, Object> user =
        usersInternalApiUtils.createUser(userCreationRequest, adminTokens.getSessionToken(),
            adminTokens.getSessionCsrfToken());

    Map<String, Object> data =
        (Map<String, Object>) user.getOrDefault("data", Collections.emptyMap());

    return (Long) data.getOrDefault("id", -1);
  }

  public void associateRsaKeyToApiAdminUser(Long userId, String apiAdminUsername)
      throws InvalidKeySpecException, NoSuchAlgorithmException, IOException,
      com.symphony.api.pod.client.ApiException {
    LOGGER.info("Associating RSA Key to Api Admin Service account {}:{}", apiAdminUsername, userId);
    RSAKeyModel rsaKeys = new RSAKeyModel();
    rsaKeys.loadKeyFromFile(
        String.format(RSA_PUBLIC_KEY_FILE, apiAdminUsername),
        RSAKeyType.PUBLIC);

    V2UserKeyRequest userKeyRequest = new V2UserKeyRequest();
    userKeyRequest.setKey(rsaKeys.getFormattedPublicKey());
    V2UserAttributes userAttributes = new V2UserAttributes();
    userAttributes.setCurrentKey(userKeyRequest);
    userAttributes.getCurrentKey().setAction(SAVE_RSA_KEY_ACTION);

    ApiClient baseSwaggerClient = new ApiClient();
    baseSwaggerClient.setBasePath(getPodBaseUrl());
    UserApi userApi = new UserApi(baseSwaggerClient);
    userApi.v2AdminUserUidUpdatePost(getAdminTokens().getSessionToken(), userId, userAttributes);
  }

  private UserCreationRequest buildApiAdminUserCreationRequest(String apiAdminUsername) {
    UserCreationRequest userCreationRequest = new UserCreationRequest();
    userCreationRequest.setUsername(apiAdminUsername);
    userCreationRequest.setDisplayName("Automation_" + apiAdminUsername);
    userCreationRequest.setPerson(false);
    userCreationRequest.setActive(true);
    userCreationRequest.setRoles(PLAT_AUT_BOT_ROLES);
    userCreationRequest.setEntitlement(buildApiAdminEntitlementMap());
    userCreationRequest.setEmailAddress(apiAdminUsername + "@symphony.com");

    return userCreationRequest;
  }

  private Map<String, Boolean> buildApiAdminEntitlementMap() {
    Map<String, Boolean> entitlements = new HashMap<>();

    entitlements.put("isCrossPodEnabled", true);
    entitlements.put("imReadEnabled", true);
    entitlements.put("postReadEnabled", true);
    entitlements.put("canCreatePublicRoom", true);
    entitlements.put("roomReadEnabled", true);
    entitlements.put("delegatesEnabled", true);
    entitlements.put("canShareFilesExternally", true);
    entitlements.put("postWriteEnabled", true);
    entitlements.put("imWriteEnabled", true);
    entitlements.put("roomWriteEnabled", true);
    entitlements.put("sendFilesEnabled", true);
    entitlements.put("canUpdateAvatar", true);

    return entitlements;
  }
}
