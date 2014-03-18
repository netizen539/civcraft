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

import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.war.War;

public class WarEndCheckTask implements Runnable {

	@Override
	public void run() {

		Date now = new Date();
		if (War.isWarTime()) {
			if (War.getEnd() == null || now.after(War.getEnd())) {
				War.setWarTime(false);
			} else {
				TaskMaster.syncTask(this, TimeTools.toTicks(1));
			}
		}		
	}

}
