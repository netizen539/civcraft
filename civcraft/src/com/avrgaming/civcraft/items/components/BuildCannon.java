package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.siege.Cannon;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class BuildCannon extends ItemComponent {

	public void onInteract(PlayerInteractEvent event) {
		try {
			
			if (!War.isWarTime()) {
				throw new CivException("Cannons can only be deployed during WarTime.");
			}
			
			Resident resident = CivGlobal.getResident(event.getPlayer());
			Cannon.newCannon(resident);
			
			CivMessage.sendCiv(resident.getCiv(), "We've deployed a cannon at "+
					event.getPlayer().getLocation().getBlockX()+","+
					event.getPlayer().getLocation().getBlockY()+","+
					event.getPlayer().getLocation().getBlockZ());
			
			ItemStack newStack = new ItemStack(Material.AIR);
			event.getPlayer().setItemInHand(newStack);
		} catch (CivException e) {
			CivMessage.sendError(event.getPlayer(), e.getMessage());
		}
		
	}

	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.RESET+CivColor.Gold+"Deploys War Cannon");
		attrUtil.addLore(ChatColor.RESET+CivColor.Rose+"<Right Click To Use>");	
	}
	
}
