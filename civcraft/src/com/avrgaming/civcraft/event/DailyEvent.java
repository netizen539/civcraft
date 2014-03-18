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

import java.text.SimpleDateFormat;
import java.util.Calendar;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.CultureProcessAsyncTask;
import com.avrgaming.civcraft.threading.timers.DailyTimer;

public class DailyEvent implements EventInterface {
	
	public static Boolean dailyTimerFinished = true;
	
	public static int dayExecuted = 0;
	
	@Override
	public void process() {
		
			CivLog.info("TimerEvent: Daily -------------------------------------");

			while (!CultureProcessAsyncTask.cultureProcessedSinceStartup) {
				CivLog.info("DailyTimer: Waiting for culture to finish processing.");
				try {
					Thread.sleep(10*1000);
				} catch (InterruptedException e) {
					e.printStackTrace();
					break;
				}
			}
			
			// TODO I don't think this timer needs to be synchronous.. we can find a way.
			if (dailyTimerFinished) {
				CivLog.info("Daily timer was finished, starting a new timer.");
				dailyTimerFinished = false;
				if (dayExecuted == 0) {
					Calendar cal = Calendar.getInstance();
					dayExecuted = cal.get(Calendar.DAY_OF_MONTH);
					TaskMaster.syncTask(new DailyTimer(), 0);
				} else {
					try {
						
						throw new CivException("TRIED TO EXECUTE DAILY EVENT TWICE");
					} catch (CivException e) {
						e.printStackTrace();
					}
				}
			} else {
				CivLog.info("Daily timer was NOT finished. skipped.");
			}
		
	
	}

	@Override
	public Calendar getNextDate() throws InvalidConfiguration {
		Calendar cal = EventTimer.getCalendarInServerTimeZone();
		int daily_upkeep_hour;
		daily_upkeep_hour = CivSettings.getInteger(CivSettings.civConfig, "global.daily_upkeep_hour");
		cal.set(Calendar.MINUTE, 0);
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.HOUR_OF_DAY, daily_upkeep_hour);

		Calendar now = Calendar.getInstance();
		if (now.after(cal)) {
			cal.add(Calendar.DATE, 1);
			cal.set(Calendar.HOUR_OF_DAY, daily_upkeep_hour);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
		}
		SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
		CivLog.info("Setting Next Daily Event:"+sdf.format(cal.getTime()));
		
		return cal;		
	}

}
