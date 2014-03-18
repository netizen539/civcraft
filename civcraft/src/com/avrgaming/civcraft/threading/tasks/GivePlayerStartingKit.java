/*************************************************************************
 * 
 * AVRGAMING LLC
 * __________________
 * 
 *  [2013] AVRGAMING LLC
 *  All Rights Reserved.
 * 
 * NOTICE:  All information contained herein is, and remains
 * the property of AVRGAMING LLC and its suppliers,
 * if any.  The intellectual and technical concepts contained
 * herein are proprietary to AVRGAMING LLC
 * and its suppliers and may be covered by U.S. and Foreign Patents,
 * patents in process, and are protected by trade secret or copyright law.
 * Dissemination of this information or reproduction of this material
 * is strictly forbidden unless prior written permission is obtained
 * from AVRGAMING LLC.
 */
package com.avrgaming.civcraft.threading.tasks;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.ItemManager;

public class GivePlayerStartingKit implements Runnable {

	public String name;
	
	public GivePlayerStartingKit(String name) {
		this.name = name;
	}
	
	@Override
	public void run() {
		try {
			Player player = CivGlobal.getPlayer(name);
			
			for (String kitItems : CivSettings.kitItems) {
				String[] split = kitItems.split(":");
				
				ItemStack stack;
				try {
					Integer type = Integer.valueOf(split[0]);
					Integer amount = Integer.valueOf(split[1]);

					stack = ItemManager.createItemStack(type, amount);

				} catch (NumberFormatException e) {
					String customMatID = split[0];
					LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(customMatID);
					if (craftMat == null) {
						CivLog.warning("Couldn't find custom material:"+customMatID+" to give to player on first join.");
						continue;
					}
					
					stack = LoreCraftableMaterial.spawn(craftMat);
				}
				
				player.getInventory().addItem(stack);
			}
			
			Resident resident = CivGlobal.getResident(name);
			if (resident != null) {
				resident.getTreasury().deposit(CivSettings.startingCoins);
				resident.setGivenKit(true);
			}
			
		
		} catch (CivException e) {
		//	e.printStackTrace();
			CivLog.warning("Tried to give starting kit to offline player:"+name);
			return;
		}
		
	}

}
