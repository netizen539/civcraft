package com.avrgaming.civcraft.randomevents;

import java.util.Calendar;
import java.util.Random;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.event.EventInterface;
import com.avrgaming.civcraft.event.EventTimer;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;

public class RandomEventTimer implements EventInterface {

	@Override
	public void process() {
		//CivMessage.global("Random Event Timer!");
		
		for (Town town : CivGlobal.getTowns()) {
			if (town.getActiveEvent() != null) {
				/* Event is already active in this town. Lets skip checking it. */
				continue;
			}
			
			/* 
			 * Choose a random event, treat all events fairly by first doing a check
			 * for each event. If it succeeds add it to a list of events that can run.
			 * 
			 *  - If the event has a lower chance, it takes prority and is run.
			 *  - If the events are the same chance, we toss a coin and choose one.
			 *  
			 */
			ConfigRandomEvent selectedEvent = null;
			Random rand = new Random();
			
			for (ConfigRandomEvent event : CivSettings.randomEvents.values()) {
				int r = rand.nextInt(1000);
				
				if (r <= event.chance) {
					if (selectedEvent == null) {
						selectedEvent = event;
						continue;
					}
					
					if (selectedEvent.chance == event.chance) {
						/* Toss a coin to pick which event should run. */
						if (rand.nextInt(1) == 0) {
							/* Override. */
							selectedEvent = event;
							continue;
						}
					} else {
						/* Choose the lowest chance event. */
						if (event.chance < selectedEvent.chance) {
							selectedEvent = event;
							continue;
						}
					}
				}
			}
			
			if (selectedEvent == null) {
				/* No event for this town at this time. */
				continue;
			}
					
			RandomEvent randEvent = new RandomEvent(selectedEvent);
			randEvent.start(town);
		}
		
	}

	@Override
	public Calendar getNextDate() throws InvalidConfiguration {
		Calendar now = EventTimer.getCalendarInServerTimeZone();
		/* Run once every 12 to 24 hours. */
		Random rand = new Random();
		int hours = rand.nextInt(12) + 12;
		now.setTimeInMillis(now.getTime().getTime() + hours*RandomEventSweeper.MILLISECONDS_PER_HOUR);
		return now;
	}


}
