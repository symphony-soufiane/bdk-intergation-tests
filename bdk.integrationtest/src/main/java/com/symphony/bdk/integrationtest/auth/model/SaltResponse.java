package com.symphony.bdk.integrationtest.auth.model;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
@Setter
public class SaltResponse {

  private String status;
  private String salt;
  private String username;

  public SaltResponse() {
  }

  @JsonProperty("userName")
  public String getUserName() {
    return username;
  }
}
