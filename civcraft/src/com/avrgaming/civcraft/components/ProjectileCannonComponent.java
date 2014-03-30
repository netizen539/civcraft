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

import java.util.Random;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Entity;

import com.avrgaming.civcraft.cache.CannonExplosionProjectile;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;

public class ProjectileCannonComponent extends ProjectileComponent {
	
	private int speed;
	private int splash;
	private int fireRate;
	private int halfSecondCount = 0;
	
	public ProjectileCannonComponent(Buildable buildable, Location turretCenter) {
		super(buildable, turretCenter);
	}

	@Override
	public void fire(Location turretLoc, Entity targetEntity) {
		if (halfSecondCount < fireRate) {
			halfSecondCount++;
			return;
		} else {
			halfSecondCount = 0;
		}
		
		class SyncDelay implements Runnable {
			public double x;
			public double y;
			public double z;
			public World world;
			
			@Override
			public void run() {
				world.createExplosion(x, y, z, 0.0f, false);
			}
		}
//		
//		class SyncExplode implements Runnable {
//			public double x;
//			public double y;
//			public double z;
//			public World world;
//			
//			@Override
//			public void run() {
//				//world.createExplosion(x, y, z, 1.0f, true);
//			}
//		}
		
		class SyncFollow implements Runnable {
			public CannonExplosionProjectile proj;
			
			@Override
			public void run() {
				
				if (proj.advance()) {
					proj = null;
					return;
				}
				TaskMaster.syncTask(this, 1);
			}
		}
		
		SyncFollow follow = new SyncFollow();
		CannonExplosionProjectile proj = new CannonExplosionProjectile(buildable, targetEntity.getLocation());
		proj.setLocation(new Location(turretLoc.getWorld(), turretLoc.getX(), turretLoc.getY(), turretLoc.getZ()));
		proj.setTargetLocation(targetEntity.getLocation());
		proj.setSpeed(speed);
		proj.setDamage(damage);
		proj.setSplash(splash);
		follow.proj = proj;
		TaskMaster.syncTask(follow);
		
		World world = turretLoc.getWorld();
		Random rand = new Random();
		for (int i = 0; i < 10; i++) {
			SyncDelay task = new SyncDelay();
			task.world = world;
			task.x = turretLoc.getX() + (rand.nextInt(5) - 2.5);
			task.y = turretLoc.getY() + (rand.nextInt(5) - 2.5);
			task.z = turretLoc.getZ() + (rand.nextInt(5) - 2.5);
			TaskMaster.syncTask(task, rand.nextInt(7));
		}
	
//		SyncExplode explode = new SyncExplode();
//		explode.world = world;
//		explode.x = targetEntity.getLocation().getX();
//		explode.y = targetEntity.getLocation().getY();
//		explode.z = targetEntity.getLocation().getZ();
		
//		TaskMaster.syncTask(explode, (int)TimeTools.toTicks(1));
	}
	
	

	@Override
	public void loadSettings() {
		try {
			setDamage(CivSettings.getInteger(CivSettings.warConfig, "cannon_tower.damage"));
			speed = CivSettings.getInteger(CivSettings.warConfig, "cannon_tower.speed");
			range = CivSettings.getDouble(CivSettings.warConfig, "cannon_tower.range");
			min_range = CivSettings.getDouble(CivSettings.warConfig, "cannon_tower.min_range");
			splash = CivSettings.getInteger(CivSettings.warConfig, "cannon_tower.splash");
			fireRate = CivSettings.getInteger(CivSettings.warConfig, "cannon_tower.fire_rate");
			
			
			this.proximityComponent.setBuildable(buildable);
			this.proximityComponent.setCenter(new BlockCoord(getTurretCenter()));
			this.proximityComponent.setRadius(range);
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	public int getHalfSecondCount() {
		return halfSecondCount;
	}

	public void setHalfSecondCount(int halfSecondCount) {
		this.halfSecondCount = halfSecondCount;
	}

	
	
}
