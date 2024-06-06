package com.avispl.symphony.dal.logitech.collabos;

import java.util.Map;

import org.junit.Assert;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.avispl.symphony.api.dal.dto.monitor.ExtendedStatistics;

/**
 * LogitechCollabOsComunicatorTest
 *
 * @author Kevin / Symphony Dev Team<br>
 * Created on 5/9/2024
 * @since 1.0.0
 */
public class LogitechCollabOsCommunicatorTest {
	private LogitechCollabOsCommunicator collabOsCommunicator;
	static ExtendedStatistics extendedStatistic;

	@BeforeEach()
	public void setUp() throws Exception {
		collabOsCommunicator = new LogitechCollabOsCommunicator();

		collabOsCommunicator.setHost("");
		collabOsCommunicator.setPort(443);
		collabOsCommunicator.setLogin("");
		collabOsCommunicator.setPassword("");
		collabOsCommunicator.init();
		collabOsCommunicator.connect();

	}

	@AfterEach()
	public void destroy() throws Exception {
		collabOsCommunicator.disconnect();
	}

	@Test
	void testGetMultipleStatistics() throws Exception {
		collabOsCommunicator.setPingMode("TCP");
		extendedStatistic = (ExtendedStatistics) collabOsCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals(20, statistics.size());
	}

	@Test
	void testDeviceInfo() throws Exception {
		extendedStatistic = (ExtendedStatistics) collabOsCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals("1.12.246", statistics.get("CollabOSVersion"));
		Assert.assertEquals("DEVICE", statistics.get("DeviceConfiguration"));
		Assert.assertEquals("Rally Bar Mini", statistics.get("DeviceName"));
		Assert.assertEquals("44:73:d6:ee:bd:df", statistics.get("EthernetMAC"));
		Assert.assertEquals("4.4", statistics.get("HwVersion"));
		Assert.assertEquals("VR0020", statistics.get("ModelName"));
		Assert.assertEquals("2346FD2KD0T2", statistics.get("SerialNumber"));
		Assert.assertEquals("BYOD", statistics.get("ServiceProvider"));
		Assert.assertEquals("RallyBarM-KD0T2", statistics.get("SystemName"));
		Assert.assertEquals("44:73:d6:ee:bd:de", statistics.get("WifiMAC"));
	}

	@Test
	void testRoomInsight() throws Exception {
		extendedStatistic = (ExtendedStatistics) collabOsCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals("0", statistics.get("RoomInsights#OccupancyCount"));
		Assert.assertEquals("ALWAYS_ON", statistics.get("RoomInsights#OccupancyMode"));
	}

	@Test
	void testPeripheralData() throws Exception {
		extendedStatistic = (ExtendedStatistics) collabOsCommunicator.getMultipleStatistics().get(0);
		Map<String, String> statistics = extendedStatistic.getStatistics();
		Assert.assertEquals("33", statistics.get("USBDevice#ID"));
		Assert.assertEquals("true", statistics.get("USBDevice#AudioDevice"));
		Assert.assertEquals("false", statistics.get("USBDevice#VideoDevice"));
		Assert.assertEquals("Mic Pod", statistics.get("USBDevice#Name"));
		Assert.assertEquals("0x046d", statistics.get("USBDevice#VID"));
	}
}
