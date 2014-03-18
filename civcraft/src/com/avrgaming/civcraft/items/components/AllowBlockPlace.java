package com.avrgaming.civcraft.items.components;

import org.bukkit.event.block.BlockPlaceEvent;

import gpl.AttributeUtil;

public class AllowBlockPlace extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {}
	
	@Override
	public boolean onBlockPlaced(BlockPlaceEvent event) { 
		return true; 
	}

}
