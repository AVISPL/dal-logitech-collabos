/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common;

/**
 * LogitechCommand
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 7/5/2024
 * @since 1.0.0
 */
public enum LogitechCommand {
	DEVICE_INFO("api/v1/device"),
	INSIGHTS_ROOM("api/v1/insights/room"),
	INSIGHTS_DEVICE("api/v1/insights/device"),
	PERIPHERALS_INFO("api/v1/peripherals"),
	;
	private final String uri;

	/**
	 * Constructor of DeviceInfo
	 *
	 * @param uri is device info
	 */
	LogitechCommand(String uri) {
		this.uri = uri;
	}

	/**
	 * Retrieves {@link #uri}
	 *
	 * @return value of {@link #uri}
	 */
	public String getUri() {
		return uri;
	}
}
