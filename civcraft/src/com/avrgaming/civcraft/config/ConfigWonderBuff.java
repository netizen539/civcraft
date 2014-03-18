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

public class ConfigWonderBuff {

	public String id;
	public ArrayList<ConfigBuff> buffs = new ArrayList<ConfigBuff>();
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigWonderBuff> wbuffs){
		wbuffs.clear();
		List<Map<?, ?>> ConfigWonderBuff = cfg.getMapList("wonder_buffs");
		for (Map<?, ?> b : ConfigWonderBuff) {
			ConfigWonderBuff buff = new ConfigWonderBuff();
			buff.id = (String)b.get("id");
			
			List<?> buffStrings = (List<?>)b.get("buffs");
			for (Object obj : buffStrings) {
				if (obj instanceof String) {
					String str = (String)obj;
					
					ConfigBuff cfgBuff = CivSettings.buffs.get(str);
					
					if (cfgBuff != null) {
						buff.buffs.add(cfgBuff);
					} else {
						CivLog.warning("Unknown buff id:"+str);
					}
					
				}
			}
			
			wbuffs.put(buff.id, buff);
		}
		
		CivLog.info("Loaded "+wbuffs.size()+" Wonder Buffs.");
	}	
}
