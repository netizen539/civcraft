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
package com.avrgaming.civcraft.config;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.MultiInventory;

public class ConfigMarketItem {
	public int id;
	public String name;
	public int type_id;
	public String custom_id;
	public int data;
	public int inital_value;
	
	private int buy_value;
	private int buy_bulk;
	private int sell_value;
	private int sell_bulk;
	
	private int bought;
	private int sold;
	private int buysell_count = 0;
	private boolean stackable = true;
	private int step;
	
	public static int BASE_ITEM_AMOUNT = 1;
	public static int STEP = 1;
	public static int STEP_COUNT = 256;
	public static double RATE = 0.15;
	
	public enum LastAction {
		NEUTRAL,
		BUY,
		SELL
	}
	
	public LastAction lastaction = LastAction.NEUTRAL;
	
	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigMarketItem> items) {
		items.clear();
		List<Map<?, ?>> culture_levels = cfg.getMapList("items");
		
		try {
			STEP = CivSettings.getInteger(CivSettings.marketConfig, "step");
			STEP_COUNT = CivSettings.getInteger(CivSettings.marketConfig, "step_count");
			RATE = CivSettings.getDouble(CivSettings.marketConfig, "rate");
		} catch (InvalidConfiguration e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		
		for (Map<?, ?> level : culture_levels) {
			
			ConfigMarketItem item = new ConfigMarketItem();
			item.id = (Integer)level.get("id");
			item.name = (String)level.get("name");
			item.type_id = (Integer)level.get("type_id");
			item.data = (Integer)level.get("data");
			
			item.inital_value = (Integer)level.get("value");
			
			if (level.get("custom_id") != null) {
				item.custom_id = (String)level.get("custom_id");
			} else {
				item.custom_id = null;
			}
			
			if (level.get("step") != null) {
				item.step = (Integer)level.get("step");
			} else {
				item.step = STEP;
			}
			
			Boolean stackable = (Boolean)level.get("stackable");
			if (stackable != null && stackable == false) {
				item.stackable = stackable;
			}
			
			items.put(item.id, item);
		}
		CivLog.info("Loaded "+items.size()+" market items.");
	}
	
	public static final String TABLE_NAME = "MARKET_ITEMS";
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`ident` VARCHAR(64) NOT NULL," +
					"`buy_value` int(11) NOT NULL DEFAULT 105," +
					"`buy_bulk` int(11) NOT NULL DEFAULT 1," +
					"`sell_value` int(11) NOT NULL DEFAULT 95," +
					"`sell_bulk` int(11) NOT NULL DEFAULT 1," +
					"`buysell` int(11) NOT NULL DEFAULT 0,"+
					"`bought` int(11) NOT NULL DEFAULT 0," +
					"`sold` int(11) NOT NULL DEFAULT 0," +
					"`last_action` mediumtext, " +
				"PRIMARY KEY (`ident`)" + ")";
			
			SQL.makeTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
		}
		
		for (ConfigMarketItem item : CivSettings.marketItems.values()) {
			item.load();
		}
	}
	
	private String getIdent() {
		if (this.custom_id == null) {
			return type_id+":"+data;
		} else {
			return this.custom_id;
		}
	}
	
	public void load() throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		try {	
			String query = "SELECT * FROM `"+SQL.tb_prefix+TABLE_NAME+"` WHERE `ident` = ?;";
			context = SQL.getGameConnection();
			ps = context.prepareStatement(query);	
			ps.setString(1, getIdent());
			rs = ps.executeQuery();
			
			if (rs.next()) {
				this.buy_value = (Integer)rs.getInt("buy_value");
				this.buy_bulk = (Integer)rs.getInt("buy_bulk");
				this.sell_value = (Integer)rs.getInt("sell_value");
				this.sell_bulk = (Integer)rs.getInt("sell_bulk");
				this.bought = (Integer)rs.getInt("bought");
				this.sold = (Integer)rs.getInt("sold");
				this.lastaction = LastAction.valueOf((String)rs.getString("last_action"));
				this.buysell_count = (Integer)rs.getInt("buysell");
			} else {
				this.bought = 0;
				this.sold = 0;
				this.buy_bulk = 1;
				this.sell_bulk = 1;
				this.buy_value = this.inital_value + (int)((double)this.inital_value*RATE);
				this.sell_value = this.inital_value;
				
				if(buy_value == sell_value) {
					buy_value++;
				}
				
				this.saveItemNow();
			}
			
		} finally {
			SQL.close(rs, ps, context);
		}
	}
	
	public void saveItemNow() throws SQLException {
		Connection context = null;
		PreparedStatement ps = null;
		
		try {
			String query = "INSERT INTO `"+SQL.tb_prefix+TABLE_NAME+"` (`ident`, `buy_value`, `buy_bulk`, `sell_value`, `sell_bulk`, `bought`, `sold`, `last_action`, `buysell`) "+
					"VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `buy_value`=?, `buy_bulk`=?, `sell_value`=?, `sell_bulk`=?, `bought`=?, `sold`=?, `last_action`=?, `buysell`=?";
			context = SQL.getGameConnection();		
			ps = context.prepareStatement(query);
			
			ps.setString(1, getIdent());
			ps.setInt(2, buy_value);
			ps.setInt(3, buy_bulk);
			ps.setInt(4, sell_value);
			ps.setInt(5, sell_bulk);
			ps.setInt(6, bought);
			ps.setInt(7, sold);
			ps.setString(8, lastaction.toString());
			ps.setInt(9, buysell_count);
			ps.setInt(10, buy_value);
			ps.setInt(11, buy_bulk);
			ps.setInt(12, sell_value);
			ps.setInt(13, sell_bulk);
			ps.setInt(14, bought);
			ps.setInt(15, sold);
			ps.setString(16, lastaction.toString());
			ps.setInt(17, buysell_count);
	
			int rs = ps.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+query);
			}
		} finally {
			SQL.close(null, ps, context);
		}
	}

	public void save() {
		class SyncTask implements Runnable {

			@Override
			public void run() {
				try {
					saveItemNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		TaskMaster.syncTask(new SyncTask());
	}
	
	public int getCoinsCostForAmount(int amount, int value, int dir) {
		int sum = 0;
		int current = value;
		int buysell = 0;
		
		for (int i = 0; i < amount; i++) {
			sum += current;
			buysell += dir;
			
			if ((dir*buysell) % (dir*STEP_COUNT) == 0) {
				current += dir*this.step;
				if (current < this.step) {
					current = this.step;
				}			
			}
		}		
		return sum;		
	}
	
	public int getBuyCostForAmount(int amount) {
		int additional = getCoinsCostForAmount(amount, buy_value, 1);
		//int additional = getSellCostForAmount(amount);
		additional *= 2;
		return additional;
	}
	
	public int getSellCostForAmount(int amount) {
		int detremental = getCoinsCostForAmount(amount, sell_value, -1);
		return detremental;
	}
	
	@SuppressWarnings("deprecation")
	public void buy(Resident resident, Player player, int amount) throws CivException {
		int total_items = 0;
		
		double coins = resident.getTreasury().getBalance();
		double cost = getBuyCostForAmount(amount);
		
		if (coins < cost) {
			throw new CivException("You do not have the required "+cost);
		}
				
		for (int i = 0; i < amount; i++) {
			coins -= buy_value;
			total_items += BASE_ITEM_AMOUNT;
			increment();
		}
		
		/* We've now got the cost and items we've bought. Give to player. */
		resident.getTreasury().withdraw(cost);
		
		ItemStack newStack;
		if (this.custom_id == null) {
			newStack = new ItemStack(this.type_id, amount, (short)this.data);
		} else {
			newStack = LoreMaterial.spawn(LoreMaterial.materialMap.get(this.custom_id));
			newStack.setAmount(amount);
		}

		HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(newStack);
		for (ItemStack stack : leftovers.values()) {
			player.getWorld().dropItem(player.getLocation(), stack);
		}
		
		CivMessage.sendSuccess(player, "Bought "+total_items+" "+this.name+" for "+cost+" coins.");
		player.updateInventory();
	}
	
	@SuppressWarnings("deprecation")
	public void sell(Resident resident, Player player, int amount) throws CivException {
		int total_coins = 0;
		int total_items = 0;
		
		MultiInventory inv = new MultiInventory();
		inv.addInventory(player.getInventory());
		
		if (!inv.contains(custom_id, this.type_id, (short)this.data, amount)) {
			throw new CivException("You do not have "+amount+" "+this.name+" to sell.");
		}
				
		for (int i = 0; i < amount; i++) {			
			total_coins += sell_value;
			total_items += BASE_ITEM_AMOUNT;
			
			decrement();
		}
		
		if (!inv.removeItem(this.custom_id, this.type_id, (short)this.data, amount)) {
			throw new CivException("Sorry, you don't have enough "+this.name+" in your inventory.");
		}
		
		resident.getTreasury().deposit(total_coins);	
		CivMessage.sendSuccess(player, "Sold "+total_items+" "+this.name+" for "+total_coins);
		player.updateInventory();
	}
	
	
	public void increment() {
		buysell_count++;
		if (((buysell_count % STEP_COUNT) == 0) || (stackable == false)) {
			sell_value += this.step;
			buy_value = sell_value + (int)((double)sell_value*RATE);
			
			if (buy_value == sell_value) {
				buy_value++;
			}
			//buy_value += STEP;
			//sell_value = buy_value - (PRICE_DIFF*2);
			buysell_count = 0;
			this.lastaction = LastAction.BUY;
		}
		this.save();
	}
	
	public void decrement() {
		buysell_count--;
		
		if ((((-buysell_count) % -STEP_COUNT) == 0) || (stackable == false)) {
			sell_value -= this.step;
			buy_value = sell_value + (int)((double)sell_value*RATE);
			
			if (buy_value == sell_value) {
				buy_value++;
			}
			
			//buy_value -= STEP;
			//sell_value = buy_value - (PRICE_DIFF*2);
			
			if (sell_value < this.step) {
				//buy_value = STEP + (PRICE_DIFF*2);
				sell_value = this.step;
				buy_value = this.step*2;
			}
			this.lastaction = LastAction.SELL;
			buysell_count = 0;
		}
		this.save();
	}

	public boolean isStackable() {
		return this.stackable;
	}

}
