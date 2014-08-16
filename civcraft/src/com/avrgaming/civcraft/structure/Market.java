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

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.LinkedList;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigMarketItem;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;

public class Market extends Structure {

	public HashMap<Integer, LinkedList<StructureSign>> signIndex = new HashMap<Integer, LinkedList<StructureSign>>();
	
	public static int BULK_AMOUNT = 64;
		
	protected Market(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		CivGlobal.addMarket(this);
	}

	public Market(ResultSet rs) throws SQLException, CivException {
		super(rs);
		CivGlobal.addMarket(this);
	}
	
	@Override
	public void delete() throws SQLException {
		super.delete();
		CivGlobal.removeMarket(this);
	}
	
	public static void globalSignUpdate(int id) {
		for (Market market : CivGlobal.getMarkets()) {
			
			LinkedList<StructureSign> signs = market.signIndex.get(id);
			if (signs == null) {
				continue;
			}
			
			for (StructureSign sign : signs) {			
				ConfigMarketItem item = CivSettings.marketItems.get(id);
				if (item != null) {
					try {
					market.setSignText(sign, item);
					} catch (ClassCastException e) {
						CivLog.error("Can't cast structure sign to sign for market update.");
						continue;
					}
				}
			}
		}
	}
	
	public void processBuy(Player player, Resident resident, int bulkCount, ConfigMarketItem item) throws CivException {
		item.buy(resident, player, bulkCount);
	}
	
	public void processSell(Player player, Resident resident, int bulkCount, ConfigMarketItem item) throws CivException {
		item.sell(resident, player, bulkCount);
	}
	
	@SuppressWarnings("deprecation")
	@Override
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) throws CivException {
		
		Integer id = Integer.valueOf(sign.getType());
		ConfigMarketItem item = CivSettings.marketItems.get(id);
		Resident resident = CivGlobal.getResident(player);

		if (resident == null) {
			CivMessage.sendError(player, "You're not registerd?? what??");
			return;
		}
		
		if (item == null) {
			CivMessage.sendError(player, "ERROR: Unknown item. Market ID:"+id);
			return;
		}
		
		switch (sign.getAction().toLowerCase()) {
		case "sellbig":
			processSell(player, resident, BULK_AMOUNT, item);
			break;
		case "sell":
			processSell(player, resident, 1, item);
			break;
		case "buy":
			processBuy(player, resident, 1, item);
			break;
		case "buybig":
			processBuy(player, resident, BULK_AMOUNT, item);
			break;
		}
	
		player.updateInventory();
		Market.globalSignUpdate(id);
	}
	
	public void setSignText(StructureSign sign, ConfigMarketItem item) {

		String itemColor;
		switch (item.lastaction) {
		case BUY:
			itemColor = CivColor.LightGreen;
			break;
		case SELL:
			itemColor = CivColor.Rose;
			break;
		default:
			itemColor = CivColor.Black;
			break;
		}
		
		try {
		Sign s;
		switch (sign.getAction().toLowerCase()) {
		case "sellbig":
			s = (Sign)sign.getCoord().getBlock().getState();
			s.setLine(0, ChatColor.BOLD+"Sell Bulk");
			s.setLine(1, item.name);
			s.setLine(2, itemColor+item.getSellCostForAmount(BULK_AMOUNT)+" Coins");
			s.setLine(3, "Amount "+BULK_AMOUNT);
			s.update();
			break;
		case "sell":
			s = (Sign)sign.getCoord().getBlock().getState();
			s.setLine(0, ChatColor.BOLD+"Sell");
			s.setLine(1, item.name);
			s.setLine(2, itemColor+item.getSellCostForAmount(1)+" Coins");
			s.setLine(3, "Amount 1");
			s.update();
			break;
		case "buy":
			s = (Sign)sign.getCoord().getBlock().getState();
			s.setLine(0, ChatColor.BOLD+"Buy");
			s.setLine(1, item.name);
			s.setLine(2, itemColor+item.getBuyCostForAmount(1)+" Coins");
			s.setLine(3, "Amount 1");
			s.update();
			break;
		case "buybig":
			s = (Sign)sign.getCoord().getBlock().getState();
			s.setLine(0, ChatColor.BOLD+"Buy Bulk");
			s.setLine(1, item.name);
			s.setLine(2, itemColor+item.getBuyCostForAmount(BULK_AMOUNT)+" Coins");
			s.setLine(3, "Amount "+BULK_AMOUNT);
			s.update();
			break;
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void buildSign(String action, Integer id, BlockCoord absCoord, 
			ConfigMarketItem item, SimpleBlock commandBlock) {
		Block b = absCoord.getBlock();
		
		ItemManager.setTypeIdAndData(b, ItemManager.getId(Material.WALL_SIGN), (byte)commandBlock.getData(), false);
		
		StructureSign structSign = CivGlobal.getStructureSign(absCoord);
		if (structSign == null) {
			structSign = new StructureSign(absCoord, this);
		}
		
		structSign.setDirection(ItemManager.getData(b.getState()));
		structSign.setType(""+id);
		structSign.setAction(action);

		structSign.setOwner(this);
		this.addStructureSign(structSign);
		CivGlobal.addStructureSign(structSign);
		
		LinkedList<StructureSign> signs = this.signIndex.get(id);
		if (signs == null) {
			signs = new LinkedList<StructureSign>();
		}
	
		signs.add(structSign);
		this.signIndex.put(id, signs);
		this.setSignText(structSign, item);
	}
	
	@Override
	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
		Integer id;
		ConfigMarketItem item;
		switch (commandBlock.command.toLowerCase().trim()) {
		case "/sellbig":
			id = Integer.valueOf(commandBlock.keyvalues.get("id"));
			item = CivSettings.marketItems.get(id);
			if (item != null) {
				if (item.isStackable()) {
					buildSign("sellbig", id, absCoord, item, commandBlock);
				}
			}
			break;
		case "/sell":
			id = Integer.valueOf(commandBlock.keyvalues.get("id"));
			
			item = CivSettings.marketItems.get(id);
			if (item != null) {
				buildSign("sell", id, absCoord, item, commandBlock);
			}		
			break;
		case "/buy":
			id = Integer.valueOf(commandBlock.keyvalues.get("id"));
			item = CivSettings.marketItems.get(id);
			if (item != null) {
				buildSign("buy", id, absCoord, item, commandBlock);
			}		
			break;
		case "/buybig":
			id = Integer.valueOf(commandBlock.keyvalues.get("id"));
			item = CivSettings.marketItems.get(id);
			if (item != null) {
				if (item.isStackable()) {
					buildSign("buybig", id, absCoord, item, commandBlock);
				}
			}		
			break;
		}
	}

	

	
	

	
}
