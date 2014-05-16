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
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.inventory.Inventory;

import com.avrgaming.civcraft.components.ConsumeLevelComponent;
import com.avrgaming.civcraft.components.ConsumeLevelComponent.Result;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCottageLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class Cottage extends Structure {

	private ConsumeLevelComponent consumeComp = null;
	
	protected Cottage(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
	}

	public Cottage(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}
	
	public ConsumeLevelComponent getConsumeComponent() {
		if (consumeComp == null) {
			consumeComp = (ConsumeLevelComponent) this.getComponent(ConsumeLevelComponent.class.getSimpleName());
		}
		return consumeComp;
	}
	
	@Override
	public void loadSettings() {
		super.loadSettings();
		
//		attrComp = new AttributeComponent();
//		attrComp.setType(AttributeType.DIRECT);
//		attrComp.setOwnerKey(this.getTown().getName());
//		attrComp.setAttrKey(Attribute.TypeKeys.COINS.name());
//		attrComp.setSource("Cottage("+this.getCorner().toString()+")");
//		attrComp.registerComponent();
	}

	
	@Override
	public String getDynmapDescription() {
		if (getConsumeComponent() == null) {
			return "";
		}
		
		String out = "";
		out += "Level: "+getConsumeComponent().getLevel()+" "+getConsumeComponent().getCountString();
		return out;
	}
	
	@Override
	public String getMarkerIconName() {
		return "house";
	}
	
	public String getkey() {
		return this.getTown().getName()+"_"+this.getConfigId()+"_"+this.getCorner().toString(); 
	}

	/*
	 * Returns true if the granary has been poisoned, false otherwise.
	 */
	public boolean processPoison(MultiInventory inv) {
		//Check to make sure the granary has not been poisoned!
		String key = "posiongranary:"+getTown().getName();
		ArrayList<SessionEntry> entries;
		entries = CivGlobal.getSessionDB().lookup(key);
		int max_poison_ticks = -1;
		for (SessionEntry entry : entries) {
			int next = Integer.valueOf(entry.value);
			
			if (next > max_poison_ticks) {
				max_poison_ticks = next;
			} 
		}
		
		if (max_poison_ticks > 0) {
			CivGlobal.getSessionDB().delete_all(key);
			max_poison_ticks--;
			
			if (max_poison_ticks > 0)
				CivGlobal.getSessionDB().add(key, ""+max_poison_ticks, this.getTown().getCiv().getId(), this.getTown().getId(), this.getId());
	
			// Add some rotten flesh to the chest lol
			CivMessage.sendTown(this.getTown(), CivColor.Rose+"Our granaries have been poisoned!!");
			inv.addItem(ItemManager.createItemStack(CivData.ROTTEN_FLESH, 4));
			return true;
		}
		return false;
	}
	
	public void generateCoins(CivAsyncTask task) {
		
		if (!this.isActive()) {
			return;
		}
		
		/* Build a multi-inv from granaries. */
		MultiInventory multiInv = new MultiInventory();

		for (Structure struct : this.getTown().getStructures()) {
			if (struct instanceof Granary) {
				ArrayList<StructureChest> chests = struct.getAllChestsById(1);
				
				// Make sure the chunk is loaded and add it to the inventory.
				try {
					for (StructureChest c : chests) {
						task.syncLoadChunk(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getZ());
						Inventory tmp;
						try {
							tmp = task.getChestInventory(c.getCoord().getWorldname(), c.getCoord().getX(), c.getCoord().getY(), c.getCoord().getZ(), true);
							multiInv.addInventory(tmp);
						} catch (CivTaskAbortException e) {
							e.printStackTrace();
						}
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}
		}
		getConsumeComponent().setSource(multiInv);
		
		double cottage_consume_mod = 1.0; //allows buildings and govs to change the totals for cottage consumption.

		if (this.getTown().getBuffManager().hasBuff(Buff.REDUCE_CONSUME)) {
			cottage_consume_mod *= this.getTown().getBuffManager().getEffectiveDouble(Buff.REDUCE_CONSUME);
		}
		if (this.getTown().getBuffManager().hasBuff("buff_pyramid_cottage_consume")) {
			cottage_consume_mod *= this.getTown().getBuffManager().getEffectiveDouble("buff_pyramid_cottage_consume");
		}
		
		if (this.getTown().getBuffManager().hasBuff(Buff.FISHING)) {
			// XXX change this to config var after testing...
			int breadPerFish = this.getTown().getBuffManager().getEffectiveInt(Buff.FISHING);
			getConsumeComponent().addEquivExchange(CivData.BREAD, CivData.FISH_RAW, breadPerFish);
		}
		
		getConsumeComponent().setConsumeRate(cottage_consume_mod);
		Result result = getConsumeComponent().processConsumption();
		getConsumeComponent().onSave();
		getConsumeComponent().clearEquivExchanges();
		
		/* Bail early for results that do not generate coins. */
		switch (result) {
		case STARVE:
			CivMessage.sendTown(getTown(), CivColor.LightGreen+"A level "+getConsumeComponent().getLevel()+" cottage "+CivColor.Rose+"starved"+
					getConsumeComponent().getCountString()+CivColor.LightGreen+" and generated no coins.");
			return;
		case LEVELDOWN:
			CivMessage.sendTown(getTown(), CivColor.LightGreen+"A level "+(getConsumeComponent().getLevel()+1)+" cottage "+CivColor.Red+"leveled-down"+CivColor.LightGreen+" and generated no coins.");
			return;
		case STAGNATE:
			CivMessage.sendTown(getTown(), CivColor.LightGreen+"A level "+getConsumeComponent().getLevel()+" cottage "+CivColor.Yellow+"stagnated"+getConsumeComponent().getCountString()+CivColor.LightGreen+" and generated no coins.");
			return;
		case UNKNOWN:
			CivMessage.sendTown(getTown(), CivColor.LightGreen+CivColor.LightGreen+"Something "+CivColor.DarkPurple+"unknown"+CivColor.LightGreen+" happened to a cottage. It generates no coins.");
			return;
		default:
			break;
		}
		
		if (processPoison(multiInv)) {
			return;
		}
		
		/* Calculate how much money we made. */
		/* XXX leveling down doesnt generate coins, so we don't have to check it here. */
		ConfigCottageLevel lvl = null;
		if (result == Result.LEVELUP) {
			lvl = CivSettings.cottageLevels.get(getConsumeComponent().getLevel()-1);	
		} else {
			lvl = CivSettings.cottageLevels.get(getConsumeComponent().getLevel());
		}
				
		int total_coins = (int)Math.round(lvl.coins*this.getTown().getCottageRate());
		if (this.getTown().getBuffManager().hasBuff("buff_pyramid_cottage_bonus")) {
			total_coins *= this.getTown().getBuffManager().getEffectiveDouble("buff_pyramid_cottage_bonus");
		}
		
		if (this.getCiv().hasTechnology("tech_taxation")) {
			double taxation_bonus;
			try {
				taxation_bonus = CivSettings.getDouble(CivSettings.techsConfig, "taxation_cottage_buff");
				total_coins *= taxation_bonus;
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
			}
		}
		
	//	this.getTown().depositTaxed(total_coins);
	//	attrComp.setValue(total_coins);
		double taxesPaid = total_coins*this.getTown().getDepositCiv().getIncomeTaxRate();

		String stateMessage = "";
		switch (result) {
		case GROW:
			stateMessage = CivColor.Green+"grew"+getConsumeComponent().getCountString()+CivColor.LightGreen;
			break;
		case LEVELUP:
			stateMessage = CivColor.Green+"leveled up"+CivColor.LightGreen;
			break;
		case MAXED:
			stateMessage = CivColor.Green+"is maxed"+getConsumeComponent().getCountString()+CivColor.LightGreen;
			break;
		default:
			break;
		}
		
		if (taxesPaid > 0) {
			CivMessage.sendTown(this.getTown(), CivColor.LightGreen+"A level "+getConsumeComponent().getLevel()+" cottage "+stateMessage+" and generated "+total_coins+" coins!"+
					CivColor.Yellow+" (Paid "+taxesPaid+" in taxes to "+this.getTown().getDepositCiv().getName()+")");
		} else {
			CivMessage.sendTown(this.getTown(), CivColor.LightGreen+"A level "+getConsumeComponent().getLevel()+" cottage "+stateMessage+" and generated "+total_coins+" coins!");
		}
		
		this.getTown().getTreasury().deposit(total_coins - taxesPaid);
		this.getTown().getDepositCiv().taxPayment(this.getTown(), taxesPaid);
	}
	
	public int getLevel() {
		return getConsumeComponent().getLevel();
	}
	
	public Result getLastResult() {
		return getConsumeComponent().getLastResult();
	}

	public int getCount() {
		return getConsumeComponent().getCount();
	}

	public int getMaxCount() {
		int level = getLevel();
		
		ConfigCottageLevel lvl = CivSettings.cottageLevels.get(level);
		return lvl.count;
	}

	public double getCoinsGenerated() {
		int level = getLevel();
		
		ConfigCottageLevel lvl = CivSettings.cottageLevels.get(level);
		if (lvl == null) {
			return 0;
		}
		return lvl.coins;
	}

	public void delevel() {
		int currentLevel = getLevel();
		
		if (currentLevel > 1) {
			getConsumeComponent().setLevel(getLevel()-1);
			getConsumeComponent().setCount(0);
			getConsumeComponent().onSave();
		}
	}
	
	@Override
	public void delete() throws SQLException {
		super.delete();
		if (getConsumeComponent() != null) {
			getConsumeComponent().onDelete();
		}
	}
	
	public void onDestroy() {
		super.onDestroy();
		
		getConsumeComponent().setLevel(1);
		getConsumeComponent().setCount(0);
		getConsumeComponent().onSave();
	}
}
