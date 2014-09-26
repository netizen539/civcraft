package com.avrgaming.civcraft.mobs;

import java.lang.reflect.Field;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Random;
import java.util.UUID;

import net.minecraft.server.v1_7_R4.DamageSource;
import net.minecraft.server.v1_7_R4.Entity;
import net.minecraft.server.v1_7_R4.EntityCreature;
import net.minecraft.server.v1_7_R4.EntityInsentient;
import net.minecraft.server.v1_7_R4.EntityLiving;
import net.minecraft.server.v1_7_R4.GenericAttributes;
import net.minecraft.server.v1_7_R4.PathfinderGoalSelector;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Biome;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftEntity;
import org.bukkit.craftbukkit.v1_7_R4.util.UnsafeList;
import org.bukkit.entity.ExperienceOrb;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityTargetEvent;
import org.bukkit.event.entity.EntityTargetEvent.TargetReason;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobLevel;
import com.avrgaming.civcraft.mobs.MobSpawner.CustomMobType;
import com.avrgaming.civcraft.mobs.components.MobComponent;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.mob.ICustomMob;
import com.avrgaming.mob.ISpawnable;

public abstract class CommonCustomMob implements ICustomMob {

	public static HashMap<UUID, CommonCustomMob> customMobs = new HashMap<UUID, CommonCustomMob>();
	public static HashMap<String, LinkedList<TypeLevel>> biomes = new HashMap<String, LinkedList<TypeLevel>>();
	public static HashSet<String> disabledMobs = new HashSet<String>();
	
	private CustomMobType type;
	private CustomMobLevel level;
	public EntityLiving entity;
	
	public HashMap<String, String> dataMap = new HashMap<String, String>();
	public HashMap<String, MobComponent> components = new HashMap<String, MobComponent>();
	public LinkedList<MobDrop> drops = new LinkedList<MobDrop>();
	
	/* Anti-trap stuff */
	//private static int 
	private String targetName;
	private Location lastLocation;
	
	private int coinMin = 0;
	private int coinMax = 0;
	
	public void setName(String name) {
		((EntityInsentient) entity).setCustomName(name);
		((EntityInsentient) entity).setCustomNameVisible(true);
	}
	
	public PathfinderGoalSelector getGoalSelector() {
		if (entity == null) {
			return null;
		}
		
		Field gsa;
		try {
			gsa = net.minecraft.server.v1_7_R4.EntityInsentient.class.getDeclaredField("goalSelector");
			gsa.setAccessible(true);
			return (PathfinderGoalSelector)gsa.get((EntityInsentient)entity);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public PathfinderGoalSelector getTargetSelector() {
		Field gsa;
		try {
			gsa = net.minecraft.server.v1_7_R4.EntityInsentient.class.getDeclaredField("targetSelector");
			gsa.setAccessible(true);
			return (PathfinderGoalSelector)gsa.get((EntityInsentient)entity);
		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	protected void initLevelAndType() {
		this.setType(MobSpawner.CustomMobType.valueOf(getData("type")));
		this.setLevel(MobSpawner.CustomMobLevel.valueOf(getData("level")));
				
		/* common drops. */
		this.addVanillaDrop(ItemManager.getId(Material.BONE), (short)0, 0.1);
		this.addVanillaDrop(ItemManager.getId(Material.SUGAR), (short)0, 0.1);
		this.addVanillaDrop(ItemManager.getId(Material.SULPHUR), (short)0, 0.25);
		this.addVanillaDrop(ItemManager.getId(Material.POTATO_ITEM), (short)0, 0.1);
		this.addVanillaDrop(ItemManager.getId(Material.CARROT_ITEM), (short)0, 0.1);
		this.addVanillaDrop(ItemManager.getId(Material.COAL), (short)0, 0.1);
		this.addVanillaDrop(ItemManager.getId(Material.STRING), (short)0, 0.1);
		this.addVanillaDrop(ItemManager.getId(Material.SLIME_BALL), (short)0, 0.02);

	}
	
	public Location getLocation(EntityLiving entity2) {
		World world = Bukkit.getWorld(entity2.world.getWorld().getName());
		Location loc = new Location(world, entity2.locX, entity2.locY, entity2.locZ);
		return loc;
	}
	
	public void printGoals(PathfinderGoalSelector goals) {
		System.out.println("Printing goals:");
	    Field gsa;
		try {
			gsa = net.minecraft.server.v1_7_R4.PathfinderGoalSelector.class.getDeclaredField("b");
			gsa.setAccessible(true);
			//PathfinderGoalSelectorItem item;
			UnsafeList<?> list = (UnsafeList<?>) gsa.get(goals);
			for (Object obj : list) {
				System.out.println("Obj:"+obj.toString());
			}

		} catch (NoSuchFieldException e) {
			e.printStackTrace();
		} catch (SecurityException e) {
			e.printStackTrace();
		} catch (IllegalArgumentException e) {
			e.printStackTrace();
		} catch (IllegalAccessException e) {
			e.printStackTrace();
		}
		
		
	}

	@Override
	public String getBaseEntity() {
		return null;
	}

	public void onCreate() {
	}

	public void onCreateAttributes() {
	
	}

	public void onDamage(EntityCreature e, DamageSource damagesource, PathfinderGoalSelector goalSelector, PathfinderGoalSelector targetSelector) {

		
	}

	@Override
	public void onDeath(EntityCreature arg0) {
		dropItems();
	}

	public void onRangedAttack(Entity arg1) {
		
	}
	
	private void checkForStuck() {
		if (this.targetName != null && this.lastLocation != null) {
			Location loc = getLocation(entity);
			if (loc.distance(this.lastLocation) < 0.5) {
				Player player;
				try {
					player = CivGlobal.getPlayer(this.targetName);
					entity.getBukkitEntity().teleport(player.getLocation());
				} catch (CivException e) {
					/* This player is no longer online. Lose target. */
					this.targetName = null;
					this.lastLocation = null;
				}			
			}
			this.lastLocation = loc;
		}
	}
	
	private void checkForTownBorders() {
		Location loc = getLocation(entity);
		TownChunk tc = CivGlobal.getTownChunk(loc);
		if (tc != null) {
			entity.getBukkitEntity().remove();
		}
		
		Camp camp = CivGlobal.getCampFromChunk(new ChunkCoord(loc));
		if (camp != null) {
			entity.getBukkitEntity().remove();
		}
	}
	
	private void checkForisWarTime() {
		if (War.isWarTime()) {
			entity.getBukkitEntity().remove();
		}
	}

	private int tickCount = 0;
	@Override
	public void onTick() {
		if (entity == null) {
			return;
		}
		
		tickCount++;
		if (tickCount > 90) {
			checkForStuck();
			checkForTownBorders();
			checkForisWarTime();
			tickCount = 0;
		}
	}

	public void setData(String key, String value) {
		dataMap.put(key, value);
	}
	
	
	public String getData(String key) {
		return dataMap.get(key);
	}

	@Override
	public void setEntity(EntityLiving e) {
		this.entity = e;
	}

	public Collection<MobComponent> getMobComponents() {
		return this.components.values();
	}

	public void addComponent(MobComponent comp) {
		this.components.put(comp.getClass().getName(), comp);
	}
	
	public static CommonCustomMob getCCM(Entity e) {
		if (!(e instanceof ISpawnable)) {
			return null;
		}
		
		ISpawnable spawn = (ISpawnable)e;
		return (CommonCustomMob)spawn.getCustomMobInterface();
	}
	
	public static CommonCustomMob getCCM(org.bukkit.entity.Entity entity) {
		Entity e = ((CraftEntity)entity).getHandle();
		return getCCM(e);
	}
	
	
	public void setAttack(double attack) {
		entity.getAttributeInstance(GenericAttributes.e).setValue(attack);
	}
	
	public void setMovementSpeed(double speed) {
		entity.getAttributeInstance(GenericAttributes.d).setValue(speed);
	}
	
	public void setFollowRange(double range) {
		entity.getAttributeInstance(GenericAttributes.b).setValue(range);
	}
	
	public double getFollowRange() {
		double value;
		try {
			value = entity.getAttributeInstance(GenericAttributes.b).getValue();
		} catch (NullPointerException e) {
			value = 32.0D;
		}
		
		return value;
	}
	
	public void modifySpeed(double percent) {
		double speed = entity.getAttributeInstance(GenericAttributes.d).getValue();
		speed *= percent;
		setMovementSpeed(speed);
	}
	
	public void setMaxHealth(double health) {
		entity.getAttributeInstance(GenericAttributes.maxHealth).setValue(health);
		entity.setHealth((float) health);
	}
	
	public void setKnockbackResistance(double resist) {
		entity.getAttributeInstance(GenericAttributes.c).setValue(resist);
	}
	

	
	protected void printAttributes() {
		try {
			if (entity == null) {
				CivLog.info("Entity was null!");
			}
			CivLog.info("Speed:"+entity.getAttributeInstance(GenericAttributes.d).getValue());
			CivLog.info("MaxHealth:"+entity.getAttributeInstance(GenericAttributes.maxHealth).getValue()+" Health:"+entity.getHealth());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	@Override
	public String getSaveString() {
		return this.getData("type")+":"+this.getData("level");
	}

	@Override
	public void loadSaveString(String str) {
		String[] split = str.split(":");
		this.setData("type", split[0]);
		this.setData("level", split[1]);
		if (this.entity == null) {
			return;
		}
		
		this.onCreate();
		this.onCreateAttributes();
	}

	@Override
	public abstract String getClassName();
	
	public static void setValidBiome(CustomMobType type, CustomMobLevel level, Biome biome) {
		LinkedList<TypeLevel> mobs = biomes.get(biome);
		if (mobs == null) {
			mobs = new LinkedList<TypeLevel>();
		}
		
		mobs.add(new TypeLevel(type, level));
		biomes.put(biome.name(), mobs);
	}
	
	public static LinkedList<TypeLevel> getValidMobsForBiome(Biome biome) {
		LinkedList<TypeLevel> mobs = biomes.get(biome.name());
		if (mobs == null) {
			mobs = new LinkedList<TypeLevel>();
		}
		
		return mobs;
	}

	public void onTarget(EntityTargetEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		if ((event.getReason().equals(TargetReason.CLOSEST_PLAYER) ||
				event.getReason().equals(TargetReason.OWNER_ATTACKED_TARGET)) &&
				(event.getTarget() instanceof Player)) {
			
			double followRange = this.getFollowRange();
			double distance = event.getEntity().getLocation().distance(event.getTarget().getLocation());
			if ((distance-0.5) <= followRange) {
				this.targetName = ((Player)event.getTarget()).getName();
				this.lastLocation = event.getEntity().getLocation();
			}
		} else {
			this.targetName = null;
			this.lastLocation = null;
		}
	}

	public void addVanillaDrop(int type, short data, double chance) {
		MobDrop drop = new MobDrop();
		drop.isVanillaDrop = true;
		drop.vanillaType = type;
		drop.vanillaData = data;
		drop.chance = chance;
		
		this.drops.add(drop);
	}
	
	public void addDrop(String craftMatId, double chance) {
		MobDrop drop = new MobDrop();
		drop.isVanillaDrop = false;
		drop.craftMatId = craftMatId;
		drop.chance = chance;
		//drop.maxAmount = max;
	//	drop.minAmount = min;
		
		this.drops.add(drop);
	}
	
	public LinkedList<MobDrop> getRandomDrops() {
		Random rand = new Random();
		LinkedList<MobDrop> dropped = new LinkedList<MobDrop>();
		
		for (MobDrop d : drops) {
			int chance = rand.nextInt(1000);
			if (chance < (d.chance*1000)) {
				/* Dropping this item! */
				dropped.add(d);
			}
		}
		
		return dropped;
	}
	
	public void dropItems() {
		try {
			if (entity == null) {
				return;
			}
			
			LinkedList<MobDrop> dropped = getRandomDrops();
			World world = entity.getBukkitEntity().getWorld();
			Location loc = getLocation(entity);
			
			for (MobDrop d : dropped) {
				ItemStack stack;
				if (d.isVanillaDrop) {
					stack = ItemManager.createItemStack(d.vanillaType, 1, d.vanillaData);
				} else {
					LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(d.craftMatId);
					stack = LoreCraftableMaterial.spawn(craftMat);
				}
				
				world.dropItem(loc, stack);
			}
			
			if (this.coinMax != 0 && this.coinMin != 0) {
				ExperienceOrb orb = (ExperienceOrb)world.spawn(loc, ExperienceOrb.class);
				Random random = new Random();
				int coins = random.nextInt(this.coinMax - this.coinMin) + this.coinMin;
				orb.setExperience(coins);

			}
		} catch(Exception e) {
			e.printStackTrace();
		}
	}
	
	public void coinDrop(int min, int max) {
		this.coinMin = min;
		this.coinMax = max;
	}

	public CustomMobLevel getLevel() {
		if (level == null) {
			/* re-init mob if it was unloaded or something. */
			//initLevelAndType();
			//onCreate();
			//onCreateAttributes();
			CivLog.warning("This mob was unloaded?!");
		}
		return level;
	}

	public void setLevel(CustomMobLevel level) {
		this.level = level;
	}

	public CustomMobType getType() {
		return type;
	}

	public void setType(CustomMobType type) {
		if (type == null) {
			/* re-init mob if it was unloaded or something. */
			initLevelAndType();
			onCreate();
			onCreateAttributes();
		}
		this.type = type;
	}
	
}
