package com.avrgaming.civcraft.mobs;

import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobLevel;
import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobType;

public class TypeLevel {
	public CustomMobType type;
	public CustomMobLevel level;

	public TypeLevel(CustomMobType type, CustomMobLevel level) {
		this.type = type;
		this.level = level;
	}
}
