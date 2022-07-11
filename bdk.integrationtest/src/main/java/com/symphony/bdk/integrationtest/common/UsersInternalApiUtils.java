package com.symphony.bdk.integrationtest.common;

import com.symphony.bdk.integrationtest.auth.api.BaseClient;
import com.symphony.bdk.integrationtest.auth.model.Envelope;
import com.symphony.bdk.integrationtest.auth.model.Request;
import com.symphony.bdk.integrationtest.exception.ApiException;

import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.client.Entity;

public class UsersInternalApiUtils {
  private static final String SESSION_TOKEN_NAME = "sessionToken";
  private static final String CSRF_TOKEN = "x-symphony-csrf-token";
  private final BaseClient<Object> client;

  public UsersInternalApiUtils(String baseUrl) {
    client = new BaseClient<>(baseUrl);
  }

  public Map<String, Object> createUser(UserCreationRequest payload, String sessionToken,
      String csrfToken) throws ApiException {
    Request<Envelope<UserCreationRequest>> request = new Request<>();
    request.setPath("/webcontroller/admin/v2/users");
    request.setPayload(Entity.json(new Envelope<>(payload)));
    request.setReturnObjectType(Map.class);

    Map<String, String> headers = getSessionAndCsrfTokenHeader(sessionToken, csrfToken);
    request.setHeaders(headers);

    return (Map<String, Object>) client.doPost(request);
  }

  private Map<String, String> getSessionAndCsrfTokenHeader(String sessionToken, String csrfToken) {
    Map<String, String> headers = new HashMap<>();
    headers.put(SESSION_TOKEN_NAME, sessionToken);

    if (csrfToken != null) {
      headers.put(CSRF_TOKEN, csrfToken);
    }

    return headers;
  }
}
