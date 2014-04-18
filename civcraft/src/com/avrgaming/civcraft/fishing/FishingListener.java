package com.avrgaming.civcraft.fishing;

import java.util.ArrayList;
import java.util.Random;

import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerFishEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigFishing;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
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
			 event.setCancelled(true);
			 Player player = event.getPlayer();
			 ItemStack stack = null;
			 
			 ArrayList<ConfigFishing> dropped = getRandomDrops();
			 
			 for (ConfigFishing d : dropped) {
				 if (d.craftMatId == null) {
					 stack = ItemManager.createItemStack(d.type_id, 1);
					 CivMessage.send(event.getPlayer(), CivColor.LightGreen+"You've fished up "+CivColor.LightPurple+stack.getType().name().replace("_", " ").toLowerCase());	
				 } else {
					 LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(d.craftMatId);
					 stack = LoreCraftableMaterial.spawn(craftMat);
					 CivMessage.send(event.getPlayer(), CivColor.LightGreen+"You've fished up "+CivColor.LightPurple+craftMat.getName());	
				 }
			 }
			 if (stack != null) {
				 player.getInventory().addItem(stack);
				 player.updateInventory();						
			} else {
				 stack = ItemManager.createItemStack(349, 1);
				 CivMessage.send(event.getPlayer(), CivColor.LightGreen+"You've fished up "+CivColor.LightPurple+stack.getType().name().replace("_", " ").toLowerCase());
				 player.getInventory().addItem(stack);
				 player.updateInventory();
			}			
		 }
	 }
}
