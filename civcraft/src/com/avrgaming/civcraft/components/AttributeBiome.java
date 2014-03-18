package com.avrgaming.civcraft.components;

import java.util.HashSet;

import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.structure.Buildable;

public class AttributeBiome extends AttributeBiomeBase {
	/*
	 * Increases the attribute output for _every_ culture biome that exists in the biome list.
	 * 
	 * This one does not inherit from AttributeBase because this attribute is accumulated on the culture biomes.
	 * This can be easily changed if we want the extra to come from "structures"
	 * 
	 */
	private HashSet<String> biomeList = new HashSet<String>();
	private String attribute;
	private double value;
	
	public AttributeBiome() {
		super();
	}
	
	public double getGenerated(CultureChunk cc) {
		if (!this.getBuildable().isActive()) {
			return 0;
		}
		
		if (!biomeList.contains(cc.getBiome().name().toUpperCase())) {
			return 0;
		}
		
		return value;
	}
	
	@Override
	public void createComponent(Buildable buildable, boolean async) {
		super.createComponent(buildable, async);
		
		String[] biomes = this.getString("biomes").split(",");
		for (String biome : biomes) {
			biomeList.add(biome.trim().toUpperCase());
		}
		
		attribute = this.getString("attribute");
		value = this.getDouble("value");
	}


	@Override
	public String getAttribute() {
		return attribute;
	}


	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}
	
}
