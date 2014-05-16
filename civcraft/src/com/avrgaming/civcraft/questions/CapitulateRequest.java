package com.avrgaming.civcraft.questions;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class CapitulateRequest implements QuestionResponseInterface {

	public Town capitulator;
	public String from;
	public String to;
	public String playerName;
	
	@Override
	public void processResponse(String param) {
		if (param.equalsIgnoreCase("accept")) {
			capitulator.capitulate();
			CivMessage.global(from+" has capitulated to "+to);
		} else {
			CivMessage.send(playerName, CivColor.LightGray+to+" declined our offer.");
		}
	}

	@Override
	public void processResponse(String response, Resident responder) {
		processResponse(response);		
	}
}
