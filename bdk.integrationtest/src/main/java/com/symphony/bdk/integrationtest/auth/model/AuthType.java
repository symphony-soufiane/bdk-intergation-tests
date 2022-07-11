package com.symphony.bdk.integrationtest.auth.model;

public enum AuthType {
  RELAY("relay"),
  LOGIN("login");

  private final String endpointPath;

  AuthType(String endpointPath) {
    this.endpointPath = endpointPath;
  }

  public String getEndpointPath() {
    return endpointPath;
  }

}
