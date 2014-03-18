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
package com.avrgaming.civcraft.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class PayCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
	
		try {
			Player player = CivGlobal.getPlayer(sender.getName());
			Resident resident = CivGlobal.getResident(player);
			if (resident == null) {
				CivMessage.sendError(sender, "Couldn't find yourself... ???");
				return false;
			}
			
			if (args.length < 2) {
				throw new CivException("Enter a player and an amount to pay /pay [player] [amount]");
			}
			
			Resident payTo = CivGlobal.getResident(args[0]);
			if (payTo == null) {
				throw new CivException("Couldn't find player "+args[0]+" to pay.");
			}
			
			if (resident == payTo) {
				throw new CivException("Don't pay yourself.");
			}
			
			Double amount;
			try {
				amount = Double.valueOf(args[1]);
				if (!resident.getTreasury().hasEnough(amount)) {
					throw new CivException("You do not have enough coins.");
				}
			} catch (NumberFormatException e) {
				throw new CivException("Please enter a number.");
			}
						
			if (amount < 1) {
				throw new CivException("Cannot pay someone less than one coin.");
			}
			amount = Math.floor(amount);
			
			resident.getTreasury().withdraw(amount);
			payTo.getTreasury().deposit(amount);
			
			CivMessage.sendSuccess(player, "Paid "+payTo.getName()+" "+amount+" coins");
			
			try {
				Player payToPlayer = CivGlobal.getPlayer(payTo);
				CivMessage.sendSuccess(payToPlayer, "Got "+amount+" coins from "+resident.getName());
			} catch (CivException e) {
				// player not online, forget it.
			}
		} catch (CivException e) {
			CivMessage.sendError(sender, e.getMessage());
			return false;
		}
		return true;
	}

}
