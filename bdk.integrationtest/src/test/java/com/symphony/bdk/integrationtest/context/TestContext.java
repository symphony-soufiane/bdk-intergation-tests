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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class TestContext {
  private static final Logger LOG = LoggerFactory.getLogger(TestContext.class);

  //TODO: make BDK_INTEGRATION_TESTS_BOT_USERNAME configurable
  private static final String BDK_INTEGRATION_TESTS_BOT_USERNAME =
      "bdk-integration-tests-service-user";
  private static final String PODS_ENVIRONMENT_FILE = "podsEnvironment";
  private static final String PODS_ENVIRONMENT_FILE_PATH = "/pod_configs/%s.yaml";
  private static final String EPOD_DEPLOYMENT_NAME = "myDeployment name";//TODO: make it configurable

  private static TestContext INSTANCE = null;

  private static Set<Pod> privatePods = new LinkedHashSet<>();
  private static Set<Pod> publicPods = new LinkedHashSet<>();

  private TestContext(Map<String, Map<String, Object>> pods, String epodDeploymentName) {
    List<String> podsNameList = new ArrayList<>(pods.keySet());

    for (String podName : podsNameList) {
      Map<String, Object> configFileObjectMap = pods.get(podName);
      String podConfigFormatString = "";

      if (!StringUtils.isBlank(epodDeploymentName)) {
        podConfigFormatString = epodDeploymentName;
      }

      Pod pod =
          buildPodObjectFromYamlConfigFile(podName, configFileObjectMap, podConfigFormatString);

      try {
        pod.authenticateAdmin();

        // Create service account for integration tests
        if (!pod.isApiAdminServiceAccountExist(BDK_INTEGRATION_TESTS_BOT_USERNAME)) {
          Long apiAdminServiceAccountUserId =
              pod.createApiAdminServiceAccount(BDK_INTEGRATION_TESTS_BOT_USERNAME);
          pod.associateRsaKeyToApiAdminUser(apiAdminServiceAccountUserId,
              BDK_INTEGRATION_TESTS_BOT_USERNAME);
        }

        pod.authenticateApiAdminIfNeeded();

        privatePods.add(pod); //TODO: handle private and public pods, get pod info to do so
      } catch (GeneralSecurityException | SymphonyInputException | IOException | ApiException |
          SymphonyEncryptionException | __login.api.package_.client.ApiException |
          com.symphony.api.pod.client.ApiException e) {
        e.printStackTrace();
      }
    }

    int totalPods = privatePods.size() + publicPods.size();
    LOG.info("STARTING {} PRIVATE {} PUBLIC PODS, FOR A TOTAL OF {} PODS", privatePods.size(),
        publicPods.size(), totalPods);

    if (totalPods == 0) {
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

    return new Pod(podName, ephemeral, adminConfig, urlConfig, BDK_INTEGRATION_TESTS_BOT_USERNAME);
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
    String podsEnvFileName = System.getProperty(PODS_ENVIRONMENT_FILE, "localhost");
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

  public static Pod getPrivatePods() {
    return privatePods.stream()
        .findFirst()
        .orElse(null);
  }
}
