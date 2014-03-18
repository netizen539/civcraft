package com.avrgaming.civcraft.randomevents.components;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;

public class Unhappiness extends RandomEventComponent {

	public static String getKey(Town town) {
		return "randomevent:unhappiness:"+town.getId();
	}
	
	
	@Override
	public void process() {
		
		int unhappiness = Integer.valueOf(this.getString("value"));
		int duration = Integer.valueOf(this.getString("duration"));
		
		CivGlobal.getSessionDB().add(getKey(this.getParentTown()), unhappiness+":"+duration, this.getParentTown().getCiv().getId(), this.getParentTown().getId(), 0);	
		sendMessage("Blast! We're now suffering a happiness penalty of "+unhappiness+" unhappiness for "+duration+" hours!");
		
	}

}
