package com.symphony.bdk.integrationtest.context.pod;

import com.symphony.bdk.integrationtest.auth.api.AuthApi;
import com.symphony.bdk.integrationtest.auth.api.AuthInternalApi;
import com.symphony.bdk.integrationtest.auth.model.AuthTokens;
import com.symphony.bdk.integrationtest.auth.model.RSAKeyModel;
import com.symphony.bdk.integrationtest.auth.model.RSAKeyType;
import com.symphony.bdk.integrationtest.exception.ApiException;
import com.symphony.security.exceptions.SymphonyEncryptionException;
import com.symphony.security.exceptions.SymphonyInputException;

import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.GeneralSecurityException;

@Getter
public class Pod {
  private static final Logger LOGGER = LoggerFactory.getLogger(Pod.class);

  //TODO: make BDK_INTEGRATION_TESTS_BOT_USERNAME configurable
  private static final String BDK_INTEGRATION_TESTS_BOT_USERNAME = "botfortest";
  private static final String TEMPORARY_AUTHORIZATION_REASON = "BDK-integration-tests";
  private static final String TEMPORARY_AUTHORIZATION_JUSTIFICATION = "Tests";
  private static final String RSA_FILE = "rsa/%s-private.pem";

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
      PodConfigUrlInfo urlConfig) {
    this.podName = name;
    this.ephemeral = isEphemeral;

    //TODO: Add default admin username/password if not provided in adminConfig
    this.adminUsername = adminConfig.getAdminUsername();
    this.adminPassword = adminConfig.getAdminPassword();
    this.apiAdminUsername = BDK_INTEGRATION_TESTS_BOT_USERNAME;

    this.url = urlConfig.getUrl();
    this.podBaseUrl = urlConfig.getPodUrl();
    this.agentBaseUrl = urlConfig.getAgentUrl();
    this.sessionAuthUrl = urlConfig.getSessionAuthUrl();
    this.keyAuthUrl = urlConfig.getKeyAuthUrl();
    this.loginUrl = urlConfig.getLoginUrl();
    this.relayUrl = urlConfig.getRelayUrl();

    try {
      this.authenticateAdmin();
      authenticateApiAdminIfNeeded();
    } catch (GeneralSecurityException | SymphonyInputException | IOException | ApiException |
        SymphonyEncryptionException | __login.api.package_.client.ApiException e) {
      e.printStackTrace();
    }
  }

  private void authenticateAdmin()
      throws UnsupportedEncodingException, SymphonyInputException, ApiException,
      SymphonyEncryptionException {
    AuthInternalApi authInternalApi = new AuthInternalApi(this.url);
    this.adminTokens =
        authInternalApi.authenticateUsernamePassword(this.adminUsername, this.adminPassword);
  }

  private void authenticateApiAdminIfNeeded()
      throws GeneralSecurityException, IOException, __login.api.package_.client.ApiException {
    if (apiAdminTokens == null) {
      RSAKeyModel rsaKeys = new RSAKeyModel();
      rsaKeys.loadKeyFromFile(String.format(RSA_FILE, BDK_INTEGRATION_TESTS_BOT_USERNAME),
          RSAKeyType.PRIVATE);

      AuthApi authApi = new AuthApi(getSessionAuthUrl(), getKeyAuthUrl());
      apiAdminTokens =
          authApi.authenticateApiAdminForIntegrationTests(this, rsaKeys.getFormattedPrivateKey());
    }
  }
}
