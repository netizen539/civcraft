package pvptimer;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;

public class PvPListener implements Listener {
	
	@EventHandler(priority = EventPriority.LOW)
	public void onPvP(EntityDamageByEntityEvent event) {
		if (event.isCancelled()) {
			return;
		}
		
		if (event.getDamager() instanceof Player) {
			Player damager = (Player) event.getDamager();
			Resident damagerResident = CivGlobal.getResident(damager);
			
			if (damagerResident.isProtected() && (event.getEntity() instanceof Player)) {
				CivMessage.sendError(damager, "You are unable to damage players while protected.");
				event.setCancelled(true);					
			}
		}
		if (event.getDamager() instanceof Arrow) {
			LivingEntity shooter = (LivingEntity) ((Arrow) event.getDamager()).getShooter();
			
			if ((shooter instanceof Player) && (event.getEntity() instanceof Player)) {
				Player damager = (Player) shooter;
				Resident damagerResident = CivGlobal.getResident(damager);

				if (damagerResident.isProtected()) {
					CivMessage.sendError(damager, "You are unable to damage players while protected.");
					event.setCancelled(true);
				} else {
					Player defendingPlayer = (Player) event.getEntity();
					Resident defendingResident = CivGlobal.getResident(defendingPlayer);
					if (defendingResident.isProtected()) {
						CivMessage.sendError(damager, "You are unable to damage protected players.");
						event.setCancelled(true);
					}
				}				
			}
		}
		if ((event.getEntity() instanceof Player) && !event.isCancelled() && (event.getDamager() instanceof Player)) {
			Player damager = (Player) event.getDamager();
			Player defendingPlayer = (Player) event.getEntity();
			Resident defendingResident = CivGlobal.getResident(defendingPlayer);
			if (event.getDamager() instanceof Player) {
				if (defendingResident.isProtected()) {
					event.setCancelled(true);
					CivMessage.sendError(damager, "You are unable to damage protected players.");					
				}
			}
		}
	}
}
