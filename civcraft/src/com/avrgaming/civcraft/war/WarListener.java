package com.avrgaming.civcraft.war;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;

public class WarListener implements Listener {

	ChunkCoord coord = new ChunkCoord();
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockBreak(BlockBreakEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		if (!War.isWarTime()) {
			return;
		}
		
		coord.setFromLocation(event.getBlock().getLocation());
		CultureChunk cc = CivGlobal.getCultureChunk(coord);
		
		if (cc == null) {
			return;
		}
		
		if (!cc.getCiv().getDiplomacyManager().isAtWar()) {
			return;
		}
				
		if (event.getBlock().getType().equals(Material.DIRT) || 
			event.getBlock().getType().equals(Material.GRASS) ||
			event.getBlock().getType().equals(Material.SAND) ||
			event.getBlock().getType().equals(Material.GRAVEL) ||
			event.getBlock().getType().equals(Material.TNT) ||
			!event.getBlock().getType().isSolid()) {
			return;
		}
		
		CivMessage.sendError(event.getPlayer(), "Must use TNT to break blocks in at-war civilization cultures during WarTime.");
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
    public void onBlockPlace(BlockPlaceEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		if (!War.isWarTime()) {
			return;
		}
		
		coord.setFromLocation(event.getBlock().getLocation());
		CultureChunk cc = CivGlobal.getCultureChunk(coord);
		
		if (!cc.getCiv().getDiplomacyManager().isAtWar()) {
			return;
		}
				
		if (event.getBlock().getType().equals(Material.DIRT) || 
			event.getBlock().getType().equals(Material.GRASS) ||
			event.getBlock().getType().equals(Material.TNT)) {
			return;
		}
		
		CivMessage.sendError(event.getPlayer(), "Can only place grass, dirt, and TNT blocks in at-war civilization cultures during WarTime.");
		event.setCancelled(true);
	}
	
	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityExplode(EntityExplodeEvent event) {
		if (!event.getEntityType().equals(EntityType.PRIMED_TNT) &&
			!event.getEntityType().equals(EntityType.MINECART_TNT)) {
			return;
		}
		
		if (event.isCancelled()) {
			return;
		}
		
		if (!War.isWarTime()) {
			return;
		}
		
		event.setCancelled(true);
			
		for (int y = -1; y <= 1; y++) {
			for (int x = -1; x <= 1; x++) {
				for (int z = -1; z <= 1; z++) {
					Location loc = event.getLocation().clone().add(new Vector(x,y,z));
					ItemManager.setTypeIdAndData(loc.getBlock(), CivData.AIR, 0, false);
				}
			}
		}
	}

}

