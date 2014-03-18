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

public class ConfigGrocerLevel {
	public int level;
	public String itemName;
	public int itemId;
	public int itemData;
	public int amount;
	public double price;
	
	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigGrocerLevel> levels) {
		levels.clear();
		List<Map<?, ?>> culture_levels = cfg.getMapList("grocer_levels");
		for (Map<?, ?> level : culture_levels) {
			ConfigGrocerLevel grocer_level = new ConfigGrocerLevel();
			grocer_level.level = (Integer)level.get("level");
			grocer_level.itemName = (String)level.get("itemName");
			grocer_level.itemId = (Integer)level.get("itemId");
			grocer_level.itemData = (Integer)level.get("itemData");
			grocer_level.amount = (Integer)level.get("amount");
			grocer_level.price = (Double)level.get("price");
			
			levels.put(grocer_level.level, grocer_level);
		}
		CivLog.info("Loaded "+levels.size()+" grocer levels.");
	}
	
}
