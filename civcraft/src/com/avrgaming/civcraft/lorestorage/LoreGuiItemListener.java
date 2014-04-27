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

import java.util.HashMap;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;

public class LoreGuiItemListener implements Listener {

	public static HashMap<String, Inventory> guiInventories = new HashMap<String, Inventory>();
	
	
	/*
	 * First phase of inventory click that cancels any
	 * event that was clicked on a gui item.
	 */
	@EventHandler(priority = EventPriority.LOWEST)
	public void OnInventoryClick(InventoryClickEvent event) {	
		if (LoreGuiItem.isGUIItem(event.getCurrentItem()) ||
			LoreGuiItem.isGUIItem(event.getCursor())) {
			event.setCancelled(true);
			
			if (event.getCurrentItem() != null) {
				String action = LoreGuiItem.getAction(event.getCurrentItem());
				if (action != null) {
					LoreGuiItem.processAction(action, event.getCurrentItem(), event);
				}
				return;
			}
			
			if (event.getCursor() != null) {
				String action = LoreGuiItem.getAction(event.getCursor());
				if (action != null) {
					LoreGuiItem.processAction(action, event.getCursor(), event);
				}
				return;
			}
			
		}
	}

	/*
	 * The second phase cancels the event if a non-gui item has been
	 * dropped into a gui inventory.
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void OnInventoryClickSecondPhase(InventoryClickEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		if (event.getRawSlot() < event.getView().getTopInventory().getSize()) {
			if (guiInventories.containsKey(event.getView().getTopInventory().getName())) {
				event.setCancelled(true);
				return;
			}
		} else if (event.isShiftClick()) {
			if (guiInventories.containsKey(event.getView().getTopInventory().getName())) {
				event.setCancelled(true);
				return;
			}			
		}
		
	}
	
	public static boolean isGUIInventory(Inventory inv) {
		return guiInventories.containsKey(inv.getName());
	}
	
	@EventHandler(priority = EventPriority.LOW) 
	public void OnInventoryDragEvent(InventoryDragEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		for (int slot : event.getRawSlots()) {
			if (slot < event.getView().getTopInventory().getSize()) {
				if (guiInventories.containsKey(event.getView().getTopInventory().getName())) {
					event.setCancelled(true);
					return;
				}
			}
		}
		
	}
	
}
