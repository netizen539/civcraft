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
package com.avrgaming.civcraft.command.plot;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionNode;
import com.avrgaming.civcraft.util.CivColor;

public class PlotPermCommand extends CommandBase {

	@Override
	public void init() {
		command = "/plot perm";
		displayName = "Plot Perm";
		
		commands.put("set", "Sets a permission flag on or off.");
	}
	
	public void set_cmd() throws CivException {
		Player player = (Player)sender;
		
		TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
		if (tc == null) {
			throw new CivException("Plot is not part of a town.");
		}
		
		if (args.length < 4) {
			showPermCmdHelp();
			throw new CivException("Incorrect number of arguments");	
		}
		
		PermissionNode node = null;
		switch(args[1].toLowerCase()) {
		case "build":
			node = tc.perms.build;
			break;
		case "destroy":
			node = tc.perms.destroy;
			break;
		case "interact":
			node = tc.perms.interact;
			break;
		case "itemuse":
			node = tc.perms.itemUse;
			break;
		case "reset":
			//TODO implement permissions reset.
			break;
		default:
			showPermCmdHelp();
			throw new CivException("Incorrect Command Arguments.");
		}
		
		if (node == null) {
			throw new CivException("Internal error, unknown permission node.");
		}
		
		boolean on;
		if (args[3].equalsIgnoreCase("on") || args[3].equalsIgnoreCase("yes") || args[3].equalsIgnoreCase("1")) {
			on = true;
		} else if (args[3].equalsIgnoreCase("off") || args[3].equalsIgnoreCase("no") || args[3].equalsIgnoreCase("0")) {
			on = false;
		} else {
			showPermCmdHelp();
			throw new CivException("Incorrect Command Arguments.");
		}
		
		switch(args[2].toLowerCase()) {
		case "owner":
			node.setPermitOwner(on);
			break;
		case "group":
			node.setPermitGroup(on);
			break;
		case "others":
			node.setPermitOthers(on);
		}
		
		tc.save();
		
		CivMessage.sendSuccess(sender, "Permission "+node.getType()+" changed to "+on+" for "+args[2]);
	}
	
	private void showPermCmdHelp() {
		CivMessage.send(sender, CivColor.LightGray+"/plot perm set <type> <groupType> [on|off] ");
		CivMessage.send(sender, CivColor.LightGray+"    types: [build|destroy|interact|itemuse|reset]");
		CivMessage.send(sender, CivColor.LightGray+"    groupType: [owner|group|others]");
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
		if (args.length != 0) {
			validPlotOwner();
		}
			
		return;
	}

}
