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

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;

public class PlayerKickBan implements Runnable {

	String name;
	boolean kick;
	boolean ban;
	String reason;
	
	public PlayerKickBan(String name, boolean kick, boolean ban, String reason) {
		this.name = name;
		this.kick = kick;
		this.ban = ban;
		this.reason = reason;
	}
	
	@Override
	public void run() {
		Player player;
		try {
			player = CivGlobal.getPlayer(name);
		} catch (CivException e) {
			return;
		}
		
		if (ban) {
			Resident resident = CivGlobal.getResident(player);
			resident.setBanned(true);
			resident.save();
		}
		
		if (kick) {
			player.kickPlayer(reason);
		}
	}

}
