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

import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.template.Template;

public class StartStructureBuild implements Runnable {

	public String playerName;
	public Structure struct;
	public Template tpl;
	public Location centerLoc;
	
	@Override
	public void run() {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e1) {
			e1.printStackTrace();
			return;
		}

		try {
			struct.doBuild(player, centerLoc, tpl);
			struct.save();
		} catch (CivException e) {
			CivMessage.sendError(player, "Unable to build: "+e.getMessage());
		} catch (IOException e) {
			CivMessage.sendError(player, "Internal IO error.");
			e.printStackTrace();
		} catch (SQLException e) {
			CivMessage.sendError(player, "Internal SQL error.");
			e.printStackTrace();
		}
	}

}
