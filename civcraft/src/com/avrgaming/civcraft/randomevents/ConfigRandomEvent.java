package com.avrgaming.civcraft.randomevents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.bukkit.configuration.file.FileConfiguration;

import com.avrgaming.civcraft.main.CivLog;

public class ConfigRandomEvent {

	public String id;
	public String name;
	public LinkedList<String> message = new LinkedList<String>();
	public int length;
	
	/* Components */
	public LinkedList<HashMap<String, String>> actions = new LinkedList<HashMap<String, String>>();
	public LinkedList<HashMap<String, String>> requirements = new LinkedList<HashMap<String, String>>();
	public LinkedList<HashMap<String, String>> success = new LinkedList<HashMap<String, String>>();
	public LinkedList<HashMap<String, String>> failure = new LinkedList<HashMap<String, String>>();
	public int chance = 0;

	
	private static void loadComponentConfig(Map<?, ?> obj, LinkedList<HashMap<String, String>> component, String configName) {
		@SuppressWarnings("unchecked")
		List<Map<?, ?>> comps = (List<Map<?, ?>>) obj.get(configName);
		if (comps != null) {
			for (Map<?, ?> compObj : comps) {
				
				HashMap<String, String> compMap = new HashMap<String, String>();
				for (Object key : compObj.keySet()) {
					compMap.put((String)key, (String)compObj.get(key));
				}
		
				component.add(compMap);	
			}
		}
	}
	
	public static void loadConfig(FileConfiguration cfg, Map<String, ConfigRandomEvent> randomEvents, ArrayList<String> eventIDs){
		randomEvents.clear();
		List<Map<?, ?>> ConfigRandomEvent = cfg.getMapList("random_events");
		for (Map<?, ?> obj : ConfigRandomEvent) {
			
			ConfigRandomEvent event = new ConfigRandomEvent();
			event.id = (String)obj.get("id");
			event.name = (String)obj.get("name");
			event.length = (Integer)obj.get("length");
			event.chance = (Integer)obj.get("chance");

			List<?> messageList = (List<?>)obj.get("message");
			for (Object str : messageList) {
				if (str instanceof String) {
					event.message.add((String)str);
				}
			}
			
			/* Get components. */
			loadComponentConfig(obj, event.actions, "actions");
			loadComponentConfig(obj, event.requirements, "requirements");
			loadComponentConfig(obj, event.success, "success");
			loadComponentConfig(obj, event.failure, "failure");

					
			randomEvents.put(event.id, event);
			eventIDs.add(event.id);
		}
		
		CivLog.info("Loaded "+randomEvents.size()+" Random Events.");
	}
}
