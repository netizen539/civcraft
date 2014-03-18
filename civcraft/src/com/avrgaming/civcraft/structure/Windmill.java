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

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.WindmillStartSyncTask;

public class Windmill extends Structure {
	
	public enum CropType {
		WHEAT,
		CARROTS,
		POTATOES
	}
	
	public Windmill(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public Windmill(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
	}

	@Override
	public void onEffectEvent() {

	
	}
	
	public void processWindmill() {
		/* Fire a sync task to perform this. */
		TaskMaster.syncTask(new WindmillStartSyncTask(this), 0);
	}
	
}
