package com.avrgaming.civcraft.mobs;

import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityCreature;
import net.minecraft.server.v1_7_R4.PathfinderGoalSelector;

import com.avrgaming.mob.ICustomMob;
import com.avrgaming.mob.MobBaseZombie;

public class AngryBird extends CommonCustomMob implements ICustomMob {
	
	public void onCreate() {
	    initLevelAndType();

//	    goalSelector.a(0, new PathfinderGoalFloat(e));
//	    goalSelector.a(2, new PathfinderGoalMeleeAttack(e, EntityHuman.class, 1.0D, false));
//	    goalSelector.a(8, new PathfinderGoalLookAtPlayer(e, EntityHuman.class, 8.0F));
//	    targetSelector.a(2, new PathfinderGoalNearestAttackableTarget(e, EntityHuman.class, 0, true));
		this.setName("Angry Bird");
	}

	@Override
	public void onCreateAttributes() {
//		MobBaseZombie zombie = (MobBaseZombie)e;
//		zombie.getAttributeInstance(GenericAttributes.e).setValue(5.0D);
	}

	@Override
	public void onTick() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getBaseEntity() {
		// TODO Auto-generated method stub
		return MobBaseZombie.class.getName();
	}

	public void onDamage(EntityCreature e, DamageSource damagesource, PathfinderGoalSelector goalSelector, PathfinderGoalSelector targetSelector) {

	}

	@Override
	public void onDeath(EntityCreature e) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onRangedAttack(Entity target) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public String getClassName() {
		return AngryBird.class.getName();
	}
	


}
