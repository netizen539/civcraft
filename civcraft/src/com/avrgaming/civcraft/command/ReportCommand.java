package com.avrgaming.civcraft.command;

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.interactive.InteractiveReportPlayer;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.global.reports.ReportManager;

public class ReportCommand extends CommandBase {

	@Override
	public void init() {
		command = "/report";
		displayName = "Report";
		
		commands.put("player", "[name] - Reports this player for misconduct.");
	}

	public void player_cmd() throws CivException {
		Resident resident = getResident();
		Resident reportedResident = getNamedResident(1);
		
		CivMessage.sendHeading(sender, "Reporting a Player");
		CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"You are reporting "+reportedResident.getName()+" for misconduct.");
		CivMessage.send(sender, " ");
		CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"Please select one of the following categories: "+CivColor.LightGreen+ChatColor.BOLD+ReportManager.getReportTypes());
		CivMessage.send(sender, " ");
		CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"Reporting players allows our staff to determine patterns of behavior in players," +
				"if a player gets too many bad reports they may be banned. Please know that filing false reports is also a bannable offense.");
		CivMessage.send(sender, CivColor.LightGray+ChatColor.BOLD+"Type 'cancel' to cancel this report.");
		resident.setInteractiveMode(new InteractiveReportPlayer(reportedResident.getName()));
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
