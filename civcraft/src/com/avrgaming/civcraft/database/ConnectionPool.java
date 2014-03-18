package com.avrgaming.civcraft.database;

import java.sql.Connection;
import java.sql.SQLException;

import com.jolbox.bonecp.BoneCP;
import com.jolbox.bonecp.BoneCPConfig;
import com.jolbox.bonecp.Statistics;

public class ConnectionPool {

	BoneCP pool;
	
	public static void loadClass(String name) {
		//File file = new File("CivCraft/lib");
		
	}
	
	public static void init() throws ClassNotFoundException {
		/* Load any dependent classes. */
		
		/* load the database driver */
        Class.forName("com.mysql.jdbc.Driver");
	}
	
	
	public ConnectionPool(String dbcUrl, String user, String pass, int minConns,
			int maxConns, int partCount) throws ClassNotFoundException, SQLException {
		/*
		 * Initialize our connection pool.
		 * 
		 * We'll use a connection pool and reuse connections on a per-thread basis. 
		 */
				
		/* setup the connection pool */
		BoneCPConfig config = new BoneCPConfig();
		config.setJdbcUrl(dbcUrl); 
		config.setUsername(user); 
		config.setPassword(pass);
		config.setMinConnectionsPerPartition(minConns);
		config.setMaxConnectionsPerPartition(maxConns);
		config.setPartitionCount(partCount);
		// Enable only for debugging.
		//config.setCloseConnectionWatch(true);
		
		pool = new BoneCP(config);
	}
	
	public Connection getConnection() throws SQLException {
		return pool.getConnection();
	}
	
	
	public Statistics getStats() {
		return pool.getStatistics();
	}
	
	public void shutdown() {
		pool.shutdown();
	}
	
	public void setMaxConnections(int max) {
		pool.getConfig().setMaxConnectionsPerPartition(max);
	}
	
	public int getMaxConnections() {
		return pool.getConfig().getMaxConnectionsPerPartition();
	}
}
