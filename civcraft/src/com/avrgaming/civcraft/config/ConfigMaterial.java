package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class ConfigMaterial {

	/* Required */
	public String id;
	public int item_id;
	public int item_data;
	public String name;
	public String category = "Misc";
	public String categoryCivColortripped = category;
	public int tier;
	
	/* Optional */
	public String[] lore = null;
	public boolean craftable = false;
	public String required_tech = null;
	public boolean shaped = false;
	public HashMap<String, ConfigIngredient> ingredients;
	public String[] shape;
	public List<HashMap<String, String>> components = new LinkedList<HashMap<String, String>>();
	public boolean vanilla = false;
	public int amount = 1;
	
	@SuppressWarnings("unchecked")
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigMaterial> materials){
		materials.clear();
		List<Map<?, ?>> configMaterials = cfg.getMapList("materials");
		for (Map<?, ?> b : configMaterials) {
			ConfigMaterial mat = new ConfigMaterial();
			
			/* Manditory Settings */
			mat.id = (String)b.get("id");
			mat.item_id = (Integer)b.get("item_id");
			mat.item_data = (Integer)b.get("item_data");
			mat.name = (String)b.get("name");
			mat.name = CivColor.colorize(mat.name);
			
			String category = (String)b.get("category");
			if (category != null) {
				mat.category = CivColor.colorize(category);
				mat.categoryCivColortripped = CivColor.stripTags(category);
				
				if (mat.category.toLowerCase().contains("tier 1")) {
					mat.tier = 1;
				} else if (mat.category.toLowerCase().contains("tier 2")) {
					mat.tier = 2;
				} else if (mat.category.toLowerCase().contains("tier 3")) {
					mat.tier = 3;
				} else if (mat.category.toLowerCase().contains("tier 4")) {
					mat.tier = 4;
				} else {
					mat.tier = 0;
				}
				
			}
			
			/* Optional Lore */
			List<?> configLore = (List<?>)b.get("lore");
			if (configLore != null) {
				String[] lore = new String[configLore.size()];
				
				int i = 0;
				for (Object obj : configLore) {
					if (obj instanceof String) {
						lore[i] = (String)obj;
						i++;
					}
				}
			}
			
			Boolean craftable = (Boolean)b.get("craftable");
			if (craftable != null) {
				mat.craftable = craftable;
			}
			
			Boolean shaped = (Boolean)b.get("shaped");
			if (shaped != null) {
				mat.shaped = shaped;
			}
			
			Boolean vanilla = (Boolean)b.get("vanilla");
			if (vanilla != null) {
				mat.vanilla = vanilla;
			}
			
			Integer amount = (Integer)b.get("amount");
			if (amount != null) {
				mat.amount = amount;
			}
			
			String required_tech = (String)b.get("required_techs");
			if (required_tech != null) {
				mat.required_tech = required_tech;
			}
			
			List<Map<?,?>> comps = (List<Map<?,?>>)b.get("components");
			if (comps != null) {
				for (Map<?, ?> compObj : comps) {
					
					HashMap<String, String> compMap = new HashMap<String, String>();
					for (Object key : compObj.keySet()) {
						compMap.put((String)key, (String)compObj.get(key));
					}
					mat.components.add(compMap);	
				}
			}
			
			List<Map<?, ?>> configIngredients = (List<Map<?,?>>)b.get("ingredients");
			if (configIngredients != null) {
				mat.ingredients = new HashMap<String, ConfigIngredient>();
				
				for (Map<?, ?> ingred : configIngredients) {
					ConfigIngredient ingredient = new ConfigIngredient();
					ingredient.type_id = (Integer)ingred.get("type_id");
					ingredient.data = (Integer)ingred.get("data");
					String key = null;
					
					String custom_id = (String)ingred.get("custom_id");
					if (custom_id != null) {
						ingredient.custom_id = custom_id;
						key = custom_id;
					} else {
						ingredient.custom_id = null;
						key = "mc_"+ingredient.type_id;
					}
					
					Boolean ignore_data = (Boolean)ingred.get("ignore_data");
					if (ignore_data == null || ignore_data == false) {
						ingredient.ignore_data = false;
					} else {
						ingredient.ignore_data = true;
					}
					
					Integer count = (Integer)ingred.get("count");
					if (count != null) {
						ingredient.count = count;
					}
					
					String letter = (String)ingred.get("letter");
					if (letter != null) {
						ingredient.letter = letter;
					}
					
					
					
					mat.ingredients.put(key, ingredient);
					//ConfigIngredient.ingredientMap.put(ingredient.custom_id, ingredient);
				}
			}
			
			if (shaped) {
				/* Optional shape argument. */
				List<?> configShape = (List<?>)b.get("shape");
				
				if (configShape != null) {
					String[] shape = new String[configShape.size()];
					
					int i = 0;
					for (Object obj : configShape) {
						if (obj instanceof String) {
							shape[i] = (String)obj;
							i++;
						}
					}
					mat.shape = shape;
				}
			}
			

			/* Add to category map. */
			ConfigMaterialCategory.addMaterial(mat);
			materials.put(mat.id, mat);
		}
		
		CivLog.info("Loaded "+materials.size()+" Materials.");
	}	
	
	public boolean playerHasTechnology(Player player) {
		if (this.required_tech == null) {
			return true;
		}
		
		Resident resident = CivGlobal.getResident(player);
		if (resident == null || !resident.hasTown()) {
			return false;
		}
		
		/* Parse technoloies */
		String[] split = this.required_tech.split(",");
		for (String tech : split) {
			tech = tech.replace(" ", "");
			if (!resident.getCiv().hasTechnology(tech)) {
				return false;
			}
		}
		
		return true;	
	}
	
	public String getRequireString() {
		String out = "";
		if (this.required_tech == null) {
			return out;
		}
				
		/* Parse technoloies */
		String[] split = this.required_tech.split(",");
		for (String tech : split) {
			tech = tech.replace(" ", "");
			ConfigTech technology = CivSettings.techs.get(tech);
			if (technology != null) {
				out += technology.name+", ";
			}
		}
		
		return out;
	}
	
}
