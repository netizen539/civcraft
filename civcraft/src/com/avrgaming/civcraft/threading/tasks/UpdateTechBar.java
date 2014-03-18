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
package com.avrgaming.civcraft.threading.tasks;

import java.util.LinkedList;
import java.util.Queue;

import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;

public class UpdateTechBar extends CivAsyncTask {

	private Civilization civ;
	
	public UpdateTechBar(Civilization civ) {
		this.civ = civ;
	}
	
	@Override
	public void run() {
		
		Queue<SimpleBlock> sbs = new LinkedList<SimpleBlock>();
				
		for (Town town : civ.getTowns()) {
			double percentageDone = 0.0;
			TownHall townhall = town.getTownHall();
			
			if (townhall == null) {
				return;
			}
			
			if (!townhall.isActive()) {
				return;
			}
			
			SimpleBlock sb; 
			if (civ.getResearchTech() != null) {
				percentageDone = (civ.getResearchProgress() / civ.getResearchTech().beaker_cost);
				/* Get the number of blocks to light up. */
				int size = townhall.getTechBarSize();
				int blockCount = (int)(percentageDone*townhall.getTechBarSize()); 
							
				for (int i = 0; i < size; i++) {
					BlockCoord bcoord = townhall.getTechBarBlockCoord(i);
					if (bcoord == null) {
						/* tech bar DNE, might not be finished yet. */
						continue;
					}
	
					if (i <= blockCount) {
						sb = new SimpleBlock(CivData.WOOL, CivData.DATA_WOOL_GREEN);
						sb.x = bcoord.getX(); sb.y = bcoord.getY(); sb.z = bcoord.getZ();
						sb.worldname = bcoord.getWorldname();
						sbs.add(sb);
					} else {
						sb = new SimpleBlock(CivData.WOOL, CivData.DATA_WOOL_BLACK);
						sb.x = bcoord.getX(); sb.y = bcoord.getY(); sb.z = bcoord.getZ();
						sb.worldname = bcoord.getWorldname();
						sbs.add(sb);				
					}
					
					townhall.addStructureBlock(townhall.getTechBar(i), false);
				}
			} else {
				/* Resets the bar after a tech is finished. */
				int size = townhall.getTechBarSize();
				for (int i = 0; i < size; i++) {
					BlockCoord bcoord = townhall.getTechBarBlockCoord(i);
					if (bcoord == null) {
						/* tech bar DNE, might not be finished yet. */
						continue;
					}
					
					sb = new SimpleBlock(CivData.WOOL, CivData.DATA_WOOL_BLACK);
					sb.x = bcoord.getX(); sb.y = bcoord.getY(); sb.z = bcoord.getZ();
					sb.worldname = bcoord.getWorldname();
					sbs.add(sb);
					townhall.addStructureBlock(townhall.getTechBar(i), false);
				}
			}
			
			if (townhall.getTechnameSign() != null) {
				BlockCoord bcoord = townhall.getTechnameSign();
				sb = new SimpleBlock(CivData.WALL_SIGN, townhall.getTechnameSignData());
				sb.x = bcoord.getX(); sb.y = bcoord.getY(); sb.z = bcoord.getZ();
				sb.worldname = bcoord.getWorldname();
				sb.specialType = Type.LITERAL;
								
				if (civ.getResearchTech() != null) {			
					sb.message[0] = "Researching";
					sb.message[1] = "";
					sb.message[2] = civ.getResearchTech().name;
					sb.message[3] = "";
				} else {
					sb.message[0] = "Researching";
					sb.message[1] = "";
					sb.message[2] = "Nothing";
					sb.message[3] = "";			
				}
				sbs.add(sb);
				
				townhall.addStructureBlock(townhall.getTechnameSign(), false);

			}
			
			if (townhall.getTechdataSign() != null) {
				BlockCoord bcoord = townhall.getTechdataSign();
				sb = new SimpleBlock(CivData.WALL_SIGN, townhall.getTechdataSignData());
				sb.x = bcoord.getX(); sb.y = bcoord.getY(); sb.z = bcoord.getZ();
				sb.worldname = bcoord.getWorldname();
				sb.specialType = Type.LITERAL;
					
				if (civ.getResearchTech() != null) {
					percentageDone = Math.round(percentageDone*100);
				
					sb.message[0] = "Percent";
					sb.message[1] = "Complete";
					sb.message[2] = ""+percentageDone+"%";
					sb.message[3] = "";
					
				} else {
					sb.message[0] = "Use";
					sb.message[1] = "/civ research";
					sb.message[2] = "to start";
					sb.message[3] = "";				
				}
				sbs.add(sb);

				
				townhall.addStructureBlock(townhall.getTechdataSign(), false);
			}
			
		}
		
		this.updateBlocksQueue(sbs);
	}

}
