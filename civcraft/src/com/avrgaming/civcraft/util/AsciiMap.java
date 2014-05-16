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

import java.util.ArrayList;
import java.util.List;

import org.bukkit.Location;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.TownChunk;

public class AsciiMap {

	private static final int width = 9;
	private static final int height = 40;	
	
	public static List<String> getMapAsString(Location center) {
		ArrayList<String> out = new ArrayList<String>();
		
	//	ChunkCoord[][] chunkmap = new ChunkCoord[width][height]; 
		ChunkCoord centerChunk = new ChunkCoord(center);
		
		/* Use the center to build a starting point. */
		ChunkCoord currentChunk = new ChunkCoord(center.getWorld().getName(),
											(centerChunk.getX() - (width/2)),
											(centerChunk.getZ() - (height/2)));
		
		int startX = currentChunk.getX();
		int startZ = currentChunk.getZ();
	
		out.add(CivMessage.buildTitle("Map"));
		
		//ChunkCoord currentChunk = new ChunkCoord(center);
		for (int x = 0; x < width; x++) {
			String outRow = new String("         ");
			for (int z = 0; z < height; z++) {
				String color = CivColor.White;
								
				currentChunk = new ChunkCoord(center.getWorld().getName(), 
						startX+x, startZ+z);
				
				if (currentChunk.equals(centerChunk)) {
					color = CivColor.Yellow;
				}
				
				/* Try to see if there is a town chunk here.. */
				TownChunk tc = CivGlobal.getTownChunk(currentChunk);
				if (tc != null) {
					
					if (color.equals(CivColor.White)) {
						if (tc.perms.getOwner() != null) {
							color = CivColor.LightGreen;
						} else {
							color = CivColor.Rose;
						}
					}
					
					if (tc.isForSale()) {
						outRow += CivColor.Yellow+"$";
					} else if (tc.isOutpost()) { 
						outRow += CivColor.Yellow+"O";
					} else {
						outRow += color+"T";
					}
				} else {
					outRow += color+"-";
				}
			}
			out.add(outRow);
		}
		
		
		return out;
	}
	
}
