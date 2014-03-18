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
package com.avrgaming.civcraft.event;

import java.util.Calendar;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;

public class GoodieRepoEvent implements EventInterface {

	public static void repoProcess() {
		class SyncTask implements Runnable {
			@Override
			public void run() {
				
				for (Town town : CivGlobal.getTowns()) {
					for (BonusGoodie goodie : town.getBonusGoodies()) {
						town.removeGoodie(goodie);
					}
				}
				
				for (BonusGoodie goodie : CivGlobal.getBonusGoodies()) {
					try {
						goodie.replenish();
					} catch (Exception e) {
						e.printStackTrace();
					}
				}
			}
			
		}
		
		TaskMaster.syncTask(new SyncTask());
	}
	
	@Override
	public void process() {
		CivLog.info("TimerEvent: GoodieRepo -------------------------------------");
		repoProcess();
		CivMessage.global("Trade Goodies have been respawned at trade outposts.");
	}

	@Override
	public Calendar getNextDate() throws InvalidConfiguration {
		Calendar cal = EventTimer.getCalendarInServerTimeZone();
		int repo_days = CivSettings.getInteger(CivSettings.goodsConfig, "trade_goodie_repo_days");
		cal.set(Calendar.SECOND, 0);
		cal.set(Calendar.MINUTE, 0);
		cal.add(Calendar.DATE, repo_days);
		return cal;
	}

}
