/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common.metric;

/**
 * Enum representing IP Controllers (TAP IP) attributes.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.1.0
 */
public enum IPControllerEnum {
	NAME("name", "Name"),
	MANUFACTURER("manufacturer", "Manufacturer"),
	FIRMWARE_VERSION("firmwareVersion", "FirmwareVersion"),
	SERIAL_NUMBER("serialNumber", "SerialNumber"),
	IP_ADDRESS("ipAddress", "IPAddress"),
	DEVICE_NAME("deviceName","DeviceName"),
	;
	private final String name;
	private final String value;

	/**
	 * Constructor for IPControllerEnum.
	 *
	 * @param name the name of the attribute.
	 * @param value the value of the attribute.
	 */
	IPControllerEnum(String name, String value) {
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
