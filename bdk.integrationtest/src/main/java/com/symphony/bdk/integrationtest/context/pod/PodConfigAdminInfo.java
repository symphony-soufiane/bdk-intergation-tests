package com.symphony.bdk.integrationtest.context.pod;

import lombok.Getter;

@Getter
public class PodConfigAdminInfo {
  private final String adminUsername;
  private final String adminPassword;

  public PodConfigAdminInfo(String adminUsername, String adminPassword) {
    this.adminUsername = adminUsername;
    this.adminPassword = adminPassword;
  }
}
