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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.TreeMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigUnit;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.interactive.InteractiveRepairItem;
import com.avrgaming.civcraft.items.components.RepairCost;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.UnitSaveAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;

public class Barracks extends Structure {

	private static final long SAVE_INTERVAL = 60*1000;

	private int index = 0;
	private StructureSign unitNameSign;
	
	private ConfigUnit trainingUnit = null;
	private double currentHammers = 0.0;
	
	private TreeMap<Integer, StructureSign> progresBar = new TreeMap<Integer, StructureSign>();
	private Date lastSave = null;
	
	protected Barracks(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
	}

	public Barracks(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	private String getUnitSignText(int index) throws IndexOutOfBoundsException {
		ArrayList<ConfigUnit> unitList = getTown().getAvailableUnits();
		
		if (unitList.size() == 0) {
			return "\n"+CivColor.LightGray+"None\n"+CivColor.LightGray+"Available";			
		}
		
		ConfigUnit unit = unitList.get(index);
		String out = "\n";
		out += CivColor.LightPurple+unit.name+"\n";
		out += CivColor.Yellow+unit.cost+"\n";
		out += CivColor.Yellow+"coins";
		
		return out;
	}
	
	private void changeIndex(int newIndex) {
		if (this.unitNameSign != null) {
			try {
				this.unitNameSign.setText(getUnitSignText(newIndex));
				index = newIndex;
			} catch (IndexOutOfBoundsException e) {
				//index = 0;
				//this.unitNameSign.setText(getUnitSignText(index));
			}
			this.unitNameSign.update();
		} else {
			CivLog.warning("Could not find unit name sign for barracks:"+this.getId()+" at "+this.getCorner());
		}
	}
	
	
	private void train(Resident whoClicked) throws CivException {
		ArrayList<ConfigUnit> unitList = getTown().getAvailableUnits();

		ConfigUnit unit = unitList.get(index);
		if (unit == null) {
			throw new CivException("Unknown unit type.");
		}
		
		if (unit.limit != 0 && unit.limit < getTown().getUnitTypeCount(unit.id)) {
			throw new CivException("We've reached the maximum number of "+unit.name+" units we can have.");
		}
		
		if (!getTown().getTreasury().hasEnough(unit.cost)) {
			throw new CivException("Not enough coins to train unit. We require "+unit.cost+" coins.");
		}
		
		if (!unit.isAvailable(getTown())) {
			throw new CivException("This unit is unavailable.");
		}
		
		if (this.trainingUnit != null) {
			throw new CivException("Already training a "+this.trainingUnit.name+".");
		}
		
		if (unit.id.equals("u_settler")) {
			if (!this.getCiv().getLeaderGroup().hasMember(whoClicked) && !this.getCiv().getAdviserGroup().hasMember(whoClicked)) {
				throw new CivException("You must be an adivser to the civilization in order to build a Settler.");
			}
		}
		
		
		getTown().getTreasury().withdraw(unit.cost);
		
		
		this.setCurrentHammers(0.0);
		this.setTrainingUnit(unit);
		CivMessage.sendTown(getTown(), "We've begun training a "+unit.name+"!");
		this.updateTraining();
	}
	
	@Override
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
		//int special_id = Integer.valueOf(sign.getAction());
		Resident resident = CivGlobal.getResident(player);
		
		if (resident == null) {
			return;
		}
		
		switch (sign.getAction()) {
		case "prev":
			changeIndex((index-1));
			break;
		case "next":
			changeIndex((index+1));
			break;
		case "train":
			if (resident.hasTown()) {
				try {
				if (getTown().getAssistantGroup().hasMember(resident) || getTown().getMayorGroup().hasMember(resident)) {
					train(resident);
				} else {
					throw new CivException("Only Mayors and Assistants may train units.");
				}
				} catch (CivException e) {
					CivMessage.send(player, CivColor.Rose+e.getMessage());
				}
			}
			break;
		case "repair_item":
			repairItem(player, resident, event);			
			break;
		}
	}
	
	private void repairItem(Player player, Resident resident, PlayerInteractEvent event) {
		try {
			ItemStack inHand = player.getItemInHand();
			if (inHand == null || inHand.getType().equals(Material.AIR)) {
				throw new CivException("Must have an item in your hand in order to repair it.");
			}
			
			if (inHand.getType().getMaxDurability() == 0) {
				throw new CivException("Can only repair items that use durability.");
			}
			
			if (inHand.getDurability() == 0) {
				throw new CivException("This item is already at full health.");
			}
			
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(inHand);
			if (craftMat == null) {
				throw new CivException("Cannot repair this item.");
			}
			
			try {
				double totalCost;
				if (craftMat.hasComponent("RepairCost")) {
					RepairCost repairCost = (RepairCost)craftMat.getComponent("RepairCost");
					totalCost = repairCost.getDouble("value");
				} else {
					double baseTierRepair = CivSettings.getDouble(CivSettings.structureConfig, "barracks.base_tier_repair");
					double tierDamp = CivSettings.getDouble(CivSettings.structureConfig, "barracks.tier_damp");
					double tierCost = Math.pow((craftMat.getConfigMaterial().tier), tierDamp);				
					double fromTier = Math.pow(baseTierRepair, tierCost);				
					totalCost = Math.round(fromTier+0);
				}
				
				InteractiveRepairItem repairItem = new InteractiveRepairItem(totalCost, player.getName(), craftMat);
				repairItem.displayMessage();
				resident.setInteractiveMode(repairItem);
				return;
				
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				throw new CivException("Internal configuration error");
			}
			
			
			
		} catch (CivException e) {
			CivMessage.sendError(player, e.getMessage());
			event.setCancelled(true);
		}
	}

	public static void repairItemInHand(double cost, String playerName, LoreCraftableMaterial craftMat) {
		Player player;
		
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		
		Resident resident = CivGlobal.getResident(player);
		
		if (!resident.getTreasury().hasEnough(cost)) {
			CivMessage.sendError(player, "Sorry, but you don't have the required "+cost+" coins.");
			return;
		}
		
		LoreCraftableMaterial craftMatInHand = LoreCraftableMaterial.getCraftMaterial(player.getItemInHand());
		
		if (!craftMatInHand.getConfigId().equals(craftMat.getConfigId())) {
			CivMessage.sendError(player, "You're not holding the item that you started the repair with.");
			return;
		}
		
		resident.getTreasury().withdraw(cost);
		player.getItemInHand().setDurability((short)0);
		
		CivMessage.sendSuccess(player, "Repaired "+craftMat.getName()+" for "+cost+" coins.");
		
	}
	
	@Override
	public void onTechUpdate() {
		
		class BarracksSyncUpdate implements Runnable {

			StructureSign unitNameSign;
			
			public BarracksSyncUpdate(StructureSign unitNameSign) {
				this.unitNameSign = unitNameSign;
			}
			
			@Override
			public void run() {

				this.unitNameSign.setText(getUnitSignText(index));
				this.unitNameSign.update();
			}
		}
		
		TaskMaster.syncTask(new BarracksSyncUpdate(this.unitNameSign));
		
	}
		
	@Override
	public void onPostBuild(BlockCoord absCoord, SimpleBlock sb) {
		StructureSign structSign;

		switch (sb.command) {
		case "/prev":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());
			structSign = new StructureSign(absCoord, this);
			structSign.setText("\n"+ChatColor.BOLD+ChatColor.UNDERLINE+"Prev Unit");
			structSign.setDirection(sb.getData());
			structSign.setAction("prev");
			structSign.update();
			this.addStructureSign(structSign);
			CivGlobal.addStructureSign(structSign);
			
			break;
		case "/unitname":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new StructureSign(absCoord, this);
			structSign.setText(getUnitSignText(0));
			structSign.setDirection(sb.getData());
			structSign.setAction("info");
			structSign.update();
			
			this.unitNameSign = structSign;
			
			this.addStructureSign(structSign);
			CivGlobal.addStructureSign(structSign);
			
			break;
		case "/next":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new StructureSign(absCoord, this);
			structSign.setText("\n"+ChatColor.BOLD+ChatColor.UNDERLINE+"Next Unit");
			structSign.setDirection(sb.getData());
			structSign.setAction("next");
			structSign.update();
			this.addStructureSign(structSign);
			CivGlobal.addStructureSign(structSign);
						
			break;
		case "/train":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new StructureSign(absCoord, this);
			structSign.setText("\n"+ChatColor.BOLD+ChatColor.UNDERLINE+"Train");
			structSign.setDirection(sb.getData());
			structSign.setAction("train");
			structSign.update();
			this.addStructureSign(structSign);
			CivGlobal.addStructureSign(structSign);
			
			break;
		case "/progress":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new StructureSign(absCoord, this);
			structSign.setText("");
			structSign.setDirection(sb.getData());
			structSign.setAction("");
			structSign.update();
			this.addStructureSign(structSign);
			CivGlobal.addStructureSign(structSign);
			
			this.progresBar.put(Integer.valueOf(sb.keyvalues.get("id")), structSign);
			
			break;
		case "/repair":
			ItemManager.setTypeId(absCoord.getBlock(), sb.getType());
			ItemManager.setData(absCoord.getBlock(), sb.getData());

			structSign = new StructureSign(absCoord, this);
			structSign.setText("\n"+ChatColor.BOLD+ChatColor.UNDERLINE+"Repair Item");
			structSign.setDirection(sb.getData());
			structSign.setAction("repair_item");
			structSign.update();
			this.addStructureSign(structSign);
			CivGlobal.addStructureSign(structSign);
			
			break;

		}
	}

	public int getIndex() {
		return index;
	}

	public void setIndex(int index) {
		this.index = index;
	}

	public ConfigUnit getTrainingUnit() {
		return trainingUnit;
	}

	public void setTrainingUnit(ConfigUnit trainingUnit) {
		this.trainingUnit = trainingUnit;
	}

	public double getCurrentHammers() {
		return currentHammers;
	}

	public void setCurrentHammers(double currentHammers) {
		this.currentHammers = currentHammers;
	}

	public void createUnit(ConfigUnit unit) {
		
		// Find the chest inventory
		ArrayList<StructureChest> chests = this.getAllChestsById(0);
		if (chests.size() == 0) {
			return;
		}
		
		Chest chest = (Chest)chests.get(0).getCoord().getBlock().getState();
		
		try {
			Class<?> c = Class.forName(unit.class_name);
			Method m = c.getMethod("spawn", Inventory.class, Town.class);
			m.invoke(null, chest.getInventory(), this.getTown());
			
			CivMessage.sendTown(this.getTown(), "Completed a "+unit.name+"!");
			this.trainingUnit = null;
			this.currentHammers = 0.0;
			
			CivGlobal.getSessionDB().delete_all(getSessionKey());
			
		} catch (ClassNotFoundException | SecurityException | 
				IllegalAccessException | IllegalArgumentException | NoSuchMethodException e) {
			this.trainingUnit = null;
			this.currentHammers = 0.0;
			CivMessage.sendTown(getTown(), CivColor.Red+"ERROR couldn't find class?:"+e.getMessage());
		} catch (InvocationTargetException e) {
			CivMessage.sendTown(getTown(), CivColor.Rose+e.getCause().getMessage());
			this.currentHammers -= 20.0;
			if (this.currentHammers < 0.0) {
				this.currentHammers = 0.0;
			}
		//	e.getCause().getMessage()
			//e.printStackTrace();
		//	CivMessage.sendTown(getTown(), CivColor.Rose+e.getMessage());
		}
		
	}
	
	public void updateProgressBar() {
		double percentageDone = 0.0;
		
		percentageDone = this.currentHammers / this.trainingUnit.hammer_cost;
		int size = this.progresBar.size();
		int textCount = (int)(size*16*percentageDone);
		int textIndex = 0;
		
		for (int i = 0; i < size; i++) {
			StructureSign structSign = this.progresBar.get(i);
			String[] text = new String[4];
			text[0] = "";
			text[1] = "";
			text[2] = "";
			text[3] = "";
			for (int j = 0; j < 16; j++) {
				if (textIndex == 0) {
					text[2] += "[";
				} else if (textIndex == ((size*15)+3)) {
					text[2] += "]";
				} else if (textIndex < textCount) {
					text[2] += "=";
				} else {
					text[2] += "_";
				}
	
				textIndex++;
			}
	
			if (i == (size/2)) {
				text[1] = CivColor.LightGreen+this.trainingUnit.name;
			}
			
			structSign.setText(text);
			structSign.update();
		}
				
	}
	
	public String getSessionKey() {
		return this.getTown().getName()+":"+"barracks"+":"+this.getId();
	}

	public void saveProgress() {
		if (this.getTrainingUnit() != null) {
			String key = getSessionKey();
			String value = this.getTrainingUnit().id+":"+this.currentHammers; 
			ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);

			if (entries.size() > 0) {
				SessionEntry entry = entries.get(0);
				CivGlobal.getSessionDB().update(entry.request_id, key, value);
				
				/* delete any bad extra entries. */
				for (int i = 1; i < entries.size(); i++) {
					SessionEntry bad_entry = entries.get(i);
					CivGlobal.getSessionDB().delete(bad_entry.request_id, key);
				}
			} else {
				this.sessionAdd(key, value);
			}
			
			lastSave = new Date();
		}	
	}
	
	@Override
	public void onUnload() {
		saveProgress();
	}
	
	@Override
	public void onLoad() {
		String key = getSessionKey();
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);
	
		if (entries.size() > 0) {
			SessionEntry entry = entries.get(0);
			String[] values = entry.value.split(":");
			
			this.trainingUnit = CivSettings.units.get(values[0]);
			
			if (trainingUnit == null) {
				CivLog.error("Couldn't find in-progress unit id:"+values[0]+" for town "+this.getTown().getName());
				return;
			}
			
			this.currentHammers = Double.valueOf(values[1]);
			
			/* delete any bad extra entries. */
			for (int i = 1; i < entries.size(); i++) {
				SessionEntry bad_entry = entries.get(i);
				CivGlobal.getSessionDB().delete(bad_entry.request_id, key);
			}
		} 
	}
	
	
	public void updateTraining() {
		if (this.trainingUnit != null) {
			// Hammers are per hour, this runs per min. We need to adjust the hammers we add.
			double addedHammers = (getTown().getHammers().total / 60) / 60;
			this.currentHammers += addedHammers;
			
			
			this.updateProgressBar();
			Date now = new Date();
			
			if (lastSave == null || ((lastSave.getTime() + SAVE_INTERVAL) < now.getTime())) {
				TaskMaster.asyncTask(new UnitSaveAsyncTask(this), 0);
			}
			
			if (this.currentHammers >= this.trainingUnit.hammer_cost) {
				this.currentHammers = this.trainingUnit.hammer_cost;
				this.createUnit(this.trainingUnit);
			}
			
		}
	}
	
}
