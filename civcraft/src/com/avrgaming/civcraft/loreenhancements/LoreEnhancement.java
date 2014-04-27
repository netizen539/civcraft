package com.avrgaming.civcraft.loreenhancements;

import gpl.AttributeUtil;

import java.util.HashMap;

import org.bukkit.event.entity.PlayerDeathEvent;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.object.BuildableDamageBlock;
import com.avrgaming.civcraft.util.ItemManager;

public abstract class LoreEnhancement {
	public AttributeUtil add(AttributeUtil attrs) {
		return attrs;
	}
	
	public static HashMap<String, LoreEnhancement> enhancements = new HashMap<String, LoreEnhancement>();
	public HashMap<String, String> variables = new HashMap<String, String>();
	
	public static void init() {
		enhancements.put("LoreEnhancementSoulBound", new LoreEnhancementSoulBound());
		enhancements.put("LoreEnhancementAttack", new LoreEnhancementAttack());
		enhancements.put("LoreEnhancementDefense", new LoreEnhancementDefense());
		enhancements.put("LoreEnhancementPunchout", new LoreEnhancementPunchout());
		enhancements.put("LoreEnhancementArenaItem", new LoreEnhancementArenaItem());
	}
	
	public boolean onDeath(PlayerDeathEvent event, ItemStack stack) { return false; }

	public boolean canEnchantItem(ItemStack item) {
		return true;
	}
	
	public static boolean isWeapon(ItemStack item) {
		switch (ItemManager.getId(item)) {
		case CivData.WOOD_SWORD:
		case CivData.STONE_SWORD:
		case CivData.IRON_SWORD:
		case CivData.GOLD_SWORD:
		case CivData.DIAMOND_SWORD:
		case CivData.BOW:
			return true;
		default:
			return false;
		}
	}
	
	public static boolean isArmor(ItemStack item) {
		switch (ItemManager.getId(item)) {
		case CivData.LEATHER_BOOTS:
		case CivData.LEATHER_CHESTPLATE:
		case CivData.LEATHER_HELMET:
		case CivData.LEATHER_LEGGINGS:
		case CivData.IRON_BOOTS:
		case CivData.IRON_CHESTPLATE:
		case CivData.IRON_HELMET:
		case CivData.IRON_LEGGINGS:
		case CivData.DIAMOND_BOOTS:
		case CivData.DIAMOND_CHESTPLATE:
		case CivData.DIAMOND_HELMET:
		case CivData.DIAMOND_LEGGINGS:
		case CivData.CHAIN_BOOTS:
		case CivData.CHAIN_CHESTPLATE:
		case CivData.CHAIN_HELMET:
		case CivData.CHAIN_LEGGINGS:
		case CivData.GOLD_BOOTS:
		case CivData.GOLD_CHESTPLATE:
		case CivData.GOLD_HELMET:
		case CivData.GOLD_LEGGINGS:
			return true;
		default:
			return false;
		}
	}
	
	public static boolean isWeaponOrArmor(ItemStack item) {
		return isWeapon(item) || isArmor(item);
	}

	public boolean hasEnchantment(ItemStack item) {
		return false;
	}

	public String getDisplayName() {
		return "LoreEnchant";
	}

	public int onStructureBlockBreak(BuildableDamageBlock dmgBlock, int damage) {
		return damage;
	}

	public double getLevel(AttributeUtil attrs) {	return 0; }
	public abstract String serialize(ItemStack stack);
	public abstract ItemStack deserialize(ItemStack stack, String data);
	
	
}
