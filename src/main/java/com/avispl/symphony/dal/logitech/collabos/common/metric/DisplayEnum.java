/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common.metric;

/**
 * Enum representing Connected Displays attributes.
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.1.0
 */
public enum DisplayEnum {
	ID("id", "ID"),
	HDMI_PORT("hdmiPort", "HDMIPort"),
	HEIGHT("height", "Height(px)"),
	WIDTH("width","Width(px)"),
	REFRESH_RATE("refreshRate", "RefreshRate(Hz)"),
			;
	private final String name;
	private final String value;

	/**
	 * Constructor for DisplayEnum.
	 *
	 * @param name the name of the attribute.
	 * @param value the value of the attribute.
	 */
	DisplayEnum(String name, String value) {
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
