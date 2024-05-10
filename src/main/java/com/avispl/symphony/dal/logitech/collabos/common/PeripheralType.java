/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common;

import com.avispl.symphony.dal.logitech.collabos.common.metric.DisplayEnum;
import com.avispl.symphony.dal.logitech.collabos.common.metric.IPControllerEnum;
import com.avispl.symphony.dal.logitech.collabos.common.metric.RemoteEnum;
import com.avispl.symphony.dal.logitech.collabos.common.metric.SightEnum;
import com.avispl.symphony.dal.logitech.collabos.common.metric.USBControllerEnum;
import com.avispl.symphony.dal.logitech.collabos.common.metric.USBDeviceEnum;

/**
 * PeripheralInfo
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/8/2024
 * @since 1.1.0
 */
public enum PeripheralType {
	DISPLAY("Display", "displays", DisplayEnum.class),
	IP_CONTROLLER("IPController", "ipControllers", IPControllerEnum.class),
	REMOTE("Remote", "remotes", RemoteEnum.class),
	SIGHT("Sight", "sights", SightEnum.class),
	USB_CONTROLLER("USBController", "usbControllers", USBControllerEnum.class),
	USB_DEVICES("USBDevice", "usbDevices", USBDeviceEnum.class),
	;
	private final String name;
	private final String value;
	private final Class<? extends Enum<?>> enumClass;


	PeripheralType(String name, String value, Class<? extends Enum<?>> enumClass) {
		this.name = name;
		this.value = value;
		this.enumClass = enumClass;
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

	/**
	 * Retrieves {@link #enumClass}
	 *
	 * @return value of {@link #enumClass}
	 */
	public Class<? extends Enum<?>> getEnumClass() {
		return enumClass;
	}
}
