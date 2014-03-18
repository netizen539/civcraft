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
package com.avrgaming.civcraft.command.debug;

import org.bukkit.Bukkit;
import org.bukkit.Effect;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.structure.farm.FarmGrowthSyncTask;
import com.avrgaming.civcraft.structure.farm.FarmPreCachePopulateTimer;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;

public class DebugFarmCommand extends CommandBase {

	@Override
	public void init() {
		command = "/dbg farm ";
		displayName = "Farm Commands";
		
		commands.put("showgrowth", "Highlight the crops that grew last tick.");
		commands.put("grow", "[x] grows ALL farm chunks x many times.");
		commands.put("cropcache", "show the crop cache for this plot.");
		commands.put("unloadchunk", "[x] [z] unloads this farm chunk");
		commands.put("cache", "Runs the crop cache task.");

	}

	public void unloadchunk_cmd() throws CivException {
		
		int x = getNamedInteger(1);
		int z = getNamedInteger(2);
		
		Bukkit.getWorld("world").unloadChunk(x, z);
		CivMessage.sendSuccess(sender, "Chunk "+x+","+z+" unloaded");
	}
	
	public void showgrowth_cmd() throws CivException {
		Player player = getPlayer();
	
		ChunkCoord coord = new ChunkCoord(player.getLocation());
		FarmChunk fc = CivGlobal.getFarmChunk(coord);
		if (fc == null) {
			throw new CivException("This is not a farm.");
		}
		
		for(BlockCoord bcoord : fc.getLastGrownCrops()) {
			bcoord.getBlock().getWorld().playEffect(bcoord.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
		}
		
		CivMessage.sendSuccess(player, "Flashed last grown crops");
	}
	
	
	public void cropcache_cmd() throws CivException {
		Player player = getPlayer();
		
		ChunkCoord coord = new ChunkCoord(player.getLocation());
		FarmChunk fc = CivGlobal.getFarmChunk(coord);
		if (fc == null) {
			throw new CivException("This is not a farm.");
		}
		
		for (BlockCoord bcoord : fc.cropLocationCache) {
			bcoord.getBlock().getWorld().playEffect(bcoord.getLocation(), Effect.MOBSPAWNER_FLAMES, 1);
		}
		CivMessage.sendSuccess(player, "Flashed cached crops.");
	}
	
	public void grow_cmd() throws CivException {
		
		int count = getNamedInteger(1);
		for (int i = 0; i < count; i++) {
			TaskMaster.asyncTask(new FarmGrowthSyncTask(), 0);
		}
		CivMessage.sendSuccess(sender, "Grew all farms.");
	}
	
	public void cache_cmd() {
		TaskMaster.syncTask(new FarmPreCachePopulateTimer());
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		
	}

}
