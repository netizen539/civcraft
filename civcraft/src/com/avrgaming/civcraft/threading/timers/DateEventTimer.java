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
package com.avrgaming.civcraft.threading.timers;

import java.util.Date;

import com.avrgaming.civcraft.main.CivGlobal;

public class DateEventTimer implements Runnable {

	/*
	 * This task runs once per min to check various times. If the time for an event is after the
	 * current time then we run the specified task.
	 */
		
	@Override
	public void run() {
	
		Date now = new Date();
		
		/* Check for spawn regen. */
		if (now.after(CivGlobal.getTodaysSpawnRegenDate())) {
			
		}
		
	}

}
