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
package com.avrgaming.civcraft.threading.timers;

import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.Map.Entry;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.AttrSource;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Cottage;
import com.avrgaming.civcraft.structure.Mine;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class EffectEventTimer extends CivAsyncTask {
	
	//public static Boolean running = false;
	
	public static ReentrantLock runningLock = new ReentrantLock();
	
	public EffectEventTimer() {
	}

	private void processTick() {
		/* Clear the last taxes so they don't accumulate. */
		for (Civilization civ : CivGlobal.getCivs()) {
			civ.lastTaxesPaidMap.clear();
		}
		
		//HashMap<Town, Integer> cultureGenerated = new HashMap<Town, Integer>();
		
		// Loop through each structure, if it has an update function call it in another async process
		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
		
		while(iter.hasNext()) {
			Structure struct = iter.next().getValue();
			TownHall townhall = struct.getTown().getTownHall();

			if (townhall == null) {
				continue;
			}

			if (!struct.isActive())
				continue;

			struct.onEffectEvent();

			if (struct.getEffectEvent() == null || struct.getEffectEvent().equals(""))
				continue;
			
			String[] split = struct.getEffectEvent().toLowerCase().split(":"); 
			switch (split[0]) {
			case "generate_coins":
				if (struct instanceof Cottage) {
					Cottage cottage = (Cottage)struct;
					//cottage.generate_coins(this);
					cottage.generateCoins(this);
				}
				break;
			case "process_mine":
				if (struct instanceof Mine) {
					Mine mine = (Mine)struct;
					try {
						mine.process_mine(this);
					} catch (InterruptedException e) {
						e.printStackTrace();
					}
				}
				break;
			}
			
		}
		
		/*
		 * Process any hourly attributes for this town.
		 *  - Culture
		 *  
		 */
		for (Town town : CivGlobal.getTowns()) {
			double cultureGenerated;
			
			// highjack this loop to display town hall warning.
			TownHall townhall = town.getTownHall();
			if (townhall == null) {
				CivMessage.sendTown(town, CivColor.Yellow+"Your town does not have a town hall! Structures have no effect!");
				continue;
			}
							
			AttrSource cultureSources = town.getCulture();
			
			// Get amount generated after culture rate/bonus.
			cultureGenerated = cultureSources.total;
			cultureGenerated = Math.round(cultureGenerated);
			town.addAccumulatedCulture(cultureGenerated);
			
			// Get from unused beakers.
			DecimalFormat df = new DecimalFormat();
			double unusedBeakers = town.getUnusedBeakers();
	
			try {
				double cultureToBeakerConversion = CivSettings.getDouble(CivSettings.cultureConfig, "beakers_per_culture");
				if (unusedBeakers > 0) {
					double cultureFromBeakers = unusedBeakers*cultureToBeakerConversion;
					cultureFromBeakers = Math.round(cultureFromBeakers);
					unusedBeakers = Math.round(unusedBeakers);
					
					if (cultureFromBeakers > 0) {
						CivMessage.sendTown(town, CivColor.LightGreen+"Converted "+CivColor.LightPurple+
								df.format(unusedBeakers)+CivColor.LightGreen+" beakers into "+CivColor.LightPurple+
								df.format(cultureFromBeakers)+CivColor.LightGreen+" culture since no tech was being researched.");
						cultureGenerated += cultureFromBeakers;
						town.addAccumulatedCulture(unusedBeakers);
						town.setUnusedBeakers(0);
					}
				}
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				return;
			}
			
			cultureGenerated = Math.round(cultureGenerated);
			CivMessage.sendTown(town, CivColor.LightGreen+"Generated "+CivColor.LightPurple+cultureGenerated+CivColor.LightGreen+" culture.");
		}
		
		/* Checking for expired vassal states. */
		CivGlobal.checkForExpiredRelations();
	}
	
	@Override
	public void run() {
		
		if (runningLock.tryLock()) {
			try {
				processTick();
			} finally {
				runningLock.unlock();
			}
		} else {
			CivLog.error("COULDN'T GET LOCK FOR HOURLY TICK. LAST TICK STILL IN PROGRESS?");
		}
		
				
	}
	

}
