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
package com.avrgaming.civcraft.command.civ;

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.questions.DiplomacyGiftResponse;
import com.avrgaming.civcraft.questions.QuestionResponseInterface;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.CivQuestionTask;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class CivDiplomacyGiftCommand extends CommandBase {

	@Override
	public void init() {
		command = "/civ dip gift";
		displayName = "Civ Diplomacy Gift";
		
		commands.put("entireciv", "[civ] - Sends our entire civilization as a gift to [civ]. Only usable by civ leaders.");
		commands.put("town", "[town] [civ] - Sends this town as a gift to [civ]. Only useable by civ leaders.");
		
	}

	private void sendGiftRequest(Civilization toCiv, Civilization fromCiv, String message, 
			QuestionResponseInterface finishedFunction) throws CivException {
		CivQuestionTask task = CivGlobal.civQuestions.get(toCiv.getName()); 
		if (task != null) {
			/* Civ already has a question pending. Lets deny this question until it times out
			 * this will allow questions to come in on a pseduo 'first come first serve' and 
			 * prevents question spamming.
			 */
			throw new CivException("Civilization already has an offer pending, wait 30 seconds and try again.");			
		}
		
		task = new CivQuestionTask(toCiv, fromCiv, message, 30000, finishedFunction);
		CivGlobal.civQuestions.put(toCiv.getName(), task);
		TaskMaster.asyncTask("", task, 0);
	}
	
	public void entireciv_cmd() throws CivException {
		this.validLeader();
		Civilization fromCiv = getSenderCiv();
		Civilization toCiv = getNamedCiv(1);
		
		if (fromCiv == toCiv) {
			throw new CivException("Cannot gift your civiliation to itself.");
		}
		
		if (fromCiv.getDiplomacyManager().isAtWar() || toCiv.getDiplomacyManager().isAtWar()) {
			throw new CivException("Cannot gift your civilization if either civ is at war.");
		}
		
		fromCiv.validateGift();
		toCiv.validateGift();
		
		if (War.isWarTime()) {
			throw new CivException("Cannot gift civilizations during WarTime.");
		}
		
		if (War.isWithinWarDeclareDays()) {
			throw new CivException("Cannot gift civilizations within "+War.getTimeDeclareDays()+" days before WarTime.");
		}
		
		
		DiplomacyGiftResponse dipResponse = new DiplomacyGiftResponse();
		dipResponse.giftedObject = fromCiv;
		dipResponse.fromCiv = fromCiv;
		dipResponse.toCiv = toCiv;
		
		sendGiftRequest(toCiv, fromCiv, 
				CivColor.Yellow+ChatColor.BOLD+"The Civilization of "+fromCiv.getName()+" wishes to give itself to you. All of their towns will be yours."+
						" It will cost us "+fromCiv.getMergeCost()+" coins. Do you accept?", dipResponse);
		CivMessage.sendSuccess(sender, "Gift request sent, waiting for them to accept the gift.");
	}
	
	public void town_cmd() throws CivException {
		this.validLeader();
		Civilization fromCiv = getSenderCiv();
		Town giftedTown = getNamedTown(1);
		Civilization toCiv = getNamedCiv(2);

		if (giftedTown.getCiv() != fromCiv) {
			throw new CivException("You cannot gift a town that is not yours.");
		}
		
		if (giftedTown.getCiv() == toCiv) {
			throw new CivException("You cannot gift a town to your own civ.");
		}
		
		if (giftedTown.getMotherCiv() != null && toCiv != giftedTown.getMotherCiv()) {
			throw new CivException("You cannot gift captured towns to another civ unless it is the mother civ.");
		}
		
		if (giftedTown.isCapitol()) {
			throw new CivException("You cannot give away your capitol town. Try gifting your entire civilization instead.");
		}
		
		if (War.isWarTime()) {
			throw new CivException("Cannot gift towns during WarTime.");
		}
		
		if (fromCiv.getDiplomacyManager().isAtWar() || toCiv.getDiplomacyManager().isAtWar()) {
			throw new CivException("Cannot gift your town if either civ is at war.");
		}
		
		fromCiv.validateGift();
		toCiv.validateGift();
		giftedTown.validateGift();
		
		DiplomacyGiftResponse dipResponse = new DiplomacyGiftResponse();
		dipResponse.giftedObject = giftedTown;
		dipResponse.fromCiv = fromCiv;
		dipResponse.toCiv = toCiv;
		
		sendGiftRequest(toCiv, fromCiv, 
				"Our Civilization of "+fromCiv.getName()+" wishes to give the town of "+giftedTown.getName()+" to you. It will cost us "+giftedTown.getGiftCost()+" coins. Do you accept?", dipResponse);
		CivMessage.sendSuccess(sender, "Gift request sent, waiting for them to accept the gift.");
		
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		// permission checked in parent command.
	}

}
