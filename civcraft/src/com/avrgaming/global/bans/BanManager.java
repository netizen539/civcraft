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
package com.avrgaming.global.bans;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Date;
import java.util.LinkedList;

import org.bukkit.Bukkit;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.PlayerKickBan;
import com.avrgaming.civcraft.util.CivColor;

public class BanManager {

	//private static HashMap<String, Boolean> bannedCache = new HashMap<String, Boolean>();
	
	public static String TABLE_NAME = "BANS";
	public static String IP_TABLE_NAME = "IP_BANS";
	public static void init() throws SQLException {
		System.out.println("================= BANS INIT ======================");
		
		// Check/Build SessionDB tables				
		if (!SQL.hasGlobalTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + TABLE_NAME+" (" + 
					"`name` VARCHAR(64)," +
					"`server` mediumtext," +
					"`reason` mediumtext," +
					"`banned_by` mediumtext," +
					"`unbanned_by` mediumtext,"+
					"`unbanned_reason` mediumtext,"+
					"`banned` boolean," +
					"`banned_count` int," +
					"`time` long," +
					"`expires` long," +
					"`muted` boolean," +
					"`muted_by` mediumtext,"+
					"`muted_reason` mediumtext,"+
					"`mute_expires` long,"+
					"PRIMARY KEY (`name`)" + ")";
			
			SQL.makeGlobalTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
			
			//Check for new columns and update the table if we dont have them.
			if (!SQL.hasGlobalColumn(TABLE_NAME, "muted")) {
				CivLog.info("\tCouldn't find muted column for globaldb.");
				SQL.addGlobalColumn(TABLE_NAME, "`muted` boolean");				
			}
			
			if (!SQL.hasGlobalColumn(TABLE_NAME, "muted_by")) {
				CivLog.info("\tCouldn't find muted_by column for globaldb.");
				SQL.addGlobalColumn(TABLE_NAME, "`muted_by` mediumtext");				
			}
			
			if (!SQL.hasGlobalColumn(TABLE_NAME, "muted_reason")) {
				CivLog.info("\tCouldn't find muted_reason column for globaldb.");
				SQL.addGlobalColumn(TABLE_NAME, "`muted_reason` mediumtext");				
			}

			if (!SQL.hasGlobalColumn(TABLE_NAME, "mute_expires")) {
				CivLog.info("\tCouldn't find mute_expires column for globaldb.");
				SQL.addGlobalColumn(TABLE_NAME, "`mute_expires` long");				
			}
		}		
		
		if (!SQL.hasGlobalTable(IP_TABLE_NAME)) {
			String table_create = "CREATE TABLE " + IP_TABLE_NAME+" (" + 
					"`ip` VARCHAR(64)," +
					"`name` mediumtext," +
					"`server` mediumtext," +
					"`reason` mediumtext," +
					"`banned_by` mediumtext," +
					"`unbanned_by` mediumtext,"+
					"`unbanned_reason` mediumtext,"+
					"`banned` boolean," +
					"`banned_count` int," +
					"`time` long," +
					"PRIMARY KEY (`ip`)" + ")";
			SQL.makeGlobalTable(table_create);
			CivLog.info("Created "+IP_TABLE_NAME+" table");
		} else {
			if (!SQL.hasGlobalColumn(IP_TABLE_NAME, "name")) {
				CivLog.info("\tCouldn't find name column for globaldb.");
				SQL.addGlobalColumn(IP_TABLE_NAME, "`name` mediumtext");				
			}
		}
		
		System.out.println("==================================================");

	}
	
	public static boolean isLocalBanned(String name) {
		Resident resident = CivGlobal.getResident(name);
		if (resident == null) {
			return false;
		}
		
		return resident.isBanned();
	}
	
	public static void updateLocalBan(BanEntry entry) {
		if (entry != null) {
			Resident resident = CivGlobal.getResident(entry.name);
			if (resident != null) {
				resident.setBanned(entry.banned);
				resident.setBannedMessage(entry.reason);
				resident.save();
			}
		}
	}
	
	public static void setLocalBan(String name, String reason) {
		Resident resident = CivGlobal.getResident(name);
		if (resident != null) {
			resident.setBanned(true);
			resident.setBannedMessage(reason);
			resident.save();
		}
	}
	
	public static void clearLocalBan(String name) {
		Resident resident = CivGlobal.getResident(name);
		if (resident != null) {
			resident.setBanned(false);
			resident.setBannedMessage("");
			resident.save();
		}
	}
	
	/*
	 * Sends a query to the remote database.
	 * NEVER call this from a sync thread as its very slow.
	 */
	public static BanEntry getBanEntry(String name) throws SQLException {
		Connection global_context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			global_context = SQL.getGlobalConnection();
			String query = "SELECT * FROM `"+TABLE_NAME+"` WHERE `name` = ?";
			ps = global_context.prepareStatement(query);
			ps.setString(1, name);
			rs = ps.executeQuery();
			
			BanEntry retEntry = null;
			
			while (rs.next()) {
				retEntry = new BanEntry();			
				retEntry.name = name;
				retEntry.server = rs.getString("server");
				retEntry.banned_by = rs.getString("banned_by");
				retEntry.unbanned_by = rs.getString("unbanned_by");
				retEntry.unbanned_reason = rs.getString("unbanned_reason");
				retEntry.banned = rs.getBoolean("banned");
				retEntry.muted = rs.getBoolean("muted");
				retEntry.muted_by = rs.getString("muted_by");
				retEntry.muted_reason = rs.getString("muted_reason");
				retEntry.banned_count = rs.getInt("banned_count");
				retEntry.reason = rs.getString("reason");
				retEntry.time = rs.getLong("time");
				retEntry.expires = rs.getLong("expires");
				retEntry.mute_expires = rs.getLong("mute_expires");
			}
			
			updateLocalBan(retEntry);
			return retEntry;
		} finally {
			SQL.close(rs, ps, global_context);
		}
	}
	
	public static BanIPEntry getIPBanEntry(String ipString) throws SQLException {
		Connection global_context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			global_context = SQL.getGlobalConnection();
			String query = "SELECT * FROM `"+IP_TABLE_NAME+"` WHERE `ip` = ?";
			ps = global_context.prepareStatement(query);
			ps.setString(1, ipString);
			rs = ps.executeQuery();
			
			BanIPEntry retEntry = null;
			
			while (rs.next()) {
				retEntry = new BanIPEntry();			
				retEntry.ip = ipString;
				retEntry.name = rs.getString("name");
				retEntry.server = rs.getString("server");
				retEntry.banned_by = rs.getString("banned_by");
				retEntry.unbanned_by = rs.getString("unbanned_by");
				retEntry.unbanned_reason = rs.getString("unbanned_reason");
				retEntry.banned = rs.getBoolean("banned");
				retEntry.banned_count = rs.getInt("banned_count");
				retEntry.reason = rs.getString("reason");
				retEntry.time = rs.getLong("time");
			}
			
			return retEntry;
		} finally {
			SQL.close(rs, ps, global_context);
		}		
	}
	
	public static LinkedList<BanIPEntry> getIPBanEntryByName(String name) throws SQLException {
		Connection global_context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			global_context = SQL.getGlobalConnection();
			String query = "SELECT * FROM `"+IP_TABLE_NAME+"` WHERE `name` = ?";
			ps = global_context.prepareStatement(query);
			ps.setString(1, name.toLowerCase());
			rs = ps.executeQuery();
			
			BanIPEntry retEntry = null;
			LinkedList<BanIPEntry> ipBans = new LinkedList<BanIPEntry>();
			
			while (rs.next()) {
				retEntry = new BanIPEntry();			
				retEntry.ip = rs.getString("ip");
				retEntry.name = rs.getString("name");
				retEntry.server = rs.getString("server");
				retEntry.banned_by = rs.getString("banned_by");
				retEntry.unbanned_by = rs.getString("unbanned_by");
				retEntry.unbanned_reason = rs.getString("unbanned_reason");
				retEntry.banned = rs.getBoolean("banned");
				retEntry.banned_count = rs.getInt("banned_count");
				retEntry.reason = rs.getString("reason");
				retEntry.time = rs.getLong("time");
				ipBans.add(retEntry);
			}
			
			return ipBans;
		} finally {
			SQL.close(rs, ps, global_context);
		}
	}
	
	public static void banIP(Resident resident, String reason, String banned_by) throws SQLException {
		Connection global_context = null;
		PreparedStatement ps = null;
		
		try {
			global_context = SQL.getGlobalConnection();	
			String query = "INSERT INTO `"+IP_TABLE_NAME+"` (`ip`, `name`, `server`, `reason`, `banned_by`, `banned`, `banned_count`, `time`) "+
					"VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `banned`=?, `reason`=?, `banned_by`=?, `time`=?, `banned_count`=`banned_count`+1";
			ps = global_context.prepareStatement(query);
			int i = 1;
			Date now = new Date();
			
			ps.setString(i++, resident.getLastIP());
			ps.setString(i++, resident.getName().toLowerCase());
			ps.setString(i++, Bukkit.getServerName());
			ps.setString(i++, reason);
			ps.setString(i++, banned_by);
			ps.setBoolean(i++, true);
			ps.setInt(i++, 1); //Set banned count to 1 on insert
			ps.setLong(i++, now.getTime());
	
			//If entry exists, set these properties again.
			ps.setBoolean(i++, true);
			ps.setString(i++, reason);
			ps.setString(i++, banned_by);
			ps.setLong(i++, now.getTime());
			
			int rs = ps.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+query);
			}
			
			TaskMaster.syncTask(new PlayerKickBan(resident.getName(), true, false, reason));
		} finally {
			SQL.close(null, ps, global_context);
		}
	}
	
	public static void unbanIP(String name, String unban_reason, String unbanned_by) throws SQLException {
		Connection global_context = null;
		PreparedStatement ps = null;
		
		try {
			global_context = SQL.getGlobalConnection();	
			String query = "UPDATE `"+IP_TABLE_NAME+"` SET `banned`=?, `unbanned_by`=?, `unbanned_reason`=?, `time`=? WHERE `name` = ?";
			ps = global_context.prepareStatement(query);
			int i = 1;
			Date now = new Date();
			
			ps.setBoolean(i++, false);
			ps.setString(i++, unbanned_by);
			ps.setString(i++, unban_reason);
			ps.setLong(i++, now.getTime());
			ps.setString(i++, name.toLowerCase());

			int rs = ps.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+query);
			}
			
		} finally {
			SQL.close(null, ps, global_context);
		}
	}
	public static void banPlayer(String name, String reason, String banned_by, int secondsToBan) throws SQLException {
		
		Connection global_context = null;
		PreparedStatement ps = null;
		
		try {
			global_context = SQL.getGlobalConnection();	
			String query = "INSERT INTO `"+TABLE_NAME+"` (`name`, `server`, `reason`, `banned_by`, `banned`, `banned_count`, `time`, `expires`) "+
					"VALUES (?, ?, ?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `banned`=?, `reason`=?, `banned_by`=?, `time`=?, `expires`=?, `banned_count`=`banned_count`+1";
			ps = global_context.prepareStatement(query);
	
			Date now = new Date();
			
			Date expire = null;
			long expireTime = 0;
			if (secondsToBan != 0) {
				expire = new Date();
				expire.setTime(expire.getTime() + (secondsToBan*1000));
				
				expireTime = expire.getTime();
			}
			
			ps.setString(1, name);
			ps.setString(2, Bukkit.getServerName());
			ps.setString(3, reason);
			ps.setString(4, banned_by);
			ps.setBoolean(5, true);
			ps.setInt(6, 1); //Set banned count to 1 on insert
			ps.setLong(7, now.getTime());
			ps.setLong(8, expireTime);
	
			//If entry exists, set these properties again.
			ps.setBoolean(9, true);
			ps.setString(10, reason);
			ps.setString(11, banned_by);
			ps.setLong(12, now.getTime());
			ps.setLong(13, expireTime);
			
			int rs = ps.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+query);
			}
			
			setLocalBan(name, reason);
			TaskMaster.syncTask(new PlayerKickBan(name, true, false, reason));
		} finally {
			SQL.close(null, ps, global_context);
		}
	}
	
	public static void unbanPlayer(String name, String unbanned_reason, String unbanned_by) throws SQLException {
		Connection global_context = null;
		PreparedStatement s = null;
		
		try {
			global_context = SQL.getGlobalConnection();
			String query = "UPDATE `"+TABLE_NAME+"` SET `banned`=?, `unbanned_by`=?, `unbanned_reason`=?, `time`=? WHERE `name` = ?";
				
			s = global_context.prepareStatement(query);
	
			Date now = new Date();
			s.setBoolean(1, false);
			s.setString(2, unbanned_by);
			s.setString(3, unbanned_reason);
			s.setLong(4, now.getTime());
			s.setString(5, name);
			
			int rs = s.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+s.toString());
			}		
	
			/* Just to make sure they are not banned locally. */
			clearLocalBan(name);
		} finally {
			SQL.close(null, s, global_context);
		}
	}
	
	public static void mutePlayer(String name, String reason, String muted_by, int secondsToMute) throws SQLException {
		Connection global_context = null;
		PreparedStatement s = null;
		
		try {
			global_context = SQL.getGlobalConnection();
			String query = "INSERT INTO `"+TABLE_NAME+"` (`name`, `server`, `muted`, `muted_by`, `muted_reason`, `mute_expires`) "+
					"VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `muted`=?, `muted_by`=?, `muted_reason`=?, `mute_expires`=?";
			s = global_context.prepareStatement(query);
			
			s.setString(1, name);
			s.setString(2, Bukkit.getServerName());
			s.setBoolean(3, true);
			s.setString(4, muted_by);
			s.setString(5, reason);
			
			long expireTime = 0;
			Date expire = null;
			if (secondsToMute != 0) {
				expire = new Date();
				expire.setTime(expire.getTime() + (secondsToMute*1000));
				expireTime = expire.getTime();
				
				Resident resident = CivGlobal.getResident(name);
				resident.setMuteExpires(expire);
				
			}
			s.setLong(6, expireTime);
		
			//If entry exists, set these properties again.
			s.setBoolean(7, true);
			s.setString(8, muted_by);
			s.setString(9, reason);
			s.setLong(10, expireTime);
	
			int rs = s.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+query);
			}
	
			Resident resident = CivGlobal.getResident(name);
			resident.setMuted(true);
			CivMessage.send(resident, CivColor.Rose+"You've been muted by "+muted_by+" for "+reason+". To appeal, create a support ticket at http://civcraft.net/support.");
		} finally {
			SQL.close(null, s, global_context);
		}
	}
	
	public static void unmutePlayer(String name, String muted_by) throws SQLException {

		Connection global_context = null;
		PreparedStatement s = null;
		
		try {
			global_context = SQL.getGlobalConnection();
			String query = "INSERT INTO `"+TABLE_NAME+"` (`name`, `server`, `muted`, `muted_by`, `muted_reason`, `mute_expires`) "+
					"VALUES (?, ?, ?, ?, ?, ?) ON DUPLICATE KEY UPDATE `muted`=?, `muted_by`=?, `muted_reason`=?, `mute_expires`=?";
			s = global_context.prepareStatement(query);
			
			s.setString(1, name);
			s.setString(2, Bukkit.getServerName());
			s.setBoolean(3, false);
			s.setString(4, muted_by);
			s.setString(5, "");
			s.setLong(6, 0);
			
			//If entry exists, set these properties again.
			s.setBoolean(7, false);
			s.setString(8, muted_by);
			s.setString(9, "");
			s.setLong(10, 0);
	
			int rs = s.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+query);
			}
	
			Resident resident = CivGlobal.getResident(name);
			resident.setMuted(false);
			if (muted_by != null) {
				CivMessage.send(resident, CivColor.LightGreen+"You've been unmuted by "+muted_by);
			}
		} finally {
			SQL.close(null, s, global_context);
		}
	}

	
	
}
