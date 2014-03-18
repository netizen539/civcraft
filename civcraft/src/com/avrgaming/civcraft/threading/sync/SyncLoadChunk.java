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
import org.bukkit.Chunk;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.sync.request.LoadChunkRequest;

public class SyncLoadChunk implements Runnable {
	/*
	 * Loads a chunk synchronously and notifies the thread that was waiting on it when it is loaded.
	 */
	public static final int UPDATE_LIMIT = 2048;
	
	public static Queue<LoadChunkRequest> requestQueue = new LinkedList<LoadChunkRequest>();
	public static ReentrantLock lock;
	
	public SyncLoadChunk() {
		lock = new ReentrantLock();
	}

	@Override
	public void run() {
		
		if (lock.tryLock()) {
			try {
				for (int i = 0; i < UPDATE_LIMIT; i++) {
					LoadChunkRequest request = requestQueue.poll();
					if (request == null) {
						return;
					}
					
					Chunk chunk = Bukkit.getWorld(request.worldName).getChunkAt(request.x, request.z);
					if (!chunk.isLoaded()) {
						if (!chunk.load()) {
							CivLog.error("Couldn't load chunk at "+request.x+","+request.z);
							continue;
						}
					}
					
					request.finished = true;
					request.condition.signalAll();					
				}
			} finally {
				lock.unlock();
			}
			
		} else {
			//CivLog.warning("SyncLoadChunk: lock was busy, try again next tick.");
		}
	}
	
	
	
}
