package com.avrgaming.mob;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.avrgaming.nms.NMSUtil;

import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityLiving;
import net.minecraft.server.v1_7_R4.EntityWither;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.World;

public class MobBaseWither extends EntityWither implements ISpawnable {
	public ICustomMob customMob = null;

	public MobBaseWither(World world) {
		super(world);
	}
	
	public MobBaseWither(World world, ICustomMob customMob) {
		super(world);
		this.customMob = customMob;
		NMSUtil.clearPathfinderGoals(this.goalSelector);
		NMSUtil.clearPathfinderGoals(this.targetSelector);
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
		
		try {
			String className = compound.getString("customMobClass");
			Class<?> customClass = Class.forName(className);
			this.customMob = (ICustomMob)customClass.newInstance();
			this.customMob.loadSaveString(compound.getString("customMobData"));
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void a(EntityLiving entityliving, float f)
	{
	
	}
	
	@Override
	public void e() {
	}

	
	public static Entity spawnCustom(Location loc, ICustomMob iCustom) {
		CraftWorld world = (CraftWorld) loc.getWorld();
		World mcWorld = world.getHandle();
		MobBaseWither pigzombie = new MobBaseWither(mcWorld, iCustom);
		iCustom.setEntity(pigzombie);
		
		pigzombie.setPosition(loc.getX(), loc.getY(), loc.getZ());
		mcWorld.addEntity(pigzombie, SpawnReason.CUSTOM);
		
		return pigzombie;
	}

	@Override
	public ICustomMob getCustomMobInterface() {
		return customMob;
	}

}
