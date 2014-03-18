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

public class ConfigCultureLevel {
	public int level;
	public int amount;
	public int chunks;
	
	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigCultureLevel> levels) {
		levels.clear();
		List<Map<?, ?>> culture_levels = cfg.getMapList("culture_levels");
		for (Map<?, ?> level : culture_levels) {
			ConfigCultureLevel culture_level = new ConfigCultureLevel();
			culture_level.level = (Integer)level.get("level");
			culture_level.amount = (Integer)level.get("amount");
			culture_level.chunks = (Integer)level.get("chunks");
			levels.put(culture_level.level, culture_level);
		}
		CivLog.info("Loaded "+levels.size()+" culture levels.");
	}
}
