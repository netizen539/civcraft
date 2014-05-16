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
package com.avrgaming.civcraft.command.camp;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCampUpgrade;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

public class CampUpgradeCommand extends CommandBase {
	@Override
	public void init() {
		command = "/camp upgrade";
		displayName = "Camp Upgrade";
		
		
		commands.put("list", "shows available upgrades to purchase.");
		commands.put("purchased", "shows a list of purchased upgrades.");
		commands.put("buy", "[name] - buys upgrade for this camp.");
		
	}

	public void purchased_cmd() throws CivException {
		Camp camp = this.getCurrentCamp();
		CivMessage.sendHeading(sender, "Upgrades Purchased");

		String out = "";
		for (ConfigCampUpgrade upgrade : camp.getUpgrades()) {
			out += upgrade.name+", ";
		}
		
		CivMessage.send(sender, out);
	}
	
	private void list_upgrades(Camp camp) throws CivException {				
		for (ConfigCampUpgrade upgrade : CivSettings.campUpgrades.values()) {
			if (upgrade.isAvailable(camp)) {
				CivMessage.send(sender, upgrade.name+CivColor.LightGray+" Cost: "+CivColor.Yellow+upgrade.cost);
			}
		}
	}
	
	public void list_cmd() throws CivException {
		Camp camp = this.getCurrentCamp();

		CivMessage.sendHeading(sender, "Available Upgrades");	
		list_upgrades(camp);		
	}
	
	public void buy_cmd() throws CivException {
		Camp camp = this.getCurrentCamp();

		if (args.length < 2) {
			CivMessage.sendHeading(sender, "Available Upgrades");
			list_upgrades(camp);		
			CivMessage.send(sender, "Enter the name of the upgrade you wish to purchase.");
			return;
		}
				
		String combinedArgs = "";
		args = this.stripArgs(args, 1);
		for (String arg : args) {
			combinedArgs += arg + " ";
		}
		combinedArgs = combinedArgs.trim();
		
		ConfigCampUpgrade upgrade = CivSettings.getCampUpgradeByNameRegex(camp, combinedArgs);
		if (upgrade == null) {
			throw new CivException("No upgrade by the name of "+combinedArgs+" could be found.");
		}
		
		if (camp.hasUpgrade(upgrade.id)) {
			throw new CivException("You already have that upgrade.");
		}
		
		camp.purchaseUpgrade(upgrade);
		CivMessage.sendSuccess(sender, "Upgrade \""+upgrade.name+"\" purchased.");
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
		this.validCampOwner();
	}
}
