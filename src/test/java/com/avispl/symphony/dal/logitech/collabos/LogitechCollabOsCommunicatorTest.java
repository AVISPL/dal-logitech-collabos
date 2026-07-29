/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.logitech.collabos;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.getRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.client.WireMock.urlPathMatching;
import static com.github.tomakehurst.wiremock.client.WireMock.verify;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;
import java.util.Map;

import javax.security.auth.login.FailedLoginException;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.client.WireMock;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import com.github.tomakehurst.wiremock.stubbing.Scenario;

/**
 * LogitechCollabOsCommunicatorTest
 *
 * Verifies statistics collection against a WireMock stub of the device's REST API, and the
 * token lifecycle: initial sign-in, 12h caching, and the retry-once-on-401 behaviour in
 * {@link LogitechCollabOsCommunicator#doGet(String, Class)}.
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.0.0
 */
@Tag("Mock")
public class LogitechCollabOsCommunicatorTest {
	private static final String SIGN_IN_URI = "/api/v1/signin";
	private static final String DEVICE_INFO_URI = "/api/v1/device";
	private static final String INSIGHTS_ROOM_URI = "/api/v1/insights/room";
	private static final String INSIGHTS_DEVICE_URI = "/api/v1/insights/device";
	private static final String PERIPHERALS_URI = "/api/v1/peripherals";

	private static final String DEVICE_INFO_BODY = "{\"code\":200,\"result\":{"
			+ "\"collabOSVersion\":\"1.12.246\",\"deviceName\":\"Rally Bar Mini\",\"ethernetMAC\":\"44:73:d6:ee:bd:df\","
			+ "\"hwVersion\":\"4.4\",\"modelName\":\"VR0020\",\"serialNumber\":\"2346FD2KD0T2\",\"serviceProvider\":\"BYOD\","
			+ "\"systemName\":\"RallyBarM-KD0T2\",\"wifiMAC\":\"44:73:d6:ee:bd:de\",\"deviceConfiguration\":\"DEVICE\"}}";
	private static final String INSIGHTS_ROOM_BODY = "{\"code\":200,\"result\":{\"occupancyCount\":\"3\",\"occupancyMode\":\"ALWAYS_ON\"}}";
	private static final String INSIGHTS_DEVICE_BODY = "{\"code\":200,\"result\":{\"deviceState\":\"IDLE\",\"micState\":\"UNMUTED\","
			+ "\"speakerMaxVolume\":\"10\",\"speakerState\":\"UNMUTED\",\"speakerVolume\":\"8\"}}";
	private static final String PERIPHERALS_BODY = "{\"code\":200,\"result\":{\"usbDevices\":[{\"id\":33,\"isAudioDevice\":true,"
			+ "\"isVideoDevice\":false,\"name\":\"Mic Pod\",\"pid\":\"0x0943\",\"vid\":\"0x046d\"}]}}";

	private static WireMockServer wireMockServer;
	private LogitechCollabOsCommunicator communicator;

	@BeforeAll
	static void startServer() {
		wireMockServer = new WireMockServer(WireMockConfiguration.options().dynamicPort());
		wireMockServer.start();
		WireMock.configureFor("localhost", wireMockServer.port());
	}

	@AfterAll
	static void stopServer() {
		wireMockServer.stop();
	}

	@BeforeEach
	public void setUp() throws Exception {
		wireMockServer.resetAll();
		communicator = new LogitechCollabOsCommunicator();
		communicator.setHost("localhost");
		communicator.setPort(wireMockServer.port());
		communicator.setLogin("user");
		communicator.setPassword("pass");
		communicator.init();
	}

	@AfterEach
	public void destroy() throws Exception {
		communicator.disconnect();
	}

	@Test
	void testGetMultipleStatisticsPropertyCount() throws Exception {
		stubHappyPath();
		Map<String, String> statistics = extractProps(communicator.getMultipleStatistics());
		assertEquals(23, statistics.size());
	}

	@Test
	void testDeviceInfo() throws Exception {
		stubHappyPath();
		Map<String, String> statistics = extractProps(communicator.getMultipleStatistics());
		assertEquals("1.12.246", statistics.get("CollabOSVersion"));
		assertEquals("DEVICE", statistics.get("DeviceConfiguration"));
		assertEquals("Rally Bar Mini", statistics.get("DeviceName"));
		assertEquals("44:73:d6:ee:bd:df", statistics.get("EthernetMAC"));
		assertEquals("4.4", statistics.get("HwVersion"));
		assertEquals("VR0020", statistics.get("ModelName"));
		assertEquals("2346FD2KD0T2", statistics.get("SerialNumber"));
		assertEquals("BYOD", statistics.get("ServiceProvider"));
		assertEquals("RallyBarM-KD0T2", statistics.get("SystemName"));
		assertEquals("44:73:d6:ee:bd:de", statistics.get("WifiMAC"));
	}

	@Test
	void testRoomInsight() throws Exception {
		stubHappyPath();
		Map<String, String> statistics = extractProps(communicator.getMultipleStatistics());
		assertEquals("3", statistics.get("RoomInsights#OccupancyCount"));
		assertEquals("ALWAYS_ON", statistics.get("RoomInsights#OccupancyMode"));
	}

	@Test
	void testDeviceInsight() throws Exception {
		stubHappyPath();
		Map<String, String> statistics = extractProps(communicator.getMultipleStatistics());
		assertEquals("IDLE", statistics.get("DeviceInsights#DeviceState"));
		assertEquals("UNMUTED", statistics.get("DeviceInsights#MicState"));
		assertEquals("10", statistics.get("DeviceInsights#SpeakerMaxVolume"));
		assertEquals("UNMUTED", statistics.get("DeviceInsights#SpeakerState"));
		assertEquals("8", statistics.get("DeviceInsights#SpeakerVolume"));
	}

	@Test
	void testPeripheralData() throws Exception {
		stubHappyPath();
		Map<String, String> statistics = extractProps(communicator.getMultipleStatistics());
		assertEquals("33", statistics.get("USBDevice#ID"));
		assertEquals("True", statistics.get("USBDevice#AudioDevice"));
		assertEquals("False", statistics.get("USBDevice#VideoDevice"));
		assertEquals("Mic Pod", statistics.get("USBDevice#Name"));
		assertEquals("0x0943", statistics.get("USBDevice#PID"));
		assertEquals("0x046d", statistics.get("USBDevice#VID"));
	}

	@Test
	void testTokenIsCachedAcrossPollingCycles() throws Exception {
		stubHappyPath();
		communicator.getMultipleStatistics();
		communicator.getMultipleStatistics();
		verify(1, postRequestedFor(urlEqualTo(SIGN_IN_URI)));
	}

	@Test
	void testExpiredTokenTriggersReauthenticationAndRetrySucceeds() throws Exception {
		stubSignIn("token-1");
		stubOk(INSIGHTS_ROOM_URI, INSIGHTS_ROOM_BODY);
		stubOk(INSIGHTS_DEVICE_URI, INSIGHTS_DEVICE_BODY);
		stubOk(PERIPHERALS_URI, PERIPHERALS_BODY);

		stubFor(get(urlEqualTo(DEVICE_INFO_URI))
				.inScenario("device-token-expiry")
				.whenScenarioStateIs(Scenario.STARTED)
				.willReturn(aResponse().withStatus(401))
				.willSetStateTo("reauthenticated"));
		stubFor(get(urlEqualTo(DEVICE_INFO_URI))
				.inScenario("device-token-expiry")
				.whenScenarioStateIs("reauthenticated")
				.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(DEVICE_INFO_BODY)));

		Map<String, String> statistics = extractProps(communicator.getMultipleStatistics());
		assertEquals("1.12.246", statistics.get("CollabOSVersion"));

		verify(2, postRequestedFor(urlEqualTo(SIGN_IN_URI)));
		verify(2, getRequestedFor(urlEqualTo(DEVICE_INFO_URI)));
	}

	@Test
	void testTokenStillInvalidAfterRetryPropagatesAsResourceNotReachable() throws Exception {
		stubSignIn("token-1");
		stubFor(get(urlPathMatching("/api/v1/.*")).willReturn(aResponse().withStatus(401)));

		assertThrows(ResourceNotReachableException.class, () -> communicator.getMultipleStatistics());
	}

	@Test
	void testInvalidCredentialsThrowsFailedLoginException() {
		stubFor(post(urlEqualTo(SIGN_IN_URI)).willReturn(aResponse().withStatus(401)));

		assertThrows(FailedLoginException.class, () -> communicator.getMultipleStatistics());
	}

	private void stubHappyPath() {
		stubSignIn("token-1");
		stubOk(DEVICE_INFO_URI, DEVICE_INFO_BODY);
		stubOk(INSIGHTS_ROOM_URI, INSIGHTS_ROOM_BODY);
		stubOk(INSIGHTS_DEVICE_URI, INSIGHTS_DEVICE_BODY);
		stubOk(PERIPHERALS_URI, PERIPHERALS_BODY);
	}

	private void stubSignIn(String token) {
		stubFor(post(urlEqualTo(SIGN_IN_URI))
				.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json")
						.withBody("{\"code\":200,\"result\":{\"auth_token\":\"" + token + "\"}}")));
	}

	private void stubOk(String uri, String body) {
		stubFor(get(urlEqualTo(uri))
				.willReturn(aResponse().withStatus(200).withHeader("Content-Type", "application/json").withBody(body)));
	}

	private Map<String, String> extractProps(List<Statistics> stats) {
		return ((ExtendedStatistics) stats.get(0)).getStatistics();
	}
}
