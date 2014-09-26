package com.avrgaming.civcraft.loregui;

import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;

public class OpenInventoryTask implements Runnable {

	public Inventory inv;
	public Player player;
	
	public OpenInventoryTask(Player player, Inventory inv) {
		this.inv = inv;
		this.player = player;
	}
	
	@Override
	public void run() {
		player.openInventory(inv);
	}

	
}
