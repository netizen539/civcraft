package com.avrgaming.civcraft.components;

import java.util.HashMap;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.util.ChunkCoord;

public class AttributeBiomeRadiusPerLevel extends AttributeBiomeBase {
	
	private HashMap<String, Double> biomeInfo = new HashMap<String, Double>();

	private String attribute;
	private double baseValue;
	private ChunkCoord centerCoord;
	
	public AttributeBiomeRadiusPerLevel() {
		super();
	}
	
	@Override
	public void createComponent(Buildable buildable, boolean async) {
		super.createComponent(buildable, async);
		
		String[] biomes = this.getString("biomes").split(",");
		for (String biomeInfoStr : biomes) {
			String[] split = biomeInfoStr.split(":");
			String biome = split[0];
			Double val = Double.valueOf(split[1]);
			
			biomeInfo.put(biome.trim().toUpperCase(), val);
		}
		
		setAttribute(this.getString("attribute"));
		setBaseValue(this.getDouble("base_value"));
		
		centerCoord = new ChunkCoord(buildable.getCorner());
	}

	@Override
	public String getAttribute() {
		return attribute;
	}

	public void setAttribute(String attribute) {
		this.attribute = attribute;
	}

	public double getBaseValue() {
		return baseValue;
	}

	public void setBaseValue(double baseValue) {
		this.baseValue = baseValue;
	}
	
	public boolean isInRange(ChunkCoord coord) {
		int diffX = coord.getX() - centerCoord.getX();
		int diffZ = coord.getZ() - centerCoord.getZ();
		
		if (diffX > 1 || diffX < -1) {
			return false;
		}
		
		if (diffZ > 1 || diffZ < -1) {
			return false;
		}
		
		return true;
	}
	
	public double getGenerated(CultureChunk cc) {		
		if (!this.getBuildable().isActive()) {
			return 0.0;
		}
		
		if (!this.isInRange(cc.getChunkCoord())) {
			return 0.0;
		}
		
		int mineLevel = -1;
		for (Component comp : this.getBuildable().attachedComponents) {
			if (comp instanceof ConsumeLevelComponent) {
				ConsumeLevelComponent consumeComp = (ConsumeLevelComponent)comp;
				mineLevel = consumeComp.getLevel();
			}
		}
		
		if (mineLevel == -1) {
			CivLog.warning("Couldn't find consume component for buildable "+this.getBuildable().getDisplayName()+
					" but it has an AttributeBiomeRadiusPerLevel component attached.");
			return 0.0;
		}
		
		double generated = this.getBaseValue()*mineLevel;
		Double extra = this.biomeInfo.get(cc.getBiome().name());
		
		if (extra != null) {
			generated += extra*mineLevel;
		}
		
		return generated;
	}
	
}
