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
package com.avrgaming.civcraft.command.town;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.TownAddOutlawTask;
import com.avrgaming.civcraft.util.CivColor;

public class TownOutlawCommand extends CommandBase {

	@Override
	public void init() {
		command = "/town outlaw";
		displayName = "Town Outlaw";
		
		commands.put("add", "[name] - Adds this player to the outlaw list.");
		commands.put("remove", "[name] - Removes this player from the outlaw list.");
		commands.put("list", "Lists all of the town's current outlaws.");
		commands.put("addall", "[town] - Adds entire town to the outlaw list.");
		commands.put("removeall", "[town] - Removes entire town from the outlaw list.");
	}
	
	public void addall_cmd() throws CivException {
		Town town = getSelectedTown();
		Town targetTown = getNamedTown(1);
	
		for (Resident resident : targetTown.getResidents()) {
			
			try {
				Player player = CivGlobal.getPlayer(args[1]);
				CivMessage.send(player, CivColor.Yellow+ChatColor.BOLD+"You're going to be declared an outlaw in "+
						town.getName()+"! You have one minute to get out ...");
			} catch (CivException e) {
			}
			TaskMaster.asyncTask(new TownAddOutlawTask(resident.getName(), town), 1000);
		}
		CivMessage.sendSuccess(sender, args[1]+" will be an outlaw in 60 seconds.");
	}
	
	public void removeall_cmd() throws CivException {
		Town town = getSelectedTown();
		Town targetTown = getNamedTown(1);
		
		for (Resident resident : targetTown.getResidents()) {
			town.removeOutlaw(resident.getName());
		}
		town.save();
	}
	
	public void add_cmd() throws CivException {
		Town town = getSelectedTown();
		
		if (args.length < 2) {
			throw new CivException("Enter player name to declare as an outlaw.");
		}
		
		Resident resident = getNamedResident(1);
		if (resident.getTown()== town) {
			throw new CivException("Cannot declare one of your own town members as an outlaw.");
		}
		
		try {
			Player player = CivGlobal.getPlayer(args[1]);
			CivMessage.send(player, CivColor.Yellow+ChatColor.BOLD+"You're going to be declared an outlaw in "+town.getName()+"! You have one minute to get out ...");			
		} catch (CivException e) {
		}	
		
		CivMessage.sendSuccess(sender, args[1]+" will be an outlaw in 60 seconds.");
		TaskMaster.asyncTask(new TownAddOutlawTask(args[1], town), 1000);	
	}
	
	public void remove_cmd() throws CivException {
		Town town = getSelectedTown();
		
		if (args.length < 2) {
			throw new CivException("Enter player name to remove as an outlaw.");
		}
		
		town.removeOutlaw(args[1]);
		town.save();
		
		CivMessage.sendSuccess(sender, "Removed "+args[1]+" from being an outlaw.");
	}
	
	public void list_cmd() throws CivException {
		Town town = getSelectedTown();
		
		CivMessage.sendHeading(sender, "Town Outlaws");
		
		String out = "";
		for (String outlaw : town.outlaws) {
			out += outlaw + ",";
		}
		
		CivMessage.send(sender, out);
		
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		validMayorAssistantLeader();
	}

}
