package com.avrgaming.civcraft.arena;


import com.avrgaming.civcraft.config.ConfigArena;

public class Arena {
	public ConfigArena config;
	public int instanceID;
	
	public static int nextInstanceID = 0;
	
	public Arena(ConfigArena a) {
		this.config = a;
		this.instanceID = nextInstanceID;
		nextInstanceID++;
	}
	
	
}
