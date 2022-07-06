package com.symphony.bdk.integrationtest.auth.api;

import com.symphony.api.auth.client.ApiClient;
import com.symphony.bdk.integrationtest.auth.jwt.JwtHelper;
import com.symphony.bdk.integrationtest.auth.model.AuthTokens;
import com.symphony.bdk.integrationtest.context.pod.Pod;

import java.security.GeneralSecurityException;

public class AuthApi {

  public AuthApi(String sessionAuthPath, String keyAuthPath) {
    ApiClient sessionAuthClient = new ApiClient();
    sessionAuthClient.setBasePath(sessionAuthPath);

    ApiClient keyAuthClient = new ApiClient();
    keyAuthClient.setBasePath(keyAuthPath);
  }

  public AuthTokens authenticateApiAdminForIntegrationTests(Pod pod, String privateRsaKey)
      throws GeneralSecurityException, __login.api.package_.client.ApiException {
    return authenticateWithRSA(pod.getLoginUrl(), pod.getRelayUrl(), pod.getApiAdminUsername(),
        privateRsaKey);
  }

  public AuthTokens authenticateWithRSA(String loginUrl, String relayUrl, String userName,
      String privateRsaKey)
      throws GeneralSecurityException, __login.api.package_.client.ApiException {
    String jwtToken = JwtHelper.createSignedJwt(userName, privateRsaKey);

    __login.api.package_.model.AuthenticateRequest rsaAuthRequest =
        new __login.api.package_.model.AuthenticateRequest();
    rsaAuthRequest.setToken(jwtToken);

    // Firstly, authenticates in the login API
    __login.api.package_.client.ApiClient loginClient = new __login.api.package_.client.ApiClient();
    loginClient.setBasePath(loginUrl);
    __login.api.package_.api.AuthenticationApi loginApi =
        new __login.api.package_.api.AuthenticationApi(loginClient);
    __login.api.package_.model.Token sessionToken = loginApi.pubkeyAuthenticatePost(rsaAuthRequest);

    // Then, authenticates in the relay API
    loginClient.setBasePath(relayUrl);
    __login.api.package_.model.Token keyManagerToken =
        loginApi.pubkeyAuthenticatePost(rsaAuthRequest);

    return new AuthTokens(sessionToken.getToken(), keyManagerToken.getToken());
  }
}
