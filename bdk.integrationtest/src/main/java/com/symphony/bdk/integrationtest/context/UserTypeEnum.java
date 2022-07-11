package com.symphony.bdk.integrationtest.context;

import lombok.Getter;

@Getter
public enum UserTypeEnum {
  BDK_INTEGRATION_TESTS_BOT("bdkIntegrationTestsBot"),
  WORKER_BOT("workerBot");

  private final String typeOfUser;

  UserTypeEnum(String typeOfUser) {
    this.typeOfUser = typeOfUser;
  }
}
