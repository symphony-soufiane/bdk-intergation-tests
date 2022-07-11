package com.symphony.bdk.integrationtest.exception;

import lombok.Getter;

@Getter
public class ApiException extends Exception {
  private final int code;

  public ApiException(int code, String message) {
    super(message);
    this.code = code;
  }
}
