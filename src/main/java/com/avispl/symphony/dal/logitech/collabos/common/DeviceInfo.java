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
	COLLAB_VERSION("collabOSVersion", "CollabOSVersion"),
	DEVICE_NAME("deviceName", "DeviceName"),
	ETHERNET_MAC("ethernetMAC", "EthernetMAC"),
	HW_VERSION("hwVersion", "HWVersion"),
	MODEL_NAME("modelName", "ModelName"),
	SERIAL_NUMBER("serialNumber", "SerialNumber"),
	SERVICE_PROVIDER("serviceProvider", "ServiceProvider"),
	SYSTEM_NAME("systemName", "SystemName"),
	WIFI_MAC("wifiMAC", "WiFiMAC"),
	DEVICE_CONFIGURATION("deviceConfiguration", "DeviceConfiguration"),
	;
	private final String name;
	private final String displayName;

	/**
	 * Constructor of DeviceInfo
	 *
	 * @param name is device info
	 * @param displayName is the property name displayed in Symphony
	 */
	DeviceInfo(String name, String displayName) {
		this.name = name;
		this.displayName = displayName;
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
	 * Retrieves {@link #displayName}
	 *
	 * @return value of {@link #displayName}
	 */
	public String getDisplayName() {
		return displayName;
	}
}
