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
package com.avrgaming.civcraft.threading.tasks;


import com.avrgaming.civcraft.config.ConfigMission;
import com.avrgaming.civcraft.items.units.MissionBook;

public class PerformMissionTask implements Runnable {
	ConfigMission mission;
	String playerName;
	
	public PerformMissionTask (ConfigMission mission, String playerName) {
		this.mission = mission;
		this.playerName = playerName;
	}
	
	
	@Override
	public void run() {
		MissionBook.performMission(mission, playerName);
	}

}
