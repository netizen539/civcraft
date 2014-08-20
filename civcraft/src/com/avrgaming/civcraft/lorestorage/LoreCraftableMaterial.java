package com.avrgaming.civcraft.lorestorage;

import gpl.AttributeUtil;

import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.entity.ItemSpawnEvent;
import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.event.entity.PlayerLeashEntityEvent;
import org.bukkit.event.inventory.CraftItemEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.player.PlayerDropItemEvent;
import org.bukkit.event.player.PlayerInteractEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemDamageEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.event.player.PlayerPickupItemEvent;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.ShapedRecipe;
import org.bukkit.inventory.ShapelessRecipe;
import org.yaml.snakeyaml.external.biz.base64Coder.Base64Coder;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigIngredient;
import com.avrgaming.civcraft.config.ConfigMaterial;
import com.avrgaming.civcraft.items.components.ItemComponent;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.BuildableDamageBlock;
import com.avrgaming.civcraft.util.ItemManager;
import com.mysql.jdbc.StringUtils;

public class LoreCraftableMaterial extends LoreMaterial {

	private boolean craftable;
	private boolean shaped;
	
	private ConfigMaterial configMaterial;
//	public LinkedList<ItemStack> shaplessIngredientList = new LinkedList<ItemStack>();
	//public Map<Character, ItemStack> shapedIngredientList = new HashMap<Character, ItemStack>();
	
	public static HashMap<String, LoreCraftableMaterial> materials = new HashMap<String, LoreCraftableMaterial>();
	
	/*
	 * We will allow duplicate recipes with MC/materials by checking this map based
	 * on the results. The key is the material's ID as a string, so we can are only checking
	 * for custom items. The Itemstack array is the matrix for the recipe where the first
	 * 3 items represent the top row, and the last 3 represent the bottom row.
	 */
	public static HashMap<LoreCraftableMaterial, ItemStack[]> shapedRecipes = new HashMap<LoreCraftableMaterial, ItemStack[]>();
	public static HashMap<String, LoreCraftableMaterial> shapedKeys = new HashMap<String, LoreCraftableMaterial>();
	

	/* 
	 * We will allow duplicate shaped recipes by checking this map based on the results. In order
	 * for the recipe to be valid it must contain all of the item stacks and the respective amounts.
	 */
	public static HashMap<LoreCraftableMaterial, LinkedList<ItemStack>> shapelessRecipes = new HashMap<LoreCraftableMaterial, LinkedList<ItemStack>>();
	public static HashMap<String, LoreCraftableMaterial> shapelessKeys = new HashMap<String, LoreCraftableMaterial>();
	
	
	/*
	 * Components that are registered to this object.
	 */
	public HashMap<String, ItemComponent> components = new HashMap<String, ItemComponent>();
	
	public LoreCraftableMaterial(String id, int typeID, short damage) {
		super(id, typeID, damage);
	}
		
	
	public static String getShapedRecipeKey(ItemStack[] matrix) {
		String key = "";
		for (int i = 0; i < matrix.length; i++) {
			key += i+":";
			
			ItemStack stack = matrix[i];
			if (stack == null) {
				key += "null,";
				continue;
			}
			
			if (LoreMaterial.isCustom(stack)) {
				LoreMaterial loreMat = LoreMaterial.getMaterial(stack);
				key += loreMat.getId()+",";
			} else {
	//			key += "mc_"+stack.getTypeId()+"_"+stack.getDurability()+",";
				key += "mc_"+ItemManager.getId(stack)+",";

			}
		}
		
		return key;
	}
	
	public static String getShapelessRecipeKey(ItemStack[] matrix) {
		HashMap<String, Integer> counts = new HashMap<String, Integer>();
		List<String> items = new LinkedList<String>();
		
		/* Gather the counts for all the items in the matrix. */
		for (int i = 0; i < matrix.length; i++) {
			ItemStack stack = matrix[i];
			
			if (stack == null || ItemManager.getId(stack) == CivData.AIR) {
				continue;
			}
			
			String item;
			if (LoreMaterial.isCustom(stack)) {
				item = LoreMaterial.getMaterial(stack).getId();
			} else {
			//	item = "mc_"+stack.getTypeId()+"_"+stack.getDurability();
				item = "mc_"+ItemManager.getId(stack)+",";
			}
			
			Integer count = counts.get(item);
			if (count == null) {
				count = 1;
			} else {
				count++;
			}
			counts.put(item, count);
		}
		
		/* Merge the counts in to the string list. */
		for (String item : counts.keySet()) {
			Integer count = counts.get(item);
			items.add(item+":"+count);
		}
		
		/* Sort the list alphabetically so that ordering is consistent. */
		java.util.Collections.sort(items);
		
		String fullString = "";
		for (String item : items) {
			fullString += item + ",";
		}
		
		return fullString;
	}
	
	public static void buildStaticMaterials() {
		/*
		 * Loads in materials from configuration file.
		 */
		for (ConfigMaterial cfgMat : CivSettings.materials.values()) {
			LoreCraftableMaterial loreMat = new LoreCraftableMaterial(cfgMat.id, cfgMat.item_id, (short)cfgMat.item_data);
			
			loreMat.setName(cfgMat.name);
			if (cfgMat.lore != null) {
				loreMat.setLore(cfgMat.lore);
			}
			
			loreMat.setCraftable(cfgMat.craftable);
			loreMat.setShaped(cfgMat.shaped);
			loreMat.configMaterial = cfgMat;
			loreMat.buildComponents();
			
			materials.put(cfgMat.id, loreMat);
		}
	}
	
	private void buildComponents() {
		List<HashMap<String, String>> compInfoList = this.configMaterial.components;
		if (compInfoList != null) {
			for (HashMap<String, String> compInfo : compInfoList) {
				String className = "com.avrgaming.civcraft.items.components."+compInfo.get("name");
				Class<?> someClass;
				
				try {
					someClass = Class.forName(className);
					ItemComponent itemCompClass;
					itemCompClass = (ItemComponent)someClass.newInstance();
					itemCompClass.setName(compInfo.get("name"));
					
					for (String key : compInfo.keySet()) {
						itemCompClass.setAttribute(key, compInfo.get(key));
					}
					
					itemCompClass.createComponent();
					this.components.put(itemCompClass.getName(), itemCompClass);
				} catch (InstantiationException e) {
					e.printStackTrace();
				} catch (IllegalAccessException e) {
					e.printStackTrace();
				}
				catch (ClassNotFoundException e) {
					e.printStackTrace();
				}
			}
		}
	}

	public static void buildRecipes() {
		/*
		 * Loads in materials from configuration file.
		 */
		for (LoreCraftableMaterial loreMat : materials.values()) {
			if (!loreMat.isCraftable()) {
				continue;
			}
			
			ItemStack stack = LoreMaterial.spawn(loreMat);			
			ConfigMaterial configMaterial = loreMat.configMaterial;
			
			if (loreMat.isShaped()) {
				ItemStack[] matrix = new ItemStack[10];
				ShapedRecipe recipe = new ShapedRecipe(stack);
				recipe.shape(configMaterial.shape[0], configMaterial.shape[1], configMaterial.shape[2]);
				
				/* Setup the ingredients. */
				for (ConfigIngredient ingred : configMaterial.ingredients.values()) {
					ItemStack ingredStack = null;

					if (ingred.custom_id == null) {
						recipe.setIngredient(ingred.letter.charAt(0), ItemManager.getMaterialData(ingred.type_id, ingred.data));
						ingredStack = ItemManager.createItemStack(ingred.type_id, 1, (short)ingred.data);
					} else{
						LoreCraftableMaterial customLoreMat = materials.get(ingred.custom_id);
						if (customLoreMat == null) {
							CivLog.warning("Couldn't find custom material id:"+ingred.custom_id);
						}
						
						ConfigMaterial customMat = customLoreMat.configMaterial;
						if (customMat != null) {
							recipe.setIngredient(ingred.letter.charAt(0), ItemManager.getMaterialData(customMat.item_id, customMat.item_data));
						} else {
							CivLog.warning("Couldn't find custom material id:"+ingred.custom_id);
						}

						ingredStack = LoreMaterial.spawn(customLoreMat);
					}
				
					/* Add this incred to the shape. */
					int i = 0;
					for (String row : configMaterial.shape) {
						for (int c = 0; c < row.length(); c++) {
							if (row.charAt(c) == ingred.letter.charAt(0)) {
								matrix[i] = ingredStack;
							} else if (row.charAt(c) == ' '){
								matrix[i] = new ItemStack(Material.AIR, 0, (short)-1);
							}
							i++;
						}
					}
				
				}
				
				shapedRecipes.put(loreMat, matrix);
				String key = getShapedRecipeKey(matrix);
				shapedKeys.put(key, loreMat);
				

				/* Register recipe with server. */
				Bukkit.getServer().addRecipe(recipe);
			} else {
				/* Shapeless Recipe */
				ShapelessRecipe recipe = new ShapelessRecipe(stack);
				LinkedList<ItemStack> items = new LinkedList<ItemStack>();
				ItemStack[] matrix = new ItemStack[9];
				int matrixIndex = 0;
				
				/* Setup the ingredients. */
				for (ConfigIngredient ingred : configMaterial.ingredients.values()) {
					ItemStack ingredStack = null;
					
					try {
					if (ingred.custom_id == null) {
						recipe.addIngredient(ingred.count, ItemManager.getMaterialData(ingred.type_id, ingred.data));
						ingredStack = ItemManager.createItemStack(ingred.type_id, 1, (short)ingred.data);
					} else {
						LoreCraftableMaterial customLoreMat = materials.get(ingred.custom_id);
						if (customLoreMat == null) {
							CivLog.error("Couldn't configure ingredient:"+ingred.custom_id+" in config mat:"+configMaterial.id);
						}
						ConfigMaterial customMat = customLoreMat.configMaterial;
						if (customMat != null) {
							recipe.addIngredient(ingred.count, ItemManager.getMaterialData(customMat.item_id, customMat.item_data));
							ingredStack = LoreMaterial.spawn(customLoreMat);
						} else {
							CivLog.warning("Couldn't find custom material id:"+ingred.custom_id);
						}
					}
					} catch (IllegalArgumentException e) {
						CivLog.warning("Trying to process ingredient:"+ingred.type_id+":"+ingred.custom_id+" for material:"+configMaterial.id);
						throw e;
					}
					
					if (ingredStack != null) {
					//	loreMat.shaplessIngredientList.add(ingredStack);
						for (int i = 0; i < ingred.count; i++) {
							if (matrixIndex > 9) {
								break;
							}
							
							matrix[matrixIndex] = ingredStack;
							matrixIndex++;
						}
						
						ingredStack.setAmount(ingred.count);
						items.add(ingredStack);
					}
				}
				
				shapelessRecipes.put(loreMat, items);
				String key = getShapelessRecipeKey(matrix);
				shapelessKeys.put(key, loreMat);
				
				/* Register recipe with server. */
				Bukkit.getServer().addRecipe(recipe);
			}
		}
		
//		/* Configure Mercury recs */
//		CivLog.debug("setting mercury..");
//
//		LoreCraftableMaterial mercury = LoreCraftableMaterial.getCraftMaterialFromId("mat_mercury");
//		if (mercury != null) {
//			CivLog.debug("no merc?");
//			ItemStack stack = LoreCraftableMaterial.spawn(mercury);
//			stack.setAmount(4);
//			ShapelessRecipe recipe = new ShapelessRecipe(stack);
//			recipe.addIngredient(1, ItemManager.getMaterialData(CivData.FISH_RAW, CivData.PUFFERFISH));
//			Bukkit.getServer().addRecipe(recipe);
//		}
//		
//		CivLog.debug("setting mercury bath...");
//		LoreCraftableMaterial mercuryBath = LoreCraftableMaterial.getCraftMaterialFromId("mat_mercury_bath");
//		if (mercuryBath != null) {
//			CivLog.debug("no bath?!");
//			ItemStack stack = LoreCraftableMaterial.spawn(mercuryBath);
//			ShapelessRecipe recipe = new ShapelessRecipe(stack);
//			recipe.addIngredient(1, ItemManager.getMaterialData(CivData.FISH_RAW, CivData.CLOWNFISH));
//			Bukkit.getServer().addRecipe(recipe);
//		}
	}
	
	
	@Override
	public void onHit(EntityDamageByEntityEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInteract(PlayerInteractEvent event) {
		for (ItemComponent ic : this.components.values()) {
			ic.onInteract(event);
		}
	}

	@Override
	public void onInteractEntity(PlayerInteractEntityEvent event) {
	}

	@Override
	public void onBlockPlaced(BlockPlaceEvent event) {
		boolean allow = false;
		for (ItemComponent ic : this.components.values()) {
			allow = ic.onBlockPlaced(event);
		}
		
		if (!allow) {
			event.setCancelled(true);
		}
	}

	@Override
	public void onBlockBreak(BlockBreakEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBlockDamage(BlockDamageEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onBlockInteract(PlayerInteractEvent event) {
		//event.setCancelled(true);
	}

	@Override
	public void onHold(PlayerItemHeldEvent event) {
		for (ItemComponent comp : this.components.values()) {
			comp.onHold(event);
		} 
	}

	@Override
	public void onDrop(PlayerDropItemEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onItemDrop(PlayerDropItemEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onItemCraft(CraftItemEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onItemPickup(PlayerPickupItemEvent event) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onItemSpawn(ItemSpawnEvent event) {
		for (ItemComponent comp : this.components.values()) {
			comp.onItemSpawn(event);
		}
		
	}

	@Override
	public boolean onAttack(EntityDamageByEntityEvent event, ItemStack stack) {
		for (ItemComponent comp : this.components.values()) {
			comp.onAttack(event, stack);
		}
		return false;
	}

	@Override
	public void onInvItemPickup(InventoryClickEvent event, Inventory fromInv,
			ItemStack stack) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInvItemDrop(InventoryClickEvent event, Inventory toInv,
			ItemStack stack) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInvShiftClick(InventoryClickEvent event, Inventory fromInv,
			Inventory toInv, ItemStack stack) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInvItemSwap(InventoryClickEvent event, Inventory toInv,
			ItemStack droppedStack, ItemStack pickedStack) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onPlayerDeath(EntityDeathEvent event, ItemStack stack) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void onInventoryClose(InventoryCloseEvent event) {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public int onStructureBlockBreak(BuildableDamageBlock sb, int damage) {
		for (ItemComponent component : this.components.values()) {
			damage = component.onStructureBlockBreak(sb, damage);
		}

		return damage;
	}

	
	@Override
	public void applyAttributes(AttributeUtil attrUtil) {
		for (ItemComponent comp : this.components.values()) {
			comp.onPrepareCreate(attrUtil);
		}
		return;
	}

	public boolean isCraftable() {
		return craftable;
	}

	public void setCraftable(boolean craftable) {
		this.craftable = craftable;
	}

	public boolean isShaped() {
		return shaped;
	}

	public void setShaped(boolean shaped) {
		this.shaped = shaped;
	}
	
	public ConfigMaterial getConfigMaterial() {
		return this.configMaterial;
	}
	
	public String getConfigId() {
		return this.configMaterial.id;
	}
	
	@Override
	public int hashCode() {
		return this.configMaterial.id.hashCode();
	}
	
	public Collection<ItemComponent> getComponents() {
		return this.components.values();
	}
	
	public void addComponent(ItemComponent itemComp) {
		this.components.put(itemComp.getName(), itemComp);
	}

	@Override
	public void onDefense(EntityDamageByEntityEvent event, ItemStack stack) {
		/* Search components for defense value. */
		for (ItemComponent comp : this.components.values()) {
			comp.onDefense(event, stack);
		}
	}

	public void onItemDurabilityChange(PlayerItemDamageEvent event) {
		for (ItemComponent comp : this.components.values()) {
			comp.onDurabilityChange(event);
		}
	}
	
	public static LoreCraftableMaterial getCraftMaterial(ItemStack stack) {
		if (stack == null) {
			return null;
		}
		
		LoreMaterial mat = materialMap.get(getMID(stack));
		if (mat instanceof LoreCraftableMaterial) {
			return (LoreCraftableMaterial)mat;
		}
		
		return null;
	}


	public boolean hasComponent(String string) {
		return this.components.containsKey(string);
	}


	public static LoreCraftableMaterial getCraftMaterialFromId(String mid) {
		LoreMaterial mat = materialMap.get(mid);
		if (mat instanceof LoreCraftableMaterial) {
			return (LoreCraftableMaterial)mat;
		}
		return null;
	}


	public ItemComponent getComponent(String string) {
		return this.components.get(string);
	}


	public void onPlayerInteractEntityEvent(PlayerInteractEntityEvent event) {
		/* Search components for defense value. */
		for (ItemComponent comp : this.components.values()) {
			comp.onPlayerInteractEntity(event);
		}
	}


	public void onPlayerLeashEvent(PlayerLeashEntityEvent event) {
		for (ItemComponent comp : this.components.values()) {
			comp.onPlayerLeashEvent(event);
		}
	}


	public void onRangedAttack(EntityDamageByEntityEvent event, ItemStack inHand) {
		for (ItemComponent comp : this.components.values()) {
			comp.onRangedAttack(event, inHand);
		}
	}


	public ItemChangeResult onDurabilityDeath(PlayerDeathEvent event, ItemStack stack) {
		
//		ItemChangeResult result = new ItemChangeResult();
//		result.stack = stack;
//		result.destroyItem = false;
		
		ItemChangeResult result = null;
		for (ItemComponent comp : this.components.values()) {
			result = comp.onDurabilityDeath(event, result, stack);
		}
		return result;
	}

	@Override
	public void onInventoryOpen(InventoryOpenEvent event, ItemStack stack) {
		for (ItemComponent comp : this.components.values()) {
			comp.onInventoryOpen(event, stack);
		}
	}
	
	
	public boolean isVanilla() {
		return this.configMaterial.vanilla;
	}


	public int getCraftAmount() {
		return this.configMaterial.amount;
	}


	public void rebuildLore() {
		
	}


	public static String serializeEnhancements(ItemStack stack) {
		String out = "";
		
		for (LoreEnhancement enh : LoreCraftableMaterial.getEnhancements(stack)) {
			out += enh.getClass().getName()+"@"+enh.serialize(stack)+",";
		}

		String outEncoded = new String(Base64Coder.encode(out.getBytes())); 
		return outEncoded;
	}

	public static ItemStack deserializeEnhancements(ItemStack stack, String serial) {
		String in = StringUtils.toAsciiString(Base64Coder.decode(serial));
		String[] enhancementsStrs = in.split(",");
		
		for (String enhString : enhancementsStrs) {
			String[] split = enhString.split("@");
			String className = split[0];
			String data = "";
			if (split.length > 1) {
				data = split[1];
			}

			try {
				Class<?> cls = Class.forName(className);
				LoreEnhancement enh = (LoreEnhancement)cls.newInstance();
				AttributeUtil attrs = new AttributeUtil(stack);
				attrs.addEnhancement(cls.getSimpleName(), null, null);
				stack = attrs.getStack();
				stack = enh.deserialize(stack, data);
			} catch (ClassNotFoundException | InstantiationException | IllegalAccessException e) {
				e.printStackTrace();
			}
			
		}
		
		return stack;
	}


	
//	public void onInteract(PlayerInteractEvent event) {
//		
//	}
//	public abstract void onInteractEntity(PlayerInteractEntityEvent event);
//	public abstract void onBlockPlaced(BlockPlaceEvent event);
	
}
