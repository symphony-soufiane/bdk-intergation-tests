package com.symphony.bdk.integrationtest;

import com.symphony.api.pod.model.V3RoomDetail;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SecondTestIT {

	private static final Logger LOG = LoggerFactory.getLogger(SecondTestIT.class);

	private static V3RoomDetail testStream;
	private static Long workerBotUserId;
	private static Long integrationTestsBotUserId;


	@Test
	void toPass() {
		assert true;
	}

	@Test
	void toFail() {
		assert false;
	}
}
