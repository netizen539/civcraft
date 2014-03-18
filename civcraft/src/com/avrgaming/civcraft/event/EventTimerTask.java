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

import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivLog;


public class EventTimerTask implements Runnable {

	@Override
	public void run() {
		
		Calendar cal = EventTimer.getCalendarInServerTimeZone();
		
		for (EventTimer timer : EventTimer.timers.values()) {
			
			if (cal.after(timer.getNext())) {
				timer.setLast(cal);

				Calendar next;
				try {
					next = timer.getEventFunction().getNextDate();
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
					continue;
				}
				
				if (next == null) {
					CivLog.warning("WARNING timer:"+timer.getName()+" did not return a next time.");
					continue;
				}

				timer.setNext(next);
				timer.save();

				timer.getEventFunction().process();
			}
			
		}
		
	}
}
