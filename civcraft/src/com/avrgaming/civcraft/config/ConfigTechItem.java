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

public class ConfigTechItem {

	public int id;
	public String name;
	public String require_tech;
	
	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigTechItem> tech_maps) {
		tech_maps.clear();
		List<Map<?, ?>> techs = cfg.getMapList("items");
		for (Map<?, ?> confTech : techs) {
			ConfigTechItem tech = new ConfigTechItem();
			
			tech.id = (Integer)confTech.get("id");
			tech.name = (String)confTech.get("name");
			tech.require_tech = (String)confTech.get("require_tech");			
			tech_maps.put(tech.id, tech);
		}
		CivLog.info("Loaded "+tech_maps.size()+" technologies.");		
	}
	
}
