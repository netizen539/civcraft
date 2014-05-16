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

import java.util.HashSet;
import java.util.List;

import org.bukkit.entity.Player;
import org.bukkit.event.Event.Result;
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
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.config.ConfigUnit;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.DelayMoveInventoryItem;
import com.avrgaming.civcraft.util.CivColor;

public class UnitMaterial extends LoreMaterial {
	
	private ConfigUnit unit = null;
	private static final int LAST_SLOT = 8;
	
	public HashSet<Integer> allowedSubslots = new HashSet<Integer>(); 
	
	public UnitMaterial(String id, int itemId, short damage) {
		super(id, itemId, damage);
	}

	public UnitMaterial(String id, ConfigUnit configUnit) {
	//	Material mat = Material.getMaterial(configUnit.item_id);
		super(id, configUnit.item_id, (short)0);
		setUnit(configUnit);
		
		this.setLore("Unit Item");
		this.setName(configUnit.name);
	}

	public ConfigUnit getUnit() {
		return unit;
	}

	public void setUnit(ConfigUnit unit) {
		this.unit = unit;
	}
	
	@Override
	public void onBlockBreak(BlockBreakEvent event) {
	}

	@Override
	public void onBlockDamage(BlockDamageEvent event) {		
	}

	@Override
	public void onBlockInteract(PlayerInteractEvent event) {
		
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onBlockPlaced(BlockPlaceEvent event) {
		event.setCancelled(true);
		CivMessage.sendError(event.getPlayer(), "Cannot place this item");
		event.getPlayer().updateInventory();
	}

	@Override
	public void onHit(EntityDamageByEntityEvent event) {
	}

	@Override
	public void onHold(PlayerItemHeldEvent event) {
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		event.setUseItemInHand(Result.DENY);
		//if (event.getClickedBlock() == null) {
			event.setCancelled(true);
			CivMessage.sendError(event.getPlayer(), "Cannot use this item.");
		//}
	}

	@Override
	public void onInteractEntity(PlayerInteractEntityEvent event) {
	}
	
	@Override
	public void onItemDrop(PlayerDropItemEvent event) {
		this.onItemFromPlayer(event.getPlayer(), event.getItemDrop().getItemStack());
	}
	
	protected void removeChildren(Inventory inv) {
		for (ItemStack stack : inv.getContents()) {
			if (stack != null) {
			//	CustomItemStack is = new CustomItemStack(stack);
				LoreMaterial material = LoreMaterial.getMaterial(stack);
				if (material != null && (material instanceof UnitItemMaterial)) {
					UnitItemMaterial umat = (UnitItemMaterial)material;
					if (umat.getParent() == this) {
						inv.remove(stack);
					}
				}
			}
		}
	}
	
	
	private static List<String> stripTownLore(List<String> lore) {
		for (String str : lore) {
			if (str.startsWith("Town:")) {
				lore.remove(str);
				break;
			}
		}
		return lore;
	}
	
	public static void setOwningTown(Town town, ItemStack stack) {
		if (town == null) {
			return;
		}
		
		ItemMeta meta = stack.getItemMeta();
		if (meta != null && meta.hasLore()) {
			List<String> lore = meta.getLore();
			
			lore = stripTownLore(lore);
			
			if (lore != null) {
				lore.add("Town:"+town.getName()+" "+CivColor.Black+town.getId());
			}
			
			meta.setLore(lore);
			stack.setItemMeta(meta);
		}	
	}

	public static Town getOwningTown(ItemStack stack) {
		ItemMeta meta = stack.getItemMeta();
		if (meta == null || !meta.hasLore()) {
			return null;
		}
		
		String loreLine = null;
		List<String> lore = meta.getLore();
		for (String str : lore) {
			if (str.startsWith("Town:")) {
				loreLine = str;
				break;
			}
		}
		
		if (loreLine == null) {
			return null;
		}
		
		try {
			String[] split = loreLine.split(CivColor.Black);
			int townId = Integer.valueOf(split[1]);
			
			return CivGlobal.getTownFromId(townId);
		} catch (Exception e) {
			e.printStackTrace();
			return null;
		}
		
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

	@SuppressWarnings("deprecation")
	@Override
	public void onItemPickup(PlayerPickupItemEvent event) {
				
		if(!validateUnitUse(event.getPlayer(), event.getItem().getItemStack())) {
			CivMessage.sendErrorNoRepeat(event.getPlayer(), "You cannot use this unit because it does not belong to your civilization.");
			event.setCancelled(true);
			return;
		}
		
		ConfigUnit unit = Unit.getPlayerUnit(event.getPlayer());
		if (unit != null) {
			CivMessage.sendErrorNoRepeat(event.getPlayer(), "Already a "+unit.name+" cannot pickup another unit item.");
			event.setCancelled(true);
		} else {
			// Reposition item to the last quickbar slot
			
			// Check that the inventory is not full, clear out the
			// the required slot, and then re-add what was in there.
			Inventory inv = event.getPlayer().getInventory();
			
			ItemStack lastSlot = inv.getItem(LAST_SLOT);
			if (lastSlot != null) {
				inv.setItem(LAST_SLOT, event.getItem().getItemStack());
				inv.addItem(lastSlot);
				event.getPlayer().updateInventory();
			} else {
				inv.setItem(LAST_SLOT, event.getItem().getItemStack());
			}
			
			
			this.onItemToPlayer(event.getPlayer(), event.getItem().getItemStack());
			event.getItem().remove();
			event.setCancelled(true);
		}
	}

	public static boolean validateUnitUse(Player player, ItemStack stack) {
		if (stack == null) {
			return true;
		}
		
		Resident resident = CivGlobal.getResident(player);
		Town town = getOwningTown(stack);
		
		
		if (town == null) {
			return true;
		}
		
		if (town.getCiv() != resident.getCiv()) {
			return false;
		}
		
		return true;
	}

	public int getFreeSlotCount(Inventory inv) {
		int count = 0;
		for (ItemStack stack : inv.getContents()) {
			if (stack == null) {
				count++;
			}
		}
		return count;
	}
	
	public boolean hasFreeSlot(Inventory inv) {
		for (ItemStack stack : inv.getContents()) {
			if (stack == null) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void onInvItemPickup(InventoryClickEvent event,
			Inventory fromInv, ItemStack stack) {

		if (fromInv.getHolder() instanceof Player) {
			Player player = (Player)fromInv.getHolder();			
			onItemFromPlayer(player, stack);
		}
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onInvItemDrop(InventoryClickEvent event,
			Inventory toInv, ItemStack stack) {
		
		if (toInv.getHolder() instanceof Player) {
			//A hack to make sure we are always moving the item to the player's inv.
			//A player inv is always on the bottom, toInv could be the 'crafting' inv 
			toInv = event.getView().getBottomInventory();
			Player player = (Player)toInv.getHolder();
				
			if(!validateUnitUse(player, stack)) {
				CivMessage.sendError(player, "You cannot use this unit because it does not belong to your civlization.");
				event.setCancelled(true);
				return;
			}
			
			//Prevent dropping in two unit materials.
			ConfigUnit unit = Unit.getPlayerUnit(player);
			if (unit != null) {
				//player already has a unit item, cancel this event.
				CivMessage.sendError(player, "You already are a "+unit.name+" cannot pickup another unit item.");
				event.setCancelled(true);
				event.setResult(Result.DENY);
				event.getView().close();
				player.updateInventory();
				return;
			}
			
			// Reposition item to the last quickbar slot
			if (event.getSlot() != LAST_SLOT) {
				
				DelayMoveInventoryItem task = new DelayMoveInventoryItem();
				task.fromSlot = event.getSlot();
				task.toSlot = LAST_SLOT;
				task.inv = toInv;
				task.playerName = player.getName();
				TaskMaster.syncTask(task);
			}

			onItemToPlayer(player, stack);
		}
	
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onInvShiftClick(InventoryClickEvent event,
			Inventory fromInv, Inventory toInv,
			ItemStack stack) {
		
		if (fromInv.equals(toInv)) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			return;
		}
		
		if (toInv.getHolder() instanceof Player) {
			Player player = (Player)toInv.getHolder();

			if(!validateUnitUse(player, stack)) {
				CivMessage.sendError(player, "You cannot use this unit because it does not belong to your civlization.");
				event.setCancelled(true);
				return;
			}
			
			//Prevent dropping in two unit materials.
			ConfigUnit unit = Unit.getPlayerUnit(player);
			if (unit != null) {
				//player already has a unit item, cancel this event.
				CivMessage.sendError(player, "You already are a "+unit.name+" cannot pickup another unit item.");
				event.setCancelled(true);
				event.setResult(Result.DENY);
				event.getView().close();
				player.updateInventory();
				return;
			}

			
			onItemToPlayer(player, stack);
		} else if (fromInv.getHolder() instanceof Player) {
			onItemFromPlayer((Player)fromInv.getHolder(), stack);
		}
		
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onInvItemSwap(InventoryClickEvent event, Inventory toInv,
			ItemStack droppedStack, ItemStack pickedStack) {
		
		// Prevent stacking items
		if (droppedStack.getTypeId() == pickedStack.getTypeId()) {
			event.setCancelled(true);
			event.setResult(Result.DENY);
			Player player = getPlayer(event);
			player.updateInventory();
			
		}
		
		if (toInv.getHolder() instanceof Player) {
			//CustomItemStack is = new CustomItemStack(droppedStack);
			LoreMaterial material = LoreMaterial.getMaterial(droppedStack);
			
			if (material != null && (material instanceof UnitMaterial)) {
				Player player = (Player)toInv.getHolder();
				
				if(!validateUnitUse(player, droppedStack)) {
					CivMessage.sendError(player, "You cannot use this unit because it does not belong to your civlization.");
					event.setCancelled(true);
					return;
				}
				
				DelayMoveInventoryItem task = new DelayMoveInventoryItem();
				task.fromSlot = event.getSlot();
				task.toSlot = LAST_SLOT;
				task.inv = toInv;
				task.playerName = player.getName();
				TaskMaster.syncTask(task);
			
				onItemToPlayer(player, droppedStack);
				onItemFromPlayer(player, pickedStack);
			}
		}
	}
	
	/*
	 * Called when a unit material is added to a player.
	 */
	public void onItemToPlayer(Player player, ItemStack stack) {
	}

	/*
	 * Called when a unit material is removed from a player.
	 */
	public void onItemFromPlayer(Player player, ItemStack stack) {
	
	}

	@Override
	public void onItemSpawn(ItemSpawnEvent event) {
		
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
		ItemStack foundStack = null;
		for (ItemStack stack : event.getPlayer().getInventory().getContents()) {
			if (stack == null) {
				continue;
			}
			
			if (LoreMaterial.isCustom(stack)) {
				if (LoreMaterial.getMaterial(stack) instanceof UnitMaterial) {
					if (foundStack == null) {
						foundStack = stack;
					} else {
						event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), stack);
						event.getPlayer().getInventory().remove(stack);
					}
				}
			}
			
		}
	}

}

