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

import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.World;

import com.avrgaming.civcraft.util.FireworkEffectPlayer;

public class FireWorkTask implements Runnable {
	
	FireworkEffectPlayer fplayer = new FireworkEffectPlayer();
	FireworkEffect fe;
	int repeats;
	World world;
	Location loc;
	
	public FireWorkTask(FireworkEffect fe, World world, Location loc, int repeats) {
		this.fe = fe;
		this.repeats = repeats;
		this.world = world;
		this.loc = loc;
	}
	
	@Override
	public void run() {
		for (int i = 0; i < repeats; i++) {
			try {
				fplayer.playFirework(world, loc, fe);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		
	}

}
