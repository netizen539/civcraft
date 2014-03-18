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

import java.util.ArrayList;

import org.bukkit.ChunkSnapshot;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.structure.Windmill;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.ChunkCoord;

public class WindmillStartSyncTask implements Runnable {

	Windmill windmill;
	
	public WindmillStartSyncTask(Windmill windmill) {
		this.windmill = windmill;
	}
	
	@Override
	public void run() {
		/* Find adjacent farms, get their chunk snapshots and continue processing in our thread. */
		ChunkCoord cc = new ChunkCoord(windmill.getCorner());
		ArrayList<ChunkSnapshot> snapshots = new ArrayList<ChunkSnapshot>();
				
		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 }, { 1, 1 }, {-1,-1 }, {-1, 1}, {1, -1} };
		for (int i = 0; i < 8; i++) {
			cc.setX(cc.getX() + offset[i][0]);
			cc.setZ(cc.getZ() + offset[i][1]);
			
			FarmChunk farmChunk = CivGlobal.getFarmChunk(cc);
			if (farmChunk != null) {
				snapshots.add(farmChunk.getChunk().getChunkSnapshot());
			}
			
			cc.setFromLocation(windmill.getCorner().getLocation());
		}
		
		
		if (snapshots.size() == 0) {
			return;
		}
		
		/* Fire off an async task to do some post processing. */
		TaskMaster.asyncTask("", new WindmillPreProcessTask(windmill, snapshots), 0);
				
	}

}
