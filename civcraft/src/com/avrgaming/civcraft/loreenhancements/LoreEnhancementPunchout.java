package com.avrgaming.civcraft.loreenhancements;

import gpl.AttributeUtil;

import java.util.Random;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.object.BuildableDamageBlock;
import com.avrgaming.civcraft.util.CivColor;

public class LoreEnhancementPunchout extends LoreEnhancement {
	
	public String getDisplayName() {
		return "Punchout";
	}
	
	public AttributeUtil add(AttributeUtil attrs) {
		attrs.addEnhancement("LoreEnhancementPunchout", null, null);
		attrs.addLore(CivColor.Gold+getDisplayName());
		return attrs;
	}
	
	@Override
	public int onStructureBlockBreak(BuildableDamageBlock sb, int damage) {
		Random rand = new Random();
		
		if (damage <= 1) {
			if (rand.nextInt(100) <= 50) {
				damage += rand.nextInt(5)+1;
			}		
		}
		
		return damage; 
	}
	
	@Override
	public String serialize(ItemStack stack) {
		return "";
	}

	@Override
	public ItemStack deserialize(ItemStack stack, String data) {
		return stack;
	}

}