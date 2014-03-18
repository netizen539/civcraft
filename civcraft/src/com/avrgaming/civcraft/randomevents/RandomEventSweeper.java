package com.avrgaming.civcraft.randomevents;

import java.util.Date;
import java.util.LinkedList;

public class RandomEventSweeper implements Runnable {

	private static LinkedList<RandomEvent> events = new LinkedList<RandomEvent>();
	public static final int MILLISECONDS_PER_HOUR = 60*60*1000;
	//public static final int MILLISECONDS_PER_HOUR = 1000;

	@Override
	public void run() {

		/*
		 * The random event sweeper runs periodically on any on-going random events in progress
		 * it executes their requirements components to check for success. If we find success we
		 * then run process() on all of the components that are successful.
		 * 
		 * If we did not have success, we check the time limit on this event. If its past the time then
		 * we're going to run the failures components.
		 */
		
		/* Iterate through requirements, use check() */
		LinkedList<RandomEvent> removed = new LinkedList<RandomEvent>();
		for (RandomEvent event : events) {
			boolean allPass = false;
			if (event.requirements.size() > 0) {
				allPass = true;
				for (RandomEventComponent comp : event.requirements.values()) {
					if (!comp.onCheck()) {
						allPass = false;
					}
				}
			}
			
			if (allPass) {
				/* Process success and then cleanup event. */
				for (RandomEventComponent comp : event.success.values()) {
					comp.process();
				}
				event.cleanup();
				removed.add(event);
			} else {
				/* Event didn't pass, might be expired. Check so. */
				Date now = new Date();
				
				long expireTime = (event.getStartDate().getTime() + (event.getLength() * MILLISECONDS_PER_HOUR));
				if (now.getTime() > expireTime) {
					/* event is expired. Run failures. */
					for (RandomEventComponent comp : event.failure.values()) {
						comp.process();
					}
					event.cleanup();
					removed.add(event);
				}
			}			
		}
		
		/* Unregister any removed events. */
		events.removeAll(removed);
		
	}

	public static void register(RandomEvent randomEvent) {
		events.add(randomEvent);
	}

}
