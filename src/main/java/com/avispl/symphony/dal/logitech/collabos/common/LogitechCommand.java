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
	private final String name;

	/**
	 * Constructor of DeviceInfo
	 *
	 * @param name is device info
	 */
	LogitechCommand(String name) {
		this.name = name;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}
}
