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

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.EntityType;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigTempleSacrifice {
	public List<String> entites;
	public int reward;
	
	public static HashSet<String> validEntities = new HashSet<String>();
	
	public static void loadConfig(FileConfiguration cfg, ArrayList<ConfigTempleSacrifice> temple_sacrifices) {
		List<Map<?, ?>> ts_list = cfg.getMapList("temple.sacrifices");
		for (Map<?,?> cl : ts_list ) {
			ConfigTempleSacrifice config_ts = new ConfigTempleSacrifice();
	
			List<?> entitiesList = (List<?>)cl.get("entities");
			if (entitiesList != null) {
				ArrayList<String> entities = new ArrayList<String>();

				for (Object obj : entitiesList) {
					if (obj instanceof String) {
						entities.add((String)obj);
						
						String split[] = ((String) obj).split(":");
						validEntities.add(split[0].toUpperCase());
					}
				}
				config_ts.entites = entities;
			}

			config_ts.reward = (Integer)cl.get("reward");
			temple_sacrifices.add(config_ts);
		}
		CivLog.info("Loaded "+temple_sacrifices.size()+" temple sacrifices.");
	}

	public static boolean isValidEntity(EntityType entityType) {
		if (validEntities.contains(entityType.toString())) {
			return true;
		}
		
		return false;
	}
}
