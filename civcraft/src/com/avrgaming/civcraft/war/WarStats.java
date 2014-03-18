package com.avrgaming.civcraft.war;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;

import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class WarStats {

	/* Lets keep track of some basic stats. */
	
	/*
	 * Stores player kills.
	 */
	private static HashMap<String, Integer> playerKills = new HashMap<String, Integer>();
	
	/*
	 * Stores Captured Civs, who conquered whom.
	 * key = civ who conquered, value = civ who was defeated.
	 */
	private static HashMap<String, LinkedList<String>> conqueredCivs = new HashMap<String, LinkedList<String>>();
	
	/*
	 * Conquered Towns, key = civ who conquered, value = town
	 */
	private static HashMap<String, LinkedList<String>> conqueredTowns = new HashMap<String, LinkedList<String>>();
	
	public static void incrementPlayerKills(String playerName) {
		Integer kills = playerKills.get(playerName);
		if (kills == null) {
			kills = 1;
		} else {
			kills++;
		}
		
		playerKills.put(playerName, kills);
	}
	
	public static void logCapturedTown(Civilization winner, Town captured) {
		LinkedList<String> towns = conqueredTowns.get(winner.getName());
		
		if (towns == null) {
			towns = new LinkedList<String>();
		}
		
		towns.add(captured.getName());
		conqueredTowns.put(winner.getName(), towns);
	}
	
	public static void logCapturedCiv(Civilization winner, Civilization captured) {
		LinkedList<String> civs = conqueredCivs.get(winner.getName());
		
		if (civs == null) {
			civs = new LinkedList<String>();
		}
		
		civs.add(captured.getName());
		conqueredCivs.put(winner.getName(), civs);
	}
	
	public static String getTopKiller() {
		
		String out = "";
		int mostKills = 0;
		for (String playerName : playerKills.keySet()) {
			int kills = playerKills.get(playerName);
			if (kills > mostKills) {
				out = playerName;
				mostKills = kills;
			}
		}
		
		return CivColor.LightGreen+CivColor.BOLD+out+CivColor.LightGray+" ("+mostKills+" kills)";
	}
	
	public static List<String> getCapturedCivs() {
		LinkedList<String> out = new LinkedList<String>();
		
		for (String key : conqueredCivs.keySet()) {
			LinkedList<String> conquered = conqueredCivs.get(key);
			if (conquered == null) {
				continue;
			}
			
			String line = CivColor.LightGreen+CivColor.BOLD+key+CivColor.Rose+CivColor.BOLD+" Conquered: "+CivColor.RESET+CivColor.LightGray;
			String tmp = "";
			for (String str : conquered) {
				tmp += str+", ";
			}
			
			line += tmp;
			out.add(line);
		}
	
		return out;
	}
	
	public static void clearStats() {
		playerKills.clear();
		conqueredCivs.clear();
		conqueredTowns.clear();
	}
	
//	
//	public List<String> getCapturedTowns() {
//		LinkedList<String> out = new LinkedList<String>();
//		
//		for (String key : conqueredTowns.keySet()) {
//			LinkedList<String> conquered = conqueredTowns.get(key);
//			if (conquered == null) {
//				continue;
//			}
//			
//			String line = CivColor.LightGreen+CivColor.BOLD+key+" Captured Towns of: "+CivColor.RESET+CivColor.LightGray;
//			String tmp = "";
//			for (String str : conquered) {
//				tmp += str+", ";
//			}
//			
//			line += tmp;
//			out.add(line);
//		}
//	
//		return out;
//	}
}
