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

public class ArenaListener implements Listener {

	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerLogin(PlayerLoginEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());
		
		if (resident.isInsideArena()) {
			
			if (resident.getCurrentArena() != null) {
	
				CivMessage.sendArena(resident.getCurrentArena(), event.getPlayer().getName()+" has rejoined the arena.");
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
		
		if (!resident.isInsideArena()) {
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
				
				CivMessage.send(resident, "opening chests later..");
				TaskMaster.syncTask(new SyncTask(arena, resident), 0);
				event.setCancelled(true);
				return;
			}
		}
		
		
		/* Did we click on a respawn sign. */
		if (ArenaManager.respawnSigns.containsKey(bcoord)) {
			if (!DateUtil.isAfterSeconds(resident.getLastKilledTime(), 30)) {
				CivMessage.sendError(resident, "You must wait 30 seconds before you can revive in the arena.");
				return;
			}
			
			BlockCoord revive = arena.getRandomReviveLocation(resident);
			if (revive != null) {
				Location loc = revive.getCenteredLocation();
				World world = Bukkit.getWorld(arena.getInstanceName());
				loc.setWorld(world);
				CivMessage.send(resident, CivColor.LightGray+"Revived in arena.");
				
				event.getPlayer().teleport(loc);
			}
		}
	}
	
}
