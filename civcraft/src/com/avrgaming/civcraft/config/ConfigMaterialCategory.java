package com.avrgaming.civcraft.config;

import java.util.Collection;
import java.util.HashMap;
import java.util.TreeMap;

public class ConfigMaterialCategory {
	
	private static TreeMap<String, ConfigMaterialCategory> categories = new TreeMap<String, ConfigMaterialCategory>();
	
	public String name;
	public HashMap<String, ConfigMaterial> materials = new HashMap<String, ConfigMaterial>();
	public int craftableCount = 0;
	
	public static void addMaterial(ConfigMaterial mat) {
		ConfigMaterialCategory cat = categories.get(mat.categoryCivColortripped);
		if (cat == null) {
			cat = new ConfigMaterialCategory();
			cat.name = mat.category;
		}
		
		cat.materials.put(mat.id, mat);
		if (mat.craftable) {
			cat.craftableCount++;
		}
		categories.put(mat.categoryCivColortripped, cat);
		
	}
	
	public static Collection<ConfigMaterialCategory> getCategories() {
		return categories.values();
	}

	public static ConfigMaterialCategory getCategory(String key) {
		return categories.get(key);
	}
	
}
