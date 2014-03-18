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
package com.avrgaming.civcraft.threading.sync.request;

import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.util.MultiInventory;

public class UpdateInventoryRequest extends AsyncRequest {

	public UpdateInventoryRequest(ReentrantLock lock) {
		super(lock);
	}

	public enum Action {
		ADD,
		REMOVE
	}
	
	public MultiInventory inv;
	public ItemStack stack;
	public Action action;
	
	
}
