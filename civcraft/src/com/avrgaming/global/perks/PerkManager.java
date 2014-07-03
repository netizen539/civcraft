package com.avrgaming.global.perks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigPerk;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;

public class PerkManager {

	public static String hostname = "";
	public static String port = "";
	public static String db_name = "";
	public static String username = "";
	public static String password = "";
	public static String dsn = "";
	//public static Connection context = null;	
	
	public static HashMap<String, Integer> identPlatinumRewards = new HashMap<String, Integer>();
	

	public void init() throws SQLException {
	}
	
	private static HashMap<String, Integer> userIdCache = new HashMap<String, Integer>();
	private static Integer getUserWebsiteId(Resident resident) throws SQLException, NotVerifiedException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement s = null;
		
		try {
			context = SQL.getPerkConnection();	
			/* User id's don't change, if we've looked it up before, dont look it up again. */
			Integer userId = userIdCache.get(resident.getName());
			if (userId != null) {
				return userId;
			}
				
			String sql = "SELECT `id`, `game_name`, `verified` FROM `users` WHERE `game_name` = ?";
			s = context.prepareStatement(sql);
			s.setString(1, resident.getName());
			
			rs = s.executeQuery();
			if (!rs.next()) {
				throw new NotVerifiedException();
			}
			
			/* Double check resident is verified. */
			Boolean verified = rs.getBoolean("verified");
			if (!verified) {
				throw new NotVerifiedException();
			}
			
			userId = rs.getInt("id");
			userIdCache.put(resident.getName(), userId);
			return userId;
		} finally {
			SQL.close(rs, s, context);
		}
	}
	
	public int addPerkToResident(Resident resident, String perk_id, Integer count) throws SQLException, CivException {
		return 0;
	}
	
	public int removePerkFromResident(Resident resident, String perk_id, Integer count) throws SQLException, CivException {
		return 0;
	}
	public void loadPerksForResident(Resident resident) throws SQLException, NotVerifiedException, CivException {
		LinkedList<String> perkIdents = new LinkedList<String>();
		String sql;
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement s = null;
		HashSet<Integer> perkIDs = new HashSet<Integer>();
		HashMap<Integer, Integer> perkCounts = new HashMap<Integer, Integer>();

		try {
			context = SQL.getPerkConnection();	
			
			/* XXX TODO Get better with JOIN statements and do this faster. */
			Integer userID = getUserWebsiteId(resident);
		
			try {
				/* Lookup join table for perks and users. */
				sql = "SELECT `perk_id`,`used`,`used_phase` FROM `userperks` WHERE `user_id` = ?";
				s = context.prepareStatement(sql);
				s.setInt(1, userID);
				
				rs = s.executeQuery();
		
				while (rs.next()) {
					/* 'used' is now deprecated. */
					//Boolean used = rs.getBoolean("used");
					String usedPhase = rs.getString("used_phase");
					if (usedPhase == null) {
						usedPhase = "old";
					}
					
					int id = rs.getInt("perk_id");
					if (!usedPhase.equals(CivGlobal.getPhase())) {
						Integer count = perkCounts.get(id);
						if (count == null) {
							perkCounts.put(id, 1);
						} else {
							perkCounts.put(id, count+1);
						}
						
						perkIDs.add(id);
					}
				}
			} finally {
				SQL.close(rs, s, null);
			}

			try {
				if (perkIDs.size() > 0) {
					/* Finally, look up perk idents. */
					StringBuilder sqlBuild = new StringBuilder("SELECT `id`, `ident` FROM `perks` WHERE id IN (");
					for (Integer id : perkIDs) {				
						sqlBuild.append(""+id+",");
					}
					sqlBuild.setCharAt(sqlBuild.length()-1, ')');
					s = context.prepareStatement(sqlBuild.toString());
					rs = s.executeQuery();
		
					/* Put all of the perk id's into a list that we'll return. */
					while(rs.next()) {
						Integer count = perkCounts.get(rs.getInt("id"));
						for (int i = 0; i < count; i++) {
							perkIdents.add(rs.getString("ident"));
						}
					}
					
					s.close();
					rs.close();
				}
			} finally {
				SQL.close(rs, s, null);
			}
			
			for (String perkID : perkIdents) {
				ConfigPerk configPerk = CivSettings.perks.get(perkID);
				if (configPerk == null) {
					continue;
				}
				
				Perk p2 = resident.perks.get(configPerk.id);
				if (p2 != null) {
					p2.count++;
					resident.perks.put(p2.getIdent(), p2);
				} else {
					Perk p = new Perk(configPerk);
					resident.perks.put(p.getIdent(), p);
				}
			}
			
			return;
		} finally {
			SQL.close(rs, s, context);
		}
	}
	
	public void markAsUsed(Resident resident, Perk parent) throws SQLException, NotVerifiedException {
		Connection context = null;
		PreparedStatement s = null;
		
		try {
			context = SQL.getPerkConnection();	
			Integer userID = getUserWebsiteId(resident);
			Integer perkID = getPerkWebsiteId(parent);
			
			if (perkID == 0) {
				CivLog.error("Couldn't find perk id in website DB. Looking for ident:"+parent.getIdent());
				return;
			}
			
			String sql = "UPDATE `userperks` SET `used_phase` = ? WHERE `user_id` = ? AND `perk_id` = ? AND (`used_phase` IS NULL OR `used_phase` NOT LIKE ?) LIMIT 1";
			s = context.prepareStatement(sql);
			s.setString(1, CivGlobal.getPhase());
			s.setInt(2, userID);
			s.setInt(3, perkID);
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

	private static Integer getPerkWebsiteId(Perk parent) throws SQLException {
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement s = null;
		
		try {
			context = SQL.getPerkConnection();	
			String sql = "SELECT `id` FROM `perks` WHERE `ident` = ?";
			s = context.prepareStatement(sql);
			s.setString(1, parent.getIdent());
			
			rs = s.executeQuery();
			Integer perkID = 0;
			if (rs.next()) {
				perkID = rs.getInt("id");
			}
			return perkID;
		} finally {
			SQL.close(rs, s, context);
		}
	}
	
	public void updatePlatinum(Resident resident, Integer plat) throws SQLException, NotVerifiedException {
		Integer userId = PerkManager.getUserWebsiteId(resident);
		PerkManager.updatePlatinum(userId, plat);
	}
	
	private static void updatePlatinum(Integer userID, Integer plat) throws SQLException {
		Connection context = null;
		PreparedStatement s = null;
		
		try {
			context = SQL.getPerkConnection();	
			String sql = "UPDATE `users` SET `platinum` = `platinum` + ? WHERE `id` = ?";
			s = context.prepareStatement(sql);
			s.setInt(1, plat);
			s.setInt(2, userID);
		
			CivLog.info("Updated Platinum, user:"+userID+" with:"+plat);
			int update = s.executeUpdate();
			if (update != 1) {
				CivLog.error("Failed to update platinum. Updated "+update+" rows when it should have been 1");
			}
			
			return;
		} finally {
			SQL.close(null, s, context);
		}
	}
		
}
