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
package com.avrgaming.civcraft.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;


public class DebugListener implements Listener {

	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDamageEvent(EntityDamageEvent event) {
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityInteractEvent(EntityInteractEvent event) {
	}

//	@EventHandler(priority = EventPriority.NORMAL)
//	public void OnBlockBreakEvent(BlockBreakEvent event) {
//		CivLog.debug("block break! setting blocs..");
//		
//		Block downBlock = event.getBlock().getRelative(BlockFace.DOWN);
//		while (downBlock.getTypeId() == CivData.AIR) {
//			downBlock = downBlock.getRelative(BlockFace.DOWN);
//		}
//		
//		Block nextBlock = event.getBlock().getRelative(BlockFace.UP);
//		downBlock.getRelative(BlockFace.UP).setTypeIdAndData(nextBlock.getTypeId(), nextBlock.getData(), false);
//		nextBlock = event.getBlock().getRelative(BlockFace.UP);
//
//		World world = nextBlock.getLocation().getWorld();
//		while (nextBlock.getTypeId() != CivData.AIR) {
//			int type = nextBlock.getTypeId();
//			byte data = nextBlock.getData();
//			
//			nextBlock.setTypeIdAndData(CivData.AIR, (byte)0, false);
//			world.spawnFallingBlock(nextBlock.getLocation(), type, data);
//			
//			nextBlock = nextBlock.getRelative(BlockFace.UP);
//		}
//	}
	

	
	
}
