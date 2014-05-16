package com.avrgaming.civcraft.siege;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.exception.InvalidObjectException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.template.Template.TemplateType;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.FireWorkTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock;
import com.avrgaming.civcraft.util.SimpleBlock.Type;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarRegen;

public class Cannon extends Buildable {

	public static HashMap<BlockCoord, Cannon> fireSignLocations = new HashMap<BlockCoord, Cannon>(); 
	public static HashMap<BlockCoord, Cannon> angleSignLocations = new HashMap<BlockCoord, Cannon>(); 
	public static HashMap<BlockCoord, Cannon> powerSignLocations = new HashMap<BlockCoord, Cannon>(); 
	public static HashMap<BlockCoord, Cannon> cannonBlocks = new HashMap<BlockCoord, Cannon>();

	private BlockCoord fireSignLocation;
	private BlockCoord angleSignLocation;
	private BlockCoord powerSignLocation;
	private Location cannonLocation;
	private Vector direction = new Vector(0,0,0);

	public static final String RESTORE_NAME = "special:Cannons";
	public static final double STEP = 1.0f;

	public static final byte WALLSIGN_EAST = 0x5;
	public static final byte WALLSIGN_WEST = 0x4;
	public static final byte WALLSIGN_NORTH = 0x2;
	public static final byte WALLSIGN_SOUTH = 0x3;
	public int signDirection;
	
	public static final double minAngle = -35.0f;
	public static final double maxAngle = 35.0f;
	private double angle = 0.0f;
	
	public static final double minPower = 0.0f;
	public static final double maxPower = 50.0f;
	private double power = 0.0f;
		
	private int tntLoaded = 0;
	private int shotCooldown = 0;
	private int hitpoints = 0;
	private Resident owner;
	
	private HashSet<BlockCoord> blocks = new HashSet<BlockCoord>();
	
	public static int tntCost;
	public static int maxCooldown;
	public static int maxHitpoints;
	public static int baseStructureDamage;
	
	private boolean angleFlip = false;
	
	static {
		try {
			tntCost = CivSettings.getInteger(CivSettings.warConfig, "cannon.tnt_cost");
			maxCooldown = CivSettings.getInteger(CivSettings.warConfig, "cannon.cooldown");
			maxHitpoints = CivSettings.getInteger(CivSettings.warConfig, "cannon.hitpoints");
			baseStructureDamage = CivSettings.getInteger(CivSettings.warConfig, "cannon.structure_damage");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	public static void newCannon(Resident resident) throws CivException {
		
		Player player = CivGlobal.getPlayer(resident);
		
		Cannon cannon = new Cannon();
		cannon.buildCannon(player, player.getLocation());
		
	}
	
	public static void cleanupAll() {
		cannonBlocks.clear();
		powerSignLocations.clear();
		angleSignLocations.clear();
		fireSignLocations.clear();
	}
	
	private static void removeAllValues(Cannon cannon, HashMap<BlockCoord, Cannon> map) {
		LinkedList<BlockCoord> removeUs = new LinkedList<BlockCoord>();
		for (BlockCoord bcoord : map.keySet()) {
			Cannon c = map.get(bcoord);
			if (c == cannon) {
				removeUs.add(bcoord);
			}
		}
		
		for (BlockCoord bcoord : removeUs) {
			map.remove(bcoord);
		}
	}
	
	public void cleanup() {
		removeAllValues(this, cannonBlocks);
		removeAllValues(this, powerSignLocations);
		removeAllValues(this, angleSignLocations);
		removeAllValues(this, fireSignLocations);
	}
	
	public void buildCannon(Player player, Location center) throws CivException {
		String templateFile;
		try {
			templateFile = CivSettings.getString(CivSettings.warConfig, "cannon.template");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		
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
		
		corner = new BlockCoord(center);
		corner.setFromLocation(this.repositionCenter(center, tpl.dir(), tpl.size_x, tpl.size_z));
		checkBlockPermissionsAndRestrictions(player, corner.getBlock(), tpl.size_x, tpl.size_y, tpl.size_z);
		buildCannonFromTemplate(tpl, corner);
		processCommandSigns(tpl, corner);
		this.hitpoints = maxHitpoints;
		this.owner = CivGlobal.getResident(player);
		
		try {
			this.saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException("Internal SQL Error.");
		}
		
	}

	protected void checkBlockPermissionsAndRestrictions(Player player, Block centerBlock, int regionX, int regionY, int regionZ) throws CivException {
		
		if (!War.isWarTime()) {
			throw new CivException("Can only build Cannons during war time.");
		}
		
		if (player.getLocation().getY() >= 200) {
			throw new CivException("You're too high to build cannons.");
		}
		
		if ((regionY + centerBlock.getLocation().getBlockY()) >= 255) {
			throw new CivException("Cannot build cannon here, would go over the minecraft height limit.");
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
										
					if (CivGlobal.getProtectedBlock(coord) != null) {
						throw new CivException("Cannot build here, protected blocks in the way.");
					}
					
					if (CivGlobal.getStructureBlock(coord) != null) {
						throw new CivException("Cannot build here, structure blocks in the way.");
					}
								
					if (CivGlobal.getCampBlock(coord) != null) {
						throw new CivException("Cannot build here, a camp is in the way.");
					}
					
					if (Cannon.cannonBlocks.containsKey(coord)) {
						throw new CivException("Cannot build here, another cannon in the way.");
					}
					
					yTotal += b.getWorld().getHighestBlockYAt(centerBlock.getX()+x, centerBlock.getZ()+z);
					yCount++;
					
					if (CivGlobal.getRoadBlock(coord) != null) {
						throw new CivException("Cannot build a cannon on top of an existing road block.");
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
	
	private void updateAngleSign(Block block) {
		Sign sign = (Sign)block.getState();
		sign.setLine(0, "YAW");
		sign.setLine(1, ""+this.angle);
		
		double a = this.angle;
		
		if (a > 0) {
			sign.setLine(2, "-->");
		} else if (a < 0){
			sign.setLine(2, "<--");
		} else {
			sign.setLine(2, "");
		}
		
		sign.setLine(3, "");
		sign.update();
	}
	
	private void updatePowerSign(Block block) {
		Sign sign = (Sign)block.getState();
		sign.setLine(0, "PITCH");
		sign.setLine(1, ""+this.power);
		sign.setLine(2, "");
		sign.setLine(3, "");
		sign.update();
	}
	
	private void updateFireSign(Block block) {
		Sign sign = (Sign)block.getState();
		sign.setLine(0, "FIRE");
		boolean loaded = false;
		
		if (this.tntLoaded >= tntCost) {
			sign.setLine(1, CivColor.LightGreen+CivColor.BOLD+"LOADED");
			loaded = true;
		} else {
			sign.setLine(1, CivColor.Yellow+"("+this.tntLoaded+"/"+tntCost+") TNT");
		}
		
		if (this.shotCooldown > 0) {
			sign.setLine(2, CivColor.LightGray+"Wait "+this.shotCooldown);
		} else {
			if (loaded) {
				sign.setLine(2, CivColor.LightGray+"READY");
			} else {
				sign.setLine(2, CivColor.LightGray+"Add TNT");
			}
		}
		
		sign.setLine(3, "");
		sign.update();
	}
	
	private void processCommandSigns(Template tpl, BlockCoord corner) {
		for (BlockCoord relativeCoord : tpl.commandBlockRelativeLocations) {
			SimpleBlock sb = tpl.blocks[relativeCoord.getX()][relativeCoord.getY()][relativeCoord.getZ()];
			BlockCoord absCoord = new BlockCoord(corner.getBlock().getRelative(relativeCoord.getX(), relativeCoord.getY(), relativeCoord.getZ()));
			BlockCoord coord;
			
			switch (sb.command) {
			case "/fire":
				coord = new BlockCoord(absCoord);
				this.setFireSignLocation(coord);

				ItemManager.setTypeIdAndData(coord.getBlock(), sb.getType(), sb.getData(), false);
				updateFireSign(coord.getBlock());

				
				Cannon.fireSignLocations.put(coord, this);
				break;
			case "/angle":
				coord = new BlockCoord(absCoord);
				this.setAngleSignLocation(coord);
				
				ItemManager.setTypeIdAndData(coord.getBlock(), sb.getType(), sb.getData(), false);
				updateAngleSign(coord.getBlock());
				
				Cannon.angleSignLocations.put(coord, this);
				break;
			case "/power":
				coord = new BlockCoord(absCoord);
				this.setPowerSignLocation(coord);

				ItemManager.setTypeIdAndData(coord.getBlock(), sb.getType(), sb.getData(), false);
				updatePowerSign(coord.getBlock());

				Cannon.powerSignLocations.put(coord, this);
				break;
			case "/cannon":
				coord = new BlockCoord(absCoord);
				this.cannonLocation = coord.getLocation();
				
				switch (sb.getData()) {
				case WALLSIGN_EAST:
					cannonLocation.add(1,0,0);
					direction.setX(1.0f);
					direction.setY(0.0f);
					direction.setZ(0.0f);
					break;
				case WALLSIGN_WEST:
					cannonLocation.add(-1,0,0);
					this.angleFlip = true;
					direction.setX(-1.0f);
					direction.setY(0.0f);
					direction.setZ(0.0f);
					break;
				case WALLSIGN_NORTH:
					cannonLocation.add(0,0,-1);
					direction.setX(0.0f);
					direction.setY(0.0f);
					direction.setZ(-1.0f);
					break;
				case WALLSIGN_SOUTH:
					cannonLocation.add(0,0,1);
					this.angleFlip = true;
					direction.setX(0.0f);
					direction.setY(0.0f);
					direction.setZ(1.0f);
					break;
				default:
					CivLog.error("INVALID SIGN DIRECTION..");
					break;
				}
				signDirection = sb.getData();
				
				break;
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
	public void build(Player player, Location centerLoc, Template tpl)
			throws Exception {		
	}

	@Override
	protected void runOnBuild(Location centerLoc, Template tpl)
			throws CivException {		
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
	public void load(ResultSet rs) throws SQLException, InvalidNameException,
			InvalidObjectException, CivException {		
	}

	@Override
	public void save() {		
	}

	@Override
	public void saveNow() throws SQLException {
	}
	
	private void buildCannonFromTemplate(Template tpl, BlockCoord corner) {
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
							/* Save it as a war block so it's automatically removed when war time ends. */
							WarRegen.saveBlock(nextBlock, Cannon.RESTORE_NAME, false);
							ItemManager.setTypeId(nextBlock, tpl.blocks[x][y][z].getType());
							ItemManager.setData(nextBlock, tpl.blocks[x][y][z].getData());
						}
						
						if (ItemManager.getId(nextBlock) != CivData.AIR) {
							BlockCoord b = new BlockCoord(nextBlock.getLocation());
							cannonBlocks.put(b, this);
							blocks.add(b);
						}
					} catch (Exception e) {
						CivLog.error(e.getMessage());
					}
				}
			}
		}
	}
	
	@Override
	protected Location repositionCenter(Location center, String dir, double x_size, double z_size) throws CivException {
		Location loc = center.clone();
		
		if (dir.equalsIgnoreCase("east")) {
			loc.setZ(loc.getZ() - (z_size / 2));
			loc.setX(loc.getX() + SHIFT_OUT);
		}
		else if (dir.equalsIgnoreCase("west")) {
			loc.setZ(loc.getZ() - (z_size / 2));
			loc.setX(loc.getX() - (SHIFT_OUT+x_size));

		}
		else if (dir.equalsIgnoreCase("north")) {
			loc.setX(loc.getX() - (x_size / 2));
			loc.setZ(loc.getZ() - (SHIFT_OUT+z_size));
		}
		else if (dir.equalsIgnoreCase("south")) {
			loc.setX(loc.getX() - (x_size / 2));
			loc.setZ(loc.getZ() + SHIFT_OUT);

		}
		
		return loc;
	}

	public BlockCoord getFireSignLocation() {
		return fireSignLocation;
	}

	public void setFireSignLocation(BlockCoord fireSignLocation) {
		this.fireSignLocation = fireSignLocation;
	}

	public BlockCoord getAngleSignLocation() {
		return angleSignLocation;
	}

	public void setAngleSignLocation(BlockCoord angleSignLocation) {
		this.angleSignLocation = angleSignLocation;
	}

	public BlockCoord getPowerSignLocation() {
		return powerSignLocation;
	}

	public void setPowerSignLocation(BlockCoord powerSignLocation) {
		this.powerSignLocation = powerSignLocation;
	}

	private void validateUse(Player player) throws CivException {
		if (this.hitpoints == 0) {
			throw new CivException("Cannon destroyed.");
		}
		
		Resident resident = CivGlobal.getResident(player);
		
		if (resident.getCiv() != owner.getCiv()) {
			throw new CivException("Only members of the owner's civilization can use a cannon.");
		}
	}
	
	@SuppressWarnings("deprecation")
	public void processFire(PlayerInteractEvent event) throws CivException {
		validateUse(event.getPlayer());
		
		if (this.shotCooldown > 0) {
			CivMessage.sendError(event.getPlayer(), "Wait for the cooldown.");
			return;
		}
		
		if (this.tntLoaded < tntCost) {
			if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
				ItemStack stack = event.getPlayer().getItemInHand();
				if (stack != null) {
					if (ItemManager.getId(stack) == CivData.TNT) {
						if (ItemManager.removeItemFromPlayer(event.getPlayer(), Material.TNT, 1)) {
							this.tntLoaded++;
							CivMessage.sendSuccess(event.getPlayer(), "Added TNT to cannon.");
							updateFireSign(fireSignLocation.getBlock());

							return;
						}
					}
				}
				
				CivMessage.sendError(event.getPlayer(), "Cannon requires TNT to function. Please insert TNT.");
				return;
			} else {
				event.setCancelled(true);
				event.getPlayer().updateInventory();
				return;
			}
		} else {
			CivMessage.send(event.getPlayer(), "Fire!");
			cannonLocation.setDirection(direction);
			Resident resident = CivGlobal.getResident(event.getPlayer());
			CannonProjectile proj = new CannonProjectile(this, cannonLocation.clone(), resident);
			proj.fire();
			this.tntLoaded = 0;
			this.shotCooldown = maxCooldown;
			
			class SyncTask implements Runnable {
				Cannon cannon;
				
				public SyncTask (Cannon cannon) {
					this.cannon = cannon;
				}

				@Override
				public void run() {
					if(cannon.decrementCooldown()) {
						return;
					}
					
					TaskMaster.syncTask(new SyncTask(cannon), TimeTools.toTicks(1));
				}
			}
			TaskMaster.syncTask(new SyncTask(this), TimeTools.toTicks(1));
		}
		
		event.getPlayer().updateInventory();
		updateFireSign(fireSignLocation.getBlock());

	}

	public boolean decrementCooldown() {
		this.shotCooldown--;
		this.updateFireSign(fireSignLocation.getBlock());
		
		if (this.shotCooldown <= 0) {
			return true;
		}
		
		return false;
	}
	
	@SuppressWarnings("deprecation")
	public void processAngle(PlayerInteractEvent event) throws CivException {
		validateUse(event.getPlayer());
		
		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			this.angle -= STEP;
			if (this.angle < minAngle) {
				this.angle = minAngle;
			}
		} else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			this.angle += STEP;
			if (this.angle > maxAngle) {
				this.angle = maxAngle;
			}
		}

		double a = this.angle;
		if (this.angleFlip) {
			a *= -1;
		}
		
		if (signDirection == WALLSIGN_EAST || signDirection == WALLSIGN_WEST) {
			direction.setZ(a / 100);
		} else {
			// NORTH/SOUTH
			direction.setX(a / 100);
		}
		
		event.getPlayer().updateInventory();
		updateAngleSign(this.angleSignLocation.getBlock());
	}

	@SuppressWarnings("deprecation")
	public void processPower(PlayerInteractEvent event) throws CivException {
		validateUse(event.getPlayer());
		
		if (event.getAction().equals(Action.LEFT_CLICK_BLOCK)) {
			this.power -= STEP;
			if (this.power < minPower) {
				this.power = minPower;
			}
		} else if (event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
			this.power += STEP;
			if (this.power > maxPower) {
				this.power = maxPower;
			}
		}
			
		direction.setY(this.power / 100);
		event.getPlayer().updateInventory();
		updatePowerSign(this.powerSignLocation.getBlock());	
	}

	public void onHit(BlockBreakEvent event) {
		Resident resident = CivGlobal.getResident(event.getPlayer());
		
		if (!resident.hasTown()) {
			CivMessage.sendError(resident, "Can't destroy cannon's if you're not part of a civilization at war.");
			return;
		}
		
		if (resident.getCiv() == owner.getCiv()) {
			CivMessage.sendError(resident, "Can't destroy your own civ's cannons during war.");
			return;
		}
		
		if (!resident.getCiv().getDiplomacyManager().atWarWith(owner.getCiv())) {
			CivMessage.sendError(resident, "You've got to be at war with this cannon's owner civ("+owner.getCiv().getName()+") to destroy it.");
			return;
		}
		
		if (this.hitpoints == 0) {
			CivMessage.sendError(resident, "Cannon already destroyed.");
			return;
		}
		
		this.hitpoints--;
		
		if (hitpoints <= 0) {
			destroy();
			CivMessage.send(event.getPlayer(), CivColor.LightGreen+CivColor.BOLD+"Cannon Destroyed!");
			CivMessage.sendCiv(owner.getCiv(), CivColor.Yellow+"Our Cannon at "+
					cannonLocation.getBlockX()+","+cannonLocation.getBlockY()+","+cannonLocation.getBlockZ()+
					" has been destroyed!");
			return;
		}
		
		CivMessage.send(event.getPlayer(), CivColor.Yellow+"Hit Cannon! ("+this.hitpoints+"/"+maxHitpoints+")");
		CivMessage.sendCiv(owner.getCiv(), CivColor.LightGray+"Our Cannon at "+
				cannonLocation.getBlockX()+","+cannonLocation.getBlockY()+","+cannonLocation.getBlockZ()+
				" has been hit! ("+hitpoints+"/"+maxHitpoints+")");
	}
	
	private void launchExplodeFirework(Location loc) {
		FireworkEffect fe = FireworkEffect.builder().withColor(Color.RED).withColor(Color.ORANGE).flicker(false).with(org.bukkit.FireworkEffect.Type.BALL).build();		
		TaskMaster.syncTask(new FireWorkTask(fe, loc.getWorld(), loc, 3), 0);
	}
	
	private void destroy() {
		for (BlockCoord b : blocks) {
			launchExplodeFirework(b.getCenteredLocation());
			if (b.getBlock().getType().equals(Material.COAL_BLOCK)) {
				ItemManager.setTypeIdAndData(b.getBlock(), CivData.GRAVEL, 0, false);
			} else {
				ItemManager.setTypeIdAndData(b.getBlock(), CivData.AIR, 0, false);
			}
		}
		
		ItemManager.setTypeIdAndData(fireSignLocation.getBlock(), CivData.AIR, 0, false);
		ItemManager.setTypeIdAndData(angleSignLocation.getBlock(), CivData.AIR, 0, false);
		ItemManager.setTypeIdAndData(powerSignLocation.getBlock(), CivData.AIR, 0, false);
		
		blocks.clear();
		this.cleanup();
	}

	public int getTntLoaded() {
		return tntLoaded;
	}

	public void setTntLoaded(int tntLoaded) {
		this.tntLoaded = tntLoaded;
	}

	public int getCooldown() {
		return shotCooldown;
	}

	public void setCooldown(int cooldown) {
		this.shotCooldown = cooldown;
	}

	public int getDamage() {
		return baseStructureDamage;
	}

	
}
