package com.avispl.symphony.dal.logitech.collabos;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;

import javax.security.auth.login.FailedLoginException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.error.CommandFailureException;

/**
 * SYAL-3257 — failure handling, run against the CollabOS simulator.
 *
 * {@link LogitechCollabOsCommunicatorTest} talks to a physical device and only covers the happy
 * path. This class covers what the adapter does when the device misbehaves, which is what
 * SYAL-3257 was about: every kind of failure was reported as "Failed all command. Please
 * double-check the requests", and a device that ended the session early kept the adapter broken
 * for hours because it never stopped presenting the token it had cached.
 *
 * The tests drive symphony-logi-collabos-simulator, which serves the CollabOS device API and
 * exposes a control API of its own. Each test puts the simulated device into a broken state
 * through that control API — invalidate or expire the token, fail an endpoint a given number of
 * times, change the credentials — and then asserts on what getMultipleStatistics() makes of it.
 *
 * To run them, start the simulator first:
 *
 * <pre>
 *   cd symphony-simulator/symphony-logi-collabos-simulator
 *   mvn clean package
 *   java -jar target/sim-logi-collabos-1.0.jar
 * </pre>
 *
 * and point them at it with -Dcollabos.simulator.url=http://host:port when it is not on the
 * default address. Without a reachable simulator every test here is skipped, not failed.
 *
 * @author Maksym Rossiitsev / Symphony Dev Team<br>
 * Created on 9/21/2026
 * @since 1.1.1
 */
public class LogitechCollabOsCommunicatorSimTest {

	/**
	 * Where the simulator is listening. Override with -Dcollabos.simulator.url=...
	 */
	private static final String SIMULATOR_URL = System.getProperty("collabos.simulator.url", "http://localhost:8085");

	/**
	 * Credentials the simulator accepts out of the box.
	 */
	private static final String USERNAME = "admin";
	private static final String PASSWORD = "admin";

	/**
	 * Consecutive failures of a single command the adapter tolerates before reporting them.
	 * The scenarios below are written around this value.
	 */
	private static final String API_RETRY_ATTEMPTS = "3";

	private final HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
	private final ObjectMapper objectMapper = new ObjectMapper();

	private LogitechCollabOsCommunicator collabOsCommunicator;

	@BeforeEach
	public void setUp() throws Exception {
		Assumptions.assumeTrue(simulatorIsReachable(),
				"CollabOS simulator is not reachable at " + SIMULATOR_URL + ", skipping");
		control("POST", "/api/simulator/reset", null);

		URI uri = URI.create(SIMULATOR_URL);
		collabOsCommunicator = new LogitechCollabOsCommunicator();
		collabOsCommunicator.setHost(uri.getHost());
		collabOsCommunicator.setPort(uri.getPort() == -1 ? 80 : uri.getPort());
		collabOsCommunicator.setProtocol(uri.getScheme());
		collabOsCommunicator.setLogin(USERNAME);
		collabOsCommunicator.setPassword(PASSWORD);
		collabOsCommunicator.setApiRetryAttempts(API_RETRY_ATTEMPTS);
		collabOsCommunicator.init();
	}

	@AfterEach
	public void destroy() throws Exception {
		if (collabOsCommunicator != null) {
			collabOsCommunicator.destroy();
			collabOsCommunicator = null;
		}
	}

	// ==========================================================================================
	// Baseline
	// ==========================================================================================

	@Test
	@DisplayName("a healthy device produces the full set of statistics")
	void healthyDeviceProducesStatistics() throws Exception {
		Map<String, String> statistics = poll();
		Assertions.assertEquals("1.12.246", statistics.get("CollabOSVersion"));
		Assertions.assertEquals("VR0020", statistics.get("ModelName"));
		Assertions.assertEquals("ALWAYS_ON", statistics.get("RoomInsights#OccupancyMode"));
		Assertions.assertEquals("IDLE", statistics.get("DeviceInsights#DeviceState"));
		Assertions.assertEquals("Mic Pod", statistics.get("USBDevice#Name"));
	}

	// ==========================================================================================
	// Token handling — the SYAL-3257 defect itself
	// ==========================================================================================

	/**
	 * The device ends the session, so the token the adapter cached for 12 hours stops being
	 * accepted. The adapter has to notice the 401, sign in again and finish the cycle, instead of
	 * failing every request until its own expiry elapses.
	 */
	@Test
	@DisplayName("a token the device no longer accepts is replaced within the same cycle")
	void adapterRecoversWhenTheDeviceInvalidatesTheToken() throws Exception {
		poll();
		int signInsBefore = signInCount();

		control("POST", "/api/simulator/token/invalidate", null);

		Map<String, String> statistics = poll();
		Assertions.assertEquals("VR0020", statistics.get("ModelName"),
				"the cycle following the invalidation still has to produce statistics");
		Assertions.assertEquals(signInsBefore + 1, signInCount(),
				"the adapter has to sign in again rather than keep presenting the rejected token");
	}

	/**
	 * The same defect reached through the token lifetime rather than an explicit invalidation:
	 * the device issues a token that lives for a second, the adapter caches it for far longer.
	 */
	@Test
	@DisplayName("an expired token is replaced within the same cycle")
	void adapterRecoversWhenTheTokenExpires() throws Exception {
		control("PUT", "/api/simulator/state", "{\"auth\":{\"tokenTtlSeconds\":1}}");
		poll();
		int signInsBefore = signInCount();

		Thread.sleep(1100L);

		Map<String, String> statistics = poll();
		Assertions.assertEquals("VR0020", statistics.get("ModelName"));
		Assertions.assertEquals(signInsBefore + 1, signInCount());
	}

	/**
	 * Credentials the device rejects are an authentication problem and have to be reported as one
	 * straight away, rather than absorbed as a monitoring failure.
	 */
	@Test
	@DisplayName("credentials the device rejects are reported as a login failure")
	void rejectedSignInIsReportedAsALoginFailure() throws Exception {
		poll();

		control("PUT", "/api/simulator/state", "{\"auth\":{\"password\":\"changed\"}}");
		control("POST", "/api/simulator/token/invalidate", null);

		Assertions.assertThrows(FailedLoginException.class, this::poll,
				"a rejected sign in has to surface as an authentication failure");
	}

	// ==========================================================================================
	// Consecutive failure tolerance
	// ==========================================================================================

	/**
	 * Below {@code apiRetryAttempts} a failing command must not break the cycle: the values it
	 * returned last time are served instead, and the commands that did succeed are unaffected.
	 */
	@Test
	@DisplayName("a command failing fewer times than apiRetryAttempts is absorbed")
	void transientFailureBelowTheThresholdIsAbsorbed() throws Exception {
		poll(); // warm the cache so there is something to fall back on

		configureFault("device", "SERVER_ERROR", 2);

		for (int attempt = 1; attempt <= 2; attempt++) {
			Map<String, String> statistics = poll();
			Assertions.assertEquals("VR0020", statistics.get("ModelName"),
					"attempt " + attempt + " has to serve the previously retrieved device info");
			Assertions.assertEquals("Mic Pod", statistics.get("USBDevice#Name"),
					"attempt " + attempt + " must not lose the commands that did succeed");
		}

		Map<String, String> recovered = poll();
		Assertions.assertEquals("VR0020", recovered.get("ModelName"),
				"the endpoint recovers by itself once the fault is exhausted");
	}

	/**
	 * At {@code apiRetryAttempts} the failure has to reach Symphony, carrying the error the device
	 * actually reported rather than the message the adapter used to invent.
	 */
	@Test
	@DisplayName("a command failing as often as apiRetryAttempts reports the device's own error")
	void failureAtTheThresholdReportsTheDeviceError() throws Exception {
		configureFault("device", "SERVER_ERROR", 3);

		poll();
		poll();

		CommandFailureException thrown = Assertions.assertThrows(CommandFailureException.class, this::poll,
				"the third consecutive failure has to be reported");
		Assertions.assertEquals(500, thrown.getStatusCode());
		// RestCommunicator reports the absolute URL here, the adapter's own CommandFailureException
		// reports the relative command URI, so match on the part they share.
		Assertions.assertTrue(thrown.getRequest().contains("api/v1/device"),
				"the failure has to name the command that failed, was: " + thrown.getRequest());
		Assertions.assertFalse(String.valueOf(thrown.getMessage()).contains("Failed all command"),
				"the fabricated SYAL-3257 message must be gone");
	}

	/**
	 * A device that answers, but reports a failure in the body, used to be silently treated as an
	 * empty payload. It is a failure and has to be counted as one.
	 */
	@Test
	@DisplayName("a non-200 code in the response body counts as a failure")
	void nonSuccessBodyCodeIsTreatedAsAFailure() throws Exception {
		configureFault("peripherals", "ERROR_CODE", 3);

		poll();
		poll();

		CommandFailureException thrown = Assertions.assertThrows(CommandFailureException.class, this::poll);
		Assertions.assertTrue(thrown.getRequest().contains("api/v1/peripherals"),
				"the failure has to name the peripherals command, was: " + thrown.getRequest());
	}

	/**
	 * One failing command must not take the healthy ones down with it.
	 */
	@Test
	@DisplayName("a failing command does not hide the commands that work")
	void oneFailingCommandDoesNotHideTheOthers() throws Exception {
		configureFault("insightsRoom", "NOT_FOUND", 2);

		Map<String, String> statistics = poll();
		Assertions.assertEquals("VR0020", statistics.get("ModelName"));
		Assertions.assertEquals("IDLE", statistics.get("DeviceInsights#DeviceState"));
		Assertions.assertEquals("Mic Pod", statistics.get("USBDevice#Name"));
		Assertions.assertNull(statistics.get("RoomInsights#OccupancyMode"),
				"the failing command has nothing cached yet, so it contributes no properties");
	}

	// ==========================================================================================
	// Simulator control API — not part of the CollabOS device API
	// ==========================================================================================

	/**
	 * Runs one monitoring cycle, the way Symphony does.
	 *
	 * @return the statistics the adapter produced
	 * @throws Exception whatever the adapter reported for the cycle
	 */
	private Map<String, String> poll() throws Exception {
		return ((ExtendedStatistics) collabOsCommunicator.getMultipleStatistics().get(0)).getStatistics();
	}

	/**
	 * Makes an endpoint of the simulated device fail.
	 *
	 * @param endpointKey one of signin, device, insightsRoom, insightsDevice, peripherals
	 * @param mode the failure to produce, one of the simulator's FaultMode values
	 * @param calls number of calls the failure applies to, after which the endpoint recovers
	 */
	private void configureFault(String endpointKey, String mode, int calls) {
		control("POST", "/api/simulator/faults/" + endpointKey,
				String.format("{\"mode\":\"%s\",\"remainingCalls\":%s}", mode, calls));
	}

	/**
	 * @return how many times the simulated device has issued a token
	 * @throws Exception if the simulator state cannot be read
	 */
	private int signInCount() throws Exception {
		JsonNode state = objectMapper.readTree(control("GET", "/api/simulator/state", null));
		return state.get("auth").get("signInCount").asInt();
	}

	/**
	 * Calls the simulator's control API.
	 *
	 * @param method the HTTP method
	 * @param path the control API path
	 * @param body the JSON body, or null when the call takes none
	 * @return the response body
	 */
	private String control(String method, String path, String body) {
		try {
			HttpRequest request = HttpRequest.newBuilder(URI.create(SIMULATOR_URL + path))
					.header("Content-Type", "application/json")
					.timeout(Duration.ofSeconds(10))
					.method(method, body == null
							? HttpRequest.BodyPublishers.noBody()
							: HttpRequest.BodyPublishers.ofString(body))
					.build();
			return http.send(request, HttpResponse.BodyHandlers.ofString()).body();
		} catch (Exception e) {
			throw new IllegalStateException("Simulator control call failed: " + method + " " + path, e);
		}
	}

	/**
	 * Whether the CollabOS simulator is answering at {@link #SIMULATOR_URL}.
	 *
	 * The state payload is inspected rather than just the status code: the other simulators in
	 * symphony-simulator expose a control API at the same path, so answering it is not on its own
	 * proof that the right one is running on that port.
	 *
	 * @return true when the CollabOS simulator is there
	 */
	private boolean simulatorIsReachable() {
		try {
			JsonNode state = objectMapper.readTree(control("GET", "/api/simulator/state", null));
			return state.hasNonNull("deviceInfo")
					&& state.path("deviceInfo").hasNonNull("collabOSVersion")
					&& state.path("endpoints").hasNonNull("insightsRoom");
		} catch (Exception e) {
			return false;
		}
	}
}
