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
package com.avrgaming.civcraft.structure.farm;

import java.util.LinkedList;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Chunk;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.TaskMaster;

public class FarmPreCachePopulateTimer implements Runnable {
	
	public static int updateLimit = 50;
	public static ReentrantLock lock = new ReentrantLock();
	
	/*
	 * This task runs synchronously and grabs chunk snapshots to send 
	 * to the async task which then does the more hard-core processing.
	 */
	public FarmPreCachePopulateTimer() {
	}
	
	@Override
	public void run() {
		if (!CivGlobal.growthEnabled) {
			return;
		}
		
		if (lock.tryLock()) {
			try {
				LinkedList<FarmChunk> farms = new LinkedList<FarmChunk>();
		
				for (int i = 0; i < updateLimit; i++) {
					FarmChunk fc = CivGlobal.pollFarmChunk();
					if (fc == null) {
						break;
					}
					
					/* 
					 * Ignore any farm chunks that are no longer in the farm chunk list, 
					 * the farm has been destroyed and doesnt need to be updated anymore.
					 */
					Chunk chunk = fc.getChunk();
					if(CivGlobal.farmChunkValid(fc) && chunk.isLoaded()) {
						farms.add(fc);
						fc.snapshot = fc.getChunk().getChunkSnapshot();
					}
				}
				
				for (FarmChunk fc : farms) {
					// put valid farms back on the queue to be populated again later.
					// dont do it in the loop above, since it causes < 50 farms to be
					// populated up to 50 times.
					CivGlobal.queueFarmChunk(fc);
				}
				
				if (farms.size() > 0) {
					TaskMaster.asyncTask(new FarmCachePopulateTask(farms), 0);
				}
			} finally {
				lock.unlock();
			}
		} else {
			CivLog.warning("Tried to execute farmPreCachePopulateTimer before queue was finished, skipping populate.");
		}
	}

}
