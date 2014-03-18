package com.avrgaming.global.bans;

import java.sql.SQLException;

import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class BanIPAsyncTask implements Runnable {
	CommandSender sender;
	String name;
	String reason;
	boolean banned;
	int secondsToBan;
	
	public BanIPAsyncTask(CommandSender sender, String name, String reason,
			int secondsToBan, boolean banned) {
		this.sender = sender;
		this.name = name;
		this.reason = reason;
		this.secondsToBan = secondsToBan;
		this.banned = banned;
	}
	
	@Override
	public void run() {
		
		try {
			String banned_by = sender.getName();
			if (banned) {
					Resident resident = CivGlobal.getResident(name);
					if (resident == null) {
						CivMessage.sendError(sender, "No resident for "+name);
						return;
					}
					BanManager.banIP(resident, reason, banned_by);
					CivMessage.sendSuccess(sender, "Banned "+resident.getName()+" with IP "+resident.getLastIP()+" globally from all servers.");
				
			} else {
				BanManager.unbanIP(name, reason, banned_by);
				CivMessage.sendSuccess(sender, "Unbanned IPs for "+name+" globally from all servers.");
			}
		} catch (SQLException e) {
			CivMessage.sendError(sender, e.getMessage());
		}
		
	}
}
