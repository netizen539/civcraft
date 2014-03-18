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
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.main.Colors;

public class BanInfoAsyncTask implements Runnable {

	CommandSender sender;
	String name;
	
	public BanInfoAsyncTask(CommandSender sender, String name) {
		this.sender = sender;
		this.name = name;
	}

	@Override
	public void run() {
		BanEntry entry;
		try {
			entry = BanManager.getBanEntry(name);
		} catch (SQLException e) {
			CivMessage.send(sender, Colors.Rose+e.getMessage());
			return;
		}
		ArrayList<String> out = new ArrayList<String>();

		if (entry != null) {
			CivMessage.sendHeading(sender, "BanEntry "+name);
			
			out.add(Colors.Green+"Muted: "+Colors.LightGreen+entry.muted+
					Colors.Green+" MutedBy: "+Colors.LightGreen+entry.muted_by+
					Colors.Green+" Reason: "+Colors.LightGreen+entry.muted_reason);
		
			out.add(Colors.Green+" Banned: "+Colors.LightGreen+entry.banned+
					Colors.Green+" Server: "+Colors.LightGreen+entry.server);
			
			out.add(Colors.Green+" BannedBy: "+Colors.LightGreen+entry.banned_by+
					Colors.Green+" BanCount: "+Colors.LightGreen+entry.banned_count);
			
			out.add(Colors.Green+" Reason: "+Colors.LightGreen+entry.reason);
			out.add(Colors.Green+" Unbanned by: "+Colors.LightGreen+entry.unbanned_by+
					Colors.Green+" UnbanReason: "+Colors.LightGreen+entry.unbanned_reason);
			
			SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
			Date bannedTime = new Date();
			bannedTime.setTime(entry.time);
			out.add(Colors.Green+"Banned On: "+Colors.LightGreen+sdf.format(bannedTime));
			
			if (entry.expires != 0) {
				Date expiresDate = new Date();
				expiresDate.setTime(entry.expires);
				out.add(Colors.Green+"Expires: "+Colors.LightGreen+sdf.format(expiresDate));
			} else {
				out.add(Colors.Green+"Expires: "+Colors.Rose+"NEVER");
			}
		}
		
		boolean ipBanned = false;
		try {
			LinkedList<BanIPEntry> ipEntries = BanManager.getIPBanEntryByName(name);
			for (BanIPEntry ipEntry : ipEntries) {
				if (ipEntry.banned) {
					ipBanned = true;
					if (sender.isOp()) {
						out.add(Colors.LightGray+"IP Banned on "+ipEntry.ip);
					} else {
						break;
					}
				}
			}
			if (ipBanned) {
				out.add(Colors.Rose+"IP Banned!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (entry == null && ipBanned == false) {
			out.add(Colors.Yellow+name+" is not banned!");
		}
		
		CivMessage.send(sender, out);
	}
	
	
	
}
