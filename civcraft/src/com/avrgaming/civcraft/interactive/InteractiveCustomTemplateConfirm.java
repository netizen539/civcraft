package com.avrgaming.civcraft.interactive;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.CustomTemplate;

public class InteractiveCustomTemplateConfirm implements InteractiveResponse {

	String playerName;
	CustomTemplate customTemplate;
	
	public InteractiveCustomTemplateConfirm(String playerName, CustomTemplate customTemplate) {
		this.playerName = playerName;
		this.customTemplate = customTemplate;
		displayQuestion();
	}
	
	public void displayQuestion() {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return;
		}
		
		Resident resident = CivGlobal.getResident(player);
		Town town = resident.getTown();
		Perk perk = customTemplate.getParent();
		
		CivMessage.sendHeading(player, "Confirm Template Binding");
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+"You are about to bind the "+perk.getDisplayName()+" template to your town of "+town.getName());
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+"Once the template is bound, you will be able to build the custom template by");
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+"Using the normal build command. This action consumes the perk, and cannot be undone.");
		CivMessage.send(player, CivColor.LightGreen+CivColor.BOLD+"Are you sure you want to bind the template? Type "+CivColor.Yellow+CivColor.BOLD+"yes");
		CivMessage.send(player, CivColor.LightGreen+ChatColor.BOLD+"Type anything else to abort.");
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
			CivMessage.sendError(player, "Template Bind Cancelled.");
			return;
		}
		
		customTemplate.bindTemplateToTown(resident.getTown(), resident);
		customTemplate.markAsUsed(resident);
		CivMessage.sendSuccess(player, "Bound "+customTemplate.getParent().getDisplayName()+" to "+resident.getTown().getName());
	}
}
