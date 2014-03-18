package com.avrgaming.dynmap.civcraft;

import java.util.logging.Logger;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.plugin.java.JavaPlugin;
import org.dynmap.DynmapAPI;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

public class DynmapCivcraftPlugin extends JavaPlugin {
	DynmapAPI api;
	Plugin dynmap;
	Plugin civcraft;
	MarkerSet townBorderSet;
	MarkerSet cultureSet;
	MarkerSet structureSet;
	//MarkerSet structureSet;
	
	
	MarkerAPI markerapi;
	
	
	
	
	public static final Logger log = Logger.getLogger("Minecraft");
	
	@Override
	public void onEnable() {
		log.info("enabled...");
		PluginManager pm = getServer().getPluginManager();
		
		dynmap = pm.getPlugin("dynmap");
		api = (DynmapAPI)dynmap;
		
		civcraft = pm.getPlugin("civcraft");

		markerapi = api.getMarkerAPI();
		
		townBorderSet = markerapi.createMarkerSet("townborder.markerset", "Town Borders", null, false);
		townBorderSet.setLayerPriority(10);
		townBorderSet.setHideByDefault(false);
		
		cultureSet = markerapi.createMarkerSet("townculture.markerset", "Culture", null, false);
		cultureSet.setLayerPriority(15);
		cultureSet.setHideByDefault(false);
		
		structureSet = markerapi.createMarkerSet("structures.markerset", "Structures", null, false);
		structureSet.setLayerPriority(20);
		structureSet.setHideByDefault(false);
		
		getServer().getScheduler().scheduleSyncRepeatingTask(this, 
				new CivCraftUpdateTask(this.api, this.markerapi, this.townBorderSet, this.cultureSet, this.structureSet), 40, 40);
	}
	
	@Override 
	public void onDisable() {
		log.info("disabled..");
	}
	
}
