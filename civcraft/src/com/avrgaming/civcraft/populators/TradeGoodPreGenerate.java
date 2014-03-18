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
package com.avrgaming.civcraft.populators;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.TreeSet;

import org.bukkit.Bukkit;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigHemisphere;
import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.ChunkCoord;

public class TradeGoodPreGenerate {

	private int chunks_min;
	private int chunks_max;
	private int chunks_x;
	private int chunks_z;
	private int seed;
	private String worldName;
	
	// Maybe all we need is a hashset?
//	public Map<ChunkCoord, String> goodPlacements = new HashMap<ChunkCoord, String>();
	public Map<ChunkCoord, TradeGoodPick> goodPicks = new HashMap<ChunkCoord, TradeGoodPick>();
	
	public TradeGoodPreGenerate() {
		
	}
	
	
    private boolean validHemisphere(ConfigHemisphere hemi, int x, int z) {
    	if (hemi.x_max != 0 && x > hemi.x_max) {
			return false;
		}
		if (hemi.x_min != 0 && x < hemi.x_min) {
			return false;
		}
		if (hemi.z_max != 0 && z > hemi.z_max) {
			return false;
		}
		if (hemi.z_min != 0 && z < hemi.z_min) {
			return false;
		}
		return true;
    }

    
    private TreeSet<ConfigTradeGood> getValidTradeGoods(int x, int z, Map<String, ConfigTradeGood> goods) {
    	
    	//ArrayList<ConfigTradeGood> validGoods = new ArrayList<ConfigTradeGood>();
    	TreeSet<ConfigTradeGood> validGoods = new TreeSet<ConfigTradeGood>();
    	for (ConfigTradeGood good : goods.values()) {
    		String hemiString = good.hemiString;
    		if (hemiString == null) {
    			//No hemis selected means valid everywhere, add it.
    			validGoods.add(good);
    			continue;
    		}
    		
    		String[] hemiStrs = hemiString.split(",");
    		for (String str : hemiStrs) {
    			ConfigHemisphere hemi = CivSettings.hemispheres.get(str);
    			if (hemi == null) {
    				CivLog.warning("Invalid hemisphere:"+str+" detected for trade good generation.");
    				continue; //ignore invalid hemisphere
    			}
    			
    			if (validHemisphere(hemi, x, z)) {
    				validGoods.add(good);
    			}
    		}
    	}
    	
    	return validGoods;
    }
	
	/*
	 * Pre-generate the locations of the trade goods so that we can
	 * validate their positions relative to each other. Once generated
	 * save results to a file, and load if that file exists.
	 */
	public void preGenerate() {
		try {
			chunks_min = CivSettings.getInteger(CivSettings.goodsConfig, "generation.chunks_min");
			chunks_max = CivSettings.getInteger(CivSettings.goodsConfig, "generation.chunks_max");
			chunks_x = CivSettings.getInteger(CivSettings.goodsConfig, "generation.chunks_x");
			chunks_z = CivSettings.getInteger(CivSettings.goodsConfig, "generation.chunks_z");
			seed = CivSettings.getInteger(CivSettings.goodsConfig, "generation.seed");
			this.worldName = Bukkit.getWorlds().get(0).getName();	
			
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		
		Random rand = new Random();
		rand.setSeed(seed);
		CivLog.info("Generating Trade Goodie Locations.");
		for (int x = -chunks_x; x < chunks_x; x += chunks_min ) {
			for (int z = -chunks_z; z < chunks_z; z += chunks_min) {
				int diff = chunks_max - chunks_min;
				int randX = x;
				int randZ = z;
				
				if (diff > 0) {
					if (rand.nextBoolean()) {
						randX += rand.nextInt(diff);
					} else {
						randX -= rand.nextInt(diff);
					}
					
					if (rand.nextBoolean()) {
						randZ += rand.nextInt(diff);
					} else {
						randZ -= rand.nextInt(diff);
					}
				}
				
				
				ChunkCoord cCoord = new ChunkCoord(worldName, randX, randZ);
				pickFromCoord(cCoord);
				//goodPlacements.put(cCoord, "goodie");
			}
		}
		
		CivLog.info("Done.");
		
		
	}
	private ConfigTradeGood pickFromSet(TreeSet<ConfigTradeGood> set, int rand) {
		
		//Find the lowest rarity that qualifies in our list.
		double lowest_rarity = Double.MAX_VALUE;
		for (ConfigTradeGood good : set) {
			if (rand < (good.rarity*100)) {
				if (good.rarity < lowest_rarity) {
					lowest_rarity = good.rarity;
				}
			}
		}
		
		// Filter out all but the lowest rarity that qualifies
		ArrayList<ConfigTradeGood> pickList = new ArrayList<ConfigTradeGood>();
		for (ConfigTradeGood good : set) {
			if (good.rarity == lowest_rarity) {
				pickList.add(good);
			}
		}
		
		// Pick a random good from this list.
		Random random = new Random();
		
		return pickList.get(random.nextInt(pickList.size()));
		
	}
	
	private void pickFromCoord(ChunkCoord cCoord) {
		TreeSet<ConfigTradeGood> validLandGoods;
		TreeSet<ConfigTradeGood> validWaterGoods;
		TradeGoodPick pick = new TradeGoodPick();

		validLandGoods = this.getValidTradeGoods(cCoord.getX(), cCoord.getZ(), CivSettings.landGoods);
		validWaterGoods =  this.getValidTradeGoods(cCoord.getX(), cCoord.getZ(), CivSettings.waterGoods);
	
		pick.chunkCoord = cCoord;
		
		Random random = new Random();
		int rand = random.nextInt(100);

		pick.landPick = pickFromSet(validLandGoods, rand);
		pick.waterPick = pickFromSet(validWaterGoods, rand);
		
		/*
		 * Do not allow two of the same goodie within
		 * 4 chunks of each other.
		 */
		for (int x = -4; x < 4; x++) {
			for (int z = -4; z < 4; z++) {
				ChunkCoord n = new ChunkCoord(cCoord.getWorldname(), cCoord.getX(), cCoord.getZ());
				n.setX(n.getX()+x);
				n.setZ(n.getZ()+z);
				
				TradeGoodPick nearby = goodPicks.get(n);
				if (nearby == null) {
					continue;
				}
				
				if (nearby.landPick == pick.landPick) {
					if (validLandGoods.size() <= 1) {
						/* Dont generate anything here. */
						return;
					} else {
						while (nearby.landPick == pick.landPick) {
							rand = random.nextInt(100);
							pick.landPick = pickFromSet(validLandGoods, rand);
						}
					}
				}
				
				if (nearby.waterPick == pick.waterPick) {
					if (validLandGoods.size() <= 1) {
						/* Dont generate anything here. */
						return;
					} else {
						while (nearby.landPick == pick.landPick) {
							rand = random.nextInt(100);
							pick.landPick = pickFromSet(validLandGoods, rand);
						}
					}
				}
			}
		}
		
		
		this.goodPicks.put(cCoord, pick);
	}
	
	
}
