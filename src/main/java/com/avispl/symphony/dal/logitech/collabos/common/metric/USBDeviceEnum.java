/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common.metric;

/**
 * Enum representing USB Devices  attributes.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.1.0
 */
public enum USBDeviceEnum {
	ID("id", "ID"),
	IS_AUDIO_DEVICE("isAudioDevice", "AudioDevice"),
	IS_VIDEO_DEVICE("isVideoDevice", "VideoDevice"),
	VIDEO_FIRMWARE_VERSION("videoFirmwareVersion", "VideoFirmwareVersion"),
	AUDIO_FIRMWARE_VERSION("audioFirmwareVersion", "AudioFirmwareVersion"),
	NAME("name", "Name"),
	PID("pid", "PID"),
	VID("vid", "VID"),
	;
	private final String name;
	private final String value;

	/**
	 * Constructor for USBDeviceEnum.
	 *
	 * @param name the name of the attribute.
	 * @param value the value of the attribute.
	 */
	USBDeviceEnum(String name, String value) {
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
