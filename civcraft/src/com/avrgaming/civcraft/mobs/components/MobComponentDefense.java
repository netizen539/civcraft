package com.avrgaming.civcraft.mobs.components;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

public class MobComponentDefense extends MobComponent {

	private double defense = 0.0;
	
	public MobComponentDefense(double defense) {
		this.defense = defense;
	}
	
	@Override
	public void onDefense(EntityDamageByEntityEvent event) {
		double damage = event.getDamage();
		
		damage -= this.defense;
		if (damage < 0.5) {
			damage = 0.0;
			
			Player player = null;
			if (event.getDamager() instanceof Arrow) {
				Arrow arrow = (Arrow)event.getDamager();
				if (arrow.getShooter() instanceof Player) {
					player = (Player)arrow.getShooter();
				}
			} else if (event.getDamager() instanceof Player){
				player = (Player)event.getDamager();
			}
			
			if (player != null) {
				CivMessage.send(player, CivColor.LightGray+"Our attack was ineffective");
			}
		}
		event.setDamage(damage);
	}

	public double getDefense() {
		return defense;
	}


	public void setDefense(double defense) {
		this.defense = defense;
	}

}
