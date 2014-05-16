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

import java.io.IOException;
import java.text.DecimalFormat;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class BuildCommand extends CommandBase {

	@Override
	public void init() {
		command = "/build";
		displayName = "Build";
		sendUnknownToDefault = true;
		
		commands.put("list", "shows all available structures.");
		commands.put("progress", "Shows progress of currently building structures.");
		commands.put("repairnearest", "Repairs destroyed structures.");
		commands.put("undo", "Undo the last structure built.");
		commands.put("demolish", "[location] - destroys the structure at this location.");
		commands.put("demolishnearest", "- destroys the nearest structure. Requires confirmation.");
		commands.put("refreshnearest", "Refreshes the nearest structure's blocks. Requires confirmation.");
		commands.put("validatenearest", "Validates the nearest structure. Removing any validation penalties if it's ok.");
		//commands.put("preview", "shows a preview of this structure at this location.");
	}
	
	public void validatenearest_cmd() throws CivException {
		Player player = getPlayer();
		Resident resident = getResident();
		Buildable buildable = CivGlobal.getNearestBuildable(player.getLocation());
		
		if (buildable.getTown() != resident.getTown()) {
			throw new CivException("You can only validate structures inside your own town.");
		}
		
		if (War.isWarTime()) {
			throw new CivException("Cannot validate structures during WarTime.");
		}
		
		if (buildable.isIgnoreFloating()) {
			throw new CivException(buildable.getDisplayName()+" is exempt from floating structure checks.");
		}
		
		CivMessage.sendSuccess(player, "Running Validation on "+buildable.getDisplayName()+" at "+buildable.getCenterLocation()+"...");
		buildable.validate(player);
	}
	
	public void refreshnearest_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident resident = getResident();
		town.refreshNearestBuildable(resident);
	}
	
	public void repairnearest_cmd() throws CivException {
		Town town = getSelectedTown();
		Player player = getPlayer();
		
		if (War.isWarTime()) {
			throw new CivException("You cannot repair structures during WarTime.");
		}
		
		Structure nearest = town.getNearestStrucutre(player.getLocation());
			
		if (nearest == null) {
			throw new CivException ("Couldn't find a structure.");
		}
		
		if (!nearest.isDestroyed()) {
			throw new CivException (nearest.getDisplayName()+" at "+nearest.getCorner()+" is not destroyed.");
		}
		
		if (!town.getCiv().hasTechnology(nearest.getRequiredTechnology())) {
			throw new CivException ("You do not have the technology to repair "+nearest.getDisplayName()+" at "+nearest.getCorner());
		}
	
		if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
			CivMessage.send(player, CivColor.LightGreen+"Are you sure you want to repair the structure "+CivColor.Yellow+nearest.getDisplayName()+
					CivColor.LightGreen+" at "+CivColor.Yellow+nearest.getCorner()+CivColor.LightGreen+" for "+CivColor.Yellow+nearest.getRepairCost()+" coins?");
			CivMessage.send(player, CivColor.LightGray+"If yes, use /build repairnearest yes");
			return;
		}
		
		town.repairStructure(nearest);		
		CivMessage.sendSuccess(player, nearest.getDisplayName()+" repaired.");
	}
	
	public void demolishnearest_cmd() throws CivException {
		Town town = getSelectedTown();
		Player player = getPlayer();
		
		Structure nearest = town.getNearestStrucutre(player.getLocation());
		
		if (nearest == null) {
			throw new CivException ("Couldn't find a structure.");
		}
		
		if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
			CivMessage.send(player, CivColor.LightGreen+"Are you sure you want to demolish the structure "+CivColor.Yellow+nearest.getDisplayName()+
					CivColor.LightGreen+" at "+CivColor.Yellow+nearest.getCorner()+CivColor.LightGreen+" ?");
			CivMessage.send(player, CivColor.LightGray+"If yes, use /build demolishnearest yes");
						
			nearest.flashStructureBlocks();
			return;
		}
		
		town.demolish(nearest, false);
		CivMessage.sendSuccess(player, nearest.getDisplayName()+" at "+nearest.getCorner()+" demolished.");
	}
	
	
	public void demolish_cmd() throws CivException {
		Town town = getSelectedTown();
		
		
		if (args.length < 2) {
			CivMessage.sendHeading(sender, "Demolish Structure");
			for (Structure struct : town.getStructures()) {
				CivMessage.send(sender, struct.getDisplayName()+" type: "+CivColor.Yellow+struct.getCorner().toString()+
						CivColor.White+" to demolish");
			}
			return;
		}
		
		try {
			BlockCoord coord = new BlockCoord(args[1]);
			Structure struct = town.getStructure(coord);
			if (struct == null) {
				CivMessage.send(sender, CivColor.Rose+"No structure at "+args[1]);
				return;
			}
			struct.getTown().demolish(struct, false);
			CivMessage.sendTown(struct.getTown(), struct.getDisplayName()+" has been demolished.");
		} catch (NumberFormatException | ArrayIndexOutOfBoundsException e) {
			CivMessage.sendError(sender, "Bad formatting. make sure to enter the text *exactly* as shown in yellow.");
		}
	}
	
	public void undo_cmd() throws CivException {
		Town town = getSelectedTown();
		town.processUndo();
	}
	
	public void progress_cmd() throws CivException {
		CivMessage.sendHeading(sender, "Building Structures");
		Town town = getSelectedTown();
		for (BuildAsyncTask task : town.build_tasks) {
			Buildable b = task.buildable;
			DecimalFormat df = new DecimalFormat();
			
			CivMessage.send(sender, CivColor.LightPurple+b.getDisplayName()+": "+CivColor.Yellow+"("+df.format(b.getBuiltHammers()) + "/"+b.getHammerCost()+")"+
					CivColor.LightPurple+" Blocks "+CivColor.Yellow+"("+b.builtBlockCount+"/"+b.getTotalBlockCount()+")");
			
			//CivMessage.send(sender, CivColor.LightPurple+b.getDisplayName()+" "+CivColor.Yellow+"("+
				//	b.builtBlockCount+" / "+b.getTotalBlockCount()+")");
		}
		
	}

	public void list_available_structures() throws CivException {
		CivMessage.sendHeading(sender, "Available Structures");
		Town town = getSelectedTown();
		for (ConfigBuildableInfo sinfo : CivSettings.structures.values()) {
			if (sinfo.isAvailable(town)) {
				String leftString = "";
				if (sinfo.limit == 0) {
					leftString = "Unlimited";
				} else {
					leftString = ""+(sinfo.limit - town.getStructureTypeCount(sinfo.id));
				}
				
				CivMessage.send(sender, CivColor.LightPurple+sinfo.displayName+
						CivColor.Yellow+
						" Cost: "+sinfo.cost+
						" Upkeep: "+sinfo.upkeep+" Hammers: "+sinfo.hammer_cost+ 
						" Left: "+leftString);
			}
		}
	}
	
	public void list_available_wonders() throws CivException {
		CivMessage.sendHeading(sender, "Available Wonders");
		Town town = getSelectedTown();
		for (ConfigBuildableInfo sinfo : CivSettings.wonders.values()) {
			if (sinfo.isAvailable(town)) {
				String leftString = "";
				if (sinfo.limit == 0) {
					leftString = "Unlimited";
				} else {
					leftString = ""+(sinfo.limit - town.getStructureTypeCount(sinfo.id));
				}
				
				if (Wonder.isWonderAvailable(sinfo.id)) {				
					CivMessage.send(sender, CivColor.LightPurple+sinfo.displayName+
							CivColor.Yellow+
							" Cost: "+sinfo.cost+
							" Upkeep: "+sinfo.upkeep+" Hammers: "+sinfo.hammer_cost+ 
							" Left: "+leftString);
				} else {
					Wonder wonder = CivGlobal.getWonderByConfigId(sinfo.id);
					CivMessage.send(sender, CivColor.LightGray+sinfo.displayName+" Cost: "+sinfo.cost+" - Already built in "+
							wonder.getTown().getName()+"("+wonder.getTown().getCiv().getName()+")");
				}
			}
		}
	}
	
	public void list_cmd() throws CivException {
		this.list_available_structures();
		this.list_available_wonders();
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		if (args.length == 0) {		
			showHelp();
			return;
		}
		
		String fullArgs = "";
		for (String arg : args) {
			fullArgs += arg + " ";
		}
		fullArgs = fullArgs.trim();
		
		buildByName(fullArgs);
	}

	public void preview_cmd() throws CivException {
		String fullArgs = this.combineArgs(this.stripArgs(args, 1));
		
		ConfigBuildableInfo sinfo = CivSettings.getBuildableInfoByName(fullArgs);
		if (sinfo == null) {
			throw new CivException("Unknown structure "+fullArgs);
		}
		
		Town town = getSelectedTown();
		if (sinfo.isWonder) {
			Wonder wonder = Wonder.newWonder(getPlayer().getLocation(), sinfo.id, town);
			try {
				wonder.buildPlayerPreview(getPlayer(), getPlayer().getLocation());
			} catch (IOException e) {
				e.printStackTrace();
				throw new CivException("Internal IO Error.");
			}
		} else {
		Structure struct = Structure.newStructure(getPlayer().getLocation(), sinfo.id, town);
			try {
				struct.buildPlayerPreview(getPlayer(), getPlayer().getLocation());
			} catch (IOException e) {
				e.printStackTrace();
				throw new CivException("Internal IO Error.");
			}
		}
		CivMessage.sendSuccess(sender, "Showing preview.");
	}
	
	
	private void buildByName(String fullArgs) throws CivException {
		ConfigBuildableInfo sinfo = CivSettings.getBuildableInfoByName(fullArgs);

		if (sinfo == null) {
			throw new CivException("Unknown structure "+fullArgs);
		}
		
		Town town = getSelectedTown();
		
		if (sinfo.isWonder) {
			Wonder wonder = Wonder.newWonder(getPlayer().getLocation(), sinfo.id, town);
			try {
				wonder.buildPlayerPreview(getPlayer(), getPlayer().getLocation());
			} catch (IOException e) {
				e.printStackTrace();
				throw new CivException("Internal IO Error.");
			}
		} else {
			Structure struct = Structure.newStructure(getPlayer().getLocation(), sinfo.id, town);
			try {
				struct.buildPlayerPreview(getPlayer(), getPlayer().getLocation());
			} catch (IOException e) {
				e.printStackTrace();
				throw new CivException("Internal IO Error.");
			}
		}
		
//		if (sinfo.isWonder) {
//			town.buildWonder(getPlayer(), sinfo.id, getPlayer().getLocation());
//		} else {
//			town.buildStructure(getPlayer(), sinfo.id, getPlayer().getLocation());
//		}
//		CivMessage.sendSuccess(sender, "Started building "+sinfo.displayName);
	}

	@Override
	public void showHelp() {
		showBasicHelp();		
		CivMessage.send(sender, CivColor.LightPurple+command+" "+CivColor.Yellow+"[structure name] "+
				CivColor.LightGray+"builds this structure at your location.");
	}

	@Override
	public void permissionCheck() throws CivException {
		validMayorAssistantLeader();
	}

}
