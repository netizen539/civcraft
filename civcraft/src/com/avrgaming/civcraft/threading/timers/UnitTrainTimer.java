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

import java.util.Iterator;
import java.util.Map.Entry;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.structure.Barracks;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.util.BlockCoord;

public class UnitTrainTimer implements Runnable {

	@Override
	public void run() {
		
		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
		while(iter.hasNext()) {
			Structure struct = iter.next().getValue();
			if (struct instanceof Barracks) {
				((Barracks)struct).updateTraining();
			}
		}
	}

}
