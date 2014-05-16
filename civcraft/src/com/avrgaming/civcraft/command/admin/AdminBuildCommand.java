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
package com.avrgaming.civcraft.command.admin;

import java.io.IOException;
import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.BuildableLayer;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class AdminBuildCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad build";
		displayName = "Admin Build";
		
		commands.put("demolish", "[town] [location] demolish the structure at this location.");
		commands.put("repair", "Fixes the nearest structure, requires confirmation.");
		commands.put("destroywonder", "[id] destroyes this wonder.");
		commands.put("destroynearest", "[town] destroys the nearest structure in this town. Confirmation is required.");
		commands.put("validatenearest", "[town] Validate the nearest structure in this town. Confirmation is required.");
		commands.put("validateall", "Gets all invalid buildables in the server.");
		commands.put("listinvalid", "lists all invalid buildables.");
		commands.put("showbuildable", "[loc] - show this buildable's y percertages.");

		//commands.put("repairwonder", "Fixes the nearest wonder, requires confirmation.");		
	}

	public void showbuildable_cmd() throws CivException {
		String locString = getNamedString(1, "Complete location.");
		
		for (Buildable buildable : Buildable.invalidBuildables) {
			if (buildable.getCorner().toString().equalsIgnoreCase(locString)) {
				
				for (Integer y : buildable.layerValidPercentages.keySet()) {
					BuildableLayer layer = buildable.layerValidPercentages.get(y);
					
					Double percentage = (double)layer.current / (double)layer.max;
					CivMessage.send(sender, "y:"+y+" percentage:"+percentage+" ("+layer.current+"/"+layer.max+")");
				}
			}
		}
		CivMessage.sendSuccess(sender, "Finished.");
	}
	
	public void listinvalid_cmd() {
		for (Buildable buildable : Buildable.invalidBuildables) {
			CivMessage.send(sender, buildable.getDisplayName()+" at "+buildable.getCorner()+" in "+buildable.getTown().getName());
		}
		CivMessage.sendSuccess(sender, "Finished.");
	}
	
	public void validateall_cmd() throws CivException {
		Buildable.invalidBuildables.clear();
		
		for (Structure struct : CivGlobal.getStructures()) {
			if (struct.isStrategic()) {
				struct.validate(null);
			}
		}
		
		for (Wonder wonder : CivGlobal.getWonders()) {
			if (wonder.isStrategic()) {
				wonder.validate(null);
			}
		}
		
		CivMessage.sendSuccess(sender, "Validating all structures.");
	}
	
	public void validatenearest_cmd() throws CivException {
		Player player = getPlayer();
		Town town = getNamedTown(1);
		Buildable buildable = town.getNearestBuildable(player.getLocation());
		
		if (args.length < 3 || !args[2].equalsIgnoreCase("yes")) {
			CivMessage.send(player, CivColor.Yellow+ChatColor.BOLD+"Would validate "+buildable.getDisplayName()+" at "+buildable.getCorner()+" are you sure? use '/ad validatenearest [town] yes' to confirm.");
			return;
		}
		
		buildable.validate(player);
	}
	
	
	public void destroynearest_cmd() throws CivException {				
		
		Town town = getNamedTown(1);
		Player player = getPlayer();
		
		Buildable struct = town.getNearestStrucutreOrWonderInprogress(player.getLocation());
		
		if (args.length < 3 || !args[2].equalsIgnoreCase("yes")) {
			CivMessage.send(player, CivColor.Yellow+ChatColor.BOLD+"Would destroy "+struct.getDisplayName()+" at "+struct.getCorner()+" are you sure? use '/ad destroynearest [town] yes' to confirm.");
			return;
		}
		
		struct.onDestroy();
		CivMessage.send(player, struct.getDisplayName()+" has been destroyed.");
	}
	
	public void destroywonder_cmd() throws CivException {
		Town town = getNamedTown(1);
		
		if (args.length < 2) {
			throw new CivException("enter wonder id to destroy.");
		}
		
		Wonder wonder = null;
		for (Wonder w : town.getWonders()) {
			if (w.getConfigId().equals(args[2])) {
				wonder = w;
				break;
			}
		}
		
		if (wonder == null) {
			throw new CivException("no wonder with id "+args[2]+" or it is not built yet");
		}
		
		wonder.fancyDestroyStructureBlocks();
		try {
			wonder.getTown().removeWonder(wonder);
			wonder.fancyDestroyStructureBlocks();
			wonder.unbindStructureBlocks();
			wonder.delete();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		CivMessage.sendSuccess(sender, "destroyed");
	}
	
	public void repair_cmd() throws CivException {
		Player player = getPlayer();
		
		Buildable nearest = CivGlobal.getNearestBuildable(player.getLocation());
		
		if (nearest == null) {
			throw new CivException ("Couldn't find a structure.");
		}
		
		if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
			CivMessage.send(player, CivColor.LightGreen+"Are you sure you want to repair the structure "+CivColor.Yellow+nearest.getDisplayName()+
					CivColor.LightGreen+" at "+CivColor.Yellow+nearest.getCorner()+CivColor.LightGreen+" ?");
			CivMessage.send(player, CivColor.LightGray+"If yes, use /ad repair yes");
			return;
		}
		
		try {
			nearest.repairFromTemplate();
		} catch (IOException e) {
			e.printStackTrace();
			throw new CivException("IO error. Couldn't find template file:"+nearest.getSavedTemplatePath()+" ?");
		}
		CivMessage.sendSuccess(player, nearest.getDisplayName()+" Repaired.");
		
	}
	
	public void demolish_cmd() throws CivException {		
		
		if (args.length < 2) {
			throw new CivException ("Enter a town and structure location.");
		}
		
		Town town = getNamedTown(1);
		
		if (args.length < 3) {
			CivMessage.sendHeading(sender, "Demolish Structure");
			for (Structure struct : town.getStructures()) {
				CivMessage.send(sender, struct.getDisplayName()+" type: "+CivColor.Yellow+struct.getCorner().toString()+
						CivColor.White+" to demolish");
			}
			return;
		}
		
		BlockCoord coord = new BlockCoord(args[2]);
		Structure struct = town.getStructure(coord);
		if (struct == null) {
			CivMessage.send(sender, CivColor.Rose+"No structure at "+args[2]);
			return;
		}
		
		struct.getTown().demolish(struct, true);
		
	
		CivMessage.sendTown(struct.getTown(), struct.getDisplayName()+" has been demolished.");
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
		
	}
}
