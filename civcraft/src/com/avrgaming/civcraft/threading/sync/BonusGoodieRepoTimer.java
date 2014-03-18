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

public class BonusGoodieRepoTimer implements Runnable {

	@Override
	public void run() {
		//UNUSED now using event interface.
	}

//	private String getKey() {
//		return "global:goodieRepoTimer";
//	}
//	
//	@Override
//	public void run() {
//	
//		// Do a SessionDB check for the last time we repo'd
//		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getKey());
//		
//		if (entries == null || entries.size() == 0) {
//			// No last time, assume that this is the first, create a new date and throw it in the SDB
//			Date now = new Date();
//			long time = now.getTime();
//			
//			String value = ""+time;
//			CivGlobal.getSessionDB().add(getKey(), value, 0, 0, 0);	
//			return;
//		}
//		
//		// An entry has been found.
//		long time = Long.valueOf(entries.get(0).value);
//		Calendar lastRepo = Calendar.getInstance();
//		lastRepo.setTimeInMillis(time);
//		
//		int trade_goodie_repo_days;
//		try {
//			trade_goodie_repo_days = CivSettings.getInteger(CivSettings.goodsConfig, "trade_goodie_repo_days");
//		} catch (InvalidConfiguration e) {
//			e.printStackTrace();
//			return;
//		}
//		
//		lastRepo.add(Calendar.DATE, trade_goodie_repo_days);
//		
//		// We are now after the repo time.
//		Calendar now = Calendar.getInstance();
//		
//		if (now.getTimeInMillis() > lastRepo.getTimeInMillis()) {
//			for (BonusGoodie goodie : CivGlobal.getBonusGoodies()) {
//				try {
//					goodie.replenish();
//				} catch (Exception e) {
//					e.printStackTrace();
//				}
//			}
//			CivMessage.global("Trade Goodies have been respawned at trade outposts.");
//
//			CivGlobal.getSessionDB().update(entries.get(0).request_id, getKey(), ""+now.getTimeInMillis()); 
//		}
//	}

	
	
}
