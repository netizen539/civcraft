package com.avrgaming.global.perks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;

public class PerkManagerSimple extends PerkManager {

	/*
	 * Create required tables in the global database if they cannot be found.
	 */
	public static String USER_PERKS_TABLE_NAME = "USER_PERKS";
	public static String USER_PLATINUM_TABLE_NAME = "USER_PLATINUM";
	
	@Override
	public void init() throws SQLException {
		System.out.println("================= USER_PERKS INIT ======================");
		
		// Check/Build SessionDB tables				
		if (!SQL.hasGlobalTable(USER_PERKS_TABLE_NAME)) {
			String table_create = "CREATE TABLE " + USER_PERKS_TABLE_NAME+" (" +
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`uuid` varchar(256)," +
					"`perk_id` mediumtext,"+
					"`used_phase` mediumtext," +
					"PRIMARY KEY (`id`)" + ")";
			
			SQL.makeGlobalTable(table_create);
			CivLog.info("Created "+USER_PERKS_TABLE_NAME+" table");
		} else {
			CivLog.info(USER_PERKS_TABLE_NAME+" table OK!");
		}		
				
		System.out.println("==================================================");	
		
		System.out.println("================= USER_PLATINUM INIT ======================");
		
		// Check/Build SessionDB tables				
		if (!SQL.hasGlobalTable(USER_PLATINUM_TABLE_NAME)) {
			String table_create = "CREATE TABLE " + USER_PLATINUM_TABLE_NAME+" (" +
					"`uuid` varchar(256)," +
					"`game_name` mediumtext," +
					"`platinum` int(11),"+
					"PRIMARY KEY (`uuid`)" + ")";
			
			SQL.makeGlobalTable(table_create);
			CivLog.info("Created "+USER_PLATINUM_TABLE_NAME+" table");
		} else {
			CivLog.info(USER_PLATINUM_TABLE_NAME+" table OK!");
		}		
				
		System.out.println("==================================================");	
	}
	
	@Override
	public void loadPerksForResident(Resident resident) throws SQLException, NotVerifiedException, CivException {
		String sql;
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement s = null;
		HashMap<String, Integer> perkCounts = new HashMap<String, Integer>();
		

		try {
			context = SQL.getGlobalConnection();
			
			String uuid = resident.getUUIDString();
		
			try {
				/* Lookup join table for perks and users. */
				sql = "SELECT `perk_id`,`used_phase` FROM `"+USER_PERKS_TABLE_NAME+"` WHERE `uuid` = ?";
				s = context.prepareStatement(sql);
				s.setString(1, uuid);
				
				rs = s.executeQuery();
		
				while (rs.next()) {
					/* 'used' is now deprecated. */
					String usedPhase = rs.getString("used_phase");					
					String id = rs.getString("perk_id");
					if ((usedPhase == null) || !usedPhase.equals(CivGlobal.getPhase())) {					
						Integer count = perkCounts.get(id);
						if (count == null) {
							perkCounts.put(id, 1);
						} else {
							perkCounts.put(id, count+1);
						}
					}
				}
			} finally {
				SQL.close(rs, s, null);
			}

			for (String perkID : perkCounts.keySet()) {
				ConfigPerk configPerk = CivSettings.perks.get(perkID);
				if (configPerk == null) {
					continue;
				}
				
				int count = perkCounts.get(perkID);
				
				Perk p = new Perk(configPerk);
				p.count = count;
				resident.perks.put(p.getIdent(), p);
			}
			
			return;
		} finally {
			SQL.close(rs, s, context);
		}
	}
	
	@Override
	public int addPerkToResident(Resident resident, String perk_id, Integer count) throws SQLException, CivException {
		String sql;
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement s = null;
		
		try {
			context = SQL.getGlobalConnection();
			
			String uuid = resident.getUUIDString();

			int added = 0;
			for (int i = 0; i < count; i++) {
				/* Lookup join table for perks and users. */
				sql = "INSERT INTO `"+USER_PERKS_TABLE_NAME+"` (uuid, perk_id) VALUES(?, ?)";
				s = context.prepareStatement(sql);
				s.setString(1, uuid);
				s.setString(2, perk_id);
				added += s.executeUpdate();
			}
			return added;
		} finally {
			SQL.close(rs, s, context);
		}
	}
	
	@Override
	public int removePerkFromResident(Resident resident, String perk_id, Integer count) throws SQLException, CivException {
		String sql;
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement s = null;
		
		try {
			context = SQL.getGlobalConnection();
			
			String uuid = resident.getUUIDString();

			/* Lookup join table for perks and users. */
			sql = "DELETE FROM `"+USER_PERKS_TABLE_NAME+"` WHERE `uuid` = ? AND `perk_id` = ? LIMIT ?";
			s = context.prepareStatement(sql);
			s.setString(1, uuid);
			s.setString(2, perk_id);
			s.setInt(3, count);
			return s.executeUpdate();
		} finally {
			SQL.close(rs, s, context);
		}
	}
	
	@Override
	public void updatePlatinum(Resident resident, Integer plat) throws SQLException, NotVerifiedException {
		Connection context = null;
		PreparedStatement s = null;
	
		try {
			context = SQL.getGlobalConnection();
			String sql = "INSERT INTO `"+USER_PLATINUM_TABLE_NAME+"` (`uuid`, `game_name`, `platinum`) "+
					 "VALUES (?, ?, ?) ON DUPLICATE KEY UPDATE `platinum`=`platinum`+?";
			s = context.prepareStatement(sql);
			s.setString(1, resident.getUUIDString());
			s.setString(2, resident.getName());
			s.setInt(3, plat);
			s.setInt(4, plat);

		
			CivLog.info("Updated Platinum, resident:"+resident.getName()+"(uuid:"+resident.getUUIDString()+")"+" with:"+plat);
			int update = s.executeUpdate();
			if (update != 1) {
				CivLog.error("Failed to update platinum. Updated "+update+" rows when it should have been 1");
			}
			
			return;
		} finally {
			SQL.close(null, s, context);
		}
	}

	@Override
	public void markAsUsed(Resident resident, Perk parent) throws SQLException, NotVerifiedException {
		Connection context = null;
		PreparedStatement s = null;
		
		try {
			context = SQL.getGlobalConnection();	
			String uuid = resident.getUUIDString();
			String perkID = parent.getIdent();
						
			String sql = "UPDATE `"+USER_PERKS_TABLE_NAME+"` SET `used_phase` = ? WHERE `uuid` = ? AND `perk_id` = ? AND (`used_phase` IS NULL OR `used_phase` NOT LIKE ?) LIMIT 1";
			s = context.prepareStatement(sql);
			s.setString(1, CivGlobal.getPhase());
			s.setString(2, uuid);
			s.setString(3, perkID);
			s.setString(4, CivGlobal.getPhase());
			
			int update = s.executeUpdate();
			if (update != 1) {
				CivLog.error("Marked an unexpected number of perks as used. Marked "+update+" should have been 1");
			}
			return;
		} finally {
			SQL.close(null, s, context);
		}
	}
	
}
