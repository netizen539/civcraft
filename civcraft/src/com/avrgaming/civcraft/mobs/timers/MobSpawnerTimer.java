package com.avrgaming.civcraft.mobs.timers;

import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import net.minecraft.server.v1_7_R4.EntityCreature;

import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.block.BlockFace;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.mobs.MobSpawner;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.EntityProximity;
import com.avrgaming.civcraft.util.ItemManager;

public class MobSpawnerTimer implements Runnable {
	
	public static int UPDATE_LIMIT = 40;
	public static int MOB_AREA_LIMIT = 5;
	public static int MOB_AREA = 32;
	
	public static int MIN_SPAWN_DISTANCE = 20;
	public static int MAX_SPAWN_DISTANCE = 50;
	public static int MIN_SPAWN_AMOUNT = 5;
	
	public static int Y_SHIFT = 3;
	
	public static Queue<String> playerQueue = new LinkedList<String>();

	@Override
	public void run() {
		String name = null;
		
		for (int i = 0; i < UPDATE_LIMIT; i++) {
			/* Find a player who is out in the wilderness. */
			try {
				name = playerQueue.poll();
				if (name == null) {
					/* Queue empty, return. */
					return;
				}
				
				Player player = CivGlobal.getPlayer(name);
				World world = player.getWorld();
				if (!world.getAllowMonsters()) {
					continue;
				}
								
				for (int j = 0; j < MIN_SPAWN_AMOUNT; j++) {
					Random random = new Random();
					int x = random.nextInt(MAX_SPAWN_DISTANCE)+MIN_SPAWN_DISTANCE;
					if (random.nextBoolean()) {
						x *= -1;
					}
					
					int z = random.nextInt(MAX_SPAWN_DISTANCE)+MIN_SPAWN_DISTANCE;
					if (random.nextBoolean()) {
						z *= -1;
					}
					
					int y = world.getHighestBlockYAt(((Double) player.getLocation().getX()).intValue() + x, ((Double) player.getLocation().getZ()).intValue() + z);
				    Location loc = new Location(world, player.getLocation().getX() + x, y, player.getLocation().getZ() + z);
					if (!loc.getChunk().isLoaded()) {
						continue;
					}
					
					
					TownChunk tc = CivGlobal.getTownChunk(new ChunkCoord(loc));
					if (tc != null) {
						/* Dont spawn in towns. */
						continue;
					}
					
					if ((ItemManager.getId(loc.getBlock().getRelative(BlockFace.DOWN)) == CivData.WATER) ||
					    (ItemManager.getId(loc.getBlock().getRelative(BlockFace.DOWN)) == CivData.WATER_RUNNING) ||
						(ItemManager.getId(loc.getBlock().getRelative(BlockFace.DOWN)) == CivData.LAVA) ||
						(ItemManager.getId(loc.getBlock().getRelative(BlockFace.DOWN)) == CivData.LAVA_RUNNING)) {
						/* Dont spawn mobs in water. */
						continue;
					}

					loc.setY(loc.getY()+Y_SHIFT);
					LinkedList<Entity> entities = EntityProximity.getNearbyEntities(null, loc, MOB_AREA, EntityCreature.class);
					if (entities.size() > MOB_AREA_LIMIT) {
						/* Dont spawn if we've reach the mob limit. */
						continue;
					}
					
					MobSpawner.spawnRandomCustomMob(loc);
				}
				break;
			} catch (CivException e) {
				/* player is offline, don't re-add to queue. */
			} finally {
				if (name != null) {
					/* Re-add to end of queue. */
					playerQueue.add(name);
				}
			}
		}
	}

}
