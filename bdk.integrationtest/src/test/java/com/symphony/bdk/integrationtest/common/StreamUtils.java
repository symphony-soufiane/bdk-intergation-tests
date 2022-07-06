package com.symphony.bdk.integrationtest.common;

import com.symphony.api.pod.api.RoomMembershipApi;
import com.symphony.api.pod.api.StreamsApi;
import com.symphony.api.pod.model.UserId;
import com.symphony.api.pod.model.V3RoomAttributes;
import com.symphony.api.pod.model.V3RoomDetail;
import com.symphony.bdk.integrationtest.context.TestContext;

public class StreamUtils {

  private StreamUtils() {}

  public static V3RoomDetail createRoom(String name)
      throws com.symphony.api.pod.client.ApiException {
    StreamsApi streamsApi = new StreamsApi();
    streamsApi.getApiClient().setBasePath(TestContext.getPrivatePods().getPodBaseUrl());

    V3RoomAttributes roomAttributes =
        new V3RoomAttributes().name(name).description("Integration test room")._public(true);
    return streamsApi.v3RoomCreatePost(roomAttributes,
        TestContext.getPrivatePods().getApiAdminTokens().getSessionToken());
  }

  public static void addRoomMember(String roomId, Long userId)
      throws com.symphony.api.pod.client.ApiException {
    RoomMembershipApi roomMembershipApi = new RoomMembershipApi();
    roomMembershipApi.getApiClient().setBasePath(TestContext.getPrivatePods().getPodBaseUrl());

    roomMembershipApi.v1RoomIdMembershipAddPost(roomId, new UserId().id(userId),
        TestContext.getPrivatePods().getApiAdminTokens().getSessionToken());
  }
}
