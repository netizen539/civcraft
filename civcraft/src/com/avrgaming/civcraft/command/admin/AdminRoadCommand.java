package com.avrgaming.civcraft.command.admin;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.road.Road;
import com.avrgaming.civcraft.structure.Buildable;

public class AdminRoadCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad road";
		displayName = "Admin Road";	
		
	//	commands.put("destroy", "Destroys nearest road.");
		commands.put("setraidtime", "d:M:y:H:m sets the raid time on the nearest road");		
	}

	public void setraidtime_cmd() throws CivException {
		Town town = getNamedTown(1);
		Player player = getPlayer();
		
		if (args.length < 3) {
			throw new CivException("Enter a date like DAY:MONTH:YEAR:HOUR:MIN");
		}
		
		Buildable buildable = town.getNearestBuildable(player.getLocation());
		Road road;
		if (!(buildable instanceof Road)) {
			throw new CivException("Nearest structure is not a road, it's a "+buildable.getDisplayName());
		}
		
		road = (Road)buildable;
				
		String dateStr = args[2];
		SimpleDateFormat parser = new SimpleDateFormat("d:M:y:H:m");
		
		Date next;
		try {
			next = parser.parse(dateStr);
			road.setNextRaidDate(next);
			CivMessage.sendSuccess(sender, "Set raid date.");
		} catch (ParseException e) {
			throw new CivException("Couldnt parse "+args[2]+" into a date, use format: DAY:MONTH:YEAR:HOUR:MIN");
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
		
	}

}
