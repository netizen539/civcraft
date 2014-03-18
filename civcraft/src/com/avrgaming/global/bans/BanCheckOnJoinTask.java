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
import java.util.Date;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.PlayerKickBan;
import com.avrgaming.civcraft.util.CivColor;

public class BanCheckOnJoinTask implements Runnable {

	String name;
	
	public BanCheckOnJoinTask(String name) {
		this.name = name;
	}

	@Override
	public void run() {
		/* IP ban checks */
		Player player;
		try {
			
			player = CivGlobal.getPlayer(name);
			if (player.isOp()) {
				return;
			}
			
			BanIPEntry ipBan = BanManager.getIPBanEntry(player.getAddress().getAddress().getHostAddress());

			if (ipBan != null && ipBan.banned) {
				TaskMaster.syncTask(new PlayerKickBan(name, true, false, ipBan.reason));
			}
		} catch (CivException e1) {
			// Player logged out?
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		
		try {
			BanEntry entry = BanManager.getBanEntry(name);
			
			if (entry == null) {
				return;
			}
			
			if (entry.banned) {
				if (entry.expires != 0) {
					Date now = new Date();
					Date expires = new Date();
					
					expires.setTime(entry.expires);
					if (now.after(expires)) {
						BanManager.unbanPlayer(name, "Ban Expired", Bukkit.getServerName());
						return;
					} else {
						SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
						entry.reason += " Your ban expires on "+sdf.format(expires);
					}
				}
				
				TaskMaster.syncTask(new PlayerKickBan(name, true, false, entry.reason));
				return;
			}
			
			if (entry.muted) {
				Resident resident = CivGlobal.getResident(entry.name);
				resident.setMuted(true);
				
				if (entry.mute_expires != 0) {
					Date muteExpires = new Date(entry.mute_expires);
					resident.setMuteExpires(muteExpires);
				}
				
				if (resident.isMuted()) {
					CivMessage.send(resident, CivColor.Rose+"You've been muted on CivCraft. File a support ticket at http://civcraft.net/support to get it lifted.");
				}
			}
			
		} catch (SQLException e) {
			e.printStackTrace();
			return;
		}
	}
	
}
