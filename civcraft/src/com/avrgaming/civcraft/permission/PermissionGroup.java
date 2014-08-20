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
package com.avrgaming.civcraft.permission;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.object.Town;

public class PermissionGroup extends SQLObject {

	private Map<String, Resident> members = new ConcurrentHashMap<String, Resident>();
	/* Only cache towns as the 'civ' can change when a town gets conquered or gifted/moved. */
	private Town cacheTown = null;
	
	private int civId;
	private int townId;
	
	public PermissionGroup(Civilization civ, String name) throws InvalidNameException {
		this.civId = civ.getId();
		this.setName(name);
	}
	
	public PermissionGroup(Town town, String name) throws InvalidNameException {
		this.townId = town.getId();
		this.cacheTown = town;
		this.setName(name);
	}
	
	public PermissionGroup(ResultSet rs) throws SQLException, InvalidNameException {
		this.load(rs);
	}

	public void addMember(Resident res) {
		if (CivGlobal.useUUID) {
			members.put(res.getUUIDString(), res);
		} else {
			members.put(res.getName(), res);
		}
	}
	
	public void removeMember(Resident res) {
		if (CivGlobal.useUUID) {
			members.remove(res.getUUIDString());
		} else {		
			members.remove(res.getName());
		}
	}

	public boolean hasMember(Resident res) {		
		if (CivGlobal.useUUID) {
			return members.containsKey(res.getUUIDString());
		} else {
			return members.containsKey(res.getName());	
		}
	}
	
	public void clearMembers() {
		members.clear();
	}

	public static final String TABLE_NAME = "GROUPS";
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`name` VARCHAR(64) NOT NULL," + 
					"`town_id` int(11)," +
					"`civ_id` int(11)," +
					"`members` mediumtext," + 
				//"FOREIGN KEY (town_id) REFERENCES "+SQL.tb_prefix+"TOWN(id),"+
				//"FOREIGN KEY (civ_id) REFERENCES "+SQL.tb_prefix+"CIVILIZATIONS(id),"+
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
		this.setName(rs.getString("name"));
		this.setTownId(rs.getInt("town_id"));
		this.setCivId(rs.getInt("civ_id"));
		loadMembersFromSaveString(rs.getString("members"));
		
		if (this.getTownId() != 0) {
			this.cacheTown = CivGlobal.getTownFromId(this.getTownId());		
			this.getTown().addGroup(this);
		} else {
			Civilization civ = CivGlobal.getCivFromId(this.getCivId());
			if (civ == null) {
				civ = CivGlobal.getConqueredCivFromId(this.getCivId());
				if (civ == null) {
					CivLog.warning("COUlD NOT FIND CIV ID:"+this.getCivId()+" for group: "+this.getName()+" to load.");
					return;
				}
			}
			
			civ.addGroup(this);
		}
	}

	@Override
	public void save() {	
		SQLUpdate.add(this);
	}
	
	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		
		hashmap.put("name", this.getName());
		hashmap.put("members", this.getMembersSaveString());
		hashmap.put("town_id", this.getTownId());
		hashmap.put("civ_id", this.getCivId());
		
		SQL.updateNamedObject(this, hashmap, TABLE_NAME);	
	}

	@Override
	public void delete() throws SQLException {
		SQL.deleteNamedObject(this, TABLE_NAME);
	}
	
	private String getMembersSaveString() {
		String ret = "";
		
		for (String name : members.keySet()) {
			ret += name + ",";
		}
		
		return ret;
	}
	
	private void loadMembersFromSaveString(String src) {
		String[] names = src.split(",");
		
		for (String n : names) {
			Resident res;
			if (CivGlobal.useUUID) {
				res = CivGlobal.getResidentViaUUID(UUID.fromString(n));
			} else {
				res = CivGlobal.getResident(n);		
			}
			
			if (res != null) {
				members.put(n, res);
			}
		}
	}

	public Town getTown() {
		return cacheTown;
	}

	public void setTown(Town town) {
		this.cacheTown = town;
	}

	public int getMemberCount() {
		return members.size();
	}

	public Collection<Resident> getMemberList() {
		return members.values();
	}

	public Civilization getCiv() {
		if (cacheTown == null) {
			return null;
		}
		
		return cacheTown.getCiv();
	}

	public boolean isProtectedGroup() {
		return isTownProtectedGroup(this.getName()) || isCivProtectedGroup(this.getName());
	}
	
	public static boolean isProtectedGroupName(String name) {
		return isTownProtectedGroup(name) || isCivProtectedGroup(name);
	}

	public boolean isTownProtectedGroup() {
		return isTownProtectedGroup(this.getName());
	}
	
	public boolean isCivProtectedGroup() {
		return isCivProtectedGroup(this.getName());
	}
	
	private static boolean isTownProtectedGroup(String name) {
		switch (name.toLowerCase()) {
		case "mayors":
		case "assistants":
		case "residents":
			return true;
		}
		return false;
	}
	
	private static boolean isCivProtectedGroup(String name) {
		switch (name.toLowerCase()) {
		case "leaders":
		case "advisers":
			return true;
		}
		return false;
	}
	
	public String getMembersString() {
		String out = "";
		
		if (CivGlobal.useUUID) {
			for (String uuid : members.keySet()) {
				Resident res = CivGlobal.getResidentViaUUID(UUID.fromString(uuid));
				out += res.getName()+", ";
			}
		} else {
			for (String name : members.keySet()) {
				out += name+", ";
			}
		}
		return out;
	}

	public int getCivId() {
		return civId;
	}

	public void setCivId(int civId) {
		this.civId = civId;
	}

	public int getTownId() {
		return townId;
	}

	public void setTownId(int townId) {
		this.townId = townId;
	}	
}
