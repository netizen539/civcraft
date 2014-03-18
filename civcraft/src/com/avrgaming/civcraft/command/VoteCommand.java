package com.avrgaming.civcraft.command;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;

public class VoteCommand implements CommandExecutor {

	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		if (args.length < 1) {
			CivMessage.sendError(sender, "/vote [civ name] - votes for your favorite civ for a diplomatic victory!");
			return false;
		}

		if (sender instanceof Player) {
			Player player = (Player)sender;
			Resident resident = CivGlobal.getResident(player);
			
			if (!resident.hasTown()) {
				CivMessage.sendError(sender, "You must be a member of a town in order to cast a vote.");
				return false;
			}
			
			Civilization civ = CivGlobal.getCiv(args[0]);
			if (civ == null) {
				CivMessage.sendError(sender, "Couldn't find eligable civ named '"+args[0]+"'.");
				return false;
			}
			
			if (!EndConditionDiplomacy.canPeopleVote()) {
				CivMessage.sendError(sender, "Council of Eight not yet built. Cannot vote for civs until then.");
				return false;
			}
			
			EndConditionDiplomacy.addVote(civ, resident);
			return true;
		} else {
			return false;
		}
	}

}
