package com.avrgaming.civcraft.randomevents.components;


import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;

public class PayPlayer extends RandomEventComponent {

	@Override
	public void process() {
		String playerName = this.getParent().componentVars.get(getString("playername_var"));
		if (playerName == null) {
			CivLog.warning("No playername var for pay player.");
			return;
		}

		Resident resident = CivGlobal.getResident(playerName);
		double coins = this.getDouble("amount");
		resident.getTreasury().deposit(coins);
		CivMessage.send(resident, "You've recieved "+coins+" coins!");	
	}

}
