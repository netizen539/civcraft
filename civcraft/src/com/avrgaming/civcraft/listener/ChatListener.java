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
import org.bukkit.event.player.AsyncPlayerChatEvent;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;

public class ChatListener implements Listener {

	@EventHandler(priority = EventPriority.HIGHEST)
	void OnPlayerAsyncChatEvent(AsyncPlayerChatEvent event) {
		
		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (resident == null) {
			/* resident not found, I guess just let the chat through. */
			return;
		}
		
		if (resident.isTownChat()) {
			event.setCancelled(true);
			if (resident.getTownChatOverride() == null) {
				CivMessage.sendTownChat(resident.getTown(), resident, event.getFormat(), event.getMessage());
			} else {
				CivMessage.sendTownChat(resident.getTownChatOverride(), resident, event.getFormat(), event.getMessage());
			}
		}
		
		if (resident.isCivChat()) {
			Civilization civ;
			event.setCancelled(true);
			if (resident.getTown() == null) {
				civ = null;
			} else {
				civ = resident.getTown().getCiv();
			}
			
			if (resident.getCivChatOverride() == null) {
				CivMessage.sendCivChat(civ, resident, event.getFormat(), event.getMessage());
			} else {
				CivMessage.sendCivChat(resident.getCivChatOverride(), resident, event.getFormat(), event.getMessage());
			}
		}
		
		if (resident.isInteractiveMode()) {
			resident.getInteractiveResponse().respond(event.getMessage(), resident);
			event.setCancelled(true);
		}
		
	//	CivLog.debug("Got message:"+event.getMessage());
		//event.setFormat("[[[%s %s]]]");
	}
	
}
