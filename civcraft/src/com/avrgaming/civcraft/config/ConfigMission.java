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

public class ConfigMission {

	public String id;
	public String name;
	public List<String> description;
	public Double cost;
	public Double range;
	public Double cooldown;
	public Integer intel;
	public Integer slot;
	public Double fail_chance;
	public Integer length;
	public Double compromise_chance;
	
	public ConfigMission() {
	}
	
	public ConfigMission(ConfigMission mission) {
		this.id = mission.id;
		this.name = mission.name;
		this.description = mission.description;
		this.cost = mission.cost;
		this.range = mission.range;
		this.cooldown = mission.cooldown;
		this.intel = mission.intel;
	}

	@SuppressWarnings("unchecked")
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigMission> missions){
		missions.clear();
		List<Map<?, ?>> configMissions = cfg.getMapList("missions");
		for (Map<?, ?> b : configMissions) {
			ConfigMission mission = new ConfigMission();
			mission.id = (String)b.get("id");
			mission.name = (String)b.get("name");
			mission.cost = (Double)b.get("cost");
			mission.range = (Double)b.get("range");
			mission.cooldown = (Double)b.get("cooldown");
			mission.intel = (Integer)b.get("intel");
			mission.length = (Integer)b.get("length");
			mission.fail_chance = (Double)b.get("fail_chance");
			mission.compromise_chance = (Double)b.get("compromise_chance");
			mission.slot = (Integer)b.get("slot");
			mission.description = (List<String>) b.get("description");
			
			missions.put(mission.id.toLowerCase(), mission);
		}
		
		CivLog.info("Loaded "+missions.size()+" Espionage Missions.");
	}	
	
}
