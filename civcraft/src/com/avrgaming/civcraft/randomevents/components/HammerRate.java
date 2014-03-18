package com.avrgaming.civcraft.randomevents.components;

import java.text.DecimalFormat;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;

public class HammerRate extends RandomEventComponent {

	@Override
	public void process() {
		double rate = this.getDouble("value");
		int duration = Integer.valueOf(this.getString("duration"));
		
		CivGlobal.getSessionDB().add(getKey(this.getParentTown()), rate+":"+duration, this.getParentTown().getCiv().getId(), this.getParentTown().getId(), 0);
		DecimalFormat df = new DecimalFormat();
		
		if (rate > 1.0) {
			sendMessage("Our production rate has increased by "+df.format((rate - 1.0)*100)+"% due to an unforseen event!");
		} else {
			sendMessage("Our production rate has decreased by "+df.format((1.0 - rate)*100)+"% due to an unforseen event!");
		}
	}

	public static String getKey(Town town) {
		return "randomevent:hammerrate"+town.getId();
	}

}
