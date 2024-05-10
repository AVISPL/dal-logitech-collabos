/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common.metric;

/**
 * IPControllerEnum
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.1.0
 */
public enum IPControllerEnum {
	NAME("name"),
	MANUFACTURER("manufacturer"),
	FIRMWARE_VERSION("firmwareVersion"),
	SERIAL_NUMBER("serialNumber"),
	IP_ADDRESS("ipAddress"),
	DEVICE_NAME("deviceName"),
	;
	private final String name;


	IPControllerEnum(String name) {
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
