package com.avrgaming.civcraft.mobs;

import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.EntityCreature;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.EntityInsentient;
import net.minecraft.server.v1_7_R4.GenericAttributes;
import net.minecraft.server.v1_7_R4.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_7_R4.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_7_R4.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_7_R4.PathfinderGoalSelector;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.mobs.components.MobComponentDefense;
import com.avrgaming.mob.ICustomMob;
import com.avrgaming.mob.MobBaseZombieGiant;

public class YoboBoss extends CommonCustomMob implements ICustomMob {
	private String entityType = MobBaseZombieGiant.class.getName();

	
	public void onCreate() {
	    initLevelAndType();
		
	    MobBaseZombieGiant zombie = (MobBaseZombieGiant)this.entity;
	    zombie.height *= 6.0f;
	    
	    getGoalSelector().a(7, new PathfinderGoalRandomStroll((EntityCreature) entity, 100.0F));
	    getGoalSelector().a(8, new PathfinderGoalLookAtPlayer((EntityInsentient) entity, EntityHuman.class, 8.0F));
	    getGoalSelector().a(2, new PathfinderGoalMeleeAttack((EntityCreature) entity, EntityHuman.class, 100.0F, false));
	   // getTargetSelector().a(1, new PathfinderGoalHurtByTarget((EntityCreature) entity, true));
	    getTargetSelector().a(2, new PathfinderGoalNearestAttackableTarget((EntityCreature) entity, EntityHuman.class, 0, true));

	    MobComponentDefense defense = new MobComponentDefense(9.0);
	    this.addComponent(defense);
	    
	    this.setName(this.getLevel().getName()+" "+this.getType().getName());
	}
	
	public void onCreateAttributes() {
		MobBaseZombieGiant zombie = (MobBaseZombieGiant)this.entity;
		zombie.getAttributeInstance(GenericAttributes.e).setValue(200.0D);
		zombie.getAttributeInstance(GenericAttributes.maxHealth).setValue(5000.0D);
		zombie.getAttributeInstance(GenericAttributes.c).setValue(1.0D);
		zombie.getAttributeInstance(GenericAttributes.d).setValue(200000.0D);
		//AttributeModifier mod = new AttributeModifier()
		zombie.setHealth(5000.0f);
	}
	
	@Override
	public String getBaseEntity() {
		return entityType;
	}
	
	@Override
	public void onDamage(EntityCreature e, DamageSource damagesource, PathfinderGoalSelector goalSelector, PathfinderGoalSelector targetSelector) {
		goalSelector.a(2, new PathfinderGoalMeleeAttack(e, EntityHuman.class, 1.0D, false));
		for (int i = 0; i < 6; i++) {
			try {
				MobSpawner.spawnCustomMob(MobSpawner.CustomMobType.ANGRYYOBO, this.getLevel(), getLocation(e));
			} catch (CivException e1) {
				e1.printStackTrace();
			}
		}		
	}

	@Override
	public String getClassName() {
		return YoboBoss.class.getName();
	}
	
}
