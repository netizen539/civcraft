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

import java.util.ArrayList;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Library;
import com.avrgaming.civcraft.structure.Store;

public class TownResetCommand extends CommandBase {

	@Override
	public void init() {
		command = "/town reset";
		displayName = "Town Reset";
		
		commands.put("library", "Removes all town library enchantment upgrades.");
		commands.put("store", "Removes all town store material upgrades.");
	}

	public void library_cmd() throws CivException {
		Town town = getSelectedTown();
		
		Library library = (Library) town.findStructureByConfigId("s_library");
		if (library == null) {
			throw new CivException("Your town doesn't have a library.");
		}
		
		ArrayList<ConfigTownUpgrade> removeUs = new ArrayList<ConfigTownUpgrade>();
		for(ConfigTownUpgrade upgrade : town.getUpgrades().values()) {
			if (upgrade.action.contains("enable_library_enchantment")) {
				removeUs.add(upgrade);
			}
		}
		
		for (ConfigTownUpgrade upgrade : removeUs) {
			town.removeUpgrade(upgrade);
		}
		
		library.reset();
		
		town.save();
		CivMessage.sendSuccess(sender, "Library enchantment upgrades reset!");
	}
	
	public void store_cmd() throws CivException {
		Town town = getSelectedTown();
		
		Store store = (Store) town.findStructureByConfigId("s_store");
		if (store == null) {
			throw new CivException("Your town doesn't have a library.");
		}
		
		ArrayList<ConfigTownUpgrade> removeUs = new ArrayList<ConfigTownUpgrade>();
		for(ConfigTownUpgrade upgrade : town.getUpgrades().values()) {
			if (upgrade.action.contains("set_store_material")) {
				removeUs.add(upgrade);
			}
		}
		
		for (ConfigTownUpgrade upgrade : removeUs) {
			town.removeUpgrade(upgrade);
		}
		
		store.reset();
		
		town.save();
		CivMessage.sendSuccess(sender, "Store material upgrades reset!");
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		this.showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		this.validMayorAssistantLeader();
	}

}
