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

import java.util.ArrayList;

import com.avrgaming.civcraft.cache.PlayerLocationCache;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.components.PlayerProximityComponent;

public class PlayerProximityComponentTimer implements Runnable {

	@Override
	public void run() {
		
		/* 
		 * Grab all of the player proximity components and update them, this task
		 * runs asynchronously once per tick and gathers all of the player locations
		 * into an async friendly data-structure.
		 */
		
		Component.componentsLock.lock();
		try {
			ArrayList<Component> proximityComponents = Component.componentsByType.get(PlayerProximityComponent.class.getName());
			
			if (proximityComponents == null) {
				return;
			}

			/* 
			 * Wait for the lock to free up before we continue; 
			 */
			for (Component comp : proximityComponents) {
				if (comp instanceof PlayerProximityComponent) {
					PlayerProximityComponent ppc = (PlayerProximityComponent)comp;
										
					if (ppc.lock.tryLock()) {
						try {
							ppc.buildNearbyPlayers(PlayerLocationCache.getCache());
						} finally {
							ppc.lock.unlock();
						}
					} 
				}
			}
		} finally {
			Component.componentsLock.unlock();
		}
	}

}
