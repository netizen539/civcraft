package gpl;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import net.minecraft.server.v1_7_R4.AttributeInstance;
import net.minecraft.server.v1_7_R4.AttributeModifier;
import net.minecraft.server.v1_7_R4.EntityInsentient;
import net.minecraft.server.v1_7_R4.GenericAttributes;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.craftbukkit.v1_7_R4.entity.CraftLivingEntity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
 
/**
* HorseModifier v1.1
*
* You are free to use it, modify it and redistribute it under the condition to give credit to me
*
* @author DarkBlade12
*/
public class HorseModifier {
    private Object entityHorse;
    private Object nbtTagCompound;
    
    private static final UUID movementSpeedUID = UUID.fromString("206a89dc-ae78-4c4d-b42c-3b31db3f5a7c");
 
    /**
    * Creates a new instance of the HorseModifier, which allows you to change/get values of horses which aren't accessible with the bukkit api atm
    */
    public HorseModifier(LivingEntity horse) {
        if (!HorseModifier.isHorse(horse)) {
            throw new IllegalArgumentException("Entity has to be a horse!");
        }
        try {
            this.entityHorse = ReflectionUtil.getMethod("getHandle", horse.getClass(), 0).invoke(horse);
            this.nbtTagCompound = NBTUtil.getNBTTagCompound(entityHorse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    /**
    * Creates a new instance of the HorseModifier; This constructor is only used for the static spawn method
    */
    private HorseModifier(Object entityHorse) {
        this.entityHorse = entityHorse;
        try {
            this.nbtTagCompound = NBTUtil.getNBTTagCompound(entityHorse);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    /**
    * Spawns a horse at a given location
    */
    public static HorseModifier spawn(Location loc) {
        World w = loc.getWorld();
        try {
            Object worldServer = ReflectionUtil.getMethod("getHandle", w.getClass(), 0).invoke(w);
            Object entityHorse = ReflectionUtil.getClass("EntityHorse", worldServer);
            ReflectionUtil.getMethod("setPosition", entityHorse.getClass(), 3).invoke(entityHorse, loc.getX(), loc.getY(), loc.getZ());
            ReflectionUtil.getMethod("addEntity", worldServer.getClass(), 1).invoke(worldServer, entityHorse);
            return new HorseModifier(entityHorse);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
 
    /**
    * Checks if an entity is a horse
    */
    public static boolean isHorse(LivingEntity le) {
        try {
            Object entityLiving = ReflectionUtil.getMethod("getHandle", le.getClass(), 0).invoke(le);
            Object nbtTagCompound = NBTUtil.getNBTTagCompound(entityLiving);
            return NBTUtil.hasKeys(nbtTagCompound, new String[] { "EatingHaystack", "ChestedHorse", "HasReproduced", "Bred", "Type", "Variant", "Temper", "Tame" });
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }
 
    
    public static void setHorseSpeed(LivingEntity entity, double amount) {
    	if (!isHorse(entity)) {
    		return;
    	}
    	
    	EntityInsentient nmsEntity = (EntityInsentient) ((CraftLivingEntity) entity).getHandle();
    	AttributeInstance attributes = nmsEntity.getAttributeInstance(GenericAttributes.d);
    	AttributeModifier modifier = new AttributeModifier(movementSpeedUID, "civcraft horse movement speed", amount, 0);
    	attributes.b(modifier); //remove the modifier, adding a duplicate causes errors
    	attributes.a(modifier); //add the modifier
  
    	//done??
    }
    
    public static boolean isCivCraftHorse(LivingEntity entity) {
    	if (!isHorse(entity)) {
    		return false;
    	}
    	
    	EntityInsentient nmsEntity = (EntityInsentient) ((CraftLivingEntity) entity).getHandle();
    	AttributeInstance attributes = nmsEntity.getAttributeInstance(GenericAttributes.d);
    	
    	if (attributes.a(movementSpeedUID) == null) {
    		return false;
    	}
    		
    	return true;
    	
    //	AttributeModifier modifier = new AttributeModifier(movementSpeedUID, "civcraft horse movement speed", amount, 1);
    	//attributes.b(modifier); //remove the modifier, adding a duplicate causes errors
    //	attributes.a(modifier); //add the modifier
    }
    
    /**
    * Changes the type of the horse
    */
    public void setType(HorseType type) {
        setHorseValue("Type", type.getId());
    }
 
    /**
    * Changes whether the horse is chested or not (only for donkeys and mules)
    */
    public void setChested(boolean chested) {
        setHorseValue("ChestedHorse", chested);
    }
 
    /**
    * Changes whether the horse is eating or not
    */
    public void setEating(boolean eating) {
        setHorseValue("EatingHaystack", eating);
    }
 
    /**
    * Changes whether the horse was bred or not
    */
    public void setBred(boolean bred) {
        setHorseValue("Bred", bred);
    }
 
    /**
    * Changes the color variant of the horse (only for normal horses)
    */
    public void setVariant(HorseVariant variant) {
        setHorseValue("Variant", variant.getId());
    }
 
    /**
    * Changes the temper of the horse
    */
    public void setTemper(int temper) {
        setHorseValue("Temper", temper);
    }
 
    /**
    * Changes whether the horse is tamed or not
    */
    public void setTamed(boolean tamed) {
        setHorseValue("Tame", tamed);
    }
 
    /**
    * Changes whether the horse is saddled or not
    */
    public void setSaddled(boolean saddled) {
        setHorseValue("Saddle", saddled);
    }
 
    /**
    * Sets the armor item of the horse (only for normal horses)
    */
    public void setArmorItem(ItemStack i) {
        if (i != null) {
            try {
                Object itemTag = ReflectionUtil.getClass("NBTTagCompound", "ArmorItem");
                Object itemStack = ReflectionUtil.getMethod("asNMSCopy", Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".inventory.CraftItemStack"), 1).invoke(this, i);
                ReflectionUtil.getMethod("save", itemStack.getClass(), 1).invoke(itemStack, itemTag);
                setHorseValue("ArmorItem", itemTag);
            } catch (Exception e) {
                e.printStackTrace();
            }
        } else {
            setHorseValue("ArmorItem", null);
        }
    }
 
    /**
    * Returns the type of the horse
    */
    public HorseType getType() {
        return HorseType.fromId((int) NBTUtil.getValue(nbtTagCompound, Integer.class, "Type"));
    }
 
    /**
    * Returns whether the horse is chested or not
    */
    public boolean isChested() {
        return (boolean) NBTUtil.getValue(nbtTagCompound, Boolean.class, "ChestedHorse");
    }
 
    /**
    * Returns whether the horse is eating or not
    */
    public boolean isEating() {
        return (boolean) NBTUtil.getValue(nbtTagCompound, Boolean.class, "EatingHaystack");
    }
 
    /**
    * Returns whether the horse was bred or not
    */
    public boolean isBred() {
        return (boolean) NBTUtil.getValue(nbtTagCompound, Boolean.class, "Bred");
    }
 
    /**
    * Returns the variant of the horse
    */
    public HorseVariant getVariant() {
        return HorseVariant.fromId((int) NBTUtil.getValue(nbtTagCompound, Integer.class, "Variant"));
    }
 
    /**
    * Returns the temper of the horse
    */
    public int getTemper() {
        return (int) NBTUtil.getValue(nbtTagCompound, Integer.class, "Temper");
    }
 
    /**
    * Returns whether the horse is tamed or not
    */
    public boolean isTamed() {
        return (boolean) NBTUtil.getValue(nbtTagCompound, Boolean.class, "Tame");
    }
 
    /**
    * Returns whether the horse is saddled or not
    */
    public boolean isSaddled() {
        return (boolean) NBTUtil.getValue(nbtTagCompound, Boolean.class, "Saddle");
    }
 
    /**
    * Returns the armor item of the horse
    */
    public ItemStack getArmorItem() {
        try {
            Object itemTag = NBTUtil.getValue(nbtTagCompound, nbtTagCompound.getClass(), "ArmorItem");
            Object itemStack = ReflectionUtil.getMethod("createStack", Class.forName(ReflectionUtil.getPackageName() + ".ItemStack"), 1).invoke(this, itemTag);
            return (ItemStack) ReflectionUtil.getMethod("asCraftMirror", Class.forName(Bukkit.getServer().getClass().getPackage().getName() + ".inventory.CraftItemStack"), 1).invoke(this, itemStack);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
 
    /**
    * Opens the inventory of the horse for a player (only for tamed horses)
    */
    public void openInventory(Player p) {
        try {
            Object entityPlayer = ReflectionUtil.getMethod("getHandle", p.getClass(), 0).invoke(p);
            ReflectionUtil.getMethod("f", entityHorse.getClass(), 1).invoke(entityHorse, entityPlayer);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
 
    /**
    * Returns the horse entity
    */
    public LivingEntity getHorse() {
        try {
            return (LivingEntity) ReflectionUtil.getMethod("getBukkitEntity", entityHorse.getClass(), 0).invoke(entityHorse);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
 
    /**
    * Changes a value in the NBTTagCompound and updates it to the horse
    */
    private void setHorseValue(String key, Object value) {
        NBTUtil.setValue(nbtTagCompound, key, value);
        NBTUtil.updateNBTTagCompound(entityHorse, nbtTagCompound);
    }
 
    public enum HorseType {
 
        NORMAL("normal", 0), DONKEY("donkey", 1), MULE("mule", 2), UNDEAD("undead", 3), SKELETAL("skeletal", 4);
 
        private String name;
        private int id;
 
        HorseType(String name, int id) {
            this.name = name;
            this.id = id;
        }
 
        public String getName() {
            return name;
        }
 
        public int getId() {
            return id;
        }
 
        private static final Map<String, HorseType> NAME_MAP = new HashMap<String, HorseType>();
        private static final Map<Integer, HorseType> ID_MAP = new HashMap<Integer, HorseType>();
        static {
            for (HorseType effect : values()) {
                NAME_MAP.put(effect.name, effect);
                ID_MAP.put(effect.id, effect);
            }
        }
 
        public static HorseType fromName(String name) {
            if (name == null) {
                return null;
            }
            for (Entry<String, HorseType> e : NAME_MAP.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue();
                }
            }
            return null;
        }
 
        public static HorseType fromId(int id) {
            return ID_MAP.get(id);
        }
    }
 
    public enum HorseVariant {
        WHITE("white", 0), CREAMY("creamy", 1), CHESTNUT("chestnut", 2), BROWN("brown", 3), BLACK("black", 4), GRAY("gray", 5), DARK_BROWN("dark brown", 6), INVISIBLE("invisible", 7), WHITE_WHITE(
                "white-white", 256), CREAMY_WHITE("creamy-white", 257), CHESTNUT_WHITE("chestnut-white", 258), BROWN_WHITE("brown-white", 259), BLACK_WHITE("black-white", 260), GRAY_WHITE("gray-white", 261), DARK_BROWN_WHITE(
                "dark brown-white", 262), WHITE_WHITE_FIELD("white-white field", 512), CREAMY_WHITE_FIELD("creamy-white field", 513), CHESTNUT_WHITE_FIELD("chestnut-white field", 514), BROWN_WHITE_FIELD(
                "brown-white field", 515), BLACK_WHITE_FIELD("black-white field", 516), GRAY_WHITE_FIELD("gray-white field", 517), DARK_BROWN_WHITE_FIELD("dark brown-white field", 518), WHITE_WHITE_DOTS(
                "white-white dots", 768), CREAMY_WHITE_DOTS("creamy-white dots", 769), CHESTNUT_WHITE_DOTS("chestnut-white dots", 770), BROWN_WHITE_DOTS("brown-white dots", 771), BLACK_WHITE_DOTS(
                "black-white dots", 772), GRAY_WHITE_DOTS("gray-white dots", 773), DARK_BROWN_WHITE_DOTS("dark brown-white dots", 774), WHITE_BLACK_DOTS("white-black dots", 1024), CREAMY_BLACK_DOTS(
                "creamy-black dots", 1025), CHESTNUT_BLACK_DOTS("chestnut-black dots", 1026), BROWN_BLACK_DOTS("brown-black dots", 1027), BLACK_BLACK_DOTS("black-black dots", 1028), GRAY_BLACK_DOTS(
                "gray-black dots", 1029), DARK_BROWN_BLACK_DOTS("dark brown-black dots", 1030);
 
        private String name;
        private int id;
 
        HorseVariant(String name, int id) {
            this.name = name;
            this.id = id;
        }
 
        public String getName() {
            return name;
        }
 
        public int getId() {
            return id;
        }
 
        private static final Map<String, HorseVariant> NAME_MAP = new HashMap<String, HorseVariant>();
        private static final Map<Integer, HorseVariant> ID_MAP = new HashMap<Integer, HorseVariant>();
        static {
            for (HorseVariant effect : values()) {
                NAME_MAP.put(effect.name, effect);
                ID_MAP.put(effect.id, effect);
            }
        }
 
        public static HorseVariant fromName(String name) {
            if (name == null) {
                return null;
            }
            for (Entry<String, HorseVariant> e : NAME_MAP.entrySet()) {
                if (e.getKey().equalsIgnoreCase(name)) {
                    return e.getValue();
                }
            }
            return null;
        }
 
        public static HorseVariant fromId(int id) {
            return ID_MAP.get(id);
        }
    }
 
    private static class NBTUtil {
        public static Object getNBTTagCompound(Object entity) {
            try {
                Object nbtTagCompound = ReflectionUtil.getClass("NBTTagCompound");
                for (Method m : entity.getClass().getMethods()) {
                    Class<?>[] pt = m.getParameterTypes();
                    if (m.getName().equals("b") && pt.length == 1 && pt[0].getName().contains("NBTTagCompound")) {
                        m.invoke(entity, nbtTagCompound);
                    }
                }
                return nbtTagCompound;
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
 
        public static void updateNBTTagCompound(Object entity, Object nbtTagCompound) {
            try {
                for (Method m : entity.getClass().getMethods()) {
                    Class<?>[] pt = m.getParameterTypes();
                    if (m.getName().equals("a") && pt.length == 1 && pt[0].getName().contains("NBTTagCompound")) {
                        m.invoke(entity, nbtTagCompound);
                    }
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
 
        public static void setValue(Object nbtTagCompound, String key, Object value) {
            try {
                if (value instanceof Integer) {
                    ReflectionUtil.getMethod("setInt", nbtTagCompound.getClass(), 2).invoke(nbtTagCompound, key, (Integer) value);
                    return;
                } else if (value instanceof Boolean) {
                    ReflectionUtil.getMethod("setBoolean", nbtTagCompound.getClass(), 2).invoke(nbtTagCompound, key, (Boolean) value);
                    return;
                } else {
                    ReflectionUtil.getMethod("set", nbtTagCompound.getClass(), 2).invoke(nbtTagCompound, key, value);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
 
        public static Object getValue(Object nbtTagCompound, Class<?> c, String key) {
            try {
                if (c == Integer.class) {
                    return ReflectionUtil.getMethod("getInt", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, key);
                } else if (c == Boolean.class) {
                    return ReflectionUtil.getMethod("getBoolean", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, key);
                } else {
                    return ReflectionUtil.getMethod("getCompound", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, key);
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
        }
 
        public static boolean hasKey(Object nbtTagCompound, String key) {
            try {
                return (boolean) ReflectionUtil.getMethod("hasKey", nbtTagCompound.getClass(), 1).invoke(nbtTagCompound, key);
            } catch (Exception e) {
                e.printStackTrace();
                return false;
            }
        }
 
        public static boolean hasKeys(Object nbtTagCompound, String[] keys) {
            for (String key : keys) {
                if (!hasKey(nbtTagCompound, key)) {
                    return false;
                }
            }
            return true;
        }
    }
 
    private static class ReflectionUtil {
        public static Object getClass(String name, Object... args) throws Exception {
            Class<?> c = Class.forName(ReflectionUtil.getPackageName() + "." + name);
            int params = 0;
            if (args != null) {
                params = args.length;
            }
            for (Constructor<?> co : c.getConstructors()) {
                if (co.getParameterTypes().length == params) {
                    return co.newInstance(args);
                }
            }
            return null;
        }
 
        public static Method getMethod(String name, Class<?> c, int params) {
            for (Method m : c.getMethods()) {
                if (m.getName().equals(name) && m.getParameterTypes().length == params) {
                    return m;
                }
            }
            return null;
        }
 
        public static String getPackageName() {
            return "net.minecraft.server." + Bukkit.getServer().getClass().getPackage().getName().replace(".", ",").split(",")[3];
        }
    }
}