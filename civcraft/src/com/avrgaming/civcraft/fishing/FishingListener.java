package com.avrgaming.civcraft.fishing;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Random;

import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigFishing;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class FishingListener implements Listener {
	
	public ArrayList<ConfigFishing> getRandomDrops() {
		Random rand = new Random();		
		ArrayList<ConfigFishing> dropped = new ArrayList<ConfigFishing>();
		
		for (ConfigFishing d : CivSettings.fishingDrops) {
			int chance = rand.nextInt(10000);
			if (chance < (d.drop_chance*10000)) {
				dropped.add(d);
			}
			
		}
		return dropped;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler(priority = EventPriority.MONITOR)
	 public void onPlayerFish (PlayerFishEvent event) {
		 if (event.getState() == PlayerFishEvent.State.CAUGHT_FISH) {
			 CivLog.debug("NOT cancelling player fish event...");
			// event.getPlayer().
			// event.setCancelled(true);
			 Player player = event.getPlayer();
			 ItemStack stack = null;
			 
			 ArrayList<ConfigFishing> dropped = getRandomDrops();
			 event.getCaught().remove();

			 if (dropped.size() == 0) {
				 stack = ItemManager.createItemStack(ItemManager.getId(Material.RAW_FISH), 1);
				 HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
				 for (ItemStack is : leftovers.values()) {
					 player.getWorld().dropItem(player.getLocation(), is);
				 }
				 CivMessage.send(event.getPlayer(), CivColor.LightGreen+"You've fished up "+CivColor.LightPurple+"Raw Fish");

			 } else {
				 for (ConfigFishing d : dropped) {
					 if (d.craftMatId == null) {
						 stack = ItemManager.createItemStack(d.type_id, 1);
						 CivMessage.send(event.getPlayer(), CivColor.LightGreen+"You've fished up "+CivColor.LightPurple+stack.getType().name().replace("_", " ").toLowerCase());	
					 } else {
						 LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(d.craftMatId);
						 if (craftMat != null) {
							 stack = LoreCraftableMaterial.spawn(craftMat);
							 CivMessage.send(event.getPlayer(), CivColor.LightGreen+"You've fished up "+CivColor.LightPurple+craftMat.getName());
						 }
					 }
					 
					 HashMap<Integer, ItemStack> leftovers = player.getInventory().addItem(stack);
					 for (ItemStack is : leftovers.values()) {
						 player.getWorld().dropItem(player.getLocation(), is);
					 }
				 }
			 }
			 
			 player.updateInventory();
		 }
	 }
}
