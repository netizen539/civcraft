/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.util;

import java.util.Calendar;
import java.util.Date;

public class TimeTools {

	public static long toTicks(long seconds) {
		return 20*seconds;
	}

	public static long getTicksUnitl(Date next) {
		Calendar c = Calendar.getInstance();
		Date now = c.getTime();
		
		long seconds = Math.abs((now.getTime() - next.getTime())/1000);
		
		return seconds*20;
	}
	
}
