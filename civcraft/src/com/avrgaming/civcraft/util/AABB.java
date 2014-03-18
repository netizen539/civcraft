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
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;

/*
 * This here is an Axis Aligned Bounding Box. We use this to determine if structures
 * may overlap each other during the build process. We may come up with other uses for it
 * as well such as checking for regions that a player is inside or something.
 * 
 * The position _must_ be the center of the box and the extents is the size/2 (aka the "radius") of the box.
 * 
 */

public class AABB {
	
	private Vector position = new Vector();
	private Vector extents = new Vector();
	
	public Vector getPosition() {
		return position;
	}
	
	public void setPosition(Vector position) {
		this.position = position;
	}
	
	public void setPosition(BlockCoord coord) {
		this.position.setX(coord.getX());
		this.position.setY(coord.getY());
		this.position.setZ(coord.getZ());
	}
	
	
	public Vector getExtents() {
		return extents;
	}
	
	public void setExtents(Vector extents) {
		this.extents = extents;
	}
	
	public void setExtents(BlockCoord coord) {
		this.extents.setX(coord.getX());
		this.extents.setY(coord.getY());
		this.extents.setZ(coord.getZ());
	}

	public void showDebugBlocks(int mat, int mat2) {
		try {
			Player dbgplayer = CivGlobal.getPlayer("netizen539");
			ItemManager.sendBlockChange(dbgplayer, new Location(Bukkit.getWorld("world"),
					this.getPosition().getX(), this.getPosition().getY(), this.getPosition().getZ()), mat, 0);
			ItemManager.sendBlockChange(dbgplayer, new Location(Bukkit.getWorld("world"), 
					this.getPosition().getX() + this.getExtents().getX(),
					this.getPosition().getY() + this.getExtents().getY(), 
					this.getPosition().getZ() + this.getExtents().getZ()),
					mat2, (byte)0);
		} catch (CivException e) {
			e.printStackTrace();
		}
	}
	
	public boolean overlaps(AABB other) {		
		if (other == null) {
			return false;
		}
		
		//this.showDebugBlocks(ItemManager.getId(Material.DIAMOND_BLOCK), ItemManager.getId(Material.DIAMOND_BLOCK));
		//other.showDebugBlocks(ItemManager.getId(Material.GOLD_BLOCK), ItemManager.getId(Material.GOLD_BLOCK));

		Vector t = new Vector();
		t.copy(other.getPosition());
		t.subtract(getPosition());
				
		return (Math.abs(t.getX()) < (getExtents().getX() + other.getExtents().getX()) &&
				Math.abs(t.getY()) < (getExtents().getY() + other.getExtents().getY()) &&
				Math.abs(t.getZ()) < (getExtents().getZ() + other.getExtents().getZ()));
	}
	
}
