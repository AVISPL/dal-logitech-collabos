/*
 *  Copyright (c) 2024-2026 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common;

/**
 * LogitechConstant class provides during the monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
 * @author Maksym Rossiitsev / Symphony Dev Team<br>
 * Created on 1/3/2024
 * @since 1.0.0
 */
public class LogitechConstant {
	public static final String CODE = "code";
	public static final String RESULT = "result";
	public static final String USERNAME = "username";
	public static final String PASSWORD = "password";
	public static final String OCCUPANCY_COUNT = "occupancyCount";
	public static final String OCCUPANCY_MODE = "occupancyMode";
	public static final String NONE = "None";
	public static final String ADAPTER_METADATA_GROUP = "AdapterMetadata#";
	public static final String ADAPTER_BUILD_DATE = "AdapterBuildDate";
	public static final String ADAPTER_UPTIME = "AdapterUptime";
	public static final String ADAPTER_UPTIME_MIN = "AdapterUptime(min)";
	public static final String ADAPTER_VERSION = "AdapterVersion";
	public static final String VERSION_PROPERTIES_FILE = "/version.properties";
	public static final String ADAPTER_BUILD_DATE_KEY = "adapter.build.date";
	public static final String ADAPTER_VERSION_KEY = "adapter.version";

	/**
	 * Number of consecutive failures a monitoring command is allowed before the adapter reports the failure to Symphony.
	 * With the default polling interval this gives a transient failure, such as a device reboot, roughly 3 minutes to recover.
	 *
	 * @since 1.1.2
	 */
	public static final int DEFAULT_API_RETRY_ATTEMPTS = 3;

}