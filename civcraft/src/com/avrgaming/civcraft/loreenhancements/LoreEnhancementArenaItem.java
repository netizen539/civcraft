package com.avrgaming.civcraft.loreenhancements;

import gpl.AttributeUtil;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.main.Colors;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class LoreEnhancementArenaItem  extends LoreEnhancement implements Listener {
	
	public String getDisplayName() {
		return "Arena";
	}
	
	public AttributeUtil add(AttributeUtil attrs) {
		attrs.addEnhancement("LoreEnhancementArenaItem", null, null);
		attrs.addLore(Colors.LightBlue+getDisplayName());
		return attrs;
	}
	
	/* Listeners for Arena Items */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(InventoryOpenEvent event) {
		
		boolean removed = false;
		for (ItemStack stack : event.getInventory().getContents()) {

			if (stack == null) {
				continue;
			}
			
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
			if (craftMat == null) {
				continue;
			}
			
			if (LoreCraftableMaterial.hasEnhancement(stack, "LoreEnhancementArenaItem")) {
				Resident resident = CivGlobal.getResident((Player) event.getPlayer());
				if (!resident.isInsideArena()) {
					event.getInventory().remove(stack);
				}
				removed = true;
			}
		}
		
		if (removed) {
			CivMessage.send(event.getPlayer(), CivColor.LightGray+"Some items were removed since they were arena items");
		}
	}
	
	/* Listeners for Arena Items */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(InventoryCloseEvent event) {
		boolean removed = false;
		for (ItemStack stack : event.getPlayer().getInventory().getContents()) {

			if (stack == null) {
				continue;
			}
			
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
			if (craftMat == null) {
				continue;
			}
			
			if (LoreCraftableMaterial.hasEnhancement(stack, "LoreEnhancementArenaItem")) {
				Resident resident = CivGlobal.getResident((Player) event.getPlayer());
				if (!resident.isInsideArena()) {
					event.getPlayer().getInventory().remove(stack);
				}
				removed = true;
			}
		}
		
		ItemStack[] contents = new ItemStack[4];
		for (int i = 0; i < event.getPlayer().getInventory().getArmorContents().length; i++) {
			ItemStack stack = event.getPlayer().getInventory().getArmorContents()[i];
			if (stack == null) {
				continue;
			}
			
						
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
			if (craftMat != null) {
				if (LoreCraftableMaterial.hasEnhancement(stack, "LoreEnhancementArenaItem")) {
					Resident resident = CivGlobal.getResident((Player) event.getPlayer());
					if (!resident.isInsideArena()) {
						continue; /* dont re-add */
					}
					removed = true;
				}
			}
			
			contents[i] = stack;
		}
		event.getPlayer().getInventory().setArmorContents(contents);
		
		if (removed) {
			CivMessage.send(event.getPlayer(), CivColor.LightGray+"Some items were removed since they were arena items");
		}
	}
	
	@Override
	public String serialize(ItemStack stack) {
		return "";
	}

	@Override
	public ItemStack deserialize(ItemStack stack, String data) {
		return stack;
	}

}
