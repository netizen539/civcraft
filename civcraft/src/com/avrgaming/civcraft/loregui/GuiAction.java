package com.avrgaming.civcraft.loregui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;


public interface GuiAction {
	public void performAction(InventoryClickEvent event, ItemStack stack);
}
