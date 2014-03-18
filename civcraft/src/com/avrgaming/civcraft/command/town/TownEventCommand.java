package com.avrgaming.civcraft.command.town;

import java.text.SimpleDateFormat;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.util.CivColor;

public class TownEventCommand extends CommandBase {

	@Override
	public void init() {
		command = "/town event";
		displayName = "Town Event";
		
		commands.put("show", "Shows current town event.");
		commands.put("activate", "Activates current event.");
	}

	public void activate_cmd() throws CivException {
		Town town = getSelectedTown();
		RandomEvent event = town.getActiveEvent();

		if (event == null) {
			CivMessage.sendError(sender, "No current event.");
		} else {
			event.activate();
			CivMessage.sendSuccess(sender, "Event activated!");
		}
	}
	
	public void show_cmd() throws CivException {
		Town town = getSelectedTown();
		RandomEvent event = town.getActiveEvent();

		if (event == null) {
			CivMessage.sendError(sender, "No current event.");
		} else {
			SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");

			CivMessage.sendHeading(sender, "Current Event: "+event.configRandomEvent.name);
			CivMessage.send(sender, CivColor.Green+"Started On: "+CivColor.LightGreen+sdf.format(event.getStartDate()));
			CivMessage.send(sender, CivColor.Green+"End Date:"+CivColor.LightGreen+sdf.format(event.getEndDate()));
			if (event.isActive()) {
				CivMessage.send(sender, CivColor.LightGray+"Event has been activated.");
			} else {
				CivMessage.send(sender, CivColor.Yellow+"Event has not been activated. Use '/town event activate' to activate the event.");
			}
			CivMessage.send(sender, CivColor.Green+"-- Messages From Event ---");
			CivMessage.send(sender, CivColor.LightGray+event.getMessages());
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
