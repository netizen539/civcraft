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
import java.util.concurrent.TimeUnit;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.threading.CivAsyncTask;

public class FarmGrowthSyncTask extends CivAsyncTask {
	
	// XXX Despite being named a sync timer, this task is now actually asynchronous
	
	public void processFarmChunks() {
		if (!CivGlobal.growthEnabled) {
			return;
		}
		
		//Queue<FarmChunk> regrow = new LinkedList<FarmChunk>();
		for (FarmChunk fc : CivGlobal.getFarmChunks()) {
			if (fc.getTown() == null || fc.getStruct() == null) {
				System.out.println("FarmChunkError: Could not process farm chunk, town or struct was null. Orphan?");
				continue;
			}
						
			/* Since we're now async, we can wait on this lock. */
			try {
				if(!fc.lock.tryLock(TIMEOUT, TimeUnit.MILLISECONDS)) {
					return;
				}
			} catch (InterruptedException e1) {
				e1.printStackTrace();
				return;
			}
			try {
				try {
					fc.processGrowth(this);
				} catch (InterruptedException e) {
					e.printStackTrace();
					return;
				}
			} finally {
				fc.lock.unlock();
			}
			//} else {
			//	regrow.add(fc);
			//}
		}
		
		//if (regrow.size() > 0) {
	//		TaskMaster.syncTask(new FarmGrowthRegrowTask(regrow), 0);
	//	}
	}
	
	
	@Override
	public void run() {
		try {
			processFarmChunks();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	
}