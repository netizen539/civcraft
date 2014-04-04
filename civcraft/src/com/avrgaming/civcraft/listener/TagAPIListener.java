package com.avrgaming.civcraft.listener;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.kitteh.tag.AsyncPlayerReceiveNameTagEvent;

import com.avrgaming.civcraft.main.CivGlobal;

public class TagAPIListener implements Listener {
	@EventHandler(priority = EventPriority.HIGHEST)
	public void onNameTag(AsyncPlayerReceiveNameTagEvent event) {
		event.setTag(CivGlobal.updateTag(event.getNamedPlayer(), event.getPlayer()));
	}
}
