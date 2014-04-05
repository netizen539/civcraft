package com.avrgaming.civcraft.command;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;

public class ShotbowCommand extends CommandBase {

	/*	 
	 * This class provides shotbow integration with the ServerStatusUpdater plugin.
	 **/
	
	@Override
	public void init() {
		command = "/sb";
		displayName = "Shotbow";
		
		commands.put("maxplayers", "[value] - sets max players to this amount.");
		commands.put("online", "makes the server online");
		commands.put("offline", "makes the server offline");
		commands.put("joinon", "makes the game joinable");
		commands.put("joinoff", "makes the game not joinable");	
	}

	public void online_cmd() {
		if (CivGlobal.info != null) {
			synchronized(CivGlobal.info) {
				CivGlobal.info.online = true;
			}
			CivMessage.sendSuccess(sender, "Server now appears online");
		}
	}

	public void offline_cmd() {
		if (CivGlobal.info != null) {
			synchronized(CivGlobal.info) {
				CivGlobal.info.online = false;
			}
			CivMessage.sendSuccess(sender, "Server now appears offline");
		}
	}
	
	public void joinon_cmd() {
		if (CivGlobal.info != null) {
			synchronized(CivGlobal.info) {
				CivGlobal.info.joinable = true;
			}
			CivMessage.sendSuccess(sender, "Players can now join from shotbow");
		}
	}

	public void joinoff_cmd() {
		if (CivGlobal.info != null) {
			synchronized(CivGlobal.info) {
				CivGlobal.info.joinable = false;
			}
			CivMessage.sendSuccess(sender, "Players can no longer join from shotbow");
		}
	}
	
	public void maxplayers_cmd() throws CivException {
	if (args.length < 2) {
		CivMessage.send(sender, "Max players is: "+CivGlobal.maxPlayers);
		return;
	}
	
	try {
		Integer size = Integer.valueOf(args[1]);
		synchronized (CivGlobal.maxPlayers) {
			CivGlobal.maxPlayers = size;
		}
		
		if (CivGlobal.info != null) {
			synchronized (CivGlobal.info) {
				CivGlobal.info.maxPlayers = size;
			}
			
			CivMessage.sendSuccess(sender, "Set Max players to "+size);
		}
	} catch (NumberFormatException e) {
		throw new CivException ("enter a number");
	}
	
}
	
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		if (sender instanceof Player) {
			if (((Player)sender).hasPermission(CivSettings.MINI_ADMIN)) {
				return;
			}
		}
		
		if (sender.isOp() == false) {
			throw new CivException("Only admins can use this command.");			
		}		
	}

}
