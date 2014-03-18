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

import java.util.ArrayList;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.sessiondb.SessionEntry;

public class ChangeGovernmentTimer implements Runnable {

	@Override
	public void run() {

		// For each town in anarchy, search the session DB for it's timer.
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.getGovernment().id.equalsIgnoreCase("gov_anarchy")) {
				String key = "changegov_"+civ.getId();
				ArrayList<SessionEntry> entries;
				
				entries = CivGlobal.getSessionDB().lookup(key);
				if (entries == null || entries.size() < 1) {
					//We are in anarchy but didn't have a sessiondb entry? huh...
					civ.setGovernment("gov_tribalism");
					return;
					//throw new TownyException("Town "+town.getName()+" in anarchy but cannot find its session DB entry with key:"+key);
				}
				
				SessionEntry se = entries.get(0);
				
				int duration = 3600;
				if (CivGlobal.testFileFlag("debug")) {
					duration = 1;
				}
			
				if (CivGlobal.hasTimeElapsed(se, (Integer)CivSettings.getIntegerGovernment("anarchy_duration")*duration)) {

					civ.setGovernment(se.value);
					CivMessage.global(civ.getName()+" has emerged from anarchy and has adopted "+CivSettings.governments.get(se.value).displayName);
					
					CivGlobal.getSessionDB().delete_all(key);
					civ.save();
				} 
			}
		}		
	}

}
