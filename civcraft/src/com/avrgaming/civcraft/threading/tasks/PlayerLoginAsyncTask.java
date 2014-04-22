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

	volatile Player player;
	
	public PlayerLoginAsyncTask(Player player) {
		this.player = player;
	}
	
	@Override
	public void run() {
		if (player == null) {
			return;
		}
		
		CivLog.info("Running PlayerLoginAsyncTask for "+player.getName());
		Resident resident = CivGlobal.getResident(player);
		
		if (resident == null) {
			CivLog.info("No resident found. Creating for "+player.getName());
			try {
				resident = new Resident(player.getName());
			} catch (InvalidNameException e) {
				TaskMaster.syncTask(new PlayerKickBan(player.getName(), true, false, "You have an invalid name. Sorry."));
				return;
			}
			
			CivGlobal.addResident(resident);
			CivLog.info("Added resident:"+resident.getName());
			resident.setRegistered(System.currentTimeMillis());
			CivTutorial.showTutorialInventory(player);
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
		
		if (!resident.isGivenKit()) {
			TaskMaster.syncTask(new GivePlayerStartingKit(resident.getName()));
		}
		
		if (resident.isBanned()) {
			TaskMaster.syncTask(new PlayerKickBan(player.getName(), true, false, resident.getBannedMessage()));
			return;
		}
		
		if (War.isWarTime() && War.isOnlyWarriors()) {
			if (player.isOp() || player.hasPermission(CivSettings.MINI_ADMIN)) {
				//Allowed to connect since player is OP or mini admin.
			} else if (!resident.hasTown() || !resident.getTown().getCiv().getDiplomacyManager().isAtWar()) {
				TaskMaster.syncTask(new PlayerKickBan(player.getName(), true, false, "Only players in civilizations at war can connect right now. Sorry."));
				return;
			}
		}
		
		/* turn on allchat by default for admins and moderators. */
		if (player.hasPermission(CivSettings.MODERATOR) || player.hasPermission(CivSettings.MINI_ADMIN)) {
			resident.allchat = true;
			Resident.allchatters.add(resident.getName());
		}

		if (resident.getTreasury().inDebt()) {
			TaskMaster.asyncTask("", new PlayerDelayedDebtWarning(resident), 1000);
		}
		
		if (!player.isOp()) {
			CultureChunk cc = CivGlobal.getCultureChunk(new ChunkCoord(player.getLocation()));
			if (cc != null && cc.getCiv() != resident.getCiv()) {
				Relation.Status status = cc.getCiv().getDiplomacyManager().getRelationStatus(player);
				String color = PlayerChunkNotifyAsyncTask.getNotifyColor(cc, status, player);
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
				
				CivMessage.sendCiv(cc.getCiv(), color+player.getDisplayName()+"("+relationName+") has logged-in to our borders.");
			}
		}
		
		if (player == null) {
			return;
		}
		
		resident.setLastOnline(System.currentTimeMillis());
		resident.setLastIP(player.getAddress().getAddress().getHostAddress());
		resident.setSpyExposure(resident.getSpyExposure());
		resident.save();
		
		//TODO send town board messages?
		//TODO set default modes?
		resident.showWarnings(player);
		resident.loadPerks();

		/* Send Anti-Cheat challenge to player. */
		resident.setUsesAntiCheat(false);
		ACManager.sendChallenge(player);

		// Check for pending respawns.
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup("global:respawnPlayer");
		ArrayList<SessionEntry> deleted = new ArrayList<SessionEntry>();
	
		for (SessionEntry e : entries) {
			String[] split = e.value.split(":");
			
			BlockCoord coord = new BlockCoord(split[1]);
			player.teleport(coord.getLocation());
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

		
	}
	


}
