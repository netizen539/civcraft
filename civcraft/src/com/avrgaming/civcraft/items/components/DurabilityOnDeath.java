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
package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.lorestorage.ItemChangeResult;

public class DurabilityOnDeath extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
//		attrs.addLore(CivColor.Blue+""+this.getDouble("value")+" Durability");
	}

	@Override
	public ItemChangeResult onDurabilityDeath(PlayerDeathEvent event, ItemChangeResult result, ItemStack sourceStack) {
		if (result == null) {
			result = new ItemChangeResult();
			result.stack = sourceStack;
			result.destroyItem = false;
		}
		
		if (result.destroyItem) {
			return result;
		}
		
		double percent = this.getDouble("value");
		
		int reduction = (int)(result.stack.getType().getMaxDurability()*percent);
		int durabilityLeft = result.stack.getType().getMaxDurability() - result.stack.getDurability();
		
		if (durabilityLeft > reduction) {
			result.stack.setDurability((short)(result.stack.getDurability() + reduction));
		} else {
			result.destroyItem = true;
		}		
		
		return result;
	}

}
