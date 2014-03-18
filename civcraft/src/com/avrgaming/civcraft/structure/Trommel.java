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
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Location;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Town;

public class Trommel extends Structure {

	private static final double IRON_CHANCE = CivSettings.getDoubleStructure("trommel.iron_chance"); //2%
	private static final double GOLD_CHANCE = CivSettings.getDoubleStructure("trommel.gold_chance"); //1%
	private static final double DIAMOND_CHANCE = CivSettings.getDoubleStructure("trommel.diamond_chance"); //0.25%
	private static final double EMERALD_CHANCE = CivSettings.getDoubleStructure("trommel.emerald_chance"); //0.10%
	private static final double CHROMIUM_CHANCE = CivSettings.getDoubleStructure("trommel.chromium_chance");
	
	public int skippedCounter = 0;
	public ReentrantLock lock = new ReentrantLock();
	
	public enum Mineral {
		EMERALD,
		DIAMOND,
		GOLD,
		IRON,
		CHROMIUM
	}
	
	protected Trommel(Location center, String id, Town town) throws CivException {
		super(center, id, town);	
	}
	
	public Trommel(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}
	
	@Override
	public String getMarkerIconName() {
		return "minecart";
	}
	
	public double getMineralChance(Mineral mineral) {
		double chance = 0;
		switch (mineral) {
		case EMERALD:
			chance = EMERALD_CHANCE;
			break;
		case DIAMOND:
			chance = DIAMOND_CHANCE;
			break;
		case GOLD:
			chance = GOLD_CHANCE;
			break;
		case IRON:
			chance = IRON_CHANCE;
			break;
		case CHROMIUM:
			chance = CHROMIUM_CHANCE;
		}
		
		double increase = chance*this.getTown().getBuffManager().getEffectiveDouble(Buff.EXTRACTION);
		chance += increase;
		
		try {
			if (this.getTown().getGovernment().id.equals("gov_despotism")) {
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.despotism_rate");
			} else {
				chance *= CivSettings.getDouble(CivSettings.structureConfig, "trommel.penalty_rate");
			}
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		
		return chance;
	}

}
