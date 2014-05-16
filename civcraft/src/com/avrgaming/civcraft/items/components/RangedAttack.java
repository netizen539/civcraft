package com.avrgaming.civcraft.items.components;

import gpl.AttributeUtil;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerItemHeldEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.items.units.Unit;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancementAttack;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.util.CivColor;

public class RangedAttack extends ItemComponent {

	@Override
	public void onPrepareCreate(AttributeUtil attrs) {
		attrs.addLore(CivColor.Rose+this.getDouble("value")+" Ranged Attack");	
	}
	
	private static double ARROW_MAX_VEL = 6.0; 
	
	@Override
	public void onInteract(PlayerInteractEvent event) {
		if (Unit.isWearingAnyMetal(event.getPlayer())) {
			event.setCancelled(true);
			CivMessage.sendError(event.getPlayer(), "Cannot use a bow while wearing metal armor.");
			return;
		}
	}
	
	@Override
	public void onHold(PlayerItemHeldEvent event) {	
		
		Resident resident = CivGlobal.getResident(event.getPlayer());
		if (!resident.hasTechForItem(event.getPlayer().getInventory().getItem(event.getNewSlot()))) {		
			CivMessage.send(resident, CivColor.Rose+"Warning - "+CivColor.LightGray+
					"You do not have the required technology to use this item. It's attack output will be reduced in half.");
		}
	}
	
	public void onRangedAttack(EntityDamageByEntityEvent event, ItemStack inHand) {
		AttributeUtil attrs = new AttributeUtil(inHand);
		double dmg = this.getDouble("value");
		
		if (event.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow)event.getDamager();
			if (arrow.getShooter() instanceof Player) {
				Player attacker = (Player)arrow.getShooter();
				if (Unit.isWearingAnyMetal(attacker)) {
					event.setCancelled(true);
					CivMessage.sendError(attacker, "Cannot use a bow while wearing metal armor.");
					return;
				}
			}
		}
		
		double extraAtt = 0.0;
		for (LoreEnhancement enh : attrs.getEnhancements()) {
			if (enh instanceof LoreEnhancementAttack) {
				extraAtt +=  ((LoreEnhancementAttack)enh).getExtraAttack(attrs);
			}
		}
		dmg += extraAtt;
		
		
		Vector vel = event.getDamager().getVelocity();
		double magnitudeSquared = Math.pow(vel.getX(), 2) + Math.pow(vel.getY(), 2) + Math.pow(vel.getZ(), 2);
		
		double percentage = magnitudeSquared / ARROW_MAX_VEL;
		double totalDmg = percentage * dmg;
		
		if (totalDmg > dmg) {
			totalDmg = dmg;
		}
		
		if (event.getDamager() instanceof Arrow) {
			Arrow arrow = (Arrow)event.getDamager();
			if (arrow.getShooter() instanceof Player) {
				Resident resident = CivGlobal.getResident(((Player)arrow.getShooter()));
				if (!resident.hasTechForItem(inHand)) {
					totalDmg = totalDmg / 2;
				}
			}
		}
		
		if (totalDmg < 0.5) {
			totalDmg = 0.5;
		}
		
		event.setDamage(totalDmg);
	}


}
