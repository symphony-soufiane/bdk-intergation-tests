package com.symphony.bdk.integrationtest;

import com.symphony.bdk.integrationtest.context.TestContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class InitContextMain {

  private static final Logger LOG = LoggerFactory.getLogger(InitContextMain.class);

  public static void main(String[] args) {
    LOG.info("Initializing the context and user/service accounts");
    TestContext.createOrGetInstance();
  }
}
