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
	OCCUPANCY_COUNT("occupancyCount", "RoomInsights"),
	OCCUPANCY_MODE("occupancyMode", "RoomInsights"),
	DEVICE_STATUS("deviceState", "DeviceInsights"),
	MIC_STATE("micState", "DeviceInsights"),
	SPEAKER_MAX_VOLUME("speakerMaxVolume", "DeviceInsights"),
	SPEAKER_STATE("speakerState", "DeviceInsights"),
	SPEAKER_VOLUME("speakerVolume", "DeviceInsights"),
	;
	private final String name;
	private final String group;

	/**
	 * Constructor of InsightInfo
	 *
	 * @param name is device info
	 * @param group is group of insight
	 */
	InsightInfo(String name, String group) {
		this.name = name;
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
	 * Retrieves {@link #group}
	 *
	 * @return value of {@link #group}
	 */
	public String getGroup() {
		return group;
	}
}
