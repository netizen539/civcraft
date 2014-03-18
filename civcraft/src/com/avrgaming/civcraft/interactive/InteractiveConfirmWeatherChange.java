package com.avrgaming.civcraft.interactive;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.global.perks.components.ChangeWeather;

public class InteractiveConfirmWeatherChange implements InteractiveResponse {

	ChangeWeather perk;
	public InteractiveConfirmWeatherChange(ChangeWeather perk) {
		this.perk = perk;
	}
	
	@Override
	public void respond(String message, Resident resident) {
		resident.clearInteractiveMode();
		
		if (message.equalsIgnoreCase("yes")) {
			Player player;
			try {
				player = CivGlobal.getPlayer(resident);
				player.getWorld().setStorm(false);
				player.getWorld().setThundering(false);
				player.getWorld().setWeatherDuration((int) TimeTools.toTicks(20*60));
				CivMessage.global(resident.getName()+" has used a "+CivColor.Yellow+"Weather Change"+CivColor.RESET+" token to change the weather to sunny!");
				perk.markAsUsed(resident);
			} catch (CivException e) {
			}
		} else {
			CivMessage.send(resident, "Weather Change cancelled.");
		}
		
	}

}
