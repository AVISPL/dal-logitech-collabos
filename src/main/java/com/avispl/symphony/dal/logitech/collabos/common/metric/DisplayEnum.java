/*
 *  Copyright (c) 2024 AVI-SPL, Inc. All Rights Reserved.
 */

package com.avispl.symphony.dal.logitech.collabos.common.metric;

/**
 * DisplayEnum
 *
 * @author Harry / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.1.0
 */
public enum DisplayEnum {
	ID("id"),
	HDMI_PORT("hdmiPort"),
	HEIGHT("height"),
	WIDTH("width"),
	REFRESH_RATE("refreshRate"),
			;
	private final String name;


	DisplayEnum(String name) {
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
