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
import com.avrgaming.civcraft.main.Colors;
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
				out.add(Colors.Green+"From "+b.getSource()+": "+Colors.LightGreen+b.getDisplayDouble());
			}
		}
		
	/*	for (Town t : civ.getTowns()) {
			for (BonusGoodie goodie : t.getEffectiveBonusGoodies()) {
				try {
					double bonus = Double.valueOf(goodie.getBonusValue("beaker_bonus"));
					out.add(Colors.Green+"From Goodie "+goodie.getDisplayName()+": "+Colors.LightGreen+(bonus*100)+"%");
					
				} catch (NumberFormatException e) {
					//Ignore this goodie might not have the bonus.
				}
				
				try {
					double bonus = Double.valueOf(goodie.getBonusValue("extra_beakers"));
					out.add(Colors.Green+"From Goodie "+goodie.getDisplayName()+": "+Colors.LightGreen+bonus);
					
				} catch (NumberFormatException e) {
					//Ignore this goodie might not have the bonus.
				}				
			}
		}*/
		
		out.add(Colors.LightBlue+"------------------------------------");
		out.add(Colors.Green+"Total: "+Colors.LightGreen+df.format(civ.getBeakers()));	
		CivMessage.send(sender, out);
	}
	
	public void taxes_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		CivMessage.sendHeading(sender, "Town Tax Info");
		for (Town t : civ.getTowns()) {
			CivMessage.send(sender, Colors.Green+"Town:"+Colors.LightGreen+t.getName()+Colors.Green+
					" Total: "+Colors.LightGreen+civ.lastTaxesPaidMap.get(t.getName()));
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
				CivMessage.send(sender, Colors.Green+"Town:"+Colors.LightGreen+town.getName()+Colors.Green+
													" Total: "+Colors.LightGreen+getTownTotalLastTick(town, civ));
			}
			CivMessage.send(sender, Colors.Green+"War: "+Colors.LightGreen+df.format(civ.getWarUpkeep()));
			
			CivMessage.send(sender, Colors.LightGray+"Shows upkeep paid for last tick.");
			CivMessage.send(sender, Colors.LightGray+"Use /civ info upkeep <town name> to show a breakdown per town.");
			
			return;
		} else {
	
			Town town = civ.getTown(args[1]);
			if (town == null) {
				throw new CivException("Civilization has no town named "+args[1]);
			}
			
			CivMessage.sendHeading(sender, "Town of "+town.getName()+"  Upkeep Details");
			CivMessage.send(sender, Colors.Green+"Base: "+Colors.LightGreen+civ.getUpkeepPaid(town, "base"));
			CivMessage.send(sender, Colors.Green+"Distance: "+Colors.LightGreen+civ.getUpkeepPaid(town, "distance"));
			CivMessage.send(sender, Colors.Green+"DistanceUpkeep: "+Colors.LightGreen+civ.getUpkeepPaid(town, "distanceUpkeep"));
			CivMessage.send(sender, Colors.Green+"Debt: "+Colors.LightGreen+civ.getUpkeepPaid(town, "debt"));
			CivMessage.send(sender, Colors.Green+"Total: "+Colors.LightGreen+getTownTotalLastTick(town, civ));

			CivMessage.send(sender, Colors.LightGray+"Shows upkeep paid for last tick.");
		}

		
	}
	

	@Override
	public void doDefaultAction() throws CivException {
		show_info();
		CivMessage.send(sender, Colors.LightGray+"Subcommands available: See /civ info help");
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
		
		CivMessage.send(sender, Colors.Green+"Score: "+Colors.LightGreen+civ.getScore()+
				Colors.Green+" Towns: "+Colors.LightGreen+civ.getTownCount());
		if (civ.getLeaderGroup() == null) {
			CivMessage.send(sender, Colors.Green+"Leaders: "+Colors.Rose+"NONE");
		} else {
			CivMessage.send(sender, Colors.Green+"Leaders: "+Colors.LightGreen+civ.getLeaderGroup().getMembersString());
		}
		
		if (civ.getAdviserGroup() == null) {
			CivMessage.send(sender, Colors.Green+"Advisers: "+Colors.Rose+"NONE");
		} else {
			CivMessage.send(sender, Colors.Green+"Advisers: "+Colors.LightGreen+civ.getAdviserGroup().getMembersString());
		}
	    
	    if (resident == null || civ.hasResident(resident)) {
	    	CivMessage.send(sender, Colors.Green+"Income Tax Rate: "+Colors.LightGreen+civ.getIncomeTaxRateString()+
					Colors.Green+" Science Percentage: "+Colors.LightGreen+DecimalHelper.formatPercentage(civ.getSciencePercentage()));
			CivMessage.send(sender ,Colors.Green+"Beakers: "+Colors.LightGreen+civ.getBeakers()+
					Colors.Green+" Online: "+Colors.LightGreen+civ.getOnlineResidents().size());
	    }
		
		if (resident == null || civ.getLeaderGroup().hasMember(resident) || civ.getAdviserGroup().hasMember(resident) || isOP) {
			CivMessage.send(sender, Colors.Green+"Treasury: "+Colors.LightGreen+civ.getTreasury().getBalance()+Colors.Green+" coins.");
		}
		
		if (civ.getTreasury().inDebt()) {
			CivMessage.send(sender, Colors.Yellow+"In Debt: "+civ.getTreasury().getDebt()+" coins.");	
			CivMessage.send(sender, Colors.Yellow+civ.getDaysLeftWarning());
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
		
		String out = Colors.Green+"Towns: ";
		for (Town town : civ.getTowns()) {
			if (town.isCapitol()) {
				out += Colors.Gold+town.getName();
			} else if (town.getMotherCiv() != null) {
				out += Colors.Yellow+town.getName();
			} else {
				out += Colors.White+town.getName();
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
