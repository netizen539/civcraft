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
package com.avrgaming.civcraft.util;

import org.bukkit.Location;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Player;
import org.bukkit.inventory.InventoryHolder;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;

public class InventoryHolderStorage {

	/*
	 * An inventory holder can be a 'block'or a player.
	 */
	private Location blockLocation;
	
	private String playerName;
	
	public InventoryHolderStorage(InventoryHolder holder, Location holderLocation) {
		if (holder instanceof Player) {
			Player player = (Player)holder;
			playerName = player.getName();
			blockLocation = null;
		} else {
			blockLocation = holderLocation;
		}
	}
	
	public InventoryHolderStorage(Location blockLoc) {
		blockLocation = blockLoc;
		playerName = null;
	}
	
	public InventoryHolderStorage(Player player) {
		blockLocation = null;
		playerName = player.getName();
	}
	
	public InventoryHolder getHolder() throws CivException {
		if (playerName != null) {
			Player player = CivGlobal.getPlayer(playerName);
			return (InventoryHolder)player;
		} 
		
		if (blockLocation != null) {
			/* Make sure the chunk is loaded. */
			
			if (!blockLocation.getChunk().isLoaded()) {
				if(!blockLocation.getChunk().load()) {
					throw new CivException("Couldn't load chunk at "+blockLocation+" where holder should reside.");
				}
			}
			if (!(blockLocation.getBlock().getState() instanceof Chest)) {
				throw new CivException("Holder location is not a chest, invalid.");
			}
			
			Chest chest = (Chest) blockLocation.getBlock().getState();
			return chest.getInventory().getHolder();
		}
		
		throw new CivException("Invalid holder.");
	}
	
	public void setHolder(InventoryHolder holder) throws CivException {
		if (holder instanceof Player) {
			Player player = (Player)holder;
			playerName = player.getName();
			blockLocation = null;
			return;
		} 
		
		if (holder instanceof Chest) {
			Chest chest = (Chest)holder;
			playerName = null;
			blockLocation = chest.getLocation();
			return;
		} 
		
		if (holder instanceof DoubleChest) {
			DoubleChest dchest = (DoubleChest)holder;
			playerName = null;
			blockLocation = dchest.getLocation();
			return;
		}
		
		throw new CivException("Invalid holder passed to set holder:"+holder.toString());
	}
	
}
