package com.avrgaming.civcraft.interactive;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.global.perks.components.RenameCivOrTown;

public class InteractiveRenameCivOrTown implements InteractiveResponse {

	public String selection = null;
	public String oldName = null;
	public String newName = null;
	public Civilization selectedCiv = null;
	public Town selectedTown = null;
	RenameCivOrTown perk;

	
	public InteractiveRenameCivOrTown(Resident resident, RenameCivOrTown perk) {
		displayQuestion(resident);
		this.perk = perk;
	}
	
	public void displayQuestion(Resident resident) {		
		CivMessage.send(resident, CivColor.Green+"Would you like to rename a "+CivColor.LightGreen+"CIV"+
								  CivColor.Green+" or a "+CivColor.LightGreen+"TOWN"+CivColor.Green+"?");
		CivMessage.send(resident, CivColor.Gray+"(Type 'civ' or 'town' anything else cancels.)");
		return;
	}
	
	@Override
	public void respond(String message, Resident resident) {
		
		
		CivMessage.sendHeading(resident, "Rename Civilization or Town");
		
		try {
			if (selection == null) {
				if (message.equalsIgnoreCase("town")) {
					CivMessage.send(resident, CivColor.Green+"Enter the name of the town you want to rename:");
					selection = "town";
				} else if (message.equalsIgnoreCase("civ")) {
					CivMessage.send(resident, CivColor.Green+"Enter the name of the civ you want to rename:");
					selection = "civ";
				} else {
					throw new CivException("Rename cancelled.");
				}
			} else if (oldName == null) {
				oldName = message;
				if (selection.equals("town")) {
					Town town = CivGlobal.getTown(oldName);
					if (town == null) {
						throw new CivException("No town named "+oldName+".");
					}
					
					if (!town.getMayorGroup().hasMember(resident) && !town.getCiv().getLeaderGroup().hasMember(resident)) {
						throw new CivException("You must be the town's mayor or the civ's leader to rename towns.");
					}
					
					selectedTown = town;
					CivMessage.send(resident, CivColor.Green+"Enter the NEW name of your town:");
				} else if (selection.equals("civ")) {
					Civilization civ = CivGlobal.getCiv(oldName);
					if (civ == null) {
						civ = CivGlobal.getConqueredCiv(oldName);
						if (civ == null) {
							throw new CivException("No civ named "+oldName+".");
						}
					}
					
					if (!civ.getLeaderGroup().hasMember(resident)) {
						throw new CivException("You must be the civ's leader in order to rename it.");
					}
					
					selectedCiv = civ;
					CivMessage.send(resident, CivColor.Green+"Enter the NEW name of your civ:");
				}
			} else if (newName == null) {
				newName = message.replace(" ", "_");
				if (selectedCiv != null) {
					try {
						CivMessage.global(resident.getName()+" has used a "+CivColor.Yellow+"Rename Token"+CivColor.RESET+" to rename the civ of "+
								selectedCiv.getName()+" to "+newName);
						selectedCiv.rename(newName);
						perk.markAsUsed(resident);
					} catch (InvalidNameException e) {
						throw new CivException("This name is not valid. Pick another.");
					}
				} else if (selectedTown != null) {
					try {
						CivMessage.global(resident.getName()+" has used a "+CivColor.Yellow+"Rename Token"+CivColor.RESET+" to rename the town of "+
								selectedTown.getName()+" to "+newName);
						selectedTown.rename(newName);
						perk.markAsUsed(resident);
					} catch (InvalidNameException e) {
						throw new CivException("This name is not valid. Pick another.");
					}
				}
			} else {
				throw new CivException("Couldn't find all the information we needed. Rename cancelled.");
			}
		} catch (CivException e) {
			CivMessage.sendError(resident, e.getMessage());
			resident.clearInteractiveMode();
			return;
		}

		
	}

}
