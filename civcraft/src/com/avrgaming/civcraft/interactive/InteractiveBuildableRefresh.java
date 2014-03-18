package com.avrgaming.civcraft.interactive;

import java.io.IOException;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;

public class InteractiveBuildableRefresh implements InteractiveResponse {

	String playerName;
	Buildable buildable;
	
	public InteractiveBuildableRefresh(Buildable buildable, String playerName) {
		this.playerName = playerName;
		this.buildable = buildable;
		displayMessage();
	}
	
	public void displayMessage() {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		
		CivMessage.sendHeading(player, "Building Refresh");
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+"Are you sure you want to refresh the blocks for your "+buildable.getDisplayName()+"?");
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+"Any blocks inside the structure (or where the structure ought to be) will be replaced with whats inside the template.");
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+"You may lose some blocks. If that's ok, please type 'yes'. Type anything else to cancel.");
		
	}
	
	
	@Override
	public void respond(String message, Resident resident) {
		resident.clearInteractiveMode();

		if (!message.equalsIgnoreCase("yes")) {
			CivMessage.send(resident, CivColor.LightGray+"Refresh cancelled.");
			return;
		}
		
		class SyncTask implements Runnable {
			Buildable buildable;
			Resident resident;
			
			public SyncTask(Buildable buildable, Resident resident) {
				this.buildable = buildable;
				this.resident = resident;
			}
			
			@Override
			public void run() {	
				try {
					try {
						buildable.repairFromTemplate();
						buildable.getTown().markLastBuildableRefeshAsNow();
						CivMessage.sendSuccess(resident, buildable.getDisplayName()+" refreshed.");
					} catch (IOException e) {
						e.printStackTrace();
						throw new CivException("IO error. Couldn't find template file:"+buildable.getSavedTemplatePath()+" ?");
					} 
				} catch (CivException e) {
					CivMessage.sendError(resident, e.getMessage());
				}
			}
		}
		
		TaskMaster.syncTask(new SyncTask(buildable, resident));	
	}
}
