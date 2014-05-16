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
package com.avrgaming.civcraft.items.units;

import java.util.List;

import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;


public class UnitItemMaterial extends LoreMaterial {

	private UnitMaterial parent = null;	
	private int socketSlot = 0;

	public UnitItemMaterial(String id, int minecraftId, short damage) {
		super(id, minecraftId, damage);
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent arg0) {		
	}

	@Override
	public void onBlockDamage(BlockDamageEvent arg0) {		
	}

	@Override
	public void onBlockInteract(PlayerInteractEvent arg0) {		
	}

	@Override
	public void onBlockPlaced(BlockPlaceEvent arg0) {
	}

	@Override
	public void onHit(EntityDamageByEntityEvent arg0) {
		
	}

	@Override
	public void onHold(PlayerItemHeldEvent event) {
	}

	@Override
	public void onInteract(PlayerInteractEvent arg0) {
	}

	@Override
	public void onInteractEntity(PlayerInteractEntityEvent arg0) {
		
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onItemDrop(PlayerDropItemEvent event) {
		CivMessage.sendError(event.getPlayer(), "Cannot drop this item, belongs to the unit you are currently assigned.");
		event.setCancelled(true);
		event.getPlayer().updateInventory();
	}

	@Override
	public void onItemCraft(CraftItemEvent event) {
		try {
			CivMessage.sendError(CivGlobal.getPlayer(event.getWhoClicked().getName()), "Cannot craft with a unit item.");
		} catch (CivException e) {
			//player offline?
		}
		event.setCancelled(true);		
	}

	@Override
	public void onItemPickup(PlayerPickupItemEvent event) {
		// Should never be able to pick up these items.
		event.setCancelled(true);
		event.getItem().remove();
	}

	@Override
	public void onInvItemPickup(InventoryClickEvent event, Inventory fromInv,
			ItemStack stack) {		
	}

	@Override
	public void onInvItemDrop(InventoryClickEvent event, Inventory toInv,
			ItemStack stack) {
			
	}

	@Override
	public void onInvShiftClick(InventoryClickEvent event, Inventory fromInv,
			Inventory toInv, ItemStack stack) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInvItemSwap(InventoryClickEvent event, Inventory toInv,
			ItemStack droppedStack, ItemStack pickedStack) {
		// TODO Auto-generated method stub
		
	}

	public UnitMaterial getParent() {
		return parent;
	}

	public void setParent(UnitMaterial parent) {
		this.parent = parent;
	}

	@Override
	public void onItemSpawn(ItemSpawnEvent event) {
		// Never let these spawn as items.
		event.setCancelled(true);
		
	}
	
	public void setLoreArray(List<String> lore) {
		super.setLore("");
		for (String str : lore) {
			this.addLore(str);
		}
		
		this.addLore(CivColor.Gold+"Soulbound");
	}

	public int getSocketSlot() {
		return socketSlot;
	}

	public void setSocketSlot(int socketSlot) {
		this.socketSlot = socketSlot;
	}

	@Override
	public boolean onAttack(EntityDamageByEntityEvent event, ItemStack stack) {
		return false;
	}

	@Override
	public void onPlayerDeath(EntityDeathEvent event, ItemStack stack) {		
	}

	@Override
	public void onDrop(PlayerDropItemEvent event) {
		
	}
	@Override
	public void onInventoryClose(InventoryCloseEvent event) {
	}
}
