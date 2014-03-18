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
package com.avrgaming.civcraft.threading;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import org.bukkit.scheduler.BukkitTask;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.BukkitObjects;

public class TaskMaster {
	
	private static HashMap<String, BukkitTask> tasks = new HashMap<String, BukkitTask>();
	private static HashMap<String, BukkitTask> timers = new HashMap<String, BukkitTask>();
	
	
	public static long getTicksTilDate(Date date) {
		Calendar c = Calendar.getInstance();
		
		if (c.getTime().after(date)) {
			return 0;
		}
		
		long timeInSeconds = (date.getTime() - c.getTime().getTime() ) / 1000;
		return timeInSeconds*20;
	}
	
	public static long getTicksToNextHour() {
		Calendar c = Calendar.getInstance();
		Date now = c.getTime();
		
		c.add(Calendar.HOUR_OF_DAY, 1);
		c.set(Calendar.MINUTE, 0);
		c.set(Calendar.SECOND, 0);
		
		Date nextHour = c.getTime();
		
		long timeInSeconds = (nextHour.getTime() - now.getTime())/1000;
		return timeInSeconds*20;
	}
	
	
	
	public static void syncTask(Runnable runnable) {
		BukkitObjects.scheduleSyncDelayedTask(runnable, 0);
	}
	
	public static void syncTask(Runnable runnable, long l) {
		BukkitObjects.scheduleSyncDelayedTask(runnable, l);	
	}

	public static void asyncTimer(String name, Runnable runnable,
			long delay, long repeat) {
		addTimer(name, BukkitObjects.scheduleAsyncRepeatingTask(runnable, delay, repeat));
	}
	
	public static void asyncTimer(String name, Runnable runnable, long time) {
		addTimer(name, BukkitObjects.scheduleAsyncRepeatingTask(runnable, time, time));
	}
	
	public static void asyncTask(String name, Runnable runnable, long delay) {
		addTask(name, BukkitObjects.scheduleAsyncDelayedTask(runnable, delay));
	}
	
	public static void asyncTask(Runnable runnable, long delay) {
		BukkitObjects.scheduleAsyncDelayedTask(runnable, delay);
	}
	
	private static void addTimer(String name, BukkitTask timer) {
		timers.put(name, timer);
	}
	
	private static void addTask(String name, BukkitTask task) {
		//RJ.out("Added task:"+name);
		tasks.put(name, task);
	}
	
	public static void stopAll() {
		stopAllTasks();
		stopAllTimers();
	}
	
	public static void stopAllTasks() {
		for (BukkitTask task : tasks.values()) {
			task.cancel();
		}
		tasks.clear();		
	}
	
	public static void stopAllTimers() {
		for (BukkitTask timer : timers.values()) {
			timer.cancel();
		}
		//RJ.out("clearing timers");

		timers.clear();
	}

	public static void cancelTask(String name) {
		BukkitTask task = tasks.get(name);
		if (task != null) {
			task.cancel();
		}
		//RJ.out("clearing tasks");

		tasks.remove(name);
	}
	
	public static void cancelTimer(String name) {
		BukkitTask timer = tasks.get(name);
		if (timer != null) {
			timer.cancel();
		}
		//RJ.out("cancel timer:"+name);

		timers.remove(name);
	}

	public static BukkitTask getTimer(String name) {
		return timers.get(name);
	}
	
	public static BukkitTask getTask(String name) {
		return tasks.get(name);
	}

	public static List<String> getTimersList() {
		List<String> out = new ArrayList<String>();
		
		out.add(CivMessage.buildTitle("Timers Running"));
		for (String name : timers.keySet()) {
			out.add("Timer: "+name+" running.");
		}
		
		return out;
	}

	public static void syncTimer(String name, Runnable runnable, long time) {
		BukkitObjects.scheduleSyncRepeatingTask(runnable, time, time);
	}

	public static void syncTimer(String name, Runnable runnable, long delay, long repeat) {
		BukkitObjects.scheduleSyncRepeatingTask(runnable, delay, repeat);
		
	}

	public static boolean hasTask(String key) {
		BukkitTask task = tasks.get(key);
		
		if (task == null) {
			return false;
		}
		
		if (BukkitObjects.getScheduler().isCurrentlyRunning(task.getTaskId()) || BukkitObjects.getScheduler().isQueued(task.getTaskId())) {
			return true;
		} 
		
		tasks.remove(key);
				
		return false;
	}

}
