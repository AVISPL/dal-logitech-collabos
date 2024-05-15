/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common;

/**
 * DeviceInfo
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/8/2024
 * @since 1.1.0
 */
public enum DeviceInfo {
	COLLAB_VERSION("collabOSVersion"),
	DEVICE_NAME("deviceName"),
	ETHERNET_MAC("ethernetMAC"),
	HW_VERSION("hwVersion"),
	MODEL_NAME("modelName"),
	SERIAL_NUMBER("serialNumber"),
	SERVICE_PROVIDER("serviceProvider"),
	SYSTEM_NAME("systemName"),
	WIFI_MAC("wifiMAC"),
	DEVICE_CONFIGURATION("deviceConfiguration"),
			;
	private final String name;


	DeviceInfo(String name) {
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
