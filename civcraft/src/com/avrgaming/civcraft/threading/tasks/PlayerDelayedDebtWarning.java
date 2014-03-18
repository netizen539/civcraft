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
package com.avrgaming.civcraft.threading.tasks;

import com.avrgaming.civcraft.object.Resident;


public class PlayerDelayedDebtWarning implements Runnable {
	Resident resident;
	
	public PlayerDelayedDebtWarning(Resident resident) {
		this.resident = resident;
	}
	
	@Override
	public void run() {
		resident.warnDebt();
	}
	
	
	
}
