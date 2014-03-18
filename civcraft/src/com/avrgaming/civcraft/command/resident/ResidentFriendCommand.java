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
package com.avrgaming.civcraft.command.resident;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class ResidentFriendCommand extends CommandBase {

	@Override
	public void init() {
		command = "/resident friend";
		displayName = "Resident Friend";
		
		commands.put("add", "[name] - adds this resident to your friends list.");
		commands.put("remove", "[name] - removes this resident from your friends list.");
		commands.put("list", "shows a list of all your current friends.");
	}
	
	public void add_cmd() throws CivException {
		Resident resident = getResident();
		
		if (args.length < 2) {
			throw new CivException("Please specify the name of the friend you want to add.");
		}
		
		Resident friendToAdd = getNamedResident(1);
		
		resident.addFriend(friendToAdd);
		CivMessage.sendSuccess(sender, "Added "+args[1]+" as a friend.");	
		resident.save();
	}
	
	public void remove_cmd() throws CivException {
	Resident resident = getResident();
		
		if (args.length < 2) {
			throw new CivException("Please specify the name of the friend you want to removed.");
		}
		
		Resident friendToRemove = getNamedResident(1);
		
		resident.removeFriend(friendToRemove);
		CivMessage.sendSuccess(sender, "Removed "+args[1]+" as a friend.");	
		resident.save();
	}
	
	public void list_cmd() throws CivException {
		Resident resident = getResident();
		CivMessage.sendHeading(sender, resident.getName()+" friend list");
		
		String out = "";
		for (String res : resident.getFriends()) {
			out += res+ ", ";
		}
		CivMessage.send(sender, out);
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
