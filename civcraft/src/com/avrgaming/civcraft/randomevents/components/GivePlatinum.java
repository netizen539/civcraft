package com.avrgaming.civcraft.randomevents.components;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;
import com.avrgaming.global.perks.PlatinumManager;

public class GivePlatinum extends RandomEventComponent {

	@Override
	public void process() {
		for (Resident resident : this.getParentTown().getResidents()) {
			PlatinumManager.givePlatinumDaily(resident, 
					CivSettings.platinumRewards.get("randomEventSuccess").name,
					CivSettings.platinumRewards.get("randomEventSuccess").amount, 
					this.getString("message"));	
		}

	}

}
