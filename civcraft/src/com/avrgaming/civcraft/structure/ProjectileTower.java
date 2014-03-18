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
import java.util.HashSet;

import org.bukkit.Location;
import org.bukkit.util.Vector;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;

public abstract class ProjectileTower extends Structure {

	
	public ProjectileTower(ResultSet rs) throws SQLException, CivException {
		super(rs);

	}

	protected ProjectileTower(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
	}
	
	private HashSet<BlockCoord> turrets = new HashSet<BlockCoord>();
	
	public void fire(Location turretLoc, Location playerLoc) {
		//override in children.
	}
	
	@Override
	public void loadSettings() {}
	
	public void setTurretLocation(BlockCoord absCoord) {	
		turrets.add(absCoord);
	}
	
	public static Vector getVectorBetween(Location to, Location from) {
		Vector dir = new Vector();
		
		dir.setX(to.getX() - from.getX());
		dir.setY(to.getY() - from.getY());
		dir.setZ(to.getZ() - from.getZ());
	
		return dir;
	}
	
//	private Location getNearestTurret(Location playerLoc) {
//		
//		double distance = Double.MAX_VALUE;
//		BlockCoord nearest = null;
//		for (BlockCoord turretCoord : turrets) {
//			Location turretLoc = turretCoord.getLocation();
//			if (playerLoc.getWorld() != turretLoc.getWorld()) {
//				return null;
//			}
//			
//			double tmp = turretLoc.distance(playerLoc);
//			if (tmp < distance) {
//				distance = tmp;
//				nearest = turretCoord;
//			}
//			
//		}
//		if (nearest == null) {
//			return null;
//		}
//		return nearest.getLocation();
//	}
	
//	private  boolean isWithinRange(Location residentLocation, double range) {
//		
//		if (residentLocation.getWorld() != this.getCorner().getLocation().getWorld()) {
//			return false;
//		}
//		
//		if (residentLocation.distance(this.getCorner().getLocation()) <= range ) {
//			return true;
//		}
//		return false;
//	}
//	
//	private boolean canSee(Player player, Location loc2) {
//		Location loc1 = player.getLocation();		
//		return ((CraftWorld)loc1.getWorld()).getHandle().a(Vec3D.a(loc1.getX(), loc1.getY() + player.getEyeHeight(), loc1.getZ()), Vec3D.a(loc2.getX(), loc2.getY(), loc2.getZ())) == null;
//	}
	
	protected Location adjustTurretLocation(Location turretLoc, Location playerLoc) {
		// Keep the y position the same, but advance 1 block in the direction of the player..?
		int diff = 2;
		
		int xdiff = 0;
		int zdiff = 0;
		if (playerLoc.getBlockX() > turretLoc.getBlockX()) {
			xdiff = diff;
		} else if (playerLoc.getBlockX() < turretLoc.getBlockX()){
			xdiff = -diff;
		}  
		
		if (playerLoc.getBlockZ() > turretLoc.getBlockZ()) {
			zdiff = diff;
		} else if (playerLoc.getBlockZ() < turretLoc.getBlockZ()){
			zdiff = -diff;
		} 
				
		return turretLoc.getBlock().getRelative(xdiff, 0, zdiff).getLocation();
	}
	
	public void process() {
		
			//for (PlayerLocationCache pc : proximityComponent.getNearbyPlayers()) {
		//		CivLog.debug("PLAYER NEARBY:"+pc.getName());
		//	}
		
//		
//			if (!this.isActive()) {
//				return;
//			}
//
//			if (this.getTown().isInLiberationMode()) {
//				return;
//			}
//			
//			Player nearestPlayer = null;
//			double nearestDistance = Double.MAX_VALUE;
//			
//			Location turretLoc = null;
//			for (PlayerLocationCache pc : proximityComponent.tryGetNearbyPlayers()) {
//								
//				if (pc == null || pc.isDead()) {
//					continue;
//				}
//			
//				if (!this.getTown().isOutlaw(pc.getName())) {
//					Resident resident = pc.getResident();
//					// Try to exit early by making sure this resident is at war.
//					if (resident == null || (!resident.hasTown())) {
//						continue;
//					}
//					
//					if (!this.getCiv().getDiplomacyManager().isHostileWith(resident)) {
//						continue;
//					}
//				}
//				
//				Location playerLoc = pc.getCoord().getLocation();
//				turretLoc = getNearestTurret(playerLoc);
//				if (turretLoc == null) {
//					// No nearest turret, player is probably not in the same world as the turret.
//					return;
//				}
//
//				Player player;
//				try {
//					player = CivGlobal.getPlayer(pc.getName());
//				} catch (CivException e) {
//					return;
//				}
//				
//				// XXX todo convert this to not use a player so we can async...
//				if (!this.canSee(player, turretLoc)) {
//					continue;
//				}
//			
//				if (isWithinRange(player.getLocation(), range)) {
//					if (isWithinRange(player.getLocation(), min_range)) {
//						continue;
//					}
//					
//					double distance = player.getLocation().distance(this.getCorner().getLocation());
//					if (distance < nearestDistance) {
//						nearestPlayer = player;
//						nearestDistance = distance;
//					}
//				}
//			}
//			
//			if (nearestPlayer == null || turretLoc == null) {
//				return;
//			}
//			
//			Location playerLoc = nearestPlayer.getLocation();
//			fire(turretLoc, playerLoc);
			
	}
	
	@Override
	public String getMarkerIconName() {
		return "tower";
	}
	
	@Override
	public int getHitPoints() {
		int base = super.getHitpoints();
		double rate = 1;
		rate += this.getTown().getBuffManager().getEffectiveDouble(Buff.BARRAICADE);
		rate += this.getTown().getBuffManager().getEffectiveDouble("buff_chichen_itza_tower_hp");
		
		return (int)(base*rate);		
	}

}
