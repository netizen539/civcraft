package com.avrgaming.civcraft.mobs;

import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityCreature;
import net.minecraft.server.v1_7_R4.PathfinderGoalSelector;

import com.avrgaming.mob.ICustomMob;
import com.avrgaming.mob.MobBaseWither;

public class Grendal extends CommonCustomMob implements ICustomMob {

	public void onCreate() {
	    initLevelAndType();

		this.setName("Grendal");
	}

	public void onCreateAttributes() {
	}

	@Override
	public void onTick() {
		
	}

	@Override
	public String getBaseEntity() {
		return MobBaseWither.class.getName();
	}

	public void onDamage(EntityCreature e, DamageSource damagesource, PathfinderGoalSelector goalSelector, PathfinderGoalSelector targetSelector) {
		
	}

	@Override
	public void onDeath(EntityCreature e) {
		
	}

	@Override
	public void onRangedAttack(Entity target) {
		
	}

	@Override
	public String getClassName() {
		return Grendal.class.getName();
	}
	

}
