package com.avrgaming.civcraft.pvplogger;

import gpl.ImprovedOfflinePlayer;

import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.UUID;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.player.PlayerLoginEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.PlayerInventory;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementSoulBound;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.moblib.MobLib;
import com.avrgaming.moblib.MobLibEntity;

public class PvPLogger implements Listener, Runnable {

	/*
	 * This class listens on entity damage events, tags players with a time 
	 * that they were last hit.
	 * 
	 * Then it listens for disconnect events, if it finds a disconnect event it will
	 * spawn an NPC which can be killed. That NPC will drop the player's non-soulbound
	 * inventory and mark that resident as dead. Next login we clear his non-soulbound 
	 * inventory and kill them.
	 */
	
	class ZombiePlayer {
		Date spawnTime;
		UUID id;
		String playerName;
		
		public ZombiePlayer(Date spawnTime, UUID id, String playerName) {
			this.spawnTime = spawnTime;
			this.id = id;
			this.playerName = playerName;
		}
	}
	
	public static HashMap<String, Date> taggedPlayers = new HashMap<String, Date>();
	public static HashMap<String, ZombiePlayer> zombiePlayers = new HashMap<String, ZombiePlayer>();
	
	@EventHandler(priority = EventPriority.MONITOR) 
	public void onEntityDamageEvent(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		Player defender = null;
		
		if (event.getEntity() instanceof Player) {
			defender = (Player)event.getEntity();
		} else {
			/* not a player being attacked, dont care. */
			return;
		}
		
		if (event.getDamager() instanceof Arrow) {
			if (((Arrow)event.getDamager()).getShooter() instanceof Player) {
				//attacker = (Player)((Arrow)event.getDamager()).getShooter();
			} else {
				/* Some non-player shot arrow. */
				return;
			}
		} else if (event.getDamager() instanceof Player) {
			//attacker = (Player)event.getDamager();
		} else {
			/* not a player being attacked by a player, dont care. */
			return;
		}
		
		if (!taggedPlayers.containsKey(defender.getName())) {
			int logoutSeconds;
			try {
				logoutSeconds = CivSettings.getInteger(CivSettings.warConfig, "war.logout_time");
				CivMessage.send(defender, CivColor.LightGray+CivColor.BOLD+"You've been PvP tagged. If you log out within "+logoutSeconds+
						" seconds you can be killed while offline by ANYONE!");
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
			}
		}
		
		taggedPlayers.put(defender.getName(), new Date());
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onPlayerQuit(PlayerQuitEvent event) {
		
		/* If player quits for whatever reason, check if they were PVP tagged. */
		if (taggedPlayers.containsKey(event.getPlayer().getName())) {
			Date lastHit = taggedPlayers.get(event.getPlayer().getName());
			Calendar now = Calendar.getInstance();
			Calendar expire = Calendar.getInstance();
			expire.setTime(lastHit);

			try {
				int logoutSeconds = CivSettings.getInteger(CivSettings.warConfig, "war.logout_time");				
				expire.add(Calendar.SECOND, logoutSeconds);
				
				if (now.before(expire)) {
					/* Make a NPC. */
					
					//RemoteEntity entity = CivGlobal.entityManager.createNamedEntity(RemoteEntityType.Zombie, event.getPlayer().getLocation(), event.getPlayer().getName(), false);
					MobLibEntity entity = MobLib.createNamedEntity("com.avrgaming.civcraft.mobs.LoboZombie", event.getPlayer().getLocation(), event.getPlayer().getName());
					//entity.setStationary(true);
					zombiePlayers.put(event.getPlayer().getName(), new ZombiePlayer(new Date(), entity.getUid(), event.getPlayer().getName()));
				} else {
					/* Must be expired, remove key. */
					taggedPlayers.remove(event.getPlayer().getName());
				}
				
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				return;
			}			
		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityCombust(EntityCombustEvent event) {
		/* Stop our player NPCs from combusting in the sunlight. */
		/* XXX Handled inside the mob lib now... */
//		if (event.getEntity() instanceof LivingEntity) {
//			if (CivGlobal.entityManager.isRemoteEntity((LivingEntity) event.getEntity())) {
//				event.setCancelled(true);
//			}
//		}
	}
	
	@EventHandler(priority = EventPriority.LOWEST) 
	public void onPlayerLogin(PlayerLoginEvent event) {
		ZombiePlayer zombiePlayer = zombiePlayers.get(event.getPlayer().getName());
		if (zombiePlayer == null) {
			return;
		}
		
		//CivGlobal.entityManager.removeEntity(zombiePlayer.id, true);
		MobLib.removeEntity(zombiePlayer.id);
		zombiePlayers.remove(event.getPlayer().getName());
	}
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDeath(EntityDeathEvent event) {
		
		if (MobLib.isMobLibEntity(event.getEntity())) {
			/*
			 * Search for tagged players and their expired time, if the NPC is not expired
			 * wipe the offline player's inventory.
			 */
			String playerName = event.getEntity().getCustomName();
			ZombiePlayer zombiePlayer = zombiePlayers.get(playerName);
			if (zombiePlayer == null) {
				return;
			}
						
			Date spawnTime = zombiePlayer.spawnTime;
			if (spawnTime == null) {
				/* No spawn time found, expired zombie?? */
				event.getEntity().remove();
				return;
			}
			
			Calendar expire = Calendar.getInstance();
			Calendar now = Calendar.getInstance();

			expire.setTime(spawnTime);

			try {
				int zombieSeconds = CivSettings.getInteger(CivSettings.warConfig, "war.zombie_time");				
				expire.add(Calendar.SECOND, zombieSeconds);
				
				if (now.after(expire)) {
					/* Zombie expired.. hmm should have been removed?? */
					event.getEntity().remove();
					zombiePlayers.remove(playerName);
					taggedPlayers.remove(playerName);
					return;
				}
				
				/* Drop the player's inventory. */
				try {
					CivGlobal.getPlayer(event.getEntity().getCustomName());
					/* Player is already online! This shouldn't happen. invalid zombie. */
					event.getEntity().remove();
					zombiePlayers.remove(playerName);
					taggedPlayers.remove(playerName);
					return;
				} catch (CivException e) {
					/* XXX TODO Fix terrible control flow. */
				}
				
				LinkedList<ItemStack> droppedStuff = new LinkedList<ItemStack>();
				
				/* Player is OFFLINE, zombie is VALID, spawn/remove items. */
				ImprovedOfflinePlayer playerOffline = new ImprovedOfflinePlayer(event.getEntity().getCustomName());
				
				/* Drop player armor. */
				PlayerInventory inv = playerOffline.getInventory();
				ItemStack[] armorContents = inv.getArmorContents();
				for (int i = 0; i < armorContents.length; i++) {
					ItemStack stack = armorContents[i];
					if (stack == null || ItemManager.getId(stack) == CivData.AIR) {
						continue;
					}
					
					LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
					if (craftMat != null) {
						boolean found = false;
						for (LoreEnhancement ench : LoreCraftableMaterial.getEnhancements(stack)) {
							if (ench instanceof LoreEnhancementSoulBound) {
								found = true;
								break;
							}
						}
						
						if (found) {
							continue;
						}
					}
					
					// drop this item.
					droppedStuff.add(stack);
					armorContents[i] = null;
				}
				inv.setArmorContents(armorContents);
				
				/* Drop player inv contents. */
				ItemStack[] contents = inv.getContents();
				for (int i = 0; i < contents.length; i++) {
					ItemStack stack = contents[i];
					if (stack == null || ItemManager.getId(stack) == CivData.AIR) {
						continue;
					}
					
					LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
					if (craftMat != null) {	
						boolean found = false;
						for (LoreEnhancement ench : LoreCraftableMaterial.getEnhancements(stack)) {
							if (ench instanceof LoreEnhancementSoulBound) {
								found = true;
								break;
							}
						}
						
						if (found) {
							continue;
						}
					}
					
					// drop this item.
					droppedStuff.add(stack);
					contents[i] = null;
				}
				inv.setContents(contents);
				playerOffline.setInventory(inv);
			
				/* Now that the player's inventory was dropped, drop the items in the world. */
				for (ItemStack stack : droppedStuff) {
					if (ItemManager.getId(stack) == CivData.AIR) {
						continue;
					}
					event.getEntity().getLocation().getWorld().dropItem(event.getEntity().getLocation(), stack);
				}
				
				/* Finally this zombie's dead zed. */
				zombiePlayers.remove(playerName);
				taggedPlayers.remove(playerName);
				CivGlobal.getSessionDB().add("pvplogger:death:"+playerName, "", 0, 0, 0);
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				return;
			}
		}
	}
	
	@Override
	public void run() {
		/*
		 * Every 5 seconds check for expired zombies and remove them.
		 */
		LinkedList<String> removedKeys = new LinkedList<String>();
		for (String playerName : zombiePlayers.keySet()) {
			ZombiePlayer zombiePlayer = zombiePlayers.get(playerName);
			Date spawnTime = zombiePlayer.spawnTime;
			if (spawnTime == null) {
				continue;
			}
			
			Calendar expire = Calendar.getInstance();
			Calendar now = Calendar.getInstance();

			expire.setTime(spawnTime);

			try {
				int zombieSeconds = CivSettings.getInteger(CivSettings.warConfig, "war.zombie_time");				
				expire.add(Calendar.SECOND, zombieSeconds);
				
				if (now.after(expire)) {
					/* Zombie expired, remove. */
					removedKeys.add(playerName);
					//CivGlobal.entityManager.removeEntity(zombiePlayer.id, true);
					MobLib.removeEntity(zombiePlayer.id);
				}
			} catch (InvalidConfiguration e){
				e.printStackTrace();
			}
		}
		
		for (String name : removedKeys) {
			zombiePlayers.remove(name);
			taggedPlayers.remove(name);
		}
		
	}
	
	
}
