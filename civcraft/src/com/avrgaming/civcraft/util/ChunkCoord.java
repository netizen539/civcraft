/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.util;

import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;

public class ChunkCoord {
	
	private String worldname;
	private int x;
	private int z;
	
	//private static World[] worlds;
	//private static String[] worldnames;
	
	private static ConcurrentHashMap<String, World> worlds = new ConcurrentHashMap<String, World>();

	public static void addWorld(World world) {
		worlds.put(world.getName(), world);
	}
	
	public static void buildWorldList() {		
		for (World world : Bukkit.getWorlds()) {
			worlds.put(world.getName(), world);
		}	
	}
	
	public ChunkCoord(String worldname, int x, int z) {
		this.setWorldname(worldname);
		this.setX(x);
		this.setZ(z);
	}
	
	public ChunkCoord(Location location) {
		this.setFromLocation(location);
	}

	public ChunkCoord(Chunk c) {
		this.setWorldname(c.getWorld().getName());
		this.setX(c.getX());
		this.setZ(c.getZ());
	}

	public ChunkCoord(BlockCoord corner) {
		this.setFromLocation(corner.getLocation());
	}

	public ChunkCoord() {
	}

	public String getWorldname() {
		return worldname;
	}

	public void setWorldname(String worldname) {
		this.worldname = worldname;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		this.x = x;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		this.z = z;
	}
	
	@Override
	public String toString() {
		return this.worldname+","+x+","+z;
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof ChunkCoord) {
			ChunkCoord otherCoord = (ChunkCoord)other;
			if (otherCoord.worldname.equals(worldname)) {
				if ((otherCoord.getX()) == x && (otherCoord.getZ() == z)) {
					return true;
				}
			}
		}
		return false;
	}
	
	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	public static int castToChunkX(int blockx) {
		return castToChunk(blockx);
	}
	
	public static int castToChunkZ(int blockz) {
		return castToChunk(blockz);
	}

	public static int castToChunk(int i) {
		return (int) Math.floor((double)i / (double) 16);
	}
	
	public void setFromLocation(Location location) {
	//	this.worldname = "world";
//		for (int i = 0; i < worlds.length; i++) {
//			if (location.getWorld() == worlds[i]) {
//				this.worldname = worldnames[i];
//			}
//		}
		for (String name : worlds.keySet()) {
			World world = worlds.get(name);
			if (world == null) {
				continue;
			}
			
			if (world.equals(location.getWorld())) {
				this.worldname = name;
				break;
			}
		}
		
		
		this.x = castToChunkX(location.getBlockX());
		this.z = castToChunkZ(location.getBlockZ());
		
		//	this.setWorldname(location.getWorld().getName());
		
		// Note: We calculate the x and z rather than get the chunk
		// since getting the chunk would require a synchronous operation
		// on the main thread.
		/*CivLog.debug("x("+location.getX()+"): "+ (int)Math.round(location.getX() / CHUNKSIZE)+
				" z("+location.getZ()+"): "+(int)Math.round(location.getZ() / CHUNKSIZE));
		this.setX((int)Math.floor(location.getX()) / CHUNKSIZE);
		this.setZ((int)Math.floor(location.getZ()) / CHUNKSIZE);	*/
		//TODO try to do this without calling getchunk..
		//Chunk chunk = location.getChunk();
		//this.worldname = chunk.getWorld().getName();
		//this.x = chunk.getX();
		//this.z = chunk.getZ();
			
	}

	public int manhattanDistance(ChunkCoord chunkCoord) {
		return Math.abs(chunkCoord.x - this.x) + Math.abs(chunkCoord.z - this.z);
	}
	
	public double distance(ChunkCoord chunkCoord) {
		if (!chunkCoord.getWorldname().equals(this.getWorldname())) {
			return Double.MAX_VALUE;
		}
		
		double dist = Math.pow(this.getX() - chunkCoord.getX(), 2) + Math.pow(this.getZ()-chunkCoord.getZ(), 2);
		return Math.sqrt(dist);		
	}

	public Chunk getChunk() {
		return Bukkit.getWorld(this.worldname).getChunkAt(this.x, this.z);
	}
}
