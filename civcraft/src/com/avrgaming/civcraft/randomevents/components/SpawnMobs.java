package com.avrgaming.civcraft.randomevents.components;

import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.EntityType;

import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;
import com.avrgaming.civcraft.threading.TaskMaster;

public class SpawnMobs extends RandomEventComponent {

	@Override
	public void process() {
		class SyncTask implements Runnable {

			@Override
			public void run() {
				/* Spawn in mobs */
				EntityType type = EntityType.valueOf(getString("what"));
				
				/* Get amount. */
				int amount = Integer.valueOf(getString("amount"));
				
				/* Pick a random town chunk and spawn mobs there. */
				Random rand = new Random();
				int index = rand.nextInt(getParentTown().getTownChunks().size());
				
				TownChunk tc = (TownChunk)getParentTown().getTownChunks().toArray()[index];
				World world = Bukkit.getServer().getWorld(tc.getChunkCoord().getWorldname());
				
				for (int i = 0; i < amount; i++) {
					int x = rand.nextInt(16);
					int z = rand.nextInt(16);
					
					x += (tc.getChunkCoord().getX() * 16);
					z += (tc.getChunkCoord().getZ() * 16);
					
					int y = world.getHighestBlockAt(x, z).getY();
					Location loc = new Location(world, x, y, z);
					
					Bukkit.getServer().getWorld(tc.getChunkCoord().getWorldname()).spawnEntity(loc, type);
				}
				
				sendMessage(amount+" "+type.toString()+" have spawned in the vincitiy of "+
					(tc.getChunkCoord().getX()*16)+",64,"+(tc.getChunkCoord().getZ()*16));
			}
		}

		TaskMaster.syncTask(new SyncTask());
	}
	
	@Override
	public boolean requiresActivation() { return true;	}

}
