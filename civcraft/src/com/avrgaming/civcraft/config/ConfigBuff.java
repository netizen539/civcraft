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
import com.avrgaming.civcraft.util.CivColor;

public class ConfigBuff {
	public String id;
	public String name;
	public String description;
	public String value;
	public boolean stackable;
	public String parent;
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigBuff> buffs){
		buffs.clear();
		List<Map<?, ?>> configBuffs = cfg.getMapList("buffs");
		for (Map<?, ?> b : configBuffs) {
			ConfigBuff buff = new ConfigBuff();
			buff.id = (String)b.get("id");
			buff.name = (String)b.get("name");
			
			buff.description = (String)b.get("description");
			buff.description = CivColor.colorize(buff.description);
			
			buff.value = (String)b.get("value");
			buff.stackable = (Boolean)b.get("stackable");
			buff.parent = (String)b.get("parent");
			
			if (buff.parent == null) {
				buff.parent = buff.id;
			}
			
			buffs.put(buff.id, buff);
		}
		
		CivLog.info("Loaded "+buffs.size()+" Buffs.");
	}	
}
