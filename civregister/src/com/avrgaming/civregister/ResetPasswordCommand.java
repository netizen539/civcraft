package com.avrgaming.civregister;

import java.sql.SQLException;
import java.util.Random;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ResetPasswordCommand implements CommandExecutor {

	public String generateRandomPassword() {
		String pool = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890";
		int size = (new Random().nextInt(4))+6;
		
		String out = "";
		for (int i = 0; i < size; i++) {
			Random rand = new Random();
			int c = rand.nextInt(pool.length());
			
			out += pool.charAt(c);
		}
		
		return out;
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String label,
			String[] args) {
		
		String playerName;
	    String newPassword = generateRandomPassword();
	    
		if (sender instanceof Player) {
			if (args.length != 1) {
				sender.sendMessage(""+ChatColor.YELLOW+ChatColor.BOLD+"Are you sure you want to reset your password for "+
							ChatColor.GREEN+"http://civcraft.net/store"+ChatColor.YELLOW+ChatColor.BOLD+"?");
				
				sender.sendMessage(""+ChatColor.YELLOW+ChatColor.BOLD+"Type '/resetpassword yes' to reset your password.");
				return false;
			}
			
			if (!args[0].equalsIgnoreCase("yes")) {
				sender.sendMessage(""+ChatColor.RED+"You must use /resetpassword 'yes' to confirm.");
			}
			
			playerName = ((Player)sender).getName();
		} else {
			if (args.length != 1) {
				sender.sendMessage(""+ChatColor.RED+"Enter a playername to reset their website password.");
				return false;
			}
			
			playerName = args[0];
		}
		
		try {
			if (!SQL.changePassword(newPassword, playerName)) {
				sender.sendMessage(""+ChatColor.RED+"Password reset failed.");
				return false;
			} else {
				sender.sendMessage(""+ChatColor.GREEN+"Reset password to: "+newPassword);
			}
		} catch (SQLException e) {
			e.printStackTrace();
			sender.sendMessage(""+ChatColor.RED+"Internal DB Error.");
			return false;
		}
		
		return true;
	}
}
