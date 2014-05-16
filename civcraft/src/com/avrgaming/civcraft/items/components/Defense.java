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

import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementDefense;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class Defense extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
		attrs.addLore(CivColor.Blue+""+this.getDouble("value")+" Defense");
	}
	
	@Override
	public void onHold(PlayerItemHeldEvent event) {	
		
		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (!resident.hasTechForItem(event.getPlayer().getInventory().getItem(event.getNewSlot()))) {		
			CivMessage.send(resident, CivColor.Rose+"Warning - "+CivColor.LightGray+
					"You do not have the required technology to use this item. It's defense output will be reduced in half.");
		}
	}
	
	@Override
	public void onDefense(EntityDamageByEntityEvent event, ItemStack stack) {
		double defValue = this.getDouble("value");
		
		/* Try to get any extra defense enhancements from this item. */
		LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
		if (craftMat == null) {
			return;
		}
				
		double extraDef = 0;
		AttributeUtil attrs = new AttributeUtil(stack);
		
		for (LoreEnhancement enh : attrs.getEnhancements()) {
			if (enh instanceof LoreEnhancementDefense) {
				extraDef +=  ((LoreEnhancementDefense)enh).getExtraDefense(attrs);
			}
		}
		
		defValue += extraDef;		
		double damage = event.getDamage();
		
		if (event.getEntity() instanceof Player) {
			Resident resident = CivGlobal.getResident(((Player)event.getEntity()));
			if (!resident.hasTechForItem(stack)) {
				defValue = defValue / 2;
			}
		}
		
		damage -= defValue;
		if (damage < 0.5) {
			/* Always do at least 0.5 damage. */
			damage = 0.5;
		}
		
		event.setDamage(damage);
	}

}
