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
package com.avrgaming.civcraft.event;

import java.util.Calendar;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.timers.WarEndCheckTask;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.war.War;

public class WarEvent implements EventInterface {

	@Override
	public void process() {
		CivLog.info("TimerEvent: WarEvent -------------------------------------");

		try {
			War.setWarTime(true);
		} catch (Exception e) {
			CivLog.error("WarStartException:"+e.getMessage());
			e.printStackTrace();
		}
		
		// Start repeating task waiting for war time to end.
		TaskMaster.syncTask(new WarEndCheckTask(), TimeTools.toTicks(1));
	}

	@Override
	public Calendar getNextDate() throws InvalidConfiguration {
		Calendar cal = EventTimer.getCalendarInServerTimeZone();
		
		int dayOfWeek = CivSettings.getInteger(CivSettings.warConfig, "war.time_day");
		int hourOfWar = CivSettings.getInteger(CivSettings.warConfig, "war.time_hour");

		cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
		cal.set(Calendar.HOUR_OF_DAY, hourOfWar);
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		
		Calendar now = Calendar.getInstance();
		if (now.after(cal)) {
			cal.add(Calendar.WEEK_OF_MONTH, 1);
			cal.set(Calendar.DAY_OF_WEEK, dayOfWeek);
			cal.set(Calendar.HOUR_OF_DAY, hourOfWar);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
		}
		
		return cal;
	}

}
