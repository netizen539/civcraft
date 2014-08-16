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
package com.avrgaming.civcraft.structure;

import gpl.AttributeUtil;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.components.Catalyst;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.tasks.NotificationTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.TimeTools;

public class Blacksmith extends Structure {
	
	private static final long COOLDOWN = 5;
	//private static final double BASE_CHANCE = 0.8;
	public static int SMELT_TIME_SECONDS = 3600*3;
	public static double YIELD_RATE = 1.25;
	
	private Date lastUse = new Date();
	
	private NonMemberFeeComponent nonMemberFeeComponent;
	
	public static HashMap<BlockCoord, Blacksmith> blacksmithAnvils = new HashMap<BlockCoord, Blacksmith>();

	protected Blacksmith(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onSave();
	}

	public Blacksmith(ResultSet rs) throws SQLException, CivException {
		super(rs);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onLoad();
	}
	
	public double getNonResidentFee() {
		return nonMemberFeeComponent.getFeeRate();
	}

	public void setNonResidentFee(double nonResidentFee) {
		this.nonMemberFeeComponent.setFeeRate(nonResidentFee);
	}
	
	private String getNonResidentFeeString() {
		return "Fee: "+((int)(this.nonMemberFeeComponent.getFeeRate()*100) + "%").toString();		
	}
	
	@Override
	public String getDynmapDescription() {
		return null;
	}
	
	@Override
	public String getMarkerIconName() {
		return "factory";
	}
	
	@Override
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) throws CivException {
		int special_id = Integer.valueOf(sign.getAction());
		
		Date now = new Date();
		
		long diff = now.getTime() - lastUse.getTime();
		diff /= 1000;
		
		if (diff < Blacksmith.COOLDOWN) {
			throw new CivException("Blacksmith is on cooldown. Please wait another "+(Blacksmith.COOLDOWN - diff)+" seconds.");
		}
		
		lastUse = now;
		
		switch (special_id) {
		case 0:
			this.deposit_forge(player);
			break;
		case 1:
			double cost = CivSettings.getDoubleStructure("blacksmith.forge_cost");
			this.perform_forge(player, cost);
			break;
		case 2:
			this.depositSmelt(player, player.getItemInHand());
			break;
		case 3:
			this.withdrawSmelt(player);
			break;
		}
		
	}
	
	@Override
	public void updateSignText() {
		double cost = CivSettings.getDoubleStructure("blacksmith.forge_cost");
		
		for (StructureSign sign : getSigns()) {
			int special_id = Integer.valueOf(sign.getAction());

			switch (special_id) {
			case 0:
				sign.setText("Deposit\nWithdraw\nCatalyst");
				break;
			case 1:
				sign.setText("Forge!\n"+
						"For "+cost+" Coins\n"+
						getNonResidentFeeString());			
				break;
			case 2:
				sign.setText("Deposit\nOre\nResidents Only");
				break;
			case 3:
				sign.setText("Withdraw\nOre\nResidents Only");
				break;
			}
				
			sign.update();
		}
	}
	
	public String getkey(Player player, Structure struct, String tag) {
		return player.getName()+"_"+struct.getConfigId()+"_"+struct.getCorner().toString()+"_"+tag; 
	}

	public void saveItem(ItemStack item, String key) {
		
		String value = ""+ItemManager.getId(item)+":";
		
		for (Enchantment e : item.getEnchantments().keySet()) {
			value += ItemManager.getId(e)+","+item.getEnchantmentLevel(e);
			value += ":";
		}
		
		sessionAdd(key, value);
	}
	
	public void saveCatalyst(LoreCraftableMaterial craftMat, String key) {
		String value = craftMat.getConfigId();
		sessionAdd(key, value);
	}
	
	public static boolean canSmelt(int blockid) {
		switch (blockid) {
		case CivData.GOLD_ORE:
		case CivData.IRON_ORE:
			return true;
		}
		return false;
	}
		
	/*
	 * Converts the ore id's into the ingot id's
	 */
	public static int convertType(int blockid) {
		switch(blockid) {
		case CivData.GOLD_ORE:
			return CivData.GOLD_INGOT;
		case CivData.IRON_ORE:
			return CivData.IRON_INGOT;
		}
		return -1;
	}
	
	/*
	 * Deposit forge will take the current item in the player's hand
	 * and deposit its information into the sessionDB. It will store the 
	 * item's id, data, and damage.
	 */
	public void deposit_forge(Player player) throws CivException {
		
		ItemStack item = player.getItemInHand();
		
		ArrayList<SessionEntry> sessions = null;
		String key = this.getkey(player, this, "forge");
		sessions = CivGlobal.getSessionDB().lookup(key);
		
		if (sessions == null || sessions.size() == 0) {
			/* Validate that the item being added is a catalyst */
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(item);
			if (craftMat == null || !craftMat.hasComponent("Catalyst")) {
				throw new CivException("You must deposit a catalyst into the forge.");
			}
			
			/* Item is a catalyst. Add it to the session DB. */
			saveCatalyst(craftMat, key);
			if (item.getAmount() > 1) {
				item.setAmount(item.getAmount()-1);
			} else {
				player.setItemInHand(new ItemStack(Material.AIR));
			//	player.getInventory().remove(item);
			}
			
			CivMessage.sendSuccess(player, "Deposited Catalyst.");
		} else {
			/* Catalyst already in blacksmith, withdraw it. */
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(sessions.get(0).value);
			if (craftMat == null) {
				throw new CivException("Error withdrawing catalyst from blacksmith. File a bug report!");
			}
			
			ItemStack stack = LoreMaterial.spawn(craftMat);
			HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
			if (leftovers.size() > 0) {
				for (ItemStack is : leftovers.values()) {
					player.getWorld().dropItem(player.getLocation(), is);
				}
			}
			CivGlobal.getSessionDB().delete_all(key);
			CivMessage.sendSuccess(player, "Withdrawn Catalyst");
		}
	}
	
	/* 
	 * Perform forge will perform the over-enchantment algorithm and determine
	 * if this player is worthy of a higher level pick. If successful it will
	 * give the player the newly created pick.
	 */
	public void perform_forge(Player player, double cost) throws CivException {

		/* Try and retrieve any catalyst in the forge. */
		String key = getkey(player, this, "forge");
		ArrayList<SessionEntry> sessions = CivGlobal.getSessionDB().lookup(key);
		
		/* Search for free catalyst. */
		ItemStack stack = player.getItemInHand();
		AttributeUtil attrs = new AttributeUtil(stack);
		Catalyst catalyst;
		
		
		String freeStr = attrs.getCivCraftProperty("freeCatalyst");
		if (freeStr == null) {
			/* No free enhancements on item, search for catalyst. */
			if (sessions == null || sessions.size() == 0) {
				throw new CivException("No catalyst in the forge. Deposit one first.");
			}
			
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(sessions.get(0).value);
			if (craftMat == null) {
				throw new CivException("Error getting catalyst from blacksmith. File a bug report!");
			}
			
			catalyst = (Catalyst)craftMat.getComponent("Catalyst");
			if (catalyst == null) {
				throw new CivException("Error getting catalyst from blacksmith. Please file a bug report.");
			}
		} else {
			String[] split = freeStr.split(":");
			Double level = Double.valueOf(split[0]);
			String mid = split[1];
			
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(mid);
			if (craftMat == null) {
				throw new CivException("Error getting catalyst from blacksmith. File a bug report!");
			}

			catalyst = (Catalyst)craftMat.getComponent("Catalyst");
			if (catalyst == null) {
				throw new CivException("Error getting catalyst from blacksmith. Please file a bug report.");
			}
			
			/* reduce level and reset item. */
			level--;
			
			String lore[] = attrs.getLore();
			for (int i = 0; i < lore.length; i++) {
				String str = lore[i];
				if (str.contains("free enhancements")) {
					if (level != 0) {
						lore[i] = CivColor.LightBlue+level+" free enhancements! Redeem at blacksmith.";
					} else {
						lore[i] = "";
					}
					break;
				}
			}
			attrs.setLore(lore);
			
			if (level != 0) {
				attrs.setCivCraftProperty("freeCatalyst", level+":"+mid);
			} else {
				attrs.removeCivCraftProperty("freeCatalyst");
			}
			
			player.setItemInHand(attrs.getStack());
			
		}
		
		stack = player.getItemInHand();
		ItemStack enhancedItem = catalyst.getEnchantedItem(stack);
		
		if (enhancedItem == null) {
			throw new CivException("You cannot use this catalyst on this item.");
		}
		
		/* Consume the enhancement. */
		CivGlobal.getSessionDB().delete_all(key);
		
		if (!catalyst.enchantSuccess(enhancedItem)) {
			/* 
			 * There is a one in third chance that our item will break.
			 * Sucks, but this is what happened here.
			 */
			player.setItemInHand(ItemManager.createItemStack(CivData.AIR, 1));
			CivMessage.sendError(player, "Enhancement failed. Item has broken.");
			return;
		} else {
			player.setItemInHand(enhancedItem);
			CivMessage.sendSuccess(player, "Enhancement succeeded!");
			return;
		}
	}
	/*
	 * Take the itemstack in hand and deposit it into
	 * the session DB.
	 */
	@SuppressWarnings("deprecation")
	public void depositSmelt(Player player, ItemStack itemsInHand) throws CivException {
		
		// Make sure that the item is a valid smelt type.
		if (!Blacksmith.canSmelt(itemsInHand.getTypeId())) {
			throw new CivException ("Can only smelt gold and iron ore.");
		}
		
		// Only members can use the smelter
		Resident res = CivGlobal.getResident(player.getName());
		if (!res.hasTown() || this.getTown() != res.getTown()) {
			throw new CivException ("Can only use the smelter if you are a town member.");
		}
		
		String value = convertType(itemsInHand.getTypeId())+":"+(itemsInHand.getAmount()*Blacksmith.YIELD_RATE);
		String key = getkey(player, this, "smelt");
		
		// Store entry in session DB
		sessionAdd(key, value);
		
		// Take ore away from player.
		player.getInventory().removeItem(itemsInHand);
		//BukkitTools.sch
		// Schedule a message to notify the player when the smelting is finished.
		BukkitObjects.scheduleAsyncDelayedTask(new NotificationTask(player.getName(), 
				CivColor.LightGreen+" Your stack of "+itemsInHand.getAmount()+" "+
				CivData.getDisplayName(itemsInHand.getTypeId())+" has finished smelting."), 
				TimeTools.toTicks(SMELT_TIME_SECONDS));
		
		CivMessage.send(player,CivColor.LightGreen+ "Deposited "+itemsInHand.getAmount()+ " ore.");
		
		player.updateInventory();
	}
	
	
	/* 
	 * Queries the sessionDB for entries for this player
	 * When entries are found, their inserted time is compared to
	 * the current time, if they have been in long enough each
	 * itemstack is sent to the players inventory.
	 * 
	 * For each itemstack ready to withdraw try to place it in the 
	 * players inventory. If there is not enough space, take the 
	 * leftovers and place them back in the sessionDB.
	 * If there are no leftovers, delete the sessionDB entry.
	 */
	@SuppressWarnings("deprecation")
	public void withdrawSmelt(Player player) throws CivException {
		
		String key = getkey(player, this, "smelt");
		ArrayList<SessionEntry> entries = null;
		
		// Only members can use the smelter
		Resident res = CivGlobal.getResident(player.getName());
		if (!res.hasTown() || this.getTown() != res.getTown()) {
			throw new CivException ("Can only use the smelter if you are a town member.");
		}
		
		entries = CivGlobal.getSessionDB().lookup(key);
		
		if (entries == null || entries.size() == 0) {
			throw new CivException ("No items to withdraw");
		}
				
		Inventory inv = player.getInventory();
		HashMap <Integer, ItemStack> leftovers = null;

		for (SessionEntry se : entries) {
			String split[] = se.value.split(":");
			int itemId = Integer.valueOf(split[0]);
			double amount = Double.valueOf(split[1]);
			long now = System.currentTimeMillis();
			int secondsBetween = CivGlobal.getSecondsBetween(se.time, now);
			
			// First determine the time between two events.
			if (secondsBetween < Blacksmith.SMELT_TIME_SECONDS) {
				 DecimalFormat df1 = new DecimalFormat("0.##"); 
				 
				double timeLeft = ((double)Blacksmith.SMELT_TIME_SECONDS - (double)secondsBetween) / (double)60;
				//Date finish = new Date(now+(secondsBetween*1000));
				CivMessage.send(player, CivColor.Yellow+"Stack of "+amount+" "+
						CivData.getDisplayName(itemId)+" will be finished in "+ df1.format(timeLeft) +" minutes.");
				continue;
			}
			
			ItemStack stack = new ItemStack(itemId, (int)amount, (short)0);
			if (stack != null)
				leftovers = inv.addItem(stack);
	
			// If this stack was successfully withdrawn, delete it from the DB.
			if (leftovers.size() == 0) {
				CivGlobal.getSessionDB().delete(se.request_id, se.key);
				CivMessage.send(player, CivColor.LightGreen+"Withdrew "+amount+" "+CivData.getDisplayName(itemId));
				break;
			} else {
				// We do not have space in our inventory, inform the player.
				CivMessage.send(player, CivColor.Rose+"Not enough inventory space for all items.");
				
				// If the leftover size is the same as the size we are trying to withdraw, do nothing.
				int leftoverAmount = CivGlobal.getLeftoverSize(leftovers);
				
				if (leftoverAmount == amount) {
					continue;
				}
				
				if (leftoverAmount == 0) {
					//just in case we somehow get an entry with 0 items in it.
					CivGlobal.getSessionDB().delete(se.request_id, se.key);
				}
				else {							
					// Some of the items were deposited into the players inventory but the sessionDB 
					// still has the full amount stored, update the db to only contain the leftovers.
					String newValue = itemId+":"+leftoverAmount;			
					CivGlobal.getSessionDB().update(se.request_id, se.key, newValue);
				}
			}
			
			// only withdraw one item at a time.
			break;
		}	
				
		player.updateInventory();
	}
	
	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
	}
}
