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
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;

public class WaterStructure extends Structure {

	public static int WATER_LEVEL = 62;
	public static int TOLERANCE = 20;

	public WaterStructure(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	protected WaterStructure(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
	}
	
	@Override
	protected Location repositionCenter(Location center, String dir, double x_size, double z_size) {
		Location loc = new Location(center.getWorld(), 
				center.getX(), center.getY(), center.getZ(), 
				center.getYaw(), center.getPitch());
		
		// Reposition tile improvements
		if (this.isTileImprovement()) {
			// just put the center at 0,0 of this chunk?
			loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
			//loc = center.getChunk().getBlock(arg0, arg1, arg2)
		} else {
			if (dir.equalsIgnoreCase("east")) {
				loc.setZ(loc.getZ() - (z_size / 2));
				loc.setX(loc.getX() + SHIFT_OUT);
			}
			else if (dir.equalsIgnoreCase("west")) {
				loc.setZ(loc.getZ() - (z_size / 2));
				loc.setX(loc.getX() - (SHIFT_OUT+x_size));
	
			}
			else if (dir.equalsIgnoreCase("north")) {
				loc.setX(loc.getX() - (x_size / 2));
				loc.setZ(loc.getZ() - (SHIFT_OUT+z_size));
			}
			else if (dir.equalsIgnoreCase("south")) {
				loc.setX(loc.getX() - (x_size / 2));
				loc.setZ(loc.getZ() + SHIFT_OUT);
	
			}
		}
		
		if (this.getTemplateYShift() != 0) {
			// Y-Shift based on the config, this allows templates to be built underground.
			loc.setY(WATER_LEVEL + this.getTemplateYShift());
		}
	
		return loc;
	}

	@Override
	protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ, Location savedLocation) throws CivException {
		super.checkBlockPermissionsAndRestrictions(player, centerBlock, regionX, regionY, regionZ, savedLocation);
		
		if ((player.getLocation().getBlockY() - WATER_LEVEL) > TOLERANCE) {
			throw new CivException("You must be close to the water's surface to build this structure.");
		}
		
	}
	
	@Override
	public String getMarkerIconName() {
		return "anchor";
	}
}
