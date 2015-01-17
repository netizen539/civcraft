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
package com.avrgaming.civcraft.database;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.avrgaming.civcraft.object.SQLObject;

public class SQLUpdate implements Runnable {
	
//	public static final int QUEUE_SIZE = 4096;
//	public static final int UPDATE_LIMIT = 50;
//	public static ReentrantLock lock = new ReentrantLock();
	
	private static ConcurrentLinkedQueue<SQLObject> saveObjects = new ConcurrentLinkedQueue<SQLObject>();
	public static ConcurrentHashMap<String, Integer> statSaveRequests = new ConcurrentHashMap<String, Integer>();
	public static ConcurrentHashMap<String, Integer> statSaveCompletions = new ConcurrentHashMap<String, Integer>();

	public static void add(SQLObject obj) {
		
		Integer count = statSaveRequests.get(obj.getClass().getSimpleName());
		if (count == null) {
			count = 0;
		}
		statSaveRequests.put(obj.getClass().getSimpleName(), ++count);
		
		saveObjects.add(obj);
	}
	
	@Override
	public void run() {	
		while(true) {
			try {
				
				SQLObject obj = saveObjects.poll();
				if (obj == null) {
					continue;
				}
				
				obj.saveNow();
				
				Integer count = statSaveCompletions.get(obj.getClass().getSimpleName());
				if (count == null) {
					count = 0;
				}
				statSaveCompletions.put(obj.getClass().getSimpleName(), ++count);
				
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}	
}
