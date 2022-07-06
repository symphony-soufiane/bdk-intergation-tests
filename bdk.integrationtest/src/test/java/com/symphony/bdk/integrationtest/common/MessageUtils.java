package com.symphony.bdk.integrationtest.common;

import com.symphony.api.agent.api.MessagesApi;
import com.symphony.api.agent.client.ApiException;
import com.symphony.api.agent.model.V4Message;
import com.symphony.api.agent.model.V4MessageList;
import com.symphony.bdk.integrationtest.context.TestContext;

public class MessageUtils {

  private MessageUtils() {}

  public static V4Message sendMessage(String message, String streamId) throws ApiException {
    MessagesApi messagesApi = new MessagesApi();
    messagesApi.getApiClient().setBasePath(TestContext.getPrivatePods().getAgentBaseUrl());

    return messagesApi.v4StreamSidMessageCreatePost(streamId,
        TestContext.getPrivatePods().getApiAdminTokens().getSessionToken(),
        TestContext.getPrivatePods().getApiAdminTokens().getKeyManagerToken(),
        message, null, null, null, null);
  }

  public static V4MessageList getMessages(String streamId, Long since) throws ApiException {
    MessagesApi messagesApi = new MessagesApi();
    messagesApi.getApiClient().setBasePath(TestContext.getPrivatePods().getAgentBaseUrl());

    return messagesApi.v4StreamSidMessageGet(streamId, since,
        TestContext.getPrivatePods().getApiAdminTokens().getSessionToken(),
        TestContext.getPrivatePods().getApiAdminTokens().getKeyManagerToken(),
        null, null);
  }
}
