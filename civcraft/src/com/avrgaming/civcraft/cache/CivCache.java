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
package com.avrgaming.civcraft.cache;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


public class CivCache {

	/* Arrows fired that need to be updated. */
	public static Map<UUID, ArrowFiredCache> arrowsFired = new HashMap<UUID, ArrowFiredCache>();
	
	/* Cannon balls fired that need to be updated. */
	public static Map<UUID, CannonFiredCache> cannonBallsFired = new HashMap<UUID, CannonFiredCache>();
	
	
}
