package com.avrgaming.civregister;

import java.sql.SQLException;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class RegCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		
		String playerName;
		String code;
		if (sender instanceof Player) {
			if (args.length != 1) {
				sender.sendMessage(ChatColor.RED+"Enter a valid registration code.");
				return false;
			}
			playerName = sender.getName();
			code = args[0];
		} else {
			if (args.length != 2) {
				sender.sendMessage(ChatColor.RED+"Enter a valid player name and registration code.");
				return false;
			}
			playerName = args[0];
			code = args[1];
		}
		
		try {
			if (SQL.sendVerification(code, playerName)) {
				sender.sendMessage(ChatColor.GREEN+"Verification success. You can now earn platinum and use perks.");
				return true;
			}
		} catch (SQLException e) {
			e.printStackTrace();
		} 
		
		sender.sendMessage(ChatColor.RED+"Verification Failure. Please try again.");
		return false;
	}

}
