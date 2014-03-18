package com.avrgaming.civcraft.util;

import java.util.Date;

import com.avrgaming.civcraft.main.CivGlobal;

public class DateUtil {

	private static final int MILLISECONDS_PER_SECOND = 1000;
	private static final int SECONDS_PER_MIN = 60;
	private static final int MINS_PER_HOUR = 60;
	private static final int HOURS_PER_DAY = 24;
	
	public static boolean isAfterSeconds(Date d, int value) {
		return isAfter(d, value, MILLISECONDS_PER_SECOND);
	}
	
	public static boolean isAfterMins(Date d, int value) {
		return isAfter(d, value, MILLISECONDS_PER_SECOND*SECONDS_PER_MIN);
	}
	
	public static boolean isAfterHours(Date d, int value) {
		return isAfter(d, value, MILLISECONDS_PER_SECOND*SECONDS_PER_MIN*MINS_PER_HOUR);
	}
	
	public static boolean isAfterDays(Date d, int value) {
		return isAfter(d, value, MILLISECONDS_PER_SECOND*
								 SECONDS_PER_MIN*
								 MINS_PER_HOUR*
								 HOURS_PER_DAY);
	}
	
	private static boolean isAfter(Date d, int value, int m) {
		Date now = new Date();
		
		if (CivGlobal.debugDateBypass) {
			return true;
		}
		
		if (d == null) {
			return true;
		}
		
		if (now.getTime() > (d.getTime() + value*m)) {
			return true;
		}
		
		return false;
	}
	
}
