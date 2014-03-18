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
package com.avrgaming.civcraft.interactive;

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;

public class InteractiveBuildCommand implements InteractiveResponse {

	Town town;
	Buildable buildable;
	Location center;
	Template tpl;
	
	public InteractiveBuildCommand(Town town, Buildable buildable, Location center, Template tpl) {
		this.town = town;
		this.buildable = buildable;
		this.center = center.clone();
		this.tpl = tpl;
	}
	
	@Override
	public void respond(String message, Resident resident) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}
		
		if (!message.equalsIgnoreCase("yes")) {
			CivMessage.sendError(player, "Build cancelled.");
			resident.clearInteractiveMode();
			resident.undoPreview();
			return;
		}
		
		
		if (!buildable.validated) {
			CivMessage.sendError(player, "Structure position is not yet validated, please wait.");
			return;
		}
		
		if (!buildable.isValid() && !player.isOp()) {
			CivMessage.sendError(player, "Structure is in an invalid position. The blocks below would not support the structure.");
			return;
		}
		
		class SyncTask implements Runnable {
			Resident resident;
			
			public SyncTask(Resident resident) {
				this.resident = resident;
			}
			
			@Override
			public void run() {
				Player player;
				try {
					player = CivGlobal.getPlayer(resident);
				} catch (CivException e) {
					return;
				}
				
				try {
					if (buildable instanceof Wonder) {
						town.buildWonder(player, buildable.getConfigId(), center, tpl);
					} else {
						town.buildStructure(player, buildable.getConfigId(), center, tpl);
					}
					resident.clearInteractiveMode();
				} catch (CivException e) {
					CivMessage.sendError(player, e.getMessage());
				}
			}
		}
		
		TaskMaster.syncTask(new SyncTask(resident));

	}

}
