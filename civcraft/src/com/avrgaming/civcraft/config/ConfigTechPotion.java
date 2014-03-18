package com.avrgaming.civcraft.config;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;

public class ConfigTechPotion {
	public String name;
	public int data;
	public String require_tech;
	
	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigTechPotion> techPotions) {
		techPotions.clear();
		List<Map<?, ?>> techs = cfg.getMapList("potions");
		for (Map<?, ?> confTech : techs) {
			ConfigTechPotion tech = new ConfigTechPotion();
			
			tech.name = (String)confTech.get("name");
			tech.data = (Integer)confTech.get("data");
			tech.require_tech = (String)confTech.get("require_tech");			
			techPotions.put(Integer.valueOf(tech.data), tech);
		}
		CivLog.info("Loaded "+techPotions.size()+" tech potions.");		
	}
	
	public boolean hasTechnology(Player player) {
		Resident resident = CivGlobal.getResident(player);
		if (resident == null || !resident.hasTown()) {
			return false;
		}
		
		if (!resident.getCiv().hasTechnology(require_tech)) {
			return false;
		}
		
		return true;
	}
}
