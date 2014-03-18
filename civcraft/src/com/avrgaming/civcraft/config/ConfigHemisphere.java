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

public class ConfigHemisphere {
	public String id;
	public int x_min;
	public int x_max;
	public int z_min;
	public int z_max;
	
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigHemisphere> hemis){
		hemis.clear();
		List<Map<?, ?>> configHemis = cfg.getMapList("hemispheres");
		for (Map<?, ?> b : configHemis) {
			ConfigHemisphere buff = new ConfigHemisphere();
			buff.id = (String)b.get("id");
			buff.x_min = (Integer)b.get("x_min");
			buff.x_max = (Integer)b.get("x_max");
			buff.z_min = (Integer)b.get("z_min");
			buff.z_max = (Integer)b.get("z_max");
			hemis.put(buff.id, buff);
		}
		
		CivLog.info("Loaded "+hemis.size()+" Hemispheres.");
	}	
	
}
