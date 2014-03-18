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
package com.avrgaming.civcraft.listener;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.util.ItemManager;

public class MarkerPlacementManager implements Listener {

	private static HashMap<String, Structure> playersInPlacementMode = new HashMap<String, Structure>();
	private static HashMap<String, ArrayList<Location>> markers = new HashMap<String, ArrayList<Location>>();
	
	
	public static void addToPlacementMode(Player player, Structure structure, String markerName) throws CivException {

		if (player.getItemInHand() != null && ItemManager.getId(player.getItemInHand()) != CivData.AIR) {
			throw new CivException("You must not be holding anything to enter placement mode.");
		}
		
		playersInPlacementMode.put(player.getName(), structure);
		markers.put(player.getName(), new ArrayList<Location>());
		
		ItemStack stack = ItemManager.createItemStack(CivData.REDSTONE_TORCH_OFF, 2);
		ItemMeta meta = stack.getItemMeta();
		if (markerName != null) {
			meta.setDisplayName(markerName);
		} else {
			meta.setDisplayName("Marker");
		}
		stack.setItemMeta(meta);
		player.setItemInHand(stack);
		
		CivMessage.send(player, "You're now in placement mode for a "+structure.getDisplayName());
	}
	
	public static void removeFromPlacementMode(Player player, boolean canceled) {
		if (canceled) {
			Structure struct = playersInPlacementMode.get(player.getName());
			struct.getTown().removeStructure(struct);
			CivGlobal.removeStructure(struct);
		}
		playersInPlacementMode.remove(player.getName());
		markers.remove(player.getName());
		player.setItemInHand(ItemManager.createItemStack(CivData.AIR, 1));
		CivMessage.send(player, "You're no longer in placement mode.");
	}
	
	public static boolean isPlayerInPlacementMode(Player player) {
		return isPlayerInPlacementMode(player.getName());
	}
	
	public static boolean isPlayerInPlacementMode(String name) {
		return playersInPlacementMode.containsKey(name);
	}
	
	public static void setMarker(Player player, Location location) throws CivException {
		ArrayList<Location> locs = markers.get(player.getName());

		Structure struct = playersInPlacementMode.get(player.getName());
		int amount = player.getItemInHand().getAmount();
		if (amount == 1) {
			player.setItemInHand(null);
		} else {
			player.getItemInHand().setAmount((amount -1));
		}
		
		locs.add(location);
		struct.onMarkerPlacement(player, location, locs);
					
	}
	
	@EventHandler(priority = EventPriority.MONITOR) 
	public void OnItemHeldChange(PlayerItemHeldEvent event) {
		if (isPlayerInPlacementMode(event.getPlayer())) {
			removeFromPlacementMode(event.getPlayer(), true);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR) 
	public void OnPlayerDropItemEvent(PlayerDropItemEvent event) {
		if (isPlayerInPlacementMode(event.getPlayer())) {
			event.setCancelled(true);
			removeFromPlacementMode(event.getPlayer(), true);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR) 
	public void OnPlayerQuit(PlayerQuitEvent event) {
		if (isPlayerInPlacementMode(event.getPlayer())) {
			removeFromPlacementMode(event.getPlayer(), true);
		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR) 
	public void OnInventoryClick(InventoryClickEvent event) {
		Player player;
		try {
			player = CivGlobal.getPlayer(event.getWhoClicked().getName());
		} catch (CivException e) {
			//Not a valid player or something, forget it.
			return;
		}

		if (isPlayerInPlacementMode(player)) {
			removeFromPlacementMode(player, true);
		}
	}
	
}
