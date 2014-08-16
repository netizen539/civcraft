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
package com.avrgaming.civcraft.threading.tasks;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;

import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.populators.TradeGoodPick;
import com.avrgaming.civcraft.populators.TradeGoodPopulator;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;

public class TradeGoodPostGenTask implements Runnable {

	String playerName;
	int start;
	
	public TradeGoodPostGenTask(String playerName, int start) {
		this.playerName = playerName;
		this.start = 0;
	}
	
	public void deleteAllTradeGoodiesFromDB() {
		/* Delete all existing trade goods from DB. */
		Connection conn = null;
		PreparedStatement ps = null;
		try {
		try {
			conn = SQL.getGameConnection();
			String code = "TRUNCATE TABLE "+TradeGood.TABLE_NAME;
			ps = conn.prepareStatement(code);
			ps.execute();
		} catch (SQLException e1) {
			e1.printStackTrace();
		}
		} finally {
			if (conn != null) {
				try {
					conn.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
			
			if (ps != null) {
				try {
					ps.close();
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	@Override
	public void run() {
		CivMessage.console(playerName, "Generating/Clearing Trade goods...");
		CivMessage.console(playerName, "|- Organizing trade picks into a Queue.");
		
		deleteAllTradeGoodiesFromDB();
		
		/* Generate Trade Good Pillars. */
		Queue<TradeGoodPick> picksQueue = new LinkedList<TradeGoodPick>();
		for (TradeGoodPick pick : CivGlobal.preGenerator.goodPicks.values()) {
			picksQueue.add(pick);
		}
		
		int count = 0;
		int amount = 20;
		int totalSize = picksQueue.size();
		while (picksQueue.peek() != null) {
			CivMessage.console(playerName, "|- Placing/Picking Goods:"+count+"/"+totalSize+" current size:"+picksQueue.size());
			
			Queue<TradeGoodPick> processQueue = new LinkedList<TradeGoodPick>();
			for (int i = 0; i < amount; i++) {
				TradeGoodPick pick = picksQueue.poll();
				if (pick == null) {
					break;
				}
				
				count++;
				processQueue.add(pick);
			}
			
			TaskMaster.syncTask(new SyncTradeGenTask(processQueue, amount));
			
			try {
				while (processQueue.peek() != null) {
					Thread.sleep(500);
				}
			} catch (InterruptedException e) {
				return;
			}
		}
		
		
		CivMessage.console(playerName, "Finished!");
	}

	class SyncTradeGenTask implements Runnable {
		public Queue<TradeGoodPick> picksQueue;
		public int amount;
		
		public SyncTradeGenTask(Queue<TradeGoodPick> picksQueue, int amount) {
			this.picksQueue = picksQueue;
			this.amount = amount;
		}
		
		@Override
		public void run() {
			World world = Bukkit.getWorld("world");
			BlockCoord bcoord2 = new BlockCoord();

			for(int i = 0; i < amount; i++) {
				TradeGoodPick pick = picksQueue.poll();
				if (pick == null) {
					return;
				}
				
				ChunkCoord coord = pick.chunkCoord;
				Chunk chunk = world.getChunkAt(coord.getX(), coord.getZ());
				
				int centerX = (chunk.getX() << 4) + 8;
				int centerZ = (chunk.getZ() << 4) + 8;
				int centerY = world.getHighestBlockYAt(centerX, centerZ);
				
				
				
				bcoord2.setWorldname("world");
				bcoord2.setX(centerX);
				bcoord2.setY(centerY - 1);
				bcoord2.setZ(centerZ);
				
				/* try to detect already existing trade goods. */
				while(true) {
					Block top = world.getBlockAt(bcoord2.getX(), bcoord2.getY(), bcoord2.getZ());
					
					if (!top.getChunk().isLoaded()) {
						top.getChunk().load();
					}
					
					if (ItemManager.getId(top) == CivData.BEDROCK) {
						ItemManager.setTypeId(top, CivData.AIR);
		    			ItemManager.setData(top, 0, true);
		    			bcoord2.setY(bcoord2.getY() - 1);
		    			
		    			top = top.getRelative(BlockFace.NORTH);
		    			if (ItemManager.getId(top) == CivData.WALL_SIGN) {
		    				ItemManager.setTypeId(top, CivData.AIR);
			    			ItemManager.setData(top, 0, true);	    			
			    		}
		    			
		    			top = top.getRelative(BlockFace.SOUTH);
		    			if (ItemManager.getId(top) == CivData.WALL_SIGN) {
		    				ItemManager.setTypeId(top, CivData.AIR);
			    			ItemManager.setData(top, 0, true);	    			
			    		}
		    			
		    			top = top.getRelative(BlockFace.EAST);
		    			if (ItemManager.getId(top) == CivData.WALL_SIGN) {
		    				ItemManager.setTypeId(top, CivData.AIR);
			    			ItemManager.setData(top, 0, true);	    			
			    		}
		    			
		    			top = top.getRelative(BlockFace.WEST);
		    			if (ItemManager.getId(top) == CivData.WALL_SIGN) {
		    				ItemManager.setTypeId(top, CivData.AIR);
			    			ItemManager.setData(top, 0, true);
			    		}
					} else {
						break;
					}
					
				}
				
				centerY = world.getHighestBlockYAt(centerX, centerZ);
				
				// Determine if we should be a water good.
				ConfigTradeGood good;
				if (ItemManager.getBlockTypeIdAt(world, centerX, centerY-1, centerZ) == CivData.WATER || 
					ItemManager.getBlockTypeIdAt(world, centerX, centerY-1, centerZ) == CivData.WATER_RUNNING) {
					good = pick.waterPick;
				}  else {
					good = pick.landPick;
				}
				
				// Randomly choose a land or water good.
				if (good == null) {
					System.out.println("Could not find suitable good type during populate! aborting.");
					continue;
				}
				
				// Create a copy and save it in the global hash table.
				BlockCoord bcoord = new BlockCoord(world.getName(), centerX, centerY, centerZ);
				TradeGoodPopulator.buildTradeGoodie(good, bcoord, world, true);
				
			}
		}
	}
	
}
