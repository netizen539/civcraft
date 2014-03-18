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

public class ConfigStableHorse {
	public int id;
	public double speed;
	public double jump;
	public double health;
	public boolean mule;
	public String variant;
	
	public static void loadConfig(FileConfiguration cfg, Map<Integer, ConfigStableHorse> horses) {
		horses.clear();
		List<Map<?, ?>> config_horses = cfg.getMapList("stable_horses");
		for (Map<?, ?> level : config_horses) {
			ConfigStableHorse horse = new ConfigStableHorse();
			horse.id = (Integer)level.get("id");
			horse.speed = (Double)level.get("speed");
			horse.jump = (Double)level.get("jump");
			horse.health = (Double)level.get("health");
			horse.variant = (String)level.get("variant");
			
			Boolean mule = (Boolean)level.get("mule");
			if (mule == null || mule == false) {
				horse.mule = false;
			} else {
				horse.mule = true;
			}
			
			horses.put(horse.id, horse);
		}
		CivLog.info("Loaded "+horses.size()+" Horses.");
	}
	
}
