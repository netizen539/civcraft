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

import java.util.LinkedList;
import java.util.Random;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.sync.request.UpdateInventoryRequest.Action;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

public class SifterComponent extends Component {

	/* What item it turns into */
	private LinkedList<SifterItem> items = new LinkedList<SifterItem>();
	
	/* Source location of items. */
	private BlockCoord sourceCoord;
	//private MultiInventory source = new MultiInventory();
 
	
	/* Destination location for items. */
	private BlockCoord destCoord;
	//private MultiInventory dest = new MultiInventory();
	
	public void addSiftItem(int source_type, short source_data, double rate, int result_type, short result_data, int amount) {
		SifterItem si = new SifterItem();
		si.source_type = source_type;
		si.source_data = source_data;
		si.rate = rate*1000;
		si.result_type = result_type;
		si.result_data = result_data;
		si.amount = amount;
		
		items.add(si);
	}
	
	public void run(CivAsyncTask task) {
		MultiInventory source = new MultiInventory();
		MultiInventory dest = new MultiInventory();
		
		if (sourceCoord == null || destCoord == null) {
			return;
		}
		
		try {
			Inventory tmp = task.getChestInventory(sourceCoord.getWorldname(), sourceCoord.getX(), sourceCoord.getY(), sourceCoord.getZ(), false);
			if (tmp == null) {
				return;
			}
			source.addInventory(tmp);
		} catch (InterruptedException | CivTaskAbortException e) {
			//e.printStackTrace();
			return;
		}
	
		boolean full = true;
		try {
			Inventory tmp = task.getChestInventory(destCoord.getWorldname(), destCoord.getX(), destCoord.getY(), destCoord.getZ(), false);
			if (tmp == null) {
				return;
			}
			dest.addInventory(tmp);
			
			for (ItemStack stack : tmp.getContents()) {
				if (stack == null) {
					full = false;
					break;
				}
			}
		} catch (InterruptedException | CivTaskAbortException e) {
			//e.printStackTrace();
			return;
		}
		
		if (full) {
			return;
		}
		
		process(source, dest, 1, task);
	}
	
	/*
	 * Run through the items in the contents up to count times and 
	 * convert them based on the rates.
	 */
	private void process(MultiInventory source, MultiInventory dest, int count, CivAsyncTask task) {
		
		Random rand = new Random();
		int i = 0;
		for (ItemStack stack : source.getContents()) {
			if (stack == null || ItemManager.getId(stack) == 0) {
				continue;
			}

			SifterItem lowestChanceItem = null;
			boolean found = false;
			
			for (SifterItem si : items) {
				if (si == null) { 
					continue;
				}
				
				if (si.source_type != ItemManager.getId(stack)) {
					continue;
				}
				
				found = true;			
				int next = rand.nextInt(1000);
				/* 
				 * We need to award the _best_ chance they succeed at so that
				 * all item chances are treated fairly. 
				 */
				if (next < si.rate) {
					if (lowestChanceItem == null || lowestChanceItem.rate < si.rate) {
						lowestChanceItem = si;
					}
					break;
				}
			}
			
			if (!found || lowestChanceItem == null) {
				continue;
			}
			
			/* Item was successfully generated. Add it to output. */
			try {
				task.updateInventory(Action.REMOVE, source, ItemManager.createItemStack(lowestChanceItem.source_type, 1));
			} catch (InterruptedException e) {
				return;
			}
			try {
				task.updateInventory(Action.ADD, dest, ItemManager.createItemStack(lowestChanceItem.result_type, lowestChanceItem.amount, (short)lowestChanceItem.result_data));
			} catch (InterruptedException e) {
				return;
			}
	
			i++;
			if (i >= count) {
				/* Finished, only generate up to 'count' items. */
				break;
			}
			
			
		}
		
		return;
	}
	
	@Override
	public void onLoad() {
		
	}

	@Override
	public void onSave() {
		
	}

	public BlockCoord getSourceCoord() {
		return sourceCoord;
	}

	public void setSourceCoord(BlockCoord sourceCoord) {
		this.sourceCoord = sourceCoord;
	}

	public BlockCoord getDestCoord() {
		return destCoord;
	}

	public void setDestCoord(BlockCoord destCoord) {
		this.destCoord = destCoord;
	}

}
