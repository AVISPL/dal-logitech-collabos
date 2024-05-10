/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common.metric;

/**
 * USBControllerEnum
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.1.0
 */
public enum USBControllerEnum {
	NAME("name"),
	MANUFACTURER("manufacturer"),
	FIRMWARE_VERSION("firmwareVersion"),
	SW_MCU_VERSION("swMcuVersion"),
	HW_MCU_VERSION("hwMcuVersion"),
	HDMI_VERSION("hdmiVersion"),
	ORIENTATION("orientation"),
	;
	private final String name;


	USBControllerEnum(String name) {
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
