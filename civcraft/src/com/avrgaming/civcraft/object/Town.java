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
package com.avrgaming.civcraft.object;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.concurrent.ConcurrentHashMap;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.AttributeBase;
import com.avrgaming.civcraft.components.AttributeRate;
import com.avrgaming.civcraft.components.AttributeWarUnhappiness;
import com.avrgaming.civcraft.components.Component;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuff;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.config.ConfigGovernment;
import com.avrgaming.civcraft.config.ConfigHappinessState;
import com.avrgaming.civcraft.config.ConfigTownLevel;
import com.avrgaming.civcraft.config.ConfigTownUpgrade;
import com.avrgaming.civcraft.config.ConfigUnit;
import com.avrgaming.civcraft.database.SQL;
import com.avrgaming.civcraft.database.SQLUpdate;
import com.avrgaming.civcraft.exception.AlreadyRegisteredException;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.interactive.InteractiveBuildableRefresh;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.items.units.Unit;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.main.Colors;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.randomevents.RandomEvent;
import com.avrgaming.civcraft.road.Road;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.structure.TradeOutpost;
import com.avrgaming.civcraft.structure.Wall;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.sync.SyncUpdateTags;
import com.avrgaming.civcraft.threading.tasks.BuildAsyncTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.DateUtil;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.util.WorldCord;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.CustomTemplate;

public class Town extends SQLObject {

	private ConcurrentHashMap<String, Resident> residents = new ConcurrentHashMap<String, Resident>();
	private ConcurrentHashMap<String, Resident> fakeResidents = new ConcurrentHashMap<String, Resident>();

	private ConcurrentHashMap<ChunkCoord, TownChunk> townChunks = new ConcurrentHashMap<ChunkCoord, TownChunk>();
	private ConcurrentHashMap<ChunkCoord, TownChunk> outposts = new ConcurrentHashMap<ChunkCoord, TownChunk>();
	private ConcurrentHashMap<ChunkCoord, CultureChunk> cultureChunks = new ConcurrentHashMap<ChunkCoord, CultureChunk>();
	
	private ConcurrentHashMap<BlockCoord, Wonder> wonders = new ConcurrentHashMap<BlockCoord, Wonder>();
	private ConcurrentHashMap<BlockCoord, Structure> structures = new ConcurrentHashMap<BlockCoord, Structure>();
	private ConcurrentHashMap<BlockCoord, Buildable> disabledBuildables = new ConcurrentHashMap<BlockCoord, Buildable>();
	
	private int level;
	private double taxRate;
	private double flatTax;
	private Civilization civ;
	private Civilization motherCiv;
	private int daysInDebt;
	
	/* Hammers */
	private double baseHammers = 1.0;
	private double extraHammers;
	public Buildable currentStructureInProgress;
	public Buildable currentWonderInProgress;
	
	/* Culture */
	private int culture;
	
	private PermissionGroup defaultGroup;
	private PermissionGroup mayorGroup;
	private PermissionGroup assistantGroup;
	
	/* Beakers */
	private double unusedBeakers;
	
	// These are used to resolve reverse references after the database loads.
	private String defaultGroupName;
	private String mayorGroupName;
	private String assistantGroupName;
	
	public ArrayList<TownChunk> savedEdgeBlocks = new ArrayList<TownChunk>();
	public HashSet<Town> townTouchList = new HashSet<Town>();
	
	private ConcurrentHashMap<String, PermissionGroup> groups = new ConcurrentHashMap<String, PermissionGroup>();
	private EconObject treasury;
	private ConcurrentHashMap<String, ConfigTownUpgrade> upgrades = new ConcurrentHashMap<String, ConfigTownUpgrade>();
			
	/* This gets populated periodically from a synchronous timer so it will be accessible from async tasks. */
	private ConcurrentHashMap<String, BonusGoodie> bonusGoodies = new ConcurrentHashMap<String, BonusGoodie>();

	private BuffManager buffManager = new BuffManager();
	
	private boolean pvp = false;
	
	public ArrayList<BuildAsyncTask> build_tasks = new ArrayList<BuildAsyncTask>();
	public Buildable lastBuildableBuilt = null;

	public boolean leaderWantsToDisband = false;
	public boolean mayorWantsToDisband = false;
	public HashSet<String> outlaws = new HashSet<String>();

	public boolean claimed = false;
	public boolean defeated = false;
	public LinkedList<Buildable> invalidStructures = new LinkedList<Buildable>();
	
	/* XXX kind of a hacky way to save the bank's level information between build undo calls */
	public int saved_bank_level = 1;
	public double saved_bank_interest_amount = 0;
	
	/* Happiness Stuff */
	private double baseHappy = 0.0;
	private double baseUnhappy = 0.0;
		
	private RandomEvent activeEvent;
	
	/* Last time someone used /build refreshblocks, make sure they can do it only so often.	 */
	private Date lastBuildableRefresh = null;
	private Date created_date;
	
	/* 
	 * Time it takes before a new attribute is calculated
	 * Otherwise its loaded from the cache.
	 */
	public static final int ATTR_TIMEOUT_SECONDS = 5;
	class AttrCache {
		public Date lastUpdate;
		public AttrSource sources;
	}
	public HashMap<String, AttrCache> attributeCache = new HashMap<String, AttrCache>();
	
	private double baseGrowth = 0.0;
	
	public static final String TABLE_NAME = "TOWNS";
	public static void init() throws SQLException {
		if (!SQL.hasTable(TABLE_NAME)) {
			String table_create = "CREATE TABLE " + SQL.tb_prefix + TABLE_NAME+" (" + 
					"`id` int(11) unsigned NOT NULL auto_increment," +
					"`name` VARCHAR(64) NOT NULL," +
					"`civ_id` int(11) NOT NULL DEFAULT 0," +
					"`master_civ_id` int(11) NOT NULL DEFAULT 0," + //XXX no longer used.
					"`mother_civ_id` int(11) NOT NULL DEFAULT 0," +
					"`defaultGroupName` mediumtext DEFAULT NULL," +
					"`mayorGroupName` mediumtext DEFAULT NULL," +
					"`assistantGroupName` mediumtext DEFAULT NULL," +
					"`upgrades` mediumtext DEFAULT NULL," +
					"`level` int(11) DEFAULT 1," +
					"`debt` double DEFAULT 0," +
					"`coins` double DEFAULT 0," +
					"`daysInDebt` int(11) DEFAULT 0,"+
					"`flat_tax` double NOT NULL DEFAULT '0'," + 
					"`tax_rate` double DEFAULT 0," + 
					"`extra_hammers` double DEFAULT 0," +
					"`culture` int(11) DEFAULT 0," +
					"`created_date` long," +
					"`outlaws` mediumtext DEFAULT NULL,"+
					"`dbg_civ_name` mediumtext DEFAULT NULL,"+
				"UNIQUE KEY (`name`), " +
				"PRIMARY KEY (`id`)" + ")";
			
			SQL.makeTable(table_create);
			CivLog.info("Created "+TABLE_NAME+" table");
		} else {
			CivLog.info(TABLE_NAME+" table OK!");
			
			//Check for new columns and update the table if we dont have them.
			SQL.makeCol("outlaws", "mediumtext", TABLE_NAME);
			SQL.makeCol("daysInDebt", "int(11)", TABLE_NAME);
			SQL.makeCol("mother_civ_id", "int(11)", TABLE_NAME);
			SQL.makeCol("dbg_civ_name", "mediumtext", TABLE_NAME);
			SQL.makeCol("created_date", "long", TABLE_NAME);
		}
	}
	
	@Override
	public void load(ResultSet rs) throws SQLException, InvalidNameException, CivException {
		this.setId(rs.getInt("id"));
		this.setName(rs.getString("name"));		
		this.setLevel(rs.getInt("level"));
		this.setCiv(CivGlobal.getCivFromId(rs.getInt("civ_id")));
		
		Integer motherCivId = rs.getInt("mother_civ_id");
		if (motherCivId != null && motherCivId != 0) {
			Civilization mother = CivGlobal.getConqueredCivFromId(motherCivId);
			if (mother == null) {
				mother = CivGlobal.getCivFromId(motherCivId);
			}
			
			if (mother == null) {
				CivLog.warning("Unable to find a mother civ with ID:"+motherCivId+"!");
			} else {
				setMotherCiv(mother);
			}
		}
		
		if (this.getCiv() == null) {
			CivLog.error("TOWN:"+this.getName()+" WITHOUT A CIV, id was:"+rs.getInt("civ_id"));
			//this.delete();
			CivGlobal.orphanTowns.add(this);
			throw new CivException("Failed to load town, bad data.");
		}
		this.setDaysInDebt(rs.getInt("daysInDebt"));
		this.setFlatTax(rs.getDouble("flat_tax"));
		this.setTaxRate(rs.getDouble("tax_rate"));
		this.setUpgradesFromString(rs.getString("upgrades"));
		
		//this.setHomeChunk(rs.getInt("homechunk_id"));
		this.setExtraHammers(rs.getDouble("extra_hammers"));
		this.setAccumulatedCulture(rs.getInt("culture"));
		
		defaultGroupName = "residents";
		mayorGroupName = "mayors";
		assistantGroupName = "assistants";

		this.setTreasury(new EconObject(this));
		this.getTreasury().setBalance(rs.getDouble("coins"), false);
		this.setDebt(rs.getDouble("debt"));
		
		String outlawRaw = rs.getString("outlaws");
		if (outlawRaw != null) {
			String[] outlaws = outlawRaw.split(",");
			
			for (String outlaw : outlaws) {
				this.outlaws.add(outlaw);
			}
		}
		
		Long ctime = rs.getLong("created_date");
		if (ctime == null || ctime == 0) {
			this.setCreated(new Date(0)); //Forever in the past.
		} else {
			this.setCreated(new Date(ctime));
		}
		
		this.getCiv().addTown(this);	
	}

	@Override
	public void save() {
		SQLUpdate.add(this);
	}
	
	@Override
	public void saveNow() throws SQLException {
		HashMap<String, Object> hashmap = new HashMap<String, Object>();
		
		hashmap.put("name", this.getName());
		hashmap.put("civ_id", this.getCiv().getId());
		
		if (this.motherCiv != null) {
			hashmap.put("mother_civ_id", this.motherCiv.getId());
		} else {
			hashmap.put("mother_civ_id", 0);
		}
		
		hashmap.put("defaultGroupName", this.getDefaultGroupName());
		hashmap.put("mayorGroupName", this.getMayorGroupName());
		hashmap.put("assistantGroupName", this.getAssistantGroupName());
		hashmap.put("level", this.getLevel());
		hashmap.put("debt", this.getTreasury().getDebt());
		hashmap.put("daysInDebt", this.getDaysInDebt());
		hashmap.put("flat_tax", this.getFlatTax());
		hashmap.put("tax_rate", this.getTaxRate());
		hashmap.put("extra_hammers", this.getExtraHammers());
		hashmap.put("culture", this.getAccumulatedCulture());
		hashmap.put("upgrades", this.getUpgradesString());
		hashmap.put("coins", this.getTreasury().getBalance());
		hashmap.put("dbg_civ_name", this.getCiv().getName());
		
		if (this.created_date != null) {
			hashmap.put("created_date", this.created_date.getTime());
		} else {
			hashmap.put("created_date", null);
		}
		
		String outlaws = "";
		for (String outlaw : this.outlaws) {
			outlaws += outlaw+",";
		}
		hashmap.put("outlaws", outlaws);

		SQL.updateNamedObject(this, hashmap, TABLE_NAME);
	}
	
	@Override
	public void delete() throws SQLException {
		
		/* Remove all our Groups */
		for (PermissionGroup grp : this.groups.values()) {
			grp.delete();
		}
		
		/* Remove all of our residents from town. */
		for (Resident resident : this.residents.values()) {
			resident.setTown(null);
			/* Also forgive their debt, nobody to pay it to. */
			resident.getTreasury().setDebt(0);
			resident.saveNow();
		}

		/* Remove all structures in the town. */
		if (this.structures != null) {
			for (Structure struct : this.structures.values()) {
				struct.delete();
			}
		}
		
		/* Remove all town chunks. */
		if (this.getTownChunks() != null) {
			for (TownChunk tc : this.getTownChunks()) {
				tc.delete();
			}
		}

		if (this.wonders != null) {
			for (Wonder wonder : wonders.values()) {
				wonder.unbindStructureBlocks();
				wonder.fancyDestroyStructureBlocks();
				wonder.delete();
			}
		}
		
		if (this.cultureChunks != null) {
			for (CultureChunk cc : this.cultureChunks.values()) {
				CivGlobal.removeCultureChunk(cc);
			}
		}
		this.cultureChunks = null;
		
		//TODO remove protected blocks?
		
		/* Remove any related SessionDB entries */
		CivGlobal.getSessionDB().deleteAllForTown(this);
		
		SQL.deleteNamedObject(this, TABLE_NAME);
		CivGlobal.removeTown(this);
	}


	public Town (String name, Resident mayor, Civilization civ) throws InvalidNameException {
		this.setName(name);
		this.setLevel(1);
		this.setTaxRate(0.0);
		this.setFlatTax(0.0);
		this.setCiv(civ);
		
		this.setDaysInDebt(0);
		this.setHammerRate(1.0);
		this.setExtraHammers(0);	
		this.addAccumulatedCulture(0);
		this.setTreasury(new EconObject(this));	
		this.getTreasury().setBalance(0, false);
		this.created_date = new Date();

		loadSettings();
	}
	
	public Town(ResultSet rs) throws SQLException, InvalidNameException, CivException {
		this.load(rs);
		loadSettings();
	}
	
	public void loadSettings() {
		try {
			this.baseHammers = CivSettings.getDouble(CivSettings.townConfig, "town.base_hammer_rate");
			this.setBaseGrowth(CivSettings.getDouble(CivSettings.townConfig, "town.base_growth_rate"));
			
//			this.happyCoinRate = new AttributeComponent();
//			this.happyCoinRate.setSource("Happiness");
//			this.happyCoinRate.setAttrKey(Attribute.TypeKeys.COINS.name());
//			this.happyCoinRate.setType(AttributeType.RATE);
//			this.happyCoinRate.setOwnerKey(this.getName());
//			this.happyCoinRate.registerComponent();
			
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}

	private void setUpgradesFromString(String upgradeString) {
		String[] split = upgradeString.split(",");
		
		for (String str : split) {
			if (str == null || str.equals("")) {
				continue;
			}
			
			ConfigTownUpgrade upgrade = CivSettings.townUpgrades.get(str);
			if (upgrade == null) {
				CivLog.warning("Unknown town upgrade:"+str+" in town "+this.getName());
				continue;
			}
			
			this.upgrades.put(str, upgrade);
		}
	}
	
	private String getUpgradesString() {
		String out = "";
		
		for (ConfigTownUpgrade upgrade : upgrades.values()) {
			out += upgrade.id + ",";
		}
		
		return out;
	}
	
	public ConfigTownUpgrade getUpgrade(String id) {
		return upgrades.get(id);
	}
	
	public boolean isMayor(Resident res) {
		if (this.getMayorGroup().hasMember(res)) {
			return true;
		}
		return false;		
	}
	
	public int getResidentCount() {
		return residents.size();
	}
	
	public Collection<Resident> getResidents() {
		return residents.values();
	}
	
	public boolean hasResident(String name) {
		return residents.containsKey(name.toLowerCase());
	}
	
	public boolean hasResident(Resident res) {
		return hasResident(res.getName());
	}
	
	public void addResident(Resident res) throws AlreadyRegisteredException {
		String key = res.getName().toLowerCase();
		
		if (residents.containsKey(key)) {
			throw new AlreadyRegisteredException(res.getName()+" already a member of town "+this.getName());
		}
		
		res.setTown(this);				
		
		residents.put(key, res);
		if (this.defaultGroup != null && !this.defaultGroup.hasMember(res)) {
			this.defaultGroup.addMember(res);
			this.defaultGroup.save();
		}
	}
	
	public void addTownChunk(TownChunk tc) throws AlreadyRegisteredException {
		
		if (townChunks.containsKey(tc.getChunkCoord())) {
			throw new AlreadyRegisteredException("TownChunk at "+tc.getChunkCoord()+" already registered to town "+this.getName());
		}
		townChunks.put(tc.getChunkCoord(), tc);
	}
	
	public Structure findStructureByName(String name) {
		for (Structure struct : structures.values()) {
			if (struct.getDisplayName().equalsIgnoreCase(name)) {
				return struct;
			}
		}
		return null;
	}
	
	public Structure findStructureByLocation(WorldCord wc) {
		return structures.get(wc);
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		
//		TownHall townhall = this.getTownHall();
//		if (townhall != null) {
//			if (townhall.nextGoodieFramePoint.size() > 0 &&
//					townhall.nextGoodieFramePoint.size() > 0) {
//				townhall.createGoodieItemFrame(townhall.nextGoodieFramePoint.get(0), level, 
//						townhall.nextGoodieFrameDirection.get(0));
//			}
//		}
		
		this.level = level;
	}

	public double getTaxRate() {
		return taxRate;
	}

	public void setTaxRate(double taxRate) {
		this.taxRate = taxRate;
	}

	public String getTaxRateString() {
		long rounded = Math.round(this.taxRate*100);
		return ""+rounded+"%";	
	}
	
	public double getFlatTax() {
		return flatTax;
	}

	public void setFlatTax(double flatTax) {
		this.flatTax = flatTax;
	}

	public Civilization getCiv() {
		return civ;
	}

	public void setCiv(Civilization civ) {
		this.civ = civ;
	}

	public int getAccumulatedCulture() {
		return culture;
	}

	public void setAccumulatedCulture(int culture) {
		this.culture = culture;
	}
	
	public AttrSource getCultureRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();
		
		double newRate = getGovernment().culture_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;
		
		ConfigHappinessState state = CivSettings.getHappinessState(this.getHappinessPercentage());
		newRate = rate * state.culture_rate;
		rates.put("Happiness", newRate - rate);
		rate = newRate;
		
		double additional = this.getBuffManager().getEffectiveDouble(Buff.FINE_ART);
		
		if (this.getBuffManager().hasBuff("buff_pyramid_culture")) {
			additional += this.getBuffManager().getEffectiveDouble("buff_pyramid_culture");
		}
		
		rates.put("Wonders/Goodies", additional);
		rate += additional;
		
		return new AttrSource(rates, rate, null);
	}
	
	public AttrSource getCulture() {

		AttrCache cache = this.attributeCache.get("CULUTRE");
		if (cache == null) {
			cache = new AttrCache();
			cache.lastUpdate = new Date();
		} else {
			Date now = new Date();
			if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS*1000)) {
				cache.lastUpdate = now;
			} else {
				return cache.sources;
			}
		}
		
		double total = 0;
		HashMap<String, Double> sources = new HashMap<String, Double>();
		
		/* Grab any culture from goodies. */
		double goodieCulture = getBuffManager().getEffectiveInt(Buff.EXTRA_CULTURE);
		sources.put("Goodies", goodieCulture);
		total += goodieCulture;
			
		/* Grab beakers generated from structures with components. */
		double fromStructures = 0;
		for (Structure struct : this.structures.values()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase)comp;
					if (as.getString("attribute").equalsIgnoreCase("CULTURE")) {
						fromStructures += as.getGenerated();
					}
				}
			}
		}
		
		total += fromStructures;
		sources.put("Structures", fromStructures);
		
		AttrSource rate = this.getCultureRate();
		total *= rate.total;
		
		if (total < 0) {
			total = 0;
		}
		
		AttrSource as = new AttrSource(sources, total, rate);
		cache.sources = as;
		this.attributeCache.put("CULTURE", cache);
		return as;
	}


	public void addAccumulatedCulture(double generated) {
		ConfigCultureLevel clc = CivSettings.cultureLevels.get(this.getCultureLevel());
				
		this.culture += generated;
		if (this.getCultureLevel() != CivSettings.getMaxCultureLevel()) {
			if (this.culture >= clc.amount) {
				CivGlobal.processCulture();
				CivMessage.sendCiv(this.civ, "The borders of "+this.getName()+" have expanded!");
			}
		}
		return;
	}


	public double getExtraHammers() {
		return extraHammers;
	}


	public void setExtraHammers(double extraHammers) {
		this.extraHammers = extraHammers;
	}
	
	public AttrSource getHammerRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();
		ConfigHappinessState state = CivSettings.getHappinessState(this.getHappinessPercentage());

		/* Happiness */
		double newRate = rate * state.hammer_rate;
		rates.put("Happiness", newRate - rate);
		rate = newRate;
		
		/* Government */
		newRate = rate * getGovernment().hammer_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;
		
		double randomRate = RandomEvent.getHammerRate(this);
		newRate = rate * randomRate;
		rates.put("Random Events", newRate - rate);
		rate = newRate;
		
		/* Captured Town Penalty */
		if (this.motherCiv != null) {
			try {
				newRate = rate * CivSettings.getDouble(CivSettings.warConfig, "war.captured_penalty");	
				rates.put("Captured Penalty", newRate - rate);
				rate = newRate;
				
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
			}
		}
		return new AttrSource(rates, rate, null);
	}
	
	public AttrSource getHammers() {
		double total = 0;
		
		AttrCache cache = this.attributeCache.get("HAMMERS");
		if (cache == null) {
			cache = new AttrCache();
			cache.lastUpdate = new Date();
		} else {
			Date now = new Date();
			if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS*1000)) {
				cache.lastUpdate = now;
			} else {
				return cache.sources;
			}
		}
	
		HashMap<String, Double> sources = new HashMap<String, Double>();
				
		/* Wonders and Goodies. */
		double wonderGoodies = this.getBuffManager().getEffectiveInt(Buff.CONSTRUCTION);
		sources.put("Wonders/Goodies", wonderGoodies);
		total += wonderGoodies;
		
		double cultureHammers = this.getHammersFromCulture();
		sources.put("Culture Biomes", cultureHammers);
		total += cultureHammers; 
		
		/* Grab happiness generated from structures with components. */
		double structures = 0;
		for (Structure struct : this.structures.values()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase)comp;
					if (as.getString("attribute").equalsIgnoreCase("HAMMERS")) {
						structures += as.getGenerated();
					}
				}
			}
		}

		total += structures;
		sources.put("Structures", structures);
		
		
		sources.put("Base Hammers", this.baseHammers);
		total += this.baseHammers;
		
		AttrSource rate = getHammerRate();
		total *= rate.total;
		
		if (total < this.baseHammers) {
			total = this.baseHammers;
		}
		
		AttrSource as = new AttrSource(sources, total, rate);
		cache.sources = as;
		this.attributeCache.put("HAMMERS", cache);
		return as;
	}

	public void setHammerRate(double hammerRate) {
		this.baseHammers = hammerRate;
	}
	
	public static Town newTown(Resident resident, String name, Civilization civ, boolean free, boolean capitol, 
			Location loc) throws CivException {
		try {
			
			if (War.isWarTime() && !free && civ.getDiplomacyManager().isAtWar()) {
				throw new CivException("Cannot start towns during WarTime if you're at war.");
			}
			
			if (civ == null) {
				throw new CivException("Towns must be founded inside a Civilization.");
			}
			
			if (resident.getTown() != null && resident.getTown().isMayor(resident)) {
				throw new CivException("You cannot start another town since you are the mayor of "+resident.getTown().getName());
			}
		
			if (resident.hasCamp()) {
				throw new CivException("You must first leave your camp before starting a town.");
			}
			
			Town existTown = CivGlobal.getTown(name);
			if (existTown != null) {
				throw new CivException("A town named "+name+" already exists!");
			}
			
			Town newTown;
			try {
				newTown = new Town(name, resident, civ);
			} catch (InvalidNameException e) {
				throw new CivException("The town name of "+name+" is invalid, choose another.");
			}
			
			Player player = Bukkit.getPlayer(resident.getName());
			if (player == null) {
				throw new CivException("Couldn't find you? Are you online? wat?");
			}
			
			if (CivGlobal.getTownChunk(loc) != null) {
				throw new CivException("Cannot start town here, chunk already registered to a town.");
			}
			
			CultureChunk cultrueChunk = CivGlobal.getCultureChunk(loc);
			if (cultrueChunk != null && cultrueChunk.getCiv() != resident.getCiv()) {
				throw new CivException("Cannot start a town inside another civ's cultural borders.");
			}
			
			double minDistanceFriend;
			double minDistanceEnemy;
			try {
				minDistanceFriend = CivSettings.getDouble(CivSettings.townConfig, "town.min_town_distance");
				minDistanceEnemy = CivSettings.getDouble(CivSettings.townConfig, "town.min_town_distance_enemy");
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				throw new CivException("Internal configuration error.");
			}
			
			for (Town town : CivGlobal.getTowns()) {
				TownHall townhall = town.getTownHall();
				if (townhall == null) {
					continue;
				}
				
				double dist = townhall.getCenterLocation().distance(new BlockCoord(player.getLocation()));
				double minDistance = minDistanceFriend;
				if (townhall.getCiv().getDiplomacyManager().atWarWith(civ)) {
					minDistance = minDistanceEnemy;
				}
								
				if (dist < minDistance) {
					DecimalFormat df = new DecimalFormat();
					throw new CivException("Cannot build town here. Too close to the town of "+town.getName()+". Distance is "+df.format(dist)+" and needs to be "+minDistance);
				}
			}
			
			//Test that we are not too close to another civ
			try {
				int min_distance = CivSettings.getInteger(CivSettings.civConfig, "civ.min_distance");
				ChunkCoord foundLocation = new ChunkCoord(loc);
				
				for (TownChunk cc : CivGlobal.getTownChunks()) {
					if (cc.getTown().getCiv() == newTown.getCiv()) {
						continue;
					}
					
					if (foundLocation.distance(cc.getChunkCoord()) <= min_distance) {
						throw new CivException("The town borders of "+cc.getTown().getName()+" are too close, cannot found town here.");
					}
				}	
			} catch (InvalidConfiguration e1) {
				e1.printStackTrace();
				throw new CivException("Internal configuration exception.");
			}		
			
			
			if (!free) {
				ConfigUnit unit = Unit.getPlayerUnit(player);
				if (unit == null || !unit.id.equals("u_settler")) {			
					throw new CivException("You must be a settler in order to found a town.");
				}
			}
			newTown.saveNow();
		
			CivGlobal.addTown(newTown);
			
			// Create permission groups for town.
			PermissionGroup residentsGroup;
			try {
				residentsGroup = new PermissionGroup(newTown, "residents");	
				residentsGroup.addMember(resident);
				residentsGroup.saveNow();
				newTown.setDefaultGroup(residentsGroup);
	
				
				PermissionGroup mayorGroup = new PermissionGroup(newTown, "mayors");
				mayorGroup.addMember(resident);
				mayorGroup.saveNow();
				newTown.setMayorGroup(mayorGroup);
							
				PermissionGroup assistantGroup = new PermissionGroup(newTown, "assistants");
				assistantGroup.saveNow();
				newTown.setAssistantGroup(assistantGroup);
			} catch (InvalidNameException e2) {
					e2.printStackTrace();
					throw new CivException("Internal naming error.");
			}
			
			ChunkCoord cl = new ChunkCoord(loc);
			TownChunk tc = new TownChunk(newTown, cl);
			tc.perms.addGroup(residentsGroup);
			try {
				newTown.addTownChunk(tc);
			} catch (AlreadyRegisteredException e1) {
				throw new CivException("Town already has this town chunk?");
			}

			tc.save();
			CivGlobal.addTownChunk(tc);			
			civ.addTown(newTown);
			
			try {
				
				Location centerLoc = loc;
				if (capitol) {
					ConfigBuildableInfo buildableInfo = CivSettings.structures.get("s_capitol");
					newTown.getTreasury().deposit(buildableInfo.cost);
					newTown.buildStructure(player, buildableInfo.id, centerLoc, resident.desiredTemplate);
				} else {
					ConfigBuildableInfo buildableInfo = CivSettings.structures.get("s_townhall");
					newTown.getTreasury().deposit(buildableInfo.cost);
					newTown.buildStructure(player, buildableInfo.id, centerLoc, resident.desiredTemplate);
				}
			} catch (CivException e) {
				civ.removeTown(newTown);
				newTown.delete();
				throw e;
			}
			
			if (!free) {
				ItemStack newStack = new ItemStack(Material.AIR);
				player.setItemInHand(newStack);	
				Unit.removeUnit(player);
			}
			
			try {	
				if (resident.getTown() != null) {
					CivMessage.sendTown(resident.getTown(), resident.getName()+" has left the town.");
					resident.getTown().removeResident(resident);
				}
				newTown.addResident(resident);
			} catch (AlreadyRegisteredException e) {
				e.printStackTrace();
				throw new CivException("Internal error, resident already registerd to this town but creating it?");
			}
			resident.saveNow();
			
			CivGlobal.processCulture();
			newTown.saveNow();
			return newTown;
		} catch (SQLException e2) {
			e2.printStackTrace();
			throw new CivException("Internal SQL Error.");
		}
	}

	public PermissionGroup getDefaultGroup() {
		return defaultGroup;
	}

	public void setDefaultGroup(PermissionGroup defaultGroup) {
		this.defaultGroup = defaultGroup;
		this.groups.put(defaultGroup.getName(), defaultGroup);
	}

	public Collection<PermissionGroup> getGroups() {
		return groups.values();
	}
	
	public PermissionGroup getGroup(String name) {
		return groups.get(name);
	}
	
	public PermissionGroup getGroupFromId(Integer id) {
		for (PermissionGroup grp : groups.values()) {
			if (grp.getId() == id) {
				return grp;
			}
		}
		return null;
	}
	
	public void addGroup(PermissionGroup grp) {
		
		if (grp.getName().equalsIgnoreCase(this.defaultGroupName)) {
			this.defaultGroup = grp;
		} else if (grp.getName().equalsIgnoreCase(this.mayorGroupName)) {
			this.mayorGroup = grp;
		} else if (grp.getName().equalsIgnoreCase(this.assistantGroupName)) {
			this.assistantGroup = grp;
		}
		
		groups.put(grp.getName(), grp);
		
	}
	
	public void removeGroup(PermissionGroup grp) {
		groups.remove(grp.getName());
	}
	
	public boolean hasGroupNamed(String name) {
		for (PermissionGroup grp : groups.values()) {
			if (grp.getName().equalsIgnoreCase(name)) {
				return true;
			}
		}
		return false;
	}
	
	public PermissionGroup getGroupByName(String name) {
		for (PermissionGroup grp : groups.values()) {
			if (grp.getName().equalsIgnoreCase(name)) {
				return grp;
			}
		}
		return null;
	}

	public String getDefaultGroupName() {
//		if (this.defaultGroup == null) {
//			return "none";
//		}
//		return this.defaultGroup.getName();
		return "residents";
	}

	public PermissionGroup getMayorGroup() {
		return mayorGroup;
	}

	public void setMayorGroup(PermissionGroup mayorGroup) {
		this.mayorGroup = mayorGroup;
		this.groups.put(mayorGroup.getName(), mayorGroup);

	}

	public PermissionGroup getAssistantGroup() {
		return assistantGroup;
	}

	public void setAssistantGroup(PermissionGroup assistantGroup) {
		this.assistantGroup = assistantGroup;
		this.groups.put(assistantGroup.getName(), assistantGroup);

	}

	public String getMayorGroupName() {
		return "mayors";
//		if (this.mayorGroup == null) {
//			return "none";
//		}
//		return this.mayorGroup.getName();
	}

	public String getAssistantGroupName() {
//		if (this.assistantGroup == null) {
//			return "none";
//		}
//		return this.assistantGroup.getName();	
		return "assistants";
	}

	public boolean isProtectedGroup(PermissionGroup grp) {
		return grp.isProtectedGroup();
	}

	public boolean playerIsInGroupName(String groupName, Player player) {
		PermissionGroup grp = this.getGroupByName(groupName);
		if (grp == null) {
			return false;
		}
		
		return grp.hasMember(player.getName());
	}

	public EconObject getTreasury() {
		return treasury;
	}
	
	public void depositDirect(double amount) {
		this.treasury.deposit(amount);
	}
	
	public void depositTaxed(double amount) {
		
		double taxAmount = amount*this.getDepositCiv().getIncomeTaxRate();
		amount -= taxAmount;
		
		if (this.getMotherCiv() != null) {
			double capturedPenalty;
			try {
				capturedPenalty = CivSettings.getDouble(CivSettings.warConfig, "war.captured_penalty");
			} catch (InvalidConfiguration e) {
				e.printStackTrace();
				return;
			}
			
			double capturePayment = amount * capturedPenalty;
			CivMessage.sendTown(this, Colors.Yellow+"Your town paid "+(amount - capturePayment)+" coins due to being captured by "+this.getCiv().getName());
			amount = capturePayment;
		}
		
		this.treasury.deposit(amount);	
		this.getDepositCiv().taxPayment(this, taxAmount);
	}

	public void withdraw(double amount)  {
		this.treasury.withdraw(amount);
	}
	
	public boolean inDebt() {
		return this.treasury.inDebt();
	}
	
	public double getDebt() {
		return this.treasury.getDebt();
	}
	
	public void setDebt(double amount) {
		this.treasury.setDebt(amount);
	}
	
	public double getBalance() {
		return this.treasury.getBalance();
	}
	
	public boolean hasEnough(double amount) {
		return this.treasury.hasEnough(amount);
	}
	
	public void setTreasury(EconObject treasury) {
		this.treasury = treasury;
	}

	public String getLevelTitle() {
		ConfigTownLevel clevel = CivSettings.townLevels.get(this.level);
		if (clevel == null) {
			return "Unknown";
		} else {
			return clevel.title;
		}
	}

	public void purchaseUpgrade(ConfigTownUpgrade upgrade) throws CivException {
		if (!this.hasUpgrade(upgrade.require_upgrade)) {
			throw new CivException("Town does not have the required upgrades to purchase this upgrade.");
		}
		
		if (!this.getTreasury().hasEnough(upgrade.cost)) {
			throw new CivException("The town does not have the required "+upgrade.cost+" coins.");
		}
		
		if (!this.hasStructure(upgrade.require_structure)) {
			throw new CivException("The town does not have the required structures to buy this upgrade.");
		}
		
		this.getTreasury().withdraw(upgrade.cost);
		
		try {
			upgrade.processAction(this);
		} catch (CivException e) {
			//Something went wrong purchasing the upgrade, refund and throw again.
			this.getTreasury().deposit(upgrade.cost);
			throw e;
		}
		
		this.upgrades.put(upgrade.id, upgrade);
		this.save();
	}

	public Structure findStructureByConfigId(String require_structure) {
		
		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equals(require_structure)) {
				return struct;
			}
		}
		
		return null;
	}

	public ConcurrentHashMap<String, ConfigTownUpgrade> getUpgrades() {
		return upgrades;
	}

	public boolean isPvp() {
		return pvp;
	}

	public void setPvp(boolean pvp) {
		this.pvp = pvp;
	}

	public String getPvpString() {
		if (!this.getCiv().getDiplomacyManager().isAtWar()) {
			if (pvp) {
				return Colors.Red+"[PvP]";
			} else {
				return Colors.Green+"[No PvP]";
			}
		} else {
			return Colors.Red+"[WAR-PvP]";
		}
	}
	
	private void kickResident(Resident resident) {
		/* Repo all this resident's plots. */
		for (TownChunk tc : townChunks.values()) {
			if (tc.perms.getOwner() == resident) {
				tc.perms.setOwner(null);
				tc.perms.replaceGroups(defaultGroup);
				tc.perms.resetPerms();
				tc.save();
			}
		}
		
		/* Clear resident's debt and remove from town. */
		resident.getTreasury().setDebt(0);
		resident.setDaysTilEvict(0);
		resident.setTown(null);
		resident.setRejoinCooldown(this);

		this.residents.remove(resident.getName().toLowerCase());
		
		resident.save();
		this.save();
	}

	public double collectPlotTax() {	
		double total = 0;
		for (Resident resident : this.residents.values()) {
			if (!resident.hasTown()) {
				CivLog.warning("Resident in town list but doesnt have a town! Resident:"+resident.getName()+" town:"+this.getName());
				continue;
			}
			
			if (resident.isTaxExempt()) {
				continue;
			}
			double tax = resident.getPropertyTaxOwed();
			boolean wasInDebt = resident.getTreasury().inDebt();

			total += resident.getTreasury().payToCreditor(this.getTreasury(), tax);
			
			if (resident.getTreasury().inDebt() && !wasInDebt) {
				resident.onEnterDebt();
			}
		}
						
		return total;
	}

	public double collectFlatTax() {
		double total = 0;
		for (Resident resident : this.residents.values()) {
			if (!resident.hasTown()) {
				CivLog.warning("Resident in town list but doesnt have a town! Resident:"+resident.getName()+" town:"+this.getName());
				continue;
			}
			
			if (resident.isTaxExempt()) {
				continue;
			}
			boolean wasInDebt = resident.getTreasury().inDebt();
			
			total += resident.getTreasury().payToCreditor(this.getTreasury(), this.getFlatTax());
			
			if (resident.getTreasury().inDebt() && !wasInDebt) {
				resident.onEnterDebt();
			}
		}
		
		return total;
	}
	public Collection<TownChunk> getTownChunks() {
		return this.townChunks.values();
	}

	public void quicksave() throws CivException {
		this.save();
	}

	public boolean isInGroup(String name, Resident resident) {
		PermissionGroup grp = this.getGroupByName(name);
		if (grp != null) {
			if (grp.hasMember(resident)) {
				return true;
			}
		}
		return false;
	}

	public TownHall getTownHall() {
		for (Structure struct : this.structures.values()) {
			if (struct instanceof TownHall) {
				return (TownHall)struct;
			}
		}
		return null;
	}
	
	public double payUpkeep() throws InvalidConfiguration {
		double upkeep = 0;
		upkeep += this.getBaseUpkeep();
		//upkeep += this.getSpreadUpkeep();
		upkeep += this.getStructureUpkeep();
		upkeep += this.getOutpostUpkeep();
		
		upkeep *= getGovernment().upkeep_rate;
		
		if (this.getBuffManager().hasBuff("buff_colossus_reduce_upkeep")) {
			upkeep = upkeep - (upkeep*this.getBuffManager().getEffectiveDouble("buff_colossus_reduce_upkeep"));
		}
		
		if (this.getBuffManager().hasBuff("debuff_colossus_leech_upkeep")) {
			double rate = this.getBuffManager().getEffectiveDouble("debuff_colossus_leech_upkeep");
			double amount = upkeep*rate;
			
			Wonder colossus = CivGlobal.getWonderByConfigId("w_colossus");
			if (colossus != null) {
				colossus.getTown().getTreasury().deposit(amount);
			} else {
				CivLog.warning("Unable to find Colossus wonder but debuff for leech upkeep was present!");
				//Colossus is "null", doesn't exist, we remove the buff in case of duplication
				this.getBuffManager().removeBuff("debuff_colossus_leech_upkeep"); 
			}
		}
		
		if (this.getTreasury().hasEnough(upkeep)) {
			this.getTreasury().withdraw(upkeep);
		} else {
			
			/* Couldn't pay the bills. Add to this town's debt, 
			 * civ may pay it later. */
			double diff = upkeep - this.getTreasury().getBalance();
			
			if (this.isCapitol()) {
				/* Capitol towns cannot be in debt, must pass debt on to civ. */
				if (this.getCiv().getTreasury().hasEnough(diff)) {
					this.getCiv().getTreasury().withdraw(diff);
				} else {
					diff -= this.getCiv().getTreasury().getBalance();
					this.getCiv().getTreasury().setBalance(0);
					this.getCiv().getTreasury().setDebt(this.getCiv().getTreasury().getDebt()+diff);
					this.getCiv().save();
				}
			} else {
				this.getTreasury().setDebt(this.getTreasury().getDebt()+diff);
			}
			this.getTreasury().withdraw(this.getTreasury().getBalance());
			
		}
		
		return upkeep;
	}

	public double getBaseUpkeep() {
		ConfigTownLevel level = CivSettings.townLevels.get(this.level);
		return level.upkeep;
	}
	
	public double getStructureUpkeep() {
		double upkeep = 0;
		
		for (Structure struct : getStructures()) {
			upkeep += struct.getUpkeepCost();
		}
		return upkeep;
	}

	public void removeResident(Resident resident) {
		this.residents.remove(resident.getName().toLowerCase());
		
		/* Remove resident from any groups. */
		for (PermissionGroup group : groups.values()) {
			if (group.hasMember(resident)) {
				group.removeMember(resident);
				group.save();
			}
		}
		
		kickResident(resident);
	}

	public boolean canClaim() {
	
		if (getMaxPlots() <= townChunks.size()) {
			return false;
		}
		
		return true;
	}

	public int getMaxPlots() {
		ConfigTownLevel lvl = CivSettings.townLevels.get(this.level);
		return lvl.plots;
	}

	public boolean hasUpgrade(String require_upgrade) {
		if (require_upgrade == null || require_upgrade.equals("")) 
			return true;
		
		return upgrades.containsKey(require_upgrade);
	}

	public boolean hasTechnology(String require_tech) {
		return this.getCiv().hasTechnology(require_tech);
	}
	
	public String getDynmapDescription() {
		String out = "";
		try {
			out += "<h3><b>"+this.getName()+"</b> (<i>"+this.getCiv().getName()+"</i>)</h3>";		
			out += "<b>Mayors: "+this.getMayorGroup().getMembersString()+"</b>";
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		return out;
	}

	public void removeCultureChunk(ChunkCoord coord) {
		this.cultureChunks.remove(coord);
	}
	
	public void removeCultureChunk(CultureChunk cc) {
		this.cultureChunks.remove(cc.getChunkCoord());
	}

	public void addCultureChunk(CultureChunk cc) {
		this.cultureChunks.put(cc.getChunkCoord(), cc);		
	}

	public int getCultureLevel() {
		
		/* Get the first level */
		int bestLevel = 0;
		ConfigCultureLevel level = CivSettings.cultureLevels.get(0);
		
		while (this.culture >= level.amount) {
			level = CivSettings.cultureLevels.get(bestLevel+1);
			if (level == null) {
				level = CivSettings.cultureLevels.get(bestLevel);
				break;
			}
			bestLevel++;
		}
		
		return level.level;
	}

	public Collection<CultureChunk> getCultureChunks() {
		return this.cultureChunks.values();
	}

	public Object getCultureChunk(ChunkCoord coord) {
		return this.cultureChunks.get(coord);
	}

	public void removeWonder(Buildable buildable) {
		if (!buildable.isComplete()) {
			this.removeBuildTask(buildable);
		}
		
		if (currentWonderInProgress == buildable) {
			currentWonderInProgress = null;
		}
		
		this.wonders.remove(buildable.getCorner());
	}
	
	public void addWonder(Buildable buildable) {
		if (buildable instanceof Wonder) {
			this.wonders.put(buildable.getCorner(), (Wonder) buildable);
		}
	}
	
	public int getStructureTypeCount(String id) {
		int count = 0;
		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equalsIgnoreCase(id)) {
				count++;
			}
		}
		return count;
	}
	
	public void giveExtraHammers(double extra) {
		if (build_tasks.size() == 0) {
			//Nothing is building, store the extra hammers for when a structure starts building.
			extraHammers = extra;
		} else {
			//Currently building structures ... divide them evenly between 
			double hammers_per_task = extra / build_tasks.size();
			double leftovers = 0.0;
			
			for (BuildAsyncTask task : build_tasks) {
				leftovers += task.setExtraHammers(hammers_per_task);	
			}
			
			extraHammers = leftovers;
		}
		this.save();
	}
	
	public void buildWonder(Player player, String id, Location center, Template tpl) throws CivException {

		if (!center.getWorld().getName().equals("world")) {
			throw new CivException("Can only build wonders in the overworld ... for now.");
		}
		
		Wonder wonder = Wonder.newWonder(center, id, this);
		
		if (!this.hasUpgrade(wonder.getRequiredUpgrade())) {
			throw new CivException("We require an upgrade we do not have yet.");
		}
		
		if (!this.hasTechnology(wonder.getRequiredTechnology())) {
			throw new CivException("We don't have the technology yet.");
		}
		
		if (!wonder.isAvailable()) {
			throw new CivException("This wonder is not currently available.");
		}
		
		wonder.canBuildHere(center, Structure.MIN_DISTANCE);
		
		if (!Wonder.isWonderAvailable(id)) {
			throw new CivException("This wonder is already built somewhere else.");
		}
		
		if (CivGlobal.isCasualMode()) {
			/* Check for a wonder already in this civ. */
			for (Town town : this.getCiv().getTowns()) {
				for (Wonder w : town.getWonders()) {
					if (w.getConfigId().equals(id)) {
						throw new CivException("Can only have one wonder of each type in your civilization in casual mode.");
					}
				}
			}
		}
		
		double cost = wonder.getCost();
		if (!this.getTreasury().hasEnough(cost)) {
			throw new CivException("Your town cannot not afford the "+cost+" coins to build "+wonder.getDisplayName());
		}
		
		wonder.runCheck(center); //Throws exception if we can't build here.	

		Buildable inProgress  = getCurrentStructureInProgress();
		if (inProgress != null) {
			throw new CivException("Your town is currently building a "+inProgress.getDisplayName()+" structure. Can only build one structure at a time.");
		} else {
			inProgress  = getCurrentWonderInProgress();
			if (inProgress != null) {
				throw new CivException("Your town is currently building "+inProgress.getDisplayName()+" and can only build one wonder at a time.");
			}
		}
		
		try {
			wonder.build(player, center, tpl);
			if (this.getExtraHammers() > 0) {
				this.giveExtraHammers(this.getExtraHammers());
			}
		} catch (Exception e) {
			if (CivGlobal.testFileFlag("debug")) {
				e.printStackTrace();
			}
			throw new CivException("Failed to build: "+e.getMessage());
		}
		
		wonders.put(wonder.getCorner(), wonder);
		
		this.getTreasury().withdraw(cost);
		CivMessage.sendTown(this, Colors.Yellow+"The town has started construction on  "+wonder.getDisplayName());
		this.save();
	}
	
	public void buildStructure(Player player, String id, Location center, Template tpl) throws CivException {

//		if (!center.getWorld().getName().equals("world")) {
//			throw new CivException("Cannot build structures in the overworld ... for now.");
//		}
		
		Structure struct = Structure.newStructure(center, id, this);
		
		if (!this.hasUpgrade(struct.getRequiredUpgrade())) {
			throw new CivException("We require an upgrade we do not have yet.");
		}
		
		if (!this.hasTechnology(struct.getRequiredTechnology())) {
			throw new CivException("We don't have the technology yet.");
		}
		
		if (!struct.isAvailable()) {
			throw new CivException("This structure is not currently available.");
		}
		
		struct.canBuildHere(center, Structure.MIN_DISTANCE);
		
		if (struct.getLimit() != 0) {
			if (getStructureTypeCount(id) >= struct.getLimit()) {
				throw new CivException("Your town can only have "+struct.getLimit()+" "+struct.getDisplayName()+" structures.");
			}
		}
		
		double cost = struct.getCost();
		if (!this.getTreasury().hasEnough(cost)) {
			throw new CivException("Your town cannot not afford the "+cost+" coins to build a "+struct.getDisplayName());
		}
		
		struct.runCheck(center); //Throws exception if we can't build here.	

		Buildable inProgress  = getCurrentStructureInProgress();
		if (inProgress != null) {
			throw new CivException("Your town is currently building a "+inProgress.getDisplayName()+" and can only build one structure at a time.");
		}
		
		try {
			/*
			 * XXX if the template is null we need to just get the template first. 
			 * This should only happen for capitols and town halls since we need to 
			 * Make them use the structure preview code and they don't yet
			 */
			if (tpl == null) {
				try {
					tpl = new Template();
					tpl.initTemplate(center, struct);
				} catch (Exception e) {
					throw e;
				}
			}
			
			struct.build(player, center, tpl);
			struct.save();
			
			// Go through and add any town chunks that were claimed to this list
			// of saved objects.
			for (TownChunk tc : struct.townChunksToSave) {
				tc.save();
			}
			struct.townChunksToSave.clear();
			
			if (this.getExtraHammers() > 0) {
				this.giveExtraHammers(this.getExtraHammers());
			}
		} catch (CivException e) {
			throw new CivException("Failed to build: "+e.getMessage());
		} catch (Exception e) {
			e.printStackTrace();
			throw new CivException("Internal Error.");
		}
				
		this.getTreasury().withdraw(cost);
		CivMessage.sendTown(this, Colors.Yellow+"The town has started construction on a "+struct.getDisplayName());
		
	//	try {
			//this.save();
			
			/* Good needs to be saved after structure to get proper structure id.*/
			if (struct instanceof TradeOutpost) {
				TradeOutpost outpost = (TradeOutpost)struct;
				if (outpost.getGood() != null) {
					outpost.getGood().save();
				}
			}
			
			//TODO fix this dependency nightmare! (the center is moved in build and needs to be resaved)
	//	} catch (SQLException e) {
	//		e.printStackTrace();
	//		throw new CivException("Internal database error");
	//	}
	}

	public boolean isStructureAddable(Structure struct) {
		int count = this.getStructureTypeCount(struct.getConfigId());

		if (struct.isTileImprovement()) {
			ConfigTownLevel level = CivSettings.townLevels.get(this.getLevel());
			if (this.getTileImprovementCount() > level.tile_improvements) {
				return false;
			}
		} else if ((struct.getLimit() != 0) && (count > struct.getLimit())) {
			return false;
		}
	
		return true;
	}
	
	public void addStructure(Structure struct) {
		this.structures.put(struct.getCorner(), struct);

		if (!isStructureAddable(struct)) {
			this.disabledBuildables.put(struct.getCorner(), struct);
			struct.setEnabled(false);
		} else {
			this.disabledBuildables.remove(struct.getCorner());
			struct.setEnabled(true);
		}
		
	}

	public Structure getStructureByType(String id) {
		for (Structure struct : this.structures.values()) {
			if (struct.getConfigId().equalsIgnoreCase(id)) {
				return struct;
			}
		}
		return null;
	}

	public void loadUpgrades() throws CivException {
		
		for (ConfigTownUpgrade upgrade : this.upgrades.values()) {
			try {
			upgrade.processAction(this);
			} catch (CivException e) {
				//Ignore any exceptions here?
				CivLog.warning("Loading upgrade generated exception:"+e.getMessage());
			}
		}
		
	}

	public Collection<Structure> getStructures() {
		return this.structures.values();
	}

	public void processUndo() throws CivException {
		if (this.lastBuildableBuilt == null) {
			throw new CivException("Cannot undo, cannot find the last thing built.");
		}
		
		if (!(this.lastBuildableBuilt instanceof Wall) &&
			 !(this.lastBuildableBuilt instanceof Road)) {
			throw new CivException("Only wall and road structures can be use build undo.");
		}
		
		this.lastBuildableBuilt.processUndo();
		this.structures.remove(this.lastBuildableBuilt.getCorner());
		removeBuildTask(lastBuildableBuilt);
		this.lastBuildableBuilt = null;
	}

	private void removeBuildTask(Buildable lastBuildableBuilt) {
		for (BuildAsyncTask task : this.build_tasks) {
			if (task.buildable == lastBuildableBuilt) {
				this.build_tasks.remove(task);
				task.abort();
				return;
			}
		}
	}

	public Structure getStructure(BlockCoord coord) {
		return structures.get(coord);
	}

	public void demolish(Structure struct, boolean isAdmin) throws CivException {
		
		if (!struct.allowDemolish() && !isAdmin) {
			throw new CivException("Cannot demolish this structure. Please re-build it instead.");	
		}
		
		try {
			struct.onDemolish();
			struct.unbindStructureBlocks();
			this.removeStructure(struct);
			struct.delete();
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException("Internal database error.");
		}
	}

	public boolean hasStructure(String require_structure) {
		if (require_structure == null || require_structure.equals("")) {
			return true;
		} 
		
		Structure struct = this.findStructureByConfigId(require_structure);
		if (struct != null && struct.isActive()) {
			return true;
		} 
		
		return false;
	}
	
	public AttrSource getGrowthRate() {
		double rate = 1.0;
		HashMap<String, Double> rates = new HashMap<String, Double>();
		
		double newRate = rate * getGovernment().growth_rate;
		rates.put("Government", newRate - rate);
		rate = newRate;
		
		/* Wonders and Goodies. */
		double additional = this.getBuffManager().getEffectiveDouble(Buff.GROWTH_RATE);
		additional += this.getBuffManager().getEffectiveDouble("buff_hanging_gardens_growth");
		
		double additionalGrapes = this.getBuffManager().getEffectiveDouble("buff_hanging_gardens_additional_growth");
		int grapeCount = 0;
		for (BonusGoodie goodie : this.getBonusGoodies()) {
			if (goodie.getDisplayName().equalsIgnoreCase("grapes")) {
				grapeCount++;
			}
		}
		
		additional += (additionalGrapes*grapeCount);
		rates.put("Wonders/Goodies", additional);
		rate += additional;
	
		return new AttrSource(rates, rate, null);
	}
	
	public AttrSource getGrowth() {
		AttrCache cache = this.attributeCache.get("GROWTH");
		if (cache == null) {
			cache = new AttrCache();
			cache.lastUpdate = new Date();
		} else {
			Date now = new Date();
			if (now.getTime() > (cache.lastUpdate.getTime() + ATTR_TIMEOUT_SECONDS*1000)) {
				cache.lastUpdate = now;
			} else {
				return cache.sources;
			}
		}
		
		double total = 0;
		HashMap<String, Double> sources = new HashMap<String, Double>();
		
		/* Grab any growth from culture. */
		double cultureSource = 0;
		for (CultureChunk cc : this.cultureChunks.values()) {
			cultureSource += cc.getGrowth();
		}
		sources.put("Culture Biomes", cultureSource);
		total += cultureSource;
		
		/* Grab any growth from structures. */
		double structures = 0;
		for (Structure struct : this.structures.values()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeBase) {
					AttributeBase as = (AttributeBase)comp;
					if (as.getString("attribute").equalsIgnoreCase("GROWTH")) {
						double h = as.getGenerated();
						structures += h;
					}
				}
			}
		}

		
		total += structures;
		sources.put("Structures", structures);
		
		sources.put("Base Growth", baseGrowth);
		total += baseGrowth;
		
		AttrSource rate = this.getGrowthRate();
		total *= rate.total;
		
		if (total < 0) {
			total = 0;
		}
		
		AttrSource as = new AttrSource(sources, total, rate);
		cache.sources = as;
		this.attributeCache.put("GROWTH", cache);
		return as;	
	}
	
	public double getCottageRate() {
		double rate = getGovernment().cottage_rate;

		double additional = rate*this.getBuffManager().getEffectiveDouble(Buff.COTTAGE_RATE);
		rate += additional;
		
		/* Adjust for happiness state. */
		rate *= this.getHappinessState().coin_rate;
		return rate;
	}

	public double getSpreadUpkeep() throws InvalidConfiguration {
		double total = 0.0;
		double grace_distance = CivSettings.getDoubleTown("town.upkeep_town_block_grace_distance");
		double base = CivSettings.getDoubleTown("town.upkeep_town_block_base");
		double falloff = CivSettings.getDoubleTown("town.upkeep_town_block_falloff");
		
		Structure townHall = this.getTownHall();
		if (townHall == null) {
			CivLog.error("No town hall for "+getName()+" while getting spread upkeep.");
			return 0.0;
		}
		
		ChunkCoord townHallChunk = new ChunkCoord(townHall.getCorner().getLocation());
		
		for (TownChunk tc : this.getTownChunks()) {
			if (tc.isOutpost()) {
				continue;
			}
			
			if (tc.getChunkCoord().equals(townHallChunk))
				continue;
			
			double distance = tc.getChunkCoord().distance(townHallChunk);
			if (distance > grace_distance) {
				distance -= grace_distance;
				double upkeep = base * Math.pow(distance, falloff);
				
				total += upkeep;
			} 
			
		}
		
		return Math.floor(total);
	}

	public double getTotalUpkeep() throws InvalidConfiguration {
		return this.getBaseUpkeep() + this.getStructureUpkeep() + this.getSpreadUpkeep() + this.getOutpostUpkeep();
	}

	public double getTradeRate() {
		double rate = getGovernment().trade_rate;
		
		/* Grab changes from any rate components. */
		double fromStructures = 0.0;
		for (Structure struct : this.structures.values()) {
			for (Component comp : struct.attachedComponents) {
				if (comp instanceof AttributeRate) {
					AttributeRate as = (AttributeRate)comp;
					if (as.getString("attribute").equalsIgnoreCase("TRADE")) {
						fromStructures += as.getGenerated();
					}
				}
			}
		}
		/* XXX TODO convert this into a 'source' rate so it can be displayed properly. */
		rate += fromStructures;
		
		double additional = rate*this.getBuffManager().getEffectiveDouble(Buff.TRADE);
		rate += additional;
		
		/* Adjust for happiness state. */
		rate *= this.getHappinessState().coin_rate;
		return rate;
	}

	public int getTileImprovementCount() {
		int count = 0;
		for (Structure struct : getStructures()) {
			if (struct.isTileImprovement()) {
				count++;
			}
		}
		return count;
	}

	public void removeTownChunk(TownChunk tc) {
		if (tc.isOutpost()) {
			this.outposts.remove(tc.getChunkCoord());
		} else {
			this.townChunks.remove(tc.getChunkCoord());
		}
	}

	public Double getHammersFromCulture() {
		double hammers = 0;
		for (CultureChunk cc : this.cultureChunks.values()) {
			hammers += cc.getHammers();
		}
		return hammers;
	}
	
	public void setBonusGoodies(ConcurrentHashMap<String, BonusGoodie> bonusGoodies) {
		this.bonusGoodies = bonusGoodies;
	}
	
	public Collection<BonusGoodie> getBonusGoodies() {
		return this.bonusGoodies.values();
	}
	
/*	public HashSet<BonusGoodie> getEffectiveBonusGoodies() {
		HashSet<BonusGoodie> returnList = new HashSet<BonusGoodie>();
		for (BonusGoodie goodie : getBonusGoodies()) {
			//CivLog.debug("hash:"+goodie.hashCode());
			if (!goodie.isStackable()) {
				boolean skip = false;
				for (BonusGoodie existing : returnList) {
					if (existing.getDisplayName().equals(goodie.getDisplayName())) {
						skip = true;
						break;
					}
				}
				
				if (skip) {
					continue;
				}
			}
		
			returnList.add(goodie);
		}		
		return returnList;
	}*/
		
	public void removeUpgrade(ConfigTownUpgrade upgrade) {
		this.upgrades.remove(upgrade.id);
	}
	
	public Structure getNearestStrucutre(Location location) {
		Structure nearest = null;
		double lowest_distance = Double.MAX_VALUE;
		
		for (Structure struct : getStructures()) {
			double distance = struct.getCenterLocation().getLocation().distance(location);
			if (distance < lowest_distance ) {
				lowest_distance = distance;
				nearest = struct;
			}
		}
		
		return nearest;
	}
	
	public Buildable getNearestStrucutreOrWonderInprogress(Location location) {
		Buildable nearest = null;
		double lowest_distance = Double.MAX_VALUE;
		
		for (Structure struct : getStructures()) {
			double distance = struct.getCenterLocation().getLocation().distance(location);
			if (distance < lowest_distance ) {
				lowest_distance = distance;
				nearest = struct;
			}
		}
		
		for (Wonder wonder : getWonders()) {
			if (wonder.isComplete()) {
				continue;
			}
			
			double distance = wonder.getCenterLocation().getLocation().distance(location);
			if (distance < lowest_distance ) {
				lowest_distance = distance;
				nearest = wonder;
			}
		}
		
		return nearest;
	}

	public void removeStructure(Structure structure) {
		if (!structure.isComplete()) {
			this.removeBuildTask(structure);
		}
		
		if (currentStructureInProgress == structure) {
			currentStructureInProgress = null;
		}
		
		this.structures.remove(structure.getCorner());
		this.invalidStructures.remove(structure);
		this.disabledBuildables.remove(structure.getCorner());
	}

	/**
	 * @return the buffManager
	 */
	public BuffManager getBuffManager() {
		return buffManager;
	}

	/**
	 * @param buffManager the buffManager to set
	 */
	public void setBuffManager(BuffManager buffManager) {
		this.buffManager = buffManager;
	}

	public void repairStructure(Structure struct) throws CivException {
		struct.repairStructure();
	}

	public void onDefeat(Civilization attackingCiv) {
		/* 
		 * We've been defeated. If we don't have our mother civilization set, this means 
		 * this is the first time this town has been conquered. 
		 */
		if (this.getMotherCiv() == null) {
			/* Save our motherland in case we ever get liberated. */
			this.setMotherCiv(this.civ);
		} else {
			/* If we've been liberated by our motherland, set things right. */
			if (this.getMotherCiv() == attackingCiv) {
				this.setMotherCiv(null);
			}
		}
		
		this.changeCiv(attackingCiv);
		this.save();
	}

	public Civilization getDepositCiv() {
		//Get the civilization we are going to deposit taxes to.
		return this.getCiv();
	}

	public Collection<Wonder> getWonders() {
		return this.wonders.values();
	}

	public void onGoodiePlaceIntoFrame(ItemFrameStorage framestore, BonusGoodie goodie) {
		TownHall townhall = this.getTownHall();
		
		if (townhall == null) {
			return;
		}
		
		for (ItemFrameStorage fs : townhall.getGoodieFrames()) {
			if (fs == framestore) {
				this.bonusGoodies.put(goodie.getOutpost().getCorner().toString(), goodie);
				for (ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
					String key = "tradegood:"+goodie.getOutpost().getCorner()+":"+cBuff.id;
					
					if (buffManager.hasBuffKey(key)) {
						continue;
					}
					
					try {
						buffManager.addBuff(key, cBuff.id, goodie.getDisplayName());
					} catch (CivException e) {
						e.printStackTrace();
					}
				}
			}
		}
		
		for (Structure struct : this.structures.values()) {
			struct.onGoodieToFrame();
		}
		
		for (Wonder wonder : this.wonders.values()) {
			wonder.onGoodieToFrame();
		}
		
	}
	
	public void loadGoodiePlaceIntoFrame(TownHall townhall, BonusGoodie goodie) {
		this.bonusGoodies.put(goodie.getOutpost().getCorner().toString(), goodie);
		for (ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
			String key = "tradegood:"+goodie.getOutpost().getCorner()+":"+cBuff.id;
			
			if (buffManager.hasBuffKey(key)) {
				continue;
			}
			
			try {
				buffManager.addBuff(key, cBuff.id, goodie.getDisplayName());
			} catch (CivException e) {
				e.printStackTrace();
			}
		}
	}
	
	public void removeGoodie(BonusGoodie goodie) {
		this.bonusGoodies.remove(goodie.getOutpost().getCorner().toString());
		for (ConfigBuff cBuff : goodie.getConfigTradeGood().buffs.values()) {
			String key = "tradegood:"+goodie.getOutpost().getCorner()+":"+cBuff.id;
			buffManager.removeBuff(key);
		}
		if (goodie.getFrame() != null) {
			goodie.getFrame().clearItem();
		}
	}
	
	public void onGoodieRemoveFromFrame(ItemFrameStorage framestore, BonusGoodie goodie) {
		TownHall townhall = this.getTownHall();
		
		if (townhall == null) {
			return;
		}
		
		for (ItemFrameStorage fs : townhall.getGoodieFrames()) {
			if (fs == framestore) {
				removeGoodie(goodie);
			}
		}
		
		for (Structure struct : this.structures.values()) {
			struct.onGoodieFromFrame();
		}
		
		for (Wonder wonder : this.wonders.values()) {
			wonder.onGoodieToFrame();
		}
	}

	public int getUnitTypeCount(String id) {
		//TODO find unit limits.
		return 0;
	}
	
	public ArrayList<ConfigUnit> getAvailableUnits() {
		ArrayList<ConfigUnit> unitList = new ArrayList<ConfigUnit>();
			
		for (ConfigUnit unit : CivSettings.units.values()) {
			if (unit.isAvailable(this)) {
				unitList.add(unit);
			}
		}
		return unitList;
	}

	public void onTechUpdate() {
		try {
			for (Structure struct : this.structures.values()) {
				if (struct.isActive()) {
					struct.onTechUpdate();
				}
			}
			
			for (Wonder wonder : this.wonders.values()) {
				if (wonder.isActive()) {
					wonder.onTechUpdate();
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			//continue in case some structure/wonder had an error.
		}
	}

	public Buildable getNearestBuildable(Location location) {
		Buildable nearest = null;
		double distance = Double.MAX_VALUE;
		
		for (Structure struct : this.structures.values()) {
			double tmp = location.distance(struct.getCenterLocation().getLocation());
			if (tmp < distance) {
				nearest = struct;
				distance = tmp;
			}
		}
		
		for (Wonder wonder : this.wonders.values()) {
			double tmp = location.distance(wonder.getCenterLocation().getLocation());
			if (tmp < distance) {
				nearest = wonder;
				distance = tmp;
			}
		}
		
		return nearest;
	}

	public boolean isCapitol() {
		if (this.getCiv().getCapitolName().equals(this.getName())) {
			return true;
		}
		return false;
	}

	public boolean isForSale() {
		if (this.getCiv().isTownsForSale()) {
			return true;
		}
		
		if (!this.inDebt()) {
			return false;
		}
		
		if (daysInDebt >= CivSettings.TOWN_DEBT_GRACE_DAYS) {
			return true;
		}
		
		return false;
	}

	public double getForSalePrice() {
		int points = this.getScore();
		try {
			double coins_per_point = CivSettings.getDouble(CivSettings.scoreConfig, "coins_per_point");
			return coins_per_point*points
