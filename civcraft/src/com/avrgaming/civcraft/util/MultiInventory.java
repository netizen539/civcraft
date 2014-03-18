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
package com.avrgaming.civcraft.util;

import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.inventory.DoubleChestInventory;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;

public class MultiInventory {
	
	private ArrayList<Inventory> invs = new ArrayList<Inventory>();
	
	public MultiInventory() {
	}

	private boolean isCorrectItemStack(ItemStack stack, String mid, int type, short data) {
		if (stack == null) {
			return false;
		}

		if (mid != null) {
			/* Looking for a custom id. */
			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
			if (craftMat == null) {
				return false;
			}
		
			if (!craftMat.getConfigId().equals(mid)) {
				return false;
			}
		} else {
			if (ItemManager.getId(stack) != type) {
				return false;
			}
			
			/* Only check item data when max dura == 0. Otherwise item doesnt use data and it's the durability. */
			if (ItemManager.getMaterial(type).getMaxDurability() == 0) {
				if (ItemManager.getData(stack) != data) {
					/* data didn't match, wrong item. */
					return false;
				}
			}
		}	
		
		return true;
	}
	
	
	/* Returns number of items removed. */
	private int removeItemFromInventory(Inventory inv, String mid, int type, short data, int amount) {
		int removed = 0;
		int notRemoved = amount;
		
		ItemStack[] contents = inv.getContents();
		for (int i = 0; i < contents.length; i++) {
			ItemStack stack = contents[i];
			if(!isCorrectItemStack(stack, mid, type, data)) {
				continue;
			}
			
			/* We've got the item we're looking for, lets remove as many as we can. */
			int stackAmount = stack.getAmount();
			
			if (stackAmount > notRemoved) {
				/* We've got more than enough in this stack. Reduce it's size. */
				stack.setAmount(stackAmount - notRemoved);
				removed = notRemoved;
				notRemoved = 0; /* We've got it all. */
			} else if (stackAmount == notRemoved) {
				/* Got the entire stack, null it out. */
				contents[i] = null;
				removed = notRemoved;
				notRemoved = 0;
			} else {
				/* There was some in this stack, but not enough for all we were looking for. */
				removed += stackAmount;
				notRemoved -= stackAmount;
				contents[i] = null;
			}
		
			if (notRemoved == 0) {
				/* We've removed everything. No need to continue. */
				break;
			}
		}
		
		inv.setContents(contents);
		return removed;
	}
	
	
	public void addInventory (Inventory inv) {
		invs.add(inv);
	}
	
	public void addInventory (DoubleChestInventory inv) {
		invs.add(inv.getLeftSide());
		invs.add(inv.getRightSide());
	}
		
	public int addItem(ItemStack items) {
		// Try to find an open spot in any of the inventories.
		HashMap<Integer, ItemStack> leftovers;
		int leftoverAmount = 0;
		for (Inventory inv : invs) {
			
			// 1) Leftovers is the same size as items. This inv was full, move on to next.
			// 2) Leftovers is none or zero, we are done return
			// 3) keep trying to place the leftovers in another chest.
			
			leftoverAmount = 0;
			leftovers = inv.addItem(items);
			
			for (ItemStack i : leftovers.values()) {
				leftoverAmount += i.getAmount();
			}
			
			if (leftoverAmount == 0)
				return 0;
			
			if (leftoverAmount == items.getAmount())
				continue;
			
			// Some items were deposited, update the items count and try again.
			items.setAmount(leftoverAmount);
		
		}
		
		return leftoverAmount;
	}
	
	/* 
	 * Validates that we have the right amount of this item
	 * before it takes it away.
	 */
	public boolean removeItem(String mid, int type, short data, int amount) throws CivException {
		ArrayList<ItemInvPair> toBeRemoved = new ArrayList<ItemInvPair>();
		
		int count = amount;
		for (Inventory inv : invs) {
			for (ItemStack item : inv.getContents()) {
				if(!isCorrectItemStack(item, mid, type, data)) {
					continue;
				}
				
				//Three possibilities, 
				//     1) This item stack has more than we are looking for. So we add a new item stack to the hashmap, with the size
				//        for the amount we want to remove. Break, and it will then be removed.
				//     2) This item stack is exactly equal to the amount we want removed. Add it to the hashmap, and break.
				//     3) This item is NOT large enough for what we want, update the count, add it to the hashmap and keep looking.
				
				if (item.getAmount() > count) {
					toBeRemoved.add(new ItemInvPair(inv, mid, type, data, count));
					count = 0;
					break;
				} else if (item.getAmount() == count) {
					toBeRemoved.add(new ItemInvPair(inv, mid, type, data, count));
					count = 0;
					break;
				} else {
					toBeRemoved.add(new ItemInvPair(inv, mid, type, data, item.getAmount()));
					count -= item.getAmount();
				}	
			}
		}
		
		// If count is not zero, we failed to find what we needed.
		if (count != 0)
			return false;
		
		// We now have a hashmap full of items to remove.
		//<Integer, ItemStack> leftovers = new HashMap<Integer, ItemStack>();
		int totalActuallyRemoved = 0;
		for (ItemInvPair invPair : toBeRemoved) {
			Inventory inv = invPair.inv;
			totalActuallyRemoved += removeItemFromInventory(inv, invPair.mid, invPair.type, invPair.data, invPair.amount);
		}
		
		if (totalActuallyRemoved != amount) {
			throw new CivException("Inventory Error! We tried to remove "+amount+" items but could only remove "+totalActuallyRemoved);
		}
		
		return true;
		
	}
	
	public boolean removeItem(ItemStack item) throws CivException {
		LoreMaterial loreMat = LoreMaterial.getMaterial(item);
		if (loreMat != null) {
			return removeItem(loreMat.getId(), 0, (short)0, item.getAmount());
		} else {
			/* Vanilla item. no custom id. */
			return removeItem(null, ItemManager.getId(item), ItemManager.getData(item), item.getAmount());
		}
	}
	
	public boolean removeItem(int typeid, int amount) throws CivException {
		return removeItem(null, typeid, (short)0, amount);
	}
	
	public boolean removeItem(int typeid, short data, int amount) throws CivException {
		return removeItem(null, typeid, data, amount);
	}
	
//	public boolean removeItem(int typeid, int data, int amount) {
//		
//		//HashMap<ItemStack, Inventory> toBeRemoved = new HashMap<ItemStack, Inventory>();
//		ArrayList<ItemInvPair> toBeRemoved = new ArrayList<ItemInvPair>();
//		
//		int count = amount;
//		
//		for (Inventory inv : invs) {
//			for (ItemStack item : inv.getContents()) {
//				if (item == null)
//					continue;
//				if (ItemManager.getId(item) != typeid)
//					continue;
//				if (item.getDurability() != data) {
//					continue;
//				}
//				
//				//Three possibilities, 
//				//     1) This item stack has more than we are looking for. So we add a new item stack to the hashmap, with the size
//				//        for the amount we want to remove. Break, and it will then be removed.
//				//     2) This item stack is exactly equal to the amount we want removed. Add it to the hashmap, and break.
//				//     3) This item is NOT large enough for what we want, update the count, add it to the hashmap and keep looking.
//				
//				if (item.getAmount() > count) {
//					ItemStack subStack = ItemManager.createItemStack(typeid, count, (short)data);
//					toBeRemoved.add(new ItemInvPair(inv, subStack));
//					count = 0;
//					break;
//				} else if (item.getAmount() == count) {
//					toBeRemoved.add(new ItemInvPair(inv, item));
//					count = 0;
//					break;
//				} else {
//					toBeRemoved.add(new ItemInvPair(inv, item));
//					count -= item.getAmount();
//				}	
//			}
//		}
//		
//		// If count is not zero, we failed to find what we needed.
//		if (count != 0)
//			return false;
//		
//		// We now have a hashmap full of items to remove.
//		for (ItemInvPair invPair : toBeRemoved) {
//			Inventory inv = invPair.inv;
//			ItemStack item = invPair.stack;
//			inv.removeItem(item);
//		}
//		
//		return true;
//	}
	

	public boolean contains(String mid, int type, short data, int amount) {
		
		int count = 0;
		for (Inventory inv : invs) {
			for (ItemStack item : inv.getContents()) {		
				if (item == null) {
					continue;
				}
				
				if (mid != null) {
					LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(item);
					if (craftMat == null) {
						continue;
					}
					
					if (!craftMat.getConfigId().equals(mid)) {
						continue;
					}
				} else {
					/* Vanilla item. */
					if (ItemManager.getId(item) != type) {
						continue;
					}
					
					/* Only check the data if this item doesnt use durability. */
					if (ItemManager.getMaterial(type).getMaxDurability() == 0) {
						if (ItemManager.getData(item) != data) {
							continue;
						}
					}
				}
				
				count += item.getAmount();
				if (count >= amount) {
					break;
				}
			}
		}
			
		if (count >= amount) 
			return true;
		return false;
	}
	
	public ItemStack[] getContents() {
		
		int size = 0;
		for (Inventory inv : invs) {
			size += inv.getContents().length;
		}
		
		ItemStack[] array = new ItemStack[size];
		
		int i = 0;
		for (Inventory inv : invs) {
			for (int j = 0; j < inv.getContents().length; j++) {
				array[i] = inv.getContents()[j];			
				i++;
			}
		}
		return array;
	}


	public int getInventoryCount() {
		return this.invs.size();
	}

//	public boolean contains(LoreMaterial loreMaterial) {
//		
//		boolean found = false;
//		for (Inventory inv : this.invs) {
//			for (ItemStack stack : inv.getContents()) {
//				if (stack == null) {
//					continue;
//				}
//				
//				LoreMaterial loreMat = LoreMaterial.getMaterial(stack);
//				if (loreMat == loreMaterial) {
//					found = true;
//					break;
//				}
//			}
//		}
//		
//		return found;
//	}

}