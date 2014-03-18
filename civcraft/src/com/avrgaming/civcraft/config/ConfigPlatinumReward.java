package com.avrgaming.civcraft.config;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigPlatinumReward {
	public String name;
	public int amount;
	public String occurs;
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigPlatinumReward> rewards) {
		rewards.clear();
		List<Map<?, ?>> culture_levels = cfg.getMapList("platinum");
		for (Map<?, ?> level : culture_levels) {
			ConfigPlatinumReward reward = new ConfigPlatinumReward();
			reward.name = (String)level.get("name");
			reward.amount = (Integer)level.get("amount");
			reward.occurs = (String)level.get("occurs");
			rewards.put(reward.name, reward);
		}
		CivLog.info("Loaded "+rewards.size()+" platinum rewards..");
	}
}
