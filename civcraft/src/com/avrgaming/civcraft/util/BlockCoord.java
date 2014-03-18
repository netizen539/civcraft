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

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.block.Block;

public class BlockCoord {
	
	private String worldname;
	private int x;
	private int y;
	private int z;
	
	private Location location = null;
	private boolean dirty = false;
	
	public BlockCoord(String worldname, int x, int y, int z) {
		this.setWorldname(worldname);
		this.setX(x);
		this.setY(y);
		this.setZ(z);
	}
	
	public BlockCoord(Location location) {
		this.setFromLocation(location);
	}

	public BlockCoord(String string) {
		String[] split = string.split(",");
		this.setWorldname(split[0]);
		this.setX(Integer.valueOf(split[1]));
		this.setY(Integer.valueOf(split[2]));
		this.setZ(Integer.valueOf(split[3]));
	}

	public BlockCoord(BlockCoord obj) {
		this.setX(obj.getX());
		this.setY(obj.getY());
		this.setZ(obj.getZ());
		this.setWorldname(obj.getWorldname());
	}

	public BlockCoord(Block block) {
		this.setX(block.getX());
		this.setY(block.getY());
		this.setZ(block.getZ());
		this.setWorldname(block.getWorld().getName());
	}

	public BlockCoord(SimpleBlock next) {
		this.setWorldname(next.worldname);
		this.setX(next.x);
		this.setY(next.y);
		this.setZ(next.z);
	}

	public BlockCoord() {
	}

	public BlockCoord(ChunkCoord nextChunk) {
		this.setWorldname(nextChunk.getWorldname());
		this.setX((nextChunk.getX() * 16)+8);
		this.setY(64);
		this.setZ((nextChunk.getZ() * 16)+8);
	}

	public void setFromLocation(Location location) {
		dirty = true;
		this.setWorldname(location.getWorld().getName());
		this.setX(location.getBlockX());
		this.setY(location.getBlockY());
		this.setZ(location.getBlockZ());
	}

	public String getWorldname() {
		return worldname;
	}

	public void setWorldname(String worldname) {
		dirty = true;
		this.worldname = worldname;
	}

	public int getX() {
		return x;
	}

	public void setX(int x) {
		dirty = true;
		this.x = x;
	}

	public int getY() {
		return y;
	}

	public void setY(int y) {
		dirty = true;
		this.y = y;
	}

	public int getZ() {
		return z;
	}

	public void setZ(int z) {
		dirty = true;
		this.z = z;
	}
	
	@Override
	public String toString() {
		return this.worldname+","+this.x+","+this.y+","+this.z;
	}

	@Override
	public int hashCode() {
		return this.toString().hashCode();
	}
	
	@Override
	public boolean equals(Object other) {
		if (other instanceof BlockCoord) {
			BlockCoord otherCoord = (BlockCoord)other;
			if (otherCoord.worldname.equals(worldname)) {
				if ((otherCoord.getX()) == x && (otherCoord.getY() == y) && 
						(otherCoord.getZ() == z)) {
					return true;
				}
			}
		}
		return false;
	}
	
	public Location getLocation() {
		if (location == null || dirty) {
			location = new Location(Bukkit.getWorld(this.worldname), x, y, z);
			dirty = false;
		}
		return location;
	}

	public Block getBlock() {
		return Bukkit.getWorld(this.worldname).getBlockAt(this.x, this.y, this.z);
	}

	public double distance(BlockCoord corner) {
		return Math.sqrt(distanceSquared(corner));
	}
	
	public double distanceXZ(BlockCoord corner) {
		return Math.sqrt(distanceXZSquared(corner));
	}
	
	public double distanceXZSquared(BlockCoord corner) {
		double distance = Double.MAX_VALUE;
		
		if (!corner.getWorldname().equals(this.worldname)) {
			return distance;
		}
		
		distance = Math.pow(corner.getX() - this.getX(), 2) + 
				   Math.pow(corner.getZ() - this.getZ(), 2);
		return distance;
	}
	
	public double distanceSquared(BlockCoord corner) {
		double distance = Double.MAX_VALUE;
		
		if (!corner.getWorldname().equals(this.worldname)) {
			return distance;
		}
		
		distance = Math.pow(corner.getX() - this.getX(), 2) + 
				   Math.pow(corner.getY() - this.getY(), 2) +
				   Math.pow(corner.getZ() - this.getZ(), 2);
		
		return distance;
	}

	public Location getCenteredLocation() {
		/* 
		 * Get a specialized location that is exactly centered in a single block.
		 * This prevents the respawn algorithm from detecting the location as "in a wall" 
		 * and searching upwards for a spawn point.
		 */
		Location loc = new Location(Bukkit.getWorld(this.worldname), (x+0.5), (y+0.5), (z+0.5));
		return loc;
	}
	
}
