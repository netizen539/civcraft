package com.avrgaming.civcraft.config;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigTownHappinessLevel {
	public int level;
	public double happiness;
	
	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigTownHappinessLevel> town_happiness_levels) {
		town_happiness_levels.clear();
		List<Map<?, ?>> list = cfg.getMapList("happiness.town_levels");
		for (Map<?,?> cl : list ) {
			
			ConfigTownHappinessLevel happy_level = new ConfigTownHappinessLevel();
			happy_level.level = (Integer)cl.get("level");
			happy_level.happiness = (Double)cl.get("happiness");
			
			town_happiness_levels.put(happy_level.level, happy_level);
			
		}
		CivLog.info("Loaded "+town_happiness_levels.size()+" Town Happiness levels.");		
	}
	
}
