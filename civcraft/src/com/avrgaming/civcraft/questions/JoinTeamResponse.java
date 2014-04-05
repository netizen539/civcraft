package com.avrgaming.civcraft.questions;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.main.Colors;
import com.avrgaming.civcraft.object.Resident;

public class JoinTeamResponse implements QuestionResponseInterface {

	public ArenaTeam team;
	public Resident resident;
	public Player sender;
	
	@Override
	public void processResponse(String param) {
		if (param.equalsIgnoreCase("accept")) {
			CivMessage.send(sender, Colors.LightGray+resident.getName()+" accepted our team invitation.");
			
			try {
				ArenaTeam.addMember(team.getName(), resident);
			} catch (CivException e) {
				CivMessage.sendError(sender, e.getMessage());
				return;
			}

			CivMessage.sendTeam(team, resident.getName()+" has joined the team.");
		} else {
			CivMessage.send(sender, Colors.LightGray+resident.getName()+" denied our team invitation.");
		}
	}
	
	@Override
	public void processResponse(String response, Resident responder) {
		processResponse(response);		
	}

}
