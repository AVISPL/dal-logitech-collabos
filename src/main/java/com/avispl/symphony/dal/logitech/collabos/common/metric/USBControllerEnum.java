/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common.metric;

/**
 * Enum representing USB Controllers (TAP USB) attributes.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.1.0
 */
public enum USBControllerEnum {
	NAME("name", "Name"),
	MANUFACTURER("manufacturer", "Manufacturer"),
	FIRMWARE_VERSION("firmwareVersion", "FirmwareVersion"),
	SW_MCU_VERSION("swMcuVersion", "SoftwareMcuVersion"),
	HW_MCU_VERSION("hwMcuVersion", "HardwareMcuVersion"),
	HDMI_VERSION("hdmiVersion", "HDMIVersion"),
	ORIENTATION("orientation", "Orientation"),
	;
	private final String name;
	private final String value;

	/**
	 * Constructor for USBControllerEnum.
	 *
	 * @param name the name of the attribute.
	 * @param value the value of the attribute.
	 */
	USBControllerEnum(String name, String value) {
		this.name = name;
		this.value = value;
	}

	/**
	 * Retrieves {@link #name}
	 *
	 * @return value of {@link #name}
	 */
	public String getName() {
		return name;
	}

	/**
	 * Retrieves {@link #value}
	 *
	 * @return value of {@link #value}
	 */
	public String getValue() {
		return value;
	}
}
