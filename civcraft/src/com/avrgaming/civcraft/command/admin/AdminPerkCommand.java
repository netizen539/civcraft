package com.avrgaming.civcraft.command.admin;

import java.sql.SQLException;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.global.perks.PlatinumManager;

public class AdminPerkCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad perk";
		displayName = "Admin Perk";
		
		commands.put("give", "[resident] [perk id] [count] - Gives this resident, the selected perk id [count] many times.");
		commands.put("remove", "[resident] [perk_id] [count] - Removes up to [count] perks from given resident");
		commands.put("list", "Lists all configured perks and their id's");
	}

	public void list_cmd() {
		CivMessage.sendHeading(sender, "Configured Perks");
		for (ConfigPerk perk : CivSettings.perks.values()) {
			CivMessage.send(sender, CivColor.Green+perk.display_name+CivColor.LightGreen+" id:"+CivColor.Rose+perk.id);
		}
		CivMessage.send(sender, CivColor.LightGray+"If list is too long, see perks.yml for all IDs.");
	}
	
	public void remove_cmd() throws CivException {
		Resident resident = getNamedResident(1);
		String perk_id = getNamedString(2, "enter a perk id");
		Integer count = getNamedInteger(3);
		
		ConfigPerk configPerk = CivSettings.perks.get(perk_id);
		if (configPerk == null) {
			throw new CivException("Unknown perk id:"+perk_id+" see '/ad perk list' for options.");
		}
		
		int deleted = 0;
		try {
			deleted = CivGlobal.perkManager.removePerkFromResident(resident, perk_id, count);
		} catch (SQLException e) {
			e.printStackTrace();
			CivMessage.sendError(sender, "SQL Error. See logs for details.");
		}
		
		resident.perks.clear();
		resident.loadPerks();
		CivMessage.sendSuccess(sender, "Removed "+deleted+" "+perk_id+" to "+resident.getName());
	}
	
	public void give_cmd() throws CivException {
		Resident resident = getNamedResident(1);
		String perk_id = getNamedString(2, "enter a perk id");
		Integer count = getNamedInteger(3);
		
		ConfigPerk configPerk = CivSettings.perks.get(perk_id);
		if (configPerk == null) {
			throw new CivException("Unknown perk id:"+perk_id+" see '/ad perk list' for options.");
		}
		
		int added = 0;
		try {
			added = CivGlobal.perkManager.addPerkToResident(resident, perk_id, count);
		} catch (SQLException e) {
			e.printStackTrace();
			CivMessage.sendError(sender, "SQL Error. See logs for details.");
		}
		
		resident.perks.clear();
		resident.loadPerks();
		CivMessage.sendSuccess(sender, "Added "+added+" "+perk_id+" to "+resident.getName());
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
		if (!PlatinumManager.isEnabled()) {
			throw new CivException("Perk system must be enabled first. See perks.yml");
		}
		
		if (PlatinumManager.isLegacyEnabled()) {
			throw new CivException("Perk command does not work with legacy perks system enabled. See perks.yml");
		}
	}

}
