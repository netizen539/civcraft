package com.avrgaming.civcraft.listener;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ExpBottleEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerExpChangeEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class DisableXPListener implements Listener {

	@EventHandler(priority = EventPriority.LOW)
	public void onExpBottleEvent(ExpBottleEvent event) {
		event.setExperience(0);
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onItemSpawnEvent(ItemSpawnEvent event) {
//		if (event.getEntity().getType().equals(EntityType.EXPERIENCE_ORB)) {
//			event.setCancelled(true);
//		}
	}
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if (!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		
		if (event.getClickedBlock() == null || ItemManager.getId(event.getClickedBlock()) == CivData.AIR) {
			return;
		}
		
		Block block = event.getClickedBlock();
		
		if (block.getType().equals(Material.ENCHANTMENT_TABLE)) {
			CivMessage.sendError(event.getPlayer(), "Cannot use enchantment tables. XP and Levels disabled in CivCraft.");
			event.setCancelled(true);
		}
		
		if (block.getType().equals(Material.ANVIL)) {
			CivMessage.sendError(event.getPlayer(), "Cannot use anvils. XP and Levels disabled in CivCraft.");
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onPlayerExpChange(PlayerExpChangeEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());
		CivMessage.send(resident, CivColor.LightGreen+"Picked up "+CivColor.Yellow+event.getAmount()+CivColor.LightGreen+" coins.");
		resident.getTreasury().deposit(event.getAmount());
		
		
		event.setAmount(0);
	}
	
}
