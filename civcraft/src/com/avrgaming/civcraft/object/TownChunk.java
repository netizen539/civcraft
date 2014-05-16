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
package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class TownChunk extends SQLObject {
	
	private ChunkCoord chunkLocation;
	private Town town;
	private boolean forSale;
	/* 
	 * Price vs value, price is what the owner is currently selling it for,
	 * value is the amount that it was last purchased at, used for taxes.
	 */
	private double value;
	private double price;
	private boolean outpost;
	
	public PlotPermissions perms = new PlotPermissions();
	
	public static final String TABLE_NAME = "TOWNCHUNKS";
	
	public TownChunk (ResultSet rs) throws SQLException, CivException {
		this.load(rs);
	}
	
	public TownChunk (Town newTown, Location location) {
		ChunkCoord coord = new ChunkCoord(location);
		setTown(newTown);
		setChunkCord(coord);
		perms.addGroup(newTown.getDefaultGroup());
	}
	
	public TownChunk(Town newTown, ChunkCoord chunkLocation) {
		setTown(newTown);
		setChunkCord(chunkLocation);
		perms.addGroup(newTown.getDefaultGroup());
	}

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
				"`id` int(11) unsigned NOT NULL auto_increment," +
				"`town_id` int(11) unsigned NOT NULL," +
				"`world` VARCHAR(32) NOT NULL," +
				 "`x` bigint(20) NOT NULL," +
				 "`z` bigint(20) NOT NULL," +
				 "`owner_id` int(11) unsigned DEFAULT NULL," +
				 "`groups` mediumtext DEFAULT NULL," +
				 "`permissions` mediumtext NOT NULL," +
				 "`for_sale` bool NOT NULL DEFAULT '0'," +
				 "`value` float NOT NULL DEFAULT '0'," +
				 "`price` float NOT NULL DEFAULT '0'," +
				 "`outpost` bool DEFAULT '0'," +			 
			//	 "FOREIGN KEY (owner_id) REFERENCES "+SQL.tb_prefix+Resident.TABLE_NAME+"(id),"+
			//	 "FOREIGN KEY (town_id) REFERENCES "+SQL.tb_prefix+Town.TABLE_NAME+"(id),"+
				 "PRIMARY KEY (`id`)" + ")";
			
			SQL.makeTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
		}		
	}

	@Override
	public void load(ResultSet rs) throws SQLException, CivException {
		this.setId(rs.getInt("id"));
		this.setTown(CivGlobal.getTownFromId(rs.getInt("town_id")));
		if (this.getTown() == null) {
			CivLog.warning("TownChunk tried to load without a town...");
			if (CivGlobal.testFileFlag("cleanupDatabase")) {
				CivLog.info("CLEANING");
				this.delete();
			}
			throw new CivException("No town("+rs.getInt("town_id")+") to load this town chunk("+rs.getInt("id"));
		}
		
		ChunkCoord cord = new ChunkCoord(rs.getString("world"), rs.getInt("x"), rs.getInt("z"));
		this.setChunkCord(cord);
		
		try {
			this.perms.loadFromSaveString(town, rs.getString("permissions"));			
		} catch (CivException e) {
			e.printStackTrace();
		}
		
		this.perms.setOwner(CivGlobal.getResidentFromId(rs.getInt("owner_id")));
		//this.perms.setGroup(CivGlobal.getPermissionGroup(this.getTown(), rs.getInt("groups")));
		String grpString = rs.getString("groups");
		if (grpString != null) {
			String[] groups = grpString.split(":");
			for (String grp : groups) {
				this.perms.addGroup(CivGlobal.getPermissionGroup(this.getTown(), Integer.valueOf(grp)));
			}
		}
			
		this.forSale = rs.getBoolean("for_sale");
		this.value = rs.getDouble("value");
		this.price = rs.getDouble("price");
		this.outpost = rs.getBoolean("outpost");
		
		if (!this.outpost) {
			try {
				this.getTown().addTownChunk(this);
			} catch (AlreadyRegisteredException e1) {
				e1.printStackTrace();
			}
		} else {
			try {
				this.getTown().addOutpostChunk(this);
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
			}
		}
		
	}

	@Override
	public void save() {
		SQLUpdate.add(this);
	}

	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		
		hashmap.put("id", this.getId());
		hashmap.put("town_id", this.getTown().getId());
		hashmap.put("world", this.getChunkCoord().getWorldname());
		hashmap.put("x", this.getChunkCoord().getX());
		hashmap.put("z", this.getChunkCoord().getZ());
		hashmap.put("permissions", perms.getSaveString());
		hashmap.put("for_sale", this.isForSale());
		hashmap.put("value", this.getValue());
		hashmap.put("price", this.getPrice());
		hashmap.put("outpost", this.outpost);
		
		if (this.perms.getOwner() != null) {
			hashmap.put("owner_id", this.perms.getOwner().getId());
		} else {
			hashmap.put("owner_id", null);
		}
		
		if (this.perms.getGroups().size() != 0) {
			String out = "";
			for (PermissionGroup grp : this.perms.getGroups()) {
				out += grp.getId()+":";
			}
			hashmap.put("groups", out);
		} else {
			hashmap.put("groups", null);
		}
						
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}
	
	public Town getTown() {
		return town;
	}

	public void setTown(Town town) {
		this.town = town;
	}

	public ChunkCoord getChunkCoord() {
		return chunkLocation;
	}

	public void setChunkCord(ChunkCoord chunkLocation) {
		this.chunkLocation = chunkLocation;
	}
	
	public static double getNextPlotCost(Town town) {
		
		ConfigTownLevel effectiveTownLevel = CivSettings.townLevels.get(CivSettings.townLevels.size());
		int currentPlotCount = town.getTownChunks().size();
			
		for (ConfigTownLevel lvl : CivSettings.townLevels.values()) {
			if (currentPlotCount < lvl.plots) {
				if (effectiveTownLevel.plots > lvl.plots) {
					effectiveTownLevel = lvl;
				}
			}
		}
		
		
		return effectiveTownLevel.plot_cost;		
	}

	public static TownChunk claim(Town town, ChunkCoord coord, boolean outpost) throws CivException {
		if (CivGlobal.getTownChunk(coord) != null) {
			throw new CivException("This plot is already claimed.");
		}
		
		double cost;	
		cost = getNextPlotCost(town);
		
		if (!town.hasEnough(cost)) {
			throw new CivException("The town does not have the required "+cost+" coins to claim this plot.");
		}
		
		CultureChunk cultureChunk = CivGlobal.getCultureChunk(coord);
		if (cultureChunk == null || cultureChunk.getCiv() != town.getCiv()) {
			throw new CivException("Cannot claim a town chunk when not in your culture.");
		}
		
		TownChunk tc = new TownChunk(town, coord);
		
		if (!outpost) {
			if (!tc.isOnEdgeOfOwnership()) {
				throw new CivException("Can only claim on the edge of town's ownership.");
			}
		
			if (!town.canClaim()) {
				throw new CivException("Town is unable to claim, doesn't have enough plots for this town level.");
			}
		}
		
		//Test that we are not too close to another civ
		try {
			int min_distance = CivSettings.getInteger(CivSettings.civConfig, "civ.min_distance");
			
			for (TownChunk cc : CivGlobal.getTownChunks()) {
				if (cc.getCiv() != town.getCiv()) {
					double dist = coord.distance(cc.getChunkCoord());
					if (dist <= min_distance) {
						throw new CivException("Too close to the culture of "+cc.getCiv().getName()+", cannot claim here.");
					}
				}
			}	
		} catch (InvalidConfiguration e1) {
			e1.printStackTrace();
			throw new CivException("Internal configuration exception.");
		}
		
		//Test that we are not too far protruding from our own town chunks
//		try {
//			int max_protrude = CivSettings.getInteger(CivSettings.townConfig, "town.max_town_chunk_protrude");
//			if (max_protrude != 0) {
//				if (isTownChunkProtruding(tc, 0, max_protrude, new HashSet<ChunkCoord>())) {
//					throw new CivException("You cannot claim here, too far away from the rest of your town chunks.");
//				}
//			}			
//		} catch (InvalidConfiguration e1) {
//			e1.printStackTrace();
//			throw new CivException("Internal configuration exception.");
//		}
		
		if (!outpost) {
			try {
				town.addTownChunk(tc);
			} catch (AlreadyRegisteredException e1) {
				e1.printStackTrace();
				throw new CivException("Internal Error Occurred.");
	
			}
		} else {
			try {
				town.addOutpostChunk(tc);
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
				throw new CivException("Internal Error Occurred.");
			}
		}
		
		Camp camp = CivGlobal.getCampFromChunk(coord);
		if (camp != null) {
			CivMessage.sendCamp(camp, CivColor.Yellow+ChatColor.BOLD+"Our camp's land was claimed by the town of "+town.getName()+" and has been disbaned!");
			camp.disband();
		}
		
		tc.setOutpost(outpost);
		tc.save();
		town.withdraw(cost);			
		CivGlobal.addTownChunk(tc);
		CivGlobal.processCulture();
		return tc;
	}
	
	
	public static TownChunk claim(Town town, Player player, boolean outpost) throws CivException {
		double cost = getNextPlotCost(town);
		TownChunk tc = claim(town, new ChunkCoord(player.getLocation()), outpost);
		CivMessage.sendSuccess(player, "Claimed chunk at "+tc.getChunkCoord()+" for "+CivColor.Yellow+cost+CivColor.LightGreen+" coins.");
		return tc;
	}
	
	
	/* Returns true if this townchunk is outside our protrude limits. */
//	private static boolean isTownChunkProtruding(TownChunk start, int protrude_count, int max_protrude, 
//			HashSet<ChunkCoord> closedList) {
//		
//		if (protrude_count > max_protrude) {
//			return true;
//		}
//		
//		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
//		ChunkCoord coord = new ChunkCoord(start.getChunkCoord().getWorldname(), 
//				start.getChunkCoord().getX(), start.getChunkCoord().getZ());
//		closedList.add(coord);
//		
//		TownChunk nextChunk = null;
//		for (int i = 0; i < 4; i++) {
//			coord.setX(coord.getX() + offset[i][0]);
//			coord.setZ(coord.getZ() + offset[i][1]);
//			
//			if (closedList.contains(coord)) {
//				continue;
//			}
//			
//			TownChunk tc = CivGlobal.getTownChunk(coord);
//			if (tc == null) {
//				continue;
//			}
//			
//			if (nextChunk == null) {
//				nextChunk = tc;
//			} else {
//				/* We found another chunk next to us, this chunk doesnt protrude. */
//				return false;
//			}
//		}
//		
//		if (nextChunk == null) {
//			/* We found no chunk next to us at all.. shouldn't happen but this chunk doesnt protrude. */
//			return false;
//		}
//		
//		return isTownChunkProtruding(nextChunk, protrude_count + 1, max_protrude, closedList);
//	}
	
	private Civilization getCiv() {
		return this.getTown().getCiv();
	}

	/*
	 * XXX This claim is only called when a town hall is building and needs to be claimed.
	 * We do not save here since its going to be saved in-order using the SQL save in order
	 * task. Also certain types of validation and cost cacluation are skipped.
	 */
	public static TownChunk townHallClaim(Town town, ChunkCoord coord) throws CivException {
		//This is only called when the town hall is built and needs to be claimed.
		
		if (CivGlobal.getTownChunk(coord) != null) {
			throw new CivException("This plot is already claimed.");
		}
		
		TownChunk tc = new TownChunk(town, coord);
		
		try {
			town.addTownChunk(tc);
		} catch (AlreadyRegisteredException e1) {
			e1.printStackTrace();
			throw new CivException("Internal Error Occurred.");

		}
	
		Camp camp = CivGlobal.getCampFromChunk(coord);
		if (camp != null) {
			CivMessage.sendCamp(camp, CivColor.Yellow+ChatColor.BOLD+"Our camp's land was claimed by the town of "+town.getName()+" and has been disbaned!");
			camp.disband();
		}
		
		CivGlobal.addTownChunk(tc);
		tc.save();
		return tc;
	}

	private boolean isOnEdgeOfOwnership() {
		
		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
				
		for (int i = 0; i < 4; i++) {
			TownChunk tc = CivGlobal.getTownChunk(new ChunkCoord(this.getChunkCoord().getWorldname(), 
					this.getChunkCoord().getX() + offset[i][0], 
					this.getChunkCoord().getZ() + offset[i][1]));
			if (tc != null && 
				tc.getTown() == this.getTown() && 
				!tc.isOutpost()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public void delete() throws SQLException {
		SQL.deleteNamedObject(this, TABLE_NAME);
		CivGlobal.removeTownChunk(this);
	}

	public boolean isForSale() {
		return forSale;
	}

	public void setForSale(boolean forSale) {
		this.forSale = forSale;
	}

	public double getValue() {
		return value;
	}

	/* Called when a player enters this plot. */
	public String getOnEnterString(Player player, TownChunk fromTc) {
		String out = "";
		
		if (this.perms.getOwner() != null) {
			out += CivColor.LightGray+"[Owned by: "+CivColor.LightGreen+this.perms.getOwner().getName()+CivColor.LightGray+"]";
		}
		
		if (this.perms.getOwner() == null && fromTc != null && fromTc.perms.getOwner() != null) {
			out += CivColor.LightGray+"[Unowned]";
		}
		
		if (this.isForSale()) {
			out += CivColor.Yellow+"[For Sale: "+this.price+" coins]";
		}
		
		return out;
	}

	public void purchase(Resident resident) throws CivException {

		if (!resident.getTreasury().hasEnough(this.price)) {
			throw new CivException("You do not have the required "+this.price+" coins to purchase this plot.");
		}
		
		if (this.perms.getOwner() == null) {
			resident.getTreasury().payTo(this.getTown().getTreasury(), this.price);
		} else {
			resident.getTreasury().payTo(this.perms.getOwner().getTreasury(), this.price);
		}
	
		this.value = this.price;
		this.price = 0;
		this.forSale = false;
		this.perms.setOwner(resident);
		this.perms.clearGroups();
		
		this.save();
	}

	public double getPrice() {
		return price;
	}

	public void setPrice(double price) {
		this.price = price;
	}
	
	public String getCenterString() {
		/* 
		 * Since the chunk is the floor of the block coords divided by 16.
		 * The middle of the chunk is 8 more from there....
		 */
		//int blockx = (this.chunkLocation.getX()*16)+8;
		//int blockz = (this.chunkLocation.getZ()*16)+8;
		//TODO work out the bugs with this.
		
		return this.chunkLocation.toString();
	}

	public boolean isEdgeBlock() {

		int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
		
		if (this.isOutpost()) {
			return false;
		}
		
		for (int i = 0; i < 4; i++) {
			TownChunk next = CivGlobal.getTownChunk(new ChunkCoord(this.chunkLocation.getWorldname(), 
					this.chunkLocation.getX() + offset[i][0], 
					this.chunkLocation.getZ()+ offset[i][1]));
			if (next == null || next.isOutpost()) { 
				return true;
			}
		}
		
		return false;
	}

	public static void unclaim(TownChunk tc) throws CivException {
		
		//TODO check that its not the last chunk
		//TODO make sure that its not owned by someone else.
		
		
		
		tc.getTown().removeTownChunk(tc);
		try {
			tc.delete();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException("Internal database error.");
		}
		
	}

	public boolean isOutpost() {
		return outpost;
	}

	public void setOutpost(boolean outpost) {
		this.outpost = outpost;
	}



}
