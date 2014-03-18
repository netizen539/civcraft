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

import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Bukkit;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;

import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;


public class SyncBuildUpdateTask implements Runnable {
	
	public static int UPDATE_LIMIT = Integer.MAX_VALUE;
	public static final int QUEUE_SIZE = 4096;
	
//	public static BlockingQueue<SimpleBlock> updateBlocks = new ArrayBlockingQueue<SimpleBlock>(QUEUE_SIZE);
	private static Queue<SimpleBlock> updateBlocks = new LinkedList<SimpleBlock>();

	
	public static ReentrantLock buildBlockLock = new ReentrantLock();
	
	public static void queueSimpleBlock(Queue<SimpleBlock> sbList) {
		buildBlockLock.lock();
		try {
			updateBlocks.addAll(sbList);
		} finally {
			buildBlockLock.unlock();
		}
	}
	
	public SyncBuildUpdateTask() {
	}
	
	/*
	 * Runs once, per tick and changes the blocks represented by SimpleBlock 
	 * up to UPDATE_LIMIT times.
	 */
	@Override
	public void run() {
		
		if (buildBlockLock.tryLock()) {
			try {
			
				int i = 0;
				for (i = 0; i < UPDATE_LIMIT; i++) {
					SimpleBlock next = updateBlocks.poll();
					if (next == null) {
						break;
					}
					
					Block block = Bukkit.getWorld(next.worldname).getBlockAt(next.x, next.y, next.z);			
					ItemManager.setTypeId(block, next.getType());
					ItemManager.setData(block, next.getData());
					
					/* Handle Special Blocks */
					Sign s;
					switch (next.specialType) {
					case COMMAND:
						ItemManager.setTypeId(block, CivData.AIR);
						ItemManager.setData(block, 0);
						break;
					case LITERAL:
						s = (Sign)block.getState();
						for (int j = 0; j < 4; j++) {
							s.setLine(j, next.message[j]);
						}
						
						s.update();
						break;
					case NORMAL:
						break;
					}
					
					if (next.buildable != null) {
						next.buildable.savedBlockCount++;
					}
				}
			} finally {
				buildBlockLock.unlock();
			}
		} else {
			CivLog.warning("Couldn't get sync build update lock, skipping until next tick.");
		}		
	}

}
