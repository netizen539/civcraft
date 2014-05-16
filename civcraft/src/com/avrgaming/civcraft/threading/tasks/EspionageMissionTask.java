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

import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.cache.PlayerLocationCache;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.units.Unit;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.ScoutTower;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class EspionageMissionTask implements Runnable {

	ConfigMission mission;
	String playerName;
	Town target;
	int secondsLeft;
	Location startLocation;
	
	
	public EspionageMissionTask (ConfigMission mission, String playerName, Location startLocation, Town target, int seconds) {
		this.mission = mission;
		this.playerName = playerName;
		this.target = target;
		this.startLocation = startLocation;
		this.secondsLeft = seconds;
	}
	
	@Override
	public void run() {
		int exposePerSecond;
		int exposePerPlayer;
		int exposePerScout;
		try {
			exposePerSecond = CivSettings.getInteger(CivSettings.espionageConfig, "espionage.exposure_per_second");
			exposePerPlayer = CivSettings.getInteger(CivSettings.espionageConfig, "espionage.exposure_per_player");
			exposePerScout = CivSettings.getInteger(CivSettings.espionageConfig, "espionage.exposure_per_scout");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}	
		
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		Resident resident = CivGlobal.getResident(player);	
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+"Mission Started.");
			
		while (secondsLeft > 0) {
	
			if (secondsLeft > 0) {
				secondsLeft--;
				
				/* Add base exposure. */
				resident.setPerformingMission(true);
				resident.setSpyExposure(resident.getSpyExposure() + exposePerSecond);
				
				/* Add players nearby exposure */
				//PlayerLocationCache.lock.lock();
				try {
					int playerCount = PlayerLocationCache.getNearbyPlayers(new BlockCoord(player.getLocation()), 600).size();
					playerCount--;
					resident.setSpyExposure(resident.getSpyExposure() + (playerCount*exposePerPlayer));
				} finally {
				//	PlayerLocationCache.lock.unlock();
				}
				
				/* Add scout tower exposure */
				int amount = 0;
				double range;
				try {
					range = CivSettings.getDouble(CivSettings.warConfig, "scout_tower.range");
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
					resident.setPerformingMission(false);
					return;
				}
				
				BlockCoord bcoord = new BlockCoord(player.getLocation());
								
				for (Structure struct : target.getStructures()) {
					if (!struct.isActive()) {
						continue;
					}
					
					if (struct instanceof ScoutTower) {
						if (bcoord.distance(struct.getCenterLocation()) < range) {
							amount += exposePerScout;							
						}
					}
				}
				resident.setSpyExposure(resident.getSpyExposure() + amount);
				
				/* Process exposure penalities */
				if (target.processSpyExposure(resident)) {
					CivMessage.global(CivColor.Yellow+"INTERNATIONAL INCIDENT!"+CivColor.White+" "+
							player.getName()+" was caught trying to perform a "+mission.name+" spy mission in "+
							target.getName()+"!");
					CivMessage.send(player, CivColor.Rose+"You've been compromised! (Exposure got too high) Spy unit was destroyed!");
					Unit.removeUnit(player);
					resident.setPerformingMission(false);
					return;
				}
				
				if ((secondsLeft % 15) == 0) {
					CivMessage.send(player, CivColor.Yellow+CivColor.BOLD+""+secondsLeft+" seconds remain");
				} else if (secondsLeft < 15) {
					CivMessage.send(player, CivColor.Yellow+CivColor.BOLD+""+secondsLeft+" seconds remain");
				}
				
			}
			
			ChunkCoord coord = new ChunkCoord(player.getLocation());
			CultureChunk cc = CivGlobal.getCultureChunk(coord);
			
			if (cc == null || cc.getCiv() != target.getCiv()) {
				CivMessage.sendError(player, "You've left the civ borders. Mission Failed.");
				return;
			}
			
			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
				return;
			}
		}
		
		resident.setPerformingMission(false);
		TaskMaster.syncTask(new PerformMissionTask(mission, playerName));
	}

}
