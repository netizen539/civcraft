package com.avrgaming.civcraft.loregui;

import gpl.AttributeUtil;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigIngredient;
import com.avrgaming.civcraft.config.ConfigMaterialCategory;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.lorestorage.LoreGuiItemListener;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.ItemManager;

public class ShowRecipe implements GuiAction {

	public static final int START_OFFSET = LoreGuiItem.INV_ROW_COUNT + 3;
	
	public ItemStack getIngredItem(ConfigIngredient ingred, Inventory recInv) {
		String name;
		String message;
		ItemStack entryStack;
		if (ingred.custom_id == null) {
			name = ItemManager.getMaterialData(ingred.type_id, ingred.data).toString();
			message = "Vanilla Item";
			entryStack = LoreGuiItem.build(name, ingred.type_id, ingred.data, message);
		} else {
			LoreCraftableMaterial cmat = LoreCraftableMaterial.getCraftMaterialFromId(ingred.custom_id);
			name = cmat.getName();
			message = "Click For Recipe";
			entryStack = LoreCraftableMaterial.spawn(cmat);
			entryStack = LoreGuiItem.asGuiItem(entryStack);
			entryStack = LoreGuiItem.setAction(entryStack, "ShowRecipe");
			entryStack = LoreGuiItem.setActionData(entryStack, "backInventory", recInv.getName());
			AttributeUtil attrs = new AttributeUtil(entryStack);
			attrs.addLore(message);
			entryStack = attrs.getStack();
		}
		return entryStack;
	}
	
	public void buildCraftTableBorder(Inventory recInv) {
		int offset = 2;
		ItemStack stack;
	
		stack = LoreGuiItem.build("Craft Table Border", ItemManager.getId(Material.WORKBENCH), 0, "");
		
		for (int y = 0; y <= 4; y++) {
			for (int x = 0; x <= 4; x++) {
				if (x == 0 || x == 4 || y == 0 || y == 4) {
					recInv.setItem(offset+(y*LoreGuiItem.INV_ROW_COUNT)+x, stack);
				}
			}
		}		
	}
	
	public void buildInfoBar(LoreCraftableMaterial craftMat, Inventory recInv, Player player) {
		int offset = 0;
		ItemStack stack = null;
		
		if (craftMat.getConfigMaterial().required_tech != null) {
			Resident resident = CivGlobal.getResident(player);
			ConfigTech tech = CivSettings.techs.get(craftMat.getConfigMaterial().required_tech);
			if (tech != null) {
			
				if (resident.hasTown() && resident.getCiv().hasTechnology(craftMat.getConfigMaterial().required_tech)) {
					stack = LoreGuiItem.build("Required Technology", ItemManager.getId(Material.EMERALD_BLOCK), 0, tech.name);
				} else {
					stack = LoreGuiItem.build("Required Technology", ItemManager.getId(Material.REDSTONE_BLOCK), 0, tech.name);
				}
			}
			
			if (stack != null) {
				recInv.setItem(offset+0, stack);
			}
		}
		
		if (craftMat.isShaped()) {
			stack = LoreGuiItem.build("Shaped", ItemManager.getId(Material.HOPPER), 0, "");
		} else {
			stack = LoreGuiItem.build("Unshaped", ItemManager.getId(Material.COAL), 0, "");
		}
		offset += LoreGuiItem.INV_ROW_COUNT;
		recInv.setItem(offset+0, stack);
		

	}
	
	@Override
	public void performAction(InventoryClickEvent event, ItemStack stack) {
		Player player = (Player)event.getWhoClicked();
		
		LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
		if (craftMat == null) {
			/* Do nothing for now. */
			return;
		}
		
		String title = craftMat.getName()+" Recipe";
		if (title.length() > 32) {
			title = title.substring(0, 32);
		}
		
		Inventory recInv = Bukkit.getServer().createInventory(player, LoreGuiItem.MAX_INV_SIZE, title);
		if (craftMat.isShaped()) {		
			int offset = START_OFFSET;
			for (String line : craftMat.getConfigMaterial().shape) {
				for (int i = 0; i < line.toCharArray().length; i++) {
					ConfigIngredient ingred = null;
					for (ConfigIngredient in : craftMat.getConfigMaterial().ingredients.values()) {
						if (in.letter.equalsIgnoreCase(String.valueOf(line.toCharArray()[i]))) {
							ingred = in;
							break;
						}
					}
					
					if (ingred != null) {
						recInv.setItem(i+offset, getIngredItem(ingred, recInv));
					}
				}
				offset += LoreGuiItem.INV_ROW_COUNT;
			}
		} else {
			int x = 0;
			int offset = START_OFFSET;
			for (ConfigIngredient ingred : craftMat.getConfigMaterial().ingredients.values()) {
				if (ingred != null) {				
					for (int i = 0; i < ingred.count; i++) {						
						recInv.setItem(x+offset, getIngredItem(ingred, recInv));
						
						x++;
						if (x >= 3) {
							x = 0;
							offset += LoreGuiItem.INV_ROW_COUNT;
						}
					}
				}
			}
		}
		
		String backInventory = LoreGuiItem.getActionData(stack, "backInventory");
		if (backInventory != null) {
			Inventory inv = LoreGuiItemListener.guiInventories.get(backInventory);
			ItemStack backButton = LoreGuiItem.build("Back", ItemManager.getId(Material.MAP), 0, "Back");
			backButton = LoreGuiItem.setAction(backButton, "OpenInventory");
			backButton = LoreGuiItem.setActionData(backButton, "invType", "showGuiInv");
			backButton = LoreGuiItem.setActionData(backButton, "invName", inv.getName());
			recInv.setItem(LoreGuiItem.MAX_INV_SIZE-1, backButton);
		} else {
			ConfigMaterialCategory cat = ConfigMaterialCategory.getCategory(craftMat.getConfigMaterial().categoryCivColortripped); 
			if (cat != null) {					
				ItemStack backButton = LoreGuiItem.build("Back", ItemManager.getId(Material.MAP), 0, "Back to Category "+cat.name);
				backButton = LoreGuiItem.setAction(backButton, "OpenInventory");
				backButton = LoreGuiItem.setActionData(backButton, "invType", "showGuiInv");
				backButton = LoreGuiItem.setActionData(backButton, "invName", cat.name+" Recipes");
				recInv.setItem(LoreGuiItem.MAX_INV_SIZE-1, backButton);
			}
		}
		
		LoreGuiItemListener.guiInventories.put(recInv.getName(), recInv);
		buildCraftTableBorder(recInv);
		buildInfoBar(craftMat, recInv, player);
		player.openInventory(recInv);
	}

}
