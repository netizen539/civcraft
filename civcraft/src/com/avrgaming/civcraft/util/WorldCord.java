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

import org.bukkit.Location;

public class WorldCord {

	private String worldname;
	private int x;
	private int y;
	private int z;
	
	
	public WorldCord(String worldname, int x, int y, int z) {
		this.worldname = worldname;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	
	public WorldCord(Location location) {
		this.worldname = location.getWorld().getName();
		this.x = location.getBlockX();
		this.y = location.getBlockY();
		this.z = location.getBlockZ();
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
	public int getY() {
		return y;
	}
	public void setY(int y) {
		this.y = y;
	}
	public int getZ() {
		return z;
	}
	public void setZ(int z) {
		this.z = z;
	}
	
	
	
	
}
