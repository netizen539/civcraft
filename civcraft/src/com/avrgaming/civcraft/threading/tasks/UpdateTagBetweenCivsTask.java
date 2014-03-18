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
package com.avrgaming.civcraft.threading.tasks;

import java.util.HashSet;
import java.util.Set;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncUpdateTagsBetweenCivs;

public class UpdateTagBetweenCivsTask implements Runnable {

	Civilization civ;
	Civilization otherCiv;
	
	public UpdateTagBetweenCivsTask(Civilization civ, Civilization otherCiv) {
		this.civ = civ;
		this.otherCiv = otherCiv;
	}
	
	@Override
	public void run() {
		Set<Player> civList = new HashSet<Player>();
		Set<Player> otherCivList = new HashSet<Player>();
		
		for (Player player : Bukkit.getOnlinePlayers()) {
			Resident resident = CivGlobal.getResident(player);
			if (resident == null || !resident.hasTown()) {
				continue;
			}
			
			if (resident.getTown().getCiv() == civ) {
				civList.add(player);
			} else if (resident.getTown().getCiv() == otherCiv) {
				otherCivList.add(player);
			}
		}
		
		TaskMaster.syncTask(new SyncUpdateTagsBetweenCivs(civList, otherCivList));		
	}

}
