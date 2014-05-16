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
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class LoreEnhancementArenaItem  extends LoreEnhancement implements Listener {
	
	public String getDisplayName() {
		return "Arena";
	}
	
	public AttributeUtil add(AttributeUtil attrs) {
		attrs.addEnhancement("LoreEnhancementArenaItem", null, null);
		attrs.addLore(CivColor.LightBlue+getDisplayName());
		return attrs;
	}
	
	private boolean isIllegalStack(ItemStack stack) {
		if (stack == null) {
			return false;
		}
		
		LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
		if (craftMat == null) {
			return false;
		}
		
		if (craftMat.getConfigMaterial().required_tech == null) {
			return false;
		}
		
		/*
		 * If nobody has EVER researched the technology for this item, then it is an ILLEGAL item.
		 */
		if (!CivGlobal.researchedTechs.contains(craftMat.getConfigMaterial().required_tech.toLowerCase())) {
			return true;
		}
		
		
		return false;
	}
	
	/* Listeners for Arena Items */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(InventoryOpenEvent event) {
		if (LoreGuiItemListener.isGUIInventory(event.getInventory())) {
			return;
		}
		
		String removedReason = null;
		for (ItemStack stack : event.getInventory().getContents()) {
			
			if (stack == null) {
				continue;
			}
			
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
			if (craftMat == null) {
				continue;
			}
			
			Resident resident = CivGlobal.getResident((Player) event.getPlayer());
			if (LoreCraftableMaterial.hasEnhancement(stack, "LoreEnhancementArenaItem")) {
				if (!resident.isInsideArena()) {
					event.getInventory().remove(stack);
					removedReason = CivColor.LightGray+"Some items were removed since they were arena items";
				}
				
				/* Arena items are OK after this point. */
				continue;
			}
			
			if (isIllegalStack(stack)) {
				if (event.getPlayer().isOp()) {
					//CivMessage.send(event.getPlayer(), CivColor.LightGray+"You're allowed to keep an illegal item because you are op.");
					continue;
				} else {
					if (!resident.isInsideArena()) {
						event.getInventory().remove(stack);
						removedReason = CivColor.LightGray+"Some items were detected as illegal/impossible and have been removed.";
					}
				}
			}
			
		}
		
		if (removedReason != null) {
			CivMessage.send(event.getPlayer(), removedReason);
		}
	}

	/* Listeners for Arena Items */
	@EventHandler(priority = EventPriority.LOWEST)
	public void onInteract(InventoryCloseEvent event) {
		if (LoreGuiItemListener.isGUIInventory(event.getInventory())) {
			return;
		}
		
		String removedReason = null;
		for (ItemStack stack : event.getPlayer().getInventory().getContents()) {

			if (stack == null) {
				continue;
			}
			
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
			if (craftMat == null) {
				continue;
			}
			Resident resident = CivGlobal.getResident((Player) event.getPlayer());

			if (LoreCraftableMaterial.hasEnhancement(stack, "LoreEnhancementArenaItem")) {
				if (!resident.isInsideArena()) {
					event.getPlayer().getInventory().remove(stack);
					removedReason = CivColor.LightGray+"Some items were removed since they were arena items";
				}
				
				/* Arena items are OK after this point. */
				continue;
			}
			
			if (isIllegalStack(stack)) {
				if (event.getPlayer().isOp()) {
					//CivMessage.send(event.getPlayer(), CivColor.LightGray+"You're allowed to keep an illegal item because you are op.");
					continue;
				} else {
					if (!resident.isInsideArena()) {
						event.getPlayer().getInventory().remove(stack);
						removedReason = CivColor.LightGray+"Some items were detected as illegal/impossible and have been removed.";
					}
				}
			}
		}
		
		ItemStack[] contents = new ItemStack[4];
		for (int i = 0; i < event.getPlayer().getInventory().getArmorContents().length; i++) {
			ItemStack stack = event.getPlayer().getInventory().getArmorContents()[i];
			if (stack == null) {
				continue;
			}
			
						
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
			Resident resident = CivGlobal.getResident((Player) event.getPlayer());

			if (craftMat != null) {
				if (LoreCraftableMaterial.hasEnhancement(stack, "LoreEnhancementArenaItem")) {
					if (!resident.isInsideArena()) {
						continue; /* dont re-add */
					}
					removedReason = CivColor.LightGray+"Some items were removed since they were arena items";
				} else {			
					if (isIllegalStack(stack)) {
						if (event.getPlayer().isOp()) {
							//CivMessage.send(event.getPlayer(), CivColor.LightGray+"You're allowed to keep an illegal item because you are op.");
						} else {
							if (!resident.isInsideArena()) {
								removedReason = CivColor.LightGray+"Some items were detected as illegal/impossible and have been removed.";
								continue; /* don't re-add */
							}
						}
					}
				}
			}
			
			contents[i] = stack;
		}
		
		event.getPlayer().getInventory().setArmorContents(contents);
		
		if (removedReason != null) {
			CivMessage.send(event.getPlayer(), removedReason);
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
