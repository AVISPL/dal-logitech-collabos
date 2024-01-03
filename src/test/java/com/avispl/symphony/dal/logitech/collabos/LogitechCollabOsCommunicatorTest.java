/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.logitech.collabos;

import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.options;

import java.util.Map;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import javax.security.auth.login.FailedLoginException;
import org.junit.Rule;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;

/**
 * LogitechCollabOsCommunicatorTest class
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 1/3/2024
 * @since 1.0.0
 */
@Tag("Mock")
public class LogitechCollabOsCommunicatorTest {
	private ExtendedStatistics extendedStatistic;
	private LogitechCollabOsCommunicator logitechCollabOsCommunicator;
	private static final int HTTP_PORT = 8088;
	private static final int HTTPS_PORT = 8443;
	private static final String HOST_NAME = "127.0.0.1";
	private static final String PROTOCOL = "http";

	@Rule
	WireMockRule wireMockRule = new WireMockRule(options().port(HTTP_PORT).httpsPort(HTTPS_PORT)
			.bindAddress(HOST_NAME));

	@BeforeEach
	void setUp() throws Exception {
		wireMockRule.start();
		logitechCollabOsCommunicator = new LogitechCollabOsCommunicator();
		logitechCollabOsCommunicator.setHost(HOST_NAME);
		logitechCollabOsCommunicator.setProtocol(PROTOCOL);
		logitechCollabOsCommunicator.setPort(HTTP_PORT);
		logitechCollabOsCommunicator.setPassword("admin");
		logitechCollabOsCommunicator.setLogin("admin");
		logitechCollabOsCommunicator.setTrustAllCertificates(false);
		logitechCollabOsCommunicator.init();
		logitechCollabOsCommunicator.connect();
	}

	@AfterEach
	void destroy() throws Exception {
		wireMockRule.stop();
		logitechCollabOsCommunicator.disconnect();
	}

	/**
	 * Test get device information from real device
	 */
	@Test
	void testGetDeviceInfo() throws Exception {
		extendedStatistic = (ExtendedStatistics) logitechCollabOsCommunicator.getMultipleStatistics().get(0);
		Map<String, String> stats = extendedStatistic.getStatistics();
		Assertions.assertEquals(9, stats.size());

		Assertions.assertEquals("RoomMate", stats.get("DeviceName"));
		Assertions.assertEquals("1.11.181", stats.get("CollabOSVersion"));
		Assertions.assertEquals("44:73:d6:ad:e6:0f", stats.get("EthernetMAC"));
		Assertions.assertEquals("5.1", stats.get("HwVersion"));
		Assertions.assertEquals("VR0030", stats.get("ModelName"));
		Assertions.assertEquals("2135FDGM2P92", stats.get("SerialNumber"));
		Assertions.assertEquals("RoomMate-GM2P92", stats.get("SystemName"));
		Assertions.assertEquals("MTR", stats.get("ServiceProvider"));
		Assertions.assertEquals("44:73:d6:ad:e6:0e", stats.get("WifiMAC"));
	}

	/**
	 * Test get token api failed, login failed
	 */
	@Test
	void testLoginError() throws Exception {
		logitechCollabOsCommunicator.destroy();
		wireMockRule.start();
		logitechCollabOsCommunicator = new LogitechCollabOsCommunicator();
		logitechCollabOsCommunicator.setHost(HOST_NAME);
		logitechCollabOsCommunicator.setProtocol(PROTOCOL);
		logitechCollabOsCommunicator.setPort(HTTP_PORT);
		logitechCollabOsCommunicator.setPassword("admin");
		logitechCollabOsCommunicator.setLogin("admin1");
		logitechCollabOsCommunicator.setTrustAllCertificates(false);
		logitechCollabOsCommunicator.init();
		logitechCollabOsCommunicator.connect();
		Assertions.assertThrows(FailedLoginException.class, () -> logitechCollabOsCommunicator.getMultipleStatistics(), "Expect error because login failed");
	}
}