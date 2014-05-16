package com.avrgaming.civcraft.questions;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FoundTownSync;
import com.avrgaming.civcraft.util.CivColor;

public class TownNewRequest implements QuestionResponseInterface {

	public Resident resident;
	public Resident leader;
	public Civilization civ;
	public String name;
	
	@Override
	public void processResponse(String param) {
		if (param.equalsIgnoreCase("accept")) {
			CivMessage.send(civ, CivColor.LightGreen+"Our Civilization leader "+leader.getName()+" has accepted the request to found the town of "+name);
			TaskMaster.syncTask(new FoundTownSync(resident));
		} else {
			CivMessage.send(resident, CivColor.LightGray+"Our request to found a town has been denied.");
		}		
	}

	@Override
	public void processResponse(String response, Resident responder) {
		this.leader = responder;
		processResponse(response);		
	}
}
