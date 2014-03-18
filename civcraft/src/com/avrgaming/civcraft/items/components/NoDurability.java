package com.avrgaming.civcraft.items.components;

import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import gpl.AttributeUtil;

public class NoDurability extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {		
	}
	
	@Override
	public void onInventoryOpen(InventoryOpenEvent event, ItemStack stack) {
		stack.setDurability((short) 0);		
	}

}
