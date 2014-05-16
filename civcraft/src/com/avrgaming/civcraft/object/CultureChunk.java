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
package com.avrgaming.civcraft.object;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.components.Attribute;
import com.avrgaming.civcraft.components.AttributeBiomeBase;
import com.avrgaming.civcraft.components.AttributeBiomeRadiusPerLevel;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCultureBiomeInfo;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.BiomeCache;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class CultureChunk {

	private Town town;
	private ChunkCoord chunkCoord;
	private int distance = 0;
	private Biome biome = null;
	
	public CultureChunk(Town town, ChunkCoord coord) {
		this.town = town;
		this.chunkCoord = coord;
		biome = BiomeCache.getBiome(this);
	}
	public Civilization getCiv() {
		return town.getCiv();
	}

	public Town getTown() {
		return town;
	}
	public void setTown(Town town) {
		this.town = town;
	}
	
	public ChunkCoord getChunkCoord() {
		return chunkCoord;
	}
	public void setChunkCoord(ChunkCoord chunkCoord) {
		this.chunkCoord = chunkCoord;
	}
	public int getDistanceToNearestEdge(ArrayList<TownChunk> edges) {
		int distance = Integer.MAX_VALUE;
		
		for (TownChunk tc : edges) {
			int tmp = tc.getChunkCoord().manhattanDistance(this.chunkCoord);
			if (tmp < distance) {
				distance = tmp;
			}
		}
		
		return distance;
	}
	public void setDistance(int distance) {
		this.distance = distance;
	}
	
	public String getOnLeaveString() {
		return CivColor.LightPurple+"Leaving "+town.getCiv().getName()+" Borders";
	}
	
	public String getOnEnterString() {
		return CivColor.LightPurple+"Entering "+town.getCiv().getName()+" Borders";
	}
	public double getPower() {
		// power = max/(distance^2).
		// if distance == 0, power = DOUBLEMAX;
		
		if (this.distance == 0) {
			return Double.MAX_VALUE;
		}
		
		ConfigCultureLevel clc = CivSettings.cultureLevels.get(getTown().getCultureLevel());
		double power = clc.amount / (Math.pow(distance, 2));
		
		return power;
	}
	
	public Biome getBiome() {
		return biome;
	}
	public void setBiome(Biome biome) {
		this.biome = biome;
	}
	
	@Override
	public String toString() {
		return this.chunkCoord.toString();
	}
	
	public ConfigCultureBiomeInfo getCultureBiomeInfo() {
		if (this.biome != null) {
			ConfigCultureBiomeInfo info = CivSettings.getCultureBiome(this.biome.name());
			return info;
		} else {
			// This can happen within 1 tick of the chunk being created, that's OK. 
			return CivSettings.getCultureBiome("UNKNOWN");
		}
	}
	
	public double getCoins() {
		return getCultureBiomeInfo().coins+getAdditionalAttributes(Attribute.TypeKeys.COINS.name());
	}
	
	public double getHappiness() {
		return getCultureBiomeInfo().happiness+getAdditionalAttributes(Attribute.TypeKeys.HAPPINESS.name());
	}
	
	public double getHammers() {
		//CivLog.debug("getting hammers...");
		return getCultureBiomeInfo().hammers+getAdditionalAttributes(Attribute.TypeKeys.HAMMERS.name());
	}
	
	public double getGrowth() {
		return getCultureBiomeInfo().growth+getAdditionalAttributes(Attribute.TypeKeys.GROWTH.name());
	}
	
	public double getBeakers() {		
		return getCultureBiomeInfo().beakers+getAdditionalAttributes(Attribute.TypeKeys.BEAKERS.name());
	}
	
	private double getAdditionalAttributes(String attrType) {
		if (getBiome() == null) {
			return 0.0;
		}
		
		Component.componentsLock.lock();
		try {
			ArrayList<Component> attrs = Component.componentsByType.get("AttributeBiomeBase");
			double total = 0;
			
			if (attrs == null) {
				return total;
			}
	
			for (Component comp : attrs) {
				if (comp instanceof AttributeBiomeRadiusPerLevel) {
				}
				
				if (comp instanceof AttributeBiomeBase) {
					AttributeBiomeBase attrComp = (AttributeBiomeBase)comp;
					if (attrComp.getAttribute().equals(attrType)) {
						total += attrComp.getGenerated(this);
					}
				}
			}
			return total;
		} finally {
			Component.componentsLock.unlock();
		}
	}
	
	public static void showInfo(Player player) {
	//	Biome biome = player.getLocation().getWorld().getBiome(player.getLocation().getBlockX(), player.getLocation().getBlockZ());
		Biome biome = getBiomeFromLocation(player.getLocation());
		
		CultureChunk cc = CivGlobal.getCultureChunk(new ChunkCoord(player.getLocation()));
		ConfigCultureBiomeInfo info = CivSettings.getCultureBiome(biome.name());
	//	CivLog.debug("showing info.");
		
		if (cc == null) {
			CivMessage.send(player, CivColor.LightPurple+biome.name()+
					CivColor.Green+" Coins: "+CivColor.LightGreen+info.coins+
					CivColor.Green+" Happiness:"+CivColor.LightGreen+info.happiness+
					CivColor.Green+" Hammers:"+CivColor.LightGreen+info.hammers+
					CivColor.Green+" Growth:"+CivColor.LightGreen+info.growth+				
					CivColor.Green+" Beakers:"+CivColor.LightGreen+info.beakers);
		} else {
			CivMessage.send(player, CivColor.LightPurple+biome.name()+
					CivColor.Green+" Coins: "+CivColor.LightGreen+cc.getCoins()+
					CivColor.Green+" Happiness:"+CivColor.LightGreen+cc.getHappiness()+
					CivColor.Green+" Hammers:"+CivColor.LightGreen+cc.getHammers()+
					CivColor.Green+" Growth:"+CivColor.LightGreen+cc.getGrowth()+				
					CivColor.Green+" Beakers:"+CivColor.LightGreen+cc.getBeakers());
		}

	}
	
	public static Biome getBiomeFromLocation(Location loc) {
		Block block = loc.getChunk().getBlock(0, 0, 0);
		return block.getBiome();
	}
	
	
}
