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
package com.avrgaming.civcraft.threading.timers;

import net.shotbow.serverstatusupdater.ServerStatusUpdater;

import org.bukkit.Bukkit;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.Colors;
import com.avrgaming.civcraft.util.CivColor;

public class ServerStatusTimer implements Runnable {

	@Override
	public void run() {
		synchronized(CivGlobal.info) {
			synchronized (CivGlobal.maxPlayers) {
			
				int maxplayers;
				if (CivGlobal.maxPlayers < 0) {
					maxplayers = Bukkit.getMaxPlayers();
				} else {
					maxplayers = CivGlobal.maxPlayers;
				}
				
				try {
					CivGlobal.info.line1 = CivSettings.getStringBase("server_name");
					if (CivGlobal.info.line1 == null) {
						CivGlobal.info.line1 = "CivCraft";
					}
					
				} catch (InvalidConfiguration e) {
					e.printStackTrace();
				}
				
				CivGlobal.info.line2 = Bukkit.getOnlinePlayers().length + "/"+maxplayers;
				
				if (CivGlobal.info.online) {
					if (CivGlobal.betaOnly) {
						CivGlobal.info.line3 = Colors.LightGray+"Beta Only";
					} else if (CivGlobal.info.joinable) {
						CivGlobal.info.line3 = Colors.LightGreen+"Online!";
					} else {
						CivGlobal.info.line3 = Colors.LightGray+"Downtime";
					}
				} else {
					CivGlobal.info.line3 = Colors.Rose+"Offline";
				}
				
				if (CivGlobal.isCasualMode()) {
					CivGlobal.info.line4 = CivColor.LightGray+"["+CivColor.LightBlue+"Casual Mode"+CivColor.LightGray+"]";
				} else {
					CivGlobal.info.line4 = CivColor.LightGray+"["+CivColor.LightBlue+"PvP Mode"+CivColor.LightGray+"]";
				}
				
				CivGlobal.info.onlinePlayers = Bukkit.getOnlinePlayers().length;
				
				ServerStatusUpdater.updateStatusAsync(CivGlobal.info);
			}
		}
	}

}
