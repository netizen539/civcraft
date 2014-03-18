package com.avrgaming.civcraft.randomevents.components;

import org.bukkit.entity.EntityType;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.plugin.PluginManager;

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;

public class KillMobs extends RandomEventComponent implements Listener {

	private int killedMobs = 0;
	private int requireMobs = 0;
	EntityType target;
	
	@Override
	public void onStart() {
		/* Register a listener to watch for killed mobs. */
		final PluginManager pluginManager = CivCraft.getPlugin().getServer().getPluginManager();
		pluginManager.registerEvents(this, CivCraft.getPlugin());
		requireMobs = Integer.valueOf(this.getString("amount"));
		target = EntityType.valueOf(getString("what"));		
	}
	
	@Override
	public void onCleanup() {
		HandlerList.unregisterAll(this);	
	}

	@EventHandler(priority = EventPriority.MONITOR)
	public void onEntityDeath(EntityDeathEvent event) {
		if (!event.getEntityType().equals(target)) {
			return;
		}
		
		if (event.getEntity().getLastDamageCause().getCause().equals(DamageCause.ENTITY_ATTACK)) {
			killedMobs++;			
		}	
	}
	

	@Override
	public void process() {
		
	}
	
	@Override
	public boolean onCheck() { 
		if (killedMobs >= requireMobs) {
			return true;
		}
		
		return false;	
	}


}
