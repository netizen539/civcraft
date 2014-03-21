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
package com.avrgaming.civcraft.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCottageLevel;
import com.avrgaming.civcraft.config.ConfigMineLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Cottage;
import com.avrgaming.civcraft.structure.Mine;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class ConsumeLevelComponent extends Component {

	/* Current level we're operating at. */
	private int level;
	
	/* Current count we have in this level. */
	private int count;
	
	/* Last result. */
	private Result lastResult;
	
	/* Consumption mod rate, can be used to increase or decrease consumption rates. */
	private double consumeRate;
	
	/* Buildable this component is attached to. */
//	private Buildable buildable;
	
	/* 
	 * The first key is the level id, followed by a hashmap containing integer, amount entries
	 * for each item consumed for that level. For each item in the hashmap, we must have ALL of the items in the inventory. 
	 */
	private HashMap<Integer, Map<Integer, Integer>> consumptions = new HashMap<Integer, Map<Integer, Integer>>();
	
	/*
	 * Contains a list of equivilent exchanges 
	 */
	private HashMap<Integer, ConsumeLevelEquivExchange> exchanges = new HashMap<Integer, ConsumeLevelEquivExchange>();
	
	/* Last found counts from call to hasEnoughToConsume */
	private Map<Integer, Integer> foundCounts;
	
	/*
	 * Contains a hashmap of levels and counts configured for this component.
	 */
	private HashMap<Integer, Integer> levelCounts = new HashMap<Integer, Integer>();
		
	/* Inventory we're trying to pull from. */
	private MultiInventory source;
	
//	consumeComp.createComponent(this);

	
	@Override
	public void createComponent(Buildable buildable, boolean async) {
		super.createComponent(buildable, async);
		
		//XXX make both mine/cottage/longhouse levels similar in the yml so they can be loaded
		// without this check.
		if (buildable instanceof Cottage) {
			for (ConfigCottageLevel lvl : CivSettings.cottageLevels.values()) {
				this.addLevel(lvl.level, lvl.count);
				this.setConsumes(lvl.level, lvl.consumes);
			}
		} else if (buildable instanceof Mine) {
			for (ConfigMineLevel lvl : CivSettings.mineLevels.values()) {
				this.addLevel(lvl.level, lvl.count);
				
				HashMap<Integer, Integer> redstoneAmounts = new HashMap<Integer, Integer>();
				redstoneAmounts.put(CivData.REDSTONE_DUST, lvl.amount);
				this.setConsumes(lvl.level, redstoneAmounts);
			}
		}
		
	}
	
	
	/* Possible Results. */
	public enum Result {
		STAGNATE,
		GROW,
		STARVE,
		LEVELUP,
		LEVELDOWN,
		MAXED,
		UNKNOWN
	}
	
	public ConsumeLevelComponent() {
		this.level = 1;
		this.count = 0;
		this.consumeRate = 1.0;
		this.lastResult = Result.UNKNOWN;
	}
	
	private String getKey() {
		return getBuildable().getDisplayName()+":"+getBuildable().getId()+":"+"levelcount";
	}
	
	private String getValue() {
		return this.level+":"+this.count;
	}
	
	@Override
	public void onLoad() {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getKey());
		
		if (entries.size() == 0) {
			getBuildable().sessionAdd(getKey(), getValue());
			return;
		}
		
		String[] split = entries.get(0).value.split(":");
		this.level = Integer.valueOf(split[0]);
		this.count = Integer.valueOf(split[1]);		
	}

	@Override
	public void onSave() {
		
		class AsyncTask implements Runnable {		
			@Override
			public void run() {
				ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getKey());
				
				if (entries.size() == 0) {
					getBuildable().sessionAdd(getKey(), getValue());
					return;
				}
				
				CivGlobal.getSessionDB().update(entries.get(0).request_id, getKey(), getValue());
			}
		}
				
		if (getBuildable().getId() != 0) {
			TaskMaster.asyncTask(new AsyncTask(), 0);
		}
	}
	
	public void onDelete() {
		class AsyncTask implements Runnable {		
			@Override
			public void run() {
				CivGlobal.getSessionDB().delete_all(getKey());
			}
		}
		
		if (getBuildable().getId() != 0) {
			TaskMaster.asyncTask(new AsyncTask(), 0);
		}
	}
	
	public void addLevel(int level, int count) {
		levelCounts.put(level, count);
	}
	
	public void setConsumes(int level, Map<Integer, Integer> consumes) {
		this.consumptions.put(level, consumes);
	}
	
	public void addEquivExchange(int baseType, int altType, int basePerAlt) {
		ConsumeLevelEquivExchange ee = new ConsumeLevelEquivExchange();
		ee.baseType = baseType;
		ee.altType = altType;
		ee.basePerAlt = basePerAlt;		
		this.exchanges.put(baseType, ee);
	}
	
	public void removeEquivExchange(int baseType) {
		this.exchanges.remove(baseType);
	}
	
	public void setSource(MultiInventory source) {
		this.source = source;
	}
	
	public int getConsumedAmount(int amount) {
		return (int) Math.max(1, amount*this.consumeRate);
	}
	
	private boolean hasEnoughToConsume() {
		
		Map<Integer, Integer> thisLevelConsumptions = consumptions.get(this.level);
		if (thisLevelConsumptions == null) {
			return false;
		}
		
		foundCounts = new HashMap<Integer, Integer>();
		for (ItemStack stack : source.getContents()) {
			if (stack == null) {
				continue;
			}
			
			boolean isRequire = thisLevelConsumptions.containsKey(ItemManager.getId(stack));
			boolean isEquiv = false;
			
			for (ConsumeLevelEquivExchange ee : this.exchanges.values()) {
				if (ee.altType == ItemManager.getId(stack)) {
					isEquiv = true;
					break;
				}
			}
		//	CivLog.debug("type:"+stack.getTypeId()+" equiv:"+isEquiv);
			
			if (!isRequire && !isEquiv) {
				continue;
			}
			
			Integer count = foundCounts.get(ItemManager.getId(stack));
			if (count == null) {
				count = stack.getAmount();
			} else {
				count += stack.getAmount();
			}
			foundCounts.put(ItemManager.getId(stack), count);
		}
			
		boolean found = true;
		for (Integer typeID : thisLevelConsumptions.keySet()) {
			Integer foundCount = foundCounts.get(typeID);
			Integer requireCount = thisLevelConsumptions.get(typeID);
			ConsumeLevelEquivExchange ee = this.exchanges.get(typeID);
						
			if (foundCount == null) {
				foundCount = 0;
			}
			
			if (foundCount < getConsumedAmount(requireCount)) {
				if (ee == null) {
					found = false;
					break;
				}
				
				/* We found an EE for this consumable, and we didn't have enough
				 * on our own. So instead, we'll try to see if we have enough if we use
				 * our EE.
				 */
				Integer totalBase = foundCounts.get(ee.baseType);
				if (totalBase == null) {
					totalBase = 0;
				}
				
				Integer totalAlt = foundCounts.get(ee.altType);
				if (totalAlt == null) {
					totalAlt = 0;
				}
				
				int total = totalBase + (totalAlt*ee.basePerAlt);
				if (total < getConsumedAmount(requireCount)) {
					found = false;
					break;
				}
				/* We have enough if we include the EE amount. */
			}
		}
		
		return found;
	}
	
	private void consumeFromInventory() {
		if (foundCounts == null) {
			return;
		}
		
		Map<Integer, Integer> thisLevelConsumptions = consumptions.get(this.level);

		for (Integer typeID : thisLevelConsumptions.keySet()) {
			Integer amount = thisLevelConsumptions.get(typeID);
			Integer count = foundCounts.get(typeID);
		
			amount = getConsumedAmount(amount);
			
			if (count == null) {
				count = 0;
			}
			
			if (count < amount) {
				//Try to use the EE. 
				ConsumeLevelEquivExchange ee = exchanges.get(typeID);
				if (ee == null) {
					/* This should never happen since we should have checked for this before calling this function. */
					CivLog.warning("Couldn't consume enough "+typeID+" and no EE was found!");
					continue;
				} else {
					/* Lets try to use the total from the EE to supplement the base item we didn't have enough of. */
					Integer totalBase = foundCounts.get(ee.baseType);
					if (totalBase == null) {
						totalBase = 0;
					}
					
					Integer totalAlt = foundCounts.get(ee.altType);
					if (totalAlt == null) {
						totalAlt = 0;
					}
					
					/* Get total 'base' amount we have. */
					int total = totalBase + (totalAlt*ee.basePerAlt);
					if (total < amount) {
						/* This should never happen since it was checked before this function in hasEnoughToConsume() */
						CivLog.warning("Couldn't find a total big enough with EE!");
						continue;
					}
					
					/* Get difference between the total base we have vs the required amount. */
					total -= amount;
					
					/* Get the number of 'alt' and 'base' types we can fit into our new total/leftover. */
					int leftOverAlt = total / ee.basePerAlt;
					int leftOverBase = total % ee.basePerAlt;
					
					int totalAltConsumed = totalAlt - leftOverAlt;
					int totalBaseConsumed = totalBase - leftOverBase;
										
					try {
						source.removeItem(ee.altType, totalAltConsumed);
					} catch (CivException e) {
						e.printStackTrace();
					}
					if (totalBaseConsumed > 0) {
						try {
							source.removeItem(ee.baseType, totalBaseConsumed);
						} catch (CivException e) {
							e.printStackTrace();
						}
					} else {
						if (totalBaseConsumed != 0) {
							/* If the total amount consumed is negative, add it to the inventory. */
							source.addItem(ItemManager.createItemStack(ee.baseType, (-1*totalBaseConsumed)));
						}
					}
				}
			} else {
				/* We had enough of our base item, consume it. */
				try {
					source.removeItem(typeID, getConsumedAmount(amount));
				} catch (CivException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	public Result processConsumption() {
		
		Integer currentCountMax = levelCounts.get(this.level);
		if (currentCountMax == null) {
			CivLog.error("Couldn't get current level count, level was:"+this.level);
			lastResult = Result.UNKNOWN;
			return lastResult;
		}
		
		if (hasEnoughToConsume()) {
			consumeFromInventory();
			
			if ((this.count+1) >= currentCountMax) {
				// Level up?
				Integer nextCountMax = levelCounts.get(this.level+1);
				if (nextCountMax == null) {
					lastResult = Result.MAXED;
					return lastResult;
				}
				
				this.count = 0;
				if (hasEnoughToConsume()) {
					// we have what we need for the next level, process it as a levelup.
					this.level++;
					lastResult = Result.LEVELUP;
					return lastResult;			
				} else {
					// we don't have enough for the next level, process as a MAXED.
					this.count = currentCountMax;
					lastResult = Result.MAXED;
					return lastResult;
				}
			} else {
				// Grow
				this.count++;
				lastResult = Result.GROW;
				return lastResult;
			}
		} else {
			
			if ((this.count-1) < 0) {
				// Level Down
				Integer lastCountMax = levelCounts.get(this.level-1);
				if (lastCountMax == null) {
					lastResult = Result.STAGNATE;
					return lastResult;
				}
				
				this.count = lastCountMax;
				this.level--;
				lastResult = Result.LEVELDOWN;
				return lastResult;
				
			} else {
				// Starve
				this.count--;
				lastResult = Result.STARVE;
				return lastResult;
			}
		}
		
	}

	
	public String getCountString() {
		String out = "("+this.count+"/";
		Integer currentCountMax = levelCounts.get(this.level);
		if (currentCountMax != null) {
			out += currentCountMax+")";
		} else {
			out += "?)";
		}
		
		return out;
	}
	
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public double getConsumeRate() {
		return consumeRate;
	}

	public void setConsumeRate(double consumeRate) {
		this.consumeRate = consumeRate;
	}

	public Result getLastResult() {
		return this.lastResult;
	}

	public void clearEquivExchanges() {
		this.exchanges.clear();
	}

}
