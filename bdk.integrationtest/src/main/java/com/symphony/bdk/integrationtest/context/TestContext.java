package com.symphony.bdk.integrationtest.context;

import com.symphony.bdk.integrationtest.context.pod.Pod;
import com.symphony.bdk.integrationtest.context.pod.PodConfigAdminInfo;
import com.symphony.bdk.integrationtest.context.pod.PodConfigUrlInfo;
import com.symphony.bdk.integrationtest.exception.ApiException;
import com.symphony.security.exceptions.SymphonyEncryptionException;
import com.symphony.security.exceptions.SymphonyInputException;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class TestContext {
  private static final Logger LOG = LoggerFactory.getLogger(TestContext.class);

  private static final String PODS_ENVIRONMENT_FILE_PROPERTY_KEY = "podsEnvironment";
  private static final String INTEGRATION_TESTS_BOT_USERNAME_PROPERTY_KEY =
      "integrationTestsBotUsername";
  private static final String INTEGRATION_TESTS_WORKER_USERNAME_PROPERTY_KEY =
      "integrationTestsWorkerUsername";
  private static final String PODS_ENVIRONMENT_FILE_PATH = "/pod_configs/%s.yaml";
  private static final String EPOD_DEPLOYMENT_NAME = "myDeployment name";//TODO: make it configurable

  private static TestContext INSTANCE = null;

  private static Pod pod;
  private String integrationTestsBotUsername;
  private String integrationTestsWorkerBotUsername;
  private static Long apiAdminServiceAccountUserId;
  private static Long apiWorkerServiceAccountUserId;

  private TestContext(Map<String, Map<String, Object>> pods, String epodDeploymentName) {

    integrationTestsBotUsername =
        System.getProperty(INTEGRATION_TESTS_BOT_USERNAME_PROPERTY_KEY, "");
    integrationTestsWorkerBotUsername =
        System.getProperty(INTEGRATION_TESTS_WORKER_USERNAME_PROPERTY_KEY, "");

    List<String> podsNameList = new ArrayList<>(pods.keySet());

    for (String podName : podsNameList) {
      Map<String, Object> configFileObjectMap = pods.get(podName);
      String podConfigFormatString = "";

      if (!StringUtils.isBlank(epodDeploymentName)) {
        podConfigFormatString = epodDeploymentName;
      }

      pod =
          buildPodObjectFromYamlConfigFile(podName, configFileObjectMap, podConfigFormatString);

      try {
        // Authenticate admin user account for integration tests
        pod.authenticateAdmin();

        // Create service account for integration tests
        apiAdminServiceAccountUserId =
            getApiAdminServiceAccountUserId(UserTypeEnum.BDK_INTEGRATION_TESTS_BOT);
        if (apiAdminServiceAccountUserId == -1L) {
          apiAdminServiceAccountUserId =
              pod.createApiAdminServiceAccount(integrationTestsBotUsername);
          pod.associateRsaKeyToApiAdminUser(apiAdminServiceAccountUserId,
              integrationTestsBotUsername);
        }

        // Create service account for JBot
        apiWorkerServiceAccountUserId =
            getApiAdminServiceAccountUserId(UserTypeEnum.WORKER_BOT);
        if (apiWorkerServiceAccountUserId == -1L) {
          apiWorkerServiceAccountUserId =
              pod.createApiAdminServiceAccount(integrationTestsWorkerBotUsername);
          pod.associateRsaKeyToApiAdminUser(apiWorkerServiceAccountUserId,
              integrationTestsWorkerBotUsername);
        }

        pod.authenticateApiAdminIfNeeded();
      } catch (GeneralSecurityException | SymphonyInputException | IOException | ApiException |
          SymphonyEncryptionException | __login.api.package_.client.ApiException |
          com.symphony.api.pod.client.ApiException e) {
        e.printStackTrace();
      }
    }

    if (TestContext.pod == null) {
      throw new RuntimeException("Cannot run without any pods");
    }
  }

  private Pod buildPodObjectFromYamlConfigFile(String podName, Map<String, Object> stringObjectMap,
      String stringFormatArg) {
    PodConfigAdminInfo adminConfig =
        new PodConfigAdminInfo((String) (stringObjectMap.get("adminUsername")),
            (String) stringObjectMap.get("adminPassword"));

    PodConfigUrlInfo urlConfig = new PodConfigUrlInfo();

    String url = resolvePodComponentUrl(stringObjectMap, "url", stringFormatArg, "");
    urlConfig.setUrl(removeEndingSlash(url));

    urlConfig.setPodUrl(
        resolvePodComponentUrl(stringObjectMap, "podUrl", stringFormatArg, url + "/pod"));
    urlConfig.setAgentUrl(
        resolvePodComponentUrl(stringObjectMap, "agentUrl", stringFormatArg, url + "/agent"));
    urlConfig.setLoginUrl(
        resolvePodComponentUrl(stringObjectMap, "loginUrl", stringFormatArg, url + "/login"));
    urlConfig.setRelayUrl(
        resolvePodComponentUrl(stringObjectMap, "relayUrl", stringFormatArg, url + "/relay"));
    urlConfig.setSessionAuthUrl(
        resolvePodComponentUrl(stringObjectMap, "sessionAuthUrl", stringFormatArg,
            url + "/sessionauth"));
    urlConfig.setKeyAuthUrl(
        resolvePodComponentUrl(stringObjectMap, "keyAuthUrl", stringFormatArg, url + "/keyauth"));

    Boolean ephemeral =
        stringObjectMap.get("ephemeral") != null ? (Boolean) stringObjectMap.get("ephemeral")
            : false;

    return new Pod(podName, ephemeral, adminConfig, urlConfig, integrationTestsBotUsername);
  }

  private String resolvePodComponentUrl(Map<String, Object> configMap, String fieldName,
      String stringFormatArg, String defaultValue) {
    String url = (String) configMap.get(fieldName);

    if (url != null && !StringUtils.isBlank(stringFormatArg)) {
      LOG.info("Using EPODS, so {} URL is: {}", fieldName, String.format(url, stringFormatArg));
      return String.format(url, stringFormatArg);
    }

    return (url != null) ? url : defaultValue;
  }

  private String removeEndingSlash(String url) {
    if (url != null && url.endsWith("/")) {
      return url.substring(0, url.length() - 1);
    }

    return url;
  }

  private static TestContext newInstance() {
    String podsEnvFileName = System.getProperty(PODS_ENVIRONMENT_FILE_PROPERTY_KEY, "localhost");
    String stringOfPodsSelectedForTesting = System.getProperty("usingPods", null);

    Map<String, Object> loadedConfigFile = loadPodsConfigFile(podsEnvFileName);
    LOG.info("initializing test context for environment {}", podsEnvFileName);

    Map<String, Map<String, Object>> podsFromConfigFile =
        (Map<String, Map<String, Object>>) loadedConfigFile.get("pods");

    if (stringOfPodsSelectedForTesting != null) {
      List<String> podsSelectedForTesting =
          Arrays.asList(stringOfPodsSelectedForTesting.split(","));
      // Using LinkedHashMap here to guarantee the insertion order will be followed when getting
      // elements as well.
      Map<String, Map<String, Object>> filteredPodsFromConfigFile = podsSelectedForTesting.stream()
          .peek(element -> LOG.info("Adding pod {} to test execution", element))
          .map(podSelectedForTesting -> new AbstractMap.SimpleEntry<>(podSelectedForTesting,
              podsFromConfigFile.get(podSelectedForTesting)))
          .collect(LinkedHashMap::new,
              (podsUsedForTesting, podSelectedForTesting) -> podsUsedForTesting.put(
                  podSelectedForTesting.getKey(), podSelectedForTesting.getValue()),
              Map::putAll);

      return new TestContext(filteredPodsFromConfigFile, EPOD_DEPLOYMENT_NAME);
    } else {
      LOG.info("FULL POD CONFIG");
      return new TestContext(podsFromConfigFile, EPOD_DEPLOYMENT_NAME);
    }
  }

  private static Map<String, Object> loadPodsConfigFile(String podsEnvFileName) {
    String configFile = String.format(PODS_ENVIRONMENT_FILE_PATH, podsEnvFileName);
    InputStream resourceAsStream = TestContext.class.getResourceAsStream(configFile);

    if (resourceAsStream == null) {
      LOG.error("Could not load configuration file at {}", configFile);
      throw new RuntimeException("Unable to load config file " + configFile);
    }

    Yaml yaml = new Yaml();
    return yaml.load(resourceAsStream);
  }

  public static TestContext createOrGetInstance() {
    if (INSTANCE == null) {
      INSTANCE = newInstance();
    }

    return INSTANCE;
  }

  public static Pod getPod() {
    return pod;
  }

  public  Long getApiAdminServiceAccountUserId(UserTypeEnum userType) {
    Long uid = null;
    switch (userType) {
      case BDK_INTEGRATION_TESTS_BOT:
        uid = apiAdminServiceAccountUserId;
        break;
      case WORKER_BOT:
        uid = apiWorkerServiceAccountUserId;
        break;
    }

    if (uid != null) {
      return uid;
    }

    if (pod == null) {
      return null;
    }

    switch (userType) {
      case BDK_INTEGRATION_TESTS_BOT:
        try {
          return pod.getApiAdminServiceAccountUid(integrationTestsBotUsername);
        } catch (com.symphony.api.pod.client.ApiException e) {
          return null;
        }
      case WORKER_BOT:
        try {
          return pod.getApiAdminServiceAccountUid(integrationTestsWorkerBotUsername);
        } catch (com.symphony.api.pod.client.ApiException e) {
          return null;
        }
    }

    return null;
  }
}
