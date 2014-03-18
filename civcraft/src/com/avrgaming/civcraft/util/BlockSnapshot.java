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

import org.bukkit.ChunkSnapshot;

import com.avrgaming.civcraft.exception.InvalidBlockLocation;

public class BlockSnapshot {

	private int x;
	private int y;
	private int z;
	private int typeId;
	private int data;
	private ChunkSnapshot snapshot;
	
	public BlockSnapshot(int x, int y, int z, ChunkSnapshot snapshot) {
		this.setFromSnapshotLocation(x, y, z, snapshot);
	}

	
	public BlockSnapshot() {
		//Used when caching.
	}

	public void setFromSnapshotLocation(int x, int y, int z, ChunkSnapshot snapshot) {
		/* Modulo in Java doesn't handle negative numbers the way we want it to, compensate here. */
		if (x < 0) {
			x += 16;
		}
		
		if (z < 0) {
			z += 16;
		}
				
		this.setX(x);
		this.setY(y);
		this.setZ(z);
		this.setSnapshot(snapshot);
		this.setTypeId(ItemManager.getBlockTypeId(snapshot, this.x, this.y, this.z));
		this.setData(ItemManager.getBlockData(snapshot, this.x, this.y, this.z));
	}

	public BlockSnapshot getRelative(int xOff, int yOff, int zOff) throws InvalidBlockLocation {
		int nX = this.getX() + xOff;
		if (nX < 0 || nX > 15) {
			throw new InvalidBlockLocation();
		}
		
		BlockSnapshot relative = new BlockSnapshot(this.getX() + xOff, this.getY() + yOff, this.getZ() + zOff, snapshot);		
		return relative;
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

	public int getTypeId() {
		return typeId;
	}

	public void setTypeId(int typeId) {
		this.typeId = typeId;
	}

	public int getData() {
		return data;
	}

	public void setData(int data) {
		this.data = data;
	}

	public ChunkSnapshot getSnapshot() {
		return snapshot;
	}

	public void setSnapshot(ChunkSnapshot snapshot) {
		this.snapshot = snapshot;
	}
	
	
}
