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
package com.avrgaming.global.scores;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.Bukkit;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;

public class ScoreManager {
	
	public static String TOWN_TABLE_NAME = "SCORES_TOWNS";
	public static String CIV_TABLE_NAME = "SCORES_CIVS";
	
	public static String getCivKey(Civilization civ) {
		return Bukkit.getServerName()+":"+civ.getName();
	}
	
	public static String getTownKey(Town town) {
		return Bukkit.getServerName()+":"+town.getName();
	}

	public static void init() throws SQLException {
		System.out.println("================= SCORE_TOWN INIT ======================");
		
		// Check/Build SessionDB tables				
		if (!SQL.hasGlobalTable(TOWN_TABLE_NAME)) {
			String table_create = "CREATE TABLE " + TOWN_TABLE_NAME+" (" + 
					"`key` VARCHAR(128)," +
					"`server` VARCHAR(64)," +
					"`local_id` int(11)," +
					"`local_name` mediumtext," +
					"`local_civ_name` mediumtext,"+
					"`points` int(11)," +
					"INDEX (`server`)," +
					"PRIMARY KEY (`key`)" + ")";
			
			SQL.makeGlobalTable(table_create);
			CivLog.info("Created "+TOWN_TABLE_NAME+" table");
		} else {
			CivLog.info(TOWN_TABLE_NAME+" table OK!");
		}		
				
		System.out.println("==================================================");

		System.out.println("================= SCORE_CIV INIT ======================");
		
		// Check/Build SessionDB tables				
		if (!SQL.hasGlobalTable(CIV_TABLE_NAME)) {
			String table_create = "CREATE TABLE " + CIV_TABLE_NAME+" (" + 
					"`key` VARCHAR(128)," +
					"`server` VARCHAR(64)," +
					"`local_id` int(11)," +
					"`local_name` mediumtext," +
					"`local_capitol_name` mediumtext,"+
					"`points` int(11)," +
					"INDEX (`server`)," +
					"PRIMARY KEY (`key`)" + ")";
			
			SQL.makeGlobalTable(table_create);
			CivLog.info("Created "+CIV_TABLE_NAME+" table");
		} else {
			CivLog.info(CIV_TABLE_NAME+" table OK!");
		}		
				
		System.out.println("==================================================");
	}
	
	public static void UpdateScore(Civilization civ, int points) throws SQLException {
		Connection global_context = null;
		PreparedStatement s = null;

		try {
			global_context = SQL.getGlobalConnection();	
			String query = "INSERT INTO `"+CIV_TABLE_NAME+"` (`key`, `server`, `local_id`, `local_name`, `local_capitol_name`, `points`) "+
					"VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `local_name`=?, `local_capitol_name`=?, `points`=?";
			s = global_context.prepareStatement(query);
		
			s.setString(1, getCivKey(civ));
			s.setString(2, Bukkit.getServerName());
			s.setInt(3, civ.getId());
			s.setString(4, civ.getName());
			s.setString(5, civ.getCapitolName());
			s.setInt(6, points);
			
			s.setString(7, civ.getName());
			s.setString(8, civ.getCapitolName());
			s.setInt(9, points);
			
			int rs = s.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+query);
			}
		
		} finally {
			SQL.close(null, s, global_context);
		}
	}
	
	public static void UpdateScore(Town town, int points) throws SQLException {
		Connection global_context = null;
		PreparedStatement s = null;
		
		try {
			global_context = SQL.getGlobalConnection();	
			String query = "INSERT INTO `"+TOWN_TABLE_NAME+"` (`key`, `server`, `local_id`, `local_name`, `local_civ_name`, `points`) "+
					"VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `local_name`=?, `local_civ_name`=?, `points`=?";
			s = global_context.prepareStatement(query);
		
			s.setString(1, getTownKey(town));
			s.setString(2, Bukkit.getServerName());
			s.setInt(3, town.getId());
			s.setString(4, town.getName());
			s.setString(5, town.getCiv().getName());
			s.setInt(6, points);
			
			s.setString(7, town.getName());
			s.setString(8, town.getCiv().getName());
			s.setInt(9, points);
			
			int rs = s.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+query);
			}
		
		} finally {
			SQL.close(null, s, global_context);
		}
	}
}
