/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.lorestorage;

import gpl.AttributeUtil;

import java.io.IOException;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.Template.TemplateType;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.tutorial.CivTutorial;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.CustomPersonalTemplate;
import com.avrgaming.global.perks.components.CustomTemplate;

public class LoreGuiItem {
			
	public static final int MAX_INV_SIZE = 54;

	public static ItemStack getGUIItem(String title, String[] messages, int type, int data) {
		ItemStack stack = ItemManager.createItemStack(type, 1, (short)data);
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.setCivCraftProperty("GUI", title);
		attrs.setName(title);
		attrs.setLore(messages);
		return attrs.getStack();
	}
	
	public static boolean isGUIItem(ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		String title = attrs.getCivCraftProperty("GUI");
		if (title != null) {
			return true;
		}
		return false;
	}
	
	public static ItemStack setAction(ItemStack stack, String action) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.setCivCraftProperty("GUI_ACTION", action);
		return attrs.getStack();
	}

	public static String getAction(ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		String action = attrs.getCivCraftProperty("GUI_ACTION");
		return action;
	}
	
	public static ItemStack build(String title, int type, int data, String... messages) {
		return getGUIItem(title, messages, type, data);
	}

	public static ItemStack asGuiItem(ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.setCivCraftProperty("GUI", ""+ItemManager.getId(stack));
		return attrs.getStack();
	}

	@SuppressWarnings("deprecation")
	public static void processAction(String action, ItemStack stack, InventoryClickEvent event) {
		String[] args = action.split(":");
		Player player = (Player)event.getWhoClicked();
		Resident resident = CivGlobal.getResident(player);
		Perk perk;
		boolean useDefaultTemplate = true;
		ConfigBuildableInfo info;
		Template tpl;
		String path;
		Location centerLoc;
				
		switch(args[0].toLowerCase()) {
		case "spawn":
			AttributeUtil attrs = new AttributeUtil(stack);
			attrs.removeCivCraftProperty("GUI");
			attrs.removeCivCraftProperty("GUI_ACTION");

			ItemStack is = attrs.getStack();
			if (event.getClick().equals(ClickType.SHIFT_LEFT) ||
					event.getClick().equals(ClickType.SHIFT_RIGHT)) {
				is.setAmount(is.getMaxStackSize());
			}
			
			event.setCursor(is);
			break;
		case "openinv":				
			player.closeInventory();
			
			class SyncTaskDelayed implements Runnable {
				String playerName;
				String[] args;
				
				public SyncTaskDelayed(String playerName, String[] args) {
					this.playerName = playerName;
					this.args = args;
				}
				
				@Override
				public void run() {
					Player player;
					try {
						player = CivGlobal.getPlayer(playerName);
					} catch (CivException e) {
						e.printStackTrace();
						return;
					}
					
					switch (args[1]) {
					case "showTutorialInventory":
						CivTutorial.showTutorialInventory(player);
						break;
					case "showCraftingHelp":
						CivTutorial.showCraftingHelp(player);
						break;
					case "showGuiInv":
						Inventory inv = LoreGuiItemListener.guiInventories.get(args[2]);
						if (inv != null) {
							player.openInventory(inv);
						} else {
							CivLog.error("Couldn't find GUI inventory:"+args[2]);
						}
						break;
					default:
						break;
					}
				}
			}
			
			TaskMaster.syncTask(new SyncTaskDelayed(player.getName(), args));
			break;
		//case ""openinv:showGuiInv:"+cat.name+" Spawn"
		case "activateperk":
			perk = resident.perks.get(args[1]);
			if (perk != null) {
				perk.onActivate(resident);
			} else {
				CivLog.error("Couldn't activate perk:"+args[1]+" cause it wasn't found in perks hashmap.");
			}
			player.closeInventory();
			break;
		case "buildwithdefaultpersonaltemplate":
			info = resident.pendingBuildableInfo;
			
			try {
				path = Template.getTemplateFilePath(info.template_base_name, Template.getDirection(player.getLocation()), TemplateType.STRUCTURE, "default");
				try {
					//tpl.load_template(path);
					tpl = Template.getTemplate(path, player.getLocation());
				} catch (IOException e) {
					e.printStackTrace();
					return;
				}
				
				centerLoc = Buildable.repositionCenterStatic(player.getLocation(), info, Template.getDirection(player.getLocation()), (double)tpl.size_x, (double)tpl.size_z);	
				//Buildable.validate(player, null, tpl, centerLoc, resident.pendingCallback);
				TaskMaster.asyncTask(new StructureValidator(player, tpl.getFilepath(), centerLoc, resident.pendingCallback), 0);
				player.closeInventory();

			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
			}
			break;
		case "buildwithpersonaltemplate":
			info = resident.pendingBuildableInfo;
			try {
				/* get the template name from the perk's CustomTemplate component. */
				perk = Perk.staticPerks.get(args[1]);
				CustomPersonalTemplate customTemplate = (CustomPersonalTemplate)perk.getComponent("CustomPersonalTemplate");
				tpl = customTemplate.getTemplate(player, resident.pendingBuildableInfo);
				centerLoc = Buildable.repositionCenterStatic(player.getLocation(), info, Template.getDirection(player.getLocation()), (double)tpl.size_x, (double)tpl.size_z);	
				//Buildable.validate(player, null, tpl, centerLoc, resident.pendingCallback);
				TaskMaster.asyncTask(new StructureValidator(player, tpl.getFilepath(), centerLoc, resident.pendingCallback), 0);
				resident.desiredTemplate = tpl;
				player.closeInventory();
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
			}
			break;
//		case "activateperkandbuildtemplate":
//			perk = resident.perks.get(args[1]);
//			if (perk != null) {
//				perk.onActivate(resident);
//			} else {
//				CivLog.error("Couldn't activate perk:"+args[1]+" cause it wasn't found in perks hashmap.");
//			}
//			break
		case "buildwithtemplate":
			useDefaultTemplate = false;
			/* fall through */
		case "buildwithdefaulttemplate":
			
			try {
				if (!useDefaultTemplate) {
					/* Use a template defined by a perk. */
					perk = Perk.staticPerks.get(args[1]);
					if (perk != null) {
		
						/* get the template name from the perk's CustomTemplate component. */
						CustomTemplate customTemplate = (CustomTemplate)perk.getComponent("CustomTemplate");
						if (customTemplate != null) {
							tpl = customTemplate.getTemplate(player, resident.pendingBuildable);
						} else {
							CustomPersonalTemplate customPersonalTemplate =  (CustomPersonalTemplate)perk.getComponent("CustomPersonalTemplate");
							tpl = customPersonalTemplate.getTemplate(player, resident.pendingBuildable.info);
						}
						
						resident.pendingBuildable.buildPlayerPreview(player, player.getLocation(), tpl);					

					} else {
						CivLog.error("Couldn't activate perk:"+args[1]+" cause it wasn't found in perks hashmap.");
					}
				} else {
					/* Use the default template. */
					tpl = new Template();
					try {
						tpl.initTemplate(player.getLocation(), resident.pendingBuildable);
					} catch (CivException e) {
						e.printStackTrace();
						throw e;
					} catch (IOException e) {
						e.printStackTrace();
						throw e;
					}
					
					resident.pendingBuildable.buildPlayerPreview(player, player.getLocation(), tpl);
				}	
			} catch (CivException e) {
				CivMessage.sendError(player, e.getMessage());
			} catch (IOException e) {
				CivMessage.sendError(player, "Internal IO Error.");
				e.printStackTrace();
			}
			player.closeInventory();
			break;
		}
		
	}
	
	
	
}
