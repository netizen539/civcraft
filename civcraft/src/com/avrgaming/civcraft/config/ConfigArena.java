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
	public int control_block_hp;
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
			arena.control_block_hp = (Integer)cl.get("control_block_hp");

			if (teams_map_list != null) {				
				arena.teams = new LinkedList<ConfigArenaTeam>();
				for (Map<?,?> tm : teams_map_list) {
					ConfigArenaTeam team = new ConfigArenaTeam();
					
					team.number = (Integer)tm.get("number");
					team.name = (String)tm.get("name");
					
					/* Set up control blocks. */
					team.controlPoints = new LinkedList<BlockCoord>();
					List<?> someList = (List<?>) tm.get("controlblocks");
					for (Object obj : someList) {
						String[] coords = ((String)obj).split(",");
						BlockCoord bcoord = new BlockCoord(arena.world_source, 
								Integer.valueOf(coords[0]),
								Integer.valueOf(coords[1]),
								Integer.valueOf(coords[2]));
						
						
						team.controlPoints.add(bcoord);
					}
					
					/* Set up revive points. */
					team.revivePoints = new LinkedList<BlockCoord>();
					someList = (List<?>) tm.get("revivepoints");
					for (Object obj : someList) {
						String[] coords = ((String)obj).split(",");
						BlockCoord bcoord = new BlockCoord(arena.world_source, 
								Integer.valueOf(coords[0]),
								Integer.valueOf(coords[1]),
								Integer.valueOf(coords[2]));
						
						
						team.revivePoints.add(bcoord);
					}
					
					/* Set up respawn points. */
					team.respawnPoints = new LinkedList<BlockCoord>();
					someList = (List<?>) tm.get("respawnpoints");
					for (Object obj : someList) {
						String[] coords = ((String)obj).split(",");
						BlockCoord bcoord = new BlockCoord(arena.world_source, 
								Integer.valueOf(coords[0]),
								Integer.valueOf(coords[1]),
								Integer.valueOf(coords[2]));
						
						
						team.respawnPoints.add(bcoord);
					}
					
					/* Set up chest points. */
					team.chests = new LinkedList<BlockCoord>();
					someList = (List<?>) tm.get("chests");
					for (Object obj : someList) {
						String[] coords = ((String)obj).split(",");
						BlockCoord bcoord = new BlockCoord(arena.world_source, 
								Integer.valueOf(coords[0]),
								Integer.valueOf(coords[1]),
								Integer.valueOf(coords[2]));
						
						
						team.chests.add(bcoord);
					}
					
					String respawnSignStr = (String)tm.get("respawnsign");
					String[] respawnSplit = respawnSignStr.split(",");
					team.respawnSign = new BlockCoord(arena.world_source,
							Integer.valueOf(respawnSplit[0]),
							Integer.valueOf(respawnSplit[1]),
							Integer.valueOf(respawnSplit[2]));
					
					arena.teams.add(team);
				}
			}
			


			config_arenas.put(arena.id, arena);
		}
		
		CivLog.info("Loaded "+config_arenas.size()+" arenas.");
	}	
}
