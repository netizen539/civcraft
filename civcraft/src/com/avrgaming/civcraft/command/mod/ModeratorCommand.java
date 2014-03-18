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
package com.avrgaming.civcraft.command.mod;

import java.sql.SQLException;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.global.bans.BanAsyncTask;
import com.avrgaming.global.bans.BanIPAsyncTask;
import com.avrgaming.global.bans.BanInfoAsyncTask;
import com.avrgaming.global.bans.BanManager;
import com.avrgaming.global.bans.UnbanAsyncTask;

public class ModeratorCommand extends CommandBase {

	@Override
	public void init() {
		command = "/mod";
		displayName = "Moderator";
		
		commands.put("ban", "[name] [reason] Globally bans player from all CivCraft servers. Admins Only.");
		commands.put("tempban", "[name] [hours] [reason] - Temp bans this player for this many hours globally.");
		commands.put("unban", "[name] [reason] Globally unbans players from all CivCraft servers. Admins Only.");
		commands.put("baninfo", "[name] Shows ban information on this player.");
		commands.put("allchat", "Toggles your ability to see all 'global' chat.");
		
		commands.put("mute", "[name] [reason] Globally mutes this player from all CivCraft servers.");
		commands.put("tempmute", "[name] [hours] [reason] - Temp mutes this player for this many hours globally.");
		commands.put("unmute", "[name] Globally unmutes this player from all CivCraft servers.");
		commands.put("banip", "[name] [reason] Globally bans this IP address from all servers.");
		commands.put("unbanip", "[name] [reason] Globally unbans this player's IP address from all servers.");

	}
	
	public void mute_cmd() throws CivException {
		Resident muter = getResident();
		Resident resident = getNamedResident(1);
		String reason = this.combineArgs(this.stripArgs(args, 2));
		_mute(muter, resident, reason, 0);
	}
	
	public void tempmute_cmd() throws CivException {
		Resident muter = getResident();
		Resident resident = getNamedResident(1);
		Integer hours = getNamedInteger(2);
		String reason = this.combineArgs(this.stripArgs(args, 3));
		_mute(muter, resident, reason, hours);
	}
	
	private void _mute(Resident muter, Resident resident, String reason, int secondsToMute) throws CivException {

		if (resident.isMuted()) {
			throw new CivException(resident.getName()+" is already muted.");
		}
		
		class AsyncTask implements Runnable {
			String reason;
			Resident muteMe;
			Resident muter;
			int secondsToMute;
			
			public AsyncTask(String reason, Resident muteMe, Resident muter, int secondsToMute) {
				this.reason = reason;
				this.muteMe = muteMe;
				this.muter = muter;
				this.secondsToMute = secondsToMute;
			}
			
			@Override
			public void run() {
				try {
					BanManager.mutePlayer(muteMe.getName(), reason, muter.getName(), secondsToMute);
				} catch (SQLException e) {
					e.printStackTrace();
					return;
				}	
				
				CivMessage.sendSuccess(muter, "Muted "+muteMe.getName()+" from all CivCraft servers.");
				Player player;
				try {
					player = CivGlobal.getPlayer(muter);
					if (!player.hasPermission(CivSettings.MINI_ADMIN)) {
						CivMessage.global(CivColor.LightGray+"Moderator "+muter.getName()+" muted "+muteMe.getName()+". Reason: "+reason);
					}
				} catch (CivException e) {
					// Muter not online anymore?
				}				
			}
		}
		
		TaskMaster.asyncTask(new AsyncTask(reason, resident, muter, secondsToMute), 0);
	}
	
	public void unmute_cmd() throws CivException {
		Resident unmuter = getResident();
		Resident resident = getNamedResident(1);
		
		class AsyncTask implements Runnable {
			Resident unmuteMe;
			Resident unmuter;
			
			public AsyncTask(Resident unmuteMe, Resident unmuter) {
				this.unmuteMe = unmuteMe;
				this.unmuter = unmuter;
			}
			
			@Override
			public void run() {
				try {
					BanManager.unmutePlayer(unmuteMe.getName(), unmuter.getName());
				} catch (SQLException e) {
					e.printStackTrace();
					return;
				}	
				
				CivMessage.sendSuccess(unmuter, "Unmuted "+unmuteMe.getName()+" from all CivCraft servers.");
				Player player;
				try {
					player = CivGlobal.getPlayer(unmuter);
					if (!player.hasPermission(CivSettings.MINI_ADMIN)) {
						CivMessage.global(CivColor.LightGray+"Moderator "+unmuter.getName()+" unmuted "+unmuteMe.getName()+".");
					}
				} catch (CivException e) {
					// Muter not online anymore?
				}				
			}
		}
		
		TaskMaster.asyncTask(new AsyncTask(resident, unmuter), 0);
	}
	
	
	public void allchat_cmd() throws CivException {
		Resident resident = getResident();
		
		resident.allchat = !resident.allchat;		
		
		if (resident.allchat) {
			Resident.allchatters.add(resident.getName());
		} else {
			Resident.allchatters.remove(resident.getName());
		}
		
		CivMessage.sendSuccess(sender, "Toggled allchat to "+resident.allchat);
	}
	
	public void banip_cmd() throws CivException {
		if (sender instanceof Player) {
			Player player = getPlayer();
			if (!player.hasPermission(CivSettings.MINI_ADMIN)) {
				throw new CivException("You do not have permission to use this command.");
			}
		}
		
		String playerName = args[1];
		String reason = this.combineArgs(this.stripArgs(args, 2));
		TaskMaster.asyncTask(new BanIPAsyncTask(sender, playerName, reason, 0, true), 0);
	}
	
	public void unbanip_cmd() throws CivException {
		if (sender instanceof Player) {
			Player player = getPlayer();
			if (!player.hasPermission(CivSettings.MINI_ADMIN)) {
				throw new CivException("You do not have permission to use this command.");
			}
		}
		
		String playerName = args[1];
		String reason = this.combineArgs(this.stripArgs(args, 2));
		TaskMaster.asyncTask(new BanIPAsyncTask(sender, playerName, reason, 0, false), 0);
	}
	
	public void ban_cmd() throws CivException {
		
		if (sender instanceof Player) {
			Player player = getPlayer();
			if (!player.hasPermission(CivSettings.MINI_ADMIN)) {
				throw new CivException("You do not have permission to use this command.");
			}
		}
		
		if (args.length < 3) {
			throw new CivException("You must provide a player name and a reason for the ban.");
		}
		
		String playerName = args[1];
		String reason = this.combineArgs(this.stripArgs(args, 2));
		TaskMaster.asyncTask(new BanAsyncTask(sender, playerName, reason, 0), 0);
	}

	public void tempban_cmd() throws CivException {
		

		String playerName = getNamedString(1, "Provide a player name to temp ban.");
		Integer hours = getNamedInteger(2);
		String reason = this.combineArgs(this.stripArgs(args, 3));
		
		if (hours < 1 || hours > 24) {
			throw new CivException("Can only temp ban between 1 and 24 hours.");
		}
		
		TaskMaster.asyncTask(new BanAsyncTask(sender, playerName, reason, hours*60*60), 0);
		
		if (sender instanceof Player) {		
			Player player = getPlayer();
			if (!player.hasPermission(CivSettings.MINI_ADMIN)) {
				CivMessage.global(CivColor.LightGray+"Moderator "+player.getName()+" temp-banned "+playerName+" for hours hours. Reason: "+reason);
			}
		}
	}
	
	public void unban_cmd() throws CivException {

		if (args.length < 3) {
			throw new CivException("You must provide a player name and a reason for the unban.");
		}
		
		String playerName = args[1];
		String unban_reason = this.combineArgs(this.stripArgs(args, 2));
		
		if (sender instanceof Player) {
			Player player = getPlayer();
			if (!player.hasPermission(CivSettings.MINI_ADMIN)) {
				CivMessage.global(CivColor.LightGray+"Moderator "+player.getName()+" un-banned "+playerName+". Reason: "+unban_reason);
			}
		}
		
		TaskMaster.asyncTask(new UnbanAsyncTask(sender, playerName, unban_reason), 0);
	}
	
	public void baninfo_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("You must provide a player name.");
		}
		
		String playerName = args[1];
		TaskMaster.asyncTask(new BanInfoAsyncTask(sender, playerName), 0);
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
			if (((Player)sender).hasPermission(CivSettings.MODERATOR)) {
				return;
			}
		}
		
		if (sender.isOp() == false) {
			throw new CivException("Only moderators can use this command.");			
		}		
	}

}
