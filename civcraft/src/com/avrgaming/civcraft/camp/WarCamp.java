package com.avrgaming.civcraft.camp;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.Effect;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.exception.InvalidObjectException;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.BuildableDamageBlock;
import com.avrgaming.civcraft.object.ControlPoint;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureBlock;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PlotPermissions;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.RespawnLocationHolder;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.Template.TemplateType;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarRegen;

public class WarCamp extends Buildable implements RespawnLocationHolder {

	public static final String RESTORE_NAME = "special:WarCamps";
	private ArrayList<BlockCoord> respawnPoints = new ArrayList<BlockCoord>();
	protected HashMap<BlockCoord, ControlPoint> controlPoints = new HashMap<BlockCoord, ControlPoint>();

	public static void newCamp(Resident resident, ConfigBuildableInfo info) {
		
		class SyncBuildWarCampTask implements Runnable {
			Resident resident;
			ConfigBuildableInfo info;
			
			public SyncBuildWarCampTask(Resident resident, ConfigBuildableInfo info) {
				this.resident = resident;
				this.info = info;
			}
			
			@Override
			public void run() {
				Player player;
				try {
					player = CivGlobal.getPlayer(resident);
				} catch (CivException e) {
					return;
				}
				
				try {					
					if (!resident.hasTown()) {
						throw new CivException("You must be part of a civilization to found a war camp.");
					}
					
					if (!resident.getCiv().getLeaderGroup().hasMember(resident) &&
						!resident.getCiv().getAdviserGroup().hasMember(resident)) {
						throw new CivException("You must be a leader or adviser of the civilization to found a war camp.");
					}
					
					int warCampMax;
					try {
						warCampMax = CivSettings.getInteger(CivSettings.warConfig, "warcamp.max");
					} catch (InvalidConfiguration e) {
						e.printStackTrace();
						return;
					}
					
					if (resident.getCiv().getWarCamps().size() >= warCampMax) {
						throw new CivException("You can only have "+warCampMax+" war camps.");
					}
					
					ItemStack stack = player.getItemInHand();
					LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterial(stack);
					if (craftMat == null || !craftMat.hasComponent("FoundWarCamp")) {
						throw new CivException("You must be holding an item that can found a war camp.");
					}
					
					WarCamp camp = new WarCamp(resident, player.getLocation(), info);
					camp.buildCamp(player, player.getLocation());
					resident.getCiv().addWarCamp(camp);
				
					CivMessage.sendSuccess(player, "You have set up a war camp!");
					camp.setWarCampBuilt();
					ItemStack newStack = new ItemStack(Material.AIR);
					player.setItemInHand(newStack);
				} catch (CivException e) {
					CivMessage.sendError(player, e.getMessage());
				}
			}
		}
		
		TaskMaster.syncTask(new SyncBuildWarCampTask(resident, info));
	}
	
	public String getSessionKey() {
		return this.getCiv().getName()+":warcamp:built";
	}
	
	public void setWarCampBuilt() {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getSessionKey());
		Date now = new Date();
		if (entries.size() == 0) {
			CivGlobal.getSessionDB().add(getSessionKey(), now.getTime()+"", this.getCiv().getId(), this.getTown().getId(), 0);
		} else {
			CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, now.getTime()+"");
		}
	}
	
	public int isWarCampCooldownLeft() {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getSessionKey());
		Date now = new Date();
		long minsLeft = 0;
		if (entries.size() == 0) {
			return 0;
		} else {
			Date then = new Date(Long.valueOf(entries.get(0).value));
			int rebuild_timeout;
			try {
				rebuild_timeout = CivSettings.getInteger(CivSettings.warConfig, "warcamp.rebuild_timeout");
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				return 0;
			}

			minsLeft = (then.getTime() + (rebuild_timeout*60*1000)) - now.getTime();
			minsLeft /= 1000;
			minsLeft /= 60;
			if (now.getTime() > (then.getTime() + (rebuild_timeout*60*1000))) {
				return 0;
			}
			return (int)minsLeft;
		}
	}

	public WarCamp(Resident resident, Location loc, ConfigBuildableInfo info) {
		this.setCorner(new BlockCoord(loc));
		this.setTown(resident.getTown());
		this.info = info;
	}
	
	public void buildCamp(Player player, Location center) throws CivException {
		
		String templateFile;
		try {
			templateFile = CivSettings.getString(CivSettings.warConfig, "warcamp.template");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		Resident resident = CivGlobal.getResident(player);

		/* Load in the template. */
		Template tpl;
		try {
			String templatePath = Template.getTemplateFilePath(templateFile, Template.getDirection(center), TemplateType.STRUCTURE, "default");
			this.setTemplateName(templatePath);
			tpl = Template.getTemplate(templatePath, center);
		} catch (IOException e) {
			e.printStackTrace();
			throw new CivException("Internal Error.");
		} catch (CivException e) {
			e.printStackTrace();
			throw new CivException("Internal Error.");
		}
	
				
		corner.setFromLocation(this.repositionCenter(center, tpl.dir(), tpl.size_x, tpl.size_z));
		checkBlockPermissionsAndRestrictions(player, corner.getBlock(), tpl.size_x, tpl.size_y, tpl.size_z);
		buildWarCampFromTemplate(tpl, corner);
		processCommandSigns(tpl, corner);
		try {
			this.saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException("Internal SQL Error.");
		}
		
		resident.save();
		
	}
	private void processCommandSigns(Template tpl, BlockCoord corner) {
		for (BlockCoord relativeCoord : tpl.commandBlockRelativeLocations) {
			SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
			BlockCoord absCoord = new BlockCoord(corner.getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));
			
			switch (sb.command) {
			case "/respawn":
				this.respawnPoints.add(absCoord);
				BlockCoord coord = new BlockCoord(absCoord);
				ItemManager.setTypeId(coord.getBlock(), CivData.AIR);
				this.addStructureBlock(new BlockCoord(absCoord), false);
				
				coord = new BlockCoord(absCoord);
				coord.setY(absCoord.getY()+1);
				ItemManager.setTypeId(coord.getBlock(), CivData.AIR);
				this.addStructureBlock(coord, false);

				break;
			case "/control":
				this.createControlPoint(absCoord);
				break;
			}
		}
	}
		
	protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ) throws CivException {
		
		if (!War.isWarTime()) {
			throw new CivException("Can only build War Camps during war time.");
		}
		
		if (player.getLocation().getY() >= 200) {
			throw new CivException("You're too high to build camps.");
		}
		
		if ((regionY + centerBlock.getLocation().getBlockY()) >= 255) {
			throw new CivException("Cannot build war camp here, would go over the minecraft height limit.");
		}
		
		int minsLeft = this.isWarCampCooldownLeft();
		if (minsLeft > 0) {
			throw new CivException("Building a War Camp is on cooldown. You must wait "+minsLeft+" mins before you can build another.");
		}
		
		if (!player.isOp()) {
			Buildable.validateDistanceFromSpawn(centerBlock.getLocation());
		}
		
		int yTotal = 0;
		int yCount = 0;
		
		for (int x = 0; x < regionX; x++) {
			for (int y = 0; y < regionY; y++) {
				for (int z = 0; z < regionZ; z++) {
					Block b = centerBlock.getRelative(x, y, z);
					
					if (ItemManager.getId(b) == CivData.CHEST) {
						throw new CivException("Cannot build here, would destroy chest.");
					}
		
					BlockCoord coord = new BlockCoord(b);
					ChunkCoord chunkCoord = new ChunkCoord(coord.getLocation());
					
					TownChunk tc = CivGlobal.getTownChunk(chunkCoord);
					if (tc != null && !tc.perms.hasPermission(PlotPermissions.Type.DESTROY, CivGlobal.getResident(player))) {
						// Make sure we have permission to destroy any block in this area.
						throw new CivException("Cannot build here, you need DESTROY permissions to the block at "+b.getX()+","+b.getY()+","+b.getZ());
					}
					
					if (CivGlobal.getProtectedBlock(coord) != null) {
						throw new CivException("Cannot build here, protected blocks in the way.");
					}
					
					if (CivGlobal.getStructureBlock(coord) != null) {
						throw new CivException("Cannot build here, structure blocks in the way.");
					}
				
					if (CivGlobal.getFarmChunk(chunkCoord) != null) {
						throw new CivException("Cannot build here, in the same chunk as a farm improvement.");
					}
		
					if (CivGlobal.getWallChunk(chunkCoord) != null) {
						throw new CivException("Cannot build here, in the same chunk as a wall improvement.");
					}
					
					if (CivGlobal.getCampBlock(coord) != null) {
						throw new CivException("Cannot build here, a camp is in the way.");
					}
					
					yTotal += b.getWorld().getHighestBlockYAt(centerBlock.getX()+x, centerBlock.getZ()+z);
					yCount++;
					
					if (CivGlobal.getRoadBlock(coord) != null) {
						throw new CivException("Cannot build a war camp on top of an existing road block.");
					}
				}
			}
		}
		
		double highestAverageBlock = (double)yTotal / (double)yCount;
		
		if (((centerBlock.getY() > (highestAverageBlock+10)) || 
				(centerBlock.getY() < (highestAverageBlock-10)))) {
			throw new CivException("Cannot build here, you must be closer to the surface.");
		}
	}
	
	private void buildWarCampFromTemplate(Template tpl, BlockCoord corner) {
		Block cornerBlock = corner.getBlock();
		for (int x = 0; x < tpl.size_x; x++) {
			for (int y = 0; y < tpl.size_y; y++) {
				for (int z = 0; z < tpl.size_z; z++) {
					Block nextBlock = cornerBlock.getRelative(x, y, z);
					
					if (tpl.blocks[x][y][z].specialType == Type.COMMAND) {
						continue;
					}
					
					if (tpl.blocks[x][y][z].specialType == Type.LITERAL) {
						// Adding a command block for literal sign placement
						tpl.blocks[x][y][z].command = "/literal";
						tpl.commandBlockRelativeLocations.add(new BlockCoord(cornerBlock.getWorld().getName(), x, y,z));
						continue;
					}

					try {
						if (ItemManager.getId(nextBlock) != tpl.blocks[x][y][z].getType()) {
							/* XXX Save it as a war block so it's automatically removed when war time ends. */
							WarRegen.saveBlock(nextBlock, WarCamp.RESTORE_NAME, false);
							ItemManager.setTypeId(nextBlock, tpl.blocks[x][y][z].getType());
							ItemManager.setData(nextBlock, tpl.blocks[x][y][z].getData());
						}
						
						if (ItemManager.getId(nextBlock) != CivData.AIR) {
							this.addStructureBlock(new BlockCoord(nextBlock.getLocation()), true);
						}
					} catch (Exception e) {
						CivLog.error(e.getMessage());
					}
				}
			}
		}
	}
	
	@Override
	public void processUndo() throws CivException {
	}

	@Override
	public void updateBuildProgess() {
		
	}

	@Override
	public void build(Player player, Location centerLoc, Template tpl) throws Exception {
		
	}

	@Override
	protected void runOnBuild(Location centerLoc, Template tpl) throws CivException {
		
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}

	@Override
	public String getMarkerIconName() {
		return null;
	}

	@Override
	public void onComplete() {
		
	}

	@Override
	public void onLoad() throws CivException {
		
	}

	@Override
	public void onUnload() {
		
	}

	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException, InvalidObjectException, CivException {
		
	}

	@Override
	public void save() {
		
	}

	@Override
	public void saveNow() throws SQLException {
		
	}
	
	public void createControlPoint(BlockCoord absCoord) {
		
		Location centerLoc = absCoord.getLocation();
		
		/* Build the bedrock tower. */
		//for (int i = 0; i < 1; i++) {
		Block b = centerLoc.getBlock();
		WarRegen.saveBlock(b, WarCamp.RESTORE_NAME, false);
		ItemManager.setTypeId(b, CivData.FENCE); ItemManager.setData(b, 0);

		StructureBlock sb = new StructureBlock(new BlockCoord(b), this);
		this.addStructureBlock(sb.getCoord(), true);
		//}
		
		/* Build the control block. */
		b = centerLoc.getBlock().getRelative(0, 1, 0);
		WarRegen.saveBlock(b, WarCamp.RESTORE_NAME, false);
		ItemManager.setTypeId(b, CivData.OBSIDIAN);

		sb = new StructureBlock(new BlockCoord(b), this);
		this.addStructureBlock(sb.getCoord(), true);
		
		int townhallControlHitpoints;
		try {
			townhallControlHitpoints = CivSettings.getInteger(CivSettings.warConfig, "warcamp.control_block_hitpoints");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		
		BlockCoord coord = new BlockCoord(b);
		this.controlPoints.put(coord, new ControlPoint(coord, this, townhallControlHitpoints));
	}

	@Override
	public void onDamage(int amount, World world, Player player, BlockCoord coord, BuildableDamageBlock hit) {
		ControlPoint cp = this.controlPoints.get(coord);
		Resident resident = CivGlobal.getResident(player);
	
		if (cp != null) {
			if (!cp.isDestroyed()) {
			
				if (resident.isControlBlockInstantBreak()) {
					cp.damage(cp.getHitpoints());
				} else{
					cp.damage(amount);
				}
				 
				if (cp.isDestroyed()) {
					onControlBlockDestroy(cp, world, player, (StructureBlock)hit);
				} else {
					onControlBlockHit(cp, world, player, (StructureBlock)hit);
				}
			} else {
				CivMessage.send(player, CivColor.Rose+"Control Block already destroyed.");
			}
			
		} else {
			CivMessage.send(player, CivColor.Rose+"Cannot Damage " +this.getDisplayName()+ ", go after the control points!");
		}
	}
	
	public void onControlBlockDestroy(ControlPoint cp, World world, Player player, StructureBlock hit) {
		//Should always have a resident and a town at this point.
		Resident attacker = CivGlobal.getResident(player);
		
		ItemManager.setTypeId(hit.getCoord().getLocation().getBlock(), CivData.AIR);
		world.playSound(hit.getCoord().getLocation(), Sound.ANVIL_BREAK, 1.0f, -1.0f);
		world.playSound(hit.getCoord().getLocation(), Sound.EXPLODE, 1.0f, 1.0f);
		
		FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.OLIVE).withColor(Color.RED).withTrail().withFlicker().build();
		FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
		for (int i = 0; i < 3; i++) {
			try {
				fePlayer.playFirework(world, hit.getCoord().getLocation(), effect);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
		boolean allDestroyed = true;
		for (ControlPoint c : this.controlPoints.values()) {
			if (c.isDestroyed() == false) {
				allDestroyed = false;
				break;
			}
		}

		if (allDestroyed) {
			this.onWarCampDestroy();	
		}
		else {
			CivMessage.sendCiv(attacker.getTown().getCiv(), CivColor.LightGreen+"We've destroyed a control block in "+getCiv().getName()+"'s War Camp!");
			CivMessage.sendCiv(getCiv(), CivColor.Rose+"A control block in our War Camp has been destroyed!");
		}
		
	}
	
	private void onWarCampDestroy() {
		CivMessage.sendCiv(this.getCiv(), CivColor.Rose+"Our War Camp has been destroyed!");
		this.getCiv().getWarCamps().remove(this);
		
		for (BlockCoord coord : this.structureBlocks.keySet()) {
			CivGlobal.removeStructureBlock(coord);
		}
		this.structureBlocks.clear();
		
		this.fancyDestroyStructureBlocks();
		setWarCampBuilt();
	}
	
	public void onControlBlockHit(ControlPoint cp, World world, Player player, StructureBlock hit) {
		world.playSound(hit.getCoord().getLocation(), Sound.ANVIL_USE, 0.2f, 1);
		world.playEffect(hit.getCoord().getLocation(), Effect.MOBSPAWNER_FLAMES, 0);
		
		CivMessage.send(player, CivColor.LightGray+"Damaged Control Block ("+cp.getHitpoints()+" / "+cp.getMaxHitpoints()+")");
		CivMessage.sendCiv(getCiv(), CivColor.Yellow+"Our War Camp's Control Points are under attack!");
	}

	@Override
	public String getRespawnName() {
		return "WarCamp\n("+this.corner.getX()+","+this.corner.getY()+","+this.corner.getZ()+")";
	}

	@Override
	public List<BlockCoord> getRespawnPoints() {
		return this.getRespawnPoints();
	}

	@Override
	public BlockCoord getRandomRevivePoint() {
		if (this.respawnPoints.size() == 0) {
			return new BlockCoord(this.getCorner());
		}
		Random rand = new Random();
		int index = rand.nextInt(this.respawnPoints.size());
		return this.respawnPoints.get(index);
	}

	public void onWarEnd() {
		
		/* blocks are cleared by war regen, but structure blocks need to be cleared. */
		for (BlockCoord coord : this.structureBlocks.keySet()) {
			CivGlobal.removeStructureBlock(coord);
		}
		
		this.structureBlocks.clear();
	}
}
