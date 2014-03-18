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
package com.avrgaming.civcraft.components;

import java.util.Collection;
import java.util.HashSet;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.cache.PlayerLocationCache;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.util.BlockCoord;

public class PlayerProximityComponent extends Component {

	/*
	 * This component maintains a list of nearby players using the player location 
	 * cache. It is populated asynchronously so that it can be accessed in as little
	 * time as possible from a synchronous thread.
	 */
	
	private HashSet<PlayerLocationCache> nearbyPlayers;
	
	public ReentrantLock lock;
	
	/* Center location from which we check */
	private BlockCoord center;
	
	/* Max distance from which we check. */
	private double radiusSquared;
	
	/* Buildable that this component is attached to. */
	private Buildable buildable;

	public PlayerProximityComponent() {
		lock = new ReentrantLock();
	}
	
	@Override
	public void onLoad() {
		
	}

	@Override
	public void onSave() {
		
	}	
	
	public void setNearbyPlayers(HashSet<PlayerLocationCache> newSet) {
		/* Proxy component should already be locked. no need to relock. */
		this.nearbyPlayers = newSet;
	}
	
	@SuppressWarnings("unchecked")
	public HashSet<PlayerLocationCache> tryGetNearbyPlayers(boolean retry) {	
		/* 
		 * Tries to grab a list of nearby players. 
		 * Sends back nothing if the lock is currently in use.
		 */
		if (retry) {
			this.lock.lock();
		} else {
			if (!this.lock.tryLock()) {
				return new HashSet<PlayerLocationCache>();
			}
		}
	
		try {	
			if (nearbyPlayers == null) {
				return new HashSet<PlayerLocationCache>();
			}	
			return (HashSet<PlayerLocationCache>) this.nearbyPlayers.clone();
				
		} finally {
			this.lock.unlock();
		}
	}

	@SuppressWarnings("unchecked")
	public HashSet<PlayerLocationCache> waitGetNearbyPlayers() {	
		/* 
		 * Tries to grab a list of nearby players. 
		 * Sends back nothing if the lock is currently in use.
		 */
		this.lock.lock();
		try {
			if (nearbyPlayers == null) {
				return new HashSet<PlayerLocationCache>();
			}
			
			return (HashSet<PlayerLocationCache>) this.nearbyPlayers.clone();
			
		} finally {
			this.lock.unlock();
		}
	}
	

	public BlockCoord getCenter() {
		return center;
	}


	public void setCenter(BlockCoord center) {
		this.center = center;
	}


	public double getRadiusSquared() {
		return radiusSquared;
	}


	public void setRadius(double radius) {
		this.radiusSquared = Math.pow(radius, 2);
	}
	
	public void buildNearbyPlayers(Collection<PlayerLocationCache> collection) {
		HashSet<PlayerLocationCache> newSet = new HashSet<PlayerLocationCache>();
		
		for (PlayerLocationCache pc : collection) {
			if (pc.isVanished()) {
				continue;
			}
			
			if (pc.getCoord().distanceSquared(this.center) < radiusSquared) {
				newSet.add(pc);
			}
		}
	
		this.setNearbyPlayers(newSet);
		
		return;
	}


	public Buildable getBuildable() {
		return buildable;
	}


	public void setBuildable(Buildable buildable) {
		this.buildable = buildable;
	}

	

}
