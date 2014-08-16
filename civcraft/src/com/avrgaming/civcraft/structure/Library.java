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
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.enchantments.Enchantment;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.components.AttributeBiome;
import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.LibraryEnchantment;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class Library extends Structure {

	private int level;
	public AttributeBiome cultureBeakers;
	
	ArrayList<LibraryEnchantment> enchantments = new ArrayList<LibraryEnchantment>();

	private NonMemberFeeComponent nonMemberFeeComponent;
	
	public static Enchantment getEnchantFromString(String name) {
		
		// Armor Enchantments
		if (name.equalsIgnoreCase("protection")) {
			return Enchantment.PROTECTION_ENVIRONMENTAL;
		}
		if (name.equalsIgnoreCase("fire_protection")) {
			return Enchantment.PROTECTION_FIRE;
		}
		if (name.equalsIgnoreCase("feather_falling")) {
			return Enchantment.PROTECTION_FALL;
		}
		if (name.equalsIgnoreCase("blast_protection")) {
			return Enchantment.PROTECTION_EXPLOSIONS;
		}
		if (name.equalsIgnoreCase("projectile_protection")) {
			return Enchantment.PROTECTION_PROJECTILE;
		}
		if (name.equalsIgnoreCase("respiration")) {
			return Enchantment.OXYGEN;
		}
		if (name.equalsIgnoreCase("aqua_affinity")) {
			return Enchantment.WATER_WORKER;
		}
		
		// Sword Enchantments
		if (name.equalsIgnoreCase("sharpness")) {
			return Enchantment.DAMAGE_ALL;
		}
		if (name.equalsIgnoreCase("smite")) {
			return Enchantment.DAMAGE_UNDEAD;
		}
		if (name.equalsIgnoreCase("bane_of_arthropods")) {
			return Enchantment.DAMAGE_ARTHROPODS;
		}
		if (name.equalsIgnoreCase("knockback")) {
			return Enchantment.KNOCKBACK;
		}
		if (name.equalsIgnoreCase("fire_aspect")) {
			return Enchantment.FIRE_ASPECT;
		}
		if (name.equalsIgnoreCase("looting")) {
			return Enchantment.LOOT_BONUS_MOBS;
		}
		
		// Tool Enchantments
		if (name.equalsIgnoreCase("efficiency")) {
			return Enchantment.DIG_SPEED;
		}
		if (name.equalsIgnoreCase("silk_touch")) {
			return Enchantment.SILK_TOUCH;
		}
		if (name.equalsIgnoreCase("unbreaking")) {
			return Enchantment.DURABILITY;
		}
		if (name.equalsIgnoreCase("fortune")) {
			return Enchantment.LOOT_BONUS_BLOCKS;
		}
		
		// Bow Enchantments
		if (name.equalsIgnoreCase("power")) {
			return Enchantment.ARROW_DAMAGE;
		}
		if (name.equalsIgnoreCase("punch")) {
			return Enchantment.ARROW_KNOCKBACK;
		}
		if (name.equalsIgnoreCase("flame")) {
			return Enchantment.ARROW_FIRE;
		}
		if (name.equalsIgnoreCase("infinity")) {
			return Enchantment.ARROW_INFINITE;
		}
		
		return null;
		
	}

	public double getNonResidentFee() {
		return this.nonMemberFeeComponent.getFeeRate();
	}

	public void setNonResidentFee(double nonResidentFee) {
		this.nonMemberFeeComponent.setFeeRate(nonResidentFee);
	}
	
	private String getNonResidentFeeString() {
		return "Fee: "+((int)(getNonResidentFee()*100) + "%").toString();		
	}
	
	protected Library(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onSave();
	}

	public Library(ResultSet rs) throws SQLException, CivException {
		super(rs);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onLoad();
	}
	
	@Override
	public void loadSettings() {
		super.loadSettings();	
	}

	public int getLevel() {
		return level;
	}


	public void setLevel(int level) {
		this.level = level;
	}
	
	private StructureSign getSignFromSpecialId(int special_id) {
		for (StructureSign sign : getSigns()) {
			int id = Integer.valueOf(sign.getAction());
			if (id == special_id) {
				return sign;
			}
		}
		return null;
	}
	
	@Override
	public void updateSignText() {

		int count = 0;
		
		for (LibraryEnchantment enchant : this.enchantments) {
			StructureSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("sign from special id was null, id:"+count);
				return;
			}
			sign.setText(enchant.displayName+"\n"+
					"Level "+enchant.level+"\n"+
					getNonResidentFeeString()+"\n"+
					"For "+enchant.price);
			sign.update();
			count++;
		}
	
		for (; count < getSigns().size(); count++) {
			StructureSign sign = getSignFromSpecialId(count);
			sign.setText("Library Slot\nEmpty");
			sign.update();
		}
	}
	
	public void validateEnchantment(ItemStack item, LibraryEnchantment ench) throws CivException {
		if (ench.enchant != null) {
			
			if(!ench.enchant.canEnchantItem(item)) {
				throw new CivException("You cannot enchant this item with this enchantment.");
			}
			
			if (item.containsEnchantment(ench.enchant) && item.getEnchantmentLevel(ench.enchant) > ench.level) {
				throw new CivException("You already have this enchantment at this level, or better.");
			}
			
			
		} else {
			if (!ench.enhancement.canEnchantItem(item)) {
				throw new CivException("You cannot enchant this item with this enchantment.");
			}
			
			if (ench.enhancement.hasEnchantment(item)) {
				throw new CivException("You already have this enchantment.");
			}
		}
	}
	
	public ItemStack addEnchantment(ItemStack item, LibraryEnchantment ench) {
		if (ench.enchant != null) {
			item.addUnsafeEnchantment(ench.enchant, ench.level);
		} else {
			item = LoreMaterial.addEnhancement(item, ench.enhancement);
		}
		return item;
	}
	
	public void add_enchantment_to_tool(Player player, StructureSign sign, PlayerInteractEvent event) throws CivException {
		int special_id = Integer.valueOf(sign.getAction());

		if (!event.hasItem()) {
			CivMessage.send(player, CivColor.Rose+"You must have the item you wish to enchant in hand.");
			return;
		}
		ItemStack item = event.getItem();
		
		if (special_id >= this.enchantments.size()) {
			throw new CivException("Library enchantment not ready.");
		}
		
		
		LibraryEnchantment ench = this.enchantments.get(special_id);
		this.validateEnchantment(item, ench);
		
		int payToTown = (int) Math.round(ench.price*getNonResidentFee());
		Resident resident;
				
		resident = CivGlobal.getResident(player.getName());
		Town t = resident.getTown();	
		if (t == this.getTown()) {
				// Pay no taxes! You're a member.
				payToTown = 0;
		}					
				
		// Determine if resident can pay.
		if (!resident.getTreasury().hasEnough(ench.price+payToTown)) {
			CivMessage.send(player, CivColor.Rose+"You do not have enough money, you need "+ench.price+payToTown+ " coins.");
			return;
		}
				
		// Take money, give to server, TEH SERVER HUNGERS ohmnom nom
		resident.getTreasury().withdraw(ench.price);
		
		// Send money to town for non-resident fee
		if (payToTown != 0) {
			getTown().depositDirect(payToTown);
			
			CivMessage.send(player, CivColor.Yellow + "Paid "+ payToTown+" coins in non-resident taxes.");
		}
				
		// Successful payment, process enchantment.
		ItemStack newStack = this.addEnchantment(item, ench);
		player.setItemInHand(newStack);
		CivMessage.send(player, CivColor.LightGreen+"Enchanted with "+ench.displayName+"!");
	}

	@Override
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
		try {
			add_enchantment_to_tool(player, sign, event);
		} catch (CivException e) {
			CivMessage.send(player, CivColor.Rose+e.getMessage());
		}	
	}
	
	@Override
	public String getDynmapDescription() {
		String out = "<u><b>Library</u></b><br/>";
		
		if (this.enchantments.size() == 0) {
			out += "Nothing stocked.";
		} 
		else {
			for (LibraryEnchantment mat : this.enchantments) {
				out += mat.displayName+" for "+mat.price+"<br/>";
			}
		}
		return out;
	}
	
	
	public ArrayList<LibraryEnchantment> getEnchants() {
		return enchantments;
	}


	public void addEnchant(LibraryEnchantment enchant) throws CivException {
		if (enchantments.size() >= 4) {
			throw new CivException("Library is full.");
		}
		enchantments.add(enchant);
	}
	
	@Override
	public String getMarkerIconName() {
		return "bookshelf";
	}

	public void reset() {
		this.enchantments.clear();
		this.updateSignText();
	}
	
}
