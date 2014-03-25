package com.avrgaming.civcraft.lorestorage;

import gpl.AttributeUtil;

import java.util.ArrayList;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.PrepareItemCraftEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTech;
import com.avrgaming.civcraft.config.ConfigTechItem;
import com.avrgaming.civcraft.items.components.Tagged;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.global.perks.PlatinumManager;

public class LoreCraftableMaterialListener implements Listener {

	@EventHandler(priority = EventPriority.LOW)
	public void OnCraftItemEvent(CraftItemEvent event) {
		if (event.getWhoClicked() instanceof Player) {
			Player player = (Player)event.getWhoClicked();
						
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(event.getInventory().getResult());
			if (craftMat == null) {
				
				/* Disable notch apples */
				ItemStack resultStack = event.getInventory().getResult();
				if (resultStack.getType().equals(Material.GOLDEN_APPLE)) {
					CivMessage.sendError((Player)event.getWhoClicked(), "You cannot craft golden apples. Sorry.");
					event.setCancelled(true);
					return;
				}
				
				ConfigTechItem restrictedTechItem = CivSettings.techItems.get(ItemManager.getId(resultStack));
				if (restrictedTechItem != null) {
					ConfigTech tech = CivSettings.techs.get(restrictedTechItem.require_tech);
					CivMessage.sendError(player, "Your civilization doesn't have the required technology ("+tech.name+") to craft this item.");
					event.setCancelled(true);
					return;
				}
				
				return;
			}
			
			if (!craftMat.getConfigMaterial().playerHasTechnology(player)) {
				CivMessage.sendError(player, "You do not have the required technology ("+craftMat.getConfigMaterial().getRequireString()+") to craft this item.");
				event.setCancelled(true);
				return;
			}
			
//			if (craftMat.hasComponent("Tagged")) {
//				String tag = Tagged.matrixHasSameTag(event.getInventory().getMatrix());
//				if (tag == null) {
//					CivMessage.sendError(player, "All items must have been generated from the same camp.");
//					event.setCancelled(true);
//					return;
//				}
//				
//				Tagged tagged = (Tagged)craftMat.getComponent("Tagged");
//				ItemStack stack = tagged.addTag(event.getInventory().getResult(), tag);
//				AttributeUtil attrs = new AttributeUtil(stack);
//				attrs.addLore(CivColor.LightGray+tag);
//				stack = attrs.getStack();
//				event.getInventory().setResult(stack);
//			}
			
			Resident resident = CivGlobal.getResident(player);
			if (craftMat.getId().equals("mat_found_camp")) {
				PlatinumManager.givePlatinumOnce(resident, 
						CivSettings.platinumRewards.get("buildCamp").name,
						CivSettings.platinumRewards.get("buildCamp").amount, 
						"Achievement! You've founded your first camp and earned %d");
			} else if(craftMat.getId().equals("mat_found_civ")) {
				PlatinumManager.givePlatinumOnce(resident, 
						CivSettings.platinumRewards.get("buildCiv").name,
						CivSettings.platinumRewards.get("buildCiv").amount, 
						"Achievement! You've founded your first Civilization and earned %d");				
			} else {
				class AsyncTask implements Runnable {
					Resident resident;
					int craftAmount;
					
					public AsyncTask(Resident resident, int craftAmount) {
						this.resident = resident;
						this.craftAmount = craftAmount;
					}
					
					
					@Override
					public void run() {
						String key = resident.getName()+":platinumCrafted";
						ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);
						Integer amount = 0;
						
						if (entries.size() == 0) {
							amount = craftAmount;
							CivGlobal.getSessionDB().add(key, ""+amount, 0, 0, 0);
							
						} else {
							amount = Integer.valueOf(entries.get(0).value);
							amount += craftAmount;
							if (amount >= 100) {
								PlatinumManager.givePlatinum(resident, 
										CivSettings.platinumRewards.get("craft100Items").amount, 
										"Expert crafting earns you %d");
								amount -= 100;
							}
						
							CivGlobal.getSessionDB().update(entries.get(0).request_id, key, ""+amount);
						}
					}
				}
				
				/* if shift clicked, the amount crafted is always min. */
				int amount;
				if (event.isShiftClick()) {
					amount = 64; //cant craft more than 64.
					for (ItemStack stack : event.getInventory().getMatrix()) {
						if (stack == null) {
							continue;
						}
						
						if (stack.getAmount() < amount) {
							amount = stack.getAmount();
						}
					}
				} else {
					amount = 1;
				}
				
				TaskMaster.asyncTask(new AsyncTask(resident, amount), 0);
			}
		}
	}
	
	
//	private boolean checkCustomMismatch(ItemStack item1, ItemStack item2) {
//		if (LoreMaterial.isCustom(item1)) {
//		//	CivLog.debug("\tmatrix is custom.");
//			if (!LoreMaterial.isCustom(item2)) {
//				/* custom item mismatch. */
//			//	CivLog.debug("custom mismatch.");
//				return false;
//			}
//			
//			LoreMaterial mMatrixMaterial = LoreMaterial.getMaterial(item2);
//			
//			if (!(mMatrixMaterial instanceof LoreCraftableMaterial)) {
//				/* some other kind of custom item, not valid. */
//			//	CivLog.debug("another type of lorecraft.");
//				return false;
//			}
//			
//			LoreCraftableMaterial isMaterial = (LoreCraftableMaterial) LoreMaterial.getMaterial(item1);
//			LoreCraftableMaterial matrixCraftMaterial = (LoreCraftableMaterial)mMatrixMaterial;
//			
//		//	CivLog.debug("\tmatrix:"+isMaterial.getConfigId()+" vs "+matrixCraftMaterial.getConfigId());
//			if (!isMaterial.getConfigId().equals(matrixCraftMaterial.getConfigId())) {
//				/* custom item id's don't match. */
//			//	CivLog.debug("invalid custom");
//				return false;
//			}
//			
//			/* By reaching this point, this itemstack is in the right location and matches this recipe. */
//			//CivLog.debug("item ok.");
//		} else {
//			//CivLog.debug("\tmatrix not custom");
//		}
//		return true;
//	}
	private boolean matrixContainsCustom(ItemStack[] matrix) {
		for (ItemStack stack : matrix) {
			if (LoreMaterial.isCustom(stack)) {
				return true;
			}
		}
		return false;
	}
	

	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOW)
	public void OnPrepareItemCraftEvent(PrepareItemCraftEvent event) {
		
		if (event.getRecipe() instanceof ShapedRecipe) {
			String key = LoreCraftableMaterial.getShapedRecipeKey(event.getInventory().getMatrix());
			LoreCraftableMaterial loreMat = LoreCraftableMaterial.shapedKeys.get(key);

			if (loreMat == null) {
				if(LoreCraftableMaterial.isCustom(event.getRecipe().getResult())) {
					/* Result is custom, but we have found no custom recipie. Set to blank. */
					event.getInventory().setResult(new ItemStack(CivData.AIR));
				}
				
				if (matrixContainsCustom(event.getInventory().getMatrix())) {
					event.getInventory().setResult(new ItemStack(CivData.AIR));
				}
				
				return;
			} else {
				if(!LoreCraftableMaterial.isCustom(event.getRecipe().getResult())) {
					/* Result is not custom, but recipie is. Set to blank. */
					if (!loreMat.isVanilla()) {
						event.getInventory().setResult(new ItemStack(CivData.AIR));
						return;
					}
				}
			}
			
			ItemStack newStack;
			if (!loreMat.isVanilla()) {
				newStack = LoreMaterial.spawn(loreMat);
				AttributeUtil attrs = new AttributeUtil(newStack);
				loreMat.applyAttributes(attrs);
				newStack.setAmount(loreMat.getCraftAmount());
			} else {
				newStack = ItemManager.createItemStack(loreMat.getTypeID(), loreMat.getCraftAmount());
			}
			
			event.getInventory().setResult(newStack);
			
		} else {
			String key = LoreCraftableMaterial.getShapelessRecipeKey(event.getInventory().getMatrix());
			LoreCraftableMaterial loreMat = LoreCraftableMaterial.shapelessKeys.get(key);
						
			if (loreMat == null) {
				if(LoreCraftableMaterial.isCustom(event.getRecipe().getResult())) {
					/* Result is custom, but we have found no custom recipie. Set to blank. */
					event.getInventory().setResult(new ItemStack(CivData.AIR));
				}
				
				if (matrixContainsCustom(event.getInventory().getMatrix())) {
					event.getInventory().setResult(new ItemStack(CivData.AIR));
				}
				
				return;
			} else {
				if(!LoreCraftableMaterial.isCustom(event.getRecipe().getResult())) {
					/* Result is not custom, but recipie is. Set to blank. */
					if (!loreMat.isVanilla()) {
						event.getInventory().setResult(new ItemStack(CivData.AIR));
						return;
					}
				}
			}
			
			
			ItemStack newStack;
			if (!loreMat.isVanilla()) {
				newStack = LoreMaterial.spawn(loreMat);
				AttributeUtil attrs = new AttributeUtil(newStack);
				loreMat.applyAttributes(attrs);
				newStack.setAmount(loreMat.getCraftAmount());
			} else {
				newStack = ItemManager.createItemStack(loreMat.getTypeID(), loreMat.getCraftAmount());
			}	
			
			event.getInventory().setResult(newStack);
		}
		
		ItemStack result = event.getInventory().getResult();
		LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(result);
		if (craftMat != null) {
			if (craftMat.hasComponent("Tagged")) {
				String tag = Tagged.matrixHasSameTag(event.getInventory().getMatrix());
				if (tag == null) {
					event.getInventory().setResult(ItemManager.createItemStack(CivData.AIR, 1));
					return;
				}
				
				Tagged tagged = (Tagged)craftMat.getComponent("Tagged");
				ItemStack stack = tagged.addTag(event.getInventory().getResult(), tag);
				AttributeUtil attrs = new AttributeUtil(stack);
				attrs.addLore(CivColor.LightGray+tag);
				stack = attrs.getStack();
				event.getInventory().setResult(stack);
			}
		}
		
	}	
}
