package com.avrgaming.global.reports;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.Date;

import org.bukkit.Bukkit;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.threading.TaskMaster;

public class ReportManager {
	
	public enum ReportType {
		LANGUAGE,
		EXPLOITING,
		HARASSMENT,
		CHEATING,
		OTHER
	}
	
	public static String TABLE_NAME = "REPORTS";
	public static void init() throws SQLException {
		System.out.println("================= REPORTS INIT ======================");
		
		// Check/Build SessionDB tables				
		if (!SQL.hasGlobalTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + TABLE_NAME+" (" +
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`name` mediumtext," +
					"`server` mediumtext," +
					"`report_type` mediumtext," +
					"`message` mediumtext," +
					"`reported_by` mediumtext," +
					"`time` long," +
					"PRIMARY KEY (`id`)" + ")";
			
			SQL.makeGlobalTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
		}		
				
		System.out.println("==================================================");
	}
	
	public static String getReportTypes() {
		String out = "";
		for (ReportType type : ReportType.values()) {
			out += type.name().toLowerCase()+", ";
		}
		return out;
	}
		
	public static void reportPlayer(String name, ReportType type, String message, String reportedBy) {
		
		class AsyncTask implements Runnable {
			String name;
			String reportType;
			String message;
			String reportedBy;
			
			public AsyncTask(String name, String reportType, String message, String reportedBy) {
				this.name = name;
				this.reportType = reportType;
				this.message = message;
				this.reportedBy = reportedBy;
			}
			
			@Override
			public void run() {
				try {
					ReportManager.reportPlayerSync(name, reportType, message, reportedBy);
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
		TaskMaster.asyncTask(new AsyncTask("player:"+name, type.name(), message, reportedBy), 0);
		
	}
	
	private static void reportPlayerSync(String name, String reportType, String message, String reportedBy) throws SQLException {
		Connection global_context = null;
		PreparedStatement s = null;
		
		try {
			global_context = SQL.getGlobalConnection();
			String query = "INSERT INTO `"+TABLE_NAME+"` (`name`, `server`, `report_type`, `message`, `reported_by`, `time`) "+
					"VALUES (?, ?, ?, ?, ?, ?)";
			s = global_context.prepareStatement(query);
	
			Date now = new Date();
			
			s.setString(1, name);
			s.setString(2, Bukkit.getServerName());
			s.setString(3, reportType);
			s.setString(4, message);
			s.setString(5, reportedBy);
			s.setLong(6, now.getTime());
		
			int rs = s.executeUpdate();
			if (rs == 0) {
				throw new SQLException("Could not execute SQL code:"+query);
			}
		} finally {
			SQL.close(null, s, global_context);
		}
	}
	
	
}
