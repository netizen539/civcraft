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

import com.avrgaming.civcraft.main.CivGlobal;


public class FarmCachePopulateTask implements Runnable {

	LinkedList<FarmChunk> farms;
	
	public FarmCachePopulateTask(LinkedList<FarmChunk> farms) {
		this.farms = farms;
	}
	
	@Override
	public void run() {
		if (!CivGlobal.growthEnabled) {
			return;
		}
		
		for (FarmChunk fc : farms) {
			try {
				fc.populateCropLocationCache();
			} catch (Exception e){
				e.printStackTrace();
			}
		}

	}

}
