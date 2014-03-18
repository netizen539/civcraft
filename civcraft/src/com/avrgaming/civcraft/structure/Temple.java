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

import org.bukkit.Location;
import org.bukkit.entity.EntityType;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigTempleSacrifice;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class Temple extends Structure {

	protected Temple(Location center, String id, Town town) throws CivException {
		super(center, id, town);
	}

	public Temple(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}
	
	@Override
	public String getDynmapDescription() {
		return null;
	}
	
	@Override
	public String getMarkerIconName() {
		return "church";
	}
	

	public void onEntitySacrifice(EntityType entityType) {
		ConfigTempleSacrifice sac = null;
		for (ConfigTempleSacrifice s : CivSettings.templeSacrifices) {
			for (String str : s.entites) {				
				if (str.equalsIgnoreCase(entityType.toString())) {
					sac = s;
					break;
				}
			}
		}
		
		if (sac == null) {
			return;
		}
		
		this.getTown().addAccumulatedCulture(sac.reward);
		CivMessage.sendTown(this.getTown(), "Our Sacrifice has awarded our town "+CivColor.LightPurple+sac.reward+CivColor.White+" culture.");
		this.getTown().save();
	}


}
