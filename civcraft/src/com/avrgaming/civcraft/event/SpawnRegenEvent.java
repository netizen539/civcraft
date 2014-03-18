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

import org.bukkit.Bukkit;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.threading.TaskMaster;

public class SpawnRegenEvent implements EventInterface {

	@Override
	public void process() {
		CivLog.info("TimerEvent: SpawnRegenEvent -------------------------------------");
		
		class RegenSyncTask implements Runnable {
			
			CultureChunk cc;
			
			public RegenSyncTask(CultureChunk cc) {
				this.cc = cc;
			}
			
			@Override
			public void run() {
				Bukkit.getWorld("world").regenerateChunk(cc.getChunkCoord().getX(), cc.getChunkCoord().getZ());
			}
		}
		
		int tickDelay = 0;
		for (Civilization civ : CivGlobal.getAdminCivs()) {
			if (civ.isAdminCiv()) {
				for (Town town : civ.getTowns()) {				
					for (CultureChunk cc : town.getCultureChunks()) {
						TownChunk tc = CivGlobal.getTownChunk(cc.getChunkCoord());
						if (tc == null) {
							TaskMaster.syncTask(new RegenSyncTask(cc), tickDelay);
							tickDelay += 1;
						}
					}
				}
			}
		}
				
		
	}

	@Override
	public Calendar getNextDate() throws InvalidConfiguration {
		Calendar cal = EventTimer.getCalendarInServerTimeZone();
		int regen_hour = CivSettings.getInteger(CivSettings.civConfig, "global.regen_spawn_hour");
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.add(Calendar.HOUR_OF_DAY, regen_hour);
		
		Calendar now = Calendar.getInstance();
		if (now.after(cal)) {
			cal.add(Calendar.DATE, 1);
			cal.set(Calendar.HOUR_OF_DAY, regen_hour);
			cal.set(Calendar.MINUTE, 0);
			cal.set(Calendar.SECOND, 0);
		}
		
		return cal;
	}

}
