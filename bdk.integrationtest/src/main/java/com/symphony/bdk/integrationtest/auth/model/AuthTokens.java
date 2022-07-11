package com.symphony.bdk.integrationtest.auth.model;

import lombok.Getter;

@Getter
public class AuthTokens {
  private final String sessionToken;
  private final String keyManagerToken;
  private String sessionCsrfToken;
  private String kmCsrfToken;

  public AuthTokens(String sessionToken, String keyManagerToken) {
    this.sessionToken = sessionToken;
    this.keyManagerToken = keyManagerToken;
  }

  public AuthTokens(String sessionToken, String keyManagerToken, String sessionCsrfToken,
      String kmCsrfToken) {
    this.sessionToken = sessionToken;
    this.keyManagerToken = keyManagerToken;
    this.sessionCsrfToken = sessionCsrfToken;
    this.kmCsrfToken = kmCsrfToken;
  }

  @Override
  public String toString() {
    return "Auth Tokens: " + '\n' +
        "sessionToken     =>'" + sessionToken + '\'' + '\n' +
        "keyManagerToken  =>'" + keyManagerToken + '\'' + '\n' +
        "sessionCsrfToken =>'" + sessionCsrfToken + '\'' + '\n' +
        "kmCsrfToken      =>'" + kmCsrfToken + '\'';
  }
}
