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
package com.avrgaming.civcraft.test;

import java.util.Date;
import java.util.Random;

import com.avrgaming.civcraft.exception.CivTaskAbortException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.CivAsyncTask;

public class TestGetChestThread extends CivAsyncTask {
	
	public TestGetChestThread() {
	}
	
	@Override
	public void run() {
		
		Date startTime = new Date();
		long start = startTime.getTime();
		int requests = 0;
		
		while (true) {
			Random rand = new Random();
			try {
				Date nowTime = new Date();
				long now = nowTime.getTime();
				
				this.getChestInventory("world", rand.nextInt(2000), rand.nextInt(200), rand.nextInt(2000), true);
				requests++;
				
				long diff = now -  start;
				if (diff > 5000) {
					start = now;
					double requestsPerSecond = (double)requests / ((double)diff/1000);
					CivLog.warning("Processed "+requestsPerSecond+" requests per second.");
					requests = 0;
				}
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (CivTaskAbortException e) {
				CivLog.warning("Can't keep up! "+e.getMessage());
			}
		}
		
	}

	
	
}
