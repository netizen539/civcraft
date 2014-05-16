package com.avrgaming.civcraft.loreenhancements;

import gpl.AttributeUtil;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.CivColor;

public class LoreEnhancementAttack extends LoreEnhancement {
	
	public LoreEnhancementAttack() {
		this.variables.put("amount", "1.0");
	}
	
	public String getLoreString(double baseLevel) {
		double m;
		try {
			m = CivSettings.getDouble(CivSettings.civConfig, "global.attack_catalyst_multiplier");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			m = 1;
		}
		return CivColor.Blue+"+"+(baseLevel*m)+" Attack";
	}
	
	@Override
	public AttributeUtil add(AttributeUtil attrs) {		
		/* 
		 * Look for any existing attack enhancements.
		 * If we have one, add to it instead of making a
		 * new one.
		 */
		
		double amount = Double.valueOf(this.variables.get("amount"));
		double baseLevel = amount;
		if (attrs.hasEnhancement("LoreEnhancementAttack")) {
			
			/* Get base Level. */
			baseLevel = Double.valueOf(attrs.getEnhancementData("LoreEnhancementAttack", "level"));

			/* Reset the lore. */
			String[] lore = attrs.getLore();
			for (int i = 0; i < lore.length; i++) {
				if (lore[i].equals(getLoreString(baseLevel))) {
					lore[i] = getLoreString(baseLevel+amount);
				}
			}
			attrs.setLore(lore);
			
			/* Reset the item name. */
			String split[] = attrs.getName().split("\\(");
			attrs.setName(split[0]+"(+"+(baseLevel+amount)+")");
			
			/* Store the data back in the enhancement. */
			attrs.setEnhancementData("LoreEnhancementAttack", "level", ""+(baseLevel+amount));
		} else {
			attrs.addEnhancement("LoreEnhancementAttack", "level", ""+baseLevel);
			attrs.addLore(getLoreString(baseLevel));
			attrs.setName(attrs.getName()+CivColor.LightBlue+"(+"+amount+")");
		}

		LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(attrs.getCivCraftProperty("mid"));
		if (craftMat == null) {
			CivLog.warning("Couldn't find craft mat with MID of:"+attrs.getCivCraftProperty("mid"));
			return attrs;
		}

		return attrs;
	}
	
	@Override
	public double getLevel(AttributeUtil attrs) {	
		if (attrs.hasEnhancement("LoreEnhancementAttack")) {
			/* Get base Level. */
			Double baseLevel = Double.valueOf(attrs.getEnhancementData("LoreEnhancementAttack", "level")); 
			return baseLevel;
		}
		return 0;
	}


	@Override
	public boolean canEnchantItem(ItemStack item) {
		return isWeapon(item);
	}

	public double getExtraAttack(AttributeUtil attrs) {
		double m;
		try {
			m = CivSettings.getDouble(CivSettings.civConfig, "global.attack_catalyst_multiplier");
			return getLevel(attrs)*m;
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		
		return getLevel(attrs);
	}
	
	@Override
	public String serialize(ItemStack stack) {
		AttributeUtil attrs = new AttributeUtil(stack);
		return attrs.getEnhancementData("LoreEnhancementAttack", "level");
	}

	@Override
	public ItemStack deserialize(ItemStack stack, String data) {
		AttributeUtil attrs = new AttributeUtil(stack);
		attrs.setEnhancementData("LoreEnhancementAttack", "level", data);
		attrs.setName(attrs.getName()+CivColor.LightBlue+"(+"+Double.valueOf(data)+")");
		return attrs.getStack();
	}
}
