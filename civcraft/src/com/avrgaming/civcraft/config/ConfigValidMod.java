package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigValidMod {
	public String name;
	public LinkedList<Long> checksums = new LinkedList<Long>();
	
	public static void loadConfig(FileConfiguration cfg, HashMap<String, ConfigValidMod> mods) {
		mods.clear();
		List<Map<?, ?>> cfg_items = cfg.getMapList("valid_mods");
		for (Map<?, ?> level : cfg_items) {
			ConfigValidMod itm = new ConfigValidMod();	
			itm.name = (String)level.get("name");
			
			List<?> checksums = (List<?>)level.get("checksums");
			for (Object chkObj : checksums) {
				itm.checksums.add(Long.valueOf((String)chkObj));			
			}
			
			mods.put(itm.name,itm);
		}
		CivLog.info("Loaded "+mods.size()+" valid mods.");
	}
}
