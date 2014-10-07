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

import java.sql.SQLException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import com.avrgaming.civcraft.object.SQLObject;

public class SQLUpdate implements Runnable {
	public static final int UPDATE_LIMIT = 50;
	private static ConcurrentLinkedQueue<SQLObject> saveObjects = new ConcurrentLinkedQueue<SQLObject>();
	public static ConcurrentHashMap<String, Integer> saveObjectCounts = new ConcurrentHashMap<String, Integer>();

	public static void add(SQLObject obj) {
		Integer count = saveObjectCounts.get(obj.getClass().getSimpleName());
		if (count == null) {
			count = 1;
		} else {
			count++;
		}
		saveObjectCounts.put(obj.getClass().getSimpleName(), count);
		saveObjects.add(obj);
	}
	
	@Override
	public void run() {
		for (int i = 0; i < UPDATE_LIMIT; i++) {
			SQLObject obj = saveObjects.poll();
			if (obj == null) {
				break;
			}

						
			try {
				Integer count = saveObjectCounts.get(obj.getClass().getSimpleName());
				if (count != null) {
					count--;
					saveObjectCounts.put(obj.getClass().getSimpleName(), count);
				}
				
				obj.saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
}
