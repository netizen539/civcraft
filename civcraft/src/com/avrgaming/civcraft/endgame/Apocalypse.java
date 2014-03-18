package com.avrgaming.civcraft.endgame;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.mobs.MobSpawner;
import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobLevel;
import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobType;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.util.ChunkCoord;

public class Apocalypse implements Runnable {

	@Override
	public void run() {
		
		if (!CivGlobal.endWorld) {
			return;
		}
		
		for (int i = 0; i < 40; i++) {
			Random rand = new Random();
			int ran = rand.nextInt(Bukkit.getOnlinePlayers().length);
			
			Player player = Bukkit.getOnlinePlayers()[ran];

			CultureChunk cc = CivGlobal.getCultureChunk(new ChunkCoord(player.getLocation()));
			if (cc != null) {
				continue;
			}
			
			Location newLoc = player.getLocation().add(rand.nextInt(20)+20, 0, rand.nextInt(20)+20);
			
			try {
				MobSpawner.spawnCustomMob(CustomMobType.YOBOBOSS, CustomMobLevel.BRUTAL, newLoc);
			} catch (CivException e) {
				e.printStackTrace();
			}
			break;
		}
		
	}

	
	
}
