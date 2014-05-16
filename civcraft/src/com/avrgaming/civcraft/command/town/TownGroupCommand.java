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

import java.sql.SQLException;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.CivColor;

public class TownGroupCommand extends CommandBase {

	@Override
	public void init() {
		command = "/town group";
		displayName = "Town Group";
		
		commands.put("new", "[name] creates a new group.");
		commands.put("delete", "Deletes an empty group.");
		commands.put("remove", "[resident] [group] - removes [resident] from group [group]");
		commands.put("add", "[resident] [group] - adds [resident] to group [group]");
		commands.put("info", "Shows town group information");
	}
	
	public void delete_cmd() throws CivException {
		Town town = getSelectedTown();
		PermissionGroup grp = this.getNamedPermissionGroup(town, 1);
		
		try {			
			if (grp.getMemberCount() > 0) {
				throw new CivException("Group must have no members before being deleted.");
			}
			
			if (town.isProtectedGroup(grp)) {
				throw new CivException("Cannot delete a protected group.");
			}
			
			town.removeGroup(grp);
			town.save();
			grp.delete();
			
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException("Internal DB Error.");
		}

		CivMessage.sendSuccess(sender, "Deleted group "+args[1]);
	}
	
	public void new_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("You must specify a group name.");
		}
		
		Town town = getSelectedTown();	
		if (town.hasGroupNamed(args[1])) {
			throw new CivException("Town already has a group named "+args[1]);
		}
		
		if (PermissionGroup.isProtectedGroupName(args[1])) {
			throw new CivException("Cannot use this group name, it is a protected group.");
		}
		
		try {
			PermissionGroup grp = new PermissionGroup(town, args[1]);
			
			grp.save();
			town.addGroup(grp);
			town.save();
			
		} catch (InvalidNameException e) {
			throw new CivException("Invalid name, please choose another.");
		}

		CivMessage.sendSuccess(sender, "Created group "+args[1]);
	}
	
	public void remove_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResidnet = getResident();
		Resident oldMember = getNamedResident(1);
		PermissionGroup grp = getNamedPermissionGroup(town, 2);
				
		if (grp == town.getMayorGroup()) {
			if(!grp.hasMember(commandSenderResidnet)) {
				throw new CivException("Only Mayors can remove members to the mayors group.");
			} 
			
			if (grp.getMemberCount() == 1) {
				throw new CivException("There must be at least one member in the mayors group.");
			}
		}
		
		grp.removeMember(oldMember);
		grp.save();
		
		CivMessage.sendSuccess(sender, "Removed "+oldMember.getName()+" from group "+grp.getName()+" in town "+town.getName());
		
		try {
			Player newPlayer = CivGlobal.getPlayer(oldMember);
			CivMessage.send(newPlayer, CivColor.Rose+"You were removed from the "+grp.getName()+" group in town "+grp.getTown().getName());
		} catch (CivException e) {
			/* player not online. forget the exception*/
		}
	}
	
	public void add_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident commandSenderResident = getResident();
		Resident newMember = getNamedResident(1);
		PermissionGroup grp = this.getNamedPermissionGroup(town, 2);
								
		if (grp == town.getMayorGroup() && !grp.hasMember(commandSenderResident)) {
			
			PermissionGroup leaderGrp = town.getCiv().getLeaderGroup();
			if (leaderGrp == null) {
				throw new CivException("ERROR: Couldn't find leader group for civ "+town.getCiv()+" contact an admin.");
			}
			
			if (!leaderGrp.hasMember(commandSenderResident)) {
				throw new CivException("Only Mayors and civ Leaders can add members to the mayors group.");
			}
		}
		
		if (grp.isProtectedGroup() && !newMember.hasTown()) {
			throw new CivException(newMember.getName()+" is not a member of a town/civ so cannot be added to a protected group.");
		}
		
		if (grp.isTownProtectedGroup() && newMember.getTown() != grp.getTown()) {
			throw new CivException(newMember.getName()+" belongs to town "+newMember.getTown().getName()+
					" and cannot be added to a protected group in town "+grp.getTown().getName());
		}
		
		if (grp.isCivProtectedGroup() && newMember.getCiv() != grp.getCiv()) {
			throw new CivException(newMember.getName()+" belongs to civ "+newMember.getCiv().getName()+
					" and cannot be added to a protected group in civ "+grp.getCiv().getName());
		}
		
		grp.addMember(newMember);
		grp.save();
		
		CivMessage.sendSuccess(sender, "Added "+newMember.getName()+" to group "+grp.getName()+" in town "+town.getName());

		try {
			Player newPlayer = CivGlobal.getPlayer(newMember);
			CivMessage.sendSuccess(newPlayer, "You were added to the "+grp.getName()+" group in town "+grp.getTown().getName());
		} catch (CivException e) {
			/* player not online. forget the exception*/
		}
	}
	
	public void info_cmd() throws CivException {
		Town town = getSelectedTown();
		
		if (args.length >= 2) {
			PermissionGroup grp = town.getGroupByName(args[1]);
			if (grp == null) {
				throw new CivException("No group named "+args[1]+" in "+town.getName());
			}
			
			CivMessage.sendHeading(sender, "Group("+town.getName()+"):"+args[1]);
			
			String residents = "";
			for (Resident res : grp.getMemberList()) {
				residents += res.getName() + " ";
			}
			CivMessage.send(sender, residents);
			
		} else {
			CivMessage.sendHeading(sender, town.getName()+" Group Information");

			for (PermissionGroup grp : town.getGroups()) {
				CivMessage.send(sender, grp.getName()+CivColor.LightGray+" ("+grp.getMemberCount()+" members)");
			}		
		}
	}
	
	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		this.validMayorAssistantLeader();
		return;
	}

	@Override
	public void doDefaultAction() {
		showHelp();
	}

}
