package com.avrgaming.mob;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import com.avrgaming.nms.NMSUtil;
import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityZombie;
import net.minecraft.server.v1_7_R4.Item;
import net.minecraft.server.v1_7_R4.NBTTagCompound;

import net.minecraft.server.v1_7_R4.World;

public class MobBaseZombie extends EntityZombie implements ISpawnable {
	
	public ICustomMob customMob = null;
	
	public MobBaseZombie(World world) {
		super(world);
	}
	
	public MobBaseZombie(World world, ICustomMob custom) {
		super(world);
		NMSUtil.clearPathfinderGoals(this.goalSelector);
		NMSUtil.clearPathfinderGoals(this.targetSelector);
		customMob = custom;
	}
		
	/* Setting and loading custom NBT data. */
	@Override
	public void b(NBTTagCompound compound) {
		super.b(compound);
		compound.setString("customMobClass", this.customMob.getClassName());
		compound.setString("customMobData", this.customMob.getSaveString());
	}
	
	@Override
	public void a(NBTTagCompound compound) {
		super.a(compound);
		
		if (!compound.hasKey("customMobClass")) {
			System.out.println("NO CUSTOM CLASS FOUND REMOVING ENTITY.");
			this.world.removeEntity(this);
			return;
		}
		
		try {
			String className = compound.getString("customMobClass");
			Class<?> customClass = Class.forName(className);
			this.customMob = (ICustomMob)customClass.newInstance();
			this.customMob.loadSaveString(compound.getString("customMobData"));
			} catch (Exception e) {
			this.world.removeEntity(this);
			e.printStackTrace();
		}
	}
	
	/* Do not drop items. */
	@Override
	protected Item getLoot() {
		return null;
	}
	
	@Override
	protected void getRareDrop(int i) {
		return;
	}
	
	@Override
	protected void dropDeathLoot(boolean flag, int i) {
		try {
		if (customMob != null) {
			customMob.onDeath(this);
	        CraftEventFactory.callEntityDeathEvent(this, new ArrayList<org.bukkit.inventory.ItemStack>());
		}
		} catch (Exception e) {
			e.printStackTrace();
		}		
	}
	
	@Override
	public boolean damageEntity(DamageSource damagesource, float f) {
		try {
		if (!super.damageEntity(damagesource, f)) {
			return false;
		}
		
		if (customMob != null) {
			customMob.onDamage(this, 
					damagesource, this.goalSelector, this.targetSelector);
		}
		} catch (Exception e) {
			e.printStackTrace();
			return false;
		}
		
		return true;
		
	}
	
	/* Try to prevent fire ticks.. */
	@Override
	public void e() {
		try {
		super.e();
		if (customMob != null) {
			customMob.onTick();	
		} else {
			System.out.println("Ticking without custom  Mob..");
			this.world.removeEntity(this);
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	public static Entity spawnCustom(Location loc, ICustomMob iCustom) {
		CraftWorld world = (CraftWorld) loc.getWorld();
		World mcWorld = world.getHandle();
		MobBaseZombie zombie = new MobBaseZombie(mcWorld, iCustom);
		iCustom.setEntity(zombie);

		zombie.setPosition(loc.getX(), loc.getY(), loc.getZ());
		mcWorld.addEntity(zombie, SpawnReason.CUSTOM);
		
		return zombie;
	}
	
	public static Entity spawn(Location loc, ICustomMob iCustom, String name) {
		CraftWorld world = (CraftWorld) loc.getWorld();
		World mcWorld = world.getHandle();
		MobBaseZombie zombie = new MobBaseZombie(mcWorld, iCustom);
		
		if (name != null) {
			zombie.setCustomName(name);
			zombie.setCustomNameVisible(true);
		}
		iCustom.setEntity(zombie);
		
		zombie.setPosition(loc.getX(), loc.getY(), loc.getZ());
		mcWorld.addEntity(zombie, SpawnReason.CUSTOM);
		
		return zombie;
	}

	@Override
	public ICustomMob getCustomMobInterface() {
		return customMob;
	}

}
