package com.avrgaming.civcraft.loregui;

import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.ItemStack;

public class BuildStructureList implements GuiAction {

	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
//		Player player = (Player)event.getWhoClicked();
//		Resident resident = CivGlobal.getResident(player);
//		Inventory guiInventory = Bukkit.getServer().createInventory(player, LoreGuiItem.MAX_INV_SIZE, "Pick Structure To Build");
//		
//		for (ConfigBuildableInfo info : CivSettings.structures.values()) {
//			int type = ItemManager.getId(Material.ANVIL);
//			if (info.itemTypeId != 0) {
//				type = info.itemTypeId;
//			}
//			
//			ItemStack is;
//			if (!resident.hasTown()) {
//				is = LoreGuiItem.build(info.displayName, ItemManager.getId(Material.BEDROCK), 0, CivColor.Rose+"Must belong to a town to build structures.");
//			} else {
//				if (!resident.getCiv().hasTechnology(info.require_tech)) {
//					ConfigTech tech = CivSettings.techs.get(info.require_tech);
//					is = LoreGuiItem.build(info.displayName, ItemManager.getId(Material.BEDROCK), 0, CivColor.Rose+"Requires: "+tech.name);
//				} else {
//					is = LoreGuiItem.build(info.displayName, type, info.itemData, CivColor.Gold+"<Click To Build>");
//					is = LoreGuiItem.setAction(is, "BuildChooseTemplate");
//					is = LoreGuiItem.setActionData(is, "info", info.id);
//				}
//			}
//			
//			guiInventory.addItem(is);
//		}
//		
//		
//		LoreGuiItemListener.guiInventories.put(guiInventory.getName(), guiInventory);		
//		TaskMaster.syncTask(new OpenInventoryTask(player, guiInventory));
	}



}
