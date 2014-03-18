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

import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.command.ReportChestsTask;
import com.avrgaming.civcraft.command.town.TownInfoCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.randomevents.ConfigRandomEvent;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;

public class AdminTownCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad town";
		displayName = "Admin town";
		
		commands.put("disband", "[town] - disbands this town");
		commands.put("claim", "[town] - forcibly claims the plot you stand on for this named town.");
		commands.put("unclaim", "forcibly unclaims the plot you stand on.");
		commands.put("hammerrate", "[town] [amount] set this town's hammer rate to this amount.");
		commands.put("addmayor", "[town] [player] - adds the player as a mayor of this town.");
		commands.put("addassistant", "[town] [player] - adds this player as an assistant to this town.");
		commands.put("rmmayor", "[town] [player] - remove this player as a mayor from this town.");
		commands.put("rmassistant", "[town] [player] - remove this player as an assistant from this town.");
		commands.put("tp", "[town] - teleports to this town's town hall");
		commands.put("culture", "[town] [amount] - gives this town this amount of culture.");
		commands.put("info", "[town] - shows information for this town as-if you were a resident.");
		commands.put("setciv", "[town] [civ] - changes this town's civilization to the named civ.");
		commands.put("select", "[town] - selects this town as if you were the owner.");
		commands.put("claimradius", "[radius] - claims chunks in this radius.");
		commands.put("chestreport", "[town] Report on the chests town.");
		commands.put("rebuildgroups", "[town] - Remakes town's protected groups if they are not present.");
		commands.put("capture", "[winner civ] [loser town] - Captures the named town for this civ.");
		commands.put("setmotherciv", "[town] [motherciv] - Changes the mother civ of this town.");
		commands.put("sethappy", "[town] [amount] - Sets a magical base happiness for this town.");
		commands.put("setunhappy", "[town] [amount] - sets a magical base unhappiness for this town.");
		commands.put("event", "[town] [event_id] - Runs the named random event in this town.");
		commands.put("rename", "[town] [new_name] - Renames this town.");
	}
	
	public void rename_cmd() throws CivException, InvalidNameException {
		Town town = getNamedTown(1);
		String name = getNamedString(2, "Name for new town.");
		
		if (args.length < 3) {
			throw new CivException("Use underscores for names with spaces.");
		}
		
		town.rename(name);
		CivMessage.sendSuccess(sender, "Renamed town.");
	}
	
	public void event_cmd() throws CivException {
		Town town = getNamedTown(1);
		
		if (args.length < 3) {
			CivMessage.sendHeading(sender, "Available Events");
			String out = "";
			for (ConfigRandomEvent configEvent : CivSettings.randomEvents.values()) {
				out += configEvent.id+",";
			}
			CivMessage.send(sender, out);
			return;
		}
		
		ConfigRandomEvent event = CivSettings.randomEvents.get(args[2]);
		RandomEvent randEvent = new RandomEvent(event);
		randEvent.start(town);
		CivMessage.sendSuccess(sender, "Started event:"+event.name);
	}
	
	public void setunhappy_cmd() throws CivException {
		Town town = getNamedTown(1);
		double happy = getNamedDouble(2);
		
		town.setBaseUnhappy(happy);
		CivMessage.sendSuccess(sender, "Set unhappiness.");

	}
	
	public void sethappy_cmd() throws CivException {
		Town town = getNamedTown(1);
		double happy = getNamedDouble(2);
		
		town.setBaseHappiness(happy);
		CivMessage.sendSuccess(sender, "Set happiness.");

	}
	
	public void setmotherciv_cmd() throws CivException {
		Town town = getNamedTown(1);
		Civilization civ = getNamedCiv(2);
		
		town.setMotherCiv(civ);
		town.save();
		
		CivMessage.sendSuccess(sender, "Set town "+town.getName()+" to civ "+civ.getName());
	}
	
	public void capture_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Town town = getNamedTown(2);
		
		town.onDefeat(civ);
		CivMessage.sendSuccess(sender, "Captured.");
	}
	
	public void rebuildgroups_cmd() throws CivException {
		Town town = getNamedTown(1);
		
		if (town.getDefaultGroup() == null) {
			PermissionGroup residents;
			try {
				residents = new PermissionGroup(town, "residents");		
				town.setDefaultGroup(residents);
				try {
					residents.saveNow();
					town.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} catch (InvalidNameException e1) {
				e1.printStackTrace();
			}
			CivMessage.sendSuccess(sender, "Created residents group.");
		}
		
		if (town.getAssistantGroup() == null) {
			PermissionGroup assistant;
			try {
				assistant = new PermissionGroup(town, "assistants");
				
				town.setAssistantGroup(assistant);
				try {
					assistant.saveNow();
					town.saveNow(); 
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			 catch (InvalidNameException e) {
					e.printStackTrace();
				}
			CivMessage.sendSuccess(sender, "Created assistants group.");
		}
		
		if (town.getMayorGroup() == null) {
			PermissionGroup mayor;
			try {
				mayor = new PermissionGroup(town, "mayors");	
				town.setMayorGroup(mayor);
				try {
					mayor.saveNow();
					town.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			} catch (InvalidNameException e) {
				e.printStackTrace();
			}
			CivMessage.sendSuccess(sender, "Created mayors groups.");
		}
		
	}
	
	public void chestreport_cmd() throws CivException {
		Town town = getNamedTown(1);
				
		Queue<ChunkCoord> coords = new LinkedList<ChunkCoord>(); 
		for (TownChunk tc : town.getTownChunks()) {
			ChunkCoord coord = tc.getChunkCoord();
			coords.add(coord);
		}
		
		CivMessage.sendHeading(sender, "Chests with Goodies in "+town.getName());
		CivMessage.send(sender, "Processing (this may take a while)");
		TaskMaster.syncTask(new ReportChestsTask(sender, coords), 0);
		
	}
	
	public static int claimradius(Town town, Location loc, Integer radius) {
			ChunkCoord coord = new ChunkCoord(loc);
						
			int count = 0;
			for (int x = -radius; x < radius; x++) {
				for (int z = -radius; z < radius; z++) {
					try {
						ChunkCoord next = new ChunkCoord(coord.getWorldname(), coord.getX() + x, coord.getZ() + z);
						TownChunk.townHallClaim(town, next);
						count++;
					} catch (CivException e) {
						//ignore errors...
					}
				}
			}
			
			town.save();
			return count;
	}
	
	public void claimradius_cmd() throws CivException {
		Town town = getSelectedTown();
		Integer radius = getNamedInteger(1);
	
		int count = claimradius(town, getPlayer().getLocation(), radius);
		CivMessage.sendSuccess(sender, "Claimed "+count+" chunks");
	}
	
	public void select_cmd() throws CivException {
		Resident resident = getResident();
		Town selectTown = getNamedTown(1);
		
		if (resident.getSelectedTown() == null) {
			if (resident.getTown() == selectTown) {
				throw new CivException("You already have "+selectTown.getName()+" selected.");
			}
		}
		
		if (resident.getSelectedTown() == selectTown) {
			throw new CivException("You already have "+selectTown.getName()+" selected.");
		}
						
		resident.setSelectedTown(selectTown);
		CivMessage.sendSuccess(sender, "You have selected "+selectTown.getName()+".");
	}
	
	
	public void setciv_cmd() throws CivException {
		Town town = getNamedTown(1);
		Civilization civ = getNamedCiv(2);
		
		if (town.getCiv() == civ) {
			throw new CivException("Town already belongs to civilization "+civ.getName());
		}
		
		if (town.isCapitol()) {
			throw new CivException("Cannot move the capitol town.");
		}
		
		town.changeCiv(civ);		
		CivGlobal.processCulture();
		CivMessage.global("An admin has moved the town of "+town.getName()+" to civilization "+civ.getName());
		
	}
	
	public void info_cmd() throws CivException {
		Town town = getNamedTown(1);
		
		TownInfoCommand cmd = new TownInfoCommand();	
		cmd.senderTownOverride = town;
		cmd.senderCivOverride = town.getCiv();
		cmd.onCommand(sender, null, "info", this.stripArgs(args, 2));		
	}
	
	public void culture_cmd() throws CivException {
		Town town = getNamedTown(1);
		Integer culture = getNamedInteger(2);
		
		town.addAccumulatedCulture(culture);
		town.save();
		
		CivMessage.sendSuccess(sender, "Gave "+town.getName()+" "+culture+" culture points.");
	}
	
	public void tp_cmd() throws CivException {
		Town town = getNamedTown(1);
		
		TownHall townhall = town.getTownHall();
	
		if (sender instanceof Player) {
			if (townhall != null && townhall.isComplete()) {
				BlockCoord bcoord = townhall.getRandomRevivePoint();
				((Player)sender).teleport(bcoord.getLocation());
				CivMessage.sendSuccess(sender, "Teleported to "+town.getName());
				return;
			} else {
				if (town.getTownChunks().size() > 0) {
					ChunkCoord coord = town.getTownChunks().iterator().next().getChunkCoord();
					
					Location loc = new Location(Bukkit.getWorld(coord.getWorldname()), (coord.getX() << 4), 100, (coord.getZ() << 4));
					((Player)sender).teleport(loc);
					CivMessage.sendSuccess(sender, "Teleported to "+town.getName());
					return;
				}
			}
			
			throw new CivException("Couldn't find a town hall or a town chunk to teleport to.");
		}
		
	}
	
	
	public void rmassistant_cmd() throws CivException {
		Town town = getNamedTown(1);
		Resident resident = getNamedResident(2);
		
		if (!town.getAssistantGroup().hasMember(resident)) {
			throw new CivException(resident.getName()+" is not in the assistants group in "+town.getName());
		}
	
		town.getAssistantGroup().removeMember(resident);
		try {
			town.getAssistantGroup().saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		CivMessage.sendSuccess(sender, "Removed"+resident.getName()+" to assistants group in "+town.getName());
		
	}
	
	public void rmmayor_cmd() throws CivException {
		Town town = getNamedTown(1);
		Resident resident = getNamedResident(2);
		
		if (!town.getMayorGroup().hasMember(resident)) {
			throw new CivException(resident.getName()+" is not in the mayors group in "+town.getName());
		}
	
		town.getMayorGroup().removeMember(resident);
		try {
			town.getMayorGroup().saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		CivMessage.sendSuccess(sender, "Removed"+resident.getName()+" to mayors group in "+town.getName());
		
	}
	
	public void addassistant_cmd() throws CivException {
		Town town = getNamedTown(1);
		Resident resident = getNamedResident(2);
		
		town.getAssistantGroup().addMember(resident);
		try {
			town.getAssistantGroup().saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		CivMessage.sendSuccess(sender, "Added "+resident.getName()+" to assistants group in "+town.getName());
		
	}
	
	public void addmayor_cmd() throws CivException {
		Town town = getNamedTown(1);
		Resident resident = getNamedResident(2);
		
		town.getMayorGroup().addMember(resident);
		try {
			town.getMayorGroup().saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		CivMessage.sendSuccess(sender, "Added "+resident.getName()+" to mayors group in "+town.getName());
		
	}
	
	public void disband_cmd() throws CivException {
		Town town = getNamedTown(1);
		
		if (town.isCapitol()) {
			throw new CivException("Cannot disband the capitol town, disband the civilization instead.");
		}
		
		CivMessage.sendTown(town, "Your town is has disbanded by an admin!");
		try {
			town.delete();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		CivMessage.sendSuccess(sender, "Town disbanded");
	}
	
	public void hammerrate_cmd() throws CivException {
		if (args.length < 3) {
			throw new CivException("Enter a town name and amount");
		}
		
		Town town = getNamedTown(1);
		
		try {
			town.setHammerRate(Double.valueOf(args[2]));
			CivMessage.sendSuccess(sender, "Set "+args[1]+" hammer rate to "+args[2]);
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" is not a number.");
		}
		
		town.save();
	}
	
	public void unclaim_cmd() throws CivException {
		Town town = getNamedTown(1);
		Player player = getPlayer();

		TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
		if (tc != null) {
			
			tc.getTown().removeTownChunk(tc);
			CivGlobal.removeTownChunk(tc);
			try {
				tc.delete();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			
			town.save();
			
			CivMessage.sendSuccess(player, "Unclaimed plot from "+town.getName());
		} else {
			CivMessage.sendError(sender, "This plot is not owned.");
		}
		
	}

	public void claim_cmd() throws CivException {
		Town town = getNamedTown(1);
		Player player = getPlayer();

		TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
		if (tc == null) {
			tc = new TownChunk(town, player.getLocation());
			CivGlobal.addTownChunk(tc);
			try {
				town.addTownChunk(tc);
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
			}
			
			tc.save();
			town.save();
			
			CivMessage.sendSuccess(player, "Claimed plot for "+town.getName());
		} else {
			CivMessage.sendError(sender, "This plot is already owned by town "+town.getName()+" use unclaim first.");
		}
		
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
		//Admin permission checked in parent.
	}

}
