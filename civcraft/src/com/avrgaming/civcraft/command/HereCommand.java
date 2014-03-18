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
package com.avrgaming.civcraft.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.main.Colors;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.util.ChunkCoord;

public class HereCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {

		if (sender instanceof Player) {
			Player player = (Player)sender;
			
			ChunkCoord coord = new ChunkCoord(player.getLocation());
			
			CultureChunk cc = CivGlobal.getCultureChunk(coord);
			if (cc != null) {
				CivMessage.send(sender, Colors.LightPurple+"You're currently inside the culture of Civ:"+
						Colors.Yellow+cc.getCiv().getName()+Colors.LightPurple+" for town:"+Colors.Yellow+cc.getTown().getName());
			}
			
			TownChunk tc = CivGlobal.getTownChunk(coord);
			if (tc != null) {
				CivMessage.send(sender, Colors.Green+"You're currently inside the town borders of "+Colors.LightGreen+tc.getTown().getName());
				if (tc.isOutpost()) {
					CivMessage.send(sender, Colors.Yellow+"This chunk is an outpost.");
				}
			}
			
			if (cc == null && tc == null) {
				CivMessage.send(sender, Colors.Yellow+"You stand in wilderness.");
			}
			
		}
		
		
		return false;
	}

}
