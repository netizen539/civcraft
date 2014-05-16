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

import java.util.Map.Entry;

import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Item;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;
import org.bukkit.event.Cancellable;
import org.bukkit.event.Event;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.ItemDespawnEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.InventoryView;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.items.units.UnitItemMaterial;
import com.avrgaming.civcraft.items.units.UnitMaterial;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.util.ItemManager;

public class BonusGoodieManager implements Listener {

	/*
	 * Keeps track of the location of bonus goodies through various events.
	 * Will also repo goodies back to the trade outposts if they get destroyed.
	 */
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void OnItemHeldChange(PlayerItemHeldEvent event) {
		Inventory inv = event.getPlayer().getInventory();
		ItemStack stack = inv.getItem(event.getNewSlot());
		
		BonusGoodie goodie = CivGlobal.getBonusGoodie(stack);
		if (goodie == null) {
			return;
		}
		
		CivMessage.send(event.getPlayer(), CivColor.Purple+"Bonus Goodie: "+CivColor.Yellow+goodie.getDisplayName());
		
	}
	
	/*
	 * When a Bonus Goodie despawns, Replenish it at the outpost.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void onItemDespawn(ItemDespawnEvent event) {
		BonusGoodie goodie = CivGlobal.getBonusGoodie(event.getEntity().getItemStack());
		if (goodie == null) {
			return;
		}
		
		goodie.replenish(event.getEntity().getItemStack(), event.getEntity(), null, null);	
	}
	
	/*
	 * When a player leaves, drop item on the ground.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void OnPlayerQuit(PlayerQuitEvent event) {				
		for (ItemStack stack : event.getPlayer().getInventory().getContents()) {
			BonusGoodie goodie = CivGlobal.getBonusGoodie(stack);
			if (goodie != null) {
				event.getPlayer().getInventory().remove(stack);
				event.getPlayer().getWorld().dropItemNaturally(event.getPlayer().getLocation(), stack);
			}
		}		
	}
	
	/*
	 * When the chunk unloads, replenish it at the outpost
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void OnChunkUnload(ChunkUnloadEvent event) {
		
		BonusGoodie goodie;
		
		for (Entity entity : event.getChunk().getEntities()) {
			if (!(entity instanceof Item)) {
				continue;
			}
			
			goodie = CivGlobal.getBonusGoodie(((Item)entity).getItemStack());
			if (goodie == null) {
				continue;
			}
			
			goodie.replenish(((Item)entity).getItemStack(), (Item)entity, null, null);
		}
	}
	
	/*
	 * If the item combusts in lava or fire, replenish it at the outpost.
	 */
	@EventHandler(priority = EventPriority.MONITOR)
	public void OnEntityCombustEvent(EntityCombustEvent event) {
		if (!(event.getEntity() instanceof Item)) {
			return;
		}
		
		BonusGoodie goodie = CivGlobal.getBonusGoodie(((Item)event.getEntity()).getItemStack());
		if (goodie == null) {
			return;
		}
		
		goodie.replenish(((Item)event.getEntity()).getItemStack(), (Item)event.getEntity(), null, null);
		
	}
	
	/*
	 * Track the location of the goodie.
	 */
	@EventHandler(priority = EventPriority.LOWEST) 
	public void OnInventoryClick(InventoryClickEvent event) {
		
		BonusGoodie goodie;
		ItemStack stack;
		
		if (event.isShiftClick()) {
			stack = event.getCurrentItem();
			goodie = CivGlobal.getBonusGoodie(stack);
		} else {
			stack = event.getCursor();
			goodie = CivGlobal.getBonusGoodie(stack);
		}
		
		if (goodie == null) {
			return;
		}
			
		InventoryView view = event.getView();
		int rawslot = event.getRawSlot();
		boolean top = view.convertSlot(rawslot) == rawslot;
		if (event.isShiftClick()) {
			top = !top;
		}
		
		InventoryHolder holder;
		if (top) {
			holder = view.getTopInventory().getHolder();
		} else {
			holder = view.getBottomInventory().getHolder();
		}
		
		boolean isAllowedHolder = (holder instanceof Chest) || (holder instanceof DoubleChest) || (holder instanceof Player);
		
		if ((holder == null) || !isAllowedHolder) {
			
			event.setCancelled(true);
			event.setResult(Event.Result.DENY);
			
			Player player;
			try {
				player = CivGlobal.getPlayer(event.getWhoClicked().getName());
				CivMessage.sendError(player, "Cannot move bonus goodie into this container.");			
			} catch (CivException e) {
				//player not found or not online.
			}
			
			// if we are not doing a shift-click close it to hide client
			// bug showing the item in the inventory even though its not
			// there.
			if (event.isShiftClick() == false) {
				view.close();
			}
			
			return;
		}
			
		if (goodie.getHolder() != holder) {
			try {
				goodie.setHolder(holder);
				goodie.update(false);
				goodie.updateLore(stack);
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
		
	}
	
	/*
	 * Track the location of the goodie if it spawns as an item.
	 */
	@EventHandler(priority = EventPriority.MONITOR) 
	public void OnItemSpawn(ItemSpawnEvent event) {
		Item item = event.getEntity();
		
		BonusGoodie goodie = CivGlobal.getBonusGoodie(item.getItemStack());
		if (goodie == null) {
			return;
		}
		
		// Cant validate here, validate in drop item events...
		goodie.setItem(item);
		try {
			goodie.update(false);
			goodie.updateLore(item.getItemStack());
		} catch (CivException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Validate that the item dropped was a valid goodie
	 */
	@EventHandler(priority = EventPriority.MONITOR) 
	public void OnPlayerDropItemEvent(PlayerDropItemEvent event) {
		
		BonusGoodie goodie = CivGlobal.getBonusGoodie(event.getItemDrop().getItemStack());
		if (goodie == null) {
			return;
		}
		
		// Verify that the player dropping this item is in fact our holder.
	//	goodie.validateItem(event.getItemDrop().getItemStack(), null, event.getPlayer(), null);
		
	}
	
	/*
	 * Track the location of the goodie if a player picks it up.
	 */
	@EventHandler(priority = EventPriority.MONITOR) 
	public void OnPlayerPickupItem(PlayerPickupItemEvent event) {
		BonusGoodie goodie = CivGlobal.getBonusGoodie(event.getItem().getItemStack());
		
		if (goodie == null) {
			return;
		}
		
		try {
			goodie.setHolder(event.getPlayer());
			goodie.update(false);
			goodie.updateLore(event.getItem().getItemStack());
		} catch (CivException e) {
			e.printStackTrace();
		}
	}
	
	/* 
	 * Track the location of the goodie if a player places it in an item frame.
	 */
	@EventHandler(priority = EventPriority.LOW) 
	public void OnPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		
		if (!(event.getRightClicked() instanceof ItemFrame)) {
			return;
		}
		
		LoreMaterial material = LoreMaterial.getMaterial(event.getPlayer().getItemInHand());
		if (material != null) {
			if (material instanceof UnitItemMaterial ||
					material instanceof UnitMaterial) {
				//Do not allow subordinate items into the frame.
				CivMessage.sendError(event.getPlayer(), "You cannot place this item into an itemframe.");
				return;
			}
		}
		
		BonusGoodie goodie = CivGlobal.getBonusGoodie(event.getPlayer().getItemInHand());
		ItemFrame frame = (ItemFrame)event.getRightClicked();
		ItemFrameStorage frameStore = CivGlobal.getProtectedItemFrame(frame.getUniqueId());
		
		if (goodie == null) {
			/* No goodie item, make sure they dont go into protected frames. */
			if (frameStore != null) {
				/* Make sure we're trying to place an item into the frame, test if the frame is empty. */
				if (frame.getItem() == null || ItemManager.getId(frame.getItem()) == CivData.AIR) {
					CivMessage.sendError(event.getPlayer(), 
							"You cannot place a non-trade goodie items in a trade goodie item frame.");
					event.setCancelled(true);
					return;
				}
			}
		} else {
			/* Trade goodie, make sure they only go into protect frames. */
			if (frameStore == null) {
				CivMessage.sendError(event.getPlayer(),
						"You cannot place a trade gooide in a non-trade goodie item frame.");
				event.setCancelled(true);
				return;
			}
		}
		
		if (frameStore != null) {
			onPlayerProtectedFrameInteract(event.getPlayer(), frameStore, goodie, event);
			event.setCancelled(true);
		}
		
		
//		BonusGoodie goodie = CivGlobal.getBonusGoodie(event.getPlayer().getItemInHand());
//		ItemFrame frame = (ItemFrame)event.getRightClicked();
//		if (frame.getItem() == null || frame.getItem().getType() == Material.AIR) {
//			
//			ItemFrameStorage frameStore = CivGlobal.getProtectedItemFrame(frame.getUniqueId());
//			
//			
//			if (frameStore == null && goodie != null) {
//				CivMessage.sendError(event.getPlayer(), "You cannot place a goodie in a non-protected item frame.");
//				event.setCancelled(true);
//				return;
//			}
//			
//			if (frameStore == null) {
//				/* non protected item frame, allow placement. */
//				return;
//			}
//			
//			if (goodie == null && event.getPlayer().getItemInHand().getTypeId() != CivData.AIR) {
//				CivMessage.sendError(event.getPlayer(), "You cannot place non-trade goodie items in a trade goodie item frame.");
//				event.setCancelled(true);
//				return;
//			}
//			
//			if (goodie == null) {
//				event.setCancelled(true);
//				return;
//			}
//			
//			Resident resident = CivGlobal.getResident(event.getPlayer());
//			if (resident == null || !resident.hasTown() || resident.getCiv() != frameStore.getTown().getCiv()) {
//				CivMessage.sendError(event.getPlayer(), "You don't have permission to add trade goodies to this item frame.");
//				event.setCancelled(true);
//				return;
//			}
//			
//			//CivGlobal.checkForEmptyDuplicateFrames(frameStore);
//			
//			event.setCancelled(true);
//						
//			/* 
//			 * Move the item manually. Creative mode keeps a copy in your inv, we want both
//			 * survival and creative to use the same code, so just cancel the event and 
//			 * do it ourselves.
//			 */
//			ItemStack stack = event.getPlayer().getItemInHand();
//			frameStore.setItem(stack);
//			
//			if (stack.getAmount() > 1) {
//				stack.setAmount(stack.getAmount() - 1);
//			} else {
//				event.getPlayer().getInventory().remove(stack);
//			}
//			
//			frameStore.getTown().onGoodiePlaceIntoFrame(frameStore, goodie);
//			
//			goodie.setFrame(frameStore);
//			try {
//				goodie.update(false);
//				goodie.updateLore(stack);
//			} catch (CivException e) {
//				e.printStackTrace();
//			}
//
//		}
	}
	
	@EventHandler(priority = EventPriority.MONITOR) 
	public void OnPlayerJoinEvent(PlayerJoinEvent event) {
		Inventory inv = event.getPlayer().getInventory();
		
		for (ConfigTradeGood good : CivSettings.goods.values()) {
			for (Entry<Integer, ? extends ItemStack> itemEntry : inv.all(ItemManager.getMaterial(good.material)).entrySet()) {
				if (good.material_data != ItemManager.getData(itemEntry.getValue())) {
					continue;
				}
				
				BonusGoodie goodie = CivGlobal.getBonusGoodie(itemEntry.getValue());
				if (goodie != null) {
					inv.remove(itemEntry.getValue());				
				}
			}
		}
	}
	
	public static void onPlayerProtectedFrameInteract(Player player, ItemFrameStorage clickedFrame,
			BonusGoodie goodie, Cancellable event) {
		
		Resident resident = CivGlobal.getResident(player);
		if (resident == null || !resident.hasTown() || resident.getCiv() != clickedFrame.getTown().getCiv()) {
			CivMessage.sendError(player, 
					"You must be a member of the owner civ to socket or unsocket trade goodies.");
			event.setCancelled(true);
			return;
		}
		
		if (!clickedFrame.getTown().getMayorGroup().hasMember(resident) && 
				!clickedFrame.getTown().getAssistantGroup().hasMember(resident)) {
			CivMessage.sendError(player, 
					"You must be a mayor or assistant in order to socket or unsocket trade goodies.");
			event.setCancelled(true);
			return;
		}
		
		
		ItemFrame frame = clickedFrame.getItemFrame();
		ItemStack stack = frame.getItem();
		//if goodie in frame, break it out
		if (stack != null && ItemManager.getId(stack) != CivData.AIR) {
			// FYI sometimes the item pops out from the player entity interact event...
			BonusGoodie goodieInFrame = CivGlobal.getBonusGoodie(frame.getItem());
			if (goodieInFrame != null) {
				clickedFrame.getTown().onGoodieRemoveFromFrame(clickedFrame, goodieInFrame);
				
				try {
					goodieInFrame.setFrame(clickedFrame);
					goodieInFrame.update(false);
				} catch (CivException e) {
					e.printStackTrace();
				}
			}
			
			player.getWorld().dropItemNaturally(frame.getLocation(), stack);
			frame.setItem(ItemManager.createItemStack(CivData.AIR, 1));
			CivMessage.send(player, CivColor.LightGray+"You unsocket the trade goodie from the frame.");
		} else if (goodie != null) {
			//Item frame was empty, add goodie to it.
			frame.setItem(player.getItemInHand());
			player.getInventory().remove(player.getItemInHand());
			CivMessage.send(player, CivColor.LightGray+"You socket the trade goodie into the frame");
			clickedFrame.getTown().onGoodiePlaceIntoFrame(clickedFrame, goodie);
			
			try {
				goodie.setFrame(clickedFrame);
				goodie.update(false);
			} catch (CivException e) {
				e.printStackTrace();
			}
			
		}
		
		
	}
	
	
	/*
	 * Prevent the player from using items that are actually trade goodies.
	 */
	@EventHandler(priority = EventPriority.LOW)
	public void OnPlayerInteractEvent(PlayerInteractEvent event) {
		
		ItemStack item = event.getPlayer().getItemInHand();
		BonusGoodie goodie = CivGlobal.getBonusGoodie(item);
		if (goodie == null) {
			return;
		}

		if (event.getClickedBlock() == null) {
			event.setCancelled(true);
			return;
		}
		
		BlockCoord bcoord = new BlockCoord(event.getClickedBlock());		
		ItemFrameStorage clickedFrame = ItemFrameStorage.attachedBlockMap.get(bcoord);
		
		if (clickedFrame != null) {
			if (clickedFrame.getItemFrame() != null) {
				if (clickedFrame.getItemFrame().getAttachedFace().getOppositeFace() ==
						event.getBlockFace()) {
					onPlayerProtectedFrameInteract(event.getPlayer(), clickedFrame,	goodie, event);
					event.setCancelled(true);
				}
			}
			return;
		}
		
		if (event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) {				
			CivMessage.sendError(event.getPlayer(), "Cannot use bonus goodie as a normal item.");
			event.setCancelled(true);
			return;
		}
	}
	
	/*
	 * Prevent the player from using goodies in crafting recipies.
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnCraftItemEvent(CraftItemEvent event) {
		Player player;
		try {
			player = CivGlobal.getPlayer(event.getWhoClicked().getName());
		} catch (CivException e) {
			e.printStackTrace();
			return;
		}
		
		for (ItemStack stack : event.getInventory().getMatrix()) {
			BonusGoodie goodie = CivGlobal.getBonusGoodie(stack);
			if (goodie != null) {
				CivMessage.sendError(player, "Cannot use bonus goodies in a crafting recipe.");
				event.setCancelled(true);
			}
		}
	}
}