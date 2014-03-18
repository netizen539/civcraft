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
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.block.Block;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;

public class Farm extends Structure {
	
	public static final long GROW_RATE = (int)CivSettings.getIntegerStructure("farm.grow_tick_rate");
	public static final int CROP_GROW_LIGHT_LEVEL = 9;
	public static final int MUSHROOM_GROW_LIGHT_LEVEL = 12;
	public static final int MAX_SUGARCANE_HEIGHT = 3;
	
	private FarmChunk fc = null;
	private double lastEffectiveGrowthRate = 0;
	
	protected Farm(Location center, String id, Town town) throws CivException {
		super(center, id, town);
	}

	public Farm(ResultSet rs) throws SQLException, CivException {
		super(rs);
		build_farm(this.getCorner().getLocation());
	}
	
	@Override
	public void delete() throws SQLException {
		if (this.getCorner() != null) {
			ChunkCoord coord = new ChunkCoord(this.getCorner().getLocation());
			CivGlobal.removeFarmChunk(coord);
			CivGlobal.getSessionDB().delete_all(getSessionKey());
		}
		super.delete();
	}
	
	@Override
	public String getDynmapDescription() {
		return null;
	}
	
	@Override
	public boolean canRestoreFromTemplate() {
		return false;
	}
	
	@Override
	public String getMarkerIconName() {
		return "basket";
	}

	public void build_farm(Location centerLoc) {
		// A new farm, add it to the farm chunk table ...
		Chunk chunk = centerLoc.getChunk();
		FarmChunk fc = new FarmChunk(chunk, this.getTown(), this);
		CivGlobal.addFarmChunk(fc.getCoord(), fc);
		this.fc = fc;
	}

	public static boolean isBlockControlled(Block b) {
		
		switch (ItemManager.getId(b)) {
		//case CivData.BROWNMUSHROOM:
		//case CivData.REDMUSHROOM:
		case CivData.COCOAPOD:
		case CivData.MELON:
		case CivData.MELON_STEM:
		case CivData.PUMPKIN:
		case CivData.PUMPKIN_STEM:
		case CivData.WHEAT:
		case CivData.CARROTS:
		case CivData.POTATOES:
		case CivData.NETHERWART:
	//	case CivData.SUGARCANE:
			return true;
		}
		
		return false;
	}

	public void saveMissedGrowths() {
		
		class AsyncSave implements Runnable {

			Farm farm;
			int missedTicks;
			
			public AsyncSave(Farm farm, int missedTicks) {
				this.farm = farm;
				this.missedTicks = missedTicks;
			}
			
			@Override
			public void run() {
				ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getSessionKey());
				
				if (entries == null || entries.size() == 0) {
					if (missedTicks > 0) {
						farm.sessionAdd(getSessionKey(), ""+missedTicks);
						return;
					}
				}
				
				if (missedTicks == 0) {
					CivGlobal.getSessionDB().delete_all(getSessionKey());
				} else {
					CivGlobal.getSessionDB().update(entries.get(0).request_id, getSessionKey(), ""+missedTicks);
				}
			}
			
		}
	
		TaskMaster.asyncTask(new AsyncSave(this, this.fc.getMissedGrowthTicks()), 0);
	}
	
	public String getSessionKey() {
		return "FarmMissedGrowth"+":"+this.getCorner().toString();
	}
	
	@Override
	public void onLoad() {
		ArrayList<SessionEntry> entries = new ArrayList<SessionEntry>();
		entries = CivGlobal.getSessionDB().lookup(getSessionKey());
		int missedGrowths = 0;
		
		if (entries.size() > 0) {
			missedGrowths = Integer.valueOf(entries.get(0).value);
		} 
		
		class AsyncTask extends CivAsyncTask {
			int missedGrowths;
			
			public AsyncTask(int missedGrowths) {
				this.missedGrowths = missedGrowths;
			}
			
			
			@Override
			public void run() {
				fc.setMissedGrowthTicks(missedGrowths);
				fc.processMissedGrowths(true, this);
				saveMissedGrowths();
			}
		}

		TaskMaster.asyncTask(new AsyncTask(missedGrowths), 0);
	}

	public void setLastEffectiveGrowth(double effectiveGrowthRate) {
		this.lastEffectiveGrowthRate = effectiveGrowthRate;
	}
	
	public double getLastEffectiveGrowthRate() {
		return this.lastEffectiveGrowthRate;
	}
}
