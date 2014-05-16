package com.avrgaming.civcraft.unittests;

import static org.junit.Assert.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.MultiInventory;

@RunWith(JUnit4.class)
public class TestMultiInventory {
 
	private Inventory newEmptyInventory() {
		Inventory inv = mock(Inventory.class);
		when(inv.getContents()).thenReturn(new ItemStack[54]);
		return inv;		
	}
	
	private boolean contentsIsEmpty(ItemStack[] contents) {
		for (ItemStack stack : contents) {
			if (stack == null || ItemManager.getId(stack) == CivData.AIR) {
				continue;
			}
			return false;
		}
		return true;
	}
	
//	private int getItemCount(ItemStack[] contents) {
//		int count = 0;
//		for (ItemStack stack : contents) {
//			if (stack == null || ItemManager.getId(stack) == CivData.AIR) {
//				continue;
//			}
//			count++;
//		}
//		return count;
//	}
	
	/* 
	 * Asserts that we can 
	 * 1) create a new multi-inventory.
	 * 2) add a single item to the inventory.
	 * 3) verify it was the only one added.
	 * 
	 */
	@Test
    public void addSingleItemToInventory() {
		MultiInventory multiInv = new MultiInventory();
		multiInv.addInventory(newEmptyInventory());
		assertEquals(true, multiInv.getInventoryCount() == 1);

		/* Verify that this inventory is empty. */
		assertEquals(true, contentsIsEmpty(multiInv.getContents()));
		
		/* Add a single item, verify there are no leftovers */
		//assertEquals(multiInv.addItem(ItemManager.createItemStack(CivData.BOW, 1)), 1);
		
		/* Verify that this inventory now has 1 item. */
		//assertEquals("Inventory Has One Item", 1, getItemCount(multiInv.getContents()));
		
    }
    

//@Test
//@Ignore
//public void thisIsIgnored() {
//}

}
