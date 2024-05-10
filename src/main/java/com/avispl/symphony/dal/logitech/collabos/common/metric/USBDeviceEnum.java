/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common.metric;

/**
 * USBDeviceEnum
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.1.0
 */
public enum USBDeviceEnum {
	ID("id"),
	IS_AUDIO_DEVICE("isAudioDevice"),
	IS_VIDEO_DEVICE("isVideoDevice"),
	VIDEO_FIRMWARE_VERSION("videoFirmwareVersion"),
	AUDIO_FIRMWARE_VERSION("audioFirmwareVersion"),
	NAME("name"),
	PID("pid"),
	VID("vid"),
	;
	private final String name;


	USBDeviceEnum(String name) {
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
