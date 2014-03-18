package com.avrgaming.civcraft.arena;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;

public class ArenaListener implements Listener {

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

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockBreakEvent(BlockBreakEvent event) {
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
	
}
