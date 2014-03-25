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

import org.bukkit.Location;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.cache.ArrowFiredCache;
import com.avrgaming.civcraft.cache.CivCache;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.util.BlockCoord;

public class ProjectileArrowComponent extends ProjectileComponent {
	
	public ProjectileArrowComponent(Buildable buildable, Location turretCenter) {
		super(buildable, turretCenter);
	}

	private double power;
	
	@Override
	public void loadSettings() {
		try {
			setDamage(CivSettings.getInteger(CivSettings.warConfig, "arrow_tower.damage"));
			power = CivSettings.getDouble(CivSettings.warConfig, "arrow_tower.power");
			range = CivSettings.getDouble(CivSettings.warConfig, "arrow_tower.range");
			min_range = CivSettings.getDouble(CivSettings.warConfig, "arrow_tower.min_range");
			
			this.proximityComponent.setBuildable(buildable);
			this.proximityComponent.setCenter(new BlockCoord(getTurretCenter()));
			this.proximityComponent.setRadius(range);
			
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void fire(Location turretLoc, Entity targetEntity) {
		if (!buildable.isValid()) {
			return;
		}
		
		Location playerLoc = targetEntity.getLocation();
		playerLoc.setY(playerLoc.getY()+1); //Target the head instead of feet.
					
		turretLoc = adjustTurretLocation(turretLoc, playerLoc);
		Vector dir = getVectorBetween(playerLoc, turretLoc).normalize();
		Arrow arrow = buildable.getCorner().getLocation().getWorld().spawnArrow(turretLoc, dir, (float)power, 0.0f);
		arrow.setVelocity(dir.multiply(power));
		
		if (buildable.getTown().getBuffManager().hasBuff(Buff.FIRE_BOMB)) {
			arrow.setFireTicks(1000);
		}
		
		CivCache.arrowsFired.put(arrow.getUniqueId(), new ArrowFiredCache(this, targetEntity, arrow));
	}

	public double getPower() {
		return power;
	}
	
	public void setPower(double power) {
		this.power = power;
	}
	
	public Town getTown() {
		return buildable.getTown();
	}
	
}
