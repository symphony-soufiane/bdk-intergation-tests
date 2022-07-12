package com.symphony.bdk.integrationtest.auth.api;


import com.symphony.bdk.integrationtest.auth.model.Request;
import com.symphony.bdk.integrationtest.exception.ApiException;

import com.fasterxml.jackson.databind.DeserializationFeature;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import org.glassfish.jersey.jackson.internal.jackson.jaxrs.json.JacksonJsonProvider;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URI;
import java.util.List;
import java.util.Map;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;

@Getter
public class BaseClient<T> {

  private static final Logger LOGGER = LoggerFactory.getLogger(BaseClient.class);

  private final String baseUrl;
  private final Client podClient;

  /**
   * Jersey-based client made to be used on MultiPart data requests (not supported by swagger
   * client), and also to allow some specific operations that SymphonyClient doesn't have
   * implemented (setting custom headers, etc.)
   *
   * @param baseUrl
   */
  public BaseClient(String baseUrl) {
    this.baseUrl = baseUrl;
    this.podClient = createPodClient();
  }

  /**
   * Creates a Jersey client for calling endpoints
   */
  private Client createPodClient() {
    ClientConfig clientConfig = new ClientConfig();
    clientConfig.register(MultiPartFeature.class);

    // Connect and read timeouts in milliseconds
    clientConfig.property(ClientProperties.READ_TIMEOUT, 30000);
    clientConfig.property(ClientProperties.CONNECT_TIMEOUT, 10000);

    // Ignoring unknown JSON properties
    JacksonJsonProvider jacksonJsonProvider = new JacksonJaxbJsonProvider()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
    clientConfig.register(jacksonJsonProvider);

    return ClientBuilder.newBuilder().withConfig(clientConfig).build();
  }

  public T doPost(Request request) throws ApiException {
    Invocation.Builder builder = buildApiRequest(request);

    Response response = builder.post(request.getPayload());

    T response1 = getResponse(HttpMethod.POST, request, response);
    return response1;
  }

  public Response doPostAndReturnResponse(Request request) {
    Invocation.Builder builder = buildApiRequest(request);
    return builder.post(request.getPayload());
  }

  private Invocation.Builder buildApiRequest(Request request) {
    URI parsedUri = URI.create(request.getPath());
    WebTarget target = podClient.target(this.baseUrl + parsedUri);
    target = buildParamsFromMap(target, request.getParams());

    Invocation.Builder builder = target.request(request.getMediaType());
    buildHeadersFromMap(builder, request.getHeaders());
    buildCookiesFromMap(builder, request.getCookies());

    return builder;
  }

  private T getResponse(String method, Request request, Response response) throws ApiException {

    LOGGER.info("[{}] {} {} {}",
        response.getHeaders().getFirst("X-Trace-Id"), method, request.getPath(), response.getStatus());
    if (response.getStatusInfo().getFamily() == Response.Status.Family.SUCCESSFUL) {
      return (T) response.readEntity(request.getReturnObjectType());
    } else {
      String stringError = response.readEntity(String.class);
      throw new ApiException(response.getStatusInfo().getStatusCode(), "Error: " + stringError);
    }
  }

  private WebTarget buildParamsFromMap(WebTarget target, MultivaluedMap<String, String> params) {
    WebTarget queryTarget = target;
    if (params != null) {
      for (Map.Entry<String, List<String>> entry : params.entrySet()) {
        List<String> paramList = entry.getValue();
        String csvParamList = StringUtils.join(paramList, ',');
        queryTarget = queryTarget.queryParam(entry.getKey(), csvParamList);
      }
    }

    return queryTarget;
  }

  private void buildCookiesFromMap(Invocation.Builder builder, Map<String, String> cookies) {
    if (cookies != null) {
      for (Map.Entry<String, String> entry : cookies.entrySet()) {
        builder.cookie(entry.getKey(), entry.getValue());
      }
    }
  }

  private void buildHeadersFromMap(Invocation.Builder builder, Map<String, String> headers) {
    if (headers != null) {
      for (Map.Entry<String, String> entry : headers.entrySet()) {
        builder.header(entry.getKey(), entry.getValue());
      }
    }
  }
}
