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

import org.bukkit.block.Sign;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.BlockCoord;

public class StructureSign extends SQLObject {

	private String text;
	private Buildable owner;
	private String type;
	private String action;
	private BlockCoord coord;
	private int direction;
	private boolean allowRightClick = false;
	
	public StructureSign(BlockCoord coord, Buildable owner) {
		this.coord = coord;
		this.owner = owner;
	}
	
	public StructureSign(ResultSet rs) throws SQLException {
		load(rs);
	}

	public static String TABLE_NAME = "STRUCTURE_SIGNS";
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`text` mediumtext, " +
					"`structure_id` int(11), " +
					"`wonder_id` int(11)," +
					"`type` mediumtext, " +
					"`action` mediumtext, "+
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
	public void load(ResultSet rs) throws SQLException {
		this.setId(rs.getInt("id"));
		this.text = rs.getString("text");
		this.action = rs.getString("action");
		this.type = rs.getString("type");
		int structure_id = rs.getInt("structure_id");
		int wonder_id = rs.getInt("wonder_id");
		this.owner = null; 
	
		if (structure_id != 0) {
			this.owner = CivGlobal.getStructureById(structure_id);
		} else if (wonder_id != 0) {
			this.owner = CivGlobal.getWonderById(wonder_id);
		} 
		
		
		this.coord = new BlockCoord(rs.getString("coordHash"));
		this.direction = rs.getInt("direction");

		if (this.owner != null) {
			owner.addStructureSign(this);
		}
	}

	@Override
	public void save() {
		SQLUpdate.add(this);
	}
	
	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		hashmap.put("text", this.getText());
		
		if (this.owner == null) {
			hashmap.put("structure_id", 0);
			hashmap.put("wonder_id", 0);
		} else if (this.owner instanceof Structure) {
			hashmap.put("structure_id", owner.getId());
			hashmap.put("wonder_id", 0);			
		} else if (this.owner instanceof Wonder) {
			hashmap.put("structure_id", 0);
			hashmap.put("wonder_id", owner.getId());
		}
		
		hashmap.put("type", this.getType());
		hashmap.put("action", this.getAction());
		hashmap.put("coordHash", this.coord.toString());
		hashmap.put("direction", this.direction);

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);	
	}

	@Override
	public void delete() throws SQLException {
		SQL.deleteNamedObject(this, TABLE_NAME);
		CivGlobal.removeStructureSign(this);	
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Buildable getOwner() {
		return owner;
	}

	public void setOwner(Buildable owner) {
		this.owner = owner;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public BlockCoord getCoord() {
		return coord;
	}

	public void setCoord(BlockCoord coord) {
		this.coord = coord;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public void setText(String[] message) {
		this.text = "";
		for (String str : message) {
			text += str+"\n";
		}
	}
	
	public void update() {
		if (coord.getBlock().getState() instanceof Sign) {
			Sign sign = (Sign)coord.getBlock().getState();
			String[] lines = this.text.split("\\n");
			
			for (int i = 0; i < 4; i++) {
				if (i < lines.length) {
					sign.setLine(i, lines[i]);
				} else {
					sign.setLine(i, "");
				}
			}
			sign.update();
		}
	}

	public boolean isAllowRightClick() {
		return allowRightClick;
	}

	public void setAllowRightClick(boolean allowRightClick) {
		this.allowRightClick = allowRightClick;
	}

}
