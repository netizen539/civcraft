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
package com.avrgaming.civcraft.items;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.DoubleChest;
import org.bukkit.entity.Item;
import org.bukkit.entity.Player;
import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.TradeOutpost;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.EntityUtil;
import com.avrgaming.civcraft.util.InventoryHolderStorage;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.util.ItemManager;

public class BonusGoodie extends LoreItem {
	/*
	 * This class represents a lore item that is tied to a trade outpost.
	 * We need to keep track of its location at all times so we can despawn it.
	 */
	public static enum LoreIndex {
		TYPE,
		OUTPOSTLOCATION
	}
		
	public static final String LORE_TYPE = "Bonus Goodie";
	
	/* Holder that holding this item. null if on the ground or in an itemframe*/
	private InventoryHolderStorage holderStore = null; 
		
	/* Entity representing this item, null if in an inventory. */
	/* NOTE: This item pointer will probably be invalid after a chunk unload and 
	 * reload. For now it's OK since we do repo goodies that are left on the ground
	 * when the chunks unload. But if we extend this in the future, we will need
	 * to store the Item's UUID and grab it like we do for frames.
	 */
	private Item item = null;
	
	/* ItemFrame holding this item. */
	private ItemFrameStorage frameStore = null;
	
	/* Outpost this goodie belongs to. */
	private TradeOutpost outpost = null;
	
	/* Config data belonging to this goodie. */
	private ConfigTradeGood config; 
	
	public static final String TABLE_NAME = "GOODIE_ITEMS";
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`holder_location` mediumtext DEFAULT NULL," +
					"`player_name` mediumtext DEFAULT NULL," +
					"`frame_location` mediumtext DEFAULT NULL,"+
					"`frame_uid` mediumtext DEFAULT NULL," +
					"`item_uid` mediumtext DEFAULT NULL," +
					"`outpost_location` mediumtext DEFAULT NULL," +
				"PRIMARY KEY (`id`)" + ")";
			
			SQL.makeTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
		}
	}
	
	/*
	 * Loads a bonus goodie from the outpost location,
	 * this is the primary load function that is called.
	 * This function calls load with the result set.
	 * Bonus goodies are loaded as the trade outposts are loaded.
	 */
	public BonusGoodie(TradeOutpost outpost) throws SQLException, InvalidNameException, CivException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
		String outpost_location = outpost.getCorner().toString();
		this.config = outpost.getGood().getInfo();
		
		context = SQL.getGameConnection();		
		ps = context.prepareStatement("SELECT * FROM "+SQL.tb_prefix+BonusGoodie.TABLE_NAME+" WHERE `outpost_location`  = ?");
		ps.setString(1, outpost_location);
		rs = ps.executeQuery();
		
		if (rs.first() == false) {
			/* Nothing found in the database, create at trade outpost. */
			this.outpost = outpost;
			createGoodieAtOutpost();
			
		} else {
			this.load(rs);
		}
		
		} finally {
			SQL.close(rs, ps, context);
		}
	}
	
	private void createGoodieAtOutpost() throws CivException {
		ItemFrameStorage outpostFrame = outpost.getItemFrameStore();
		if (outpostFrame == null) {
			throw new CivException("Couldn't find an item frame to construct outpost with.");
		}
		
		ItemStack stack = ItemManager.createItemStack(this.config.material, 1, (short) this.config.material_data);
		
		updateLore(stack);
		
		outpostFrame.setItem(stack);
		this.setFrame(outpostFrame);
		
		CivGlobal.addBonusGoodie(this);
		this.update(false);	
	}
	
	
	public void updateLore(ItemStack stack) {
		TradeGood good = outpost.getGood();

		ArrayList<String> lore = new ArrayList<String>();
		lore.add(LORE_TYPE);
		lore.add(outpost.getCorner().toString());
		
		String[] split = getBonusDisplayString().split(";");
		for (String str : split) {
			lore.add(CivColor.Yellow+str);
		}
				
		if (isStackable()) {
			lore.add(CivColor.LightBlue+"Stackable");
		}
		
		this.setLore(stack, lore);
		this.setDisplayName(stack, good.getInfo().name);

	}
	
	/*
	 * Tries to find a goodie's itemstack
	 */
	public ItemStack findStack() {		
		// Now that we have an outpost for this good, lets see if we 
		// can find where it is stored. 	
		if (this.holderStore != null) {
			InventoryHolder holder;
			try {
				holder = this.holderStore.getHolder();
			} catch (CivException e) {
				e.printStackTrace();
				return null;
			}		
			for (ConfigTradeGood good : CivSettings.goods.values()) {
				for (Entry<Integer, ? extends ItemStack> itemEntry : holder.getInventory().all(ItemManager.getMaterial(good.material)).entrySet()) {
					if (ItemManager.getData(itemEntry.getValue()) != good.material_data) {
						continue;
					}
					ItemStack stack = itemEntry.getValue();
					
					if (this.isItemStackOurs(stack)) {
						// Found ya!						
						return stack;
					} 
				}
			}
		} 	
		
		// if in an item frame
		if (this.frameStore != null) {
			try {
				if (frameStore.isEmpty() || !isItemStackOurs(frameStore.getItem())) {
					CivLog.warning("Found frame, but item was wrong, trying to recover by spawning item.");
					
					ItemStack stack = ItemManager.createItemStack(this.config.material, 1, (short) this.config.material_data);
					updateLore(stack);
					
					frameStore.setItem(stack);
					return stack;
				}
			} catch (CivException e) {
				e.printStackTrace();
				try {
					deleteAndReset();
				} catch (SQLException e1) {
					e1.printStackTrace();
				}
			}
			
			return this.frameStore.getItem();
		}
		
		if (this.item != null) {
			return item.getItemStack();
		}		
		return null;
	}
	
	/*
	 * Despawns the item from its current location
	 * and puts it back in the itemframe on the outpost which
	 * it belongs to.
	 */
	public void replenish(ItemStack itemStack, Item itemEntity, Inventory inventory, ItemFrameStorage frame) {
				
		if ((itemEntity == null && inventory == null && frame == null) || itemStack == null) {
			try {
				this.deleteAndReset();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return;
		}
		
		/* Add this item to the item frame. */
		ItemFrameStorage frameStore = outpost.getItemFrameStore();
		if (frameStore != null) {
			frameStore.setItem(new ItemStack(itemStack));
			setFrame(frameStore);
		} else {
			CivLog.warning("Couldn't replenish good, item frame was missing.");
			return;
		}
	
		/* Remove any item in an inventory. */
		if (inventory != null) {
			if (inventory instanceof DoubleChestInventory) {
				DoubleChestInventory dv = (DoubleChestInventory)inventory;
				dv.remove(itemStack);
			} else {
				inventory.remove(itemStack);
			}
		}
		
		/* Remove item from any frame. */
		if (frame != null && (frame.getUUID() != frameStore.getUUID())) {
			frame.clearItem();
		}
		
		
		/* Remove any item on the ground. */
		if (itemEntity != null) {
			itemEntity.remove();
		} 
		
		try {
			this.update(false);
			this.updateLore(itemStack);
		} catch (CivException e) {
			e.printStackTrace();
		}
	}
	
	/*
	 * De-spawns an item from its current location, and puts it back in
	 * the item frame on the outpost which it belongs to.
	 */
		
	public void replenish() {
		class SyncTask implements Runnable {

			@Override
			public void run() {
				if (holderStore != null) {
					try {
						replenish(findStack(), null, holderStore.getHolder().getInventory(), null);
					} catch (CivException e) {
						e.printStackTrace();
					}
								
				} else if (frameStore != null) {
					replenish(findStack(), null, null, frameStore);

				}
				else {
					replenish(findStack(), item, null, null);
				}
			}
		}
		TaskMaster.syncTask(new SyncTask());
	}
	
	
	/*
	 * Updates the internal location data of the goodie and
	 * saves it to the database if needed.
	 */
	public void update(boolean sync) throws CivException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
				
		//Verify that the structure still exists, could have been unbuilt.
		Structure struct = CivGlobal.getStructure(this.outpost.getCorner());
		if (struct == null) {
			try {
				this.delete();
			} catch (SQLException e) {
				e.printStackTrace();
			}
			return;
		}
		
		if (holderStore != null) {
			if (holderStore.getHolder() instanceof Chest) {
				Location holderLocation = ((Chest)holderStore.getHolder()).getLocation();
				
				hashmap.put("holder_location", new BlockCoord(holderLocation).toString());
				hashmap.put("player_name", null);
				hashmap.put("frame_uid", null);
				hashmap.put("frame_location", null);
				hashmap.put("item_uid", null);
				
			} 
			else if (holderStore.getHolder() instanceof DoubleChest) {
				Location holderLocation = ((DoubleChest)holderStore.getHolder()).getLocation();
				
				hashmap.put("holder_location", new BlockCoord(holderLocation).toString());
				hashmap.put("player_name", null);
				hashmap.put("frame_uid", null);
				hashmap.put("frame_location", null);
				hashmap.put("item_uid", null);
			}
			
			else if (holderStore.getHolder() instanceof Player) {
				// Set player name instead of holder location...
				hashmap.put("player_name", ((Player)holderStore.getHolder()).getName());
				hashmap.put("holder_location", null);
				hashmap.put("frame_uid", null);
				hashmap.put("frame_location", null);
				hashmap.put("item_uid", null);
			}
		} else {
			// Holder was null..
			hashmap.put("holder_location", null);
			hashmap.put("player_name", null);
		}
		
		if (this.frameStore != null) {
			hashmap.put("frame_uid", this.frameStore.getUUID().toString());
			hashmap.put("frame_location", this.frameStore.getCoord().toString());
			hashmap.put("player_name", null);
			hashmap.put("holder_location", null);
			hashmap.put("item_uid", null);
		} else {
			// Frame was null ...
			hashmap.put("frame_uid", null);
			hashmap.put("frame_location", null);
		}
		
		if (this.item != null) {
			hashmap.put("item_uid", this.item.getUniqueId().toString());
			hashmap.put("player_name", null);
			hashmap.put("holder_location",  null);
			hashmap.put("frame_uid", null);
			hashmap.put("frame_location", null);
		} else {
			hashmap.put("item_uid", null);
		}
		
		hashmap.put("outpost_location", this.getOutpost().getCorner().toString());
		
		try {
			if (sync) {
				SQL.updateNamedObject(this, hashmap, TABLE_NAME);
			} else {
				SQL.updateNamedObjectAsync(this, hashmap, TABLE_NAME);
			}
		} catch (SQLException e) {
			CivLog.error("Internal Database error in update of goodie.");
			e.printStackTrace();
		}	

	}
	
	public TradeOutpost getOutpost() {
		return outpost;
	}

	public void setOutpost(TradeOutpost outpost) {
		this.outpost = outpost;
	}

	public InventoryHolder getHolder() {
		if (holderStore == null) {
			return null;
		}
		try {
			return holderStore.getHolder();
		} catch (CivException e) {
			e.printStackTrace();
			return null;
		}
	}

	public void setHolder(InventoryHolder holder) throws CivException {
		if (holder == null) {
			return;
		}
		
		if (holderStore == null) {
			if (holder instanceof Chest) {
				holderStore = new InventoryHolderStorage(((Chest)holder).getLocation());
			} else if (holder instanceof Player){
				holderStore = new InventoryHolderStorage((Player)holder);
			} else {
				throw new CivException("Invalid holder.");
			}
		} else {
			holderStore.setHolder(holder);
		}
		
		// If we have a holder, we cannot be on the ground or in a item frame.
		this.frameStore = null;
		this.item = null;
		
	}

	public ItemStack getStack() {
		return findStack();
	}

	public Item getItem() {
		return item;
	}

	public void setItem(Item item) {
		this.item = item;
		if (item != null) {
			// If we are an entity on the ground, we cannot have a holder or be in a frame.
			this.frameStore = null;
			holderStore = null;
		}
	}

	public ItemFrameStorage getFrame() {
		return frameStore;
	}

	public void setFrame(ItemFrameStorage frameStore) {
		this.frameStore = frameStore;
		if (frameStore != null) {
			//If we are in a frame, we cant be on the ground or in an inventory.
			holderStore = null;
			this.item = null;
		}
	}

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException {
		this.setId(rs.getInt("id"));

		String holderLocString = rs.getString("holder_location");
		String outpostLocString = rs.getString("outpost_location");
		String frameUID = rs.getString("frame_uid");
		String itemUID = rs.getString("item_uid");
		Location outpostLocation = null;
		
		// First find the outpost for this goodie.
		try {
			if (outpostLocString != null && !outpostLocString.equals("")) {
				outpostLocation = CivGlobal.getLocationFromHash(outpostLocString);
				this.outpost = (TradeOutpost)CivGlobal.getStructure(new BlockCoord(outpostLocation));
			}
		} catch (Exception e) {
			e.printStackTrace();
			return;
		}
		

		if (this.outpost == null || outpostLocation == null) {
			// couldn't find the outpost. Delete this good from the db.
			this.delete();
			return;
		}
		

		// Now that we have an outpost for this good, lets see if we 
		// can find where it is stored. 	
		if (holderLocString != null && !holderLocString.equals("")) {
			Location loc = CivGlobal.getLocationFromHash(holderLocString);
			BlockCoord bcoord = new BlockCoord(loc);
			
			Block b = bcoord.getBlock();		
			if (b.getState() instanceof Chest) { // || (bcoord.getBlock() instanceof DoubleChest)) {
				//Stored somewhere in this chest. 
				Inventory inv = ((Chest)b.getState()).getInventory();
				
				for (ConfigTradeGood good : CivSettings.goods.values()) {
					for (Entry<Integer, ? extends ItemStack> itemEntry : inv.all(ItemManager.getMaterial(good.material)).entrySet()) {
						if (ItemManager.getData(itemEntry.getValue()) != good.material_data) {
							continue;
						}
						ItemStack stack = itemEntry.getValue();
						
						if (this.isItemStackOurs(stack)) {
							// Found ya!						
							holderStore = new InventoryHolderStorage(inv.getHolder(), b.getLocation());
							this.frameStore = null;
							this.item = null;
							CivGlobal.addBonusGoodie(this);
							return;
						}
					}
				}
			}
		} 
		
		// if in an item frame
		if (frameUID != null && !frameUID.equals("")) {
			Location loc = CivGlobal.getLocationFromHash(rs.getString("frame_location"));
			loc.getWorld().loadChunk(loc.getChunk());
			
			try {
			//	this.frameStore = new ItemFrameStorage((ItemFrame) CivGlobal.getEntityClassFromUUID(outpostLocation.getWorld(), 
			//			ItemFrame.class, UUID.fromString(frameUID)));
				this.frameStore = CivGlobal.getProtectedItemFrame(UUID.fromString(frameUID));
				if (frameStore == null) {
					throw new CivException("Couldn't find frame loaded from a structure? missing frame:"+frameUID);
				}
				//CivLog.debug("BonusGoodie set town:"+outpost.getTown().getName());
				//this.frameStore.setTown(outpost.getTown());
				
			} catch (CivException e) {
				CivLog.warning("Couldn't find frame loaded from DB:"+frameUID);
				deleteAndReset();
				return;
			}
			
			holderStore = null;
			this.item = null;
			
			try {
				if (frameStore.isEmpty() || !isItemStackOurs(frameStore.getItem())) {
					//Couldn't find good, deleting...
					CivLog.warning("Found frame, but item was wrong:"+frameUID);
					deleteAndReset();
					return;
				}
			} catch (CivException e) {
				e.printStackTrace();
				deleteAndReset();
			}
			
			CivGlobal.addBonusGoodie(this);
			return;
		}
		
		if (itemUID != null && !itemUID.equals("")) {
			this.item = (Item) EntityUtil.getEntity(outpostLocation.getWorld(), UUID.fromString(itemUID));
			if (this.item == null) {
				CivLog.warning("ITEM ON GROUND WAS NULL...deleting goodie");
				this.delete();
				return;
			}
			this.frameStore = null;
			holderStore = null;
			
			if (!this.isItemStackOurs(this.item.getItemStack())) {
				deleteAndReset();
				return;
			}
			
			CivGlobal.addBonusGoodie(this);
			return;
		}
			
		deleteAndReset();
	}
	
	private void deleteAndReset() throws SQLException {
		this.delete();
		try {
			this.createGoodieAtOutpost();
		} catch (CivException e) {
			e.printStackTrace();
		}
	}
	
	
	private boolean isItemStackOurs(ItemStack stack) {
		ItemMeta meta = stack.getItemMeta();
		if (meta == null) {
			return false;
		}
		
		if (meta.hasLore() && meta.getLore().size() >= LoreIndex.values().length) {
			if (meta.getLore().get(LoreIndex.TYPE.ordinal()).equals(BonusGoodie.LORE_TYPE)) {
				String outpostLoreLoc = meta.getLore().get(LoreIndex.OUTPOSTLOCATION.ordinal());
				Location loc = CivGlobal.getLocationFromHash(outpostLoreLoc);
				
				if (loc.getBlockX() == outpost.getCorner().getX() &&
						loc.getBlockY() == outpost.getCorner().getY() &&
						loc.getBlockZ() == outpost.getCorner().getZ()) {
					// Found ya!						
					return true;
				}
			}
		}
		return false;
	}
	
	@Override 
	public void saveNow() throws SQLException {
		try {
			update(true);
		} catch (CivException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void save() {
//		try {
//			update(sync);
//		} catch (CivException e) {
//			e.printStackTrace();
//		}
		
		SQLUpdate.add(this);
		
	}

	@Override
	public void delete() throws SQLException {
		if (this.item != null) {
			this.item.remove();
		}
		
		if (this.frameStore != null) {
			this.frameStore.setItem(new ItemStack(Material.AIR));
		}
		
		if (holderStore  != null) {
			try {
				this.holderStore.getHolder().getInventory().remove(findStack());
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
		
		SQL.deleteNamedObject(this, BonusGoodie.TABLE_NAME);
	}

	@Override
	public void load() {
		//This is the 'lore item' load which might not mean anything.
	}

	
	/*
	 * Gets the bonus value from the specified key.
	 */
	public String getBonusValue(String key) {
		if (config.buffs.containsKey(key)) {
			return config.buffs.get(key).value;
		}
		return "";
	}

	public ConfigTradeGood getConfigTradeGood() {
		return this.config;
	}
	
	public String getOutpostStringFromLore() {
		return this.getLore(findStack()).get(1);
	}
	
	public String getBonusDisplayString() {
		String out = "";
		
		for (ConfigBuff cBuff : this.config.buffs.values()) {
			out += ChatColor.UNDERLINE+cBuff.name;
			out += ";";
			out += CivColor.White+ChatColor.ITALIC+cBuff.description;
			out += ";";
		}

		return out;		
	}
	
	public String getDisplayName() {
		return config.name;
	}

	public boolean isStackable() {
		// RJ TODO remove me... bonuses are stackable not goodies.
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.outpost.getCorner().toString().hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof BonusGoodie) {
			BonusGoodie otherCoord = (BonusGoodie)other;
			if (otherCoord.getOutpost().getCorner().equals(this.getOutpost().getCorner().toString())) {
				return true;
			}
		}
		return false;
	}
	

}
