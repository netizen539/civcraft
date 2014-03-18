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
package com.avrgaming.civcraft.threading.sync;

import java.util.Iterator;
import java.util.Map.Entry;
import java.util.Random;

import org.bukkit.Effect;
import org.bukkit.World;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.util.BlockCoord;

public class DamagedStructureTimer implements Runnable {
	
	@Override
	public void run() {
		
		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
		while(iter.hasNext()) {
			Structure struct = iter.next().getValue();
			
			if (struct.isDestroyed()) {
				int size = struct.getStructureBlocks().size();
				World world = struct.getCorner().getBlock().getWorld();

				for (int i = 0; i < size/10; i++) {
					Random rand = new Random();
					int index = rand.nextInt(size);
					
					// slower but uses less memory.
					int j = 0;
					for (BlockCoord coord : struct.getStructureBlocks().keySet()) {
						
						if (j < index) {
							j++;
							continue;
						}
						
						world.playEffect(coord.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
						break;
					}
					//BlockCoord coord = (BlockCoord) struct.getStructureBlocks().keySet().toArray()[index];
				}
			}
		}
	}
	
}
