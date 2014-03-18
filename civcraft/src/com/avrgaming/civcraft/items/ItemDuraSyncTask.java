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
package com.avrgaming.civcraft.items;

import java.util.LinkedList;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.listener.CustomItemManager;
import com.avrgaming.civcraft.main.CivGlobal;

public class ItemDuraSyncTask implements Runnable {

	@SuppressWarnings("deprecation")
	@Override
	public void run() {
		
		for (String playerName : CustomItemManager.itemDuraMap.keySet()) {
			Player player;
			try {
				player = CivGlobal.getPlayer(playerName);
			} catch (CivException e) {
				continue;
			}
			
			LinkedList<ItemDurabilityEntry> entries = CustomItemManager.itemDuraMap.get(playerName);
			
			for (ItemDurabilityEntry entry : entries) {
				entry.stack.setDurability(entry.oldValue);
			}
			
			player.updateInventory();
		}
		
		CustomItemManager.duraTaskScheduled = false;
	}
}