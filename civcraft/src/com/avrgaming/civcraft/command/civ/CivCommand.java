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

import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.TimeZone;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.endgame.EndConditionDiplomacy;
import com.avrgaming.civcraft.endgame.EndGameCondition;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Relation.Status;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;

public class CivCommand extends CommandBase {

	@Override
	public void init() {		
		command = "/civ";
		displayName = "Civ";
		
		commands.put("townlist", "Shows a list of all towns in the civilization.");
		commands.put("deposit", "[amount] - deposits this amount into the civ's treasury.");
		commands.put("withdraw", "[amount] - withdraws this amount from the civ's treasury.");
		commands.put("info", "Shows information about this Civilization");
		commands.put("show", "[name] gives you information about the civ named [name].");
		commands.put("list", "(name) - shows all civs in the world, or the towns for the civ named (name).");
		commands.put("research", "Manage civilization's research.");
		commands.put("gov", "Manage your civilizations government.");
		commands.put("time", "View information about upcoming events.");
		commands.put("set", "Set various civilization properties such as taxes and border color");
		commands.put("group", "Manage the leaders and advisers group.");
		commands.put("dip", "Manage civilization's diplomacy.");
		commands.put("victory", "Show which civs are close to victory.");
		commands.put("votes", "Shows the diplomatic votes for all civs.");
		commands.put("top5", "Show the top 5 civilizations in the world.");
		commands.put("disbandtown", "[town] Disbands this town. Mayor must also issue /town disbandtown");
		commands.put("revolution", "stages a revolution for the mother civilization!");
		commands.put("claimleader", "claim yourself as leader of this civ. All current leaders must be inactive.");
	}
	
	public void claimleader_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		Resident resident = getResident();		
		
		if (!civ.areLeadersInactive()) {
			throw new CivException("At least one leader is not inactive for your civ. Cannot claim leadership.");
		}
		
		civ.getLeaderGroup().addMember(resident);
		civ.getLeaderGroup().save();
		CivMessage.sendSuccess(sender, "You are now a leader in "+civ.getName());
		CivMessage.sendCiv(civ, resident.getName()+" has assumed control of the civilization due to inactive leadership.");
	}
	
	public void votes_cmd() throws CivException {
		
		CivMessage.sendHeading(sender, "Diplomatic Votes");
		for (Civilization civ : CivGlobal.getCivs()) {
			Integer votes = EndConditionDiplomacy.getVotesFor(civ);
			if (votes != 0) {
				CivMessage.send(sender, CivColor.LightBlue+
						CivColor.BOLD+civ.getName()+CivColor.White+" has "+
						CivColor.LightPurple+CivColor.BOLD+votes+CivColor.White+" votes");
			}
		}
	}
	
	public void victory_cmd() {
		
		CivMessage.sendHeading(sender, "Civs Close To Victory");
		boolean anybody = false;

		for (EndGameCondition endCond : EndGameCondition.endConditions) {
			ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(endCond.getSessionKey());
			if (entries.size() == 0) {
				continue;
			}
			
			anybody = true;
			for (SessionEntry entry : entries) {
				Civilization civ = EndGameCondition.getCivFromSessionData(entry.value);
				Integer daysLeft = endCond.getDaysToHold() - endCond.getDaysHeldFromSessionData(entry.value);
				CivMessage.send(sender, CivColor.LightBlue+CivColor.BOLD+civ.getName()+CivColor.White+" is "+
				CivColor.Yellow+CivColor.BOLD+daysLeft+CivColor.White+" days away from a "+CivColor.LightPurple+CivColor.BOLD+endCond.getVictoryName()+
				CivColor.White+" victory!");
			}
		}
		
		if (!anybody) {
			CivMessage.send(sender, CivColor.LightGray+"Nobody yet...");
		}
		
	}
	
	public void revolution_cmd() throws CivException {
		Town town = getSelectedTown();
		
		if (War.isWarTime() || War.isWithinWarDeclareDays()) {
			throw new CivException("Can not start a revolution during WarTime or "+War.getTimeDeclareDays()+" days before WarTime");
		}
		
		if (town.getMotherCiv() == null) {
			throw new CivException("Cannot start a revolution unless captured by another civilization.");
		}
		
		Civilization motherCiv = town.getMotherCiv();
		
		if (!motherCiv.getCapitolName().equals(town.getName())) {
			throw new CivException("Can only start a revolution from your mother civilization's capitol town("+motherCiv.getCapitolName()+").");
		}
		
		
		try {
			int revolution_cooldown = CivSettings.getInteger(CivSettings.civConfig, "civ.revolution_cooldown");
		
			Calendar cal = Calendar.getInstance();
			Calendar revCal = Calendar.getInstance();
			
			Date conquered = town.getMotherCiv().getConqueredDate();
			if (conquered == null) {
				throw new CivException("You must have been conquered to start a revolution.");
			}
			
			revCal.setTime(town.getMotherCiv().getConqueredDate());
			revCal.add(Calendar.DAY_OF_MONTH, revolution_cooldown);
			
			if (!cal.after(revCal)) {
				throw new CivException("Cannot start a revolution within "+revolution_cooldown+" of being conquered.");
			}
			
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			throw new CivException("Internal Configuration Error.");
		}
		
		
		double revolutionFee = motherCiv.getRevolutionFee();
		
		if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"For a measly "+revolutionFee+" we could fund a revolution and get our old civ back!");
			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"Funding a revolution will put us AT WAR with any town that currently owns one of our native towns. To win the revolution, we" +
					"only need to survive the wars. Are you sure you want to do this?");
			CivMessage.send(sender, CivColor.LightGreen+"Type '/civ revolution yes' to start the revolution.");
			return;
		}
		
		if(!town.getTreasury().hasEnough(revolutionFee)) {
			throw new CivException("The capitol town doesnt have the required "+revolutionFee+" in order to start a revolution.");
		}

		/* Starting a revolution! Give back all of our towns to us. */
		HashSet<String> warCivs = new HashSet<String>(); 
		for (Town t : CivGlobal.getTowns()) {
			if (t.getMotherCiv() == motherCiv) {
				warCivs.add(t.getCiv().getName());
				t.changeCiv(motherCiv);
				t.setMotherCiv(null);
				t.save();
			}
		}
		
		for (String warCivName : warCivs) {
			Civilization civ = CivGlobal.getCiv(warCivName);
			if (civ != null) {
				CivGlobal.setRelation(civ, motherCiv, Status.WAR);
				/* THEY are the aggressor in a revolution. */
				CivGlobal.setAggressor(civ, motherCiv, civ);
			}
		}
		
		motherCiv.setConquered(false);
		CivGlobal.removeConqueredCiv(motherCiv);
		CivGlobal.addCiv(motherCiv);
		motherCiv.save();
		
		
		town.getTreasury().withdraw(revolutionFee);
		CivMessage.global(CivColor.Yellow+ChatColor.BOLD+"The civilization of "+motherCiv.getName()+" demands its freedom and has started a revolution! It has declared WAR on any civ that owns its old towns.");
		CivMessage.global(CivColor.Rose+"RED!"+CivColor.LightGreen+" The blood of angry men! "+CivColor.LightGray+"BLACK!"+CivColor.LightGreen+" The dark of ages past!");
		CivMessage.global(CivColor.Rose+"RED!"+CivColor.LightGreen+" A world about to dawn! "+CivColor.LightGray+"BLACK!"+CivColor.LightGreen+" The night that ends at last!");

	}
	
//	public void capitulate_cmd() throws CivException {
//		Town town = getSelectedTown();
//		Resident resident = getResident();
//		
//		if (town.getMotherCiv() == null) {
//			throw new CivException("Cannot capitulate unless captured by another civilization.");
//		}
//		
//		Civilization motherCiv = town.getMotherCiv();
//		
//		if (!town.getMotherCiv().getCapitolName().equals(town.getName())) {
//			throw new CivException("Can only capitulate your entire civ from the capitol town.");
//		}
//		
//		if (!town.getMotherCiv().getLeaderGroup().hasMember(resident)) {
//			throw new CivException("You must be the leader of the captured civilization in order to capitulate.");
//		}
//		
//		if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
//			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"Capitualting means that this civ will be DELETED and all of its towns will become a normal towns in "+
//					town.getCiv().getName()+" and can no longer revolt. Are you sure?");
//			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"If you're sure, type /civ capitulate yes");
//			return;
//		}
//		
//
//	}
	
	public void disbandtown_cmd() throws CivException {
		this.validLeaderAdvisor();
		Town town = this.getNamedTown(1);
		
		if (town.getMotherCiv() != null) {
			throw new CivException("You cannot disband towns that are currently conquered.");
		}
		
		if (town.leaderWantsToDisband) {
			town.leaderWantsToDisband = false;
			CivMessage.send(sender, "No longer want to disband.");
			return;
		}	
		
		town.leaderWantsToDisband = true;		

		if (town.leaderWantsToDisband && town.mayorWantsToDisband) {
			CivMessage.sendCiv(town.getCiv(), "Town "+town.getName()+" is being disbanded by agreement from the civ leader and the mayor");
			town.disband();
		}
		
		CivMessage.send(sender, CivColor.Yellow+"Waiting on mayor to type /town disbandtown");
	}
	
	public void top5_cmd() {	
		CivMessage.sendHeading(sender, "Top 5 Civilizations");
//		TreeMap<Integer, Civilization> scores = new TreeMap<Integer, Civilization>();
//		
//		for (Civilization civ : CivGlobal.getCivs()) {
//			if (civ.isAdminCiv()) {
//				continue;
//			}
//			scores.put(civ.getScore(), civ);
//		}
		
		synchronized(CivGlobal.civilizationScores) {
			int i = 1;
			for (Integer score : CivGlobal.civilizationScores.descendingKeySet()) {
				CivMessage.send(sender, i+") "+CivColor.Gold+CivGlobal.civilizationScores.get(score).getName()+CivColor.White+" - "+score+" points");
				i++;
				if (i > 5) {
					break;
				}
			}
		}
		
	}
	
	public void dip_cmd() {
		CivDiplomacyCommand cmd = new CivDiplomacyCommand();	
		cmd.onCommand(sender, null, "dip", this.stripArgs(args, 1));
	}
	
	public void group_cmd() {
		CivGroupCommand cmd = new CivGroupCommand();	
		cmd.onCommand(sender, null, "group", this.stripArgs(args, 1));	
	}
	
	public void set_cmd() {
		CivSetCommand cmd = new CivSetCommand();	
		cmd.onCommand(sender, null, "set", this.stripArgs(args, 1));	
	}
	
	public void time_cmd() throws CivException {
		CivMessage.sendHeading(sender, "CivCraft Timers");
		Resident resident = getResident();
		ArrayList<String> out = new ArrayList<String>();
		SimpleDateFormat sdf = new SimpleDateFormat("M/dd h:mm:ss a z");
		
		Calendar cal = Calendar.getInstance();
		cal.setTimeZone(TimeZone.getTimeZone(resident.getTimezone()));
		sdf.setTimeZone(cal.getTimeZone());
		
		
		out.add(CivColor.Green+"Current Server Time: "+CivColor.LightGreen+sdf.format(cal.getTime()));
		
		cal.setTime(CivGlobal.getNextUpkeepDate());
		out.add(CivColor.Green+"Next Upkeep: "+CivColor.LightGreen+sdf.format(cal.getTime()));
		
		cal.setTime(CivGlobal.getNextHourlyTickDate());
		out.add(CivColor.Green+"Next Hourly Tick: "+CivColor.LightGreen+sdf.format(cal.getTime()));
		
		cal.setTime(CivGlobal.getNextRepoTime());
		out.add(CivColor.Green+"Next Trade Good Repo: "+CivColor.LightGreen+sdf.format(cal.getTime()));
		
		if (War.isWarTime()) {
			out.add(CivColor.Yellow+"WarTime is now!");
			cal.setTime(War.getStart());
			out.add(CivColor.Yellow+"    Started: "+CivColor.LightGreen+sdf.format(cal.getTime()));
			
			cal.setTime(War.getEnd());
			out.add(CivColor.Yellow+"    Ends: "+CivColor.LightGreen+sdf.format(cal.getTime()));
		} else {
			cal.setTime(War.getNextWarTime());
			out.add(CivColor.Green+"Next WarTime: "+CivColor.LightGreen+sdf.format(cal.getTime()));
		}
		
		Player player = null;
		try {
			player = getPlayer();
		} catch (CivException e) {
		}

		if (player == null || player.hasPermission(CivSettings.MINI_ADMIN) || player.isOp()) {
			cal.setTime(CivGlobal.getTodaysSpawnRegenDate());
			out.add(CivColor.LightPurple+"Next Spawn Regen: "+CivColor.LightGreen+sdf.format(cal.getTime()));
			
			cal.setTime(CivGlobal.getNextRandomEventTime());
			out.add(CivColor.LightPurple+"Next Random Event: "+CivColor.LightGreen+sdf.format(cal.getTime()));
		}
		
		CivMessage.send(sender, out);
	}
	
	public void gov_cmd() {
		CivGovCommand cmd = new CivGovCommand();	
		cmd.onCommand(sender, null, "gov", this.stripArgs(args, 1));	
	}
	
	public void research_cmd() {
		CivResearchCommand cmd = new CivResearchCommand();	
		cmd.onCommand(sender, null, "research", this.stripArgs(args, 1));	
	}
	
	public void list_cmd() throws CivException {
		if (args.length < 2) {	
			String out = "";
			CivMessage.sendHeading(sender, "Civs in the World");
			for (Civilization civ : CivGlobal.getCivs()) {
				out += civ.getName()+", ";
			}
			
			CivMessage.send(sender, out);
			return;
		}
		
		Civilization civ = getNamedCiv(1);
		
		String out = "";
		CivMessage.sendHeading(sender, "Towns in "+args[1]);
		
		for (Town t : civ.getTowns()) {
			out += t.getName()+", ";
		}
		
		CivMessage.send(sender, out);
	}
	
	public void show_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("You need to enter the civ name you wish to know about.");
		}
		
		Civilization civ = getNamedCiv(1);
		if (sender instanceof Player) {
			CivInfoCommand.show(sender, getResident(), civ);
		} else {
			CivInfoCommand.show(sender, null, civ);
		}
	}
	
	public void deposit_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter the amount you want to deposit.");
		}
		
		Resident resident = getResident();
		Civilization civ = getSenderCiv();
		
		try {
			Double amount = Double.valueOf(args[1]);
			if (amount < 1) {
				throw new CivException("Cannot deposit less than 1");
			}
			amount = Math.floor(amount);
			
			civ.depositFromResident(resident, Double.valueOf(args[1]));			
			
		} catch (NumberFormatException e) {
			throw new CivException(args[1]+" is not a valid number.");
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException("Internal Database Exception");
		}
		
		CivMessage.sendSuccess(sender, "Deposited "+args[1]+" coins.");
	}

	public void withdraw_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter the amount you want to withdraw.");
		}
		
		Civilization civ = getSenderCiv();
		Resident resident = getResident();
		
		if (!civ.getLeaderGroup().hasMember(resident)) {
			throw new CivException("Only leaders can use this command.");
		}
		
		try {
			Double amount = Double.valueOf(args[1]);
			if (amount < 1) {
				throw new CivException("Cannot withdraw less than 1");
			}
			amount = Math.floor(amount);
			
			if(!civ.getTreasury().payTo(resident.getTreasury(), Double.valueOf(args[1]))) {
				throw new CivException("The civ does not have that much.");
			}
		} catch (NumberFormatException e) {
			throw new CivException(args[1]+" is not a valid number.");
		}
		
		CivMessage.sendSuccess(sender, "Withdrew "+args[1]+" coins.");
	}
	
	public void townlist_cmd() throws CivException {
		Civilization civ = getSenderCiv();
		
		CivMessage.sendHeading(sender, civ.getName()+" Town List");
		String out = "";
		for (Town town : civ.getTowns()) {
			out += town.getName()+",";
		}
		CivMessage.send(sender, out);	
	}
	
	public void info_cmd() throws CivException {
		CivInfoCommand cmd = new CivInfoCommand();	
		cmd.onCommand(sender, null, "info", this.stripArgs(args, 1));		
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
	}

	@Override
	public void showHelp() {
		this.showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {
		
	}
	
}
