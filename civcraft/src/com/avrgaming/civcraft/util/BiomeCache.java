package com.avrgaming.civcraft.util;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import org.bukkit.Chunk;
import org.bukkit.block.Biome;

import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.threading.TaskMaster;

public class BiomeCache {
	/*
	 * We need to figure out which "biome" a chunk is when we create a culture chunk.
	 * The problem is, this requires us to load the ENTIRE CHUNK to get at this little
	 * snippet of data. (the biome at a particlar block in a chunk). This can cause us
	 * to load literally gigabytes of extra data for "no reason" other than to find out
	 * what the biome is. Hence this cache.
	 * 
	 */
	public static HashMap<String, String> biomeCache = new HashMap<String, String>();	
	
	public static String TABLE_NAME = "CHUNK_BIOMES";
	public static void init() throws SQLException {
		System.out.println("================= BiomeCache INIT ======================");
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`key` varchar(64) NOT NULL," +
					"`value` mediumtext," +
					"PRIMARY KEY (`key`)" + ")";
			
			SQL.makeTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
		}			
		
		Connection context = null;
		ResultSet rs = null;
		PreparedStatement ps = null;
		
		try {
			int count = 0;
			context = SQL.getGameConnection();		
			ps = context.prepareStatement("SELECT * FROM "+SQL.tb_prefix+TABLE_NAME);
			rs = ps.executeQuery();
	
			while(rs.next()) {
				count++;
				String key = rs.getString("key");
				String value = rs.getString("value");
				biomeCache.put(key, value);
			}
			
			CivLog.info("Loaded "+count+" Biome Cache Entries");
		} finally {
			SQL.close(rs, ps, context);
		}

		System.out.println("==================================================");
	}
	
	public static void saveBiomeInfo(CultureChunk cc) {
		class AsyncTask implements Runnable {
			CultureChunk cc;
			
			public AsyncTask(CultureChunk cc) {
				this.cc = cc;
			}
			
			@Override
			public void run() {
				Connection context = null;
				PreparedStatement ps = null;
				
				try {
					context = SQL.getGameConnection();		
					ps = context.prepareStatement("INSERT INTO `"+SQL.tb_prefix+TABLE_NAME+"` (`key`, `value`) VALUES (?, ?)"+
							" ON DUPLICATE KEY UPDATE `value` = ?");
					ps.setString(1, cc.getChunkCoord().toString());
					ps.setString(2, cc.getBiome().name());
					ps.setString(3, cc.getBiome().name());
				
					int rs = ps.executeUpdate();
					if (rs == 0) {
						CivLog.error("Couldn't update biome cache for key:"+cc.getChunkCoord().toString()+" with value: "+cc.getBiome().name());
					}
					
				} catch (SQLException e) {
					e.printStackTrace();
				} finally {
					if (context != null) {
						try {
							context.close();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
				}
			}
		}
		
		TaskMaster.asyncTask(new AsyncTask(cc), 0);
	}
	
	public static Biome getBiome(CultureChunk cc) {
		if (biomeCache.containsKey(cc.getChunkCoord().toString())) {
			return Biome.valueOf(biomeCache.get(cc.getChunkCoord().toString()));
		} else {
			class SyncTask implements Runnable {
				CultureChunk cc;
				
				public SyncTask(CultureChunk cc) {
					this.cc = cc;
				}
				
				@Override
				public void run() {
					Chunk chunk = cc.getChunkCoord().getChunk();
					cc.setBiome(chunk.getWorld().getBiome((chunk.getX()*16), (chunk.getZ()*16)));
					BiomeCache.saveBiomeInfo(cc);
				}
			}
			
			TaskMaster.syncTask(new SyncTask(cc));
			return Biome.HELL;
		}
	}
	

}
