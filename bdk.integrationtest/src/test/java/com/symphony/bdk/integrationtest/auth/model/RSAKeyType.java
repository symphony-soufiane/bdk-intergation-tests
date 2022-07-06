package com.symphony.bdk.integrationtest.auth.model;

import lombok.Getter;

@Getter
public enum RSAKeyType {
  PRIVATE("private"),
  PUBLIC("public");

  private final String typeOfKey;

  RSAKeyType(String typeOfKey) {
    this.typeOfKey = typeOfKey;
  }
}
