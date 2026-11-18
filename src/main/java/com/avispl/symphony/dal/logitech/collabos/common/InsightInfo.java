/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common;

/**
 * InsightInfo
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 7/4/2024
 * @since 1.0.0
 */
public enum InsightInfo {
	OCCUPANCY_COUNT("occupancyCount", "OccupancyCount", "RoomInsights"),
	OCCUPANCY_MODE("occupancyMode", "OccupancyMode", "RoomInsights"),
	DEVICE_STATUS("deviceState", "DeviceState", "DeviceInsights"),
	MIC_STATE("micState", "MicrophoneState", "DeviceInsights"),
	SPEAKER_MAX_VOLUME("speakerMaxVolume", "SpeakerMaximumVolume", "DeviceInsights"),
	SPEAKER_STATE("speakerState", "SpeakerState", "DeviceInsights"),
	SPEAKER_VOLUME("speakerVolume", "SpeakerVolume", "DeviceInsights"),
	;
	private final String name;
	private final String displayName;
	private final String group;

	/**
	 * Constructor of InsightInfo
	 *
	 * @param name is device info
	 * @param displayName is the property name displayed in Symphony
	 * @param group is group of insight
	 */
	InsightInfo(String name, String displayName, String group) {
		this.name = name;
		this.displayName = displayName;
		this.group = group;
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

	/**
	 * Retrieves {@link #group}
	 *
	 * @return value of {@link #group}
	 */
	public String getGroup() {
		return group;
	}
}
