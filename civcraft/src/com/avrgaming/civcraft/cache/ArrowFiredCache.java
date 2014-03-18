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
package com.avrgaming.civcraft.cache;

import java.util.Calendar;
import java.util.UUID;

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;

import com.avrgaming.civcraft.components.ProjectileArrowComponent;

public class ArrowFiredCache {
	private ProjectileArrowComponent fromTower;
	private Location target;
	private Entity targetEntity;
	private Arrow arrow;
	private UUID uuid;
	private Calendar expired;
	private boolean hit = false;
	
	public ArrowFiredCache(ProjectileArrowComponent tower, Entity targetEntity, Arrow arrow) {
		this.setFromTower(tower);
		this.target = targetEntity.getLocation();
		this.targetEntity = targetEntity;
		this.setArrow(arrow);
		this.uuid = arrow.getUniqueId();
		expired = Calendar.getInstance();
		expired.add(Calendar.SECOND, 5);
	}


	/**
	 * @return the target
	 */
	public Location getTarget() {
		return target;
	}

	/**
	 * @param target the target to set
	 */
	public void setTarget(Location target) {
		this.target = target;
	}

	/**
	 * @return the arrow
	 */
	public Arrow getArrow() {
		return arrow;
	}

	/**
	 * @param arrow the arrow to set
	 */
	public void setArrow(Arrow arrow) {
		this.arrow = arrow;
	}

	public Object getUUID() {
		return uuid;
	}
	
	public void destroy(Arrow arrow) {
		arrow.remove();
		this.arrow = null;
		CivCache.arrowsFired.remove(this.getUUID());
		this.uuid = null;
	}


	public void destroy(Entity damager) {
		if (damager instanceof Arrow) {
			this.destroy((Arrow)damager);
		}
	}


	public Calendar getExpired() {
		return expired;
	}


	public void setExpired(Calendar expired) {
		this.expired = expired;
	}


	public boolean isHit() {
		return hit;
	}


	public void setHit(boolean hit) {
		this.hit = hit;
	}


	public ProjectileArrowComponent getFromTower() {
		return fromTower;
	}


	public void setFromTower(ProjectileArrowComponent fromTower) {
		this.fromTower = fromTower;
	}


	public Entity getTargetEntity() {
		return targetEntity;
	}


	public void setTargetEntity(Entity targetEntity) {
		this.targetEntity = targetEntity;
	}

	
}
