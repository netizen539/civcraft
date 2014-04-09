package com.avrgaming.civcraft.siege;

import org.bukkit.World;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.BuildableDamageBlock;
import com.avrgaming.civcraft.object.Resident;

public class StructureBlockCannonHitEvent implements Runnable {
	Cannon cannon;
	Resident whoFired;
	BuildableDamageBlock dmgBlock;
	World world;
	
	public StructureBlockCannonHitEvent(Cannon cannon, Resident whoFired, World world, BuildableDamageBlock dmgBlock) {
		this.cannon = cannon;
		this.whoFired = whoFired;
		this.dmgBlock = dmgBlock;
		this.world = world;
	}
	
	@Override
	public void run() {
		CivLog.debug("structure cannon hit event.");


	}

}
