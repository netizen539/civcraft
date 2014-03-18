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
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Civilization;

public class ConfigGovernment {

	public String id;
	public String displayName;
	public String require_tech;
	
	public double trade_rate;
	public double upkeep_rate;
	public double cottage_rate;
	public double growth_rate;
	public double culture_rate;
	public double hammer_rate;
	public double beaker_rate;
	public double maximum_tax_rate;
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigGovernment> government_map) {
		government_map.clear();
		List<Map<?, ?>> techs = cfg.getMapList("governments");
		for (Map<?, ?> level : techs) {
			ConfigGovernment gov = new ConfigGovernment();
			
			gov.id = (String)level.get("id");
			gov.displayName = (String)level.get("displayName");
			gov.require_tech = (String)level.get("require_tech");

			gov.trade_rate = (Double)level.get("trade_rate");
			gov.upkeep_rate = (Double)level.get("upkeep_rate");
			gov.cottage_rate = (Double)level.get("cottage_rate");
			gov.growth_rate = (Double)level.get("growth_rate");
			gov.culture_rate = (Double)level.get("culture_rate");
			gov.hammer_rate = (Double)level.get("hammer_rate");
			gov.beaker_rate = (Double)level.get("beaker_rate");
			gov.maximum_tax_rate = (Double)level.get("maximum_tax_rate");

			government_map.put(gov.id, gov);
		}
		CivLog.info("Loaded "+government_map.size()+" governments.");		
	}

	public static ArrayList<ConfigGovernment> getAvailableGovernments(Civilization civ) {
		ArrayList<ConfigGovernment> govs = new ArrayList<ConfigGovernment>();
		
		for (ConfigGovernment gov : CivSettings.governments.values()) {
			if (gov.id.equalsIgnoreCase("gov_anarchy")) {
				continue;
			}
			if (gov.isAvailable(civ)) {
				govs.add(gov);
			}
		}
		
		return govs;
	}

	public static ConfigGovernment getGovernmentFromName(String string) {
		
		for (ConfigGovernment gov : CivSettings.governments.values()) {
			if (gov.id.equalsIgnoreCase("gov_anarchy")) {
				continue;
			}
			if (gov.displayName.equalsIgnoreCase(string)) {
				return gov;
			}
		}
		
		return null;
	}

	public boolean isAvailable(Civilization civ) {
		if (civ.hasTechnology(this.require_tech)) {
			return true;
		}
		return false;
	}
	
}
