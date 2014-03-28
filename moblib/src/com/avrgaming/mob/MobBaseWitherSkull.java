package com.avrgaming.mob;

import net.minecraft.server.v1_7_R2.EntityLiving;
import net.minecraft.server.v1_7_R2.EntityWitherSkull;
import net.minecraft.server.v1_7_R2.MovingObjectPosition;
import net.minecraft.server.v1_7_R2.World;

public class MobBaseWitherSkull extends EntityWitherSkull {

	public MobBaseWitherSkull(World world) {
		super(world);
	}

	public MobBaseWitherSkull(World world, EntityLiving entityliving, double d0, double d1, double d2) {
		super(world, entityliving, d0, d1, d2);
	}

	@Override
	protected void a(MovingObjectPosition movingobjectposition) {
		
	}

}
