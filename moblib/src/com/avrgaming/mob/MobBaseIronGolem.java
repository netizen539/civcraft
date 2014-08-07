package com.avrgaming.mob;

import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.avrgaming.nms.NMSUtil;

import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityIronGolem;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.World;

public class MobBaseIronGolem extends EntityIronGolem implements ISpawnable {
	public ICustomMob customMob = null;

	public MobBaseIronGolem(World world) {
		super(world);

	}
	
	public MobBaseIronGolem(World world, ICustomMob custom) {
		super(world);
		NMSUtil.clearPathfinderGoals(this.goalSelector);
		NMSUtil.clearPathfinderGoals(this.targetSelector);
		this.customMob = custom;
	}

	@Override
	public void o(Entity entity) {
		//Do nothing, don't target monsters.
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
	
	public static Entity spawnCustom(Location loc, ICustomMob iCustom) {
		CraftWorld world = (CraftWorld) loc.getWorld();
		World mcWorld = world.getHandle();
		MobBaseIronGolem pigzombie = new MobBaseIronGolem(mcWorld, iCustom);
		iCustom.setEntity(pigzombie);
		
		pigzombie.setPosition(loc.getX(), loc.getY(), loc.getZ());
		mcWorld.addEntity(pigzombie, SpawnReason.CUSTOM);
		
		return pigzombie;
	}
	
	@Override
	public boolean damageEntity(DamageSource damagesource, float f) {
		try {
		if (!super.damageEntity(damagesource, f)) {
			return false;
		}
		
		if (customMob != null) {
			customMob.onDamage(this, damagesource, 
					this.goalSelector, this.targetSelector);
		}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return true;
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
	protected void getRareDrop(int i) {
		return;
	}
	
	@Override
	public ICustomMob getCustomMobInterface() {
		return customMob;
	}
	
}
