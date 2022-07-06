package com.symphony.bdk.integrationtest.context.pod;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter
@Setter
public class PodConfigUrlInfo {
  private String url;
  private String podUrl;
  private String agentUrl;
  private String loginUrl;
  private String relayUrl;
  private String sessionAuthUrl;
  private String keyAuthUrl;
}
