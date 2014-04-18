package pvptimer;

import org.bukkit.entity.Arrow;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.EntityDamageByEntityEvent;

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
			if (Resident.isProtected(damager) && (event.getEntity() instanceof Player)) {
				CivMessage.sendError(damager, "You are unable to damage players while protected.");
				event.setCancelled(true);					
			}
		} else if (event.getDamager() instanceof Arrow) {
			LivingEntity shooter = (LivingEntity) ((Arrow) event.getDamager()).getShooter();
			
			if ((shooter instanceof Player) && (event.getEntity() instanceof Player)) {
				Player damager = (Player) shooter;
				if (Resident.isProtected(damager)) {
					CivMessage.sendError(damager, "You are unable to damage players while protected.");
					event.setCancelled(true);
				} else {
					Player defendingPlayer = (Player) event.getEntity();
					if (Resident.isProtected(defendingPlayer)) {
						CivMessage.sendError(damager, "You are unable to damage protected players.");
						event.setCancelled(true);
					}
				}				
			}
		}
		if ((event.getEntity() instanceof Player) && !event.isCancelled()) {
			Player damager = (Player) event.getDamager();
			Player defendingPlayer = (Player) event.getEntity();
			if (event.getDamager() instanceof Player) {
				if (Resident.isProtected(defendingPlayer)) {
					event.setCancelled(true);
					CivMessage.sendError(damager, "You are unable to damage protected players.");					
				}
			}
		}
	}
}
