package com.avrgaming.civcraft.interactive;

import java.text.DecimalFormat;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.items.units.MissionBook;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.EspionageMissionTask;
import com.avrgaming.civcraft.util.CivColor;

public class InteractiveSpyMission implements InteractiveResponse {

	public ConfigMission mission;
	public String playerName;
	public Location playerLocation;
	public Town target;
	
	public InteractiveSpyMission(ConfigMission mission, String playerName, Location playerLocation, Town target) {
		this.mission = mission;
		this.playerName = playerName;
		this.playerLocation = playerLocation;
		this.target = target;
		displayQuestion();
	}
	
	public void displayQuestion() {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		
		CivMessage.sendHeading(player, "Mission: "+mission.name);
		
		double failChance = MissionBook.getMissionFailChance(mission, target);
		double compChance = MissionBook.getMissionCompromiseChance(mission, target);
		DecimalFormat df = new DecimalFormat();
		
		String successChance = df.format((1 - failChance)*100)+"%";
		String compromiseChance = df.format(compChance)+"%";
		String length = "";
		
		int mins = mission.length / 60;
		int seconds = mission.length % 60;
		if (mins > 0) {
			length += mins+" mins";
			if (seconds > 0) {
				length += " and ";
			}
		}
		
		if (seconds > 0) {
			length += seconds+" seconds";
		}
		
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+"We have a "+CivColor.LightGreen+successChance+CivColor.Green+CivColor.BOLD+" chance of success.");
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+"If we fail, the chance of being compromised is "+CivColor.LightGreen+compromiseChance);
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+"It will cost our town "+CivColor.Yellow+mission.cost+CivColor.Green+CivColor.BOLD+" coins to perform this mission.");
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+"The mission will take "+CivColor.Yellow+length+CivColor.Green+CivColor.BOLD+" to complete.");
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+"You must remain within the civ's borders during the mission, otherwise you'll fail the mission.");
		CivMessage.send(player, CivColor.Green+CivColor.BOLD+"If these conditions are acceptible, type "+CivColor.Yellow+"yes");
		CivMessage.send(player, CivColor.Green+ChatColor.BOLD+"Type anything else to abort.");
	}
	
	
	@Override
	public void respond(String message, Resident resident) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
		} catch (CivException e) {
			return;
		}
		resident.clearInteractiveMode();

		if (!message.equalsIgnoreCase("yes")) {
			CivMessage.sendError(player, "Mission Aborted.");
			return;
		}
		
		if(!TaskMaster.hasTask("missiondelay:"+playerName)) {
			TaskMaster.asyncTask("missiondelay:"+playerName, (new EspionageMissionTask(mission, playerName, playerLocation, target, mission.length)), 0);
		} else {
			CivMessage.sendError(player, "Waiting on countdown to start mission.");
			return;
		}
	}
}
