package com.avrgaming.civcraft.mobs;

import net.minecraft.server.v1_7_R4.EntityCreature;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.EntityInsentient;
import net.minecraft.server.v1_7_R4.PathfinderGoalLookAtPlayer;
import net.minecraft.server.v1_7_R4.PathfinderGoalMeleeAttack;
import net.minecraft.server.v1_7_R4.PathfinderGoalNearestAttackableTarget;
import net.minecraft.server.v1_7_R4.PathfinderGoalRandomStroll;

import org.bukkit.block.Biome;

import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobLevel;
import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobType;
import com.avrgaming.civcraft.mobs.components.MobComponentDefense;
import com.avrgaming.mob.ICustomMob;
import com.avrgaming.mob.MobBaseIronGolem;

public class Behemoth extends CommonCustomMob implements ICustomMob {

	public void onCreate() {
	    initLevelAndType();

		getGoalSelector().a(7, new PathfinderGoalRandomStroll((EntityCreature) entity, 1.0D));
		getGoalSelector().a(8, new PathfinderGoalLookAtPlayer((EntityInsentient) entity, EntityHuman.class, 8.0F));
	    getGoalSelector().a(2, new PathfinderGoalMeleeAttack((EntityCreature) entity, EntityHuman.class, 1.0D, false));
	    getTargetSelector().a(2, new PathfinderGoalNearestAttackableTarget((EntityCreature) entity, EntityHuman.class, 0, true));

	    this.setName(this.getLevel().getName()+" "+this.getType().getName());
	}

	public void onCreateAttributes() {
		MobComponentDefense defense;
	    this.setKnockbackResistance(1.0D);
	    this.setMovementSpeed(0.15);

		switch (this.getLevel()) {
		case LESSER:
		    defense = new MobComponentDefense(3.5);
		    setMaxHealth(75);
		    modifySpeed(1.3);
		    this.addDrop("mat_ionic_crystal_fragment_1", 0.05);
		    
		    this.addDrop("mat_forged_clay", 0.1);
		    this.addDrop("mat_crafted_reeds", 0.1);
		    this.addDrop("mat_crafted_sticks", 0.1);
		    this.coinDrop(1, 25);
			break;
		case GREATER:
		    defense = new MobComponentDefense(10);
		    setMaxHealth(125.0);
		    modifySpeed(1.4);
		    this.addDrop("mat_ionic_crystal_fragment_2", 0.05);

		    this.addDrop("mat_steel_plate", 0.1);
		    this.addDrop("mat_steel_ingot", 0.05);
		    this.addDrop("mat_clay_molding", 0.05);

		    this.addDrop("mat_varnish", 0.01);
		    this.addDrop("mat_sticky_resin", 0.05);
		    this.coinDrop(10, 50);
		    break;
		case ELITE:
		    defense = new MobComponentDefense(16);
		    setMaxHealth(150.0);
		    modifySpeed(1.5);
		    this.addDrop("mat_ionic_crystal_fragment_3", 0.05);

		    this.addDrop("mat_carbide_steel_plate", 0.1);
		    this.addDrop("mat_carbide_steel_ingot", 0.05);
		    this.addDrop("mat_clay_molding", 0.05);

		    this.addDrop("mat_sticky_resin", 0.1);
		    this.addDrop("mat_smithy_resin", 0.05);
		    this.coinDrop(20, 80);
			break;
		case BRUTAL:
		    this.addDrop("mat_ionic_crystal_fragment_4", 0.05);
		    
		    this.addDrop("mat_tungsten_plate", 0.1);
		    this.addDrop("mat_tungsten_ingot", 0.05);
		    this.addDrop("mat_clay_tungsten_casting", 0.05);
		    
		    this.addDrop("mat_sticky_resin", 0.1);
		    this.addDrop("mat_smithy_resin", 0.05);

		    defense = new MobComponentDefense(20);
		    setMaxHealth(160.0);
		    modifySpeed(1.6);
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
		return MobBaseIronGolem.class.getName();
	}

	@Override
	public String getClassName() {
		return Behemoth.class.getName();
	}

	public static void register() {
		
		    setValidBiome(CustomMobType.BEHEMOTH, CustomMobLevel.LESSER, Biome.FROZEN_RIVER);
		    setValidBiome(CustomMobType.BEHEMOTH, CustomMobLevel.LESSER, Biome.FROZEN_OCEAN);
		    setValidBiome(CustomMobType.BEHEMOTH, CustomMobLevel.LESSER, Biome.COLD_BEACH);
		    setValidBiome(CustomMobType.BEHEMOTH, CustomMobLevel.LESSER, Biome.COLD_TAIGA);
	
		    setValidBiome(CustomMobType.BEHEMOTH, CustomMobLevel.GREATER, Biome.COLD_TAIGA_HILLS);
		    setValidBiome(CustomMobType.BEHEMOTH, CustomMobLevel.GREATER, Biome.COLD_TAIGA_MOUNTAINS);
		
		    setValidBiome(CustomMobType.BEHEMOTH, CustomMobLevel.ELITE, Biome.ICE_PLAINS);
		    setValidBiome(CustomMobType.BEHEMOTH, CustomMobLevel.GREATER, Biome.ICE_MOUNTAINS);

		    setValidBiome(CustomMobType.BEHEMOTH, CustomMobLevel.BRUTAL, Biome.ICE_PLAINS_SPIKES);
	}
	
}
