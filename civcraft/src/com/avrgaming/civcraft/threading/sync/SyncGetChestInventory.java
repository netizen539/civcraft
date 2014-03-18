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
import org.bukkit.block.Chest;

import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.threading.sync.request.GetChestRequest;
import com.avrgaming.civcraft.util.ItemManager;

public class SyncGetChestInventory implements Runnable {
	
	public static final int TIMEOUT_SECONDS = 2;
	public static final int UPDATE_LIMIT = 20;
	
	public static ReentrantLock lock;
	
	public static Queue<GetChestRequest> requestQueue = new LinkedList<GetChestRequest>();
	
	public static boolean add(GetChestRequest request)  
	{
		//XXX is this needed anymore?
		return requestQueue.offer(request);
	}
	
	public SyncGetChestInventory() {
		lock = new ReentrantLock();
	}
	
	@Override
	public void run() {
		
		if (lock.tryLock()) {
			try {	
				for (int i = 0; i < UPDATE_LIMIT; i++) {
					GetChestRequest request = requestQueue.poll();
					if (request == null) {
						return;
					}
					
					Block b = Bukkit.getWorld(request.worldName).getBlockAt(request.block_x, request.block_y, request.block_z);
					Chest chest = null;

					// We will return NULL if the chunk was not loaded.
					if (b.getChunk().isLoaded()) {
						try {
							chest = (Chest)b.getState();
						} catch (ClassCastException e) {
							/* The block wasn't a chest, but force it. */
							ItemManager.setTypeId(b, CivData.CHEST);
							ItemManager.setTypeId(b.getState(), CivData.CHEST);
							b.getState().update();
							chest = (Chest)b.getState();
							
						}
					} 
			
					/* Set the result and signal all threads we're complete. */					
					request.result = chest.getBlockInventory();
					request.finished = true;
					request.condition.signalAll();
					
				}
			} finally {
				lock.unlock();
			}
		} else {
		//	CivLog.warning("Unable to aquire lock in sync tick thread. Lock busy.");
		}
	}
}
