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
package com.avrgaming.civcraft.util;

import org.bukkit.inventory.Inventory;

public class ItemInvPair {
	public Inventory inv;
	public String mid;
	public int type;
	public short data;
	public int amount;
	
	public ItemInvPair (Inventory inv, String mid, int type, short data, int amount) {
		this.inv = inv;
		this.mid = mid;
		this.type = type;
		this.data = data;
		this.amount = amount;
	}
}