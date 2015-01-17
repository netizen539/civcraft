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

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.listener.MarkerPlacementManager;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.StructureBlock;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.object.WallBlock;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.war.War;

public class Wall extends Structure {

	//TODO make these configurable.
	private static final double MAX_SEGMENT = 300;
	private static final int RECURSION_LIMIT = 350;

	private static int HEIGHT;
	private static int MAX_HEIGHT;
	private static double COST_PER_SEGMENT;
	
	public static void init_settings() throws InvalidConfiguration {
		HEIGHT = CivSettings.getInteger(CivSettings.warConfig, "wall.height");
		MAX_HEIGHT = CivSettings.getInteger(CivSettings.warConfig, "wall.maximum_height");
		COST_PER_SEGMENT = CivSettings.getDouble(CivSettings.warConfig, "wall.cost_per_segment");
	}
	
	public Map<BlockCoord, WallBlock> wallBlocks = new HashMap<BlockCoord, WallBlock>();
	public HashSet<ChunkCoord> wallChunks = new HashSet<ChunkCoord>();
	
	/*
	 *  This is used to chain together the wall chunks built by the last operation. 
	 * this allows us to undo all of the walls built in a single pass.
	 */
	private Wall nextWallBuilt = null;
	
//	private int verticalsegments = 0;
	
//	private HashMap<String, SimpleBlock> simpleBlocks = new HashMap<String, SimpleBlock>();
	
	protected Wall(Location center, String id, Town town) throws CivException {
		super(center, id, town);
	}

	public Wall(ResultSet rs) throws SQLException, CivException {
		super(rs);
		this.hitpoints = this.getMaxHitPoints();
	}

	@Override
	public void bindStructureBlocks() {
	}
	
	@Override
	public boolean hasTemplate() {
		return false;
	}
	
	@Override
	public boolean canRestoreFromTemplate() {
		return false;
	}
	
	@Override
	public void processUndo() throws CivException {
		
		double refund = 0.0;
		for (WallBlock wb : wallBlocks.values()) {
			
			Material material = ItemManager.getMaterial(wb.getOldId());
			if (CivSettings.restrictedUndoBlocks.contains(material)) {
				continue;
			}
			
			ItemManager.setTypeId(wb.getCoord().getBlock(), wb.getOldId());
			ItemManager.setData(wb.getCoord().getBlock(), wb.getOldData());
			refund += COST_PER_SEGMENT;
			try {
				wb.delete();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		refund /= HEIGHT;
		refund = Math.round(refund);
		this.getTown().getTreasury().deposit(refund);
		CivMessage.sendTown(this.getTown(), CivColor.Yellow+"Refunded "+refund+" coins from wall construction.");
		try {
			this.delete();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	@Override
	public void unbindStructureBlocks() {
		super.unbindStructureBlocks();
	}
	
	@Override
	protected Location repositionCenter(Location center, String dir, double x_size, double z_size)  {
		return center;
	}
	
	@Override
	public void resumeBuildFromTemplate() throws Exception
	{
	}
	
	@Override 
	public void delete() throws SQLException {
		if (this.wallBlocks != null) {
			for (WallBlock wb : this.wallBlocks.values()) {
				wb.delete();
			}
		}
		
		if (wallChunks != null) {
			for (ChunkCoord coord : wallChunks) {
				CivGlobal.removeWallChunk(this, coord);
			}
		}
		
		super.delete();
	}
	
	@Override
	public void undoFromTemplate() {
		
		if (this.nextWallBuilt == null) {
			for (BlockCoord coord : wallBlocks.keySet()) {
				WallBlock wb = wallBlocks.get(coord);
				ItemManager.setTypeId(coord.getBlock(), wb.getOldId());
				ItemManager.setData(coord.getBlock(), wb.getOldData());
				try {
					wb.delete();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			// Remove this wall chunk.
			ChunkCoord coord = new ChunkCoord(this.getCorner());
			CivGlobal.removeWallChunk(this, coord);
		} else {
			try {
				this.nextWallBuilt.processUndo();
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ, Location savedLocation) throws CivException {
	}
	
	@Override
	public synchronized void buildRepairTemplate(Template tpl, Block centerBlock) {
	}
	
	@Override
	public void buildPlayerPreview(Player player, Location centerLoc) throws CivException, IOException {
		// Set the player into "place mode" which allows them to place down
		// markers.
		if (!this.getTown().hasTechnology(this.getRequiredTechnology())) {
			throw new CivException("We don't have the technology yet.");
		}
		
		if (War.isWarTime()) {
			throw new CivException("Cannot build walls during WarTime.");
		}
		
		MarkerPlacementManager.addToPlacementMode(player, this, "Wall Marker");		
	}
	
	@Override
	public void build(Player player, Location centerLoc, Template tpl) throws Exception {		
//		// Set the player into "place mode" which allows them to place down
//		// markers.
//		//XXX never called anymore??
//		MarkerPlacementManager.addToPlacementMode(player, this, "Wall Marker");		
	}
	
	
	private boolean isValidWall() {	
		for (WallBlock block : this.wallBlocks.values()) {
			BlockCoord bcoord = new BlockCoord(block.getCoord());
			
			for (int y = 0; y < 256; y++) {
				bcoord.setY(y);
				
				StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
				if (sb != null) {
				    if (sb.getOwner() != this) {
						return false;
					}
				}
			}
		}
		
		return true;
	}
	
	@Override
	public boolean showOnDynmap() {
		return false;
	}
	
	@Override
	public void onMarkerPlacement(Player player, Location next, ArrayList<Location> locs) throws CivException {		
		BlockCoord first = new BlockCoord(next);
		BlockCoord second = null;
		
		CultureChunk cc = CivGlobal.getCultureChunk(next);
		if (cc == null || cc.getTown().getCiv() != this.getTown().getCiv()) {
			throw new CivException("Cannot build here, you need to build inside your culture.");
		}
		
		if (locs.size() <= 1) {
			CivMessage.send(player, CivColor.LightGray+"First location placed, place another to start building a wall.");
			return;
		}
		
		// Validate our locations
		if (locs.get(0).distance(locs.get(1)) > Wall.MAX_SEGMENT) {
			throw new CivException("Can only build a wall in "+Wall.MAX_SEGMENT+" block segments, pick a closer location");
		}
		
		
		second = new BlockCoord(locs.get(0));
		locs.clear();
		MarkerPlacementManager.removeFromPlacementMode(player, false);
		
		
		Location secondLoc = second.getLocation();
		// Setting to a new block coord so we can increment in buildWallSegment without changing the corner.
		this.setCorner(new BlockCoord(secondLoc));
		this.setComplete(true);
		this.save();
			
		// We should now be able to draw a line between these two block points.
		HashMap<String, SimpleBlock> simpleBlocks = new HashMap<String, SimpleBlock>();
		int verticalSegments = this.buildWallSegment(player, first, second, 0, simpleBlocks, 0);
		
		// Pay the piper
		double cost = verticalSegments*COST_PER_SEGMENT;
		if (!this.getTown().getTreasury().hasEnough(cost)) {
			
			for (WallBlock wb : this.wallBlocks.values()) {
				try {
					wb.delete();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			this.wallBlocks.clear();
			
			throw new CivException("Cannot build, not enough coins to pay "+cost+" coins for wall of length "+verticalSegments+" blocks.");
		}
		
		this.getTown().getTreasury().withdraw(cost);
		
		CivMessage.sendTown(this.getTown(), CivColor.Yellow+"Paid "+cost+" coins for "+verticalSegments+" wall segments.");
		
		// build the blocks
		for (SimpleBlock sb : simpleBlocks.values()) {
			BlockCoord bcoord = new BlockCoord(sb);
			ItemManager.setTypeId(bcoord.getBlock(), sb.getType());
			ItemManager.setData(bcoord.getBlock(), sb.getData());
			
		}
		
		// Add wall to town and global tables
		this.getTown().addStructure(this);
		CivGlobal.addStructure(this);
		this.getTown().lastBuildableBuilt = this;
	}
	
	private void validateBlockLocation(Player player, Location loc) throws CivException {
		Block b = loc.getBlock();
		
		if (ItemManager.getId(b) == CivData.CHEST) {
			throw new CivException("Cannot build here, would destroy chest.");
		}
							
		TownChunk tc = CivGlobal.getTownChunk(b.getLocation());
			
		if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
			// Make sure we have permission to destroy any block in this area.
			throw new CivException("Cannot build here, you need DESTROY permissions to the block at "+b.getX()+","+b.getY()+","+b.getZ());
		}

		BlockCoord coord = new BlockCoord(b);
		//not building a trade outpost, prevent protected blocks from being destroyed.
		if (CivGlobal.getProtectedBlock(coord) != null) {
			throw new CivException("Cannot build here, protected blocks in the way.");
		}

		
		if (CivGlobal.getStructureBlock(coord) != null) {
			throw new CivException("Cannot build here, structure blocks in the way at "+coord);
		}
		
	
		if (CivGlobal.getFarmChunk(new ChunkCoord(coord.getLocation())) != null) {
			throw new CivException("Cannot build here, in the same chunk as a farm improvement.");
		}
		
		if (loc.getBlockY() >= Wall.MAX_HEIGHT) {
			throw new CivException("Cannot build here, wall is too high.");
		}
		
		if (loc.getBlockY() <= 1) {
			throw new CivException("Cannot build here, too close to bedrock.");
		}
		
		BlockCoord bcoord = new BlockCoord(loc);
		for (int y = 0; y < 256; y++) {
			bcoord.setY(y);
			StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
			if (sb != null) {
				throw new CivException("Cannot build here, this wall segment overlaps with a structure block belonging to a "+sb.getOwner().getName()+" structure.");
			}
		}
		
	}

	private void getVerticalWallSegment(Player player, Location loc, Map<String, SimpleBlock> simpleBlocks) throws CivException {
		Location tmp = new Location(loc.getWorld(), loc.getX(), loc.getY(), loc.getZ());
		for (int i = 0; i < Wall.HEIGHT; i++) {
			SimpleBlock sb;
			if (i == 0) {
				sb = new SimpleBlock(CivData.STONE_BRICK, 0x1);
			} else {
				sb = new SimpleBlock(CivData.STONE_BRICK, 0);
			}
			sb.worldname = tmp.getWorld().getName();
			sb.x = tmp.getBlockX();
			sb.y = tmp.getBlockY();
			sb.z = tmp.getBlockZ();
			
			validateBlockLocation(player, tmp);
			simpleBlocks.put(sb.worldname+","+sb.x+","+sb.y+","+sb.z, sb);
			
			tmp.add(0, 1.0, 0);
		} 
	}
	
//	private boolean inSameChunk(Location loc1, Location loc2) {
//		
//		if (loc1.getChunk().getX() == loc2.getChunk().getX()) {
//			if (loc1.getChunk().getZ() == loc2.getChunk().getZ()) {
//				return true;
//			}
//		}
//		
//		return false;
//	}
	
	private int buildWallSegment(Player player, BlockCoord first, BlockCoord second, int blockCount, 
			HashMap<String, SimpleBlock> simpleBlocks, int verticalSegments) throws CivException {
		Location locFirst = first.getLocation();
		Location locSecond = second.getLocation();
		
		Vector dir = new Vector(locFirst.getX() - locSecond.getX(), 
								locFirst.getY() - locSecond.getY(),
								locFirst.getZ() - locSecond.getZ());
		dir.normalize();
		dir.multiply(0.5);
		HashMap<String, SimpleBlock> thisWallBlocks = new HashMap<String, SimpleBlock>();
 		
		this.getTown().lastBuildableBuilt = null;
	
		getVerticalWallSegment(player, locSecond, thisWallBlocks);
		simpleBlocks.putAll(thisWallBlocks);
		verticalSegments++;

		double distance = locSecond.distance(locFirst);
		BlockCoord lastBlockCoord = new BlockCoord(locSecond);
		BlockCoord currentBlockCoord = new BlockCoord(locSecond);
		while (locSecond.distance(locFirst) > 1.0) {
			locSecond.add(dir);		
			ChunkCoord coord = new ChunkCoord(locSecond);
			CivGlobal.addWallChunk(this, coord);

			currentBlockCoord.setFromLocation(locSecond);
			if (lastBlockCoord.equals(currentBlockCoord)) {
				continue;
			} else {
				lastBlockCoord.setFromLocation(locSecond);
			}
					
			blockCount++; 
			if (blockCount > Wall.RECURSION_LIMIT) {
				throw new CivException("ERROR: Building wall blocks exceeded recusion limit! Halted to keep server alive.");
			}
			
			getVerticalWallSegment(player, locSecond, thisWallBlocks);
			simpleBlocks.putAll(thisWallBlocks);
			verticalSegments++;

			//Distance should always be going down, as a failsave
			//check that it is. Abort if our distance goes up.
			double tmpDist = locSecond.distance(locFirst);
			if (tmpDist > distance) {
				break;
			}
		}
		
		/* build the last wall segment. */
		if (!wallBlocks.containsKey(new BlockCoord(locFirst))) {
			try {
				getVerticalWallSegment(player, locFirst, thisWallBlocks);
				simpleBlocks.putAll(thisWallBlocks);
				verticalSegments++;
			} catch (CivException e) {
				CivLog.warning("Couldn't build the last wall segment, oh well.");
			}
		}
		
		for (SimpleBlock sb : simpleBlocks.values()) {
			BlockCoord bcoord = new BlockCoord(sb);
			int old_id = ItemManager.getId(bcoord.getBlock());
			int old_data = ItemManager.getData(bcoord.getBlock());
			if (!wallBlocks.containsKey(bcoord)) {
				try {
					WallBlock wb = new WallBlock(bcoord, this, old_id, old_data, sb.getType(), sb.getData());
					
					wallBlocks.put(bcoord, wb);
					this.addStructureBlock(bcoord, true);
					wb.save();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		return verticalSegments;
	}

	public boolean isProtectedLocation(Location location) {
		// Destroyed walls do not protect anything.
		if (!this.isActive()) {
			return false;
		}
		
		// We already know this location is inside a protected chunk
		// A protected location then, is any location which has a x, z match
		// and its y is less than our structure blocks'
		
		for (BlockCoord coord : this.wallBlocks.keySet()) {
			Location blockLocation = coord.getLocation();
			
			if (location.getBlockX() == blockLocation.getBlockX() &&
					location.getBlockZ() == blockLocation.getBlockZ()) {
				
				//x and z match, now check that block is 'below' us.
				if (location.getBlockY() < Wall.MAX_HEIGHT) {
					return true;
				}
			}
		}
		
		return false;
	}
	
	@Override
	public void repairStructure() throws CivException {
		double cost = getRepairCost();
		
		if (!this.isValidWall()) {
			throw new CivException("This wall is no longer valid and cannot be repaired. Walls can no longer overlap with protected structure blocks, demolish this wall instead.");
		}
		
		if (!getTown().getTreasury().hasEnough(cost)) {
			throw new CivException("Your town cannot not afford the "+cost+" coins to build a "+getDisplayName());
		}
		
		setHitpoints(this.getMaxHitPoints());
		bindStructureBlocks();
		
		for (WallBlock wb : this.wallBlocks.values()) {
			BlockCoord bcoord = wb.getCoord();
			ItemManager.setTypeId(bcoord.getBlock(), wb.getTypeId());
			ItemManager.setData(bcoord.getBlock(), wb.getData());
		}
		
		save();
		getTown().getTreasury().withdraw(cost);
		CivMessage.sendTown(getTown(), CivColor.Yellow+"The town has repaired a "+getDisplayName()+" at "+getCorner());
	}
	
	@Override
	public int getMaxHitPoints() {
		double rate = 1;
		if (this.getTown().getBuffManager().hasBuff("buff_chichen_itza_tower_hp")) {
			rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
			rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.BARRICADE);
		}
		return (int) (info.max_hitpoints * rate);
	}
	
}
