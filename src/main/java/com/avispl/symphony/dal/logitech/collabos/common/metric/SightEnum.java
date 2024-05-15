/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common.metric;

/**
 * Enum representing Sights (Logitech Sight) attributes.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.1.0
 */
public enum SightEnum {
	ID("id"),
	CAMERA_CONNECTED("cameraConnected"),
	MICROPHONE_CONNECTED("microphoneConnected"),
	FIRMWARE_VERSION("firmwareVersion"),
			;
	private final String name;


	SightEnum(String name) {
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
