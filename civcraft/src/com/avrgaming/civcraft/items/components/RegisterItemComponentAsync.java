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
package com.avrgaming.civcraft.items.components;


public class RegisterItemComponentAsync implements Runnable {

	public ItemComponent component;
	public String name;
	public boolean register;
	
	public RegisterItemComponentAsync(ItemComponent itemComp, String name, boolean register) {
		this.component = itemComp;
		this.name = name;
		this.register = register;
	}
	
	@Override
	public void run() {
//		
//		if (register) {
//		ItemComponent.lock.lock();
//			try {
//				ArrayList<ItemComponent> components = ItemComponent.componentsByType.get(name);
//				
//				if (components == null) {
//					components = new ArrayList<ItemComponent>();
//				}
//			
//				components.add(component);
//				ItemComponent.componentsByType.put(name, components);
//			} finally {
//				ItemComponent.lock.unlock();
//			}		
//		} else {
//			ItemComponent.lock.lock();
//			try {
//				ArrayList<ItemComponent> components = ItemComponent.componentsByType.get(name);
//				
//				if (components == null) {
//					return;
//				}
//			
//				components.remove(component);
//				ItemComponent.componentsByType.put(name, components);
//			} finally {
//				ItemComponent.lock.unlock();
//			}
//		}	
	}

}
