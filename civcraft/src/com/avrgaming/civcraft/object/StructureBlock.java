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
package com.avrgaming.civcraft.object;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.war.War;

public class StructureBlock implements BuildableDamageBlock {

	private BlockCoord coord = null;
	private Buildable owner = null;
	private boolean damageable = true;
	private boolean alwaysDamage = false;
	
	/* This is a block that can be damaged. */
	public StructureBlock(BlockCoord coord, Buildable owner) {
		this.coord = coord;
		this.owner = owner;
	}
	
	public Buildable getOwner() {
		return owner;
	}

	public void setOwner(Buildable owner) {
		this.owner = owner;
	}
	
	public Town getTown() {
		return this.owner.getTown();
	}
	
	public Civilization getCiv() {
		return this.owner.getCiv();
	}

	public BlockCoord getCoord() {
		return coord;
	}

	public void setCoord(BlockCoord coord) {
		this.coord = coord;
	}
	
	public int getX() {
		return this.coord.getX();
	}
	
	public int getY() {
		return this.coord.getY();
	}
	
	public int getZ() {
		return this.coord.getZ();
	}
	
	public String getWorldname() {
		return this.coord.getWorldname();
	}

	public boolean isDamageable() {
		return damageable;
	}

	public void setDamageable(boolean damageable) {
		this.damageable = damageable;
	}
	
	public boolean canDestroyOnlyDuringWar() {
		return true;
	}

	@Override
	public boolean allowDamageNow(Player player) {	
		// Dont bother making any checks if we're not at war
		if (War.isWarTime()) {
			// Structures with max hitpoints of 0 cannot be damaged.
			if (this.getOwner().getMaxHitPoints() != 0) {
				Resident res = CivGlobal.getResident(player.getName());
				if (res == null) {
					return false;
				}
				
				// Make sure the resident has a town
				if (res.hasTown()) {
					if (res.getTown().defeated) {
						CivMessage.sendError(player, "Cannot damage structures when your town has been defeated.");
						return false;
					}
					
					Civilization civ = res.getTown().getCiv();
					// Make sure we are at war with this civilization. 
					// Cant be at war with our own, will be false if our own structure.
					if (civ.getDiplomacyManager().atWarWith(this.getCiv())) {
						if (this.alwaysDamage) {
							return true;
						}
						
						if (!this.isDamageable()) {
							CivMessage.sendError(player, "Cannot damage this structure block. Choose another.");
						} else if (CivGlobal.willInstantBreak(this.getCoord().getBlock().getType())) {
							CivMessage.sendError(player, "Cannot damage structure with this block, try another.");								
						} else {
							return true;
						}
					}
				}
			}
		}
		return false;
	}

	public boolean isAlwaysDamage() {
		return alwaysDamage;
	}

	public void setAlwaysDamage(boolean alwaysDamage) {
		this.alwaysDamage = alwaysDamage;
	}
}
