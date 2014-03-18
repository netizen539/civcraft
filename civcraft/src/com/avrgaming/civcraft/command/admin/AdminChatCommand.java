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

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

public class AdminChatCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad chat";
		displayName = "Admin Chat";
		
		commands.put("tc", "[town] - joins this town's chat channel.");
		commands.put("cc", "[civ] - join's this civ's chat channel.");
		commands.put("cclisten", "[name] toggles listening in on this civ's chat channel.");
		commands.put("tclisten", "[name] toggles listening in on this town's chat channel.");
		commands.put("listenoff", "removes you from all chat channels.");
		commands.put("cclistenall", "adds listening to every civ's chat channel.");
		commands.put("tclistenall", "adds listening to every town's chat channel.");	
		commands.put("banwordon", "Turns on banning from words.");
		commands.put("banwordoff", "Turns off banning from words.");
		commands.put("banwordadd", "Adds this word to the ban word list");
		commands.put("banwordremove", "Removes this word to the ban word list");
		commands.put("banwordtoggle", "Toggles all ban words to ban regardless of time online.");
		
	}

	public void tclistenall_cmd() throws CivException {
		Resident resident = getResident();
		
		for (Town t : CivGlobal.getTowns()) {
			CivMessage.addExtraTownChatListener(t, resident.getName());
		}
		
		CivMessage.sendSuccess(sender, "Added you from all town chat channels.");
	}
	
	public void cclistenall_cmd() throws CivException {
		Resident resident = getResident();
		
		for (Civilization civ : CivGlobal.getCivs()) {
			CivMessage.addExtraCivChatListener(civ, resident.getName());
		}
		
		CivMessage.sendSuccess(sender, "Added you from all civ chat channels.");
	}
	
	public void listenoff_cmd() throws CivException {
		Resident resident = getResident();
		
		for (Town t : CivGlobal.getTowns()) {
			CivMessage.removeExtraTownChatListener(t, resident.getName());
		}
		
		for (Civilization civ : CivGlobal.getCivs()) {
			CivMessage.removeExtraCivChatListener(civ, resident.getName());
		}
		
		CivMessage.sendSuccess(sender, "Removed you from all chat channels.");
	}
	
	public void cclisten_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Please enter a civ name.");
		}
		
		Resident resident = getResident();
		
		Civilization civ = getNamedCiv(1);
		
		for (String str : CivMessage.getExtraCivChatListeners(civ)) {
			if (str.equalsIgnoreCase(resident.getName())) {
				CivMessage.removeExtraCivChatListener(civ, str);
				CivMessage.sendSuccess(sender, "No longer listening to civ "+civ.getName());
				return;
			}
		}
		
		CivMessage.addExtraCivChatListener(civ, resident.getName());
		CivMessage.sendSuccess(sender, "Listening to civ "+civ.getName());
	}
	
	public void tclisten_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Please enter a town name.");
		}
		
		Resident resident = getResident();
		
		Town town = getNamedTown(1);
		
		for (String str : CivMessage.getExtraTownChatListeners(town)) {
			if (str.equalsIgnoreCase(resident.getName())) {
				CivMessage.removeExtraTownChatListener(town, str);
				CivMessage.sendSuccess(sender, "No longer listening to town "+town.getName());
				return;
			}
		}
		
		CivMessage.addExtraTownChatListener(town, resident.getName());
		CivMessage.sendSuccess(sender, "Listening to town "+town.getName());
	}
	
	public void tc_cmd() throws CivException {
		Resident resident = getResident();
		if (args.length < 2) {
			resident.setTownChat(false);
			resident.setTownChatOverride(null);
			CivMessage.sendSuccess(sender, "Toggled tc off.");
			return;
		}
		
		Town town = getNamedTown(1);
		
		resident.setTownChat(true);
		resident.setTownChatOverride(town);
		CivMessage.sendSuccess(sender, "Now chatting in town chat:"+town.getName());
	}
	
	public void cc_cmd() throws CivException {
		Resident resident = getResident();
		if (args.length < 2) {
			resident.setCivChat(false);
			resident.setCivChatOverride(null);
			CivMessage.sendSuccess(sender, "Toggled cc off.");
			return;
		}
		
		Civilization civ = getNamedCiv(1);
		
		resident.setCivChat(true);
		resident.setCivChatOverride(civ);
		CivMessage.sendSuccess(sender, "Now chatting in civ chat:"+civ.getName());
	}
	
	public void banwordon_cmd() {
		CivGlobal.banWordsActive = true;
		CivMessage.sendSuccess(sender, "Activated banwords.");
	}
	
	public void banwordoff_cmd() {
		CivGlobal.banWordsActive = false;
		CivMessage.sendSuccess(sender, "Deactivated banwords.");
		
	}
	
	public void banwordadd_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter a word to ban");
		}
		
		CivGlobal.banWords.add(args[1]);
		CivMessage.sendSuccess(sender, "added "+args[1]);
	}
	
	public void banwordremove_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter a word to ban");
		}
		
		CivGlobal.banWords.remove(args[1]);
		CivMessage.sendSuccess(sender, "removed "+args[1]);
	}
	
	public void banwordtoggle() throws CivException {
		
		CivGlobal.banWordsAlways = !CivGlobal.banWordsAlways;
		CivMessage.sendSuccess(sender, "Ban always:"+CivGlobal.banWordsAlways);
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
