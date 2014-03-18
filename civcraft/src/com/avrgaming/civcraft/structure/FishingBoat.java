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
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.StructureBlock;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.util.ItemManager;

public class FishingBoat extends TradeOutpost {

	/*
	 * Fishing boats extend trade outposts, so we only need to
	 * override methods that are relevant to the construction of the
	 * goodie's tower.
	 */
	public static int WATER_LEVEL = 62;
	public static int TOLERANCE = 20;
	
	protected FishingBoat(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
	}

	protected FishingBoat(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}
	
	@Override
	public String getMarkerIconName() {
		return "anchor";
	}
	
	@Override
	public void build_trade_outpost(Location centerLoc) throws CivException {
		
		/* Add trade good to town. */
		TradeGood good = CivGlobal.getTradeGood(tradeGoodCoord);
		if (good == null) {
			throw new CivException("Couldn't find trade good at location:"+good);
		}
		
		if (!good.getInfo().water) {
			throw new CivException("Fishing boats can only be built on water goods.");
		}
		
		if (good.getTown() != null) {
			throw new CivException("Good is already claimed.");
		}
		
		good.setStruct(this);
		good.setTown(this.getTown());
		good.setCiv(this.getTown().getCiv());
		/* Save the good *afterwards* so the structure id is properly set. */
		this.setGood(good);
	}
	
	@Override
	public void build_trade_outpost_tower() throws CivException {
		/* Add trade good to town. */
		
		/* this.good is set by the good's load function or by the onBuild function. */
		TradeGood good = this.good;
		if (good == null) {
			throw new CivException("Couldn't find trade good at location:"+good);
		}
		
		/* Build the 'trade good tower' */
		/* This is always set on post build using the post build sync task. */
		if (tradeOutpostTower == null) {
			throw new CivException("Couldn't find trade outpost tower.");
		}
		
		Location centerLoc = tradeOutpostTower.getLocation();
		
		/* Build the bedrock tower. */
		for (int i = 0; i < 3; i++) {
			Block b = centerLoc.getBlock().getRelative(0, i, 0);
			ItemManager.setTypeId(b, CivData.BEDROCK); ItemManager.setData(b, 0);
			
			StructureBlock sb = new StructureBlock(new BlockCoord(b), this);
			this.addStructureBlock(sb.getCoord(), false);
			//CivGlobal.addStructureBlock(sb.getCoord(), this);
		}
		
		/* Place the sign. */
		Block b = centerLoc.getBlock().getRelative(1, 2, 0);
		ItemManager.setTypeId(b, CivData.WALL_SIGN); 
		ItemManager.setData(b, CivData.DATA_SIGN_EAST);
		Sign s = (Sign)b.getState();
		s.setLine(0, good.getInfo().name);
		s.update();
		StructureBlock sb = new StructureBlock(new BlockCoord(b), this);
		//CivGlobal.addStructureBlock(sb.getCoord(), this);
		this.addStructureBlock(sb.getCoord(), false);
		
		/* Place the itemframe. */
		b = centerLoc.getBlock().getRelative(0,1,0);
		this.addStructureBlock(new BlockCoord(b), false);
		Block b2 = b.getRelative(1, 0, 0);
		Entity entity = CivGlobal.getEntityAtLocation(b2.getLocation());
		
		if (entity == null || (!(entity instanceof ItemFrame))) {
			this.frameStore = new ItemFrameStorage(b.getLocation(), BlockFace.EAST);	
		} else {
			this.frameStore = new ItemFrameStorage((ItemFrame)entity, b.getLocation());
		}
		
		this.frameStore.setBuildable(this);
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
	public void onLoad() throws CivException {
		super.createTradeGood();
	}
	
	@Override
	protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ, Location savedLocation) throws CivException {
		super.checkBlockPermissionsAndRestrictions(player, centerBlock, regionX, regionY, regionZ, savedLocation);
		
		if ((player.getLocation().getBlockY() - WATER_LEVEL) > TOLERANCE) {
			throw new CivException("You must be close to the water's surface to build this structure.");
		}
		
	}
	
}
