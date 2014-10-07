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
package com.avrgaming.civcraft.structure;

import java.io.IOException;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.ChunkSnapshot;
import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.interactive.InteractiveBuildCommand;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.BuildableDamageBlock;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.SQLObject;
import com.avrgaming.civcraft.object.StructureChest;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.road.RoadBlock;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.structurevalidation.StructureValidator;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.Template.TemplateType;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.threading.tasks.PostBuildSyncTask;
import com.avrgaming.civcraft.tutorial.CivTutorial;
import com.avrgaming.civcraft.util.AABB;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.BukkitObjects;
import com.avrgaming.civcraft.util.CallbackInterface;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import com.avrgaming.global.perks.Perk;
import com.wimbli.WorldBorder.BorderData;
import com.wimbli.WorldBorder.Config;

public abstract class Buildable extends SQLObject {
	
	private Town town;
	protected BlockCoord corner;
	public ConfigBuildableInfo info = new ConfigBuildableInfo(); //Blank buildable info for buildables which do not have configs.
	protected int hitpoints;

	public int builtBlockCount = 0;
	public int savedBlockCount = 0;
	private int totalBlockCount = 0;
	private boolean complete = false;
	protected boolean autoClaim = false;
	private boolean enabled = true;
	
	private String templateName;
	private int templateX;
	private int templateY;
	private int templateZ;
	
	// Number of blocks to shift the structure away from us when built.
	public static final double SHIFT_OUT = 0;
	public static final int MIN_DISTANCE = 7;
	
	private Map<BlockCoord, StructureSign> structureSigns = new ConcurrentHashMap<BlockCoord, StructureSign>();
	private Map<BlockCoord, StructureChest> structureChests = new ConcurrentHashMap<BlockCoord, StructureChest>();
	
	/* Used to keep track of which blocks belong to this buildable so they can be removed when the buildable is removed. */
	protected Map<BlockCoord, Boolean> structureBlocks = new ConcurrentHashMap<BlockCoord, Boolean>();
	private BlockCoord centerLocation;
	
	// XXX this is a bad hack to get the townchunks to load in the proper order when saving asynchronously
	public ArrayList<TownChunk> townChunksToSave = new ArrayList<TownChunk>();
	public ArrayList<Component> attachedComponents = new ArrayList<Component>();
	
	private boolean valid = true;
	public static double validPercentRequirement = 0.8;
	public static HashSet<Buildable> invalidBuildables = new HashSet<Buildable>();
	public HashMap<Integer, BuildableLayer> layerValidPercentages = new HashMap<Integer, BuildableLayer>();
	public boolean validated = false;
	
	private String invalidReason = "";
	
	public static final double DEFAULT_HAMMERRATE = 1.0;
	public AABB templateBoundingBox = null;
	public String invalidLayerMessage = "";
	
	/* True when the corner has been repositioned during the build process. */
	public Town getTown() {
		return town;
	}
	public void setTown(Town town) {
		this.town = town;
	}
	
	public Civilization getCiv() {
		if (this.getTown() == null) {
			return null;
		}
		return this.getTown().getCiv();
	}
	
	public String getHash() {
		return corner.toString();
	}
	
	
	public String getConfigId() {
		return info.id;
	}
	
	public String getTemplateBaseName() {
		return info.template_base_name;
	}
	
	public String getDisplayName() {
		return info.displayName;
	}

	
	public int getMaxHitPoints() {
		return info.max_hitpoints;
	}

	
	public double getCost() {
		return info.cost;
	}
	
	public int getRegenRate() {
		if (this.info.regenRate == null) {
			return 0;
		}
		
		return info.regenRate;
	}

	
	public double getHammerCost() {
		double rate = 1;
		if (this.getTown().getBuffManager().hasBuff(Buff.RUSH)) {
			rate -= this.getTown().getBuffManager().getEffectiveDouble(Buff.RUSH);
		}
		return rate*info.hammer_cost;
	}
	
	
	public double getUpkeepCost() {
		return info.upkeep;
	}
	
	
	public int getTemplateYShift() {
		return info.templateYShift;
	}
	
	
	public String getRequiredUpgrade() {
		return info.require_upgrade;
	}

	
	public String getRequiredTechnology() {
		return info.require_tech;
	}
	
	
	public String getUpdateEvent() {
		return info.update_event;
	}
	
	public int getPoints() {
		if (info.points != null) {
			return info.points;
		}
		return 0;
	}
	
	public String getEffectEvent() {
		return info.effect_event;
	}
	
	public void onEffectEvent() {
	}
	
	public String getOnBuildEvent() {
		return info.onBuild_event;
	}
	
	public boolean allowDemolish() {
		return info.allow_demolish;
	}
	
	public boolean isTileImprovement() {
		return info.tile_improvement;
	}
	
	public boolean isActive() {	
		return this.isComplete() && !isDestroyed() && isEnabled();
	}

	public abstract void processUndo() throws CivException;
	
	public int getBuiltBlockCount() {
		return builtBlockCount;
	}
	public void setBuiltBlockCount(int builtBlockCount) {
		this.builtBlockCount = builtBlockCount;
		this.savedBlockCount = builtBlockCount;
	}
	public int getTotalBlockCount() {
		return totalBlockCount;
	}
	public void setTotalBlockCount(int totalBlockCount) {
		this.totalBlockCount = totalBlockCount;
	}
	
	public boolean isDestroyed() {
		if ((hitpoints == 0) && (this.getMaxHitPoints() != 0)) {
			return true;
		}
		return false;
	}
	
	public boolean isDestroyable() {
		return (info.destroyable != null) && (info.destroyable == true);
	}
	
	public boolean isComplete() {
		return complete;
	}
	public void setComplete(boolean complete) {
		this.complete = complete;
	}
	
	public abstract void updateBuildProgess();

	public BlockCoord getCorner() {
		return corner;
	}
	
	public void setCorner(BlockCoord center) {
		this.corner = center;
	}
	
	public BlockCoord getCenterLocation() {
		if (this.centerLocation == null) {		
			int centerX = getCorner().getX() + (getTemplateX() / 2);
			int centerY = getCorner().getY() + (getTemplateY() / 2);
			int centerZ = getCorner().getZ() + (getTemplateZ() / 2);
			
			this.centerLocation = new BlockCoord(this.getCorner().getWorldname(), centerX, centerY, centerZ);
		}
		
		return this.centerLocation;
	}
	
	public double getBlocksPerHammer() {
		// no hammer cost should be instant...
		if (this.getHammerCost() == 0)
			return this.totalBlockCount;
		
		return this.totalBlockCount / this.getHammerCost();
	}
	
	public int getHitpoints() {
		return hitpoints;
	}

	public void setHitpoints(int hitpoints) {
		this.hitpoints = hitpoints;
	}
	
	public abstract void build(Player player, Location centerLoc, Template tpl) throws Exception;
	protected abstract void runOnBuild(Location centerLoc, Template tpl) throws CivException;
	
	public void bindStructureBlocks() {
		// Called mostly on a reload, determines which blocks should be protected based on the corner
		// location and the template's size. We need to verify that each block is a part of the template.
		// We might be able to restore broken/missing structures from here in the future.
		if (isDestroyable())
			return;
		
		Template tpl;
		try {
			tpl = Template.getTemplate(this.templateName, null);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		} catch (CivException e) {
			e.printStackTrace();
			return;
		}
		
		this.setTemplateX(tpl.size_x);
		this.setTemplateY(tpl.size_y);
		this.setTemplateZ(tpl.size_z);
		this.setTemplateAABB(getCorner(), tpl);
		
		for (int y = 0; y < this.getTemplateY(); y++) {
			for (int z = 0; z < this.getTemplateZ(); z++) {
				for (int x = 0; x < this.getTemplateX(); x++) {
					int relx = getCorner().getX() + x;
					int rely = getCorner().getY() + y;
					int relz = getCorner().getZ() + z;
					
					BlockCoord coord = new BlockCoord(this.getCorner().getWorldname(), (relx), (rely), (relz));
					
					if (tpl.blocks[x][y][z].getType() == CivData.AIR) {
						continue;
					}
					
					if (tpl.blocks[x][y][z].specialType == SimpleBlock.Type.COMMAND) {
						continue;
					}
						
					if (y == 0) {
						this.addStructureBlock(coord, false);	
					} else {
						this.addStructureBlock(coord, true);
					}
				}
			}
		}
	}
	
	public void buildPlayerPreview(Player player, Location centerLoc) throws CivException, IOException {
		
		/* Look for any custom template perks and ask the player if they want to use them. */
		Resident resident = CivGlobal.getResident(player);
		ArrayList<Perk> perkList = this.getTown().getTemplatePerks(this, resident, this.info);		
		ArrayList<Perk> personalUnboundPerks = resident.getUnboundTemplatePerks(perkList, this.info);
		if (perkList.size() != 0 || personalUnboundPerks.size() != 0) {
			/* Store the pending buildable. */
			resident.pendingBuildable = this;
			
			/* Build an inventory full of templates to select. */
			Inventory inv = Bukkit.getServer().createInventory(player, CivTutorial.MAX_CHEST_SIZE*9);
			ItemStack infoRec = LoreGuiItem.build("Default "+this.getDisplayName(), 
					ItemManager.getId(Material.WRITTEN_BOOK), 
					0, CivColor.Gold+"<Click To Build>");
			infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
			inv.addItem(infoRec);
			
			for (Perk perk : perkList) {
				infoRec = LoreGuiItem.build(perk.getDisplayName(), 
						perk.configPerk.type_id, 
						perk.configPerk.data, CivColor.Gold+"<Click To Build>",
						CivColor.Gray+"Provided by: "+CivColor.LightBlue+perk.provider);
				infoRec = LoreGuiItem.setAction(infoRec, "BuildWithTemplate");
				infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getIdent());
				inv.addItem(infoRec);
			}
			
			for (Perk perk : personalUnboundPerks) {
				infoRec = LoreGuiItem.build(perk.getDisplayName(), 
						CivData.BEDROCK, 
						perk.configPerk.data, CivColor.Gold+"<Click To Bind>",
						CivColor.Gray+"Unbound Temple",
						CivColor.Gray+"You own this template.",
						CivColor.Gray+"The town is missing it.",
						CivColor.Gray+"Click to bind to town first.",
						CivColor.Gray+"Then build again.");				
				infoRec = LoreGuiItem.setAction(infoRec, "ActivatePerk");
				infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getIdent());
				
			}
			
			/* We will resume by calling buildPlayerPreview with the template when a gui item is clicked. */
			player.openInventory(inv);
			return;
		}
		

		
		Template tpl;
		
		tpl = new Template();
		try {
			tpl.initTemplate(centerLoc, this);
		} catch (CivException e) {
			e.printStackTrace();
			throw e;
		} catch (IOException e) {
			e.printStackTrace();
			throw e;
		}
		
		buildPlayerPreview(player, centerLoc, tpl);
	}
	
	
	public void buildPlayerPreview(Player player, Location centerLoc, Template tpl) throws CivException, IOException {
		centerLoc = repositionCenter(centerLoc, tpl.dir(), tpl.size_x, tpl.size_z);
		tpl.buildPreviewScaffolding(centerLoc, player);
		
		this.setCorner(new BlockCoord(centerLoc));
		
		CivMessage.sendHeading(player, "Building a Structure");
		CivMessage.send(player, CivColor.Yellow+ChatColor.BOLD+"We've placed a bedrock outline, only visible to you which outlines "+
				" the structure's location.");
		CivMessage.send(player, CivColor.LightGreen+ChatColor.BOLD+"If this location looks good, type 'yes'. Otherwise, type anything else to cancel building.");
		Resident resident = CivGlobal.getResident(player);
		resident.startPreviewTask(tpl, centerLoc.getBlock(), player.getUniqueId());
		
		/* Run validation on position. */
		//validate(player, this, tpl, centerLoc, null);
		this.templateName = tpl.getFilepath();
		TaskMaster.asyncTask(new StructureValidator(player, this), 0);
		resident.setInteractiveMode(new InteractiveBuildCommand(this.getTown(), this, player.getLocation(), tpl));
	}

	/*
	 * This function is called before we build structures that do not have a town yet.
	 * This includes Capitols, Camps, and Town Halls.
	 */
	
	public static void buildVerifyStatic(Player player, ConfigBuildableInfo info, Location centerLoc, CallbackInterface callback) throws CivException {
	
		Resident resident = CivGlobal.getResident(player);
		/* Look for any custom template perks and ask the player if they want to use them. */
		LinkedList<Perk> perkList = resident.getPersonalTemplatePerks(info);
		if (perkList.size() != 0) {
			
			/* Store the pending buildable. */
			resident.pendingBuildableInfo = info;
			resident.pendingCallback = callback;

			/* Build an inventory full of templates to select. */
			Inventory inv = Bukkit.getServer().createInventory(player, CivTutorial.MAX_CHEST_SIZE*9);
			ItemStack infoRec = LoreGuiItem.build("Default "+info.displayName, 
					ItemManager.getId(Material.WRITTEN_BOOK), 
					0, CivColor.Gold+"<Click To Build>");
			infoRec = LoreGuiItem.setAction(infoRec, "BuildWithDefaultPersonalTemplate");
			inv.addItem(infoRec);
			
			for (Perk perk : perkList) {
				infoRec = LoreGuiItem.build(perk.getDisplayName(), 
						perk.configPerk.type_id, 
						perk.configPerk.data, CivColor.Gold+"<Click To Build>",
						CivColor.Gray+"Provided by: "+CivColor.LightBlue+"Yourself :)");
				infoRec = LoreGuiItem.setAction(infoRec, "BuildWithPersonalTemplate");
				infoRec = LoreGuiItem.setActionData(infoRec, "perk", perk.getIdent());
				inv.addItem(infoRec);
				player.openInventory(inv);
			}
			/* We will resume by calling buildPlayerPreview with the template when a gui item is clicked. */
			return;
		}
		
		String path = Template.getTemplateFilePath(info.template_base_name,
				Template.getDirection(player.getLocation()), TemplateType.STRUCTURE, "default");
		
		Template tpl;
		try {
			tpl = Template.getTemplate(path, player.getLocation());
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		centerLoc = repositionCenterStatic(centerLoc, info, tpl.dir(), tpl.size_x, tpl.size_z);	
		//validate(player, null, tpl, centerLoc, callback);
		TaskMaster.asyncTask(new StructureValidator(player, tpl.getFilepath(), centerLoc, callback), 0);
	}
	
	public void undoFromTemplate() throws IOException, CivException {
		Template undo_tpl = new Template();
		undo_tpl.initUndoTemplate(this.getCorner().toString(), this.getTown().getName());
		undo_tpl.buildUndoTemplate(undo_tpl, this.getCorner().getBlock());
		
		for (BuildAsyncTask task : this.getTown().build_tasks) {
			if (task.buildable == this) {
				task.abort();
			}
		}
		undo_tpl.deleteUndoTemplate(this.getCorner().toString(), this.getTown().getName());
	}
	
	public void unbindStructureBlocks() {
		for (BlockCoord coord : this.structureBlocks.keySet()) {
			CivGlobal.removeStructureBlock(coord);
		}
	}
	
	/*
	 * XXX this is called only on structures which do not have towns yet.
	 * For Example Capitols, Camps and Town Halls.
	 */
	public static Location repositionCenterStatic(Location center, ConfigBuildableInfo info, String dir, double x_size, double z_size) throws CivException {
		Location loc = new Location(center.getWorld(), 
				center.getX(), center.getY(), center.getZ(), 
				center.getYaw(), center.getPitch());
		
		
		// Reposition tile improvements
		if (info.tile_improvement) {
			// just put the center at 0,0 of this chunk?
			loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
		} else { 
			if (dir.equalsIgnoreCase("east")) {				
				loc.setZ(loc.getZ() - (z_size / 2));
				loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
				loc.setX(loc.getX() + SHIFT_OUT);				
			}
			else if (dir.equalsIgnoreCase("west")) {
				loc.setZ(loc.getZ() - (z_size / 2));
				loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
				loc.setX(loc.getX() - (SHIFT_OUT+x_size));
			}
			else if (dir.equalsIgnoreCase("north")) {
				loc.setX(loc.getX() - (x_size / 2));
				loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
				loc.setZ(loc.getZ() - (SHIFT_OUT+z_size));
			}
			else if (dir.equalsIgnoreCase("south")) {
				loc.setX(loc.getX() - (x_size / 2));
				loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
				loc.setZ(loc.getZ() + SHIFT_OUT);
			}
		}   
		if (info.templateYShift != 0) {
			// Y-Shift based on the config, this allows templates to be built underground.
			loc.setY(loc.getY() + info.templateYShift);
			
			if (loc.getY() < 1) {
				throw new CivException("Cannot build here, too close to bedrock.");
			}
		}
				
		return loc;
	}
	
	protected Location repositionCenter(Location center, String dir, double x_size, double z_size) throws CivException {
		Location loc = new Location(center.getWorld(), 
				center.getX(), center.getY(), center.getZ(), 
				center.getYaw(), center.getPitch());
		
		
		// Reposition tile improvements
		if (this.isTileImprovement()) {
			// just put the center at 0,0 of this chunk?
			loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
		} else {  
			if (dir.equalsIgnoreCase("east")) {
				loc.setZ(loc.getZ() - (z_size / 2));
				loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
				loc.setX(loc.getX() + SHIFT_OUT);
			}
			else if (dir.equalsIgnoreCase("west")) {
				loc.setZ(loc.getZ() - (z_size / 2));
				loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
				loc.setX(loc.getX() - (SHIFT_OUT+x_size));
			}
			else if (dir.equalsIgnoreCase("north")) {
				loc.setX(loc.getX() - (x_size / 2));
				loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
				loc.setZ(loc.getZ() - (SHIFT_OUT+z_size));
			}
			else if (dir.equalsIgnoreCase("south")) {
				loc.setX(loc.getX() - (x_size / 2));
				loc = center.getChunk().getBlock(0, center.getBlockY(), 0).getLocation();
				loc.setZ(loc.getZ() + SHIFT_OUT);
			}
		}  
		if (this.getTemplateYShift() != 0) {
			// Y-Shift based on the config, this allows templates to be built underground.
			loc.setY(loc.getY() + this.getTemplateYShift());
			
			if (loc.getY() < 1) {
				throw new CivException("Cannot build here, too close to bedrock.");
			}
		}
				
		return loc;
	}
	
	public void resumeBuildFromTemplate() throws Exception
	{
		Template tpl;
		
		Location corner = getCorner().getLocation();

		try {
			tpl = new Template();
			tpl.resumeTemplate(this.getSavedTemplatePath(), this);
		} catch (Exception e) {
			throw e;
		}
		
		this.totalBlockCount = tpl.size_x*tpl.size_y*tpl.size_z;
		
		if (this instanceof Wonder) {
			this.getTown().setCurrentWonderInProgress(this);
		} else {
			this.getTown().setCurrentStructureInProgress(this);
		}
		
		this.startBuildTask(tpl, corner);
	}
	
	
	protected void setTemplateAABB(BlockCoord corner, Template tpl) {
		
		this.setCorner(corner);
		this.templateBoundingBox = new AABB();
		this.templateBoundingBox.setPosition(this.getCenterLocation());
		this.templateBoundingBox.setExtents(new BlockCoord(getCorner().getWorldname(),
											this.getTemplateX()/2, 
											this.getTemplateY()/2, 
											this.getTemplateZ()/2));
	}
	
	public static void validateDistanceFromSpawn(Location loc) throws CivException {
		/* Check distance from spawn. */
		double requiredDistance;
		try {
			requiredDistance = CivSettings.getDouble(CivSettings.civConfig, "global.distance_from_spawn");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		
		for (Civilization civ : CivGlobal.getAdminCivs()) {
			Location townHallLoc = civ.getCapitolTownHallLocation();
			if (townHallLoc == null) {
				continue;
			}

			double distance = townHallLoc.distance(loc);
			if (distance < requiredDistance) {
				throw new CivException("You must build at least "+requiredDistance+" blocks away from spawn.");
			}
			
		}
	}
	
	protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ, Location origin) throws CivException {
		
		boolean foundTradeGood = false;
		TradeOutpost tradeOutpost = null;
		boolean ignoreBorders = false;
		boolean autoClaim = this.autoClaim;
		
		if (this instanceof TradeOutpost) {
			tradeOutpost = (TradeOutpost)this;
		}
		
		//Make sure we are building this building inside of culture.
		if (!isTownHall()) {
			CultureChunk cc = CivGlobal.getCultureChunk(centerBlock.getLocation());
			if (cc == null || cc.getTown().getCiv() != this.town.getCiv()) {
				throw new CivException("Cannot build here, you need to build inside your culture.");
			}
		} else {
			/* Structure is a town hall, auto-claim the borders. */
			ignoreBorders = true;
		}
		
		if (isTownHall()) {
			double minDistance;
			try {
				minDistance = CivSettings.getDouble(CivSettings.townConfig, "town.min_town_distance");
			} catch (InvalidConfiguration e) {
				CivMessage.sendError(player, "Internal configuration error.");
				e.printStackTrace();
				return;
			}
			
			for (Town town : CivGlobal.getTowns()) {
				TownHall townhall = town.getTownHall();
				if (townhall == null) {
					continue;
				}
				
				double dist = townhall.getCenterLocation().distance(new BlockCoord(centerBlock));
				if (dist < minDistance) {
					DecimalFormat df = new DecimalFormat();
					CivMessage.sendError(player, "Cannot build town here. Too close to the town of "+town.getName()+". Distance is "+df.format(dist)+" and needs to be "+minDistance);
					return;
				}
			}
		}
		
		if (this.getConfigId().equals("s_shipyard")) {
			if (!centerBlock.getBiome().equals(Biome.OCEAN) && 
				!centerBlock.getBiome().equals(Biome.BEACH) &&
				!centerBlock.getBiome().equals(Biome.DEEP_OCEAN) &&
				!centerBlock.getBiome().equals(Biome.RIVER) &&
				!centerBlock.getBiome().equals(Biome.FROZEN_OCEAN) &&
				!centerBlock.getBiome().equals(Biome.FROZEN_RIVER)) {
				throw new CivException("Cannot build shipyard here, you need to be in a majority of ocean, river, or beach biome. Try repositioning it if you are.");
			}
		}
		
		Structure struct = CivGlobal.getStructure(new BlockCoord(centerBlock));
		if (struct != null) {
			throw new CivException("Cannot build here, structure already at this location.");
		}

		ignoreBorders = this.isAllowOutsideTown();
		
		if (!player.isOp()) {
			validateDistanceFromSpawn(centerBlock.getLocation());
		}
		
		if (this.isTileImprovement()) {
			ignoreBorders = true;
			ConfigTownLevel level = CivSettings.townLevels.get(getTown().getLevel());
			
			if (getTown().getTileImprovementCount() >= level.tile_improvements) {
				throw new CivException("Cannot build tile improvement. Already at tile improvement limit.");
			}
			
			ChunkCoord coord = new ChunkCoord(centerBlock.getLocation());
			for (Structure s : getTown().getStructures()) {
				if (!s.isTileImprovement()) {
					continue;
				}
				ChunkCoord sCoord = new ChunkCoord(s.getCorner());
				if (sCoord.equals(coord)) {
					throw new CivException("Cannot build a tile improvement on the same chunk as another tile improvement.");
				}
			}
			
		}
		
		TownChunk centertc = CivGlobal.getTownChunk(origin);
		if (centertc == null && ignoreBorders == false) {
			throw new CivException("You must build this structure while inside town borders.");
		}
		
		if (centerBlock.getLocation().getY() >= 255) {
			throw new CivException("You're too high to build structures.");
		}
		
		if (centerBlock.getLocation().getY() <= 7) {
			throw new CivException("You can not place structures this close to bedrock!");
		
		}
		
		if ((regionY + centerBlock.getLocation().getBlockY()) >= 255) {
			throw new CivException("Cannot build structure here, would go over the minecraft height limit.");
		}
		
		/* Check that we're not overlapping with another structure's template outline. */
		/* XXX this needs to check actual blocks, not outlines cause thats more annoying than actual problems caused by building into each other. */
//		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
//		while(iter.hasNext()) {
//			Entry<BlockCoord, Structure> entry = iter.next();
//			Structure s = entry.getValue();
//			
//			if (s.templateBoundingBox != null) {
//				if (s.templateBoundingBox.overlaps(this.templateBoundingBox)) {
//					throw new CivException("Cannot build structure here as it would overlap with a "+s.getDisplayName());
//				}
//			}
//		}
		
		onCheck();
		
		LinkedList<RoadBlock> deletedRoadBlocks = new LinkedList<RoadBlock>();
		ArrayList<ChunkCoord> claimCoords = new ArrayList<ChunkCoord>();
		for (int x = 0; x < regionX; x++) {
			for (int y = 0; y < regionY; y++) {
				for (int z = 0; z < regionZ; z++) {
					Block b = centerBlock.getRelative(x, y, z);
					
					if (ItemManager.getId(b) == CivData.CHEST) {
						throw new CivException("Cannot build here, would destroy chest.");
					}
										
					TownChunk tc = CivGlobal.getTownChunk(b.getLocation());
					if (tc == null && autoClaim == true) {
						claimCoords.add(new ChunkCoord(b.getLocation()));
					}
					
					if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
						// Make sure we have permission to destroy any block in this area.
						throw new CivException("Cannot build here, you need DESTROY permissions to the block at "+b.getX()+","+b.getY()+","+b.getZ());
					}
		
					BlockCoord coord = new BlockCoord(b);
					ChunkCoord chunkCoord = new ChunkCoord(coord.getLocation());

					if (tradeOutpost == null) {
						//not building a trade outpost, prevent protected blocks from being destroyed.
						if (CivGlobal.getProtectedBlock(coord) != null) {
							throw new CivException("Cannot build here, protected blocks in the way.");
						}
					} else {
						if (CivGlobal.getTradeGood(coord) != null) {
							// Make sure we encompass entire trade good.
							if ((y+3) < regionY) {
								foundTradeGood = true;
								tradeOutpost.setTradeGoodCoord(coord);
							}
						}
					}
					
					if (CivGlobal.getStructureBlock(coord) != null) {
						throw new CivException("Cannot build here, structure blocks in the way.");
					}
				
					if (CivGlobal.getFarmChunk(new ChunkCoord(coord.getLocation())) != null) {
						throw new CivException("Cannot build here, in the same chunk as a farm improvement.");
					}
		
					if (CivGlobal.getWallChunk(chunkCoord) != null) {
						throw new CivException("Cannot build here, in the same chunk as a wall improvement.");
					}
					
					if (CivGlobal.getCampBlock(coord) != null) {
						throw new CivException("Cannot build here, structure blocks in the way.");
					}
					
					if (CivGlobal.getBuildablesAt(coord) != null) {
						throw new CivException("Cannot build here, there is already a structure here.");
					}
					
					RoadBlock rb = CivGlobal.getRoadBlock(coord);
					if (rb != null) {
						deletedRoadBlocks.add(rb);
					}
				
					BorderData border = Config.Border(b.getWorld().getName());
					if (border != null) {
						if(!border.insideBorder(b.getLocation().getX(), b.getLocation().getZ(), Config.ShapeRound())) {
							throw new CivException("Cannot build here. Part of the structure would sit beyond the world border.");
						}
					}
				}
			}
		}
		
		if (tradeOutpost != null) {
			if (!foundTradeGood) {
				throw new CivException("Must be built on top of a trade good.");
			}
		}
		
		for (ChunkCoord c : claimCoords) {
			try {
				//XXX These will be added to the array list of objects to save in town.buildStructure();
				this.townChunksToSave.add(TownChunk.townHallClaim(this.getTown(), c));
			} catch (Exception e) {
			}
		}
		
		/* Delete any road blocks we happen to come across. */
		for (RoadBlock rb : deletedRoadBlocks) {
			rb.getRoad().deleteRoadBlock(rb);
		}
		
	}
	
	public void onCheck() throws CivException {
		/* Override in children */
	}
	
	
	public synchronized void buildRepairTemplate(Template tpl, Block centerBlock) {
		HashMap<Chunk, Chunk> chunkUpdates = new HashMap<Chunk, Chunk>();
		
		for (int x = 0; x < tpl.size_x; x++) {
			for (int y = 0; y < tpl.size_y; y++) {
				for (int z = 0; z < tpl.size_z; z++) {
					Block b = centerBlock.getRelative(x, y, z);
						//b.setTypeIdAndData(tpl.blocks[x][y][z].getType(), (byte)tpl.blocks[x][y][z].getData(), false);
						if (tpl.blocks[x][y][z].specialType == Type.COMMAND) {
							ItemManager.setTypeIdAndData(b, CivData.AIR, (byte)0, false);
						} else {
							ItemManager.setTypeIdAndData(b, tpl.blocks[x][y][z].getType(), (byte)tpl.blocks[x][y][z].getData(), false);
						}
						
						chunkUpdates.put(b.getChunk(), b.getChunk());
						
						if (ItemManager.getId(b) == CivData.WALL_SIGN || ItemManager.getId(b) == CivData.SIGN) {
							Sign s2 = (Sign)b.getState();
							s2.setLine(0, tpl.blocks[x][y][z].message[0]);
							s2.setLine(1, tpl.blocks[x][y][z].message[1]);
							s2.setLine(2, tpl.blocks[x][y][z].message[2]);
							s2.setLine(3, tpl.blocks[x][y][z].message[3]);
							s2.update();
						}
				}
			}
		}
	}
	
	protected void startBuildTask(Template tpl, Location center) {
		//CivBuildTask task = new CivBuildTask(TownyUniverse.getPlugin(), this, tpl, 
			//	this.getBuildSpeed(), this.getBlocksPerTick(), center.getBlock());
		if (this instanceof Structure) {
			this.getTown().setCurrentStructureInProgress(this);
		} else {
			this.getTown().setCurrentWonderInProgress(this);
		}
		BuildAsyncTask task = new BuildAsyncTask(this, tpl, this.getBuildSpeed(), this.getBlocksPerTick(), center.getBlock());
		
		this.town.build_tasks.add(task);
		BukkitObjects.scheduleAsyncDelayedTask(task, 0);
	}
	
	public int getBuildSpeed() {
		// buildTime is in hours, we need to return milliseconds.
		// We should return the number of milliseconds to wait between each block placement.
		double hoursPerBlock = ( this.getHammerCost() / this.town.getHammers().total ) / this.totalBlockCount;
		double millisecondsPerBlock = hoursPerBlock * 60 * 60 * 1000;
		// Clip millisecondsPerBlock to 500 milliseconds.
		if (millisecondsPerBlock < 500) {
			millisecondsPerBlock = 500;
		}
		
		return (int)millisecondsPerBlock;
	}
	
	public double getBuiltHammers() {
		double hoursPerBlock = ( this.getHammerCost() / DEFAULT_HAMMERRATE ) / this.totalBlockCount;
		return this.builtBlockCount * hoursPerBlock;
	}
	
	
	public int getBlocksPerTick() {
		// We do not want the blocks to be placed faster than 500 milliseconds.
		// So in order to deal with speeds that are faster than that, we will
		// increase the number of blocks given per tick. 
		double hoursPerBlock = ( this.getHammerCost() / this.town.getHammers().total ) / this.totalBlockCount;
		double millisecondsPerBlock = hoursPerBlock * 60 * 60 * 1000;
		
		// Dont let this get lower than 1 just in case to prevent any crazyiness...
		//if (millisecondsPerBlock < 1)
			//millisecondsPerBlock = 1;
		
		double blocks = (500 / millisecondsPerBlock);
		
		if (blocks < 1) {
			blocks = 1;
		}

		return (int)blocks;		
	}
	
	/* Checks to see if the area is covered by another structure */
	public void canBuildHere(Location center, double distance) throws CivException {
		
		// Do not let tile improvements be built on top of each other.
		//String chunkHash = Civ.chunkHash(center.getChunk());
		
		//TODO Revisit for walls and farms?
//		if (Civ.getWallChunk(chunkHash) != null) {
//			throw new CivException("Cannot build here, another tile improvement is in this chunk.");
//			
//		}
//		
//		if (Civ.getFarmChunk(chunkHash) != null) {
//			throw new CivException("Cannot build here, another tile improvement is in this chunk.");
//		}
		

				
		return;
	}
	
	public int getTemplateX() {
		return templateX;
	}
	public void setTemplateX(int templateX) {
		this.templateX = templateX;
	}
	public int getTemplateY() {
		return templateY;
	}
	public void setTemplateY(int templateY) {
		this.templateY = templateY;
	}
	public int getTemplateZ() {
		return templateZ;
	}
	public void setTemplateZ(int templateZ) {
		this.templateZ = templateZ;
	}
	
	public void addStructureSign(StructureSign s) {
		this.structureSigns.put(s.getCoord(), s);
	}
	
	public Collection<StructureSign> getSigns() {
		return this.structureSigns.values();
	}
	
	public StructureSign getSign(BlockCoord coord) {
		return this.structureSigns.get(coord);
	}
	
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) throws CivException {
		CivLog.info("No Sign action for this buildable?:"+this.getDisplayName());
	}
	
	public String getSavedTemplatePath() {
		return templateName;
	}
	public void setTemplateName(String templateName) {
		this.templateName = templateName;
	}
	
	public void addStructureChest(StructureChest chest) {
		this.structureChests.put(chest.getCoord(), chest);
	}
	
	public ArrayList<StructureChest> getAllChestsById(int id) {
		ArrayList<StructureChest> chests = new ArrayList<StructureChest>();
		
		for (StructureChest chest : this.structureChests.values()) {
			if (chest.getChestId() == id) {
				chests.add(chest);
			}
		}
		
		return chests;
	}
	
	public Collection<StructureChest> getChests() {
		return this.structureChests.values();
	}
	
	public void addStructureBlock(BlockCoord coord, boolean damageable) {
		//CivLog.debug("Added structure block:"+this);
		CivGlobal.addStructureBlock(coord, this, damageable);
		
		// all we really need is it's key, we'll put in true
		// to make sure this structureBlocks collection isnt
		// abused.
		this.structureBlocks.put(coord, true);
		
	}
	
	public abstract String getDynmapDescription();
	public abstract String getMarkerIconName();
	
	
	/* SessionDB helpers */
	public void sessionAdd(String key, String value) {
		CivGlobal.getSessionDB().add(key, value, this.getCiv().getId(), this.getTown().getId(), this.getId());
	}
	
	/*
	 * Damages this structure by this amount. If hitpoints go to 0, structure is demolished.
	 */
	public int getHitPoints() {
		return hitpoints;
	}

	public int getDamagePercentage() {
		double percentage = (double)hitpoints / (double)this.getMaxHitPoints();
		percentage *= 100;
		return (int)percentage;
	}

	public void damage(int amount) {
		if (hitpoints == 0)
			return;
		hitpoints -= amount;
		
		if (hitpoints <= 0) {
			hitpoints = 0;
			onDestroy();
		}
	}
	
	public void onDestroy() {
		//can be overriden in subclasses.
		CivMessage.global("A "+this.getDisplayName()+" in "+this.getTown().getName()+" has been destroyed!");
		this.hitpoints = 0;
		this.fancyDestroyStructureBlocks();
		this.save();
	}
	
	public void onDamage(int amount, World world, Player player, BlockCoord coord, BuildableDamageBlock hit) {
		boolean wasTenPercent = false;
		
		if(hit.getOwner().isDestroyed()) {
			if (player != null) {
				CivMessage.sendError(player, hit.getOwner().getDisplayName()+" is already destroyed.");
			}
			return;
		}
		
		if (!hit.getOwner().isComplete() && !(hit.getOwner() instanceof Wonder)) {
			if (player != null) {
				CivMessage.sendError(player, hit.getOwner().getDisplayName()+" is still being built, cannot be destroyed.");
			}
			return;		
		}
		
		if ((hit.getOwner().getDamagePercentage() % 10) == 0) {
			wasTenPercent = true;
		}
			
		this.damage(amount);
		
		world.playSound(hit.getCoord().getLocation(), Sound.ANVIL_USE, 0.2f, 1);
		world.playEffect(hit.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
		
		if ((hit.getOwner().getDamagePercentage() % 10) == 0 && !wasTenPercent) {
			if (player != null) {
				onDamageNotification(player, hit);
			}
		}
		
		if (player != null) {
		Resident resident = CivGlobal.getResident(player);
			if (resident.isCombatInfo()) {
				CivMessage.send(player, CivColor.LightGray+hit.getOwner().getDisplayName()+" has been damaged ("+
						hit.getOwner().hitpoints+"/"+hit.getOwner().getMaxHitPoints()+")");
			}
		}
		
	}
	
	public void onDamageNotification(Player player, BuildableDamageBlock hit) {
		CivMessage.send(player, CivColor.LightGray+hit.getOwner().getDisplayName()+" has been damaged "+
				hit.getOwner().getDamagePercentage()+"%!");
		CivMessage.sendTown(hit.getTown(), CivColor.Yellow+"Our "+hit.getOwner().getDisplayName()+" at ("+hit.getOwner().getCorner()+
				") is under attack! Damage is "+hit.getOwner().getDamagePercentage()+"%!");	
	}
	
	public Map<BlockCoord, Boolean> getStructureBlocks() {
		return this.structureBlocks;
	}
	
	
	public boolean isAvailable() {
		return info.isAvailable(this.getTown());
	}
	
	public int getLimit() {
		return info.limit;
	}
	
	public boolean isAllowOutsideTown() {
		return (info.allow_outside_town != null) && (info.allow_outside_town == true);
	}
	
	public boolean isStrategic() {
		return info.strategic;
	}
	
	public void runCheck(Location center) throws CivException {
	}
	
	//public void fancyDestroyStructureBlocks() {
		
	//	for (BlockCoord coord : this.structureBlocks.keySet()) {
	//		coord.getLocation().getWorld().spawnFallingBlock(coord.getLocation(), coord.getBlock().getTypeId(), coord.getBlock().getData());
	//		coord.getBlock().setTypeId(CivData.AIR);
	//	}
		
	//}
	
	public void fancyDestroyStructureBlocks() {
		
		class SyncTask implements Runnable {

			@Override
			public void run() {
				for (BlockCoord coord : structureBlocks.keySet()) {
					
					if (CivGlobal.getStructureChest(coord) != null) {
						continue;
					}
					
					if (CivGlobal.getStructureSign(coord) != null) {
						continue;
					}
					
					if (ItemManager.getId(coord.getBlock()) == CivData.AIR) {
						continue;
					}
					
					if (ItemManager.getId(coord.getBlock()) == CivData.CHEST) {
						continue;
					}
					
					if (ItemManager.getId(coord.getBlock()) == CivData.SIGN) {
						continue;
					}
					
					if (ItemManager.getId(coord.getBlock()) == CivData.WALL_SIGN) {
						continue;
					}
					
					if (CivSettings.alwaysCrumble.contains(ItemManager.getId(coord.getBlock()))) {
						ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
						continue;
					}
								
					Random rand = new Random();
					
					// Each block has a 10% chance to turn into gravel
					if (rand.nextInt(100) <= 10) {
						ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
						ItemManager.setData(coord.getBlock(), 0, true);
						continue;
					}
					
					// Each block has a 50% chance of starting a fire
					if (rand.nextInt(100) <= 50) {
						ItemManager.setTypeId(coord.getBlock(), CivData.FIRE);
						ItemManager.setData(coord.getBlock(), 0, true);
						continue;
					}
					
					// Each block has a 1% chance of launching an explosion effect
					if (rand.nextInt(100) <= 1) {
						FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.ORANGE).withColor(Color.RED).withTrail().withFlicker().build();
						FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
						for (int i = 0; i < 3; i++) {
							try {
								fePlayer.playFirework(coord.getBlock().getWorld(), coord.getLocation(), effect);
							} catch (Exception e) {
								e.printStackTrace();
							}
						}			
					}
				}
			}
		}
		
		TaskMaster.syncTask(new SyncTask());
	}
	
	public abstract void onComplete();
	public abstract void onLoad() throws CivException;
	public abstract void onUnload();
	
	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
	}
	
	public void onTechUpdate() {
	}
	
	public void processRegen() {
		if (this.validated && !this.isValid()) {
			/* Do not regen invalid structures. */
			return;
		}
		
		int regenRate = this.getRegenRate();
		regenRate += this.getTown().getBuffManager().getEffectiveInt("buff_chichen_itza_regen_rate");
		
		if (regenRate != 0) {
			if ((this.getHitpoints() != this.getMaxHitPoints()) && 
					(this.getHitpoints() != 0)) {
				this.setHitpoints(this.getHitpoints() + regenRate);				
				
				if (this.getHitpoints() > this.getMaxHitPoints()) {
					this.setHitpoints(this.getMaxHitPoints());
				}
			}
		}
	}
	
	public void onUpdate() {
	}
	
	/*
	 * Plays a fire effect on all of the structure blocks for this structure.
	 */
	public void flashStructureBlocks() {
		World world = null;
		for (BlockCoord coord : structureBlocks.keySet()) {
			if (world == null) {
				world = coord.getLocation().getWorld();
			}
			
			world.playEffect(coord.getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
		}
	}
	
	public boolean showOnDynmap() {
		return true;
	}
	public void onDailyEvent() {		
	}
	
	public void onPreBuild(Location centerLoc) throws CivException {
	}
	public void updateSignText() {}
	
//	public boolean isAborted() {
//		return aborted;
//	}
//	
//	public void setAborted(boolean aborted) {
//		this.aborted = aborted;
//	}
	
	public void repairFromTemplate() throws IOException, CivException {
	//	Template tpl = new Template();
		//tpl.load_template(this.getSavedTemplatePath());
		Template tpl = Template.getTemplate(this.getSavedTemplatePath(), null);
		this.buildRepairTemplate(tpl, this.getCorner().getBlock());
		TaskMaster.syncTask(new PostBuildSyncTask(tpl, this));
	}
	
	public boolean isTownHall() {
		return (this instanceof TownHall);
	}
	
	public void markInvalid() {
		if (this.getCiv().isAdminCiv()) {
			this.valid = true;
		} else {
			this.valid = false;
			this.getTown().invalidStructures.add(this);
		}
	}

	public boolean isValid() {
		if (this.getCiv().isAdminCiv()) {
			return true;
		}
		
		return valid;
	}

	public String getInvalidReason() {
		return invalidReason;
	}

	public void setInvalidReason(String invalidReason) {
		this.invalidReason = invalidReason;
	}
	
	public static int getBlockIDFromSnapshotMap(HashMap<ChunkCoord, ChunkSnapshot> snapshots, int absX, int absY, int absZ, String worldName) throws CivException {
		
		int chunkX = ChunkCoord.castToChunkX(absX);
		int chunkZ = ChunkCoord.castToChunkZ(absZ);
		
		int blockChunkX = absX % 16;
		int blockChunkZ = absZ % 16;
		
		if (blockChunkX < 0) {
			blockChunkX += 16;
		}
		
		if (blockChunkZ < 0) {
			blockChunkZ += 16;
		}
		
		ChunkCoord coord = new ChunkCoord(worldName, chunkX, chunkZ);
		
		ChunkSnapshot snapshot = snapshots.get(coord);
		if (snapshot == null) {
			throw new CivException("Snapshot for chunk "+chunkX+", "+chunkZ+" in "+worldName+" not found for abs:"+absX+","+absZ);
		}
		
		return ItemManager.getBlockTypeId(snapshot, blockChunkX, absY, blockChunkZ);
	}
	
	public static double getReinforcementRequirementForLevel(int level) {
		if (level > 10) {
			return Buildable.validPercentRequirement*0.3;
		}
		
		if (level > 40) {
			return Buildable.validPercentRequirement*0.1;
		}
		
		return Buildable.validPercentRequirement;
	}
	
	
	public boolean isIgnoreFloating() {
		return info.ignore_floating;
	}
	
	//public static ReentrantLock validateLock = new ReentrantLock();
	public void validate(Player player) throws CivException {
		TaskMaster.asyncTask(new StructureValidator(player, this), 0);
	}

	public void setValid(boolean b) {
		if (this.getCiv().isAdminCiv()) {
			this.valid = true;
		} else {
			this.valid = b;
		}
	}
	
	public void onGoodieFromFrame() {
	}

	public void onGoodieToFrame() {
	}
	
	@Override
	public void delete() throws SQLException {
		for (Component comp : this.attachedComponents) {
			comp.destroyComponent();
		}
	}
	
	protected List<HashMap<String,String>> getComponentInfoList() {
		return info.components;
	}
		
	public Component getComponent(String name) {
		
		for (Component comp : this.attachedComponents) {
			if (comp.getName().equals(name)) {
				return comp;
			}
		}
		return null;
	}
	
	public void loadSettings() {
	}
	
	public void onDemolish() throws CivException {
	}
	
	public static int getReinforcementValue(int typeId) {
		switch (typeId) {
		case CivData.WATER:
		case CivData.WATER_RUNNING:
		case CivData.LAVA:
		case CivData.LAVA_RUNNING:
		case CivData.AIR:
		case CivData.COBWEB:
			return 0;
		case CivData.IRON_BLOCK:
			return 4;
		case CivData.STONE_BRICK:
			return 3;
		case CivData.STONE:
			return 2;
		default:
			return 1;
		}
	}
	
	public boolean hasTemplate() { return info.has_template; }
	public boolean canRestoreFromTemplate() { return true; }
	
	
	public void onInvalidPunish() {
		BlockCoord center = this.getCenterLocation();
		double invalid_hourly_penalty;
		try {
			invalid_hourly_penalty = CivSettings.getDouble(CivSettings.warConfig, "war.invalid_hourly_penalty");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		
		int damage = (int) (this.getMaxHitPoints()*invalid_hourly_penalty);
		if (damage <= 0) {
			damage = 10;
		}
		
		this.damage(damage);

		DecimalFormat df = new DecimalFormat("###");
		CivMessage.sendTown(this.getTown(), CivColor.Rose+"Our town's "+this.getDisplayName()+" at ("+
				center.getX()+","+center.getY()+","+center.getZ()+") cannot be supported by the blocks underneath!");
		CivMessage.sendTown(this.getTown(), CivColor.Rose+"It's lost "+df.format(invalid_hourly_penalty*100)+"% of it's hitpoints! HP is now ("+this.hitpoints+"/"+this.getMaxHitPoints()+")");
		CivMessage.sendTown(this.getTown(), CivColor.Rose+this.invalidLayerMessage);
		CivMessage.sendTown(this.getTown(), CivColor.Rose+"Fix the blocks on this layer then run '/build validatenearest' to fix it.");
		this.save();
			
	}
	public boolean isEnabled() {
		return enabled;
	}
	public void setEnabled(boolean enabled) {
		this.enabled = enabled;
	}
	
}
