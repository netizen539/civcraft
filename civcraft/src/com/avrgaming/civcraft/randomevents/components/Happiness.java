package com.avrgaming.civcraft.randomevents.components;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;

public class Happiness extends RandomEventComponent {

	@Override
	public void process() {
		int happiness = Integer.valueOf(this.getString("value"));
		int duration = Integer.valueOf(this.getString("duration"));
		
		CivGlobal.getSessionDB().add(getKey(this.getParentTown()), happiness+":"+duration, this.getParentTown().getCiv().getId(), this.getParentTown().getId(), 0);	
		sendMessage("We're now enjoying a happiness bonus  of "+happiness+" happiness for "+duration+" hours!");		
	}

	public static String getKey(Town town) {
		return "randomevent:happiness:"+town.getId();
	}

}
