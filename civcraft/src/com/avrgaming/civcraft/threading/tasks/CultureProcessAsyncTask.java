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

import java.util.HashMap;
import java.util.HashSet;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.util.ChunkCoord;

public class CultureProcessAsyncTask extends CivAsyncTask {

	public ReentrantLock lock = new ReentrantLock();
	public static boolean cultureProcessedSinceStartup = false;
	
	private void processTownCulture(Town town) {
		ChunkCoord origin;
		try {
			origin = town.getTownCultureOrigin();
		} catch (NoSuchElementException e) {
			CivLog.error("Couldn't find town chunks for town:"+town.getName()+" could not process it's culture.");
			return;
		}
		

		HashSet<ChunkCoord> expanded = new HashSet<ChunkCoord>();
		CultureChunk starting = CivGlobal.getCultureChunk(origin);
		if (starting == null) {
			starting = new CultureChunk(town, origin);
			town.addCultureChunk(starting);
			CivGlobal.addCultureChunk(starting);
		}
		
		_processCultureBreadthFirst(town, origin, starting, expanded);

		town.trimCultureChunks(expanded);
		int expandedAmount = expanded.size() - town.getCultureChunks().size();
		if (expandedAmount > 0) {
			CivMessage.sendCiv(town.getCiv(), "Town of "+town.getName()+" expanded "+expandedAmount+" chunks!");
		}
		
	}
	
	private void _processCultureBreadthFirst(Town town, ChunkCoord origin, 
			CultureChunk starting, HashSet<ChunkCoord> expanded) {
		
		Queue<CultureChunk> openList = new LinkedBlockingQueue<CultureChunk>();		
		HashMap<ChunkCoord, CultureChunk> closedChunks = new HashMap<ChunkCoord, CultureChunk>();
		ConfigCultureLevel clc = CivSettings.cultureLevels.get(town.getCultureLevel());
		
		openList.add(starting);
		if (starting.getTown() != town) {
			//Since certain towns process first, sometimes our starting node can belong
			//to another town. This is never valid, so always give the starting node back to us.
			starting.getTown().removeCultureChunk(starting);
			starting.setTown(town);
			starting.getTown().addCultureChunk(starting);
		}
						
		while (openList.isEmpty() == false) {
			//Dequeue a node.
			CultureChunk node = openList.poll();
		
			//If it was already examined, skip it.
			if (closedChunks.containsKey(node.getChunkCoord())) {
				continue;
			}
			
			//RJ.out("node:"+node.toString());

			//If the distance is greater than the current culture level's chunks we are done.
			if (node.getChunkCoord().manhattanDistance(origin) > clc.chunks) {
				break;
				//continue;
			}
					
			//Add to closed list so that any children won't re-add it.
			closedChunks.put(node.getChunkCoord(), node);			
			
			//Enqueue all neighbors.
			int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
			for (int i = 0; i < 4; i++) {
				ChunkCoord nextCoord = new ChunkCoord(node.getChunkCoord().getWorldname(), 
						node.getChunkCoord().getX() + offset[i][0], 
						node.getChunkCoord().getZ() + offset[i][1]);
				
				TownChunk townChunk = CivGlobal.getTownChunk(nextCoord);
				
				if (townChunk != null) {
					if (townChunk.getTown() != town) //part of another town's actual city limits. Never claim this. 
						continue;
				}
				
				CultureChunk neighbor = CivGlobal.getCultureChunk(nextCoord);
				if (neighbor == null) {
					// This is unclaimed territory. If the distance to this chunk is less than what this culture
					// level allows, we will claim it.
					if ((node.getChunkCoord().manhattanDistance(origin)+1) < clc.chunks) {
						neighbor = new CultureChunk(town, nextCoord);
						town.addCultureChunk(neighbor);
						CivGlobal.addCultureChunk(neighbor);
						expanded.add(neighbor.getChunkCoord());	
					} else {
						continue;
					}
				} else { 			
					// This chunk is already a culture chunk, but it might belong to another town.
					if (neighbor.getTown() != node.getTown()) {
						
						// Found another town's culture.	
						// We will take over this plot if our "power" value is larger than
						// their "power" value. 
						double nodePower = node.getPower();
						double neighborPower = neighbor.getPower();
						boolean switchOwners = false;
						
						if (nodePower > neighborPower) {
							switchOwners = true;
							
						} else if (nodePower == neighborPower) {
							// This should be fairly unlikey, but if it happens, give the plot
							// to whatever town has the most total culture.
							if (node.getTown().getAccumulatedCulture() > neighbor.getTown().getAccumulatedCulture()) {
								switchOwners = true;
							}
						}
												
						if (switchOwners) {
							neighbor.getTown().removeCultureChunk(neighbor);
							node.getTown().addCultureChunk(neighbor);
							expanded.add(neighbor.getChunkCoord());
							neighbor.setTown(node.getTown());
							// We can't really count this as an expansion because the ordering
							// of culture processing can cause false positive expansions.
						} else {
							// We could not overtake the culture of the neighbor, stop processing it.
							continue;
						}
					} else {
						/* 
						 * This is our chunk, but we should add it to expanded anyway so we
						 * can remove unused culture chunks.
						 */					
						if (neighbor.getChunkCoord().manhattanDistance(origin) < clc.chunks) {
							expanded.add(neighbor.getChunkCoord());
						}
					}
				}
				//Update the distance by finding our nearest edge block.
				//neighbor.setDistance(this.getLowestDistance(town, origin, neighbor.getChunkCoord()));
				neighbor.setDistance(neighbor.getChunkCoord().manhattanDistance(origin));

				//Override distance to 0 if it is our town's block.
				//town blocks should always be free culture.
				if (townChunk != null) {
					if (townChunk.getTown() == town) {
						neighbor.setDistance(0);
					}
				}
								
				if (closedChunks.containsKey(neighbor.getChunkCoord())) {
					//Already examined this block and its children, dont try to again.
					continue;
				} 
				
				openList.add(neighbor);
			}			
		}
		
		// Look for any orphaned culture chunks. culture chunks can becomed orphaned
		// if the 
		
		
		//RJ.out("Finished BFS.");
		return;
	}
	
	
//	private int getLowestDistance(Town town, ArrayList<TownChunk> edgeBlocks, ChunkCoord c1) {
//		int distance = Integer.MAX_VALUE;
//		
//		for(TownChunk b : edgeBlocks) {
//			ChunkCoord c2 = b.getChunkCoord();
//			int new_distance = Math.abs((c1.getX() - c2.getX())) + Math.abs((c1.getZ() - c2.getZ()));
//			if (new_distance < distance) {
//				distance = new_distance;
//			}
//		}		
//		return distance;
//	}

	@Override
	public void run() {	
		lock.lock();
		try {
			for (Town t : CivGlobal.getTowns()) {
				try {
				processTownCulture(t);
				} catch (Exception e) {
					CivLog.error("Exception generated during culture process for town:"+t.getName());
					e.printStackTrace();
				}
			}
			
			recalculateTouchingCultures();
			processStructureFlipping();
			
			CultureProcessAsyncTask.cultureProcessedSinceStartup = true;
		} finally {
			lock.unlock();
		}	
	}
	
	private static void processStructureFlipping() {
		/* 
		 * Now that all of the towns have had their say on what chunks belong to who,
		 * now we need to determine if any structures belonging to another town are in
		 * the wrong spot.
		 */
		HashMap<ChunkCoord, Structure> centerCoords = new HashMap<ChunkCoord, Structure>();
		/* Build a cache containing all of the center locations. */
		for (Structure struct : CivGlobal.getStructures()) {
			ChunkCoord coord = new ChunkCoord(struct.getCenterLocation());
			centerCoords.put(coord, struct);
		}
		
		for (Town t : CivGlobal.getTowns()) {
			t.processStructureFlipping(centerCoords);
		}
	}
	
	private static void recalculateTouchingCultures() {
		for (Town t : CivGlobal.getTowns()) {
			// Search for towns touching our town. 
			t.townTouchList.clear();
			for (CultureChunk cc : t.getCultureChunks()) {
				// Only add to open list if there is enough culture left.
				int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
				for (int i = 0; i < 4; i++) {
					ChunkCoord nextCoord = new ChunkCoord(cc.getChunkCoord().getWorldname(), 
							cc.getChunkCoord().getX() + offset[i][0], 
							cc.getChunkCoord().getZ() + offset[i][1]);
					
					CultureChunk next = CivGlobal.getCultureChunk(nextCoord);
					if (next == null) {
						continue;					
					}
					
					if (next.getTown() == cc.getTown()) {
						continue;
					}
					
					if (t.townTouchList.contains(next.getTown())) {
						continue;
					}
					
					t.townTouchList.add(next.getTown());
				}
			}
		}
	}

}
