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


import java.util.HashSet;

import org.bukkit.ChatColor;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.questions.CapitulateRequest;
import com.avrgaming.civcraft.questions.ChangeRelationResponse;
import com.avrgaming.civcraft.threading.tasks.CivQuestionTask;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class CivDiplomacyCommand extends CommandBase {
	public static final long INVITE_TIMEOUT = 30000; //30 seconds

	@Override
	public void init() {
		command = "/civ dip";
		displayName = "Civ Diplomacy";
		
		commands.put("show", "[civ] - Lists all current diplomatic relations for [civ].");
		commands.put("declare", "[civ] [hostile|war] - Sets your relationship with this civ.");
		commands.put("request", "[civ] [neutral|peace|ally] - Sends a request to the other civ to change your relations.");
		commands.put("gift", "Sends a gift to another civilization.");
		commands.put("global", "Shows diplomatic relations for entire server.");
		commands.put("wars", "Shows only the wars going on in the entire server.");
		commands.put("respond", "[yes|no] - Responds to a request sent by another civ.");
		commands.put("liberate", "[town] - Gives this town back to its rightful owner, it if's a capitol the civlization is restored.");
		commands.put("capitulate", "[town] - Capitulates this town, if it is conquered, to it's current owner. Requires confirmation.");
	}
	
	public void capitulate_cmd() throws CivException {
		if (War.isWarTime()) {
			throw new CivException("You cannot use this diplomacy command while it is WarTime.");
		}
		Town town = getNamedTown(1);
		Resident resident = getResident();
		boolean entireCiv = false;
		
		Civilization motherCiv = town.getMotherCiv();
		
		if (motherCiv == null) {
			throw new CivException("Cannot capitulate unless captured by another civilization.");
		}
		
		if (!town.getMotherCiv().getLeaderGroup().hasMember(resident)) {
			throw new CivException("You must be the leader of the captured civilization in order to capitulate.");
		}
		
		if (town.getMotherCiv().getCapitolName().equals(town.getName())) {
			entireCiv = true;
		}
		
		String requestMessage = "";
		CapitulateRequest capitulateResponse = new CapitulateRequest();

		if (args.length < 3 || !args[2].equalsIgnoreCase("yes")) {
			if (entireCiv) {
				CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"Capitualting means that this civ will be DELETED and all of its towns will become a normal towns in "+
						town.getCiv().getName()+" and can no longer revolt. Are you sure?");
				CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"If you're sure, type /civ dip capitulate "+town.getName()+" yes");
			} else {
				CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"Capitualting means that this town will become a normal town in "+town.getCiv().getName()+" and can no longer revolt. Are you sure?");
				CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"If you're sure, type /civ dip capitulate "+town.getName()+" yes");
			}
			return;
		}
		
		if (entireCiv) {
			requestMessage = CivColor.Yellow+ChatColor.BOLD+"The Civilization of "+motherCiv.getName()+" would like to capitulate. Bringing in more towns will increase civ-wide unhappiness. Do we accept?";
			capitulateResponse.from = town.getMotherCiv().getName();
		} else {
			capitulateResponse.from = "Town of "+town.getName();
			requestMessage = CivColor.Yellow+ChatColor.BOLD+"The Town of "+town.getName()+" would like to capitulate. If we accept this town become ours and we"+
					" will have to pay distance upkeep to it. Do we accept?";	
		}
		
		capitulateResponse.playerName = resident.getName();
		capitulateResponse.capitulator = town;
		capitulateResponse.to = town.getCiv().getName();
		
		CivGlobal.requestRelation(motherCiv, town.getCiv(), requestMessage,
				INVITE_TIMEOUT, capitulateResponse);
		CivMessage.sendSuccess(sender, "Sent capitulate request.");
		
	}
	
	public void liberate_cmd() throws CivException {
		if (War.isWarTime()) {
			throw new CivException("You cannot use this diplomacy command while it is WarTime.");
		}
		this.validLeader();
		Town town = getNamedTown(1);
		Civilization civ = getSenderCiv();
		
		if (town.getCiv() != civ) {
			throw new CivException("This town does not belong to your civlization.");
		}

		Civilization motherCiv = town.getMotherCiv();
		if (motherCiv == null) {
			throw new CivException("This town has not been captured, you cannot liberate it.");
		}
		
		if (town.getName().equals(motherCiv.getCapitolName())) {
			Civilization capitolOwnerCiv = town.getCiv();
			
			/* Liberate the civ. */
			for (Town t : CivGlobal.getTowns()) {
				if (t.getMotherCiv() == motherCiv && t.getCiv() == capitolOwnerCiv) {
					t.changeCiv(motherCiv);
					t.setMotherCiv(null);
					t.save();
				}
			}
			
			motherCiv.setConquered(false);
			CivGlobal.removeConqueredCiv(motherCiv);
			CivGlobal.addCiv(motherCiv);
			motherCiv.save();	
			CivMessage.global("The civilization of "+motherCiv.getName()+" has been liberated by the good graces of its owner "+civ.getName());
		} else {
			if (motherCiv.isConquered()) {
				throw new CivException("The mother civilization of "+town.getName()+" is conquered. You cannot liberate this town at the moment.");
			}
			
			/* Liberate just the town. */
			town.changeCiv(motherCiv);
			town.setMotherCiv(null);
			town.save();
			CivMessage.global("The town of "+town.getName()+" has been liberated by the good graces of its owner "+civ.getName()+
					". It has joined its homeland "+motherCiv.getName());
		}
	}
	
	public void gift_cmd() throws CivException {
		CivDiplomacyGiftCommand cmd = new CivDiplomacyGiftCommand();	
		if (War.isWarTime()) {
			throw new CivException("You cannot use this diplomacy command while it is WarTime.");
		}
		cmd.onCommand(sender, null, "gift", this.stripArgs(args, 1));	
	}
	
	public void global_cmd() {
		CivMessage.sendHeading(sender, "Global Relations");

		for (Civilization civ : CivGlobal.getCivs()) {
			for (Relation relation : civ.getDiplomacyManager().getRelations()) {
				CivMessage.send(sender, civ.getName()+": "+relation.toString());
			}
		}
	}
	
	public void wars_cmd() {
		CivMessage.sendHeading(sender, "Wars");
		HashSet<String> usedRelations = new HashSet<String>();
		
		for (Civilization civ : CivGlobal.getCivs()) {
			for (Relation relation : civ.getDiplomacyManager().getRelations()) {
				if (relation.getStatus().equals(Status.WAR)) {
					if (!usedRelations.contains(relation.getPairKey())) {
						CivMessage.send(sender, 
								CivColor.LightBlue+CivColor.BOLD+relation.getCiv().getName()+CivColor.Rose+" <-- WAR --> "+CivColor.LightBlue+CivColor.BOLD+relation.getOtherCiv().getName());						
						usedRelations.add(relation.getPairKey());
					}
				}
			}
		}
	}
	
	public void respond_cmd() throws CivException {
		validLeaderAdvisor();
		if (War.isWarTime()) {
			throw new CivException("You cannot use this diplomacy command while it is WarTime.");
		}

		if (args.length < 2) {
			throw new CivException("Please enter 'yes' or 'no'");
		}
	
		CivQuestionTask task = CivGlobal.getCivQuestionTask(getSenderCiv());
		if (task == null) {
			throw new CivException("No offer to respond to.");
		}		
		
		if (args[1].equalsIgnoreCase("yes")) {
			synchronized(task) {
				task.setResponse("accept");
				task.notifyAll();
			}
		} else if (args[1].equalsIgnoreCase("no")) {
			synchronized(task) {
				task.setResponse("decline");
				task.notifyAll();
			}
		} else {
			throw new CivException("Please enter 'yes' or 'no'");
		}
		
		CivMessage.sendSuccess(sender, "Response sent.");
	}
	
	public void request_cmd() throws CivException {
		validLeaderAdvisor();
		Civilization ourCiv = getSenderCiv();
		if (War.isWarTime()) {
			throw new CivException("You cannot use this diplomacy command while it is WarTime.");
		}
		
		if (args.length < 3) {
			throw new CivException("Enter a civ name followed by 'neutral', 'peace', or 'ally'");
		}
		
		Civilization otherCiv = getNamedCiv(1);
		
		if (ourCiv.getId() == otherCiv.getId()) {
			throw new CivException("Cannot request anything on from your own civilization");
		}
		
		try {
			Relation.Status status = Relation.Status.valueOf(args[2].toUpperCase());
			Relation.Status currentStatus = ourCiv.getDiplomacyManager().getRelationStatus(otherCiv);

			if (currentStatus == status) {
				throw new CivException("Already "+status.name()+" with "+otherCiv.getName());
			}
			
			String message = CivColor.LightGreen+ChatColor.BOLD+ourCiv.getName()+" has requested ";
			switch (status) {
			case NEUTRAL:
				message += "a NEUTRAL relationship";
				break;
			case PEACE:
				message += "a PEACE treaty";
				break;
			case ALLY:
				message += "an ALLY";
				
				if (War.isWithinWarDeclareDays()) {
					if (ourCiv.getDiplomacyManager().isAtWar() || otherCiv.getDiplomacyManager().isAtWar()) {
						throw new CivException("Cannot make new allies within "+War.getTimeDeclareDays()+" before WarTime when one of you is at war.");
					}
				}
				break;
			case WAR:
				if (!CivGlobal.isCasualMode()) {
					throw new CivException("Can only request war in casual mode.");
				}
				
				message += "a WAR";
				break;
			default:
				throw new CivException("Options are 'neutral', 'peace', 'ally' or 'war'");
			}
			message += ". Do we accept?";
			
			ChangeRelationResponse relationresponse = new ChangeRelationResponse();
			relationresponse.fromCiv = ourCiv;
			relationresponse.toCiv = otherCiv;
			relationresponse.status = status;
			
			CivGlobal.requestRelation(ourCiv, otherCiv, 
					message,
					INVITE_TIMEOUT, relationresponse);
			
			CivMessage.sendSuccess(sender, "Request sent.");
		} catch (IllegalArgumentException e) {
			throw new CivException("Unknown relationship type, options are 'neutral', 'peace', 'ally' or 'war'");
		}
		
	}
	
	public void declare_cmd() throws CivException {
		validLeaderAdvisor();
		Civilization ourCiv = getSenderCiv();
		if (War.isWarTime()) {
			throw new CivException("You cannot use this diplomacy command while it is WarTime.");
		}
		
		if (args.length < 3) {
			throw new CivException("Enter a civ name, followed by 'hostile', or 'war'.");
		}
		
		Civilization otherCiv = getNamedCiv(1);
		
		if (ourCiv.getId() == otherCiv.getId()) {
			throw new CivException("Cannot declare anything on your own civilization.");
		}
		
		if (otherCiv.isAdminCiv()) {
			throw new CivException("Cannot declare war on an admin civilization.");
		}
		
		try {
			Relation.Status status = Relation.Status.valueOf(args[2].toUpperCase());
			Relation.Status currentStatus = ourCiv.getDiplomacyManager().getRelationStatus(otherCiv);
			//boolean aidingAlly = false;

			if (currentStatus == status) {
				throw new CivException("Already "+status.name()+" with "+otherCiv.getName());
			}
			
			switch (status) {
			case HOSTILE:
				if (currentStatus == Relation.Status.WAR) {
					throw new CivException("Cannot declare "+status.name()+" when at war.");
				}
			break;
			case WAR:
				if (CivGlobal.isCasualMode()) {
					throw new CivException("Cannot declare war in casual mode. Use '/civ dip request' instead.");
				}
				
				if (War.isWarTime()) {
					throw new CivException("Cannot declare war during WarTime.");
				}
				
				if (War.isWithinWarDeclareDays()) {
					if (War.isCivAggressorToAlly(otherCiv, ourCiv)) {
						if (War.isWithinAllyDeclareHours()) {
							throw new CivException("Too soon to next WarTime. Allies can only aid other allies within "+War.getAllyDeclareHours()+" hours before WarTime.");
						} else {
							//aidingAlly = true;
						}
					} else {		
						throw new CivException("Too soon to next WarTime. Cannot declare "+War.getTimeDeclareDays()+" before WarTime.");
					}
				}
				
				if (ourCiv.getTreasury().inDebt()) {
					throw new CivException("Cannot declare ware while our civilization is in debt.");
				}
				
				break;
			default:
				throw new CivException("Options are hostile or war");
			}
			
			CivGlobal.setRelation(ourCiv, otherCiv, status);
			//Boolean aidingAlly is in commentaries a couple lines higher (2 times) 
			//if (aidingAlly) {
			//	/* If we're aiding an ally, the other civ is the true aggressor. */
			//	CivGlobal.setAggressor(otherCiv, ourCiv, otherCiv);
			//} else {
			//	CivGlobal.setAggressor(ourCiv, otherCiv, ourCiv);
			//} 
			CivGlobal.setAggressor(ourCiv, otherCiv, ourCiv);
						
		} catch (IllegalArgumentException e) {
			throw new CivException("Unknown relationship type, options hostile or war");
		}
	
	}
	
	public void show_cmd() throws CivException {
		if (args.length < 2) {
			show(getSenderCiv());
			return;
		}
		
		Civilization civ = getNamedCiv(1);
		
		show(civ);
	}
	
	public void show(Civilization civ) {
		CivMessage.sendHeading(sender, "Diplomatic Relations for "+CivColor.Yellow+civ.getName());
		
		for (Relation relation : civ.getDiplomacyManager().getRelations()) {
			if (relation.getStatus() == Relation.Status.NEUTRAL) {
				continue;
			}
			CivMessage.send(sender, relation.toString());
		}
		
		int warCount = civ.getDiplomacyManager().getWarCount();
		if (warCount != 0) {
			CivMessage.send(sender, CivColor.Rose+"Your civilization is currently engaged in "+warCount+" wars.");
		}
		CivMessage.send(sender, CivColor.LightGray+"Not shown means NEUTRAL.");
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
	
	}

}
