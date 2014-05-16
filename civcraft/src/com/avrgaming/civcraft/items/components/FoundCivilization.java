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

package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveCivName;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.CivColor;

public class FoundCivilization extends ItemComponent implements CallbackInterface{
	
	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		attrUtil.addLore(ChatColor.RESET+CivColor.Gold+"Founds a Civilization");
		attrUtil.addLore(ChatColor.RESET+CivColor.Rose+"<Right Click To Use>");
		attrUtil.addEnhancement("LoreEnhancementSoulBound", null, null);
		attrUtil.addLore(CivColor.Gold+"Soulbound");
	}
	
	public void foundCiv(Player player) throws CivException {
		
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) {
			throw new CivException("You must be a registered resident to found a civ. This shouldn't happen. Contact an admin.");
		}
			
		/*
		 * Build a preview for the Capitol structure.
		 */
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+"Checking structure position...Please wait.");
		ConfigBuildableInfo info = CivSettings.structures.get("s_capitol");
		Buildable.buildVerifyStatic(player, info, player.getLocation(), this);	
	}
	
	public void onInteract(PlayerInteractEvent event) {
		
		event.setCancelled(true);
		if (!event.getAction().equals(Action.RIGHT_CLICK_AIR) &&
				!event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			return;
		}
		
		class SyncTask implements Runnable {
			String name;
				
			public SyncTask(String name) {
				this.name = name;
			}
			
			@SuppressWarnings("deprecation")
			@Override
			public void run() {
				Player player;
				try {
					player = CivGlobal.getPlayer(name);
					try {
						foundCiv(player);
					} catch (CivException e) {
						CivMessage.sendError(player, e.getMessage());
					}
				} catch (CivException e) {
					return;
				}
				player.updateInventory();
			}
		}
		TaskMaster.syncTask(new SyncTask(event.getPlayer().getName()));
		
	}

	@Override
	public void execute(String playerName) {
		
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		
		Resident resident = CivGlobal.getResident(player);
		
		/* Save the location so we dont have to re-validate the structure position. */
		resident.desiredTownLocation = player.getLocation();
		CivMessage.sendHeading(player, "Founding A New Civ");
		CivMessage.send(player, CivColor.LightGreen+"You and your small band of travelers have finally found the chosen land.");
		CivMessage.send(player, CivColor.LightGreen+"While you are few, will your numbers will grow?");
		CivMessage.send(player, CivColor.LightGreen+"Will you journey boldy forth into new frontiers?");
		CivMessage.send(player, CivColor.LightGreen+"Can you build a Civilization that can stand the test of time?");
		CivMessage.send(player, " ");
		CivMessage.send(player, CivColor.LightGreen+ChatColor.BOLD+"What shall your new Civilization be called?");
		CivMessage.send(player, CivColor.LightGray+"(To cancel, type 'cancel')");
		
		resident.setInteractiveMode(new InteractiveCivName());
	}

	
}
