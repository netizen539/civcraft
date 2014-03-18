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
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigPerk {
	public String id;
	public LinkedList<HashMap<String, String>> components;
	public String display_name;
	public Integer type_id;
	public Integer data;
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigPerk> perk_map) {
		perk_map.clear();
		List<Map<?, ?>> perks = cfg.getMapList("perks");
		for (Map<?, ?> obj : perks) {
			ConfigPerk p = new ConfigPerk();
			
			p.id = (String)obj.get("id");
			p.display_name = (String)obj.get("display_name");
			p.type_id = (Integer)obj.get("item_id");
			p.data = (Integer)obj.get("data");
			
			p.components = new LinkedList<HashMap<String, String>>();
			
			@SuppressWarnings("unchecked")
			List<Map<?, ?>> comps = (List<Map<?, ?>>) obj.get("components");
			if (comps != null) {
				for (Map<?, ?> compObj : comps) {
					
					HashMap<String, String> compMap = new HashMap<String, String>();
					for (Object key : compObj.keySet()) {
						compMap.put((String)key, (String)compObj.get(key));
					}
			
					p.components.add(compMap);	
				}
			}
			
			perk_map.put(p.id, p);
		}
		CivLog.info("Loaded "+perk_map.size()+" Perks.");		
	}
	
	
	
	

}
