package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

public class LeatherColor extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
		attrs.setColor(Long.decode("0x"+this.getString("color")));		
	}

}
