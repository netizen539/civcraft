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

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.recover.RecoverStructuresAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;

public class AdminRecoverCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad recover";
		displayName = "Admin recover";
		
		commands.put("structures", "Finds and recovers all of the 'broken' structures.");
		commands.put("listbroken", "Lists all broken structures and their locations.");
		
		commands.put("listorphantowns", "Lists all of the currently orphaned towns.");
		commands.put("listorphancivs", "Lists all of the currently orphaned civs.");
		
		commands.put("listorphanleaders", "Lists all orphaned leaders.");
		commands.put("fixleaders", "Looks up leaders of civilizations and sets them back in town.");
		
		commands.put("listorphanmayors", "List all leaders who are not mayors of the capitol.");
		commands.put("fixmayors", "Makes all leaders of civs mayors in the capitol town.");
		
		commands.put("forcesaveresidents", "force saves all residents");
		commands.put("forcesavetowns", "force saves all towns");
		commands.put("forcesavecivs", "force saves all civs");
		
		commands.put("listdefunctcivs", "list all towns with no leader group.");
		commands.put("killdefunctcivs", "attempts to delete defunct civs.");
		
		commands.put("listdefuncttowns", "list all towns with no mayors group");
		commands.put("killdefuncttowns", "attempts to delete defunct towns.");
		
		commands.put("listnocaptials", "list all civs with no capitols");
		commands.put("cleannocapitols", "clean out all civs with no capitols.");
		
		commands.put("fixtownresidents", "Restores all residents to the towns listed in their debug_town field.");

	}
	
	public void fixtownresidents_cmd() {
		for (Resident resident : CivGlobal.getResidents()) {
			if (resident.debugTown != null && !resident.debugTown.equals("")) {
				Town town = CivGlobal.getTown(resident.debugTown);
				if (town == null) {
					CivLog.error("Couldn't find town:"+resident.debugTown+" for resident:"+resident.getName()+" is this town deleted?");
					continue;
				}
				
				resident.setTown(town);
				try {
					resident.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	public void listnocapitols_cmd() {
		CivMessage.sendHeading(sender, "Defunct Civs");
		for (Civilization civ : CivGlobal.getCivs()) {
			
			Town town = CivGlobal.getTown(civ.getCapitolName());
			if (town == null) {
				CivMessage.send(sender, civ.getName());
			}
		}
	}
	
	public void cleannocapitols_cmd() {
		for (Civilization civ : CivGlobal.getCivs()) {
			
			Town town = CivGlobal.getTown(civ.getCapitolName());
			if (town == null) {
				CivMessage.send(sender, "Deleting "+civ.getName());
				try {
					civ.delete();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void listdefunctcivs_cmd() {
		CivMessage.sendHeading(sender, "Defunct Civs");
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.getLeaderGroup() == null) {
				CivMessage.send(sender, civ.getName());
			}
		}
	}
	
	public void killdefunctcivs_cmd() {
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.getLeaderGroup() == null) {
				CivMessage.send(sender, "Deleteing "+civ.getName());
				try {
					civ.delete();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public void listdefuncttowns_cmd() {
		CivMessage.sendHeading(sender, "Defunct Towns");
		for (Town town : CivGlobal.getTowns()) {
			if (town.getMayorGroup() == null) {
				CivMessage.send(sender, town.getName());
			}
		}
	}
	
	public void killdefuncttowns_cmd() {
		for (Town town : CivGlobal.getTowns()) {
			if (town.getMayorGroup() == null) {
				CivMessage.send(sender, "Deleting "+town.getName());
				try {
					town.delete();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
			}
		}
	}
	
	public void forcesaveresidents_cmd() throws SQLException {
		for (Resident resident : CivGlobal.getResidents()) {
			resident.saveNow();
		}
		CivMessage.sendSuccess(sender, "Saved "+CivGlobal.getResidents().size()+" residents");
	}
	
	public void forcesavetowns_cmd() throws SQLException {
		for (Town town : CivGlobal.getTowns()) {
			town.saveNow();
		}
		CivMessage.sendSuccess(sender, "Saved "+CivGlobal.getTowns().size()+" towns");
	}
	
	public void forcesavecivs_cmd() throws SQLException {
		for (Civilization civ : CivGlobal.getCivs()) {
			civ.saveNow();
		}
		CivMessage.sendSuccess(sender, "Saved "+CivGlobal.getCivs().size()+" civs");
	}
	
	public void listorphanmayors_cmd() {
		for (Civilization civ : CivGlobal.getCivs()) {
			Town capitol = civ.getTown(civ.getCapitolName());
			if (capitol == null) {
				continue;
			}
			
			Resident leader = civ.getLeader();
			if (leader == null) {
				continue;
			}
			
			CivMessage.send(sender, "Broken: "+leader.getName()+" in civ: "+civ.getName()+" in capitol:"+capitol.getName());
			
		}
		
		CivMessage.sendSuccess(sender, "Finished");
	}
	
	public void fixmayors_cmd() {
		
		for (Civilization civ : CivGlobal.getCivs()) {
			Town capitol = civ.getTown(civ.getCapitolName());
			if (capitol == null) {
				continue;
			}
			
			Resident leader = civ.getLeader();
			if (leader == null) {
				continue;
			}
			
			if (capitol.getMayorGroup() == null) {
				CivMessage.send(sender, "Town:"+capitol.getName()+" doesnt have a mayors group??");
				continue;
			}
			
			capitol.getMayorGroup().addMember(leader);
			try {
				capitol.getMayorGroup().saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			CivMessage.send(sender, "Fixed "+leader.getName()+" in civ: "+civ.getName()+" in capitol:"+capitol.getName());
			
		}
		
		CivMessage.sendSuccess(sender, "Finished");
		
	}

	public void fixleaders_cmd() {
		
		for (Civilization civ : CivGlobal.getCivs()) {
			Resident res = civ.getLeader();
			if (res == null) {
				continue;
			}
			
			if (!res.hasTown()) {
				Town capitol = civ.getTown(civ.getCapitolName());
				if (capitol == null) {
					CivMessage.send(sender, "-- no capitol for civ "+civ.getName());
					continue;
				}
				res.setTown(capitol);
				try {
					res.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
				CivMessage.send(sender, "Fixed Civ:"+civ.getName()+" leader:"+res.getName());
			}
			
			if (!civ.getLeaderGroup().hasMember(res)) {
				civ.getLeaderGroup().addMember(res);
				try {
					civ.getLeaderGroup().saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
		}
	}
	
	public void listorphanleaders_cmd() {
		CivMessage.sendHeading(sender, "Orphan Leaders");
		
		for (Civilization civ : CivGlobal.getCivs()) {
			Resident res = civ.getLeader();
			if (res == null) {
				continue;
			}
			
			if (!res.hasTown()) {
				Town capitol = civ.getTown(civ.getCapitolName());
				if (capitol == null) {
					CivMessage.send(sender, "-- no capitol for civ "+civ.getName());
					continue;
				}
				
				CivMessage.send(sender, "Broken Civ:"+civ.getName()+" Leader:"+res.getName());
			}
			
		}
		
	}
	
	public void listorphantowns_cmd() {
		CivMessage.sendHeading(sender, "Orphan Towns");
		
		for (Town town : CivGlobal.orphanTowns) {
			CivMessage.send(sender, town.getName());
		}
	}
	
	public void listorphancivs_cmd() {
		CivMessage.sendHeading(sender, "Orphan Civs");
		
		for (Civilization civ : CivGlobal.orphanCivs) {
			CivMessage.send(sender, civ.getName()+ " capitol:"+civ.getCapitolName());
		}
		
	}
	
	public void listbroken_cmd() {
		CivMessage.send(sender, "Starting List Broken Task");
		TaskMaster.syncTask(new RecoverStructuresAsyncTask(sender, true), 0);
	}
	
	public void structures_cmd() {
		CivMessage.send(sender, "Starting Recover Task");
		TaskMaster.syncTask(new RecoverStructuresAsyncTask(sender, false), 0);
		
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
		//Permissions checked in /ad command above.
	}

	
	
}
