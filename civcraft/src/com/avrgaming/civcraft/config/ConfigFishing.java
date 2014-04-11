package com.avrgaming.civcraft.config;
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


import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigFishing {
	public String craftMatId;
	public int type_id;	
	public double drop_chance;
	
	public static void loadConfig(FileConfiguration cfg, ArrayList<ConfigFishing> configList) {
		  configList.clear();
			  List<Map<?, ?>> drops = cfg.getMapList("fishing_drops");
			  for (Map<?, ?> item : drops) {
			   ConfigFishing g = new ConfigFishing();
			   
			   g.craftMatId = (String)item.get("craftMatId");
			   g.type_id = (Integer)item.get("type_id");
			   g.drop_chance = (Double)item.get("drop_chance");
			   
			   configList.add(g);
			   
			  }
		  CivLog.info("Loaded "+configList.size()+" fishing drops.");  
		  
	}

}


