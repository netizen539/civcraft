package com.avrgaming.civcraft.components;

import java.util.HashSet;

import com.avrgaming.civcraft.structure.Buildable;

public class ActivateOnBiome extends Component {
	
	public enum EffectType {
		ALL,
		THIS
	}
	
	private HashSet<String> biomeList = new HashSet<String>();
	private String attribute;
	private double value;
	private EffectType effectType; 
	
	@Override
	public void createComponent(Buildable buildable, boolean async) {
		super.createComponent(buildable, async);
		
		String[] biomes = this.getString("biomes").split(",");
		for (String biome : biomes) {
			biomeList.add(biome.trim().toUpperCase());
		}
		
		setAttribute(this.getString("attribute"));
		setValue(this.getDouble("value"));
		setEffectType(EffectType.valueOf(this.getString("effect").toUpperCase()));
	}

	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public double getValue() {
		return value;
	}

	public void setValue(double value) {
		this.value = value;
	}

	public EffectType getEffectType() {
		return effectType;
	}

	public void setEffectType(EffectType effectType) {
		this.effectType = effectType;
	}

	public boolean isValidBiome(String biomeName) {
		return this.biomeList.contains(biomeName);
	}
}
