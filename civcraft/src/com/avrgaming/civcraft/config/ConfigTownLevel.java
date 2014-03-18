/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.config;

import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigTownLevel {
	public int level;
	public String title;
	public double upkeep;
	public int plots;
	public double plot_cost;
	public int tile_improvements;
	
	
	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigTownLevel> levels) {
		levels.clear();
		List<Map<?, ?>> culture_levels = cfg.getMapList("town_levels");
		for (Map<?, ?> level : culture_levels) {
			ConfigTownLevel town_level = new ConfigTownLevel();
			town_level.level = (Integer)level.get("level");
			town_level.title = (String)level.get("title");
			town_level.upkeep = (Double)level.get("upkeep");
			town_level.plots = (Integer)level.get("plots");
			town_level.plot_cost = (Double)level.get("plot_cost");
			town_level.tile_improvements = (Integer)level.get("tile_improvements");
			
			levels.put(town_level.level, town_level);
		}
		CivLog.info("Loaded "+levels.size()+" town levels.");
	}
	
}
