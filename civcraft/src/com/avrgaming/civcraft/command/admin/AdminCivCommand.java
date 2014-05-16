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

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.command.civ.CivInfoCommand;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class AdminCivCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad civ";
		displayName = "Admin civ";
		
		commands.put("disband", "[civ] - disbands this civilization");
		commands.put("addleader", "[civ] [player] - adds this player to the leaders group.");
		commands.put("addadviser", "[civ] [player] - adds this player to the advisers group.");
		commands.put("rmleader", "[civ] [player] - removes this player from the leaders group.");
		commands.put("rmadviser", "[civ] [player] - removes this player from the advisers group.");
		commands.put("givetech", "[civ] [tech_id] - gives this civilization this technology.");
		commands.put("beakerrate", "[civ] [amount] set this towns's beaker rate to this amount.");
		commands.put("toggleadminciv", "[civ] - sets/unsets this civilization to an admin civ. Prevents war.");
		commands.put("alltech", "[civ] - gives this civilization every technology.");
		commands.put("setrelation", "[civ] [otherCiv] [NEUTRAL|HOSTILE|WAR|PEACE|ALLY] sets the relationship between [civ] and [otherCiv].");
		commands.put("info", "[civ] - Processes /civ info command as if you were a member of this civilization.");
		commands.put("merge", "[oldciv] [newciv] - Merges oldciv into newciv. oldciv is then destroyed");
		commands.put("setgov", "[civ] [government] - Changes this civilization's government immediatly. Removes any anarchy timers.");
		commands.put("bankrupt", "[civ] Clear the coins of all towns, and all members of this civ. requires confirmation.");
		commands.put("setgov", "[civ] [gov_id] - sets this civ to this government, no anarchy");
		commands.put("conquered", "[civ] - Marks this civ as a conquered civ");
		commands.put("unconquer", "[civ] - Unmarks this civ as a conquered civ");
		commands.put("liberate", "[civ] - Liberates the specified civ if it is conquered.");
		commands.put("setvotes", "[civ] [votes] - sets this civ's diplomatic votes to this amount.");
		commands.put("rename", "[civ] [new name] - Renames this civ.");
	}
	
	public void liberate_cmd() throws CivException {
		Civilization motherCiv = getNamedCiv(1);
		
		/* Liberate the civ. */
		for (Town t : CivGlobal.getTowns()) {
			if (t.getMotherCiv() == motherCiv) {
				t.changeCiv(motherCiv);
				t.setMotherCiv(null);
				t.save();
			}
		}
		
		motherCiv.setConquered(false);
		CivGlobal.removeConqueredCiv(motherCiv);
		CivGlobal.addCiv(motherCiv);
		motherCiv.save();
		CivMessage.sendSuccess(sender, "Liberated "+motherCiv.getName());
	}
	
	public void rename_cmd() throws CivException, InvalidNameException {
		Civilization civ = getNamedCiv(1);
		String name = getNamedString(2, "Name for new civ.");
		
		if (args.length < 3) {
			throw new CivException("Use underscores for names with spaces.");
		}
		
		civ.rename(name);
		CivMessage.sendSuccess(sender, "Renamed civ.");
	}
	
	public void setvotes_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Integer votes = getNamedInteger(2);
		EndConditionDiplomacy.setVotes(civ, votes);
		CivMessage.sendSuccess(sender, "Set votes for "+civ.getName()+" to "+votes);
	}
	
	public void conquered_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		civ.setConquered(true);
		CivGlobal.removeCiv(civ);
		CivGlobal.addConqueredCiv(civ);
		civ.save();
		
		CivMessage.sendSuccess(sender, "civ is now conquered.");
	}
	
	public void unconquer_cmd() throws CivException {
		String conquerCiv = this.getNamedString(1, "conquered civ");
		
		Civilization civ = CivGlobal.getConqueredCiv(conquerCiv);
		if (civ == null) {
			civ = CivGlobal.getCiv(conquerCiv);
		}
		
		if (civ == null) {
			throw new CivException ("No civ called "+conquerCiv);
		}
		
		civ.setConquered(false);
		CivGlobal.removeConqueredCiv(civ);
		CivGlobal.addCiv(civ);
		civ.save();
		
		CivMessage.sendSuccess(sender, "Civ is now unconquered.");
	}
	
	
	public void bankrupt_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		if (args.length < 3) {
			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"Are you absolutely sure you want to wipe ALL COINS from ALL RESIDENTS and ALL TOWNS of this civ?");
			CivMessage.send(sender, "use /civ bankrupt yes if you do.");
		}
		
		civ.getTreasury().setBalance(0);
		
		for (Town town : civ.getTowns()) {
			town.getTreasury().setBalance(0);
			town.save();
			
			for (Resident resident : town.getResidents()) {
				resident.getTreasury().setBalance(0);
				resident.save();
			}
		}
		
		civ.save();
		CivMessage.sendSuccess(sender, "Bankrupted "+civ.getName());
	}
	
	public void setgov_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		if (args.length < 3) {
			throw new CivException("Enter a government name");
		}
		
		ConfigGovernment gov = CivSettings.governments.get(args[2]);
		if (gov == null) {
			throw new CivException("No government with that id.. try gov_monarchy, gov_depostism... etc");
		}
		// Remove any anarchy timers
		String key = "changegov_"+civ.getId();
		CivGlobal.getSessionDB().delete_all(key);
		
		civ.setGovernment(gov.id);
		CivMessage.global(civ.getName()+" has emerged from anarchy and has adopted "+CivSettings.governments.get(gov.id).displayName);
		CivMessage.sendSuccess(sender, "Successfully changed government");
		
	}
	
	public void merge_cmd() throws CivException {
		Civilization oldciv = getNamedCiv(1);
		Civilization newciv = getNamedCiv(2);
		
		if (oldciv == newciv) {
			throw new CivException("Cannot merge a civ into itself.");
		}
		
		newciv.mergeInCiv(oldciv);
		CivMessage.global("An admin has merged "+oldciv.getName()+" into "+newciv.getName());
	}
	
	public void info_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		CivInfoCommand cmd = new CivInfoCommand();	
		cmd.senderCivOverride = civ;
		cmd.onCommand(sender, null, "info", this.stripArgs(args, 2));	
	}
	
//	public void setmaster_cmd() throws CivException {
//		Civilization vassal = getNamedCiv(1);
//		Civilization master = getNamedCiv(2);
//		
//		if (vassal == master) {
//			throw new CivException("cannot make vassal and master the same");
//		}
//		
//		CivGlobal.setVassalState(master, vassal);
//		CivMessage.sendSuccess(sender, "Vassaled "+vassal.getName()+" to "+master.getName());
//		
//	}
	
	public void setmaster_cmd() {
		
	}
	
	public void setrelation_cmd() throws CivException {
		if (args.length < 4) {
			throw new CivException("Usage: [civ] [otherCiv] [NEUTRAL|HOSTILE|WAR|PEACE|ALLY]");
		}
		
		Civilization civ = getNamedCiv(1);
		Civilization otherCiv = getNamedCiv(2);
		
		Relation.Status status = Relation.Status.valueOf(args[3].toUpperCase());
		
		CivGlobal.setRelation(civ, otherCiv, status);
		if (status.equals(Status.WAR)) {
			CivGlobal.setAggressor(civ, otherCiv, civ);
			CivGlobal.setAggressor(otherCiv, civ, civ);
		}
		CivMessage.sendSuccess(sender, "Set relationship between "+civ.getName()+" and "+otherCiv.getName()+" to "+status.name());
		
	}
	
	public void alltech_cmd() throws CivException {
	
		Civilization civ = getNamedCiv(1);
		
		for (ConfigTech tech : CivSettings.techs.values()) {
			civ.addTech(tech);
		}
		
		civ.save();
		
		CivMessage.sendSuccess(sender, "All techs awarded.");
	}
	
	public void toggleadminciv_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		civ.setAdminCiv(!civ.isAdminCiv());
		civ.save();
		CivMessage.sendSuccess(sender, civ.getName()+" admin civ is now:"+civ.isAdminCiv());
	}
	
	public void beakerrate_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Double amount = getNamedDouble(2);
		
		civ.setBaseBeakers(amount);
		civ.save();

		CivMessage.sendSuccess(sender, "Set "+civ.getName()+" beaker rate to "+amount);
	}
	
	public void givetech_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		if (args.length < 3) {
			throw new CivException("Enter a tech ID");
		}
		
		ConfigTech tech = CivSettings.techs.get(args[2]);
		if (tech == null) {
			throw new CivException("No tech with ID:"+args[2]);
		}
		
		if (civ.hasTechnology(tech.id)) {
			throw new CivException("Civ "+civ.getName()+" already has tech id:"+tech.id);
		}
		
		civ.addTech(tech);
		civ.save();
		
		CivMessage.sendSuccess(sender, "Added "+tech.name+" to "+civ.getName());
		
	}
	
	public void rmadviser_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Resident resident = getNamedResident(2);
		
		if (civ.getAdviserGroup().hasMember(resident)) {
			civ.getAdviserGroup().removeMember(resident);
			civ.save();
			CivMessage.sendSuccess(sender, "Removed "+resident.getName()+" to advisers group in "+civ.getName());
		} else {
			CivMessage.sendError(sender, resident.getName()+" is not currently in the advisers group for "+civ.getName());
		}
	}
	
	public void rmleader_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Resident resident = getNamedResident(2);
		
		if (civ.getLeaderGroup().hasMember(resident)) {
			civ.getLeaderGroup().removeMember(resident);
			civ.save();
			CivMessage.sendSuccess(sender, "Removed "+resident.getName()+" to leaders group in "+civ.getName());
		} else {
			CivMessage.sendError(sender, resident.getName()+" is not currently in the leaders group for "+civ.getName());
		}
	}
	
	public void addadviser_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Resident resident = getNamedResident(2);
		
		civ.getAdviserGroup().addMember(resident);
		civ.getAdviserGroup().save();
		civ.save();
		
		CivMessage.sendSuccess(sender, "Added "+resident.getName()+" to advisers group in "+civ.getName());
	}

	public void addleader_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		Resident resident = getNamedResident(2);
		
		civ.getLeaderGroup().addMember(resident);
		civ.getLeaderGroup().save();
		civ.save();
		
		CivMessage.sendSuccess(sender, "Added "+resident.getName()+" to leaders group in "+civ.getName());
	}
	
	public void disband_cmd() throws CivException {
		Civilization civ = getNamedCiv(1);
		
		CivMessage.sendCiv(civ, "Your civ is has disbanded by an admin!");
		try {
			civ.delete();
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		CivMessage.sendSuccess(sender, "Civ disbanded");
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
		//Admin is checked in parent command
	}

}
