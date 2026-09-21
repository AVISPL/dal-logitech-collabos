/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */
package com.avispl.symphony.dal.logitech.collabos;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.EnumMap;
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
import com.avispl.symphony.api.dal.error.CommandFailureException;
import com.avispl.symphony.api.dal.error.ResourceNotReachableException;
import com.avispl.symphony.api.dal.monitor.Monitorable;
import com.avispl.symphony.dal.communicator.RestCommunicator;
import com.avispl.symphony.dal.logitech.collabos.common.DeviceInfo;
import com.avispl.symphony.dal.logitech.collabos.common.InsightInfo;
import com.avispl.symphony.dal.logitech.collabos.common.LogitechCommand;
import com.avispl.symphony.dal.logitech.collabos.common.LogitechConstant;
import com.avispl.symphony.dal.logitech.collabos.common.PeripheralType;
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
	 * number of consecutive failures of a monitoring command that are tolerated before the failure is reported to Symphony
	 */
	private int apiRetryAttempts = LogitechConstant.DEFAULT_API_RETRY_ATTEMPTS;

	/**
	 * number of consecutive failures of each monitoring command, reset as soon as the command succeeds again
	 */
	private final Map<LogitechCommand, Integer> consecutiveFailures = new EnumMap<>(LogitechCommand.class);

	/**
	 * last successful result payload of each monitoring command
	 *
	 * Served while a command keeps failing below {@link #apiRetryAttempts}, so that a transient failure of a
	 * single endpoint does not remove the properties of all the other ones from the device.
	 */
	private final Map<LogitechCommand, JsonNode> cachedResponses = new EnumMap<>(LogitechCommand.class);

	/**
	 * authentication failure registered during the current monitoring cycle
	 *
	 * Set when the device rejects a new sign in attempt, so that the cycle is reported as an authentication problem
	 * instead of a generic monitoring failure. Reset on every {@link #getMultipleStatistics()} call.
	 */
	private FailedLoginException authenticationFailure;

	/**
	 * cached data
	 */
	private Map<String, String> cachedData = new HashMap<>();

	/**
	 * Retrieves {@link #apiRetryAttempts}
	 *
	 * @return value of {@link #apiRetryAttempts}
	 */
	public String getApiRetryAttempts() {
		return String.valueOf(apiRetryAttempts);
	}

	/**
	 * Sets {@link #apiRetryAttempts} value
	 *
	 * Values that cannot be parsed, or that are not greater than zero, fall back to
	 * {@link LogitechConstant#DEFAULT_API_RETRY_ATTEMPTS}.
	 *
	 * @param apiRetryAttempts new value of {@link #apiRetryAttempts}
	 */
	public void setApiRetryAttempts(String apiRetryAttempts) {
		int value = LogitechConstant.DEFAULT_API_RETRY_ATTEMPTS;
		try {
			value = Integer.parseInt(apiRetryAttempts.trim());
		} catch (Exception e) {
			logger.error(String.format("Invalid apiRetryAttempts value %s, the default value of %s is used instead", apiRetryAttempts, value), e);
		}
		this.apiRetryAttempts = value > 0 ? value : LogitechConstant.DEFAULT_API_RETRY_ATTEMPTS;
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
		authenticationFailure = null;
		checkValidApiToken();
		ExtendedStatistics extendedStatistics = new ExtendedStatistics();
		Map<String, String> stats = new HashMap<>();
		cachedData.clear();
		retrieveDeviceInfo();
		retrievePeripheralsData();
		retrieveDeviceSightsData();
		retrieveRoomSightsData();
		if (authenticationFailure != null) {
			throw authenticationFailure;
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
		token = null;
		tokenExpire = null;
		authenticationFailure = null;
		consecutiveFailures.clear();
		cachedResponses.clear();
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
		if (StringUtils.isNullOrEmpty(token) || tokenExpire == null || System.currentTimeMillis() - tokenExpire >= expiresIn) {
			token = getTokenAPI();
		}
		return StringUtils.isNotNullOrEmpty(token);
	}

	/**
	 * Get token api from the device
	 *
	 * A {@link ResourceNotReachableException} raised by the sign in request is propagated as is, so that a device that
	 * cannot be reached is not reported as a credentials problem. Any other failure keeps its original cause attached.
	 *
	 * @throws FailedLoginException if login fail
	 */
	private String getTokenAPI() throws FailedLoginException {
		JsonNode response;
		try {
			Map<String, String> payload = new HashMap<>();
			payload.put(LogitechConstant.USERNAME, this.getLogin());
			payload.put(LogitechConstant.PASSWORD, this.getPassword());
			response = doPost("api/v1/signin", objectMapper.writeValueAsString(payload), JsonNode.class);
		} catch (ResourceNotReachableException e) {
			throw e;
		} catch (Exception e) {
			throw (FailedLoginException) new FailedLoginException("Login fail. Please check the credentials").initCause(e);
		}
		if (response != null && response.has(LogitechConstant.CODE) && !response.get(LogitechConstant.CODE).isNull() && 200 == response.get(LogitechConstant.CODE).intValue()
				&& response.has(LogitechConstant.RESULT) && !response.get(LogitechConstant.RESULT).isEmpty()) {
			tokenExpire = System.currentTimeMillis();
			return response.get(LogitechConstant.RESULT).get("auth_token").asText();
		}
		throw new FailedLoginException("Login fail. Please check the credentials");
	}

	/**
	 * Performs a GET request and retries it once with a freshly issued API token if the device rejects the current one.
	 *
	 * The token is cached locally for {@link #expiresIn}, but the device may end its session earlier, for instance after a
	 * reboot. Without this retry the adapter keeps sending the stale token until the local expiration elapses, and every
	 * monitoring request fails until the adapter is re-created. See SYAL-3257.
	 *
	 * Only one sign in attempt is made per monitoring cycle: once {@link #authenticationFailure} is set, the remaining
	 * commands of the cycle fail without contacting the sign in endpoint again.
	 *
	 * @param uri the uri to request
	 * @param clazz the expected response type
	 * @return the deserialized response
	 * @throws Exception if the request fails, or if a new token cannot be obtained
	 */
	private <T> T doGetWithRetryOnUnauthorized(String uri, Class<T> clazz) throws Exception {
		try {
			return doGet(uri, clazz);
		} catch (FailedLoginException e) {
			if (authenticationFailure != null || StringUtils.isNullOrEmpty(getLogin()) || StringUtils.isNullOrEmpty(getPassword())) {
				throw e;
			}
			if (logger.isDebugEnabled()) {
				logger.debug(String.format("The device has rejected the cached API token while requesting %s, signing in again and retrying the request.", uri));
			}
			token = null;
			tokenExpire = null;
			try {
				checkValidApiToken();
			} catch (FailedLoginException loginException) {
				authenticationFailure = loginException;
				throw loginException;
			}
			return doGet(uri, clazz);
		}
	}

	/**
	 * Executes a monitoring command and returns its result payload.
	 *
	 * A command that fails, or that answers with anything other than a successful payload, is not reported right away:
	 * the last payload the command returned successfully is served instead, and the failure is only propagated once the
	 * command has failed {@link #apiRetryAttempts} times in a row. This keeps a transient failure, such as the
	 * device rebooting, from raising an alarm while the data stays at most a few polling cycles old. The failure is
	 * propagated unchanged, so that the alarm carries the error the device actually reported.
	 *
	 * @param command the monitoring command to execute
	 * @return the result payload of the command, the last known one if the command is currently failing, or null if the
	 * command has never succeeded
	 * @throws Exception the failure reported by the device, once the command has failed {@link #apiRetryAttempts} times in a row
	 */
	private JsonNode retrieveCommandResult(LogitechCommand command) throws Exception {
		JsonNode response;
		try {
			response = doGetWithRetryOnUnauthorized(command.getUri(), JsonNode.class);
		} catch (Exception e) {
			return registerCommandFailure(command, e);
		}
		if (response != null && response.has(LogitechConstant.CODE) && !response.get(LogitechConstant.CODE).isNull() && 200 == response.get(LogitechConstant.CODE).intValue()
				&& response.has(LogitechConstant.RESULT)) {
			consecutiveFailures.remove(command);
			JsonNode results = response.get(LogitechConstant.RESULT);
			cachedResponses.put(command, results);
			return results;
		}
		return registerCommandFailure(command, new CommandFailureException(host, command.getUri(), String.valueOf(response)));
	}

	/**
	 * Registers a failure of the given command and decides whether it has to be reported to Symphony.
	 *
	 * @param command the command that has failed
	 * @param error the failure reported by the device
	 * @return the last known result payload of the command, or null if the command has never succeeded
	 * @throws Exception the given error, once the command has failed {@link #apiRetryAttempts} times in a row
	 */
	private JsonNode registerCommandFailure(LogitechCommand command, Exception error) throws Exception {
		int failures = consecutiveFailures.merge(command, 1, Integer::sum);
		if (failures >= apiRetryAttempts) {
			logger.error(String.format("Command %s has failed %s times in a row, reporting the failure", command.name(), failures), error);
			cachedResponses.remove(command);
			throw error;
		}
		logger.error(String.format("Error while retrieving %s data from device, attempt %s of %s, the previously retrieved data is used instead", command.name(), failures, apiRetryAttempts),
				error);
		return cachedResponses.get(command);
	}

	/**
	 * Retrieve monitoring data of the device
	 */
	private void retrieveDeviceInfo() throws Exception {
		JsonNode results = retrieveCommandResult(LogitechCommand.DEVICE_INFO);
		if (results == null) {
			return;
		}
		for (DeviceInfo item : DeviceInfo.values()) {
			cachedData.put(capitalizeFirstLetter(item.getName()), checkNullOrEmptyValue(results.get(item.getName())));
		}
	}

	/**
	 * Retrieves room sights data from the device.
	 */
	private void retrieveRoomSightsData() throws Exception {
		JsonNode results = retrieveCommandResult(LogitechCommand.INSIGHTS_ROOM);
		if (results == null) {
			return;
		}
		if (results.has(LogitechConstant.OCCUPANCY_COUNT)) {
			cachedData.put(capitalizeFirstLetter(LogitechConstant.OCCUPANCY_COUNT), getDefaultValueForNullData(results.get(LogitechConstant.OCCUPANCY_COUNT).asText()));
		}

		if (results.has(LogitechConstant.OCCUPANCY_MODE)) {
			cachedData.put(capitalizeFirstLetter(LogitechConstant.OCCUPANCY_MODE), getDefaultValueForNullData(results.get(LogitechConstant.OCCUPANCY_MODE).asText()));
		}
	}

	/**
	 * Retrieves device sights data from the device.
	 */
	private void retrieveDeviceSightsData() throws Exception {
		JsonNode results = retrieveCommandResult(LogitechCommand.INSIGHTS_DEVICE);
		if (results == null) {
			return;
		}
		for (InsightInfo item : InsightInfo.values()) {
			if ("RoomInsights".equalsIgnoreCase(item.getGroup())) {
				continue;
			}
			String propertyName = item.getName();
			if (results.has(propertyName)) {
				cachedData.put(capitalizeFirstLetter(propertyName), getDefaultValueForNullData(results.get(propertyName).asText()));
			}
		}
	}

	/**
	 * Retrieves room peripheral data from the device.
	 */
	private void retrievePeripheralsData() throws Exception {
		JsonNode results = retrieveCommandResult(LogitechCommand.PERIPHERALS_INFO);
		if (results == null) {
			return;
		}
		for (PeripheralType item : PeripheralType.values()) {
			if (results.has(item.getValue())) {
				cachedData.put(item.getName(), results.get(item.getValue()).toString());
			}
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