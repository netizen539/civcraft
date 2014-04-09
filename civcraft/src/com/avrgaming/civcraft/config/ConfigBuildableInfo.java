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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Town;

public class ConfigBuildableInfo {
	public String id = "";
	public String template_base_name = "";
	public int templateYShift = 0;
	public String displayName = "";
	public String require_tech = "";
	public String require_upgrade = "";
	public String require_structure = "";
	public String check_event = "";
	public String effect_event= "";
	public String update_event = "";
	public String onBuild_event = "";
	public int limit = 0;
	public ArrayList<String> signs = new ArrayList<String>();
	public double cost = 0;
	public double upkeep = 0;
	public double hammer_cost = 0;
	public int max_hitpoints = 0;
	public Boolean destroyable = false;
	public Boolean allow_outside_town = false;
	public Boolean isWonder = false;
	public Integer regenRate = 0;
	public Boolean tile_improvement = false;
	public Integer points = 0;
	public boolean allow_demolish = false;
	public boolean strategic = false;
	public boolean ignore_floating = false;
	public List<HashMap<String, String>> components = new LinkedList<HashMap<String, String>>();
	public boolean has_template = true;
	
	public boolean isAvailable(Town town) {
		if (town.hasTechnology(require_tech)) {
			if (town.hasUpgrade(require_upgrade)) {
				if (town.hasStructure(require_structure)) {
					if (limit == 0 || town.getStructureTypeCount(id) < limit) {					
						boolean capitol = town.isCapitol();

						if (id.equals("s_townhall") && capitol) {
							return false;
						}
						
						if (id.equals("s_capitol") && !capitol) {
							return false;
						}
						
						return true;
					}
				}
			}
		}
		return false;
	}
	
	public static void loadConfig(FileConfiguration cfg, String path, Map<String, ConfigBuildableInfo> structureMap, boolean isWonder) {
		structureMap.clear();
		List<Map<?, ?>> structures = cfg.getMapList(path);
		for (Map<?, ?> obj : structures) {
			ConfigBuildableInfo sinfo = new ConfigBuildableInfo();
			
			sinfo.id = (String)obj.get("id");
			sinfo.template_base_name = (String)obj.get("template");
			sinfo.templateYShift = (Integer)obj.get("template_y_shift");
			sinfo.displayName = (String)obj.get("displayName");
			sinfo.require_tech = (String)obj.get("require_tech");
			sinfo.require_upgrade = (String)obj.get("require_upgrade");
			sinfo.require_structure = (String)obj.get("require_structure");
			sinfo.check_event = (String)obj.get("check_event");
			sinfo.effect_event = (String)obj.get("effect_event");
			sinfo.update_event = (String)obj.get("update_event");
			sinfo.onBuild_event = (String)obj.get("onBuild_event");
			sinfo.limit = (Integer)obj.get("limit");
			//TODO handle signs
			sinfo.cost = (Double)obj.get("cost");
			sinfo.upkeep = (Double)obj.get("upkeep");
			sinfo.hammer_cost = (Double)obj.get("hammer_cost");
			sinfo.max_hitpoints = (Integer)obj.get("max_hitpoints");
			sinfo.destroyable = (Boolean)obj.get("destroyable");
			sinfo.allow_outside_town = (Boolean)obj.get("allow_outside_town");
			sinfo.regenRate = (Integer)obj.get("regen_rate");
			sinfo.isWonder = isWonder;
			sinfo.points = (Integer)obj.get("points");
			
				@SuppressWarnings("unchecked")
				List<Map<?, ?>> comps = (List<Map<?, ?>>) obj.get("components");
				if (comps != null) {
					for (Map<?, ?> compObj : comps) {
						
						HashMap<String, String> compMap = new HashMap<String, String>();
						for (Object key : compObj.keySet()) {
							compMap.put((String)key, (String)compObj.get(key));
						}
				
						sinfo.components.add(compMap);	
					}
				}
			
			
			Boolean tileImprovement = (Boolean)obj.get("tile_improvement");
			if (tileImprovement != null && tileImprovement == true) {
				sinfo.tile_improvement = true;
			} else {
				sinfo.tile_improvement = false;
			}
			
			Boolean allowDemolish = (Boolean)obj.get("allow_demolish");
			if (allowDemolish == null || allowDemolish == true) {
				sinfo.allow_demolish = true;
			} else {
				sinfo.allow_demolish = false;
			}
			
			Boolean strategic = (Boolean)obj.get("strategic");
			if (strategic == null || strategic == false) {
				sinfo.strategic  = false;
			} else {
				sinfo.strategic = true;
			}
			
			Boolean ignore_floating = (Boolean)obj.get("ignore_floating");
			if (ignore_floating != null) {
				sinfo.ignore_floating = ignore_floating;
			}
			
			Boolean has_template = (Boolean)obj.get("has_template");
			if (has_template != null) {
				sinfo.has_template = has_template;
			}
			
			
			if (isWonder) {
				sinfo.strategic = true;
			}
	
			structureMap.put(sinfo.id, sinfo);
		}
		CivLog.info("Loaded "+structureMap.size()+" structures.");
	}
	
}
