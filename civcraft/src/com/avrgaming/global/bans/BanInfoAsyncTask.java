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
import com.avrgaming.civcraft.util.CivColor;

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
			CivMessage.send(sender, CivColor.Rose+e.getMessage());
			return;
		}
		ArrayList<String> out = new ArrayList<String>();

		if (entry != null) {
			CivMessage.sendHeading(sender, "BanEntry "+name);
			
			out.add(CivColor.Green+"Muted: "+CivColor.LightGreen+entry.muted+
					CivColor.Green+" MutedBy: "+CivColor.LightGreen+entry.muted_by+
					CivColor.Green+" Reason: "+CivColor.LightGreen+entry.muted_reason);
		
			out.add(CivColor.Green+" Banned: "+CivColor.LightGreen+entry.banned+
					CivColor.Green+" Server: "+CivColor.LightGreen+entry.server);
			
			out.add(CivColor.Green+" BannedBy: "+CivColor.LightGreen+entry.banned_by+
					CivColor.Green+" BanCount: "+CivColor.LightGreen+entry.banned_count);
			
			out.add(CivColor.Green+" Reason: "+CivColor.LightGreen+entry.reason);
			out.add(CivColor.Green+" Unbanned by: "+CivColor.LightGreen+entry.unbanned_by+
					CivColor.Green+" UnbanReason: "+CivColor.LightGreen+entry.unbanned_reason);
			
			SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
			Date bannedTime = new Date();
			bannedTime.setTime(entry.time);
			out.add(CivColor.Green+"Banned On: "+CivColor.LightGreen+sdf.format(bannedTime));
			
			if (entry.expires != 0) {
				Date expiresDate = new Date();
				expiresDate.setTime(entry.expires);
				out.add(CivColor.Green+"Expires: "+CivColor.LightGreen+sdf.format(expiresDate));
			} else {
				out.add(CivColor.Green+"Expires: "+CivColor.Rose+"NEVER");
			}
		}
		
		boolean ipBanned = false;
		try {
			LinkedList<BanIPEntry> ipEntries = BanManager.getIPBanEntryByName(name);
			for (BanIPEntry ipEntry : ipEntries) {
				if (ipEntry.banned) {
					ipBanned = true;
					if (sender.isOp()) {
						out.add(CivColor.LightGray+"IP Banned on "+ipEntry.ip);
					} else {
						break;
					}
				}
			}
			if (ipBanned) {
				out.add(CivColor.Rose+"IP Banned!");
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		if (entry == null && ipBanned == false) {
			out.add(CivColor.Yellow+name+" is not banned!");
		}
		
		CivMessage.send(sender, out);
	}
	
	
	
}
