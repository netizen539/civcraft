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
package com.avrgaming.global.serverstatus;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.Bukkit;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.war.War;

public class ServerStatusManager {

	
	public static String TABLE_NAME = "SERVERS";
	public static void init() throws SQLException {
		System.out.println("================= SERVERS INIT ======================");
		
		// Check/Build SessionDB tables				
		if (!SQL.hasGlobalTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + TABLE_NAME+" (" + 
					"`name` VARCHAR(64)," +
					"`status` mediumtext," +
					"`online_players` int," +
					"`max_players` int," +
					"`next_war` mediumtext,"+
					"PRIMARY KEY (`name`)" + ")";
			
			SQL.makeGlobalTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
		}		
				
		System.out.println("==================================================");

	}
	
	public static String getServerStatus() {
		try {
			Class.forName("net.shotbow.serverstatusupdater.StatusInfo");
			
			if (CivGlobal.info.online == false) {
				return "Offline";
			}
			
			if (CivGlobal.info.joinable == false) {
				return "Downtime";
			} 
			
			return "Online";			
		} catch (ClassNotFoundException e) {
			return "Online";
		}
	}
	
	public static int getMaxPlayers() {
		try {
			Class.forName("net.shotbow.serverstatusupdater.StatusInfo");	
			return CivGlobal.info.maxPlayers;
		} catch (ClassNotFoundException e) {
			return Bukkit.getMaxPlayers();
		}
	}
	
	
	public static void updateServerStatus() throws SQLException {
		Connection global_context = null;
		PreparedStatement s = null;
		
		try {
			global_context = SQL.getGlobalConnection();	
			String query = "INSERT INTO `"+TABLE_NAME+"` (`name`, `status`, `online_players`, `max_players`, `next_war`) "+
					"VALUES (?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `status`=?, `online_players`=?, `max_players`=?, `next_war`=?";
			s = global_context.prepareStatement(query);
	
			s.setString(1, Bukkit.getServerName());
			s.setString(2, getServerStatus());
			s.setInt(3, Bukkit.getOnlinePlayers().length);
			s.setInt(4, getMaxPlayers());
			
			Date nextWar = War.getNextWarTime();
			SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
			s.setString(5, sdf.format(nextWar));
			
			s.setString(6, getServerStatus());
			s.setInt(7, Bukkit.getOnlinePlayers().length);
			s.setInt(8, getMaxPlayers());
			s.setString(9, sdf.format(nextWar));
			
			int rs = s.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+query);
			}	

		} finally {
			SQL.close(null, s, global_context);
		}
	}
	
	
}
