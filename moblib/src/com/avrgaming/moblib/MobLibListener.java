package com.avrgaming.moblib;

import org.bukkit.entity.LivingEntity;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityCombustEvent;

public class MobLibListener implements Listener {
	
	@EventHandler(priority = EventPriority.LOWEST)
	public void onEntityCombust(EntityCombustEvent event) {
		if (event.getEntity() instanceof LivingEntity) {
			if (MobLib.isMobLibEntity((LivingEntity) event.getEntity())) {
				event.setCancelled(true);
			}
		}
	}
}
