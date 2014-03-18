package com.avrgaming.civcraft.components;

import java.util.HashSet;

import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;

public class AttributeTradeGood extends AttributeBase {
	
	HashSet<String> goods = new HashSet<String>();
	double value;
	String attribute;
	
	@Override
	public void createComponent(Buildable buildable, boolean async) {
		super.createComponent(buildable, async);
		
		String[] good_ids = this.getString("goods").split(",");
		for (String id : good_ids) {
			goods.add(id.toLowerCase().trim());
		}
		
		attribute = this.getString("attribute");
		value = this.getDouble("value");
	}
	
	
	@Override
	public double getGenerated() {
		if (!this.getBuildable().isActive()) {
			return 0.0;
		}
		
		Town town = this.getBuildable().getTown();
		double generated = 0.0;
		
		for (BonusGoodie goodie :town.getBonusGoodies()) {
			if (goods.contains(goodie.getConfigTradeGood().id)) {
				generated += value;
			}
		}
		
		
		return generated;
	}

}
