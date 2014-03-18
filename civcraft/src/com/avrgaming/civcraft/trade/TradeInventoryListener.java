package com.avrgaming.civcraft.trade;

import java.util.HashMap;
import java.util.LinkedList;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryDragEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class TradeInventoryListener implements Listener {

	public static HashMap<String, TradeInventoryPair> tradeInventories = new HashMap<String, TradeInventoryPair>();
	
	public static final int OTHERS_SLOTS_START = 1*9;
	public static final int OTHERS_SLOTS_END = 2*9;
	public static final int MY_SLOTS_START = 3*9;
	public static final int MY_SLOTS_END = 4*9;
	public static final int SLOTS_END = 5*9;
	public static final int MY_SLOT_BUTTON = 44;
	public static final int OTHER_SLOT_BUTTON = 8;
	
	public static final int MY_COINS_DOWN = 9*4;
	public static final int MY_COINS_UP = (9*4) + 1;
	public static final int MY_COIN_OFFER = 43;
	public static final int OTHER_COIN_OFFER = 7;
	
	public static String getTradeInventoryKey(Resident resident) {
		return resident.getName()+":inventroy";
	}
		
	public class SyncInventoryChange implements Runnable {
		int sourceSlot;
		int destSlot;
		
		Inventory sourceInventory;
		Resident otherResident;
		Inventory otherInventory;
		
		public SyncInventoryChange(int sourceSlot, int destSlot, Inventory sourceInventory, Resident otherResident, Inventory otherInventory) {
			this.sourceInventory = sourceInventory;
			this.sourceSlot = sourceSlot;
			this.destSlot = destSlot;
			this.otherResident = otherResident;
			this.otherInventory = otherInventory;
		}
		
		@Override
		public void run() {
			try {
				Player otherPlayer = CivGlobal.getPlayer(otherResident);
				if (otherPlayer.getOpenInventory() != otherInventory) {
					return;
				}
				
				if (otherInventory != null) {
					otherInventory.setItem(destSlot, sourceInventory.getItem(sourceSlot));
				}
				
			} catch (CivException e) {
			}			
		}
	}
	
	public class SyncInventoryChangeAll implements Runnable {
		Inventory sourceInventory;
		Resident otherResident;
		Inventory otherInventory;
		
		public SyncInventoryChangeAll(Inventory src, Resident other, Inventory otherInv) {
			this.sourceInventory = src;
			this.otherResident = other;
			this.otherInventory = otherInv;
		}

		@Override
		public void run() {
			try {
				Player otherPlayer = CivGlobal.getPlayer(otherResident);
				if (otherPlayer.getOpenInventory() != this.otherInventory) {
					return;
				}
				
				if (otherInventory != null) {
					int k = OTHERS_SLOTS_START;
					for (int i = MY_SLOTS_START; i < MY_SLOTS_END; i++) {
						otherInventory.setItem(k, sourceInventory.getItem(i));
						k++;
					}
				}
			} catch (CivException e) {
			}
		}
	}
	
	
	public boolean handleSlotChange(int slot, TradeInventoryPair pair) {
	
		/* Prevent user from clicking in the other player's spot. */
		if (slot <= OTHERS_SLOTS_END) {
			return false;
		}
		
		/* Update the other inventory */
		if ((slot >= MY_SLOTS_START) && (slot <= MY_SLOTS_END)) {
			int relativeSlot = slot % 9;
			TaskMaster.syncTask(new SyncInventoryChange(slot, OTHERS_SLOTS_START + relativeSlot, pair.inv, pair.otherResident, pair.otherInv));				
		}
		
		return true;
	}
	
	public void handleShiftClick(InventoryClickEvent event, Player player, TradeInventoryPair pair) {
		/* First determine if we're shifting into our inv or into the trade window. */
		if (event.getRawSlot() > SLOTS_END) {
			/* We're clicking in our inventory, trying to bring in an item. 
			 * lets cheat by creating a new temp inventory which contains the
			 * current contents of our slots, add this item to it, then replace
			 * the slots with that inv's contents. 
			 */
			Inventory tempInv = Bukkit.createInventory(event.getWhoClicked(), 9);
			/* Copy contents from current slots. */
			int k = 0;
			for (int i = MY_SLOTS_START; i < MY_SLOTS_END; i++) {
				tempInv.setItem(k, event.getInventory().getItem(i));;
				k++;
			}
			
			/* Add this item to our tempInv. */
			HashMap<Integer, ItemStack> leftovers = tempInv.addItem(event.getCurrentItem());
			
			/* Copy contents of the temp inventory on top of our slots. */
			k = 0;
			for (int i = MY_SLOTS_START; i < MY_SLOTS_END; i++) {
				event.getInventory().setItem(i, tempInv.getItem(k));
				k++;
			}
			
			/* Now, remove the item from the player's inventory. */
			player.getInventory().setItem(event.getSlot(), null);
			
			/* Re-add any leftovers we might have. */
			for (ItemStack stack : leftovers.values()) {
				player.getInventory().addItem(stack);
			}
			
			/* Cancel normal event processing. */
			TaskMaster.syncTask(new SyncInventoryChangeAll(pair.inv, pair.otherResident, pair.otherInv));				
			event.setCancelled(true);
			return;
		} else {
			/* We're clicking in the trade inventory, tryign to take out an item. */
			if (event.getRawSlot() < OTHERS_SLOTS_END) {
				/* We tried to shift click on the other player's side. Cancel. */
				event.setCancelled(true);
				return;
			} else {
				/* We're clicking on our side, allow it as normal. */
				TaskMaster.syncTask(new SyncInventoryChangeAll(pair.inv, pair.otherResident, pair.otherInv));		
				return;
			}
		}
	}
	
	private void handleDoubleClick(InventoryClickEvent event, Player player, TradeInventoryPair pair) {
		/* If we've double clicked anywhere at all, just update the inventory to reflect the changes. */
		TaskMaster.syncTask(new SyncInventoryChangeAll(pair.inv, pair.otherResident, pair.otherInv));	
	}
	
	
	private void handleCoinChange(TradeInventoryPair pair, TradeInventoryPair otherPair, double change) {
		
		if (change > 0) {
			/* We're adding coins, so lets check that we have enough coins on our person. */
			if (pair.resident.getTreasury().getBalance() < (pair.coins + change)) {
				pair.coins = pair.resident.getTreasury().getBalance();
				otherPair.otherCoins = pair.coins;
			} else {
				/* We've got enough, so add it for now. */
				pair.coins += change;
				otherPair.otherCoins = pair.coins;
			}
		} else {
			/* We're removing coins. */
			change *= -1; /* flip sign on change so we can make sense of things */
			if (change > pair.coins) {
				/* Remove all the offered coins. */
				pair.coins = 0;
				otherPair.otherCoins = 0;
			} else {
				/* change is negative, so just add. */
				pair.coins -= change;
				otherPair.otherCoins = pair.coins;
			}
		}
		
		/* Update our display item. */
		ItemStack guiStack;
		if (pair.coins == 0) {
			guiStack = LoreGuiItem.build("Coins Offered", 
					ItemManager.getId(Material.NETHER_BRICK_ITEM), 0, 
					CivColor.Yellow+"0 Coins");
		} else {
			guiStack = LoreGuiItem.build("Coins Offered", 
					ItemManager.getId(Material.GOLD_INGOT), 0, 
					CivColor.Yellow+pair.coins+" Coins");
		}
		pair.inv.setItem(MY_COIN_OFFER, guiStack);
		
		/* Update our offerings in the other's inventory. */
		otherPair.inv.setItem(OTHER_COIN_OFFER, guiStack);
		
	}
	
	
	public void markTradeValid(TradeInventoryPair pair) {
		pair.valid = true;
		ItemStack guiStack = LoreGuiItem.build("Your Confirm", 
				CivData.WOOL, CivData.DATA_WOOL_GREEN, 
				CivColor.Gold+"<Click to Unconfirm Trade>");
		pair.inv.setItem(MY_SLOT_BUTTON, guiStack);
		
		 guiStack = LoreGuiItem.build("Your Confirm", 
					CivData.WOOL, CivData.DATA_WOOL_GREEN, 
					CivColor.LightBlue+pair.otherResident.getName()+CivColor.LightGreen+" has confirmed the trade.");
		pair.otherInv.setItem(OTHER_SLOT_BUTTON, guiStack);

	}
	
	public void markTradeInvalid(TradeInventoryPair pair) {
		pair.valid = false;
		ItemStack guiStack = LoreGuiItem.build("Your Confirm", 
				CivData.WOOL, CivData.DATA_WOOL_RED, 
				CivColor.Gold+"<Click to Confirm Trade>");
		
		pair.inv.setItem(MY_SLOT_BUTTON, guiStack);
		
		ItemStack guiStack2 = LoreGuiItem.build(pair.otherResident.getName()+" Confirm", 
				CivData.WOOL, CivData.DATA_WOOL_RED, 
				CivColor.LightGreen+"Waiting for "+CivColor.LightBlue+pair.otherResident.getName(),
				CivColor.LightGray+"to confirm this trade.");
		pair.otherInv.setItem(OTHER_SLOT_BUTTON, guiStack2);
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOW)
	public void onInventoryClickEvent(InventoryClickEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		Player player = (Player)event.getWhoClicked();
		Resident resident = CivGlobal.getResident(player);
		TradeInventoryPair pair = tradeInventories.get(getTradeInventoryKey(resident));
		if (pair == null) {
			return;
		}
		TradeInventoryPair otherPair = tradeInventories.get(getTradeInventoryKey(pair.otherResident));
		if (otherPair == null) {
			return;
		}
		
		Inventory savedTradeInventory = pair.inv;
		if (savedTradeInventory == null) {
			return;
		}
		
		if (!savedTradeInventory.getName().equals(event.getInventory().getName())) {
			return;
		}
	
		/* Check to see if we've clicked on a button. */
		if (event.getRawSlot() == MY_SLOT_BUTTON) {
			ItemStack button = event.getInventory().getItem(MY_SLOT_BUTTON);
			if (ItemManager.getData(button) == CivData.DATA_WOOL_RED) {
				/* Mark trade as valid. */
				markTradeValid(pair);
				
				if (pair.valid && otherPair.valid) {
					try {
						completeTransaction(pair, otherPair);
					} catch (CivException e) {
						e.printStackTrace();
						CivMessage.sendError(pair.resident, e.getMessage());
						CivMessage.sendError(pair.otherResident, e.getMessage());
						player.closeInventory();
						try {
							Player otherPlayer = CivGlobal.getPlayer(pair.otherResident);
							otherPlayer.closeInventory();
						} catch (CivException e2) {
							e2.printStackTrace();
						}
						
						return;
					}
				}
				
			} else {
				/* Mark trade as invalid. */
				markTradeInvalid(pair);
			}	
			return;
		} else if (event.getRawSlot() == MY_COINS_DOWN) {
			if (event.isShiftClick()) {
				handleCoinChange(pair, otherPair, -1000.0);
			} else {
				handleCoinChange(pair, otherPair, -100.0);				
			}
			event.setCancelled(true);
			return;
		} else if (event.getRawSlot() == MY_COINS_UP) {
			if (event.isShiftClick()) {
				handleCoinChange(pair, otherPair, 1000.0);
			} else {
				handleCoinChange(pair, otherPair, 100.0);				
			}
			event.setCancelled(true);
			return;
		}
					
		if (pair.valid || otherPair.valid) {
			/* We're changing the inventory. Cant be valid anymore. */
			markTradeInvalid(pair);
			player.updateInventory();
			markTradeInvalid(otherPair);
			try {
				Player otherPlayer = CivGlobal.getPlayer(pair.otherResident);
				otherPlayer.updateInventory();
			} catch (CivException e) {
			}
			event.setCancelled(true);
			return;
		} 
		
		/* Handle the ugly click types. */
		if (event.getClick().equals(ClickType.SHIFT_LEFT) || event.getClick().equals(ClickType.SHIFT_RIGHT)) {
			/* Manually move over item to correct slot. */
			handleShiftClick(event, player, pair);
			return;
		}
		
		if (event.getClick().equals(ClickType.DOUBLE_CLICK)) {
			handleDoubleClick(event, player, pair);
		}
		
		
		if (!handleSlotChange(event.getRawSlot(), pair)) {
			event.setCancelled(true);
			return;
		}
		
	}

	private void completeTransaction(TradeInventoryPair pair, TradeInventoryPair otherPair) throws CivException {
		Player us = CivGlobal.getPlayer(pair.resident);
		Player them = CivGlobal.getPlayer(pair.otherResident);
		
		try {
	
			/* Remove these pairs from the hashtable as we dont need them anymore. */
			tradeInventories.remove(getTradeInventoryKey(pair.resident));
			tradeInventories.remove(getTradeInventoryKey(otherPair.resident));
			
			LinkedList<ItemStack> myStuff = new LinkedList<ItemStack>();
			LinkedList<ItemStack> theirStuff = new LinkedList<ItemStack>();
			
			int k = OTHERS_SLOTS_START;
			for (int i = MY_SLOTS_START; i < MY_SLOTS_END; i++, k++) {
				
				/* Verify that our "mine" inventory matches the other player's "theirs" inventory. */
				ItemStack stack = pair.inv.getItem(i);
				ItemStack stack2 = otherPair.inv.getItem(k);
				
				if (stack == null && stack2 == null) {
					continue;
				}
				
				if ((stack == null && stack2 != null) || (stack != null && stack2 == null)) {
					CivLog.error("Mismatch. One stack was null. stack:"+stack+" stack2:"+stack2+" i:"+i+" vs k:"+k);
					throw new CivException("Inventory mismatch");
				}
				
				if ((stack == null && stack2 != null) || (stack != null && stack2 == null)) {
					CivLog.error("Mismatch. One stack was null. stack:"+stack+" stack2:"+stack2);
					throw new CivException("Inventory mismatch");
				}
				
				if (!stack.toString().equals(stack2.toString())) {
					CivLog.error("Is Mine Equal to Theirs?");
					CivLog.error("Position i:"+i+" stack:"+stack);
					CivLog.error("Position k:"+k+" stack2:"+stack2);
					throw new CivException("Inventory mismatch.");
				}
				
				if (stack != null) {
					myStuff.add(stack);
				}				
			}
			
			k = MY_SLOTS_START;
			for (int i = OTHERS_SLOTS_START; i < OTHERS_SLOTS_END; i++, k++) {
				/* Verify that our "theirs" inventory matches the other player's "mine" inventory. */
				ItemStack stack = pair.inv.getItem(i);
				ItemStack stack2 = otherPair.inv.getItem(k);
				
				if (stack == null && stack2 == null) {
					continue;
				}
				
				if ((stack == null && stack2 != null) || (stack != null && stack2 == null)) {
					CivLog.error("Mismatch. One stack was null. stack:"+stack+" stack2:"+stack2+" i:"+i+" vs k:"+k);
					throw new CivException("Inventory mismatch");
				}
				
				if (!stack.toString().equals(stack2.toString())) {
					CivLog.error("Is Theirs Equal to Mine?");
					CivLog.error("Position i:"+i+" stack:"+stack);
					CivLog.error("Position k:"+k+" stack2:"+stack2);
					throw new CivException("Inventory mismatch.");
				}
				
				if (stack != null) {
					theirStuff.add(stack);
				}				
			}
			/* Transfer any coins. */
			if (pair.coins != otherPair.otherCoins) {
				CivLog.error("pair.coins = "+pair.coins);
				CivLog.error("otherPair.otherCoins = "+otherPair.otherCoins);
				throw new CivException("Coin mismatch...");
			}
			
			if (otherPair.coins != pair.otherCoins) {
				CivLog.error("otherPair.coins = "+otherPair.coins);
				CivLog.error("pair.coins = "+pair.coins);
				new CivException("Coin mismatch...");
			}
			
			if (pair.coins < 0 || pair.otherCoins < 0 || otherPair.coins < 0 || otherPair.otherCoins < 0) {
				throw new CivException("Coin amount invalid.");
			}
			
			if (!pair.resident.getTreasury().hasEnough(pair.coins)) {
				CivMessage.sendError(us, pair.resident.getName()+" doesnt have enough coins!");
				CivMessage.sendError(them, pair.resident.getName()+" doesnt have enough coins!");
				us.closeInventory();
				them.closeInventory();
				return;
			}
			
			if (!pair.otherResident.getTreasury().hasEnough(pair.otherCoins)) {
				CivMessage.sendError(us, pair.otherResident.getName()+" doesnt have enough coins!");
				CivMessage.sendError(them, pair.otherResident.getName()+" doesnt have enough coins!");
				us.closeInventory();
				them.closeInventory();
				return;
			}
	
			if (pair.coins != 0) {
				pair.resident.getTreasury().withdraw(pair.coins);
				pair.otherResident.getTreasury().deposit(pair.coins);
				CivMessage.sendSuccess(pair.resident, "Gave "+CivColor.Rose+pair.coins+" to "+pair.otherResident.getName());
				CivMessage.sendSuccess(pair.otherResident, "Recieved "+CivColor.Yellow+pair.coins+" from "+pair.resident.getName());
			}

			if (pair.otherCoins != 0) {
				pair.otherResident.getTreasury().withdraw(pair.otherCoins);
				pair.resident.getTreasury().deposit(pair.otherCoins);
				CivMessage.sendSuccess(pair.resident, "Recieved "+CivColor.Yellow+pair.otherCoins+" from "+pair.otherResident.getName());
				CivMessage.sendSuccess(pair.otherResident, "Gave "+CivColor.Rose+pair.otherCoins+" to "+pair.resident.getName());
			}

			/* Finally, give their stuff to me. And my stuff to them. */
			for (ItemStack is : theirStuff) {
				HashMap<Integer, ItemStack> leftovers = us.getInventory().addItem(is);
				for (ItemStack stack : leftovers.values()) {
					us.getPlayer().getWorld().dropItem(us.getLocation(), stack);
				}
			}
			
			for (ItemStack is : myStuff) {
				HashMap<Integer, ItemStack> leftovers = them.getInventory().addItem(is);
				for (ItemStack stack : leftovers.values()) {
					them.getPlayer().getWorld().dropItem(them.getLocation(), stack);
				}
			}
			
			
			CivMessage.sendSuccess(us, "Transaction Successful.");
			CivMessage.sendSuccess(them, "Transaction Successful.");		
		} finally {
			us.closeInventory();
			them.closeInventory();
		}
		
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onInventoryDragEvent(InventoryDragEvent event) {
		if (!(event.getWhoClicked() instanceof Player)) {
			return;
		}
		Player player = (Player)event.getWhoClicked();
		Resident resident = CivGlobal.getResident(player);
		TradeInventoryPair pair = tradeInventories.get(getTradeInventoryKey(resident));
		if (pair == null) {
			return;
		}
		
		Inventory savedTradeInventory = pair.inv;
		if (savedTradeInventory == null) {
			return;
		}
		
		if (!savedTradeInventory.getName().equals(event.getInventory().getName())) {
			return;
		}
		
		for (int slot : event.getRawSlots()) {
			if (!handleSlotChange(slot, pair)) {
				event.setCancelled(true);
				return;
			}
		}
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.LOW)
	public void onInventoryClose(InventoryCloseEvent event) {
		if (!(event.getPlayer() instanceof Player)) {
			return;
		}
		Player player = (Player)event.getPlayer();
		Resident resident = CivGlobal.getResident(player);
		
		if (resident == null) {
			CivLog.error("Got InvCloseEvent with player name:"+player.getName()+" but could not find resident object?");
			return;
		}
		
		TradeInventoryPair pair = tradeInventories.get(getTradeInventoryKey(resident));
		if (pair == null) {
			return;
		}
		
		Inventory savedTradeInventory = pair.inv;
		if (savedTradeInventory == null) {
			return;
		}
		
		if (!savedTradeInventory.getName().equals(event.getInventory().getName())) {
			return;
		}
		
		/* Refund anything in our slots. */
		for (int i = MY_SLOTS_START; i < MY_SLOTS_END; i++) {
			ItemStack stack = pair.inv.getItem(i);
			if (stack == null) {
				continue;
			}
			
			HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
			for (ItemStack left : leftovers.values()) {
				player.getWorld().dropItem(player.getLocation(), left);
			}
		}
		
		class SyncTask implements Runnable {
			String playerName;
			
			public SyncTask(String name) {
				this.playerName = name;
			}
			
			@Override
			public void run() {
				Player player;
				try {
					player = CivGlobal.getPlayer(playerName);
					player.updateInventory();	
				} catch (CivException e) {
				}	
			}
			
		}
		TaskMaster.syncTask(new SyncTask(player.getName()));
	
		tradeInventories.remove(getTradeInventoryKey(resident));
		
		/* Close other player's inventory if open. */
		TradeInventoryPair otherPair = tradeInventories.get(getTradeInventoryKey(pair.otherResident));
		if (otherPair != null) {
			ItemStack guiStack = LoreGuiItem.build(pair.otherResident.getName()+" Confirm", 
					CivData.BEDROCK, 0, 
					CivColor.LightGray+player.getName()+" has closed the trading window.");
			for (int i = OTHERS_SLOTS_START; i < OTHERS_SLOTS_END; i++) {
				otherPair.inv.setItem(i, guiStack);
			}
		}
	
		
		
		
	}
	
}
