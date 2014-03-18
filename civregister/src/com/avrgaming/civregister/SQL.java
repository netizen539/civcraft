package com.avrgaming.civregister;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;

import org.bukkit.plugin.java.JavaPlugin;

public class SQL {

	public static String hostname = "";
	public static String port = "";
	public static String db_name = "";
	public static String username = "";
	public static String password = "";
	
	private static String dsn = "";
	public static Connection context = null;
	
	public static String salt = "";
	
	public static void init(JavaPlugin plugin) throws SQLException {
		hostname = plugin.getConfig().getString("mysql.hostname");
		port = plugin.getConfig().getString("mysql.port");
		db_name = plugin.getConfig().getString("mysql.db_name");
		username = plugin.getConfig().getString("mysql.username");
		password = plugin.getConfig().getString("mysql.password");
		salt = plugin.getConfig().getString("salt");
		
		dsn = "jdbc:mysql://"+hostname+":"+port+"/"+db_name;
		connect();
	}
	
	public static void connect() throws SQLException {
		if (context == null || context.isClosed()) {
			context = DriverManager.getConnection(dsn, username, password);
		}
		
		if (context == null || context.isClosed()) {
			throw new SQLException("Lost context to MYSQL server!");
		}
	}
	
	public static boolean sendVerification(String code, String playerName) throws SQLException {
		connect();
		
		String sql = "UPDATE `users` SET `returnCode`=?, `game_name`=?, `verified`=? WHERE `verifyCode` = ?";
		PreparedStatement ps = SQL.context.prepareStatement(sql);
		ps.setString(1, code);
		ps.setString(2, playerName);
		ps.setBoolean(3, true);
		ps.setString(4, code);
		
		if (ps.executeUpdate() == 1) {
			ps.close();
			return true;
		}
		
		ps.close();		
		return false;
	}
	
	
    private static String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest mDigest = MessageDigest.getInstance("SHA1");
        byte[] result = mDigest.digest(input.getBytes());
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < result.length; i++) {
            sb.append(Integer.toString((result[i] & 0xff) + 0x100, 16).substring(1));
        }
         
        return sb.toString();
    }
	
	public static boolean changePassword(String newPassword, String playerName) throws SQLException {
		connect();
		
		String sql = "UPDATE `users` SET `password`=? WHERE `game_name` = ?";
		PreparedStatement ps = SQL.context.prepareStatement(sql);
		
		String hashedPassword = salt + newPassword;
		try {
			hashedPassword = sha1(hashedPassword);
		} catch (NoSuchAlgorithmException e) {
			e.printStackTrace();
			return false;
		}
		
		ps.setString(1, hashedPassword);
		ps.setString(2, playerName);
		
		if (ps.executeUpdate() == 1) {
			ps.close();
			return true;
		}
		
		ps.close();		
		return false;		
	}
}
