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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivLog;

public class BuffManager {

	/* 
	 * Contains a list of buffs currently active for this 'object'(town, or wonder)
	 * it will manage which goods stack with which and provide easy
	 * access functions to the amounts from each bonus and totals.
	 */
	
	/* Contains _all_ of the buffs attached to us. */
	private HashMap<String, Buff> buffs = new HashMap<String, Buff>();
	
	/* Contains the _effective_ buffs attached to us, taking stacking into account. */
	private HashMap<String, Buff> effectiveBuffs = new HashMap<String, Buff>();
	
	
	public void clearBuffs() {
		synchronized(this) {
			buffs.clear();
			effectiveBuffs.clear();
		}
	}
	
	
	private boolean hasBuffId(String id, HashMap<String, Buff> map) {
		for (Buff buff : map.values()) {
			if (buff.getId().equals(id)) {
				return true;
			}
		}
		return false;
	}
	
	/* Adds a buff to this collection. */
	public void addBuff(String buffkey, String buff_id, String source) throws CivException {
		synchronized(this) {
			if (buffs.containsKey(buffkey)) {
				throw new CivException("Already contains buff key:"+buffkey);
			}
			
			Buff buff = new Buff(buffkey, buff_id, source);
			buffs.put(buff.getKey(), buff);
			if (buff.isStackable() || !hasBuffId(buff_id, effectiveBuffs)) {
				effectiveBuffs.put(buff.getKey(), buff);
			}
		}
	}
	
	/* Removes a buff from this collection. */
	public void removeBuff(String buff_key) {
		buffs.remove(buff_key);
		effectiveBuffs.remove(buff_key);
	}
	
	public Collection<Buff> getEffectiveBuffs() {
		return effectiveBuffs.values();
	}
	
	public Collection<Buff> getEffectiveBuffs(String buff_id) {
		ArrayList<Buff> returnList = new ArrayList<Buff>();
		synchronized(this) {
			for (Buff buff : effectiveBuffs.values()) {
				if (buff.getParent().equals(buff_id)) {
					returnList.add(buff);
				}
			}
		}
		return returnList;
	}
	
	/*
	 * searches for any buff who's parent matches this id.
	 * Buffs with no parent get it set to it's own id when loaded in.
	 * This allows us to have multiple levels of buffs e.g. buff_monopoly_2
	 * whose parent is set to buff_monopoly so that it will act like a normal 
	 * monopoly buff with a different value.
	 */
	public double getEffectiveDouble(String buff_id) {
		double ret = 0.0;
		
		synchronized(this) {
			for (Buff buff : effectiveBuffs.values()) {
				if (buff.getParent().equals(buff_id)) {
					ret += Double.valueOf(buff.getValue());
				}
			}
		}
		
		return ret;
	}
	
	public int getEffectiveInt(String buff_id) {
		int ret = 0;
		
		synchronized(this) {
			for (Buff buff : effectiveBuffs.values()) {
				if (buff.getParent().equals(buff_id)) {
					ret += Integer.valueOf(buff.getValue());
				}
			}
		}
		return ret;
	}
	
	/* Gets the value of the first buff it finds with buff id. */
	public String getValue(String buff_id) {
		synchronized(this) {
			for (Buff buff : effectiveBuffs.values()) {
				if (buff.getParent().equals(buff_id)) {
					return buff.getValue();
				}
			}
		}
		return null;
	}
	
	public void debugPrint() {
		String out = "";
		for (Buff buff : buffs.values()) {
			out += "key:"+buff.getKey()+" id:"+buff.getId()+" source:"+buff.getSource()+",";
		}
		
		out = "";
		for (Buff buff : effectiveBuffs.values()) {
			out += "key:"+buff.getKey()+" id:"+buff.getId()+" source:"+buff.getSource()+",";
		}
		CivLog.info(out);
	}

	public boolean hasBuff(String id) {
		for (Buff buff : buffs.values()) {
			if (buff.getId().equals(id) || buff.getParent().equals(id)) {
				return true;
			}
		}
		
		return false;
	}

	public Collection<Buff> getAllBuffs() {
		return this.buffs.values();
	}


	public boolean hasBuffKey(String key) {
		return buffs.containsKey(key);
	}
	
	
	
}
