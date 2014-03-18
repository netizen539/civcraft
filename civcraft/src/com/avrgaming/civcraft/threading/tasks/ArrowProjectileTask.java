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
package com.avrgaming.civcraft.threading.tasks;

import java.util.ArrayList;
import java.util.Calendar;

import org.bukkit.entity.Arrow;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.cache.ArrowFiredCache;
import com.avrgaming.civcraft.cache.CivCache;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;

public class ArrowProjectileTask implements Runnable {

	private double homing_stop_distance = 0;
	
	public ArrowProjectileTask() {
		try {
			homing_stop_distance = CivSettings.getDouble(CivSettings.warConfig, "arrow_tower.homing_stop_distance");
			homing_stop_distance *= homing_stop_distance; //Square it now and compare agaisnts distanceSquared.
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void run() {
		Calendar now = Calendar.getInstance();
		ArrayList<ArrowFiredCache> removeUs = new ArrayList<ArrowFiredCache>();
		for (ArrowFiredCache afc : CivCache.arrowsFired.values()) {
			Arrow arrow = afc.getArrow();
			if (!arrow.isValid() || arrow.isOnGround() || arrow.isDead() || afc.isHit()) {
				removeUs.add(afc);
				continue;
			}
			
			
			if (now.after(afc.getExpired())) {
				removeUs.add(afc);
				continue;
			}
			
			double distance = afc.getArrow().getLocation().distanceSquared(afc.getTarget());
			
			if (distance < 1.0) {
				removeUs.add(afc);
				continue;
			}
			
			if (distance > homing_stop_distance) {
				afc.setTarget(afc.getTargetEntity().getLocation().add(0, 1, 0));
			}
			
			Vector dir = afc.getFromTower().getVectorBetween(afc.getTarget(), arrow.getLocation()).normalize();
			afc.getArrow().setVelocity(dir.multiply(afc.getFromTower().getPower()));
			
		}
		
		for (ArrowFiredCache afc : removeUs) {
			afc.destroy(afc.getArrow());
		}
		
	}

	
}
