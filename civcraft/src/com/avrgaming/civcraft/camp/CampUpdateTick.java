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
package com.avrgaming.civcraft.camp;

import com.avrgaming.civcraft.threading.CivAsyncTask;

public class CampUpdateTick extends CivAsyncTask {

	private Camp camp;
	
	public CampUpdateTick(Camp camp) {
		this.camp = camp;
	}
	
	@Override
	public void run() {
		if(camp.sifterLock.tryLock()) {
			try {
				if (camp.isSifterEnabled()) {
					camp.sifter.run(this);
				}
			} finally {
				camp.sifterLock.unlock();
			}
		} 
		
	}

}
