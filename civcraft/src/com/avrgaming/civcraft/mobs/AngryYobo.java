package com.avrgaming.civcraft.mobs;


import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityCreature;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.EntityInsentient;
import net.minecraft.server.v1_7_R4.PathfinderGoalFloat;
import net.minecraft.server.v1_7_R4.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_7_R4.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_7_R4.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalSelector;

import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;

import com.avrgaming.civcraft.mobs.components.MobComponentDefense;
import com.avrgaming.mob.ICustomMob;
import com.avrgaming.mob.MobBaseZombie;

public class AngryYobo  extends CommonCustomMob implements ICustomMob {

	public void onCreate() {
	    initLevelAndType();

	    getGoalSelector().a(0, new PathfinderGoalFloat((EntityInsentient) entity));
	    getGoalSelector().a(2, new PathfinderGoalMeleeAttack((EntityCreature) entity, EntityHuman.class, 1.0D, false));
	    getGoalSelector().a(8, new PathfinderGoalLookAtPlayer((EntityInsentient) entity, EntityHuman.class, 8.0F));

	    getTargetSelector().a(2, new PathfinderGoalNearestAttackableTarget((EntityCreature) entity, EntityHuman.class, 0, true));
	    this.setName(this.getLevel().getName()+" "+this.getType().getName());
	    MobBaseZombie zombie = ((MobBaseZombie)this.entity);
	    zombie.setBaby(true);
	}

	@Override
	public void onTick() {
		super.onTick();		
	}

	@Override
	public String getBaseEntity() {
		return MobBaseZombie.class.getName();
	}

	public void onDamage(EntityCreature e, DamageSource damagesource, PathfinderGoalSelector goalSelector, PathfinderGoalSelector targetSelector) {

		
	}
	
	public void onCreateAttributes() {
		MobComponentDefense defense;
	    this.setKnockbackResistance(0.99);

		switch (this.getLevel()) {
		case LESSER:
		    defense = new MobComponentDefense(3.5);
		    setMaxHealth(10.0);
		    this.setAttack(5.0);
		    this.addDrop("mat_metallic_crystal_fragment_1", 0.05);
		    
		    this.addDrop("mat_forged_clay", 0.1);
		    this.addDrop("mat_crafted_reeds", 0.1);
		    this.addDrop("mat_crafted_sticks", 0.1);
		    this.coinDrop(1, 25);

			break;
		case GREATER:
		    defense = new MobComponentDefense(10);
		    setMaxHealth(15.0);
		    this.setAttack(8.0);
		    
		    this.addDrop("mat_metallic_crystal_fragment_2", 0.05);

		    this.addDrop("mat_clay_steel_cast", 0.05);
		    this.addDrop("mat_steel_ingot", 0.05);

		    this.addDrop("mat_varnish", 0.01);
		    this.addDrop("mat_sticky_resin", 0.01);
		    this.coinDrop(10, 50);
		    break;
		case ELITE:
		    defense = new MobComponentDefense(16);
		    setMaxHealth(20.0);
		    this.setAttack(13.0);
		    
		    this.addDrop("mat_metallic_crystal_fragment_3", 0.05);

		    this.addDrop("mat_clay_steel_cast", 0.05);
		    this.addDrop("mat_carbide_steel_ingot", 0.05);

		    this.addDrop("mat_sticky_resin", 0.1);
		    this.addDrop("mat_smithy_resin", 0.01);
		    this.coinDrop(20, 80);
			break;
		case BRUTAL:
		    defense = new MobComponentDefense(20);
		    setMaxHealth(30.0);
		    this.setAttack(18.0);
		    
		    this.addDrop("mat_metallic_crystal_fragment_4", 0.05);

		    this.addDrop("mat_tungsten_ingot", 0.05);
		    this.addDrop("mat_clay_tungsten_casting", 0.05);

		    this.addDrop("mat_sticky_resin", 0.1);
		    this.addDrop("mat_smithy_resin", 0.01);
		    this.coinDrop(20, 150);
			break;
		default:
		    defense = new MobComponentDefense(2);
			break;
		}
		
	    this.addComponent(defense);
	}
	
	@Override
	public void onRangedAttack(Entity target) {
		
	}

	@Override
	public String getClassName() {
		return AngryYobo.class.getName();
	}
	
	@Override
	public void onTarget(EntityTargetEvent event) {
		super.onTarget(event);
		
		if (event.getReason().equals(TargetReason.FORGOT_TARGET) ||
		    event.getReason().equals(TargetReason.TARGET_DIED)) {
			event.getEntity().remove();
		}
		
	}


}
