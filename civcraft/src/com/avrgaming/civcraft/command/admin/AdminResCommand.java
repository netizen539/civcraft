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

import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigPlatinumReward;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.global.perks.PlatinumManager;

public class AdminResCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad res";
		displayName = "Admin Resident";
		
		commands.put("settown", "[player] [town] - puts this player in this town.");
		commands.put("setcamp", "[player] [camp] - puts this player in this camp.");
		commands.put("cleartown", "[resident] - clears this residents town.");
		commands.put("enchant", "[enchant] [level] - Adds the enchantment with level to the item in your hand.");
		commands.put("giveplat", "[player] [amount] - Gives this player the specified amount of platinum.");
		commands.put("givereward", "[player] [rewardID] - Gives player this achievement with its plat rewards.");
		commands.put("rename", "[old_name] [new_name] - Rename this resident. Useful if players change their name.");
	}
	
	public void rename_cmd() throws CivException {
		Resident resident = getNamedResident(1);
		String newName = getNamedString(2, "Enter a new name");

		
		
		Resident newResident = CivGlobal.getResident(newName);
		if (newResident != null) {
			throw new CivException("Already another resident with the name:"+newResident.getName()+" cannot rename "+resident.getName());
		}
		
		/* Create a dummy resident to make sure name is valid. */
		try {
			new Resident(null, newName);
		} catch (InvalidNameException e1) {
			throw new CivException("Invalid name. Pick again.");
		}
		
		/* Delete the old resident object. */
		try {
			resident.delete();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException(e.getMessage());
		}
		
		/* Remove resident from CivGlobal tables. */
		CivGlobal.removeResident(resident);
		
		/* Change the resident's name. */
		try {
			resident.setName(newName);
		} catch (InvalidNameException e) {
			e.printStackTrace();
			throw new CivException("Internal error:"+e.getMessage());
		}
		
		/* Resave resident to DB and global tables. */
		CivGlobal.addResident(resident);
		resident.save();
		
		CivMessage.send(sender, "Resident renamed.");
	}
	
	public void givereward_cmd() throws CivException {
		Resident resident = getNamedResident(1);
		String rewardID = getNamedString(2, "Enter a Reward ID");
		
		for (ConfigPlatinumReward reward : CivSettings.platinumRewards.values()) {
			if (reward.name.equals(rewardID)) {
				switch (reward.occurs) {
				case "once":
					PlatinumManager.givePlatinumOnce(resident, reward.name, reward.amount, "Sweet! An admin gave you a platinum reward of %d");
					break;
				case "daily":
					PlatinumManager.givePlatinumDaily(resident, reward.name, reward.amount, "Sweet! An admin gave you a platinum reward of %d");
					break;
				default:
					PlatinumManager.givePlatinum(resident, reward.amount, "Sweet! An admin gave you a platinum reward of %d");
					break;
				}
				CivMessage.sendSuccess(sender, "Reward Given.");
				return;
			}
		}
		
		CivMessage.sendError(sender, "Couldn't find reward named:"+rewardID);
	}
	
	
	public void giveplat_cmd() throws CivException {
		Resident resident = getNamedResident(1);
		Integer plat = getNamedInteger(2);
		
		PlatinumManager.givePlatinum(resident, plat, "Sweet! You were given %d by an admin!");
		CivMessage.sendSuccess(sender, "Gave "+resident.getName()+" "+plat+" platinum");
	}
	
	public void enchant_cmd() throws CivException {
		Player player = getPlayer();
		String enchant = getNamedString(1, "Enchant name");
		int level = getNamedInteger(2);
		
		
		ItemStack stack = player.getItemInHand();
		Enchantment ench = Enchantment.getByName(enchant);
		if (ench == null) {
			String out ="";
			for (Enchantment ench2 : Enchantment.values()) {
				out += ench2.getName()+",";
			}
			throw new CivException("No enchantment called "+enchant+" Options:"+out);
		}
		
		stack.addUnsafeEnchantment(ench, level);
		CivMessage.sendSuccess(sender, "Enchanted.");
	}
	
	public void cleartown_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter a player name");
		}
				
		Resident resident = getNamedResident(1);
		
		if (resident.hasTown()) {
			resident.getTown().removeResident(resident);
		}
		
		resident.save();
		CivMessage.sendSuccess(sender, "Cleared "+resident.getName()+" from any town.");

	}
	
	public void setcamp_cmd() throws CivException {		
		Resident resident = getNamedResident(1);
		Camp camp = getNamedCamp(2);

		if (resident.hasCamp()) {
			resident.getCamp().removeMember(resident);
		}		
		
		camp.addMember(resident);
		
		camp.save();
		resident.save();
		CivMessage.sendSuccess(sender, "Moved "+resident.getName()+" into camp "+camp.getName());
	}
	
	
	public void settown_cmd() throws CivException {
		
		if (args.length < 3) {
			throw new CivException("Enter player and its new town.");
		}
		
		Resident resident = getNamedResident(1);

		Town town = getNamedTown(2);

		if (resident.hasTown()) {
			resident.getTown().removeResident(resident);
		}
		
		try {
			town.addResident(resident);
		} catch (AlreadyRegisteredException e) {
			e.printStackTrace();
			throw new CivException("Already in this town?");
		}
		
		town.save();
		resident.save();
		CivMessage.sendSuccess(sender, "Moved "+resident.getName()+" into town "+town.getName());
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
