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

import java.text.DecimalFormat;
import java.util.ArrayList;

import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.endgame.EndConditionScience;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.DecimalHelper;

public class CivInfoCommand extends CommandBase {

	@Override
	public void init() {
		command = "/civ info";
		displayName = "Civ Info";
		
		commands.put("upkeep", "Shows upkeep information for this civ.");
		commands.put("taxes", "Shows tax information on towns.");
		commands.put("beakers", "Shows Civilization beaker information.");
		commands.put("online", "Lists all members of the civilization that are currently online.");
	}
	
	public void online_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		CivMessage.sendHeading(sender, "Online Players In "+civ.getName());
		String out = "";
		for (Resident resident : civ.getOnlineResidents()) {
			out += resident.getName()+" ";
		}
		CivMessage.send(sender, out);
	}
	
	public void beakers_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		CivMessage.sendHeading(sender, "Civ Beaker Info");
		ArrayList<String> out = new ArrayList<String>();
		
		for (Town t : civ.getTowns()) {
			for (Buff b : t.getBuffManager().getEffectiveBuffs(Buff.SCIENCE_RATE)) {
				out.add(CivColor.Green+"From "+b.getSource()+": "+CivColor.LightGreen+b.getDisplayDouble());
			}
		}
		
	/*	for (Town t : civ.getTowns()) {
			for (BonusGoodie goodie : t.getEffectiveBonusGoodies()) {
				try {
					double bonus = Double.valueOf(goodie.getBonusValue("beaker_bonus"));
					out.add(CivColor.Green+"From Goodie "+goodie.getDisplayName()+": "+CivColor.LightGreen+(bonus*100)+"%");
					
				} catch (NumberFormatException e) {
					//Ignore this goodie might not have the bonus.
				}
				
				try {
					double bonus = Double.valueOf(goodie.getBonusValue("extra_beakers"));
					out.add(CivColor.Green+"From Goodie "+goodie.getDisplayName()+": "+CivColor.LightGreen+bonus);
					
				} catch (NumberFormatException e) {
					//Ignore this goodie might not have the bonus.
				}				
			}
		}*/
		
		out.add(CivColor.LightBlue+"------------------------------------");
		out.add(CivColor.Green+"Total: "+CivColor.LightGreen+df.format(civ.getBeakers()));	
		CivMessage.send(sender, out);
	}
	
	public void taxes_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		CivMessage.sendHeading(sender, "Town Tax Info");
		for (Town t : civ.getTowns()) {
			CivMessage.send(sender, CivColor.Green+"Town:"+CivColor.LightGreen+t.getName()+CivColor.Green+
					" Total: "+CivColor.LightGreen+civ.lastTaxesPaidMap.get(t.getName()));
		}
		
	}
	
	private double getTownTotalLastTick(Town town, Civilization civ) {
		double total = 0;
		for (String key : civ.lastUpkeepPaidMap.keySet()) {
			String townName = key.split(",")[0];
			
			if (townName.equalsIgnoreCase(town.getName())) {
				total += civ.lastUpkeepPaidMap.get(key);
			}
		}
		return total;
	}
	
	public void upkeep_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		if (args.length < 2) {		
			CivMessage.sendHeading(sender, civ.getName()+" Upkeep Info");
	
			for (Town town : civ.getTowns()) {
				CivMessage.send(sender, CivColor.Green+"Town:"+CivColor.LightGreen+town.getName()+CivColor.Green+
													" Total: "+CivColor.LightGreen+getTownTotalLastTick(town, civ));
			}
			CivMessage.send(sender, CivColor.Green+"War: "+CivColor.LightGreen+df.format(civ.getWarUpkeep()));
			
			CivMessage.send(sender, CivColor.LightGray+"Shows upkeep paid for last tick.");
			CivMessage.send(sender, CivColor.LightGray+"Use /civ info upkeep <town name> to show a breakdown per town.");
			
			return;
		} else {
	
			Town town = civ.getTown(args[1]);
			if (town == null) {
				throw new CivException("Civilization has no town named "+args[1]);
			}
			
			CivMessage.sendHeading(sender, "Town of "+town.getName()+"  Upkeep Details");
			CivMessage.send(sender, CivColor.Green+"Base: "+CivColor.LightGreen+civ.getUpkeepPaid(town, "base"));
			CivMessage.send(sender, CivColor.Green+"Distance: "+CivColor.LightGreen+civ.getUpkeepPaid(town, "distance"));
			CivMessage.send(sender, CivColor.Green+"DistanceUpkeep: "+CivColor.LightGreen+civ.getUpkeepPaid(town, "distanceUpkeep"));
			CivMessage.send(sender, CivColor.Green+"Debt: "+CivColor.LightGreen+civ.getUpkeepPaid(town, "debt"));
			CivMessage.send(sender, CivColor.Green+"Total: "+CivColor.LightGreen+getTownTotalLastTick(town, civ));

			CivMessage.send(sender, CivColor.LightGray+"Shows upkeep paid for last tick.");
		}

		
	}
	

	@Override
	public void doDefaultAction() throws CivException {
		show_info();
		CivMessage.send(sender, CivColor.LightGray+"Subcommands available: See /civ info help");
	}
	
	public static void show(CommandSender sender, Resident resident, Civilization civ) {
		
		boolean isOP = false;
		if (sender instanceof Player) {
			Player player;
			try {
				player = CivGlobal.getPlayer(resident);
				if (player.isOp()) {
					isOP = true;
				}
			} catch (CivException e) {
				/* Allow console to display. */
			}
		}	else {
			/* We're the console. */
			isOP = true;
		}
		
		
		CivMessage.sendHeading(sender, "Civilization of "+civ.getName());
		
		CivMessage.send(sender, CivColor.Green+"Score: "+CivColor.LightGreen+civ.getScore()+
				CivColor.Green+" Towns: "+CivColor.LightGreen+civ.getTownCount());
		if (civ.getLeaderGroup() == null) {
			CivMessage.send(sender, CivColor.Green+"Leaders: "+CivColor.Rose+"NONE");
		} else {
			CivMessage.send(sender, CivColor.Green+"Leaders: "+CivColor.LightGreen+civ.getLeaderGroup().getMembersString());
		}
		
		if (civ.getAdviserGroup() == null) {
			CivMessage.send(sender, CivColor.Green+"Advisers: "+CivColor.Rose+"NONE");
		} else {
			CivMessage.send(sender, CivColor.Green+"Advisers: "+CivColor.LightGreen+civ.getAdviserGroup().getMembersString());
		}
	    
	    if (resident == null || civ.hasResident(resident)) {
	    	CivMessage.send(sender, CivColor.Green+"Income Tax Rate: "+CivColor.LightGreen+civ.getIncomeTaxRateString()+
					CivColor.Green+" Science Percentage: "+CivColor.LightGreen+DecimalHelper.formatPercentage(civ.getSciencePercentage()));
			CivMessage.send(sender ,CivColor.Green+"Beakers: "+CivColor.LightGreen+civ.getBeakers()+
					CivColor.Green+" Online: "+CivColor.LightGreen+civ.getOnlineResidents().size());
	    }
		
		if (resident == null || civ.getLeaderGroup().hasMember(resident) || civ.getAdviserGroup().hasMember(resident) || isOP) {
			CivMessage.send(sender, CivColor.Green+"Treasury: "+CivColor.LightGreen+civ.getTreasury().getBalance()+CivColor.Green+" coins.");
		}
		
		if (civ.getTreasury().inDebt()) {
			CivMessage.send(sender, CivColor.Yellow+"In Debt: "+civ.getTreasury().getDebt()+" coins.");	
			CivMessage.send(sender, CivColor.Yellow+civ.getDaysLeftWarning());
		}
		
		for (EndGameCondition endCond : EndGameCondition.endConditions) {
			ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(endCond.getSessionKey());
			if (entries.size() == 0) {
				continue;
			}
			
			for (SessionEntry entry : entries) {
				if (civ == EndGameCondition.getCivFromSessionData(entry.value)) {
					Integer daysLeft = endCond.getDaysToHold() - endCond.getDaysHeldFromSessionData(entry.value);
					CivMessage.send(sender, CivColor.LightBlue+CivColor.BOLD+civ.getName()+CivColor.White+" is "+
					CivColor.Yellow+CivColor.BOLD+daysLeft+CivColor.White+" days away from a "+CivColor.LightPurple+CivColor.BOLD+endCond.getVictoryName()+
					CivColor.White+" victory!");
					break;
				}
			}
		}
		
		Integer votes = EndConditionDiplomacy.getVotesFor(civ);
		if (votes > 0) {
			CivMessage.send(sender, CivColor.LightBlue+CivColor.BOLD+civ.getName()+CivColor.White+" has "+
					CivColor.LightPurple+CivColor.BOLD+votes+CivColor.White+" diplomatic votes");
		}
		
		Double beakers = EndConditionScience.getBeakersFor(civ);
		if (beakers > 0) {
			DecimalFormat df = new DecimalFormat("#.#");
			CivMessage.send(sender, CivColor.LightBlue+CivColor.BOLD+civ.getName()+CivColor.White+" has "+
					CivColor.LightPurple+CivColor.BOLD+df.format(beakers)+CivColor.White+" beakers on The Enlightenment.");			
		}
		
		String out = CivColor.Green+"Towns: ";
		for (Town town : civ.getTowns()) {
			if (town.isCapitol()) {
				out += CivColor.Gold+town.getName();
			} else if (town.getMotherCiv() != null) {
				out += CivColor.Yellow+town.getName();
			} else {
				out += CivColor.White+town.getName();
			}
			out += ", ";
		}
		
		CivMessage.send(sender, out);
	}
	
	public void show_info() throws CivException {
		Civilization civ = getSenderCiv();
		Resident resident = getResident();
		show(sender, resident, civ);
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		
	}

	
}
