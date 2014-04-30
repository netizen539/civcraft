package com.avrgaming.civcraft.arena;

import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerRespawnEvent;
import org.bukkit.inventory.Inventory;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.DateUtil;
import com.avrgaming.civcraft.util.TimeTools;

public class ArenaListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockIgniteEvent(BlockIgniteEvent event) {
		if (ArenaManager.activeArenas.containsKey(event.getBlock().getWorld().getName())) {
			event.setCancelled(true);
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (resident == null) {
			return;
		}
		
		if (resident.isInsideArena()) {
			if (resident.getCurrentArena() != null) {
				
				CivMessage.sendArena(resident.getCurrentArena(), event.getPlayer().getName()+" has rejoined the arena.");
				
				class SyncTask implements Runnable {
					String name;
					
					public SyncTask(String name) {
						this.name = name;
					}
					
					@Override
					public void run() {
						Player player;
						try {
							Resident resident = CivGlobal.getResident(name);
							player = CivGlobal.getPlayer(resident);
							player.setScoreboard(resident.getCurrentArena().getScoreboard(resident.getTeam().getName()));
						} catch (CivException e) {
						}						
					}
				}
				
				TaskMaster.syncTask(new SyncTask(event.getPlayer().getName()));
				return;
			} else {
				
				class SyncTask implements Runnable {
					String name;
					
					public SyncTask(String name) { 
						this.name = name; 
					}
					
					@Override
					public void run() {
						Resident resident = CivGlobal.getResident(name);
						
						/* Player is rejoining but the arena is no longer active. Return home. */
						resident.teleportHome();
						resident.restoreInventory();
						resident.setInsideArena(false);
						resident.save();
						
						

						CivMessage.send(resident, CivColor.LightGray+"You've been teleported home since the arena you were in no longer exists.");
					}
				}
				
				event.getPlayer().getInventory().clear();
				TaskMaster.syncTask(new SyncTask(event.getPlayer().getName()), 10);
			}
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerQuit(PlayerQuitEvent event) {
		String worldName = event.getPlayer().getWorld().getName();
		if (!ArenaManager.activeArenas.containsKey(worldName)) {
			return;
		}
		
		/* Player is leaving an active arena. Let everyone know. */
		CivMessage.sendArena(ArenaManager.activeArenas.get(worldName), event.getPlayer().getName()+" has logged out of the arena.");
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockPlaceEvent(BlockPlaceEvent event) {
		String worldName = event.getBlock().getWorld().getName();
		if (!ArenaManager.activeArenas.containsKey(worldName)) {
			return;
		}
		
		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (resident.isSBPermOverride()) {
			return;
		}
		
		event.setCancelled(true);
	}
	
	
	public static BlockCoord bcoord = new BlockCoord();
	@EventHandler(priority = EventPriority.LOWEST)
	public void onBlockBreakEvent(BlockBreakEvent event) {
		String worldName = event.getBlock().getWorld().getName();
		if (!ArenaManager.activeArenas.containsKey(worldName)) {
			return;
		}
		
		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (resident.isSBPermOverride()) {
			return;
		}
		
		bcoord.setFromLocation(event.getBlock().getLocation());
		ArenaControlBlock acb = ArenaManager.arenaControlBlocks.get(bcoord);
		if (acb != null) {
			acb.onBreak(resident);
		}
		
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onPlayerRespawn(PlayerRespawnEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());
		
		if (!resident.isInsideArena()) {
			return;
		}
		
		Arena arena = resident.getCurrentArena();
		if (arena == null) {
			return;
		}
		
		Location loc = arena.getRespawnLocation(resident);
		if (loc != null) {
			CivMessage.send(resident, CivColor.LightGray+"Respawned in arena.");
			World world = Bukkit.getWorld(arena.getInstanceName());
			loc.setWorld(world);
			
			resident.setLastKilledTime(new Date());
			event.setRespawnLocation(loc);
		}
	}
	
	@EventHandler(priority = EventPriority.NORMAL)
	public void onPlayerInteract(PlayerInteractEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());
		
		if (!resident.isInsideArena()) 
		{
			return;
		}
		
		Arena arena = resident.getCurrentArena();
		if (arena == null) {
			return;
		}
		
		if (!event.hasBlock()) {
			return;
		}
		
		BlockCoord bcoord = new BlockCoord(event.getClickedBlock().getLocation());
		if (ArenaManager.chests.containsKey(bcoord)) {
			if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
				Player player;
				try {
					player = CivGlobal.getPlayer(resident);
					player.closeInventory();
				} catch (CivException e) {
				}
				
				class SyncTask implements Runnable {
					Arena arena;
					Resident resident;
					
					public SyncTask(Arena arena, Resident resident) {
						this.arena = arena;
						this.resident = resident;
					}
					
					@Override
					public void run() {
						Player player;
						try {
							player = CivGlobal.getPlayer(resident);
							Inventory inv = arena.getInventory(resident);
							player.openInventory(inv);
						} catch (CivException e) {
						}
	
					}
					
				}
				
				TaskMaster.syncTask(new SyncTask(arena, resident), 0);
				event.setCancelled(true);
				return;
			}
		}
		
		
		/* Did we click on a respawn sign. */
		if (ArenaManager.respawnSigns.containsKey(bcoord)) {
			class SyncTask implements Runnable {
				Resident resident;
				Arena arena;
				
				public SyncTask(Resident resident, Arena arena) {
					this.resident = resident;
					this.arena = arena;
				}
				
				@Override
				public void run() {					
					if (!DateUtil.isAfterSeconds(resident.getLastKilledTime(), 30)) {
						Date now = new Date();
						long secondsLeft = (now.getTime() - resident.getLastKilledTime().getTime()) / 1000;
						secondsLeft = 30 - secondsLeft;
						
						CivMessage.sendError(resident, "Respawning back into arena in "+secondsLeft+" seconds.");
						TaskMaster.syncTask(this, TimeTools.toTicks(1));
					} else {
						BlockCoord revive = arena.getRandomReviveLocation(resident);
						if (revive != null) {
							Location loc = revive.getCenteredLocation();
							World world = Bukkit.getWorld(arena.getInstanceName());
							loc.setWorld(world);
							CivMessage.send(resident, CivColor.LightGray+"Revived in arena.");
							
							Player player;
							try {
								player = CivGlobal.getPlayer(resident);
								player.teleport(loc);
							} catch (CivException e) {
								return;
							}
						}	
					}
				}
			}
			
			TaskMaster.syncTask(new SyncTask(resident, arena));
		}
	}
	
}
