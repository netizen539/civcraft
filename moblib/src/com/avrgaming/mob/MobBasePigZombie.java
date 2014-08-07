package com.avrgaming.mob;


import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.craftbukkit.v1_7_R4.CraftWorld;
//import org.bukkit.craftbukkit.v1_7_R4.TrigMath;
import org.bukkit.craftbukkit.v1_7_R4.event.CraftEventFactory;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;

import com.avrgaming.nms.NMSUtil;

import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityHuman;
import net.minecraft.server.v1_7_R4.EntityPigZombie;
import net.minecraft.server.v1_7_R4.NBTTagCompound;
import net.minecraft.server.v1_7_R4.World;

public class MobBasePigZombie extends EntityPigZombie implements ISpawnable {
	public ICustomMob customMob = null;
	 
	// private static final UUID bq = UUID.fromString("49455A49-7EC5-45BA-B886-3B90B23A1718");
	 // private static final UUID bq = UUID.fromString("B9766B59-9566-4402-BC1F-2EE2A276D836");

	public MobBasePigZombie(World world) {
		super(world);
	}
	
	public MobBasePigZombie(World world, ICustomMob customMob) {
		super(world);
		NMSUtil.clearPathfinderGoals(this.goalSelector);
		NMSUtil.clearPathfinderGoals(this.targetSelector);
		this.customMob = customMob;
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
	
	
	@Override
	public boolean bk() {
		return false;
	}
	
	//@Override
	//public void h() {
	//	C();
	//	  if (!this.world.isStatic)
	//	    {
	//	      int i = aY();
	//	      if (i > 0)
	//	      {
	//	        if (this.aw <= 0) {
	//	          this.aw = (20 * (30 - i));
	//	        }
	//	        this.aw -= 1;
	//	        if (this.aw <= 0) {
	//	          p(i - 1);
	//	        }
	//	      }
	//	    }
	//	    e();
	//	    double d0 = this.locX - this.lastX;
	//	    double d1 = this.locZ - this.lastZ;
	//	    float f = (float)(d0 * d0 + d1 * d1);
	//	    float f1 = this.aN;
	//	    float f2 = 0.0F;
	//	    
	//	    this.aW = this.aX;
	//	    float f3 = 0.0F;
	//	    if (f > 0.0025F)
	//	    {
	//	      f3 = 1.0F;
	//	      f2 = (float)Math.sqrt(f) * 3.0F;
	//	      
	//	      f1 = (float)TrigMath.atan2(d1, d0) * 180.0F / 3.141593F - 90.0F;
	//	    }
	//	    if (this.aE > 0.0F) {
	//	      f1 = this.yaw;
	//	    }
	//	    if (!this.onGround) {
	//	      f3 = 0.0F;
	//	    }
	//	    this.aX += (f3 - this.aX) * 0.3F;
	//	    this.world.methodProfiler.a("headTurn");
	//	    f2 = f(f1, f2);
	//	    this.world.methodProfiler.b();
	//	    this.world.methodProfiler.a("rangeChecks");
	//	    while (this.yaw - this.lastYaw < -180.0F) {
	//	      this.lastYaw -= 360.0F;
	//	    }
	//	    while (this.yaw - this.lastYaw >= 180.0F) {
	//	      this.lastYaw += 360.0F;
	//	    }
	//	    while (this.aN - this.aO < -180.0F) {
	//	      this.aO -= 360.0F;
	//	    }
	//	    while (this.aN - this.aO >= 180.0F) {
	//	      this.aO += 360.0F;
	//	    }
	//	    while (this.pitch - this.lastPitch < -180.0F) {
	//	      this.lastPitch -= 360.0F;
	//	    }
	//	    while (this.pitch - this.lastPitch >= 180.0F) {
	//	      this.lastPitch += 360.0F;
	//	    }
	//	    while (this.aP - this.aQ < -180.0F) {
	//	      this.aQ -= 360.0F;
	//	    }
	//	    while (this.aP - this.aQ >= 180.0F) {
	//	      this.aQ += 360.0F;
	//	    }
	//	    this.world.methodProfiler.b();
	//	    this.aY += f2;
	//	
	//
	
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
	protected Entity findTarget() {
		EntityHuman entityhuman = this.world.findNearbyVulnerablePlayer(this, 3.0D);
		//this.n(arg0)
		return (entityhuman != null) && (hasLineOfSight(entityhuman)) ? entityhuman : null;
	}
	
	@Override
	protected void getRareDrop(int i) {
		return;
	}
	
	public static Entity spawnCustom(Location loc, ICustomMob iCustom) {
		CraftWorld world = (CraftWorld) loc.getWorld();
		World mcWorld = world.getHandle();
		MobBasePigZombie pigzombie = new MobBasePigZombie(mcWorld, iCustom);
		iCustom.setEntity(pigzombie);
		
		pigzombie.setPosition(loc.getX(), loc.getY(), loc.getZ());
		mcWorld.addEntity(pigzombie, SpawnReason.CUSTOM);
		
		return pigzombie;
	}
	
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
	public ICustomMob getCustomMobInterface() {
		return customMob;
	}
}
