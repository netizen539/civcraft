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
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.exception.InvalidObjectException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.Wall;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;

public class WallBlock extends SQLObject {

	private BlockCoord coord;
	private Wall struct;
	int old_id;
	int old_data;
	int type_id;
	int data;
	
	public WallBlock(BlockCoord coord, Structure struct, int old_id, int old_data, int type, int data) throws SQLException {
		this.coord = coord;
		this.struct = (Wall)struct;
		this.old_data = old_data;
		this.old_id = old_id;
		this.type_id = type;
		this.data = data;
	}
	
	public WallBlock(ResultSet rs) throws SQLException, InvalidNameException, InvalidObjectException, CivException {
		this.load(rs);
	}
	
	public BlockCoord getCoord() {
		return coord;
	}

	public void setCoord(BlockCoord coord) {
		this.coord = coord;
	}
	
	public static final String TABLE_NAME = "WALLBLOCKS";
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`struct_id` int(11) NOT NULL DEFAULT 0," +
					"`coord` mediumtext DEFAULT NULL," +
					"`type_id` int(11) DEFAULT 0,"+
					"`data` int(11) DEFAULT 0," +
					"`old_id` int(11) DEFAULT 0," +
					"`old_data` int(11) DEFAULT 0," +
				"PRIMARY KEY (`id`)" + ")";
			
			SQL.makeTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
			
			if (!SQL.hasColumn(TABLE_NAME, "type_id")) {
				CivLog.info("\tCouldn't find type_id column for wallblock.");
				SQL.addColumn(TABLE_NAME, "`type_id` int(11) default 0");				
			}
			
			if (!SQL.hasColumn(TABLE_NAME, "data")) {
				CivLog.info("\tCouldn't find data column for wallblock.");
				SQL.addColumn(TABLE_NAME, "`data` int(11) default 0");				
			}
		}
	}

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException,
			InvalidObjectException, CivException {
		this.setId(rs.getInt("id"));
		this.setStruct(CivGlobal.getStructureById(rs.getInt("struct_id")));
		if (this.struct == null) {
			int id = rs.getInt("struct_id");
			this.delete();
			throw new CivException("Could not load WallBlock, could not find structure:"+id);
		}
		
		this.setCoord(new BlockCoord(rs.getString("coord")));
		
		CivGlobal.addWallChunk(this.struct, new ChunkCoord(getCoord().getLocation()));
		this.struct.addStructureBlock(this.getCoord(), true);
		this.struct.wallBlocks.put(this.getCoord(), this);
		this.old_id = rs.getInt("old_id");
		this.old_data = rs.getInt("old_data");
		this.type_id = rs.getInt("type_id");
		this.data = rs.getInt("data");
		
	}

	@Override
	public void save() {
		SQLUpdate.add(this);
	}
	
	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		
		hashmap.put("struct_id", this.getStruct().getId());
		hashmap.put("coord", this.getCoord().toString());
		hashmap.put("old_id", this.old_id);
		hashmap.put("old_data", this.old_data);
		hashmap.put("type_id", this.type_id);
		hashmap.put("data", this.data);

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() throws SQLException {
		if (this.coord != null) {
			CivGlobal.removeStructureBlock(this.coord);
		}
		SQL.deleteNamedObject(this, TABLE_NAME);
	}

	public Structure getStruct() {
		return struct;
	}

	public void setStruct(Structure struct) {
		this.struct = (Wall) struct;
	}

	public int getOldId() {
		return this.old_id;
	}
	
	public byte getOldData() {
		return (byte)this.old_data;
	}

	public int getTypeId() {
		return this.type_id;
	}
	
	public int getData() {
		return this.data;
	}
	
}
