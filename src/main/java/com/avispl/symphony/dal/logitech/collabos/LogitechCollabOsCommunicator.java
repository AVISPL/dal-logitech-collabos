/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.logitech.collabos;

import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.security.auth.login.FailedLoginException;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;
import com.avispl.symphony.api.dal.dto.monitor.Statistics;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.util.StringUtils;

/**
 * Logitech CollabOs Communicator Adapter
 *
 * Supported features are:
 * Monitoring for System and Network information
 *
 * Monitoring Capabilities:
 * CollabOS version 1.10
 *
 * CollabOSVersion
 * DeviceConfiguration
 * EthernetMAC
 * HwVersion
 * ModelName
 * SerialNumber
 * SystemName
 * WifiMAC
 * DeviceName
 * ServiceProvider
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 1/3/2024
 * @since 1.0.0
 */
public class LogitechCollabOsCommunicator extends RestCommunicator implements Monitorable {
	/**
	 * Private variable representing the local extended statistics.
	 */
	private ExtendedStatistics localExtendedStatistics;

	/**
	 * A mapper for reading and writing JSON using Jackson library.
	 * ObjectMapper provides functionality for converting between Java objects and JSON.
	 * It can be used to serialize objects to JSON format, and deserialize JSON data to objects.
	 */
	private ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * store token api to get monitoring and controlling data
	 */
	private String token;

	/**
	 * Constructor instance
	 */
	public LogitechCollabOsCommunicator() {
		this.setTrustAllCertificates(true);
	}

	/**
	 * {@inheritDoc}
	 * This method is recalled by Symphony to get the list of statistics to be displayed
	 *
	 * @return List<Statistics> This return the list of statistics.
	 */
	@Override
	public List<Statistics> getMultipleStatistics() throws Exception {
		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		Map<String, String> stats = new HashMap<>();
		getTokenAPI();
		retrieveMonitoringData(stats);
		extendedStatistics.setStatistics(stats);

		localExtendedStatistics = extendedStatistics;

		return Collections.singletonList(localExtendedStatistics);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void authenticate() throws Exception {
		// The device no require authenticate
	}

	/**
	 * {@inheritDoc}
	 * <p>
	 *
	 * Check for available devices before retrieving the value
	 * ping latency information to Symphony
	 */
	@Override
	public int ping() throws Exception {
		if (isInitialized()) {
			long pingResultTotal = 0L;

			for (int i = 0; i < this.getPingAttempts(); i++) {
				long startTime = System.currentTimeMillis();

				try (Socket puSocketConnection = new Socket(this.host, this.getPort())) {
					puSocketConnection.setSoTimeout(this.getPingTimeout());
					if (puSocketConnection.isConnected()) {
						long pingResult = System.currentTimeMillis() - startTime;
						pingResultTotal += pingResult;
						if (this.logger.isTraceEnabled()) {
							this.logger.trace(String.format("PING OK: Attempt #%s to connect to %s on port %s succeeded in %s ms", i + 1, host, this.getPort(), pingResult));
						}
					} else {
						if (this.logger.isDebugEnabled()) {
							logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
						}
						return this.getPingTimeout();
					}
				} catch (SocketTimeoutException | ConnectException tex) {
					throw new SocketTimeoutException("Socket connection timed out");
				} catch (UnknownHostException tex) {
					throw new SocketTimeoutException("Socket connection timed out" + tex.getMessage());
				} catch (Exception e) {
					if (this.logger.isWarnEnabled()) {
						this.logger.warn(String.format("PING TIMEOUT: Connection to %s did not succeed, UNKNOWN ERROR %s: ", host, e.getMessage()));
					}
					return this.getPingTimeout();
				}
			}
			return Math.max(1, Math.toIntExact(pingResultTotal / this.getPingAttempts()));
		} else {
			throw new IllegalStateException("Cannot use device class without calling init() first");
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected HttpHeaders putExtraRequestHeaders(HttpMethod httpMethod, String uri, HttpHeaders headers) throws Exception {
		if (StringUtils.isNotNullOrEmpty(token)) {
			headers.setBearerAuth(token);
		}
		return super.putExtraRequestHeaders(httpMethod, uri, headers);
	}

	/**
	 * Get token api from the device
	 *
	 * @throws FailedLoginException if login fail
	 */
	private void getTokenAPI() throws FailedLoginException {
		try {
			Map<String, String> payload = new HashMap<>();
			payload.put("username", this.getLogin());
			payload.put("password", this.getPassword());
			JsonNode response = doPost("api/v1/signin", objectMapper.writeValueAsString(payload), JsonNode.class);
			if (response != null && !response.get("code").isNull() && 200 == response.get("code").intValue() && !response.get("result").isEmpty()) {
				token = response.get("result").get("auth_token").asText();
				return;
			}
			throw new FailedLoginException("Error while get token");
		} catch (Exception e) {
			throw new FailedLoginException("Login fail. Please check the credentials");
		}
	}

	/**
	 * Retrieve monitoring data of the device
	 *
	 * @param stats the stats are list of Statistics
	 */
	private void retrieveMonitoringData(Map<String, String> stats) {
		try {
			JsonNode response = doGet("/api/v1/device", JsonNode.class);
			if (response != null && !response.get("code").isNull() && 200 == response.get("code").intValue() && !response.get("result").isEmpty()) {
				JsonNode results = response.get("result");
				stats.put(capitalizeFirstLetter(LogitechConstant.DEVICE_NAME), checkNullOrEmptyValue(results.get(LogitechConstant.DEVICE_NAME)));
				stats.put(capitalizeFirstLetter(LogitechConstant.COLLAB_OS_VERSION), checkNullOrEmptyValue(results.get(LogitechConstant.COLLAB_OS_VERSION)));
				stats.put(capitalizeFirstLetter(LogitechConstant.ETHERNET_MAC), checkNullOrEmptyValue(results.get(LogitechConstant.ETHERNET_MAC)));
				stats.put(capitalizeFirstLetter(LogitechConstant.HW_VERSION), checkNullOrEmptyValue(results.get(LogitechConstant.HW_VERSION)));
				stats.put(capitalizeFirstLetter(LogitechConstant.MODEL_NAME), checkNullOrEmptyValue(results.get(LogitechConstant.MODEL_NAME)));
				stats.put(capitalizeFirstLetter(LogitechConstant.SERIAL_NUMBER), checkNullOrEmptyValue(results.get(LogitechConstant.SERIAL_NUMBER)));
				stats.put(capitalizeFirstLetter(LogitechConstant.SYSTEM_NAME), checkNullOrEmptyValue(results.get(LogitechConstant.SYSTEM_NAME)));
				stats.put(capitalizeFirstLetter(LogitechConstant.SERVICE_PROVIDER), checkNullOrEmptyValue(results.get(LogitechConstant.SERVICE_PROVIDER)));
				stats.put(capitalizeFirstLetter(LogitechConstant.ETHERNET_MAC), checkNullOrEmptyValue(results.get(LogitechConstant.ETHERNET_MAC)));
				stats.put(capitalizeFirstLetter(LogitechConstant.WIFI_MAC), checkNullOrEmptyValue(results.get(LogitechConstant.WIFI_MAC)));
				stats.put(capitalizeFirstLetter(LogitechConstant.DEVICE_CONFIGURATION), checkNullOrEmptyValue(results.get(LogitechConstant.DEVICE_CONFIGURATION)));
			}
		} catch (Exception e) {
			throw new ResourceNotReachableException("Error while retrieving monitoring data from device", e);
		}
	}

	/**
	 * Check null value
	 *
	 * @param value the value is JsonNode value
	 * @return String / None if value is empty
	 */
	private String checkNullOrEmptyValue(JsonNode value) {
		return value == null || StringUtils.isNullOrEmpty(value.textValue()) ? LogitechConstant.NONE : value.asText();
	}

	/**
	 * Capitalizes the first letter of a given string.
	 *
	 * @param input The input string to be capitalized.
	 * @return a new string with the first letter capitalized, or the original string if it is null or empty.
	 */
	private static String capitalizeFirstLetter(String input) {
		if (input == null || input.isEmpty()) {
			return input;
		}
		return input.substring(0, 1).toUpperCase() + input.substring(1);
	}
}