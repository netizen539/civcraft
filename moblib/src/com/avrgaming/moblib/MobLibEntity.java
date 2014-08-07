package com.avrgaming.moblib;

import java.util.UUID;

import net.minecraft.server.v1_7_R4.Entity;

public class MobLibEntity {

	private UUID uid;
	private Entity entity;

	public MobLibEntity(UUID uid, Entity entity) {
		this.uid = uid;
		this.entity = entity;
	}
	
	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public UUID getUid() {
		return uid;
	}

	public void setUid(UUID uid) {
		this.uid = uid;
	}
	
}
