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

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class TownAddOutlawTask implements Runnable {

	String name;
	Town town;
	
	
	public TownAddOutlawTask(String name, Town town) {
		this.name = name;
		this.town = town;
	}

	@Override
	public void run() {
		
		try {
			Player player = CivGlobal.getPlayer(name);
			CivMessage.send(player, CivColor.Yellow+ChatColor.BOLD+"You are now an outlaw to "+town.getName()+" towers will fire upon you if you visit them!");
		} catch (CivException e) {
		}
		
		town.addOutlaw(name);
		town.save();
		CivMessage.sendTown(town, CivColor.Yellow+name+" is now an outlaw in this town!");
		
	}
	
}
