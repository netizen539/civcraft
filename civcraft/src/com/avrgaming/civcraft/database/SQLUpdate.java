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
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.threading.TaskMaster;

public class SQLUpdate implements Runnable {
	
//	public static final int QUEUE_SIZE = 4096;
	public static final int UPDATE_LIMIT = 50;
	public static ReentrantLock lock = new ReentrantLock();
	
	private static Queue<SQLObject> saveObjects = new LinkedList<SQLObject>();
	
	public static void add(SQLObject obj) {
		/* XXX dont wait here, could be in sync thread */
		if (lock.tryLock()) {
			try {
				saveObjects.add(obj);
			} finally {
				lock.unlock();
			}
		} else {
			class AsyncRetrySQLUpdateTask implements Runnable {
				SQLObject obj;
				
				public AsyncRetrySQLUpdateTask(SQLObject obj) {
					this.obj = obj;
				}
				
				@Override
				public void run() {
					while (true) {
						try {
							if (lock.tryLock(3, TimeUnit.SECONDS)) {
								saveObjects.add(obj);
								return;
							} else {
								CivLog.warning("Couldn't obtain lock to save SQL Object:"+obj+" after 3 seconds! Retrying.");
							}
						} catch (InterruptedException e) {
							e.printStackTrace();
						} finally {
							lock.unlock();
						}
					}
				}
			}
			TaskMaster.asyncTask(new AsyncRetrySQLUpdateTask(obj), 0);
		}
	}
	
	@Override
	public void run() {
		lock.lock();
		try {
			for (int i = 0; i < UPDATE_LIMIT; i++) {
				SQLObject obj = saveObjects.poll();
				if (obj == null) {
					break;
				}
							
				try {
					obj.saveNow();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		} finally {
			lock.unlock();
		}
	}

}
