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

public class LagSimulationTimer implements Runnable {

	int targetTPS;
	
	public LagSimulationTimer(int targetTPS) {
		this.targetTPS = targetTPS;
	}
	
	
	@Override
	public void run() {
	
		/* Assume we're currently running at 20 tps. */
		int waitTime = 20 - targetTPS;
		
		if (waitTime < 0) {
			return;
		}
		
		double secondsPerTick = 0.05;
		long millis = (long)(waitTime*secondsPerTick*1000);
		synchronized (this) {
		try {
			this.wait(millis);
		} catch (InterruptedException e) {
		}
		}
	}

}
