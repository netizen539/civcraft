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
package com.avrgaming.civcraft.threading.tasks;

import java.util.ArrayList;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.avrgaming.anticheat.ACManager;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.tutorial.CivTutorial;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.global.perks.PlatinumManager;

public class PlayerLoginAsyncTask implements Runnable {
	
	UUID playerUUID;
	public PlayerLoginAsyncTask(UUID playerUUID) {
		this.playerUUID = playerUUID;
	}
	
	public Player getPlayer() throws CivException {
		Player player = Bukkit.getPlayer(playerUUID);
		if (player == null) {
			throw new CivException("Player offline now. May have been kicked.");
		}
		
		return player;
	}
	
	@Override
	public void run() {
		try {
			CivLog.info("Running PlayerLoginAsyncTask for "+getPlayer().getName()+" UUID("+playerUUID+")");
			Resident resident = CivGlobal.getResident(getPlayer().getName());
			
			/* 
			 * Test to see if player has changed their name. If they have, these residents
			 * will not match. Disallow players changing their name without admin approval. 
			 */
			if (CivGlobal.getResidentViaUUID(getPlayer().getUniqueId()) != resident) {
				TaskMaster.syncTask(new PlayerKickBan(getPlayer().getName(), true, false, 
						"Your user ID on record does not match the player name you're attempting to log in with."+
						"If you changed your name, please change it back or contact an admin to request a name change."));
				return;
			}
	
			if (resident == null) {
				CivLog.info("No resident found. Creating for "+getPlayer().getName());
				try {
					resident = new Resident(getPlayer().getUniqueId(), getPlayer().getName());
				} catch (InvalidNameException e) {
					TaskMaster.syncTask(new PlayerKickBan(getPlayer().getName(), true, false, "You have an invalid name. Sorry."));
					return;
				}
				
				CivGlobal.addResident(resident);
				CivLog.info("Added resident:"+resident.getName());
				resident.setRegistered(System.currentTimeMillis());
				CivTutorial.showTutorialInventory(getPlayer());
				resident.setisProtected(true);
				int mins;
				try {
					mins = CivSettings.getInteger(CivSettings.civConfig, "global.pvp_timer");
				} catch (InvalidConfiguration e1) {
					e1.printStackTrace();
					return;
				}
				CivMessage.send(resident, CivColor.LightGray+"You have a PvP timer enabled for "+mins+" mins. You cannot attack or be attacked until it expires.");
				CivMessage.send(resident, CivColor.LightGray+"To remove it, type /resident pvptimer");
	
			} 
			
			/* 
			 * Resident is present. Lets check the UUID against the stored UUID.
			 * We are not going to allow residents to change names without admin permission.
			 * If someone logs in with a name that does not match the stored UUID, we'll kick them.
			 */
			if (resident.getUUID() == null) {
				/* This resident does not yet have a UUID stored. Free lunch. */
				resident.setUUID(getPlayer().getUniqueId());
				CivLog.info("Resident named:"+resident.getName()+" was acquired by UUID:"+resident.getUUIDString());
			} else if (!resident.getUUID().equals(getPlayer().getUniqueId())) {
				TaskMaster.syncTask(new PlayerKickBan(getPlayer().getName(), true, false, 
						"You're attempting to log in with a name already in use. Please change your name."));
				return;
			}
			
			if (!resident.isGivenKit()) {
				TaskMaster.syncTask(new GivePlayerStartingKit(resident.getName()));
			}
					
			if (War.isWarTime() && War.isOnlyWarriors()) {
				if (getPlayer().isOp() || getPlayer().hasPermission(CivSettings.MINI_ADMIN)) {
					//Allowed to connect since player is OP or mini admin.
				} else if (!resident.hasTown() || !resident.getTown().getCiv().getDiplomacyManager().isAtWar()) {
					TaskMaster.syncTask(new PlayerKickBan(getPlayer().getName(), true, false, "Only players in civilizations at war can connect right now. Sorry."));
					return;
				}
			}
			
			/* turn on allchat by default for admins and moderators. */
			if (getPlayer().hasPermission(CivSettings.MODERATOR) || getPlayer().hasPermission(CivSettings.MINI_ADMIN)) {
				resident.allchat = true;
				Resident.allchatters.add(resident.getName());
			}
	
			if (resident.getTreasury().inDebt()) {
				TaskMaster.asyncTask("", new PlayerDelayedDebtWarning(resident), 1000);
			}
			
			if (!getPlayer().isOp()) {
				CultureChunk cc = CivGlobal.getCultureChunk(new ChunkCoord(getPlayer().getLocation()));
				if (cc != null && cc.getCiv() != resident.getCiv()) {
					Relation.Status status = cc.getCiv().getDiplomacyManager().getRelationStatus(getPlayer());
					String color = PlayerChunkNotifyAsyncTask.getNotifyColor(cc, status, getPlayer());
					String relationName = status.name();
					
					if (War.isWarTime() && status.equals(Relation.Status.WAR)) {
						/* 
						 * Test for players who were not logged in when war time started.
						 * If they were not logged in, they are enemies, and are inside our borders
						 * they need to be teleported back to their own town hall.
						 */
						
						if (resident.getLastOnline() < War.getStart().getTime()) {
							resident.teleportHome();
							CivMessage.send(resident, CivColor.LightGray+"You've been teleported back to your home since you've logged into enemy during WarTime.");
						}
					}
					
					CivMessage.sendCiv(cc.getCiv(), color+getPlayer().getDisplayName()+"("+relationName+") has logged-in to our borders.");
				}
			}
					
			resident.setLastOnline(System.currentTimeMillis());
			resident.setLastIP(getPlayer().getAddress().getAddress().getHostAddress());
			resident.setSpyExposure(resident.getSpyExposure());
			resident.save();
			
			//TODO send town board messages?
			//TODO set default modes?
			resident.showWarnings(getPlayer());
			resident.loadPerks();
	
			try {
				if (CivSettings.getString(CivSettings.perkConfig, "system.free_perks").equalsIgnoreCase("true")) {
					resident.giveAllFreePerks();
				} else if (CivSettings.getString(CivSettings.perkConfig, "system.free_admin_perks").equalsIgnoreCase("true")) {
					if (getPlayer().hasPermission(CivSettings.MINI_ADMIN) || getPlayer().hasPermission(CivSettings.FREE_PERKS)) {
						resident.giveAllFreePerks();
					}
				}
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
			}
			
			
			/* Send Anti-Cheat challenge to player. */
			if (!getPlayer().hasPermission("civ.ac_valid")) {
				resident.setUsesAntiCheat(false);
				ACManager.sendChallenge(getPlayer());
			} else {
				resident.setUsesAntiCheat(true);
			}
	
			// Check for pending respawns.
			ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup("global:respawnPlayer");
			ArrayList<SessionEntry> deleted = new ArrayList<SessionEntry>();
		
			for (SessionEntry e : entries) {
				String[] split = e.value.split(":");
				
				BlockCoord coord = new BlockCoord(split[1]);
				getPlayer().teleport(coord.getLocation());
				deleted.add(e);
			}
			
			for (SessionEntry e : deleted) {
				CivGlobal.getSessionDB().delete(e.request_id, "global:respawnPlayer");
			}
			
			try {
				Player p = CivGlobal.getPlayer(resident);
				PlatinumManager.givePlatinumDaily(resident,
						CivSettings.platinumRewards.get("loginDaily").name, 
						CivSettings.platinumRewards.get("loginDaily").amount, 
						"Welcome back to CivCraft! Here is %d for logging in today!" );			
		
				
				ArrayList<SessionEntry> deathEvents = CivGlobal.getSessionDB().lookup("pvplogger:death:"+resident.getName());
				if (deathEvents.size() != 0) {
					CivMessage.send(resident, CivColor.Rose+CivColor.BOLD+"You were killed while offline because you logged out while in PvP!");
					class SyncTask implements Runnable {
						String playerName; 
						
						public SyncTask(String playerName) {
							this.playerName = playerName;
						}
						
						@Override
						public void run() {
							Player p;
							try {
								p = CivGlobal.getPlayer(playerName);
								p.setHealth(0);
								CivGlobal.getSessionDB().delete_all("pvplogger:death:"+p.getName());
							} catch (CivException e) {
								// You cant excape death that easily!
							}
						}
					}
					
					TaskMaster.syncTask(new SyncTask(p.getName()));
				}	
			} catch (CivException e1) {
				//try really hard not to give offline players who were kicked platinum.
			}
			
			if (EndConditionDiplomacy.canPeopleVote()) {
				CivMessage.send(resident, CivColor.LightGreen+"The Council of Eight is built! Use /vote to vote for your favorite Civilization!");
			}
		} catch (CivException playerNotFound) {
			// Player logged out while async task was running.
			CivLog.warning("Couldn't complete PlayerLoginAsyncTask. Player may have been kicked while async task was running.");
		}
	}
	


}
