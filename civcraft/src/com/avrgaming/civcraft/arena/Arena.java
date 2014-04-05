package com.avrgaming.civcraft.arena;


import java.util.Collection;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.ConfigArena;
import com.avrgaming.civcraft.config.ConfigArenaTeam;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class Arena {
	public ConfigArena config;
	public int instanceID;
	
	private HashMap<Integer, ArenaTeam> teams = new HashMap<Integer, ArenaTeam>();
	private HashMap<Integer, Integer> teamIDmap = new HashMap<Integer, Integer>();
	private HashMap<Integer, Integer> teamHP = new HashMap<Integer, Integer>();
	private HashMap<String, Inventory> playerInvs = new HashMap<String, Inventory>();
	
	int teamCount = 0;
	
	public static int nextInstanceID = 0;
	
	public Arena(ConfigArena a) throws CivException {
		this.config = a;
		
		/* Search for a free instance id. */
		boolean found = false;
		int id = 0;
		for (int i = 0; i < ArenaManager.MAX_INSTANCES; i++) {
			String possibleName = getInstanceName(id, config);
			if (ArenaManager.activeArenas.containsKey(possibleName)) {
				id++;
			} else {
				found = true;
				break;
			}
		}
		
		if (!found) {
			throw new CivException("Couldn't find a free instance ID!");
		}
		
		this.instanceID = id;
	}

	public static String getInstanceName(int id, ConfigArena config) {
		String instanceWorldName = config.world_source+"_"+"instance_"+id;
		return instanceWorldName;
	}
	
	public String getInstanceName() {
		return getInstanceName(this.instanceID, config);
	}
	
	public void addTeam(ArenaTeam team) throws CivException {
		teams.put(teamCount, team);
		teamIDmap.put(team.getId(), teamCount);
		teamHP.put(teamCount, config.teams.get(teamCount).controlPoints.size());
		
		for (Resident resident : team.teamMembers) {
			try {
				teleportToRandomRevivePoint(resident, teamCount);
				createInventory(resident);
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
		
		teamCount++;
	}
	
	private void addCivCraftItemToInventory(String id, Inventory inv) {
		LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(id);
		ItemStack stack = LoreCraftableMaterial.spawn(craftMat);
		stack = LoreCraftableMaterial.addEnhancement(stack, LoreEnhancement.enhancements.get("LoreEnhancementSoulBound"));
		inv.addItem(stack);
	}
	
	private void addItemToInventory(Material mat, Inventory inv, int amount) {
		ItemStack stack = ItemManager.createItemStack(ItemManager.getId(mat), amount);
		stack = LoreCraftableMaterial.addEnhancement(stack, LoreEnhancement.enhancements.get("LoreEnhancementSoulBound"));
		inv.addItem(stack);
	}
	
	private void createInventory(Resident resident) {
		Player player;
		try {
			player = CivGlobal.getPlayer(resident);
			Inventory inv = Bukkit.createInventory(player, 9*6, resident.getName()+"'s Gear");

			addCivCraftItemToInventory("mat_tungsten_sword", inv);
			addCivCraftItemToInventory("mat_tungsten_boots", inv);
			addCivCraftItemToInventory("mat_tungsten_chestplate", inv);
			addCivCraftItemToInventory("mat_tungsten_leggings", inv);
			addCivCraftItemToInventory("mat_tungsten_helmet", inv);

			addCivCraftItemToInventory("mat_carbide_steel_sword", inv);
			addCivCraftItemToInventory("mat_carbide_steel_boots", inv);
			addCivCraftItemToInventory("mat_carbide_steel_chestplate", inv);
			addCivCraftItemToInventory("mat_carbide_steel_leggings", inv);
			addCivCraftItemToInventory("mat_carbide_steel_helmet", inv);	
			
			addCivCraftItemToInventory("mat_steel_sword", inv);
			addCivCraftItemToInventory("mat_steel_boots", inv);
			addCivCraftItemToInventory("mat_steel_chestplate", inv);
			addCivCraftItemToInventory("mat_steel_leggings", inv);
			addCivCraftItemToInventory("mat_steel_helmet", inv);
			
			addCivCraftItemToInventory("mat_iron_sword", inv);
			addCivCraftItemToInventory("mat_iron_boots", inv);
			addCivCraftItemToInventory("mat_iron_chestplate", inv);
			addCivCraftItemToInventory("mat_iron_leggings", inv);
			addCivCraftItemToInventory("mat_iron_helmet", inv);
			
			addCivCraftItemToInventory("mat_marksmen_bow", inv);
			addCivCraftItemToInventory("mat_composite_leather_boots", inv);
			addCivCraftItemToInventory("mat_composite_leather_chestplate", inv);
			addCivCraftItemToInventory("mat_composite_leather_leggings", inv);
			addCivCraftItemToInventory("mat_composite_leather_helmet", inv);
			
			addCivCraftItemToInventory("mat_longbow", inv);
			addCivCraftItemToInventory("mat_hardened_leather_boots", inv);
			addCivCraftItemToInventory("mat_hardened_leather_chestplate", inv);
			addCivCraftItemToInventory("mat_hardened_leather_leggings", inv);
			addCivCraftItemToInventory("mat_hardened_leather_helmet", inv);
			
			addCivCraftItemToInventory("mat_recurve_bow", inv);
			addCivCraftItemToInventory("mat_refined_leather_boots", inv);
			addCivCraftItemToInventory("mat_refined_leather_chestplate", inv);
			addCivCraftItemToInventory("mat_refined_leather_leggings", inv);
			addCivCraftItemToInventory("mat_refined_leather_helmet", inv);
			
			addCivCraftItemToInventory("mat_hunting_bow", inv);
			addCivCraftItemToInventory("mat_leather_Boots", inv);
			addCivCraftItemToInventory("mat_leather_chestplate", inv);
			addCivCraftItemToInventory("mat_leather_leggings", inv);
			addCivCraftItemToInventory("mat_leather_helmet", inv);
			
			addCivCraftItemToInventory("mat_vanilla_diamond_pickaxe", inv);

			addItemToInventory(Material.ARROW, inv, 64);
			addItemToInventory(Material.ARROW, inv, 64);
			addItemToInventory(Material.ARROW, inv, 64);
			addItemToInventory(Material.ARROW, inv, 64);
			addItemToInventory(Material.ARROW, inv, 64);
			addItemToInventory(Material.ARROW, inv, 64);
			addItemToInventory(Material.PUMPKIN_PIE, inv, 64);

			
			playerInvs.put(resident.getName(), inv);
			
		} catch (CivException e) {
			e.printStackTrace();
		}
		
	}
	
	private ConfigArenaTeam getConfigTeam(int id) throws CivException {
		for (ConfigArenaTeam ct : config.teams) {
			if (ct.number == id) {
				return ct;
			}
		}
		throw new CivException("Couldn't find configuration for team id:"+id);
	}
	
	public void teleportToRandomRevivePoint(Resident r, int teamID) throws CivException {
		ConfigArenaTeam ct = getConfigTeam(teamID);
		Random rand = new Random();
		int index = rand.nextInt(ct.revivePoints.size());
		
		int i = 0;
		for (BlockCoord coord : ct.revivePoints) {
			
			if (index == i) {
				try {
					Player player = CivGlobal.getPlayer(r);
					coord.setWorldname(this.getInstanceName());
					player.teleport(coord.getLocation());
				} catch (CivException e) {
					// Player offline..
					e.printStackTrace();
				}
			}
			i++;
		}

	}

	public void returnPlayers() {
		for (ArenaTeam team : teams.values()) {
			for (Resident r : team.teamMembers) {
				try {
					/* Only set inside arena to false if the player is online. */
					CivGlobal.getPlayer(r);
					r.setInsideArena(false);
					r.restoreInventory();
					r.teleportHome();
					r.save();
					CivMessage.send(r, CivColor.LightGray+"We've been teleported back to our home since the arena has ended.");
				} catch (CivException e) {
					/* player not online, inside arena is set true */
				}
			}
		}
	}

	public Collection<ArenaTeam> getTeams() {
		return teams.values();
	}
	
	public ArenaTeam getTeamFromID(int id) {
		return teams.get(id);
	}
	
	public void onControlBlockDestroy(int teamID, ArenaTeam attackingTeam) {
		Integer hp = teamHP.get(teamID);
		hp--;
		teamHP.put(teamID, hp);
		
		ArenaTeam team = teams.get(teamID);
		
		if (hp <= 0) {
			ArenaManager.declareVictor(this, team, attackingTeam);
		}
		
	}

	public void clearTeams() {
		for (ArenaTeam team : teams.values()) {
			team.setCurrentArena(null);
		}
		
		teams.clear();
	}

	public Location getRespawnLocation(Resident resident) {
		int teamID = teamIDmap.get(resident.getTeam().getId());
		for (int i = 0; i < config.teams.size(); i++) {
			ConfigArenaTeam configTeam = config.teams.get(i);
			if (configTeam.number == teamID) {
				Random rand = new Random();
				int index = rand.nextInt(configTeam.respawnPoints.size());
				return configTeam.respawnPoints.get(index).getCenteredLocation();
			}
		}
		
		return null;
	}

	public BlockCoord getRandomReviveLocation(Resident resident) {
		int teamID = teamIDmap.get(resident.getTeam().getId());
		for (int i = 0; i < config.teams.size(); i++) {
			ConfigArenaTeam configTeam = config.teams.get(i);
			if (configTeam.number == teamID) {
				Random rand = new Random();
				int index = rand.nextInt(configTeam.respawnPoints.size());
				return configTeam.revivePoints.get(index);
			}
		}
		
		return null;
	}

	public Inventory getInventory(Resident resident) {
		return playerInvs.get(resident.getName());
	}
	
}
