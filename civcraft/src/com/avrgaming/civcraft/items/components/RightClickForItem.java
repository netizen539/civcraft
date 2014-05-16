package com.avrgaming.civcraft.items.components;


import gpl.AttributeUtil;

import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.lorestorage.LoreCraftableMaterial;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

public class RightClickForItem extends ItemComponent {
	
	@Override
	public void onPrepareCreate(AttributeUtil attrUtil) {
		String amount = this.getString("amount");
		String mat_id = this.getString("custom_id");
		
		LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(mat_id);
		attrUtil.addLore("Right click for "+amount+" "+craftMat.getName());
	}
	
	
	public void onInteract(PlayerInteractEvent event) {
		CivMessage.send(event.getPlayer(), CivColor.Rose+"Disabled for now...");
//		if (event.getAction().equals(Action.RIGHT_CLICK_AIR) || event.getAction().equals(Action.RIGHT_CLICK_BLOCK)) {
//			String amount = this.getString("amount");
//			String mat_id = this.getString("custom_id");
//			
//			LoreCraftableMaterial craftMat = LoreCraftableMaterial.getCraftMaterialFromId(mat_id);
//			ItemStack stack = LoreCraftableMaterial.spawn(craftMat);
//			stack.setAmount(Integer.valueOf(amount));
//
//			int count = 0;
//			LoreCraftableMaterial sourceMat = LoreCraftableMaterial.getCraftMaterial(event.getPlayer().getItemInHand());
//			for (ItemStack s : event.getPlayer().getInventory()) {
//				LoreCraftableMaterial invMat = LoreCraftableMaterial.getCraftMaterial(s);
//				if (invMat == null) {
//					continue;
//				}
//				
//				if (invMat.getId().equals(sourceMat.getId())) {
//					count++;
//				}
//			}
//			
//			if (event.getPlayer().getItemInHand().getAmount() <= 1) {
//				event.getPlayer().getInventory().removeItem(event.getPlayer().getItemInHand());
//			} else {
//				event.getPlayer().getItemInHand().setAmount(event.getPlayer().getItemInHand().getAmount()-1);
//			}
//
//			int count2 = 0;
//			for (ItemStack s : event.getPlayer().getInventory()) {
//				LoreCraftableMaterial invMat = LoreCraftableMaterial.getCraftMaterial(s);
//				if (invMat == null) {
//					continue;
//				}
//				
//				if (invMat.getId().equals(sourceMat.getId())) {
//					count2++;
//				}
//			}
//			
//			if (count2 != (count - 1)) {
//				CivMessage.sendError(event.getPlayer(), "Error: Item count mismatch.");
//				event.setCancelled(true);
//			} else {			
//				HashMap<Integer, ItemStack> leftovers = event.getPlayer().getInventory().addItem(stack);
//				for (ItemStack s : leftovers.values()) {
//					event.getPlayer().getWorld().dropItem(event.getPlayer().getLocation(), s);
//				}
//				event.getPlayer().updateInventory();
//			}
//		}
	}


}
