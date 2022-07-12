package com.symphony.bdk.integrationtest.auth.model;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.Map;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

@Getter
@Setter
@ToString
public class Request<T> {
  private String path;
  private Entity<T> payload;
  private MediaType mediaType;
  private Class returnObjectType;
  private Map<String, String> headers;
  private MultivaluedMap<String, String> params;
  private Map<String, String> cookies;
}
