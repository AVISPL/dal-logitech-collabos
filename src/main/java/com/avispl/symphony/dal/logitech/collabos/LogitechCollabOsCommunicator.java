/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.logitech.collabos;

import java.lang.reflect.Method;
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
import com.avispl.symphony.dal.logitech.collabos.common.DeviceInfo;
import com.avispl.symphony.dal.logitech.collabos.common.InsightInfo;
import com.avispl.symphony.dal.logitech.collabos.common.LogitechCommand;
import com.avispl.symphony.dal.logitech.collabos.common.LogitechConstant;
import com.avispl.symphony.dal.logitech.collabos.common.PeripheralType;
import com.avispl.symphony.dal.logitech.collabos.common.PingMode;
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
	 * save time get token
	 */
	private Long tokenExpire;

	/**
	 * time the token expires
	 */
	private Long expiresIn = 12 * 3600L * 1000;

	/**
	 * failed monitor
	 */
	private int failedMonitor = 0;

	/**
	 * cached data
	 */
	private Map<String, String> cachedData = new HashMap<>();

	/**
	 * ping mode
	 */
	private PingMode pingMode = PingMode.ICMP;

	/**
	 * Retrieves {@link #pingMode}
	 *
	 * @return value of {@link #pingMode}
	 */
	public String getPingMode() {
		return pingMode.name();
	}

	/**
	 * Sets {@link #pingMode} value
	 *
	 * @param pingMode new value of {@link #pingMode}
	 */
	public void setPingMode(String pingMode) {
		this.pingMode = PingMode.ofString(pingMode);
	}

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
		checkValidApiToken();
		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		Map<String, String> stats = new HashMap<>();
		cachedData.clear();
		failedMonitor = 0;
		retrieveDeviceInfo();
		retrievePeripheralsData();
		retrieveDeviceSightsData();
		retrieveRoomSightsData();
		if (failedMonitor == LogitechCommand.values().length) {
			throw new ResourceNotReachableException("Failed all command. Please double-check the requests");
		}
		populateDeviceInfo(stats);
		populateInsightData(stats);
		populatePeripheralData(stats);
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
		if (this.pingMode == PingMode.ICMP) {
			return super.ping();
		} else if (this.pingMode == PingMode.TCP) {
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
								this.logger.debug(String.format("PING DISCONNECTED: Connection to %s did not succeed within the timeout period of %sms", host, this.getPingTimeout()));
							}
							return this.getPingTimeout();
						}
					} catch (SocketTimeoutException | ConnectException tex) {
						throw new RuntimeException("Socket connection timed out", tex);
					} catch (UnknownHostException ex) {
						throw new UnknownHostException(String.format("Connection timed out, UNKNOWN host %s", host));
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
		} else {
			throw new IllegalArgumentException("Unknown PING Mode: " + pingMode);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalInit() throws Exception {
		if (logger.isDebugEnabled()) {
			logger.debug("Internal init is called.");
		}
		super.internalInit();
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	protected void internalDestroy() {
		if (localExtendedStatistics != null && localExtendedStatistics.getStatistics() != null && localExtendedStatistics.getControllableProperties() != null) {
			localExtendedStatistics.getStatistics().clear();
			localExtendedStatistics.getControllableProperties().clear();
		}
		if (!cachedData.isEmpty()) {
			cachedData.clear();
		}
		super.internalDestroy();
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
	 * Check API token validation
	 * If the token expires, we send a request to get a new token
	 *
	 * @return boolean
	 */
	private boolean checkValidApiToken() throws Exception {
		if (StringUtils.isNullOrEmpty(getLogin()) || StringUtils.isNullOrEmpty(getPassword())) {
			return false;
		}
		if (StringUtils.isNullOrEmpty(token) || System.currentTimeMillis() - tokenExpire >= expiresIn) {
			token = getTokenAPI();
		}
		return StringUtils.isNotNullOrEmpty(token);
	}

	/**
	 * Get token api from the device
	 *
	 * @throws FailedLoginException if login fail
	 */
	private String getTokenAPI() throws FailedLoginException {
		try {
			Map<String, String> payload = new HashMap<>();
			payload.put(LogitechConstant.USERNAME, this.getLogin());
			payload.put(LogitechConstant.PASSWORD, this.getPassword());
			JsonNode response = doPost("api/v1/signin", objectMapper.writeValueAsString(payload), JsonNode.class);
			if (response != null && !response.get(LogitechConstant.CODE).isNull() && 200 == response.get(LogitechConstant.CODE).intValue() && !response.get(LogitechConstant.RESULT).isEmpty()) {
				tokenExpire = System.currentTimeMillis();
				return response.get(LogitechConstant.RESULT).get("auth_token").asText();
			}
			throw new FailedLoginException("Error while get token");
		} catch (Exception e) {
			throw new FailedLoginException("Login fail. Please check the credentials");
		}
	}

	/**
	 * Retrieve monitoring data of the device
	 */
	private void retrieveDeviceInfo() {
		try {
			JsonNode response = doGet(LogitechCommand.DEVICE_INFO.getName(), JsonNode.class);
			if (response != null && !response.get(LogitechConstant.CODE).isNull() && 200 == response.get(LogitechConstant.CODE).intValue() && response.has(LogitechConstant.RESULT)) {
				JsonNode results = response.get(LogitechConstant.RESULT);
				for (DeviceInfo item : DeviceInfo.values()) {
					cachedData.put(capitalizeFirstLetter(item.getName()), checkNullOrEmptyValue(results.get(item.getName())));
				}
			}
		} catch (Exception e) {
			failedMonitor++;
			logger.error("Error while retrieving device info data from device", e);
		}
	}

	/**
	 * Retrieves room sights data from the device.
	 */
	private void retrieveRoomSightsData() {
		try {
			JsonNode response = doGet(LogitechCommand.INSIGHTS_ROOM.getName(), JsonNode.class);
			if (response != null && !response.get(LogitechConstant.CODE).isNull() && 200 == response.get(LogitechConstant.CODE).intValue() && response.has(LogitechConstant.RESULT)) {
				JsonNode results = response.get(LogitechConstant.RESULT);
				if (results.has(LogitechConstant.OCCUPANCY_COUNT)) {
					cachedData.put(capitalizeFirstLetter(LogitechConstant.OCCUPANCY_COUNT), getDefaultValueForNullData(results.get(LogitechConstant.OCCUPANCY_COUNT).asText()));
				}

				if (results.has(LogitechConstant.OCCUPANCY_MODE)) {
					cachedData.put(capitalizeFirstLetter(LogitechConstant.OCCUPANCY_MODE), getDefaultValueForNullData(results.get(LogitechConstant.OCCUPANCY_MODE).asText()));
				}
			}
		} catch (Exception e) {
			failedMonitor++;
			logger.error("Error while retrieving room insights data from device", e);
		}
	}

	/**
	 * Retrieves device sights data from the device.
	 */
	private void retrieveDeviceSightsData() {
		try {
			JsonNode response = doGet(LogitechCommand.INSIGHTS_DEVICE.getName(), JsonNode.class);
			if (response != null && !response.get(LogitechConstant.CODE).isNull() && 200 == response.get(LogitechConstant.CODE).intValue() && response.has(LogitechConstant.RESULT)) {
				JsonNode results = response.get(LogitechConstant.RESULT);
				for (InsightInfo item : InsightInfo.values()) {
					if ("RoomInsights".equalsIgnoreCase(item.getGroup())) {
						continue;
					}
					if (results.has(item.getName())) {
						cachedData.put(capitalizeFirstLetter(item.getName()), getDefaultValueForNullData(results.get(item.getName()).asText()));
					}
				}
			}
		} catch (Exception e) {
			failedMonitor++;
			logger.error("Error while retrieving device insights data from device", e);
		}
	}

	/**
	 * Retrieves room peripheral data from the device.
	 */
	private void retrievePeripheralsData() {
		try {
			JsonNode response = doGet(LogitechCommand.PERIPHERALS_INFO.getName(), JsonNode.class);
			if (response != null && !response.get(LogitechConstant.CODE).isNull() && 200 == response.get(LogitechConstant.CODE).intValue() && response.has(LogitechConstant.RESULT)) {
				JsonNode results = response.get(LogitechConstant.RESULT);
				for (PeripheralType item : PeripheralType.values()) {
					if (results.has(item.getValue())) {
						cachedData.put(item.getName(), results.get(item.getValue()).toString());
					}
				}
			}
		} catch (Exception e) {
			failedMonitor++;
			logger.error("Error while retrieving peripherals data from device", e);
		}
	}

	/**
	 * Populates device info and room insight data into the given stats map.
	 *
	 * @param stats The map to populate with device and room insight data.
	 */
	private void populateDeviceInfo(Map<String, String> stats) {
		for (DeviceInfo item : DeviceInfo.values()) {
			String propertyName = capitalizeFirstLetter(item.getName());
			stats.put(propertyName, getDefaultValueForNullData(cachedData.get(propertyName)));
		}
	}

	/**
	 * Populates insight data into the given stats map.
	 *
	 * @param stats The map to populate with insight data.
	 */
	private void populateInsightData(Map<String, String> stats) {
		for (InsightInfo item : InsightInfo.values()) {
			String group = item.getGroup();
			String name = item.getName();
			if (cachedData.get(capitalizeFirstLetter(name)) != null) {
				stats.put(group + "#" + capitalizeFirstLetter(name), getDefaultValueForNullData(cachedData.get(capitalizeFirstLetter(name))));
			}
		}
	}

	/**
	 * Populates peripheral data into the given stats map.
	 *
	 * @param stats The map to populate with peripheral data.
	 */
	private void populatePeripheralData(Map<String, String> stats) {
		for (PeripheralType type : PeripheralType.values()) {
			String name = type.getName();
			String value = getDefaultValueForNullData(cachedData.get(name));
			Class<? extends Enum<?>> enumClass = type.getEnumClass();
			JsonNode data = convertStringToJson(value);
			if (data != null && data.isArray()) {
				int index = 1;
				for (JsonNode item : data) {
					String group = name + (data.size() == 1 ? "" : String.valueOf(index)) + "#";
					populateStats(stats, group, enumClass, item);
					index++;
				}
			}
		}
	}

	/**
	 * Populates stats map with data extracted from the given JsonNode based on the provided enum class.
	 * Adds the extracted data to the stats map with the specified group prefix.
	 * @param stats The map to populate with data.
	 * @param group The prefix for the keys in the stats map.
	 * @param enumClass The enum class representing the properties to extract from the JsonNode.
	 * @param item The JsonNode containing the data to extract.
	 */
	private void populateStats(Map<String, String> stats, String group, Class<? extends Enum<?>> enumClass, JsonNode item) {
		for (Enum<?> property : enumClass.getEnumConstants()) {
			try {
				Method methodName = property.getClass().getMethod("getName");
				Method methodValue = property.getClass().getMethod("getValue");
				String nameMetric = (String) methodName.invoke(property);
				String valueMetric = (String) methodValue.invoke(property);
				if (item.get(nameMetric) != null) {
					stats.put(group + valueMetric, capitalizeFirstLetter(item.get(nameMetric).asText()));
				}
			} catch (Exception e) {
				logger.error("Error when populate peripheral data", e);
			}
		}
	}

	/**
	 * Converts the given JSON string to a JsonNode.
	 * @param data The JSON string to convert.
	 * @return The JsonNode representing the converted JSON data, or null if conversion fails.
	 */
	private JsonNode convertStringToJson(String data) {
		try {
			return objectMapper.readTree(data);
		} catch (Exception e) {
			return null;
		}
	}

	/**
	 * Check null value
	 *
	 * @param value the value is JsonNode value
	 * @return String / None if value is empty
	 */
	private String checkNullOrEmptyValue(JsonNode value) {
		return value == null || StringUtils.isNullOrEmpty(value.asText()) ? LogitechConstant.NONE : value.asText();
	}

	/**
	 * check value is null or empty
	 *
	 * @param value input value
	 * @return value after checking
	 */
	private String getDefaultValueForNullData(String value) {
		return StringUtils.isNotNullOrEmpty(value) ? value : LogitechConstant.NONE;
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