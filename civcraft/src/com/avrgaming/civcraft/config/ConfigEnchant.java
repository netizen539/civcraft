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

public class ConfigEnchant {

	public String id;
	public String name;
	public String description;
	public double cost;
	public String enchant_id;
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigEnchant> enchant_map) {
		enchant_map.clear();
		List<Map<?, ?>> techs = cfg.getMapList("enchants");
		for (Map<?, ?> level : techs) {
			ConfigEnchant enchant = new ConfigEnchant();
			
			enchant.id = (String)level.get("id");
			enchant.name = (String)level.get("name");
			enchant.description = (String)level.get("description");
			enchant.cost = (Double)level.get("cost");
			enchant.enchant_id = (String)level.get("enchant_id");			
			enchant_map.put(enchant.id, enchant);
		}
		CivLog.info("Loaded "+enchant_map.size()+" enchantments.");		
	}

	
}
