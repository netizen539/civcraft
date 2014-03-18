package com.avrgaming.civcraft.config;

import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.util.BlockCoord;

public class ConfigArena {
	public String id;
	public String name;
	public String world_source;
	public LinkedList<ConfigArenaTeam> teams;
	
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigArena> config_arenas) {
		config_arenas.clear();
		
		List<Map<?, ?>> cottage_list = cfg.getMapList("arenas");
		
		for (Map<?,?> cl : cottage_list ) {
			@SuppressWarnings("unchecked")
			List<Map<?, ?>> teams_map_list = (List<Map<?, ?>>) cl.get("teams");
			
			ConfigArena arena = new ConfigArena();
			arena.id = (String)cl.get("id");
			arena.name = (String)cl.get("name");
			arena.world_source = (String)cl.get("world_folder");

			if (teams_map_list != null) {				
				arena.teams = new LinkedList<ConfigArenaTeam>();
				for (Map<?,?> tm : teams_map_list) {
					ConfigArenaTeam team = new ConfigArenaTeam();
					
					team.number = (Integer)tm.get("number");
					team.name = (String)tm.get("name");
					
					team.controlPoints = new LinkedList<BlockCoord>();
					List<?> structureList = (List<?>) tm.get("controlblocks");
					for (Object obj : structureList) {
						String[] coords = ((String)obj).split(",");
						BlockCoord bcoord = new BlockCoord(arena.world_source, 
								Integer.valueOf(coords[0]),
								Integer.valueOf(coords[1]),
								Integer.valueOf(coords[2]));
						
						
						team.controlPoints.add(bcoord);
					}
					
					arena.teams.add(team);
				}
			}
			


			config_arenas.put(arena.id, arena);
		}
		
		CivLog.info("Loaded "+config_arenas.size()+" arenas.");
	}	
}
