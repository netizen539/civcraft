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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;

public class ConfigCampUpgrade {

	public String id;
	public String name;
	public double cost;
	public String action;
	public String require_upgrade = null;
	
	public static HashMap<String, Integer> categories = new HashMap<String, Integer>();
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigCampUpgrade> upgrades) {
		upgrades.clear();
		List<Map<?, ?>> culture_levels = cfg.getMapList("upgrades");
		for (Map<?, ?> level : culture_levels) {
			ConfigCampUpgrade upgrade = new ConfigCampUpgrade();
			
			upgrade.id = (String)level.get("id");
			upgrade.name = (String)level.get("name");
			upgrade.cost = (Double)level.get("cost");
			upgrade.action = (String)level.get("action");
			upgrade.require_upgrade = (String)level.get("require_upgrade");	
			upgrades.put(upgrade.id, upgrade);
		}
		CivLog.info("Loaded "+upgrades.size()+" camp upgrades.");		
	}

	public boolean isAvailable(Camp camp) {
		if (camp.hasUpgrade(this.id)) {
			return false;
		}
		
		if (this.require_upgrade == null || this.require_upgrade.equals("")) {
			return true;
		}
		
		if (camp.hasUpgrade(this.require_upgrade)) {
			return true;
		}
		return false;
	}

	public void processAction(Camp camp) {
		
		if (this.action == null) {
			CivLog.warning("No action found for upgrade:"+this.id);
			return;
		}
		
		switch(this.action.toLowerCase()) {
		case "enable_sifter":
			camp.setSifterEnabled(true);
			CivMessage.sendCamp(camp, "Our Sifter has been enabled!");
			break;
		case "enable_longhouse":
			camp.setLonghouseEnabled(true);
			CivMessage.sendCamp(camp, "Our longhouse has been enabled!");
			break;
		case "enable_garden":
			camp.setGardenEnabled(true);
			CivMessage.sendCamp(camp, "Our garden has been enabled!");
			break;
		default:
			CivLog.warning("Unknown action:"+this.action+" processed for upgrade:"+this.id);
			break;
		}
	}
	
}
