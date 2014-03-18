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

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.InvalidObjectException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.util.BlockCoord;

public class StructureChest extends SQLObject {

	private BlockCoord coord;
	private Buildable owner;
	private int direction;
	
	/* The chest id defines which chests are 'paired' for double chests. */
	private int chestId;
	
	public StructureChest(BlockCoord coord, Buildable owner) {
		this.setCoord(coord);
		this.setOwner(owner);
	}
	
	public StructureChest(ResultSet rs) throws SQLException, InvalidObjectException {
		this.load(rs);
	}
	
	public static String TABLE_NAME = "STRUCTURE_CHESTS";
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`structure_id` int(11), " +
					"`chest_id` int(11), "+
					"`coordHash` mediumtext, "+
					"`direction` int(11), "+
				"PRIMARY KEY (`id`)" + ")";
			
			SQL.makeTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
		}
	}
	
	@Override
	public void load(ResultSet rs) throws SQLException, InvalidObjectException {
		this.setId(rs.getInt("id"));
		Buildable owner = CivGlobal.getStructureById(rs.getInt("structure_id"));
		if (owner == null) {
			CivLog.warning("Couldn't find structure id:"+rs.getInt("structure_id")+" while loading structure chests.");
			throw new InvalidObjectException("Couldn't find structure id:"+rs.getInt("structure_id"));
		}
		
		this.owner = owner;
		this.direction = rs.getInt("direction");
		this.coord = new BlockCoord(rs.getString("coordHash"));
		this.chestId = rs.getInt("chest_id");
		owner.addStructureChest(this);
	}

	@Override
	public void save() {
		SQLUpdate.add(this);
	}
	
	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("structure_id", this.owner.getId());
		hashmap.put("coordHash", this.coord.toString());
		hashmap.put("direction", this.direction);
		hashmap.put("chest_id", this.chestId);
		
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() throws SQLException {
		SQL.deleteNamedObject(this, TABLE_NAME);
		CivGlobal.removeStructureChest(this);
	}

	public BlockCoord getCoord() {
		return coord;
	}

	public void setCoord(BlockCoord coord) {
		this.coord = coord;
	}

	public Buildable getOwner() {
		return owner;
	}

	public void setOwner(Buildable owner) {
		this.owner = owner;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public int getChestId() {
		return chestId;
	}

	public void setChestId(int chestId) {
		this.chestId = chestId;
	}

	
	
}
