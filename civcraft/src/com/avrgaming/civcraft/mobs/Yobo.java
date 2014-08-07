package com.avrgaming.civcraft.mobs;

import java.util.LinkedList;

import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityCreature;
import net.minecraft.server.v1_7_R4.EntityDamageSource;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.EntityInsentient;
import net.minecraft.server.v1_7_R4.PathfinderGoalHurtByTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_7_R4.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_7_R4.PathfinderGoalRandomStroll;
import net.minecraft.server.v1_7_R4.PathfinderGoalSelector;

import org.bukkit.block.Biome;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobLevel;
import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobType;
import com.avrgaming.civcraft.mobs.components.MobComponentDefense;
import com.avrgaming.mob.ICustomMob;
import com.avrgaming.mob.MobBaseZombie;

public class Yobo extends CommonCustomMob implements ICustomMob {

	private String entityType = MobBaseZombie.class.getName();
	private boolean angry = false;
	
	LinkedList<Entity> minions = new LinkedList<Entity>();
	
	public void onCreate() {
	    initLevelAndType();

	    getGoalSelector().a(7, new PathfinderGoalRandomStroll((EntityCreature) entity, 1.0D));
	    getGoalSelector().a(8, new PathfinderGoalLookAtPlayer((EntityInsentient) entity, EntityHuman.class, 8.0F));
	    getTargetSelector().a(1, new PathfinderGoalHurtByTarget((EntityCreature) entity, true));

	    this.setName(this.getLevel().getName()+" "+this.getType().getName());
	}
	
	public void onCreateAttributes() {
		MobComponentDefense defense;
	    this.setKnockbackResistance(0.99);

		switch (this.getLevel()) {
		case LESSER:
		    defense = new MobComponentDefense(3.5);
		    setMaxHealth(20.0);
		    modifySpeed(1.3);
		    this.setAttack(8.0);
		    
		    this.addDrop("mat_metallic_crystal_fragment_1", 0.05);
		    
		    this.addDrop("mat_forged_clay", 0.1);
		    this.addDrop("mat_crafted_reeds", 0.1);
		    this.addDrop("mat_crafted_sticks", 0.1);
		    this.coinDrop(1, 25);

			break;
		case GREATER:
		    defense = new MobComponentDefense(10);
		    setMaxHealth(25.0);
		    modifySpeed(1.4);
		    this.setAttack(13.0);
		    
		    this.addDrop("mat_metallic_crystal_fragment_2", 0.05);

		    this.addDrop("mat_clay_steel_cast", 0.05);
		    this.addDrop("mat_leather_straps", 0.05);
		    this.addDrop("mat_steel_ingot", 0.05);

		    this.addDrop("mat_varnish", 0.01);
		    this.addDrop("mat_sticky_resin", 0.01);
		    this.coinDrop(10, 50);

		    break;
		case ELITE:
		    defense = new MobComponentDefense(16);
		    setMaxHealth(30.0);
		    modifySpeed(1.5);
		    this.setAttack(15.0);
		    
		    this.addDrop("mat_metallic_crystal_fragment_3", 0.05);

		    this.addDrop("mat_clay_steel_cast", 0.05);
		    this.addDrop("mat_reinforced_braid", 0.05);
		    this.addDrop("mat_carbide_steel_ingot", 0.05);

		    this.addDrop("mat_sticky_resin", 0.1);
		    this.addDrop("mat_smithy_resin", 0.01);
		    this.coinDrop(20, 80);

			break;
		case BRUTAL:
		    defense = new MobComponentDefense(16);
		    setMaxHealth(40.0);
		    modifySpeed(1.5);
		    this.setAttack(20.0);
		    
		    this.addDrop("mat_metallic_crystal_fragment_4", 0.05);

		    this.addDrop("mat_clay_tungsten_casting", 0.05);
		    this.addDrop("mat_artisan_leather", 0.05);
		    this.addDrop("mat_tungsten_ingot", 0.05);

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
	public String getBaseEntity() {
		return entityType;
	}

	@Override
	public void onDamage(EntityCreature e, DamageSource damagesource, PathfinderGoalSelector goalSelector, PathfinderGoalSelector targetSelector) {
		
		if (!(damagesource instanceof EntityDamageSource)) {
			return;
		}
		
		if (this.getLevel() == null) {
			this.setLevel(MobSpawner.CustomMobLevel.valueOf(getData("level")));
			if (this.getLevel() == null) {
				try {
					throw new CivException("Level was null after retry.");
				} catch (CivException e2) {
					CivLog.error("getData(level):"+getData("level"));
					e2.printStackTrace();
				}
			}
		}
		
		if (!angry) {
			angry = true;
			goalSelector.a(2, new PathfinderGoalMeleeAttack(e, EntityHuman.class, 1.0D, false));
			for (int i = 0; i < 4; i++) {
				try {
					this.minions.add(MobSpawner.spawnCustomMob(MobSpawner.CustomMobType.ANGRYYOBO, this.getLevel(), getLocation(e)).entity);
				} catch (CivException e1) {
					e1.printStackTrace();
				}
			}
		}
	}

	@Override
	public String getClassName() {
		return Yobo.class.getName();
	}

	public static void register() {
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.LESSER, Biome.PLAINS);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.LESSER, Biome.FOREST);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.LESSER, Biome.BIRCH_FOREST);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.LESSER, Biome.BIRCH_FOREST_HILLS);
		
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.GREATER, Biome.SUNFLOWER_PLAINS);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.GREATER, Biome.FLOWER_FOREST);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.GREATER, Biome.BIRCH_FOREST_HILLS_MOUNTAINS);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.GREATER, Biome.BIRCH_FOREST_MOUNTAINS);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.GREATER, Biome.FOREST_HILLS);

		
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.ELITE, Biome.EXTREME_HILLS);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.ELITE, Biome.EXTREME_HILLS_PLUS);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.ELITE, Biome.ROOFED_FOREST);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.ELITE, Biome.ROOFED_FOREST_MOUNTAINS);

	
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.BRUTAL, Biome.MEGA_SPRUCE_TAIGA_HILLS);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.BRUTAL, Biome.EXTREME_HILLS_MOUNTAINS);
		    setValidBiome(CustomMobType.YOBO, CustomMobLevel.BRUTAL, Biome.EXTREME_HILLS_PLUS_MOUNTAINS);
	}

	@Override
	public void onTarget(EntityTargetEvent event) {
		super.onTarget(event);
		
		if (event.getReason().equals(TargetReason.FORGOT_TARGET) ||
		    event.getReason().equals(TargetReason.TARGET_DIED)) {
			this.angry = false;
			for (Entity e : minions) {
				e.getBukkitEntity().remove();
			}
			
		}
		
	}

}
