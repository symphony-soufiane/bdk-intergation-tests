package com.symphony.bdk.integrationtest;

import com.symphony.bdk.integrationtest.context.TestContext;

public class InitContextMain {
  public static void main(String[] args) {
    System.out.println("Initializing the context and user/service accounts");
    TestContext.createOrGetInstance();
  }
}
