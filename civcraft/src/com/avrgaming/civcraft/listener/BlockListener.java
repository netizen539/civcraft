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
package com.avrgaming.civcraft.listener;

import gpl.HorseModifier;

import java.util.HashSet;
import java.util.Random;

import net.minecraft.server.v1_7_R4.NBTTagCompound;

import org.bukkit.Chunk;
import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.FireworkEffect.Type;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Fireball;
import org.bukkit.entity.ItemFrame;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Painting;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.entity.ThrownPotion;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockBurnEvent;
import org.bukkit.event.block.BlockDispenseEvent;
import org.bukkit.event.block.BlockFormEvent;
import org.bukkit.event.block.BlockFromToEvent;
import org.bukkit.event.block.BlockGrowEvent;
import org.bukkit.event.block.BlockIgniteEvent;
import org.bukkit.event.block.BlockPistonExtendEvent;
import org.bukkit.event.block.BlockPistonRetractEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityBreakDoorEvent;
import org.bukkit.event.entity.EntityChangeBlockEvent;
import org.bukkit.event.entity.EntityCreatePortalEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.EntityExplodeEvent;
import org.bukkit.event.entity.EntityInteractEvent;
import org.bukkit.event.entity.PotionSplashEvent;
import org.bukkit.event.entity.ProjectileHitEvent;
import org.bukkit.event.hanging.HangingBreakByEntityEvent;
import org.bukkit.event.player.PlayerBedEnterEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemConsumeEvent;
import org.bukkit.event.world.ChunkLoadEvent;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import com.avrgaming.civcraft.cache.ArrowFiredCache;
import com.avrgaming.civcraft.cache.CannonFiredCache;
import com.avrgaming.civcraft.cache.CivCache;
import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.camp.CampBlock;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTempleSacrifice;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.ProtectedBlock;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureBlock;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.road.Road;
import com.avrgaming.civcraft.road.RoadBlock;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.BuildableLayer;
import com.avrgaming.civcraft.structure.Farm;
import com.avrgaming.civcraft.structure.Pasture;
import com.avrgaming.civcraft.structure.Temple;
import com.avrgaming.civcraft.structure.Wall;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.threading.CivAsyncTask;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FireWorkTask;
import com.avrgaming.civcraft.threading.tasks.StructureBlockHitEvent;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarRegen;
import com.avrgaming.moblib.MobLib;



public class BlockListener implements Listener {

	/* Experimental, reuse the same object because it is single threaded. */
	public static ChunkCoord coord = new ChunkCoord("", 0, 0);
	public static BlockCoord bcoord = new BlockCoord("", 0,0,0);

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockIgniteEvent(BlockIgniteEvent event) {
	//	CivLog.debug("block ignite event");

		for (int x = -1; x <= 1; x++) {
			for (int y = -1; y <= 1; y++) {
				for (int z = -1; z <= 1; z++) {
					Block b = event.getBlock().getRelative(x, y, z);		
					bcoord.setFromLocation(b.getLocation());
					StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
					if (sb != null) {
						if (b.getType().isBurnable()) {
							event.setCancelled(true);
						}
						return;
					}

					RoadBlock rb = CivGlobal.getRoadBlock(bcoord);
					if (rb != null) {
						event.setCancelled(true);
						return;
					}

					CampBlock cb = CivGlobal.getCampBlock(bcoord);
					if (cb != null) {
						event.setCancelled(true);
						return;
					}

					StructureSign structSign = CivGlobal.getStructureSign(bcoord);
					if (structSign != null) {
						event.setCancelled(true);
						return;
					}

					StructureChest structChest = CivGlobal.getStructureChest(bcoord);
					if (structChest != null) {
						event.setCancelled(true);
						return;
					}
				}
			}
	    }


		coord.setFromLocation(event.getBlock().getLocation());
		TownChunk tc = CivGlobal.getTownChunk(coord);

		if (tc == null) {
			return;
		}

		if (tc.perms.isFire() == false) {
			CivMessage.sendError(event.getPlayer(), "Fire disabled in this chunk.");
			event.setCancelled(true);
		}		
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onEntityBlockChange(EntityChangeBlockEvent event) {
		bcoord.setFromLocation(event.getBlock().getLocation());

		StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
		if (sb != null) {
			event.setCancelled(true);
			return;
		}

		RoadBlock rb = CivGlobal.getRoadBlock(bcoord);
		if (rb != null) {
			event.setCancelled(true);
			return;
		}

		CampBlock cb = CivGlobal.getCampBlock(bcoord);
		if (cb != null) {
			event.setCancelled(true);
			return;
		}

		return;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onBlockBurnEvent(BlockBurnEvent event) {
		bcoord.setFromLocation(event.getBlock().getLocation());

		StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
		if (sb != null) {
			event.setCancelled(true);
			return;
		}

		RoadBlock rb = CivGlobal.getRoadBlock(bcoord);
		if (rb != null) {
			event.setCancelled(true);
			return;
		}

		CampBlock cb = CivGlobal.getCampBlock(bcoord);
		if (cb != null) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onProjectileHitEvent(ProjectileHitEvent event) {
		if (event.getEntity() instanceof Arrow) {
			ArrowFiredCache afc = CivCache.arrowsFired.get(event.getEntity().getUniqueId());
			if (afc != null) {
				afc.setHit(true);
			}
		}

		if (event.getEntity() instanceof Fireball) {
			CannonFiredCache cfc = CivCache.cannonBallsFired.get(event.getEntity().getUniqueId());
			if (cfc != null) {

				cfc.setHit(true);

				FireworkEffect fe = FireworkEffect.builder().withColor(Color.RED).withColor(Color.BLACK).flicker(true).with(Type.BURST).build();

				Random rand = new Random();
				int spread = 30;
				for (int i = 0; i < 15; i++) {
					int x = rand.nextInt(spread) - spread/2;
					int y = rand.nextInt(spread) - spread/2;
					int z = rand.nextInt(spread) - spread/2;


					Location loc = event.getEntity().getLocation();
					Location location = new Location(loc.getWorld(), loc.getX(),loc.getY(), loc.getZ());
					location.add(x, y, z);

					TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 5), rand.nextInt(30));
				}

			}
		}
	}

	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		/* Protect the Protected Item Frames! */
		if (event.getEntity() instanceof ItemFrame) {
			ItemFrameStorage iFrameStorage = CivGlobal.getProtectedItemFrame(event.getEntity().getUniqueId());
			if (iFrameStorage != null) {
				event.setCancelled(true);
				return;
			}
		}

		if (!(event.getEntity() instanceof Player)) {			
			return;
		}	

		Player defender = (Player)event.getEntity();
		/* Only protect agaisnt players and entities that players can throw. */
		if (!CivSettings.playerEntityWeapons.contains(event.getDamager().getType())) {
			return;
		}

		if (event.getDamager() instanceof Arrow) {

		}

		if (event.getDamager() instanceof Fireball) {
			CannonFiredCache cfc = CivCache.cannonBallsFired.get(event.getDamager().getUniqueId());
			if (cfc != null) {

				cfc.setHit(true);
				cfc.destroy(event.getDamager());
				event.setDamage((double)cfc.getFromTower().getDamage());
			}
		}

		coord.setFromLocation(event.getEntity().getLocation());
		TownChunk tc = CivGlobal.getTownChunk(coord);
		boolean allowPVP = false;
		String denyMessage = "";

		if (tc == null) {
			/* In the wilderness, anything goes. */
			allowPVP = true;
		} else {	
			Player attacker = null;
			if (event.getDamager() instanceof Player) {
				attacker = (Player)event.getDamager();
			} else if (event.getDamager() instanceof Projectile) {
				LivingEntity shooter = (LivingEntity) ((Projectile)event.getDamager()).getShooter();
				if (shooter instanceof Player) {
					attacker = (Player) shooter;
				}
			} 

			if (attacker == null) {
				/* Attacker wasnt a player or known projectile, allow it. */
				allowPVP = true;
			} else {
				switch(playersCanPVPHere(attacker, defender, tc)) {
				case ALLOWED:
					allowPVP = true;
					break;
				case NOT_AT_WAR:
					allowPVP = false;
					denyMessage = "You cannot PvP with "+defender.getName()+" as you are not at war.";
					break;
				case NEUTRAL_IN_WARZONE:
					allowPVP = false;
					denyMessage = "You cannot PvP here with "+defender.getName()+" since you are a neutral in a war-zone.";
					break;
				case NON_PVP_ZONE:
					allowPVP = false;
					denyMessage = "You cannot PvP with "+defender.getName()+" since you are in a non-pvp zone.";
					break;
				}
			}

			if (!allowPVP) {
				CivMessage.sendError(attacker, denyMessage);
				event.setCancelled(true);
			} else {

			}
		}

		return;
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnCreateSpawnEvent(CreatureSpawnEvent event) {

		if (event.getSpawnReason().equals(SpawnReason.BREEDING)) {
			ChunkCoord coord = new ChunkCoord(event.getEntity().getLocation());
			Pasture pasture = Pasture.pastureChunks.get(coord);

			if (pasture != null) {
				pasture.onBreed(event.getEntity());
			}
		}

		class SyncTask implements Runnable {
			LivingEntity entity;

			public SyncTask(LivingEntity entity) {
				this.entity = entity;
			}

			@Override
			public void run() {
				if (entity != null) {
					if (!HorseModifier.isCivCraftHorse(entity)) {
						CivLog.warning("Removing a normally spawned horse.");
						entity.remove();
					}
				}
			}
		}

		if (event.getEntityType() == EntityType.HORSE) {
			if (event.getSpawnReason().equals(SpawnReason.DEFAULT)) {
				TaskMaster.syncTask(new SyncTask(event.getEntity()));
				return;
			}

			CivLog.warning("Canceling horse spawn reason:"+event.getSpawnReason().name());
			event.setCancelled(true);
		}

		coord.setFromLocation(event.getLocation());
		TownChunk tc = CivGlobal.getTownChunk(coord);
		if (tc == null) {
			return;
		}

		if (tc.perms.isMobs() == false) {
			if (event.getSpawnReason().equals(SpawnReason.CUSTOM)) {
				return;
			}

			if (CivSettings.restrictedSpawns.containsKey(event.getEntityType())) {
				event.setCancelled(true);
				return;
			}
		}		
	}


	@EventHandler(priority = EventPriority.NORMAL)
	public void OnEntityExplodeEvent(EntityExplodeEvent event) {
		if (event.getEntity() == null) {
			return;
		}
		/* prevent ender dragons from breaking blocks. */
		if (event.getEntityType().equals(EntityType.COMPLEX_PART)) {
			event.setCancelled(true);
		} else if (event.getEntityType().equals(EntityType.ENDER_DRAGON)) {
			event.setCancelled(true);
		}

		for (Block block : event.blockList()) {
			bcoord.setFromLocation(block.getLocation());
			StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
			if (sb != null) {
				event.setCancelled(true);
				return;
			}

			RoadBlock rb = CivGlobal.getRoadBlock(bcoord);
			if (rb != null) {
				event.setCancelled(true);
				return;
			}

			CampBlock cb = CivGlobal.getCampBlock(bcoord);
			if (cb != null) {
				event.setCancelled(true);
				return;
			}

			StructureSign structSign = CivGlobal.getStructureSign(bcoord);
			if (structSign != null) {
				event.setCancelled(true);
				return;
			}

			StructureChest structChest = CivGlobal.getStructureChest(bcoord);
			if (structChest != null) {
				event.setCancelled(true);
				return;
			}

			coord.setFromLocation(block.getLocation());

			HashSet<Wall> walls = CivGlobal.getWallChunk(coord);
			if (walls != null) {
				for (Wall wall : walls) {
					if (wall.isProtectedLocation(block.getLocation())) {
						event.setCancelled(true);
						return;
					}
				}
			}

			TownChunk tc = CivGlobal.getTownChunk(coord);
			if (tc == null) {
				continue;
			}

			event.setCancelled(true);
			return;
		}

	}

     private final BlockFace[] faces = new BlockFace[] {
			        BlockFace.DOWN,
		            BlockFace.NORTH,
		            BlockFace.EAST,
		            BlockFace.SOUTH,
		            BlockFace.WEST,		            
		            BlockFace.SELF,
		            BlockFace.UP
	  };

    public BlockCoord generatesCobble(int id, Block b)
    {
        int mirrorID1 = (id == CivData.WATER_RUNNING || id == CivData.WATER ? CivData.LAVA_RUNNING : CivData.WATER_RUNNING);
        int mirrorID2 = (id == CivData.WATER_RUNNING || id == CivData.WATER ? CivData.LAVA : CivData.WATER);
        for(BlockFace face : faces)
        {
            Block r = b.getRelative(face, 1);
            if(ItemManager.getId(r) == mirrorID1 || ItemManager.getId(r) == mirrorID2)
            {
            	
            	return new BlockCoord(r);
            }
        }
        
        return null;
    }

//    private static void destroyLiquidRecursive(Block source) {
//    	//source.setTypeIdAndData(CivData.AIR, (byte)0, false);
//    	NMSHandler nms = new NMSHandler();
//    	nms.setBlockFast(source.getWorld(), source.getX(), source.getY(), source.getZ(), 0, (byte)0);
//    	
//    	for (BlockFace face : BlockFace.values()) {
//    		Block relative = source.getRelative(face);
//    		if (relative == null) {
//    			continue;
//    		}
//    		
//    		if (!isLiquid(relative.getTypeId())) {
//    			continue;
//    		}
//    		
//    		destroyLiquidRecursive(relative);
//    	}
//    }
    
//    private static boolean isLiquid(int id) {
//    	return (id >= CivData.WATER && id <= CivData.LAVA);
//    }
    
    private static HashSet<BlockCoord> stopCobbleTasks = new HashSet<BlockCoord>();
	@EventHandler(priority = EventPriority.NORMAL)
	public void OnBlockFromToEvent(BlockFromToEvent event) {
		/* Disable cobblestone generators. */
		int id = ItemManager.getId(event.getBlock());
	    if(id >= CivData.WATER && id <= CivData.LAVA)
	    {
	        Block b = event.getToBlock();
	        bcoord.setFromLocation(b.getLocation());

	        int toid = ItemManager.getId(b);
	        if(toid == 0)
	        {
	            BlockCoord other = generatesCobble(id, b);
	        	if(other != null)
	            {
	            	//BlockCoord d = new BlockCoord(event.getToBlock());
//	            	BlockCoord fromCoord = new BlockCoord(event.getBlock());
	            	event.setCancelled(true);

	            	class SyncTask implements Runnable {
	            		BlockCoord block;

	            		public SyncTask(BlockCoord block) {
	            			this.block = block;
	            		}

						@Override
						public void run() {
							ItemManager.setTypeIdAndData(block.getBlock(), CivData.NETHERRACK, (byte)0, true);
							stopCobbleTasks.remove(block);
						}
	            	}

	            	if (!stopCobbleTasks.contains(other)) {
	            		stopCobbleTasks.add(other);
	            		TaskMaster.syncTask(new SyncTask(other), 2);
	            	}

//	            	if (!stopCobbleTasks.contains(fromCoord)) {
//	            		stopCobbleTasks.add(fromCoord);
//	            		TaskMaster.syncTask(new SyncTask(fromCoord));
//	            	}
	            }
	        }
	    }
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnBlockFormEvent (BlockFormEvent event) {

		/* Disable cobblestone generators. */
		if (ItemManager.getId(event.getNewState()) == CivData.COBBLESTONE) {
			ItemManager.setTypeId(event.getNewState(), CivData.GRAVEL);
			return;
		}

		Chunk spreadChunk = event.getNewState().getChunk();
		coord.setX(spreadChunk.getX());
		coord.setZ(spreadChunk.getZ());
		coord.setWorldname(spreadChunk.getWorld().getName());

		TownChunk tc = CivGlobal.getTownChunk(coord);
		if (tc == null) {
			return;
		}

		if (tc.perms.isFire() == false) {
			if(event.getNewState().getType() == Material.FIRE) {
				event.setCancelled(true);
			}
		}
	}

	@EventHandler(priority = EventPriority.LOW)
	public void OnBlockPlaceEvent(BlockPlaceEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());

		if (resident == null) {
			event.setCancelled(true);
			return;
		}

		if(resident.isSBPermOverride()) {
			return;
		}

		bcoord.setFromLocation(event.getBlockAgainst().getLocation());
		StructureSign sign = CivGlobal.getStructureSign(bcoord);
		if (sign != null) {
			event.setCancelled(true);
			return;
		}

		bcoord.setFromLocation(event.getBlock().getLocation());
		StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
		if (sb != null) {
			event.setCancelled(true);
			CivMessage.sendError(event.getPlayer(), 
					"This block belongs to a "+sb.getOwner().getDisplayName()+" and cannot be destroyed.");
			return;
		}

		RoadBlock rb = CivGlobal.getRoadBlock(bcoord);
		if (rb != null) {
			if (rb.isAboveRoadBlock()) {
				if (resident.getCiv() != rb.getRoad().getCiv()) {
					event.setCancelled(true);
					CivMessage.sendError(event.getPlayer(), 
							"Cannot place blocks "+(Road.HEIGHT-1)+" blocks above a road that does not belong to your civ.");
				}
			}
			return;
		}

		CampBlock cb = CivGlobal.getCampBlock(bcoord);
		if (cb != null && !cb.canBreak(event.getPlayer().getName())) {
			event.setCancelled(true);
			CivMessage.sendError(event.getPlayer(), "This block is part of camp "+cb.getCamp().getName()+" owned by "+cb.getCamp().getOwner().getName()+" and cannot be destroyed.");
			return;
		}  		

		coord.setFromLocation(event.getBlock().getLocation());
		TownChunk tc = CivGlobal.getTownChunk(coord);
		if (CivSettings.blockPlaceExceptions.get(event.getBlock().getType()) != null) {
			return;
		}

		if (tc != null) {	
			if(!tc.perms.hasPermission(PlotPermissions.Type.BUILD, resident)) {
				if (War.isWarTime() && resident.hasTown() && 
						resident.getTown().getCiv().getDiplomacyManager().atWarWith(tc.getTown().getCiv())) {
					if (WarRegen.canPlaceThisBlock(event.getBlock())) {
						WarRegen.saveBlock(event.getBlock(), tc.getTown().getName(), true);
						return;
					} else {
						CivMessage.sendErrorNoRepeat(event.getPlayer(), "Cannot place this type of block in an emeny town during WarTime.");
						event.setCancelled(true);
						return;
					}
				} else {
					event.setCancelled(true);
					CivMessage.sendError(event.getPlayer(), "You do not have permission to build here.");
				}
			} 
		}

		/* Check if we're going to break too many structure blocks beneath a structure. */
		//LinkedList<StructureBlock> sbList = CivGlobal.getStructureBlocksAt(bcoord.getWorldname(), bcoord.getX(), bcoord.getZ());
		HashSet<Buildable> buildables = CivGlobal.getBuildablesAt(bcoord);
		if (buildables != null) {
			for (Buildable buildable : buildables) {		
				if (!buildable.validated) {
					try {
						buildable.validate(event.getPlayer());
					} catch (CivException e) {
						e.printStackTrace();
					}
					continue;
				}

				/* Building is validated, grab the layer and determine if this would set it over the limit. */
				BuildableLayer layer = buildable.layerValidPercentages.get(bcoord.getY());
				if (layer == null) {
					continue;
				}

				/* Update the layer. */
				layer.current += Buildable.getReinforcementValue(ItemManager.getId(event.getBlockPlaced()));
				if (layer.current < 0) {
					layer.current = 0;
				}
				buildable.layerValidPercentages.put(bcoord.getY(), layer);
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnBlockBreakEvent(BlockBreakEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());

		if (resident == null) {
			event.setCancelled(true);
			return;
		}

		if (resident.isSBPermOverride()) {
			return;
		}

		bcoord.setFromLocation(event.getBlock().getLocation());
		StructureBlock sb = CivGlobal.getStructureBlock(bcoord);

		if (sb != null) {
			event.setCancelled(true);
			TaskMaster.syncTask(new StructureBlockHitEvent(event.getPlayer().getName(), bcoord, sb, event.getBlock().getWorld()), 0);
			return;
		}

		RoadBlock rb = CivGlobal.getRoadBlock(bcoord);
		if (rb != null && !rb.isAboveRoadBlock()) {
			if (War.isWarTime()) {
				/* Allow blocks to be 'destroyed' during war time. */
				WarRegen.destroyThisBlock(event.getBlock(), rb.getTown());
				event.setCancelled(true);
				return;
			} else {
				event.setCancelled(true);
				rb.onHit(event.getPlayer());
				return;
			}
		}

		ProtectedBlock pb = CivGlobal.getProtectedBlock(bcoord);
		if (pb != null) {
			event.setCancelled(true);
			CivMessage.sendError(event.getPlayer(), "This block is protected and cannot be destroyed.");
			return;
		}

		CampBlock cb = CivGlobal.getCampBlock(bcoord);
		if (cb != null && !cb.canBreak(event.getPlayer().getName())) {
			ControlPoint cBlock = cb.getCamp().controlBlocks.get(bcoord);
			if (cBlock != null) {
				cb.getCamp().onDamage(1, event.getBlock().getWorld(), event.getPlayer(), bcoord, null);
				event.setCancelled(true);
				return;
			} else {	
				event.setCancelled(true);
				CivMessage.sendError(event.getPlayer(), "This block is part of camp "+cb.getCamp().getName()+" owned by "+cb.getCamp().getOwner().getName()+" and cannot be destroyed.");
				return;
			}
		}

		StructureSign structSign = CivGlobal.getStructureSign(bcoord);
		if (structSign != null && !resident.isSBPermOverride()) {
			event.setCancelled(true);
			CivMessage.sendError(event.getPlayer(), "Please do not destroy signs.");
			return;
		}

		StructureChest structChest = CivGlobal.getStructureChest(bcoord);
		if (structChest != null && !resident.isSBPermOverride()) {
			event.setCancelled(true);
			CivMessage.sendError(event.getPlayer(), "Please do not destroy chests.");
			return;
		}

		coord.setFromLocation(event.getBlock().getLocation());
		HashSet<Wall> walls = CivGlobal.getWallChunk(coord);

		if (walls != null) {
			for (Wall wall : walls) {
				if (wall.isProtectedLocation(event.getBlock().getLocation())) {
					if (resident == null || !resident.hasTown() || resident.getTown().getCiv() != wall.getTown().getCiv() && !resident.isSBPermOverride()) {
						
						StructureBlock tmpStructureBlock = new StructureBlock(bcoord, wall);
						tmpStructureBlock.setAlwaysDamage(true);
						TaskMaster.syncTask(new StructureBlockHitEvent(event.getPlayer().getName(), bcoord, tmpStructureBlock, event.getBlock().getWorld()), 0);
						//CivMessage.sendError(event.getPlayer(), "Cannot destroy this block, protected by a wall, destroy it first.");
						event.setCancelled(true);
						return;
					} else {
						CivMessage.send(event.getPlayer(), CivColor.LightGray+"We destroyed a block protected by a wall. This was allowed because we're a member of "+
								resident.getTown().getCiv().getName());
						break;
					}
				}
			}
		}

		TownChunk tc = CivGlobal.getTownChunk(coord);
		if (tc != null) {
			if(!tc.perms.hasPermission(PlotPermissions.Type.DESTROY, resident)) {
				event.setCancelled(true);

				if (War.isWarTime() && resident.hasTown() && 
						resident.getTown().getCiv().getDiplomacyManager().atWarWith(tc.getTown().getCiv())) {
					WarRegen.destroyThisBlock(event.getBlock(), tc.getTown());
				} else {
					CivMessage.sendErrorNoRepeat(event.getPlayer(), "You do not have permission to destroy here.");
				}
			}
		}

		/* Check if we're going to break too many structure blocks beneath a structure. */
		//LinkedList<StructureBlock> sbList = CivGlobal.getStructureBlocksAt(bcoord.getWorldname(), bcoord.getX(), bcoord.getZ());
		HashSet<Buildable> buildables = CivGlobal.getBuildablesAt(bcoord);
		if (buildables != null) {
			for (Buildable buildable : buildables) {
				if (!buildable.validated) {
					try {
						buildable.validate(event.getPlayer());
					} catch (CivException e) {
						e.printStackTrace();
					}
					continue;
				}

				/* Building is validated, grab the layer and determine if this would set it over the limit. */
				BuildableLayer layer = buildable.layerValidPercentages.get(bcoord.getY());
				if (layer == null) {
					continue;
				}

				double current = layer.current - Buildable.getReinforcementValue(ItemManager.getId(event.getBlock()));
				if (current < 0) {
					current = 0;
				}
				Double percentValid = (double)(current) / (double)layer.max;

				if (percentValid < Buildable.validPercentRequirement) {
					CivMessage.sendError(event.getPlayer(), "Cannot break this block since it's supporting the "+buildable.getDisplayName()+" above it.");
					event.setCancelled(true);
					return;
				}

				/* Update the layer. */
				layer.current = (int)current;
				buildable.layerValidPercentages.put(bcoord.getY(), layer);
			}
		}

	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnEntityInteractEvent(EntityInteractEvent event) {
		if (event.getBlock() != null) {			
			if (CivSettings.switchItems.contains(event.getBlock().getType())) {
				coord.setFromLocation(event.getBlock().getLocation());
				TownChunk tc = CivGlobal.getTownChunk(coord);

				if (tc == null) {
					return;
				}

				/* A non-player entity is trying to trigger something, if interact permission is
				 * off for others then disallow it.
				 */
				if (tc.perms.interact.isPermitOthers()) {
					return;
				}

				if (event.getEntity() instanceof Player) {
					CivMessage.sendErrorNoRepeat((Player)event.getEntity(), "You do not have permission to interact here...");
				}

				event.setCancelled(true);
			}
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnPlayerConsumeEvent(PlayerItemConsumeEvent event) {
		ItemStack stack = event.getItem();

		/* Disable notch apples */
		if (ItemManager.getId(event.getItem()) == ItemManager.getId(Material.GOLDEN_APPLE)) {
			if (event.getItem().getDurability() == (short)0x1) {
				CivMessage.sendError(event.getPlayer(), "You cannot use notch apples. Sorry.");
				event.setCancelled(true);
				return;
			}
		}	

		if (stack.getType().equals(Material.POTION)) {
			int effect = event.getItem().getDurability() & 0x000F;			
			if (effect == 0xE) {
				event.setCancelled(true);
				CivMessage.sendError(event.getPlayer(), "You cannot use invisibility potions for now... Sorry.");
				return;
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL) 
	public void onBlockDispenseEvent(BlockDispenseEvent event) {
		ItemStack stack = event.getItem();
		if (stack != null) {
			if (event.getItem().getType().equals(Material.POTION)) {
				int effect = event.getItem().getDurability() & 0x000F;			
				if (effect == 0xE) { 
					event.setCancelled(true);
					return;
				}
			}

			if (event.getItem().getType().equals(Material.INK_SACK)) {
				//if (event.getItem().getDurability() == 15) { 
					event.setCancelled(true);
					return;
				//}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void OnPlayerInteractEvent(PlayerInteractEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());

		if (resident == null) {
			event.setCancelled(true);
			return;
		}
		
		if (event.isCancelled()) {
			// Fix for bucket bug.
			if (event.getAction() == Action.RIGHT_CLICK_AIR) {
				Integer item = ItemManager.getId(event.getPlayer().getItemInHand());
				// block cheats for placing water/lava/fire/lighter use.
				if (item == 326 || item == 327 || item == 259 || (item >= 8 && item <= 11) || item == 51) { 
					event.setCancelled(true);
				}
			}
			return;
		}		
		
		if (event.hasItem()) {

			if (event.getItem().getType().equals(Material.POTION)) {
				int effect = event.getItem().getDurability() & 0x000F;			
				if (effect == 0xE) {
					event.setCancelled(true);
					CivMessage.sendError(event.getPlayer(), "You cannot use invisibility potions for now.. Sorry.");
					return;
				}
			}

			if (event.getItem().getType().equals(Material.INK_SACK) && event.getItem().getDurability() == 15) {
				Block clickedBlock = event.getClickedBlock();
				if (ItemManager.getId(clickedBlock) == CivData.WHEAT || 
					ItemManager.getId(clickedBlock) == CivData.CARROTS || 
					ItemManager.getId(clickedBlock) == CivData.POTATOES) {
					event.setCancelled(true);
					CivMessage.sendError(event.getPlayer(), "You cannot use bone meal on carrots, wheat, or potatoes.");
					return;
				}
			}
		}

		Block soilBlock = event.getPlayer().getLocation().getBlock().getRelative(BlockFace.DOWN);

		// prevent players trampling crops
		if ((event.getAction() == Action.PHYSICAL)) {
			if ((soilBlock.getType() == Material.SOIL) || (soilBlock.getType() == Material.CROPS)) {
				//CivLog.debug("no crop cancel.");
				event.setCancelled(true);
				return;	
			}
		}

		/* 
		 * Right clicking causes some dupe bugs for some reason with items that have "actions" such as swords.
		 * It also causes block place events on top of signs. So we'll just only allow signs to work with left click.
		 */
		boolean leftClick = event.getAction().equals(Action.LEFT_CLICK_AIR) || event.getAction().equals(Action.LEFT_CLICK_BLOCK);

		if (event.getClickedBlock() != null) {		

			if (MarkerPlacementManager.isPlayerInPlacementMode(event.getPlayer())) {
				Block block;
				if (event.getBlockFace().equals(BlockFace.UP)) {
					block = event.getClickedBlock().getRelative(event.getBlockFace());
				} else {
					block = event.getClickedBlock();
				}

				try {
					MarkerPlacementManager.setMarker(event.getPlayer(), block.getLocation());
					CivMessage.send(event.getPlayer(), CivColor.LightGreen+"Marked Location.");
				} catch (CivException e) {
					CivMessage.send(event.getPlayer(), CivColor.Rose+e.getMessage());
				}

				event.setCancelled(true);
				return;
			}

			// Check for clicked structure signs.
			bcoord.setFromLocation(event.getClickedBlock().getLocation());
			StructureSign sign = CivGlobal.getStructureSign(bcoord);
			if (sign != null) {
				if (leftClick || sign.isAllowRightClick()) {
					if (sign.getOwner() != null && sign.getOwner().isActive()) {
						try {
							sign.getOwner().processSignAction(event.getPlayer(), sign, event);
							event.setCancelled(true);
						} catch (CivException e) {
							CivMessage.send(event.getPlayer(), CivColor.Rose+e.getMessage());
							event.setCancelled(true);
							return;
						}
					}
				}
				return;
			}

			if (CivSettings.switchItems.contains(event.getClickedBlock().getType())) {
				OnPlayerSwitchEvent(event);
				if (event.isCancelled()) {
					return;
				}
			}
		}

		if (event.hasItem()) {

			if (event.getItem() == null) {
			} else {
				if (CivSettings.restrictedItems.containsKey(event.getItem().getType())) {
					OnPlayerUseItem(event);
					if (event.isCancelled()) {
						return;
					}
				}
			}
		}

	}
	
	public void OnPlayerBedEnterEvent(PlayerBedEnterEvent event) {
		
		Resident resident = CivGlobal.getResident(event.getPlayer().getName());

		if (resident == null) {
			event.setCancelled(true);
			return;
		}
				
		coord.setFromLocation(event.getPlayer().getLocation());
		Camp camp = CivGlobal.getCampFromChunk(coord);
		if (camp != null) {
			if (!camp.hasMember(event.getPlayer().getName())) {
				CivMessage.sendError(event.getPlayer(), "You cannot sleep in a camp you do not belong to.");
				event.setCancelled(true);
				return;
			}
		}		
	}

	public static void OnPlayerSwitchEvent(PlayerInteractEvent event) {

		if (event.getClickedBlock() == null) {
			return;
		}

		Resident resident = CivGlobal.getResident(event.getPlayer().getName());

		if (resident == null) {
			event.setCancelled(true);
			return;
		}

		bcoord.setFromLocation(event.getClickedBlock().getLocation());
		CampBlock cb = CivGlobal.getCampBlock(bcoord);
		if (cb != null && !resident.isPermOverride()) {
			if (!cb.getCamp().hasMember(resident.getName())) {
				CivMessage.sendError(event.getPlayer(), "You cannot interact with a camp you do not belong to.");
				event.setCancelled(true);
				return;
			}
		}

		coord.setFromLocation(event.getClickedBlock().getLocation());
		TownChunk tc = CivGlobal.getTownChunk(coord);

		if (tc == null) {
			return;
		}

		if (resident.hasTown()) {
			if (War.isWarTime()) {
				if(tc.getTown().getCiv().getDiplomacyManager().atWarWith(resident.getTown().getCiv())) {

					switch (event.getClickedBlock().getType()) {
					case WOODEN_DOOR:
					case IRON_DOOR:
						return;
					default:
						break;
					}
				}
			}
		}

		event.getClickedBlock().getType();

		if(!tc.perms.hasPermission(PlotPermissions.Type.INTERACT, resident)) {
			event.setCancelled(true);

			if (War.isWarTime() && resident.hasTown() && 
					resident.getTown().getCiv().getDiplomacyManager().atWarWith(tc.getTown().getCiv())) {
				WarRegen.destroyThisBlock(event.getClickedBlock(), tc.getTown());
			} else {
				CivMessage.sendErrorNoRepeat(event.getPlayer(), "You do not have permission to interact with "+event.getClickedBlock().getType().toString()+" here.");
			}
		}

		return;
	}

	private void OnPlayerUseItem(PlayerInteractEvent event) {
		Location loc = (event.getClickedBlock() == null) ? 
				event.getPlayer().getLocation() : 
				event.getClickedBlock().getLocation();

		ItemStack stack = event.getItem();

		coord.setFromLocation(event.getPlayer().getLocation());
		Camp camp = CivGlobal.getCampFromChunk(coord);
		if (camp != null) {
			if (!camp.hasMember(event.getPlayer().getName())) {
				CivMessage.sendError(event.getPlayer(), "You cannot use "+stack.getType().toString()+" in a camp you do not belong to.");
				event.setCancelled(true);
				return;
			}
		}

		TownChunk tc = CivGlobal.getTownChunk(loc);
		if (tc == null) {
			return;
		}

		Resident resident = CivGlobal.getResident(event.getPlayer().getName());

		if (resident == null) {
			event.setCancelled(true);
		}

		if(!tc.perms.hasPermission(PlotPermissions.Type.ITEMUSE, resident)) {
			event.setCancelled(true);
			CivMessage.sendErrorNoRepeat(event.getPlayer(), "You do not have permission to use "+stack.getType().toString()+" here.");
		}

		return;
	}

	/*
	 * Handles rotating of itemframes
	 */
	@EventHandler(priority = EventPriority.HIGHEST)
	public void OnPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {

		if (event.getRightClicked().getType().equals(EntityType.HORSE)) {
			if (!HorseModifier.isCivCraftHorse((LivingEntity)event.getRightClicked())) {
				CivMessage.sendError(event.getPlayer(), "Invalid horse! You can only get horses from stable structures.");
				event.setCancelled(true);
				event.getRightClicked().remove();
				return;
			}
		}

		ItemStack inHand = event.getPlayer().getItemInHand();
			if (inHand != null) {

				boolean denyBreeding = false;
				switch (event.getRightClicked().getType()) {
				case COW:
				case SHEEP:
				case MUSHROOM_COW:
					if (inHand.getType().equals(Material.WHEAT)) {
						denyBreeding = true;
					}
					break;
				case PIG:
					if (inHand.getType().equals(Material.CARROT_ITEM)) {
						denyBreeding = true;
					}
					break;
				case HORSE:
					if (inHand.getType().equals(Material.GOLDEN_APPLE) ||
							inHand.getType().equals(Material.GOLDEN_CARROT)) {
						CivMessage.sendError(event.getPlayer(), "You cannot breed horses, buy them from the stable.");
						event.setCancelled(true);
						return;
					}
					break;
				case CHICKEN:
					if (inHand.getType().equals(Material.SEEDS) ||
						inHand.getType().equals(Material.MELON_SEEDS) ||
						inHand.getType().equals(Material.PUMPKIN_SEEDS)) {
						denyBreeding = true;
					}
					break;
				default:
					break;
				}

				if (denyBreeding) {
					ChunkCoord coord = new ChunkCoord(event.getPlayer().getLocation());
					Pasture pasture = Pasture.pastureChunks.get(coord);

					if (pasture == null) {
						CivMessage.sendError(event.getPlayer(), "You cannot breed mobs in the wild, take them to a pasture.");
						event.setCancelled(true);
					} else {
							int loveTicks;
							NBTTagCompound tag = new NBTTagCompound();
							((CraftEntity)event.getRightClicked()).getHandle().c(tag);
							loveTicks = tag.getInt("InLove");

							if (loveTicks == 0) {	
								if(!pasture.processMobBreed(event.getPlayer(), event.getRightClicked().getType())) {
									event.setCancelled(true);
								}
							} else {
								event.setCancelled(true);
							}
					}

					return;			
				}
			}
		if (!(event.getRightClicked() instanceof ItemFrame) && !(event.getRightClicked() instanceof Painting)) {
			return;
		}

		coord.setFromLocation(event.getPlayer().getLocation());
		TownChunk tc = CivGlobal.getTownChunk(coord);
		if (tc == null) {
			return;
		}

		Resident resident = CivGlobal.getResident(event.getPlayer().getName());
		if (resident == null) {
			return;
		}

		if(!tc.perms.hasPermission(PlotPermissions.Type.INTERACT, resident)) {
			event.setCancelled(true);
			CivMessage.sendErrorNoRepeat(event.getPlayer(), "You do not have permission to interact with this painting/itemframe.");
		}

	}


	/*
	 * Handles breaking of paintings and itemframes.
	 */
	@EventHandler(priority = EventPriority.NORMAL)
	public void OnHangingBreakByEntityEvent(HangingBreakByEntityEvent event) {	
	//	CivLog.debug("hanging painting break event");

		ItemFrameStorage frameStore = CivGlobal.getProtectedItemFrame(event.getEntity().getUniqueId());
		if (frameStore != null) {		
//			if (!(event.getRemover() instanceof Player)) {
//				event.setCancelled(true);
//				return;
//			}
//			
//			if (frameStore.getTown() != null) {
//				Resident resident = CivGlobal.getResident((Player)event.getRemover());
//				if (resident == null) {
//					event.setCancelled(true);
//					return;
//				}
//				
//				if (resident.hasTown() == false || resident.getTown() != frameStore.getTown()) {
//					event.setCancelled(true);
//					CivMessage.sendError((Player)event.getRemover(), "Cannot remove item from protected item frame. Belongs to another town.");
//					return;
//				}
//			}
//			
//			CivGlobal.checkForEmptyDuplicateFrames(frameStore);
//			
//			ItemStack stack = ((ItemFrame)event.getEntity()).getItem();
//			if (stack != null && !stack.getType().equals(Material.AIR)) {
//				BonusGoodie goodie = CivGlobal.getBonusGoodie(stack);
//				if (goodie != null) {
//					frameStore.getTown().onGoodieRemoveFromFrame(frameStore, goodie);
//				}
//				frameStore.clearItem();
//				TaskMaster.syncTask(new DelayItemDrop(stack, event.getEntity().getLocation()));
//			}
			if (event.getRemover() instanceof Player) {
				CivMessage.sendError((Player)event.getRemover(), "Cannot break protected item frames. Right click to interact instead.");
			}
			event.setCancelled(true);	
			return;
		}

		if (event.getRemover() instanceof Player) {
			Player player = (Player)event.getRemover();

			coord.setFromLocation(player.getLocation());
			TownChunk tc = CivGlobal.getTownChunk(coord);

			if (tc == null) {
				return;
			}

			Resident resident = CivGlobal.getResident(player.getName());
			if (resident == null) {
				event.setCancelled(true);
			}

			if (!tc.perms.hasPermission(PlotPermissions.Type.DESTROY, resident)) {
				event.setCancelled(true);
				CivMessage.sendErrorNoRepeat(player, "You do not have permission to destroy here.");
			}
		}


	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onChunkUnloadEvent(ChunkUnloadEvent event) {
		Boolean persist = CivGlobal.isPersistChunk(event.getChunk());		
		if (persist != null && persist == true) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onChunkLoadEvent(ChunkLoadEvent event) {
		ChunkCoord coord = new ChunkCoord(event.getChunk());
		FarmChunk fc = CivGlobal.getFarmChunk(coord);
		if (fc == null) {
			return;
		}

		for (org.bukkit.entity.Entity ent : event.getChunk().getEntities()) {
			if (ent.getType().equals(EntityType.ZOMBIE)) {
				ent.remove();
			}
		}

		class AsyncTask extends CivAsyncTask {

			FarmChunk fc;
			public AsyncTask(FarmChunk fc) {
				this.fc = fc;
			}

			@Override
			public void run() {
				if (fc.getMissedGrowthTicks() > 0) {
					fc.processMissedGrowths(false, this);
					fc.getFarm().saveMissedGrowths();
				}
			}

		}

		TaskMaster.syncTask(new AsyncTask(fc), 500);
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {

		Pasture pasture = Pasture.pastureEntities.get(event.getEntity().getUniqueId());
		if (pasture != null) {
			pasture.onEntityDeath(event.getEntity());
		}


		if (!ConfigTempleSacrifice.isValidEntity(event.getEntityType())) {
			return;
		}

		/* Check if we're 'inside' a temple. */
		bcoord.setFromLocation(event.getEntity().getLocation());
		HashSet<Buildable> buildables = CivGlobal.getBuildablesAt(bcoord);
		if (buildables == null) {
			return;
		}

		for (Buildable buildable : buildables) {
			if (buildable instanceof Temple) {
				if (buildable.getCorner().getY() <= event.getEntity().getLocation().getBlockY()) {
					/* We're 'above' the temple. Good enough. */
					((Temple)buildable).onEntitySacrifice(event.getEntityType());
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onBlockGrowEvent(BlockGrowEvent event) {
		bcoord.setFromLocation(event.getBlock().getLocation().add(0, -1, 0));
		if (CivGlobal.vanillaGrowthLocations.contains(bcoord)) {
			/* Allow vanilla growth on these plots. */
			return;
		}

		Block b = event.getBlock();

		if (Farm.isBlockControlled(b)) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.HIGH)
	public void onEntityBreakDoor(EntityBreakDoorEvent event) {
		bcoord.setFromLocation(event.getBlock().getLocation());
		StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
		if (sb != null) {
			event.setCancelled(true);
		}

		CampBlock cb = CivGlobal.getCampBlock(bcoord);
		if (cb != null) {
			event.setCancelled(true);
		}
	}

	@EventHandler(priority = EventPriority.NORMAL)
	public void onCreatureSpawnEvent(CreatureSpawnEvent event) {
		if (War.isWarTime()) {
			if (!event.getSpawnReason().equals(SpawnReason.BREEDING)){
				event.setCancelled(true);
				return;
			}
		}
		
		if (event.getEntity().getType().equals(EntityType.CHICKEN)) {
			if (event.getSpawnReason().equals(SpawnReason.EGG)) {
				event.setCancelled(true);
				return;
			}
			NBTTagCompound compound = new NBTTagCompound();
			if (compound.getBoolean("IsChickenJockey")) {
				event.setCancelled(true);
				return;			
			}
		}

		if (event.getEntity().getType().equals(EntityType.IRON_GOLEM) &&
			event.getSpawnReason().equals(SpawnReason.BUILD_IRONGOLEM)) {
				event.setCancelled(true);
				return;
		}

		if (MobLib.isMobLibEntity(event.getEntity())) {
			return;
		}

		if (event.getEntity().getType().equals(EntityType.ZOMBIE) ||
			event.getEntity().getType().equals(EntityType.SKELETON) ||
			event.getEntity().getType().equals(EntityType.BAT) ||
			event.getEntity().getType().equals(EntityType.CAVE_SPIDER) ||
			event.getEntity().getType().equals(EntityType.SPIDER) ||
			event.getEntity().getType().equals(EntityType.CREEPER) ||
			event.getEntity().getType().equals(EntityType.WOLF) ||
			event.getEntity().getType().equals(EntityType.SILVERFISH) ||
			event.getEntity().getType().equals(EntityType.OCELOT) ||
			event.getEntity().getType().equals(EntityType.WITCH) ||
			event.getEntity().getType().equals(EntityType.ENDERMAN)) {

			event.setCancelled(true);
			return;
		}

		if (event.getSpawnReason().equals(SpawnReason.SPAWNER)) {
			event.setCancelled(true);
			return;
		}
	}

	public boolean allowPistonAction(Location loc) {
		bcoord.setFromLocation(loc);
		StructureBlock sb = CivGlobal.getStructureBlock(bcoord);
		if (sb != null) {
			return false;
		}

		RoadBlock rb = CivGlobal.getRoadBlock(bcoord);
		if (rb != null) {
			return false;
		}

		CampBlock cb = CivGlobal.getCampBlock(bcoord);
		if (cb != null) {
			return false;
		}

		/* 
		 * If we're next to an attached protected item frame. Disallow 
		 * we cannot break protected item frames.
		 * 
		 * Only need to check blocks directly next to us.
		 */
		BlockCoord bcoord2 = new BlockCoord(bcoord);
		bcoord2.setX(bcoord.getX() - 1);
		if (ItemFrameStorage.attachedBlockMap.containsKey(bcoord2)) {
			return false;
		}

		bcoord2.setX(bcoord.getX() + 1);
		if (ItemFrameStorage.attachedBlockMap.containsKey(bcoord2)) {
			return false;
		}

		bcoord2.setZ(bcoord.getZ() - 1);
		if (ItemFrameStorage.attachedBlockMap.containsKey(bcoord2)) {
			return false;
		}

		bcoord2.setZ(bcoord.getZ() + 1);
		if (ItemFrameStorage.attachedBlockMap.containsKey(bcoord2)) {
			return false;
		}

		coord.setFromLocation(loc);
		HashSet<Wall> walls = CivGlobal.getWallChunk(coord);

		if (walls != null) {
			for (Wall wall : walls) {
				if (wall.isProtectedLocation(loc)) {
					return false;
				}
			}
		}		

		return true;
	}

	@EventHandler(priority = EventPriority.HIGHEST) 
	public void onBlockPistonExtendEvent(BlockPistonExtendEvent event) {

		/* UGH. If we extend into 'air' it doesnt count them as blocks...
		 * we need to check air to prevent breaking of item frames...
		 */
		final int PISTON_EXTEND_LENGTH = 13;
		Block currentBlock = event.getBlock().getRelative(event.getDirection());
		for (int i = 0; i < PISTON_EXTEND_LENGTH; i++) {
			if(ItemManager.getId(currentBlock) == CivData.AIR) {
				if (!allowPistonAction(currentBlock.getLocation())) {
					event.setCancelled(true);
					return;
				}
			}

			currentBlock = currentBlock.getRelative(event.getDirection());
		}
		
		if (War.isWarTime()) {
			event.setCancelled(true);
			return;
		}

//		if (event.getBlocks().size() == 0) {
//			Block extendInto = event.getBlock().getRelative(event.getDirection());
//			if (!allowPistonAction(extendInto.getLocation())) {
//				event.setCancelled(true);
//				return;
//			}
//		}
		coord.setFromLocation(event.getBlock().getLocation());
		FarmChunk fc = CivGlobal.getFarmChunk(coord);
		if (fc == null) {
			event.setCancelled(true);
			
		}
		
		for (Block block : event.getBlocks()) {
			if (!allowPistonAction(block.getLocation())) {
				event.setCancelled(true);
				break;

			}
		}

	}

	@EventHandler(priority = EventPriority.HIGHEST) 
	public void onBlockPistonRetractEvent(BlockPistonRetractEvent event) {
		if (!allowPistonAction(event.getRetractLocation())) {
			event.setCancelled(true);
			return;
		}
	}

	@EventHandler(priority = EventPriority.HIGHEST) 
	public void onPotionSplashEvent(PotionSplashEvent event) {
		ThrownPotion potion = event.getPotion();

		if (!(potion.getShooter() instanceof Player)) {
			return;
		} 

		Player attacker = (Player)potion.getShooter();

		for (PotionEffect effect : potion.getEffects()) {
			if (effect.getType().equals(PotionEffectType.INVISIBILITY)) {
				event.setCancelled(true);
				return;
			}
		}

		boolean protect = false;
		for (PotionEffect effect : potion.getEffects()) {
			if (effect.getType().equals(PotionEffectType.BLINDNESS) ||
				effect.getType().equals(PotionEffectType.CONFUSION) ||
				effect.getType().equals(PotionEffectType.HARM) ||
				effect.getType().equals(PotionEffectType.POISON) ||
				effect.getType().equals(PotionEffectType.SLOW) ||
				effect.getType().equals(PotionEffectType.SLOW_DIGGING) ||
				effect.getType().equals(PotionEffectType.WEAKNESS) ||
				effect.getType().equals(PotionEffectType.WITHER)) {

				protect = true;
				break;
			}
		}

		if (!protect) {
			return;
		}

		for (LivingEntity entity : event.getAffectedEntities()) {
			if (entity instanceof Player) {
				Player defender = (Player)entity;
				coord.setFromLocation(entity.getLocation());
				TownChunk tc = CivGlobal.getTownChunk(coord);
				if (tc == null) {
					continue;
				}

				switch(playersCanPVPHere(attacker, defender, tc)) {
				case ALLOWED:
					continue;
				case NOT_AT_WAR:
					CivMessage.send(attacker, CivColor.Rose+"You cannot use potions against "+defender.getName()+". You are not at war.");
					event.setCancelled(true);
					return;
				case NEUTRAL_IN_WARZONE:
					CivMessage.send(attacker, CivColor.Rose+"You cannot use potions against "+defender.getName()+". You a neutral in a war-zone.");
					event.setCancelled(true);
					return;
				case NON_PVP_ZONE:
					CivMessage.send(attacker, CivColor.Rose+"You cannot use potions against "+defender.getName()+". You are in a non-pvp zone.");
					event.setCancelled(true);
					return;
				}
			}
		}
	}

	@EventHandler(priority = EventPriority.NORMAL) 
	public void onBlockRedstoneEvent(BlockRedstoneEvent event) {
		
		bcoord.setFromLocation(event.getBlock().getLocation());

		CampBlock cb = CivGlobal.getCampBlock(bcoord);
		if (cb != null) {
			if (ItemManager.getId(event.getBlock()) == CivData.WOOD_DOOR ||
					ItemManager.getId(event.getBlock()) == CivData.IRON_DOOR) {
				event.setNewCurrent(0);
				return;
			}
		}
		
		if (War.isWarTime()) {
			event.setNewCurrent(0);
			return;
		}

	}

	private enum PVPDenyReason {
		ALLOWED,
		NON_PVP_ZONE,
		NOT_AT_WAR,
		NEUTRAL_IN_WARZONE
	}

	private PVPDenyReason playersCanPVPHere(Player attacker, Player defender, TownChunk tc) {
		Resident defenderResident = CivGlobal.getResident(defender);
		Resident attackerResident = CivGlobal.getResident(attacker);
		PVPDenyReason reason = PVPDenyReason.NON_PVP_ZONE;

		/* Outlaws can only pvp each other if they are declared at this location. */
		if (CivGlobal.isOutlawHere(defenderResident, tc) || 
			CivGlobal.isOutlawHere(attackerResident, tc)) {
			return PVPDenyReason.ALLOWED;
		}

		/* 
		 * If it is WarTime and the town we're in is at war, allow neutral players to be 
		 * targeted by anybody.
		 */
		if (War.isWarTime()) {
			if (tc.getTown().getCiv().getDiplomacyManager().isAtWar()) {
				/* 
				 * The defender is neutral if he is not in a town/civ, or not in his own civ AND not 'at war'
				 * with the attacker.
				 */
				if (!defenderResident.hasTown() || (!defenderResident.getTown().getCiv().getDiplomacyManager().atWarWith(tc.getTown().getCiv()) && 
						defenderResident.getTown().getCiv() != tc.getTown().getCiv())) {
					/* Allow neutral players to be hurt, but not hurt them back. */
					return PVPDenyReason.ALLOWED;
				} else if (!attackerResident.hasTown() || (!attackerResident.getTown().getCiv().getDiplomacyManager().atWarWith(tc.getTown().getCiv()) &&
						attackerResident.getTown().getCiv() != tc.getTown().getCiv())) {
					reason = PVPDenyReason.NEUTRAL_IN_WARZONE;
				}
			}
		}

		boolean defenderAtWarWithAttacker = false;
		if (defenderResident != null && defenderResident.hasTown()) {
			defenderAtWarWithAttacker = defenderResident.getTown().getCiv().getDiplomacyManager().atWarWith(attacker);
			/* 
			 * If defenders are at war with attackers allow PVP. Location doesnt matter. Allies should be able to help
			 * defend each other regardless of where they are currently located.
			 */
			if (defenderAtWarWithAttacker) {
				//if (defenderResident.getTown().getCiv() == tc.getTown().getCiv() ||
				//	attackerResident.getTown().getCiv() == tc.getTown().getCiv()) {
					return PVPDenyReason.ALLOWED;
				//}
			} else if (reason.equals(PVPDenyReason.NON_PVP_ZONE)) {
				reason = PVPDenyReason.NOT_AT_WAR;
			}
		}

		return reason;
	}

	@EventHandler(priority = EventPriority.LOW)
	public void onEntityPortalCreate(EntityCreatePortalEvent event) {
		event.setCancelled(true);
	}

}
