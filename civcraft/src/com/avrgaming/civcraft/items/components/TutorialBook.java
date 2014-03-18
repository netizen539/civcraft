package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.event.block.Action;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.main.Colors;
import com.avrgaming.civcraft.tutorial.CivTutorial;

public class TutorialBook extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
		attrs.addLore(Colors.Gold+"CivCraft Info");
		attrs.addLore(Colors.Rose+"<Right Click to Open>");
	}

	
	public void onInteract(PlayerInteractEvent event) {
		
		event.setCancelled(true);
		if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) &&
				!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		
		//CivTutorial.showCraftingHelp(event.getPlayer());
		CivTutorial.spawnGuiBook(event.getPlayer());

	}
	
	public void onItemSpawn(ItemSpawnEvent event) {
		event.setCancelled(true);
	}

	
}
