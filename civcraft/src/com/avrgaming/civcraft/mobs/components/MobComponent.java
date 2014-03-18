package com.avrgaming.civcraft.mobs.components;

import org.bukkit.entity.Entity;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.avrgaming.civcraft.mobs.CommonCustomMob;

public class MobComponent {

	public void onDefense(EntityDamageByEntityEvent event) {}

	public static void onDefense(Entity entity, EntityDamageByEntityEvent event) {
		CommonCustomMob custom = CommonCustomMob.customMobs.get(entity.getUniqueId());
		if (custom != null) {
			for (MobComponent comp : custom.getMobComponents()) {
				comp.onDefense(event);
			}
		}
	}
}
