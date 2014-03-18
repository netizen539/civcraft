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
package com.avrgaming.civcraft.threading.sync;

public class SyncUpdateChunks implements Runnable {

	
	//public static HashSet<ChunkCoord> updateChunks = new HashSet<ChunkCoord>();

	/*
	 * XXX This is not going to be used anymore. the "refreshChunk" has a bug in it that
	 * prevents players from being able to hit each other when it finishes a refresh. 
	 * 
	 */
	
	@Override
	public void run() {

//		int viewDistance = Bukkit.getViewDistance();
//		
//		ChunkCoord playerChunkCoord = new ChunkCoord();
//		
//		for (ChunkCoord c : updateChunks) {
//			CivLog.debug("Updating chunks c:"+c.toString());
//			
//		//	World world = Bukkit.getWorld(c.getWorldname());
//		//	world.refreshChunk(c.getX(), c.getZ());
//			
//		//	for (Player p : Bukkit.getOnlinePlayers()) {
//			///	playerChunkCoord.setFromLocation(p.getLocation());
//				
//			//	if (c.distance(playerChunkCoord) < viewDistance) {
//					//CivLog.debug("\tskipping...");
//					//CivGlobal.nms.queueChunkForUpdate(p, c.getX(), c.getZ());
//				//	p.g
//			//	}
//			//}
//		}
//	
//		updateChunks.clear();
	}
}
