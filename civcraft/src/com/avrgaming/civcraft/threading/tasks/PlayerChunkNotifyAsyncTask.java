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

import java.util.Date;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.util.AsciiMap;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class PlayerChunkNotifyAsyncTask implements Runnable {

	Location from;
	Location to;
	String playerName;
	
	public static int BORDER_SPAM_TIMEOUT = 30000; //30 second border spam protection.
	public static HashMap<String, Date> cultureEnterTimes = new HashMap<String, Date>();
	
	public PlayerChunkNotifyAsyncTask(Location from, Location to, String playerName) {
		this.from = from;
		this.to = to;
		this.playerName = playerName;
	}
	
	public static String getNotifyColor(CultureChunk toCc, Relation.Status status, Player player) {

		String color = CivColor.White;
		switch (status) {
		case NEUTRAL:
			if (toCc.getTown().isOutlaw(player.getName())) {
				color = CivColor.Yellow;
			}
			
			break;
		case HOSTILE:
			color = CivColor.Yellow;
			break;
		case WAR:
			color = CivColor.Rose;

			break;
		case PEACE:
			color = CivColor.LightBlue;

			break;
		case ALLY:
			color = CivColor.Green;
		}
		
		return color;
	}
	
	private String getToWildMessage() {
		return CivColor.LightGray+"Entering Wilderness "+CivColor.Rose+"[PvP]";
	}
	
	private String getToTownMessage(Town town, TownChunk tc) {
		Player player;
		try {
			player = CivGlobal.getPlayer(playerName);
		} catch (CivException e) {
			return "";
		}
		
		if (town.getBuffManager().hasBuff("buff_hanging_gardens_regen")) {
			Resident resident = CivGlobal.getResident(player);
			if (resident != null && resident.getTown() == town) {
				CivMessage.send(player, CivColor.Green+ChatColor.ITALIC+"You feel invigorated by the glorious hanging gardens.");
			}
		}
		
		if (!tc.isOutpost()) {
			return CivColor.LightGray+"Entering "+CivColor.White+town.getName()+" "+town.getPvpString()+" ";
		} else {
			return CivColor.LightGray+"Entering Outpost of "+CivColor.White+town.getName()+" "+town.getPvpString()+" ";
		}
	}
	
	private void showPlotMoveMessage() {
		
		TownChunk fromTc = CivGlobal.getTownChunk(from);
		TownChunk toTc = CivGlobal.getTownChunk(to);
		CultureChunk fromCc = CivGlobal.getCultureChunk(from);
		CultureChunk toCc = CivGlobal.getCultureChunk(to);
		Camp toCamp = CivGlobal.getCampFromChunk(new ChunkCoord(to));
		Camp fromCamp = CivGlobal.getCampFromChunk(new ChunkCoord(from));

		Player player;
		Resident resident;
		try {
			player = CivGlobal.getPlayer(this.playerName);
			resident = CivGlobal.getResident(this.playerName);
		} catch (CivException e) {
			return;
		}
		
		String out = "";
		
		//We've entered a camp.
		if (toCamp != null && toCamp != fromCamp) {
			out += CivColor.Gold+"Camp "+toCamp.getName()+" "+CivColor.Rose+"[PvP]";
		}
		
		if (toCamp == null && fromCamp != null) {
			out += getToWildMessage();
		}
		
		// From Wild, to town
		if (fromTc == null && toTc != null) {			
			// To Town
			out += getToTownMessage(toTc.getTown(), toTc);
		}
		
		// From a town... to the wild
		if (fromTc != null && toTc == null) {
			out += getToWildMessage();
		}
		
		// To another town(should never happen with culture...)
		if (fromTc != null && toTc != null && fromTc.getTown() != toTc.getTown()) {
			out += getToTownMessage(toTc.getTown(), toTc);
		}
		
		if (toTc != null) {
			out += toTc.getOnEnterString(player, fromTc);
		}
		
		// Leaving culture to the wild.
		if (fromCc != null && toCc == null) {
			out += fromCc.getOnLeaveString();
		}
		
		// Leaving wild, entering culture. 
		if (fromCc == null && toCc != null) {
			out += toCc.getOnEnterString();
			onCultureEnter(toCc);
		}
		
		//Leaving one civ's culture, into another. 
		if (fromCc != null && toCc !=null && fromCc.getCiv() != toCc.getCiv()) {
			out += fromCc.getOnLeaveString() +" | "+ toCc.getOnEnterString();
			onCultureEnter(toCc);
		}
		
		if (!out.equals("")) {
			//ItemMessage im = new ItemMessage(CivCraft.getPlugin());
			//im.sendMessage(player, CivColor.BOLD+out, 3);
			
			CivMessage.send(player, out);
		}
		
		if (resident.isShowInfo()) {
			CultureChunk.showInfo(player);
		}
		
	}
	
	private void onCultureEnter(CultureChunk toCc) {
		Player player;
		try {
			player = CivGlobal.getPlayer(this.playerName);
		} catch (CivException e) {
			return;
		}
		
		Relation.Status status = toCc.getCiv().getDiplomacyManager().getRelationStatus(player);
		String color = getNotifyColor(toCc, status, player);
		String relationName = status.name();
		
		if (player.isOp()) {
			return;
		}
		
		Resident resident = CivGlobal.getResident(player);
		if (resident != null && resident.hasTown() && resident.getCiv() == toCc.getCiv()) {
			return;
		}
		
		
		String borderSpamKey = player.getName()+":"+toCc.getCiv().getName();
		Date lastMessageTime = cultureEnterTimes.get(borderSpamKey);
		
		Date now = new Date();
		if ((lastMessageTime != null) && (now.getTime() < (lastMessageTime.getTime() + BORDER_SPAM_TIMEOUT))) {
			// Preventing border spam, not issuing message.
			return;
		}
		lastMessageTime = now;

		cultureEnterTimes.put(borderSpamKey, lastMessageTime);
		CivMessage.sendCiv(toCc.getCiv(), color+player.getDisplayName()+"("+relationName+") has entered our borders.");
	}


	@Override
	public void run() {		
		showPlotMoveMessage();
		showResidentMap();
	}


	private void showResidentMap() {
		Player player;
		try {
			player = CivGlobal.getPlayer(this.playerName);
		} catch (CivException e) {
			return;
		}
		
		Resident resident = CivGlobal.getResident(player);
		if (resident == null) {
			return;
		}
		
		if (resident.isShowMap()) {
			CivMessage.send(player, AsciiMap.getMapAsString(player.getLocation()));
		}	
	}
	
	

}
