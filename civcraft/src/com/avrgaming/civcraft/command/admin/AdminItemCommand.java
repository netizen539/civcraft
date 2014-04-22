package com.avrgaming.civcraft.command.admin;

import java.util.HashMap;

import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementArenaItem;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementAttack;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementDefense;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementSoulBound;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.ItemManager;

public class AdminItemCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad item";
		displayName = "Admin Item";
		
		commands.put("enhance", "[name] - Adds the specified enhancement.");
	}

	public void enhance_cmd() throws CivException {
		Player player = getPlayer();
		HashMap<String, LoreEnhancement> enhancements = new HashMap<String, LoreEnhancement>();
		ItemStack inHand = getPlayer().getItemInHand();
		
		enhancements.put("soulbound", new LoreEnhancementSoulBound());
		enhancements.put("attack", new LoreEnhancementAttack());
		enhancements.put("defence", new LoreEnhancementDefense());
		enhancements.put("arena", new LoreEnhancementArenaItem());

		if (inHand == null || ItemManager.getId(inHand) == CivData.AIR) {
			throw new CivException("You must have an item in your hand to enhance it.");
		}
		
		if (args.length < 2) {
			CivMessage.sendHeading(sender, "Possible Enchants");
			String out = "";
			for (String str : enhancements.keySet()) {
				out += str + ", ";
			}
			CivMessage.send(sender, out);
			return;
		}
		
		String name = getNamedString(1, "enchantname");
		name.toLowerCase();
		for (String str : enhancements.keySet()) {
			if (name.equals(str)) {
				LoreEnhancement enh = enhancements.get(str);
				ItemStack stack = LoreMaterial.addEnhancement(inHand, enh);
				player.setItemInHand(stack);
				CivMessage.sendSuccess(sender, "Enhanced with "+name);
				return;
			}
		}
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		
	}

}
