package com.avrgaming.civcraft.config;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.inventory.Recipe;
import org.bukkit.inventory.ShapedRecipe;

import com.avrgaming.civcraft.util.ItemManager;

public class ConfigRemovedRecipes {
	public int type_id;
	public int data;
	
	
	public static void removeRecipes(FileConfiguration cfg, HashMap<Integer, ConfigRemovedRecipes> removedRecipies){
		
		List<Map<?, ?>> configMaterials = cfg.getMapList("removed_recipes");
		for (Map<?, ?> b : configMaterials) {
			ConfigRemovedRecipes item = new ConfigRemovedRecipes();
			item.type_id = (Integer)b.get("type_id");
			item.data = (Integer)b.get("data");
		
			removedRecipies.put(item.type_id, item);
			
			Iterator<Recipe> it = Bukkit.getServer().recipeIterator();
			while (it.hasNext()) {
				Recipe recipe = it.next();
				
				if (recipe instanceof ShapedRecipe) {
					ShapedRecipe shapedRecipe = (ShapedRecipe)recipe;
					if (ItemManager.getId(shapedRecipe.getResult()) == item.type_id &&
							shapedRecipe.getResult().getDurability() == (short)item.data) {
						it.remove();
						break;
					}
				}
			}
		}
	}
	
}
