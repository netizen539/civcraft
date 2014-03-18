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
package com.avrgaming.civcraft.object;

import org.bukkit.enchantments.Enchantment;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.loreenhancements.LoreEnhancement;
import com.avrgaming.civcraft.structure.Library;

public class LibraryEnchantment {
	public Enchantment enchant;
	public LoreEnhancement enhancement;
	public int level;
	public double price;
	public String name;
	public String displayName;

	public LibraryEnchantment(String name, int lvl, double p) throws CivException {
		enchant = Library.getEnchantFromString(name);
		if (enchant == null)  {
			enhancement = LoreEnhancement.enhancements.get(name);
			if (enhancement == null) {
				throw new CivException("Could not create CivEnchantment:"+name+". Couldn't find enchantment or enhancement");
			}
		}
		level = lvl;
		price = p;
		
		this.name = name;
		if (enchant != null) {
			displayName = name.replace("_", " ");
		} else {
			displayName = enhancement.getDisplayName();
		}
		
	}
}
