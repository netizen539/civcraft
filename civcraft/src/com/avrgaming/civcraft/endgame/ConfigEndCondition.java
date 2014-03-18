package com.avrgaming.civcraft.endgame;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigEndCondition {
	public String id;
	public String  className;
	public HashMap<String, String> attributes = new HashMap<String, String>();
	public String victoryName;
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigEndCondition> endconditionMap) {
		endconditionMap.clear();
		List<Map<?, ?>> perks = cfg.getMapList("end_conditions");
		for (Map<?, ?> obj : perks) {
			ConfigEndCondition p = new ConfigEndCondition();
			
			p.id = (String)obj.get("id");
			p.className = (String)obj.get("class");
			p.victoryName = (String)obj.get("name");
			
			for (Entry<?, ?> entry : obj.entrySet()) {
				if (entry.getKey() instanceof String && entry.getValue() instanceof String) {
					p.attributes.put((String)entry.getKey(), (String)entry.getValue());
				}
			}
			
			endconditionMap.put(p.id, p);
		}
		CivLog.info("Loaded "+endconditionMap.size()+" Perks.");		
	}
	
}
