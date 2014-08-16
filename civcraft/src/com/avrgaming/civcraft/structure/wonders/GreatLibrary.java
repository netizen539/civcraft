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
package com.avrgaming.civcraft.structure.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigEnchant;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;

public class GreatLibrary extends Wonder {

	public GreatLibrary(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
	}

	public GreatLibrary(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void onLoad() {
		if (this.isActive()) {
			addBuffs();
		}
	}
	
	@Override
	public void onComplete() {
		addBuffs();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		removeBuffs();
	}
	
	@Override
	protected void removeBuffs() {
		this.removeBuffFromCiv(this.getCiv(), "buff_greatlibrary_extra_beakers");
		this.removeBuffFromTown(this.getTown(), "buff_greatlibrary_double_tax_beakers");
	}

	@Override
	protected void addBuffs() {
		this.addBuffToCiv(this.getCiv(), "buff_greatlibrary_extra_beakers");
		this.addBuffToTown(this.getTown(), "buff_greatlibrary_double_tax_beakers");
	}
	
	
	@Override
	public void updateSignText() {
		
		for (StructureSign sign : getSigns()) {
			ConfigEnchant enchant;
			switch (sign.getAction().toLowerCase()) {
			case "0":
				enchant = CivSettings.enchants.get("ench_fire_aspect");
				sign.setText(enchant.name+"\n\n"+CivColor.LightGreen+enchant.cost+" Coins.");
				break;
			case "1":
				enchant = CivSettings.enchants.get("ench_fire_protection");
				sign.setText(enchant.name+"\n\n"+CivColor.LightGreen+enchant.cost+" Coins.");
				break;
			case "2":
				enchant = CivSettings.enchants.get("ench_flame");
				sign.setText(enchant.name+"\n\n"+CivColor.LightGreen+enchant.cost+" Coins.");				
				break;			
			case "3":
				enchant = CivSettings.enchants.get("ench_punchout");
				sign.setText(enchant.name+"\n\n"+CivColor.LightGreen+enchant.cost+" Coins.");
				break;
			}
			
			sign.update();
		}
	}
	
	@Override
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
		//int special_id = Integer.valueOf(sign.getAction());
		Resident resident = CivGlobal.getResident(player);
		
		if (resident == null) {
			return;
		}
		
		if (!resident.hasTown() || resident.getCiv() != this.getCiv()) {
			CivMessage.sendError(player, "Only members of "+this.getCiv().getName()+" may use The Great Library.");
			return;
		}
		
		ItemStack hand = player.getItemInHand();
		ConfigEnchant configEnchant;
		
		switch (sign.getAction()) {
		case "0": /* fire aspect */
			if (!Enchantment.FIRE_ASPECT.canEnchantItem(hand)) {
				CivMessage.sendError(player, "You cannot put this enchantment on this item.");
				return;
			}
			
			configEnchant = CivSettings.enchants.get("ench_fire_aspect");
			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
				CivMessage.send(player, CivColor.Rose+"You do not have enough money, you need "+configEnchant.cost+" coins.");
				return;
			}
			
			resident.getTreasury().withdraw(configEnchant.cost);
			hand.addEnchantment(Enchantment.FIRE_ASPECT, 2);			
			break;
		case "1": /* fire protection */
			if (!Enchantment.PROTECTION_FIRE.canEnchantItem(hand)) {
				CivMessage.sendError(player, "You cannot put this enchantment on this item.");
				return;	
			}
			
			configEnchant = CivSettings.enchants.get("ench_fire_protection");
			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
				CivMessage.send(player, CivColor.Rose+"You do not have enough money, you need "+configEnchant.cost+" coins.");
				return;
			}
			
			resident.getTreasury().withdraw(configEnchant.cost);
			hand.addEnchantment(Enchantment.PROTECTION_FIRE, 3);			
			break;
		case "2": /* flame */
			if (!Enchantment.ARROW_FIRE.canEnchantItem(hand)) {
				CivMessage.sendError(player, "You cannot put this enchantment on this item.");
				return;	
			}
			
			configEnchant = CivSettings.enchants.get("ench_flame");
			if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
				CivMessage.send(player, CivColor.Rose+"You do not have enough money, you need "+configEnchant.cost+" coins.");
				return;
			}
			
			resident.getTreasury().withdraw(configEnchant.cost);
			hand.addEnchantment(Enchantment.ARROW_FIRE, 1);	
			break;
		case "3":
			switch (ItemManager.getId(hand)) {
			case CivData.WOOD_PICKAXE:
			case CivData.STONE_PICKAXE:
			case CivData.IRON_PICKAXE:
			case CivData.DIAMOND_PICKAXE:
			case CivData.GOLD_PICKAXE:
				configEnchant = CivSettings.enchants.get("ench_punchout");
				
				if (!LoreMaterial.isCustom(hand)) {					
					CivMessage.sendError(player, "This item is not a custom civcraft item and cannot recieve this enhancement.");
					return;
				}
				
				if (LoreMaterial.hasEnhancement(hand, configEnchant.enchant_id)) {
					CivMessage.sendError(player, "You already have this enhancement on this item.");
					return;
				}
				
				if (!resident.getTreasury().hasEnough(configEnchant.cost)) {
					CivMessage.send(player, CivColor.Rose+"You do not have enough money, you need "+configEnchant.cost+" coins.");
					return;
				}
				
				resident.getTreasury().withdraw(configEnchant.cost);
				ItemStack newItem = LoreMaterial.addEnhancement(hand, LoreEnhancement.enhancements.get(configEnchant.enchant_id));				
				player.setItemInHand(newItem);
				break;
			default:
				CivMessage.sendError(player, "You can only add this enchantment to pickaxes.");
				return;	
			}
			break;
		default:
			return;
		}
		
		CivMessage.sendSuccess(player, "Enchant Success!");
	}

}
