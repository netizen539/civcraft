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
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.CivColor;

public class Relation extends SQLObject {

	private Civilization civ;
	private Civilization other_civ;
	private Civilization aggressor_civ = null;
	
	/*
	 * Relationships are going to be 1 per-civ pair. This should simplify things.
	 */
	public enum Status {
		NEUTRAL,
		HOSTILE,
		WAR,
		PEACE,
		ALLY,
//		MASTER,
//		VASSAL
	}

	private Status relation = Status.NEUTRAL;
	private Date created = null;
	private Date expires = null;
	
	public static final String TABLE_NAME = "RELATIONS";
	
	public Relation(Civilization civ, Civilization otherCiv, Status status, Date expires) {
		this.civ = civ;
		this.other_civ = otherCiv;
		this.relation = status;
		this.created = new Date();
		this.expires = expires;
		
		this.save();
	}
	
	public Relation(ResultSet rs) throws SQLException, InvalidNameException {
		this.load(rs);
		if (this.civ != null) {
			civ.getDiplomacyManager().addRelation(this);
		}
	}

	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`civ_id` int(11) NOT NULL DEFAULT 0," +
					"`other_civ_id` int(11) NOT NULL DEFAULT 0," +
					"`relation` mediumtext DEFAULT NULL," +
					"`aggressor_civ_id` int(11) NOT NULL DEFAULT 0,"+
					"`created` long," +
					"`expires` long," +
				"PRIMARY KEY (`id`)" + ")";
			
			SQL.makeTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
		}
	}
	
	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException {
		this.setId(rs.getInt("id"));
		civ = CivGlobal.getCivFromId(rs.getInt("civ_id"));
		if (civ == null) {
			CivLog.warning("Couldn't find civ id:"+rs.getInt("civ_id")+" deleting this relation.");
			this.delete();
			return;
		}
		
		other_civ = CivGlobal.getCivFromId(rs.getInt("other_civ_id"));
		if (other_civ == null) {
			CivLog.warning("Couldn't find other civ id:"+rs.getInt("other_civ_id")+" deleting this relation.");
			this.civ = null;
			this.delete();
			return;
		}
		
		try {
			relation = Status.valueOf(rs.getString("relation"));
		} catch(IllegalArgumentException e) {
			relation = Status.WAR;
		}
				
		int aggressor_id = rs.getInt("aggressor_civ_id");
		if (aggressor_id != 0) {
			setAggressor(CivGlobal.getCivFromId(aggressor_id));
		}
		
		
		Long createdLong = rs.getLong("created");
		Long expiresLong = rs.getLong("expires");
		
		if (createdLong != null && createdLong != 0) {
			created = new Date(createdLong);
		}
		
		if (expiresLong != null && expiresLong != 0) {
			expires = new Date(expiresLong);
		}
	}

	@Override
	public void save() {
		SQLUpdate.add(this);
	}
	
	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		
		hashmap.put("civ_id", civ.getId());
		hashmap.put("other_civ_id", other_civ.getId());
		hashmap.put("relation", relation.name());
		if (aggressor_civ != null) {
			hashmap.put("aggressor_civ_id", aggressor_civ.getId());
		}
		
		if (created != null) {
			hashmap.put("created", created.getTime());
		} else {
			hashmap.put("created", 0);			
		}
		
		if (expires != null) {
			hashmap.put("expires", expires.getTime());
		} else {
			hashmap.put("expires", 0);
		}
		
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}

	@Override
	public void delete() throws SQLException {
		SQL.deleteNamedObject(this, TABLE_NAME);
	}

	public Status getStatus() {
		return relation;
	}

	public Civilization getOtherCiv() {
		return other_civ;
	}

	public void setStatus(Status status) {
		relation = status;
		this.save();
	}

	@Override
	public String toString() {
		String color = CivColor.White;
		String out = "";
		
		out = relation.name()+CivColor.White+" with "+this.other_civ.getName();
		switch (relation) {
		case NEUTRAL:
			break;
		case HOSTILE:
			color = CivColor.Yellow;
			break;
		case WAR:
			color = CivColor.Rose;
			break;
		case PEACE:
			color = CivColor.LightBlue;
			break;
		case ALLY:
			color = CivColor.Green;
			break;
//		case MASTER:
//			color = CivColor.Gold;
//			out = "MASTER"+CivColor.White+" of "+this.other_civ.getName();
//			break;
//		case VASSAL:
//			color = CivColor.LightPurple;
//			out = "VASSAL"+CivColor.White+" to "+this.other_civ.getName();
//			break;
		}
		
		String expireString = "";
		if (this.expires != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("M/d/y k:m:s z");
			expireString = CivColor.LightGray+" (Expires "+sdf.format(expires)+")";
		}
		
		return color+out+expireString;
		
		
	}
	
	public static String getRelationColor(Status status) {
		switch (status) {
		case NEUTRAL:
			return CivColor.White;
		case HOSTILE:
			return CivColor.Yellow;
		case WAR:
			return CivColor.Rose;
		case PEACE:
			return CivColor.LightBlue;
		case ALLY:
			return CivColor.Green;
//		case MASTER:
//			return CivColor.Gold;
//		case VASSAL:
//			return CivColor.LightPurple;
		default:
			return CivColor.White;
		}
	}

	public Date getExpireDate() {
		return expires;
	}

	public void setExpires(Date expires2) {
		this.expires = expires2;
	}

	public Civilization getCiv() {
		return civ;
	}

	public Civilization getAggressor() {
		return aggressor_civ;
	}

	public void setAggressor(Civilization aggressor_civ) {
		this.aggressor_civ = aggressor_civ;
	}
	
	/*
	 * This key is unique to the 'pair' of relations so that
	 * Civ A --> WAR --> CivB
	 * Uses the same key as 
	 * Civ B --> WAR --> CivA
	 * 
	 * We'll ensure that by comparing they're ids and returning the string
	 * id1:id2 
	 * 
	 * where id1 is always less than id2.
	 */
	public String getPairKey() {
		String key = "";
		
		if (this.getCiv().getId() < this.getOtherCiv().getId()) {
			key += this.getCiv().getId()+":"+this.getOtherCiv().getId();
		} else {
			key += this.getOtherCiv().getId()+":"+this.getCiv().getId();
		}
		
		return key;
	}
}
