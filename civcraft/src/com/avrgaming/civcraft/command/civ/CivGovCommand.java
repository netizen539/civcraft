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
package com.avrgaming.civcraft.command.civ;

import java.util.ArrayList;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.util.CivColor;

public class CivGovCommand extends CommandBase {

	@Override
	public void init() {
		command = "/civ gov";
		displayName = "Civ Gov";
		
		commands.put("info", "Information about your current government.");
		commands.put("change", "[name] - change your government to the named government.");
		commands.put("list", "lists available governments to change to.");
	}
	
	public void change_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		if (args.length < 2) {
			throw new CivException("You must enter the name of a government you want to change to.");
		}
		
		ConfigGovernment gov = ConfigGovernment.getGovernmentFromName(args[1]);
		if (gov == null) {
			throw new CivException("Could not find government named "+args[1]);
		}
		
		if (!gov.isAvailable(civ)) {
			throw new CivException(gov.displayName+" is not yet available.");
		}
		
		civ.changeGovernment(civ, gov, false);
		CivMessage.sendSuccess(sender, "Revolution Successful.");
	}
	
	public void list_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		CivMessage.sendHeading(sender, "Available Governments");
		ArrayList<ConfigGovernment> govs = ConfigGovernment.getAvailableGovernments(civ);
		
		for (ConfigGovernment gov : govs) {
			if (gov == civ.getGovernment()) {
				CivMessage.send(sender, CivColor.Gold+gov.displayName+" (current)");
			} else {
				CivMessage.send(sender, CivColor.Green+gov.displayName);
			}
		}
		
	}
	
	public void info_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		CivMessage.sendHeading(sender, "Government "+civ.getGovernment().displayName);
		CivMessage.send(sender, CivColor.Green+"Trade Rate: "+CivColor.LightGreen+civ.getGovernment().trade_rate+
				CivColor.Green+" Cottage Rate: "+CivColor.LightGreen+civ.getGovernment().cottage_rate);
		CivMessage.send(sender, CivColor.Green+"Upkeep Rate: "+CivColor.LightGreen+civ.getGovernment().upkeep_rate+
				CivColor.Green+" Growth Rate: "+CivColor.LightGreen+civ.getGovernment().growth_rate);
		CivMessage.send(sender, CivColor.Green+"Hammer Rate: "+CivColor.LightGreen+civ.getGovernment().hammer_rate+
				CivColor.Green+" Beaker Rate: "+CivColor.LightGreen+civ.getGovernment().beaker_rate);
		CivMessage.send(sender, CivColor.Green+"Culture Rate: "+CivColor.LightGreen+civ.getGovernment().culture_rate+
				CivColor.Green+" Max Tax Rate: "+CivColor.LightGreen+civ.getGovernment().maximum_tax_rate);
				
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
		validLeaderAdvisor();		
	}

}
