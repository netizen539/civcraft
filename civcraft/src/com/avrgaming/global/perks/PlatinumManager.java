package com.avrgaming.global.perks;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;

public class PlatinumManager implements Runnable {

	/* Pending platinum updates that didn't make it. Holding until reboot. */
	public static ConcurrentHashMap<String, Queue<PendingPlatinum>> pendingPlatinum = new ConcurrentHashMap<String, Queue<PendingPlatinum>>();
		
	public static void givePlatinum(Resident resident, Integer plat, String reason) {
		if (!isEnabled()) {
			return;
		}
		
		Queue<PendingPlatinum> pending = pendingPlatinum.get(resident.getName());
		if (pending == null) {
			pending = new LinkedList<PendingPlatinum>();
		}
		
		PendingPlatinum pendPlat = new PendingPlatinum();
		pendPlat.amount = plat;
		pendPlat.resident = resident;
		pendPlat.reason = reason;
		
		pending.add(pendPlat);
		pendingPlatinum.put(resident.getName(), pending);
	}
	
	
	private static String getDailyKey(Resident resident, String ident) {
		return resident.getName()+":dailyPlatinum:"+ident;
	}
	
	public static void givePlatinumDaily(Resident resident, String ident, Integer plat, String reason) {
		if (!isEnabled()) {
			return;
		}
		
		class AsyncTask implements Runnable {
			Resident resident;
			Integer plat;
			String reason;
			String ident;
			
			public AsyncTask(Resident resident, Integer plat, String reason, String ident) {
				this.resident = resident;
				this.plat = plat;
				this.reason = reason;
				this.ident = ident;
			}
			
			@Override
			public void run() {
				ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().global_lookup(getDailyKey(resident, ident));
				Calendar now = Calendar.getInstance();

				if (entries.size() == 0) {
					/* No daily yet. Allow through.*/
					CivGlobal.getSessionDB().global_add(getDailyKey(resident, ident), ""+now.getTimeInMillis());
				} else {
					Calendar then = Calendar.getInstance();
					
					then.setTimeInMillis(Long.valueOf(entries.get(0).value));
					then.add(Calendar.DAY_OF_MONTH, 2);
					
					if (!now.after(then)) {
						return;
					}
					
					CivGlobal.getSessionDB().global_update(entries.get(0).request_id, getDailyKey(resident, ident), ""+now.getTimeInMillis());
				}
				
				givePlatinum(resident, plat, reason);
			}
		}
		TaskMaster.asyncTask(new AsyncTask(resident, plat, reason, ident), 0);
	}
	
	public static void giveManyPlatinumDaily(LinkedList<Resident> residents, String ident, Integer plat, String reason) {
		if (!isEnabled()) {
			return;
		}
		
		class GiveManyPlatinumDailyAsyncTask implements Runnable {
			LinkedList<Resident> residents;
			Integer plat;
			String reason;
			String ident;
			
			public GiveManyPlatinumDailyAsyncTask(LinkedList<Resident> residents, Integer plat, String reason, String ident) {
				this.residents = residents;
				this.plat = plat;
				this.reason = reason;
				this.ident = ident;
			}

			@Override
			public void run() {
				for (Resident resident : residents) {
					ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().global_lookup(getDailyKey(resident, ident));
					Calendar now = Calendar.getInstance();
	
					if (entries.size() == 0) {
						/* No daily yet. Allow through.*/
						CivGlobal.getSessionDB().global_add(getDailyKey(resident, ident), ""+now.getTimeInMillis());
					} else {
						Calendar then = Calendar.getInstance();
						
						then.setTimeInMillis(Long.valueOf(entries.get(0).value));
						then.add(Calendar.DAY_OF_MONTH, 2);
						
						if (!now.after(then)) {
							return;
						}
						
						CivGlobal.getSessionDB().global_update(entries.get(0).request_id, getDailyKey(resident, ident), ""+now.getTimeInMillis());
					}
					
					givePlatinum(resident, plat, reason);
				}
			}
		}
		
		TaskMaster.asyncTask(new GiveManyPlatinumDailyAsyncTask(residents, plat, reason, ident), 0);

	}
	
	private static String getOnceKey(Resident resident, String ident) {
		return resident.getName()+":oncePlatinum:"+ident;
	}
	
	public static void givePlatinumOnce(Resident resident, String ident, Integer plat, String reason) {
		if (!isEnabled()) {
			return;
		}
		
		class AsyncTask implements Runnable {
			Resident resident;
			Integer plat;
			String reason;
			String ident;
			
			public AsyncTask(Resident resident, Integer plat, String reason, String ident) {
				this.resident = resident;
				this.plat = plat;
				this.reason = reason;
				this.ident = ident;
			}
			
			@Override
			public void run() {
				ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().global_lookup(getOnceKey(resident, ident));
				Calendar now = Calendar.getInstance();

				if (entries.size() == 0) {
					/* No daily yet. Allow through.*/
					CivGlobal.getSessionDB().global_add(getOnceKey(resident, ident), ""+now.getTimeInMillis());
					givePlatinum(resident, plat, reason);
				} else {
					/* only once. */
					return;
				}
			}
		}
		TaskMaster.asyncTask(new AsyncTask(resident, plat, reason, ident), 0);
	}
	
	public static HashSet<String> warnedPlayers = new HashSet<String>();
	public void updatePendingPlatinum() {
			HashMap<String, Queue<PendingPlatinum>> readdUs = new HashMap<String, Queue<PendingPlatinum>>();
			for (String key : pendingPlatinum.keySet()) {
				Queue<PendingPlatinum> pendingList = pendingPlatinum.get(key);
				Queue<PendingPlatinum> newQueue = new LinkedList<PendingPlatinum>();
				PendingPlatinum pending = pendingList.poll();

				while(pending != null) {
					Resident resident = pending.resident;
					Integer plat = pending.amount;
					String reason = pending.reason;
					
					if (resident == null || plat == null) {
						continue;
					}
					
				    try {
						CivGlobal.perkManager.updatePlatinum(resident, plat);
						
						reason = CivColor.LightGreen+reason.replace("%d", CivColor.Yellow+plat+" Platinum"+CivColor.LightGreen);
						CivMessage.sendSuccess(resident, reason);
					} catch (SQLException e) {
						/* Cant reach the DB err on the side of not sending anything */
						e.printStackTrace();
						pendingPlatinum.clear();
						return;
					} catch (NotVerifiedException e) {
						
						if (!warnedPlayers.contains(resident.getName())) {
							String url;
							try {
								url = CivSettings.getString(CivSettings.perkConfig, "system.store_url");
							} catch (InvalidConfiguration e1) {
								e1.printStackTrace();
								return;
							}
							
							CivMessage.sendError(resident, "Aww man! You've earned "+CivColor.Yellow+pending.amount+" Platinum"+CivColor.Rose+" but your in-game name is not currently verified!");
							CivMessage.sendError(resident, "Go to "+CivColor.Yellow+url+CivColor.Rose+" and verify first! We'll hold on to it until the server reboots.");
							warnedPlayers.add(resident.getName());
						}
						newQueue.add(pending);
					}
				    
				    pending = pendingList.poll();
				}
				
				/* Readd this queue as this player wasnt verfied. */
				readdUs.put(key, newQueue);
			}
			
			pendingPlatinum.clear();
			
			/* Readd */
			for (String key : readdUs.keySet()) {
				Queue<PendingPlatinum> list = readdUs.get(key);
				pendingPlatinum.put(key, list);
			}
		}
	
		@Override
		public void run() {
			/* Every so often say 5 sec, check for any missing platinum. And try to add it to the DB. */
			/* Implemented as a bukkit timer. */
			updatePendingPlatinum();
		}


		public static boolean isEnabled() {
			String enabledStr;
			try {
				enabledStr = CivSettings.getString(CivSettings.perkConfig, "system.enabled");
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				return false;
			}
			
			if (enabledStr.equalsIgnoreCase("true")) {
				return true;
			}
			
			return false;
		}
		
		public static boolean isLegacyEnabled() {
			String enabledStr;
			try {
				enabledStr = CivSettings.getString(CivSettings.perkConfig, "system.legacy_enabled");
			} catch (InvalidConfiguration e) {
				// Ignore missing config option.
				return false;
			}
			
			if (enabledStr.equalsIgnoreCase("true")) {
				return true;
			}
			
			return false;
		}
}

