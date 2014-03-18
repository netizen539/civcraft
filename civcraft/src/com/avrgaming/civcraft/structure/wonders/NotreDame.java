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
package com.avrgaming.civcraft.structure.wonders;

import java.sql.ResultSet;
import java.sql.SQLException;

import org.bukkit.Location;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;

public class NotreDame extends Wonder {

	public NotreDame(Location center, String id, Town town) throws CivException {
		super(center, id, town);
	}

	public NotreDame(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	@Override
	public void onLoad() {
		if (this.isActive()) {
			addBuffs();
		}
	}
	
	@Override
	public void onComplete() {
		addBuffs();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		removeBuffs();
	}
	
	@Override
	protected void removeBuffs() {
		this.removeBuffFromTown(this.getTown(), "buff_notre_dame_no_anarchy");
		this.removeBuffFromTown(this.getTown(), "buff_notre_dame_coins_from_peace");
		this.removeBuffFromTown(this.getTown(), "buff_notre_dame_extra_war_penalty");
	}

	@Override
	protected void addBuffs() {
		this.addBuffToTown(this.getTown(), "buff_notre_dame_no_anarchy");
		this.addBuffToTown(this.getTown(), "buff_notre_dame_coins_from_peace");
		this.addBuffToTown(this.getTown(), "buff_notre_dame_extra_war_penalty");

	}

	public void processPeaceTownCoins() {
		double totalCoins = 0;
		
		double coinsPerTown = this.getTown().getBuffManager().getEffectiveInt("buff_notre_dame_coins_from_peace");
		
		for (Civilization civ : CivGlobal.getCivs()) {
			if (civ.isAdminCiv()) {
				continue;
			}
			
			if (civ.getDiplomacyManager().isAtWar()) {
				continue;
			}
			
			totalCoins += (coinsPerTown*civ.getTowns().size());
		}
		
		this.getTown().depositTaxed(totalCoins);
		CivMessage.sendTown(this.getTown(), "Generated "+totalCoins+" from the peaceful towns of the world!");	
		
	}

}
