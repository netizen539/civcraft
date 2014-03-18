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
import com.avrgaming.civcraft.threading.sync.SyncBuildUpdateTask;

public class AdminLagCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad lag";
		displayName = "Admin Lag";
		
		commands.put("trommels", "Toggles trommels globally.");
		commands.put("towers", "Toggles towers globally.");
		commands.put("growth", "Toggles farm growth.");
		commands.put("trade", "Toggles farm growth.");
		commands.put("score", "Toggles score calculations");
		commands.put("warning", "Toggles warnings in the logs.");
		commands.put("blockupdate", "[#] - sets the block update limit to this amount.");
		
	}
	
	public void blockupdate_cmd() throws CivException {
		Integer blocks = this.getNamedInteger(1);
		
		SyncBuildUpdateTask.UPDATE_LIMIT = blocks;
		CivMessage.sendSuccess(sender, "Set block update limit to "+blocks);
	}
	
	public void score_cmd() {
		CivGlobal.scoringEnabled = !CivGlobal.scoringEnabled;
		
		if (CivGlobal.scoringEnabled) {
			CivMessage.sendSuccess(sender, "Scoring enabled.");
		} else {
			CivMessage.sendError(sender, "Scoring disabled");
		}
	}

	public void trommels_cmd() {
		CivGlobal.trommelsEnabled = !CivGlobal.trommelsEnabled;
		
		if (CivGlobal.trommelsEnabled) {
			CivMessage.sendSuccess(sender, "Trommels enabled.");
		} else {
			CivMessage.sendError(sender, "Trommels disabled");
		}
	}
	
	public void towers_cmd() {
		CivGlobal.towersEnabled = !CivGlobal.towersEnabled;
		
		if (CivGlobal.towersEnabled) {
			CivMessage.sendSuccess(sender, "Towers enabled.");
		} else {
			CivMessage.sendError(sender, "Towers disabled");
		}
	}
	
	public void growth_cmd() {
		CivGlobal.growthEnabled = !CivGlobal.growthEnabled;
		
		if (CivGlobal.growthEnabled) {
			CivMessage.sendSuccess(sender, "Growth enabled.");
		} else {
			CivMessage.sendError(sender, "Growth disabled");
		}
	}
	
	public void trade_cmd() {
		CivGlobal.tradeEnabled = !CivGlobal.tradeEnabled;
		
		if (CivGlobal.tradeEnabled) {
			CivMessage.sendSuccess(sender, "Trade enabled.");
		} else {
			CivMessage.sendError(sender, "Trade disabled");
		}
	}
	
	public void warning_cmd() {
		CivGlobal.growthEnabled = !CivGlobal.growthEnabled;
		
		if (CivGlobal.warningsEnabled) {
			CivMessage.sendSuccess(sender, "Warnings enabled.");
		} else {
			CivMessage.sendError(sender, "Warnings disabled");
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
		//Admin is checked in parent command.
	}

}
