package com.symphony.bdk.integrationtest.auth.api;

import com.symphony.bdk.integrationtest.auth.model.AuthTokens;
import com.symphony.bdk.integrationtest.auth.model.AuthType;
import com.symphony.bdk.integrationtest.auth.model.Password;
import com.symphony.bdk.integrationtest.auth.model.Request;
import com.symphony.bdk.integrationtest.auth.model.SaltResponse;
import com.symphony.bdk.integrationtest.exception.ApiException;

import org.apache.commons.codec.binary.Base64;
import org.bouncycastle.crypto.digests.SHA256Digest;
import org.bouncycastle.crypto.generators.PKCS5S2ParametersGenerator;
import org.bouncycastle.crypto.params.KeyParameter;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;

public class AuthInternalApi {
  private static final String SALT_ENDPOINT = "/%s/salt?userName=%s";
  private static final String USERNAME_PASSWORD_ENDPOINT = "/%s/username_password";
  private static final String ENCODING_CHARSET = "UTF-8";
  private static final String ANTI_CSRF_COOKIE = "anti-csrf-cookie";
  private static final String CSRF_SESSION_TOKEN = "x-symphony-csrf-token";
  private static final String CSRF_KM_TOKEN = "x-km-csrf-token";
  private static final String SKEY_TOKEN_NAME = "skey";
  private static final String KMSESSION_TOKEN_NAME = "kmsession";
  private static final String TOKEN_NOT_FOUND_STR = "TOKEN_NOT_FOUND";

  private final BaseClient<Object> client;

  public AuthInternalApi(String podBaseUrl) {
    client = new BaseClient<>(podBaseUrl);
  }

  /**
   * Method for authenticating a user on the KeyManager and Pod using its username and password.
   * It calls Salt and simulate an UI request.
   */
  public AuthTokens authenticateUsernamePassword(String username, String password)
      throws ApiException, UnsupportedEncodingException {
    Password saltedPasswords = getSaltedPasswords(username, password);

    Map<String, String> sessionTokens =
        authenticateLoginRelaySalt(AuthType.LOGIN, username, saltedPasswords);
    Map<String, String> keyManagerTokens =
        authenticateLoginRelaySalt(AuthType.RELAY, username, saltedPasswords);

    return new AuthTokens(
        sessionTokens.get(SKEY_TOKEN_NAME), keyManagerTokens.get(KMSESSION_TOKEN_NAME),
        sessionTokens.get(CSRF_SESSION_TOKEN), keyManagerTokens.get(CSRF_KM_TOKEN));
  }

  /**
   * Method to authenticate a user using username and password.
   * This uses the type of authentication as a parameter, so you can select between Relay and
   * Login authentication.
   * It also returns the antiCsrfToken received by the authentication endpoint.
   *
   * @param authType        RELAY or LOGIN, to decide which endpoint it will try to authenticate to.
   * @param username        Username of the user which wants to be authenticated.
   * @param saltedPasswords the passwords and salt values gotten/formatted from Salt for usage on
   *                        the authentication
   * @return Map with the wanted token (skey OR kmsession, depending on AuthType) and its
   * respective anti-csrf-token.
   */
  private Map<String, String> authenticateLoginRelaySalt(AuthType authType, String username,
      Password saltedPasswords) {
    MultivaluedMap<String, String> formData = new MultivaluedHashMap<>();
    formData.add("userName", username);
    formData.add("hPassword", AuthType.LOGIN.equals(authType) ? saltedPasswords.gethPassword()
        : saltedPasswords.getKhPassword());

    Request<Form> request = new Request<>();
    request.setReturnObjectType(String.class);
    request.setPayload(Entity.form(formData));
    request.setPath(String.format(USERNAME_PASSWORD_ENDPOINT, authType.getEndpointPath()));
    Response response = client.doPostAndReturnResponse(request);
    Map<String, NewCookie> token = response.getCookies();

    String tokenName = AuthType.LOGIN.equals(authType) ? SKEY_TOKEN_NAME : KMSESSION_TOKEN_NAME;
    String authTokenAsString =
        token.containsKey(tokenName) ? token.get(tokenName).toString() : TOKEN_NOT_FOUND_STR;

    String csrfTokenName = AuthType.LOGIN.equals(authType) ? CSRF_SESSION_TOKEN : CSRF_KM_TOKEN;
    String csrfTokenAsString =
        token.containsKey(ANTI_CSRF_COOKIE) ? token.get(ANTI_CSRF_COOKIE).toString()
            : TOKEN_NOT_FOUND_STR;

    Map<String, String> tokensResponse = new HashMap<>();

    if (!TOKEN_NOT_FOUND_STR.equals(authTokenAsString)) {
      tokensResponse.put(tokenName, authTokenAsString
          .replace(tokenName + "=", "")
          .split(";")[0]);
    }

    if (!TOKEN_NOT_FOUND_STR.equals(csrfTokenAsString)) {
      tokensResponse.put(csrfTokenName, csrfTokenAsString
          .replace(ANTI_CSRF_COOKIE + "=", "")
          .split(";")[0]);
    }

    return tokensResponse;
  }

  /**
   * Method to get the salted password value based on an username.
   * It calls salt then applies the salting into the password and returns the object with it.
   * Request is done for both RELAY and LOGIN salts.
   */
  private Password getSaltedPasswords(String username, String password)
      throws ApiException, UnsupportedEncodingException {

    String sessionSalt = getSalt(username, AuthType.LOGIN);
    String sessionSaltedPassword = generatedSaltedPassword(password, sessionSalt);

    String kmSalt = getSalt(username, AuthType.RELAY);
    String kmSaltedPassword = generatedSaltedPassword(password, kmSalt);

    Password pw = new Password();
    pw.sethSalt(sessionSalt);
    pw.sethPassword(sessionSaltedPassword);

    pw.setKhPassword(kmSaltedPassword);
    pw.setKhSalt(kmSalt);

    return pw;
  }

  private String generatedSaltedPassword(String password, String salt) {
    byte[] saltDecodedBase64 = Base64.decodeBase64(salt);
    PKCS5S2ParametersGenerator gen = new PKCS5S2ParametersGenerator(new SHA256Digest());
    gen.init(password.getBytes(StandardCharsets.UTF_8), saltDecodedBase64, 10000);
    byte[] saltedPasswordAsByte = ((KeyParameter) gen.generateDerivedParameters(256)).getKey();
    return Base64.encodeBase64String(saltedPasswordAsByte);
  }

  private String getSalt(String userName, AuthType authType)
      throws ApiException, UnsupportedEncodingException {
    Request<Void> request = new Request<>();
    request.setReturnObjectType(SaltResponse.class);

    String urlEncodedUsername = URLEncoder.encode(userName, ENCODING_CHARSET);
    request.setPath(String.format(SALT_ENDPOINT, authType.getEndpointPath(), urlEncodedUsername));
    SaltResponse response = (SaltResponse) client.doPost(request);

    return response.getSalt();
  }
}
