/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common;

/**
 * LogitechConstant class provides during the monitoring and controlling process
 *
 * @author Kevin / Symphony Dev Team<br>
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

	/**
	 * Number of consecutive failures a monitoring command is allowed before the adapter reports the failure to Symphony.
	 * With the default polling interval this gives a transient failure, such as a device reboot, roughly 3 minutes to recover.
	 */
	public static final int DEFAULT_API_RETRY_ATTEMPTS = 3;

}