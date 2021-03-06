package com.symphony.bdk.integrationtest;

import static org.assertj.core.api.Assertions.assertThat;

import com.symphony.api.agent.client.ApiException;
import com.symphony.api.agent.model.V4Message;
import com.symphony.api.agent.model.V4MessageList;
import com.symphony.api.pod.model.V3RoomDetail;
import com.symphony.bdk.integrationtest.common.MessageUtils;
import com.symphony.bdk.integrationtest.common.StreamUtils;
import com.symphony.bdk.integrationtest.context.TestContext;
import com.symphony.bdk.integrationtest.context.UserTypeEnum;

import net.bytebuddy.utility.RandomString;
import org.assertj.core.api.Fail;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.stream.Collectors;

@SpringBootTest
class SlashCommandActivityIT {

	private static final Logger LOG = LoggerFactory.getLogger(SlashCommandActivityIT.class);

	private static V3RoomDetail testStream;
	private static Long workerBotUserId;
	private static Long integrationTestsBotUserId;


	@BeforeAll
	static void initContext() {
		TestContext testContext = TestContext.createOrGetInstance();
		integrationTestsBotUserId =
				testContext.getApiAdminServiceAccountUserId(UserTypeEnum.BDK_INTEGRATION_TESTS_BOT);
		workerBotUserId = testContext.getApiAdminServiceAccountUserId(UserTypeEnum.WORKER_BOT);
	}

	@DisplayName("Given a bot and a slash command '/ping' with mention required, "
			+ "when I send '/ping' with mentioning the bot, "
			+ "then he replies 'pong' in the same stream")
	@Test
	void testSlashCommandWithMention_botMentioned() {
		try {

			// Create new room
			testStream =
					StreamUtils.createRoom("BDK_INTEGRATION_TESTS_RUN_" + new RandomString().nextString());
			LOG.info("Created stream with id {} and name {}", testStream.getRoomSystemInfo().getId(),
					testStream.getRoomAttributes().getName());

			// Add members to the room
			StreamUtils.addRoomMember(testStream.getRoomSystemInfo().getId(), integrationTestsBotUserId);
			StreamUtils.addRoomMember(testStream.getRoomSystemInfo().getId(), workerBotUserId);

			// Send message
			V4Message v4Message =
					MessageUtils.sendMessage(
							String.format("<messageML><mention uid=\"%s\"/>/ping</messageML>", workerBotUserId),
							testStream.getRoomSystemInfo().getId());

			// Wait for the bot to react
			Thread.sleep(2000);

			// Get latest messages
			V4MessageList messages =
					MessageUtils.getMessages(testStream.getRoomSystemInfo().getId(), v4Message.getTimestamp());

			List<V4Message> filteredMessages = messages.stream()
					.filter(message -> !message.equals(v4Message))
					.collect(Collectors.toList());

			String expectedMessage =
					"<div data-format=\"PresentationML\" data-version=\"2.0\">pong</div>";
			assertThat(filteredMessages)
					.as("Bot reply has not been filtered")
					.hasSize(1);
			assertThat(filteredMessages.get(0).getMessage())
					.as("Bot replied with the expected message")
					.isEqualTo(expectedMessage);
		} catch (ApiException | com.symphony.api.pod.client.ApiException | InterruptedException e) {
			Fail.fail("The scenario has not been completely executed", e);
		}
	}

	@DisplayName("Given a bot and a slash command '/ping' with mention required, "
			+ "when I send '/ping' without mentioning the bot, "
			+ "then he replies 'pong' in the same stream")
	@Test
	void testSlashCommandWithMention_botNotMentioned() {
		try {

			// Send message
			V4Message v4Message = MessageUtils.sendMessage("<messageML>/ping</messageML>",
					testStream.getRoomSystemInfo().getId());

			// Wait for the bot to react
			Thread.sleep(2000);

			// Get latest messages
			V4MessageList messages =
					MessageUtils.getMessages(testStream.getRoomSystemInfo().getId(), v4Message.getTimestamp());

			List<V4Message> filteredMessages = messages.stream()
					.filter(message -> !message.equals(v4Message))
					.collect(Collectors.toList());

			assertThat(filteredMessages)
					.as("Bot did not reply")
					.isEmpty();
		} catch (ApiException | InterruptedException e) {
			Fail.fail("The scenario has not been completely executed", e);
		}
	}

}
