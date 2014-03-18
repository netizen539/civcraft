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
package com.avrgaming.global.bans;

import java.sql.SQLException;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.main.CivMessage;

public class BanAsyncTask implements Runnable {

	CommandSender sender;
	String name;
	String reason;
	int secondsToBan;
	
	public BanAsyncTask(CommandSender sender, String name, String reason,
			int secondsToBan) {
		this.sender = sender;
		this.name = name;
		this.reason = reason;
		this.secondsToBan = secondsToBan;
	}
	
	@Override
	public void run() {
		
		try {
			String banned_by = sender.getName();
			BanEntry entry = BanManager.getBanEntry(name);
			if (entry != null) {
				if (this.secondsToBan > 0 && entry.banned) {
					CivMessage.sendError(sender, "User is already banned.");
					return;
				}
			}
			
			BanManager.banPlayer(name, reason, banned_by, secondsToBan);
			CivMessage.sendSuccess(sender, "Banned "+name+" globally from all servers.");
		
		} catch (SQLException e) {
			CivMessage.sendError(sender, e.getMessage());
		}
		
	}

}
