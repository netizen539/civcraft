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
package com.avrgaming.civcraft.command;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.camp.Camp;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.util.CivColor;

public abstract class CommandBase implements CommandExecutor {
	
	private static final int MATCH_LIMIT = 5;

	protected HashMap<String, String> commands = new HashMap<String, String>();
	
	protected String[] args;
	protected CommandSender sender;
	
	protected String command = "FIXME";
	protected String displayName = "FIXME";
	protected boolean sendUnknownToDefault = false;
	protected DecimalFormat df = new DecimalFormat();

	public Town senderTownOverride = null;
	public Civilization senderCivOverride = null;
	
	public abstract void init();
	
	/* Called when no arguments are passed. */
	public abstract void doDefaultAction() throws CivException;
	
	/* Called on syntax error. */
	public abstract void showHelp();
	
	/* Called before command is executed to check permissions. */
	public abstract void permissionCheck() throws CivException;
	
	@Override
	public boolean onCommand(CommandSender sender, Command cmd, String commandLabel, String[] args) {
		init();
		
		this.args = args;
		this.sender = sender;
		
		try {
			permissionCheck();
		} catch (CivException e1) {
			CivMessage.sendError(sender, e1.getMessage());
			return false;
		}
		
		doLogging();
		
		if (args.length == 0) {
			try {
				doDefaultAction();
			} catch (CivException e) {
				CivMessage.sendError(sender, e.getMessage());
			}
			return false;
		}
		
		if (args[0].equalsIgnoreCase("help")) {
			showHelp();
			return true;
		}
		
		for (String c : commands.keySet()) {
		  if (c.equalsIgnoreCase(args[0])) {
				try { 
					Method method = this.getClass().getMethod(args[0].toLowerCase()+"_cmd");
					try {
						method.invoke(this);
						return true;
					} catch (IllegalAccessException | IllegalArgumentException e) {
						e.printStackTrace();
						CivMessage.sendError(sender, "Internal Command Error.");
					} catch (InvocationTargetException e) {
						if (e.getCause() instanceof CivException) {
							CivMessage.sendError(sender, e.getCause().getMessage());
						} else {
							CivMessage.sendError(sender, "Internal Command Error.");
							e.getCause().printStackTrace();
						}
					}

					
				} catch (NoSuchMethodException e) {
					if (sendUnknownToDefault) {
						try {
							doDefaultAction();
						} catch (CivException e1) {
							CivMessage.sendError(sender, e.getMessage());
						}
						return false;
					}
					CivMessage.sendError(sender, "Unknown method "+args[0]);
				}
				return true;
			}
		}
		
		if (sendUnknownToDefault) {
			try {
				doDefaultAction();
			} catch (CivException e) {
				CivMessage.sendError(sender, e.getMessage());
			}
			return false;
		}
		
		CivMessage.sendError(sender, "Unknown command "+args[0]);
		return false;
	}
	
	
	public void doLogging() {	
	}
	
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		List<String> al = new ArrayList<String>();
		al.add("sub1");
		al.add("barg");
		al.add("borg");
		return al;
	}

	public void showBasicHelp() {
		CivMessage.sendHeading(sender, displayName+" Command Help");
		for (String c : commands.keySet()) {
			String info = commands.get(c);
			
			info = info.replace("[", CivColor.Yellow+"[");
			info = info.replace("]", "]"+CivColor.LightGray);
			info = info.replace("(", CivColor.Yellow+"(");
			info = info.replace(")", ")"+CivColor.LightGray);
						
			CivMessage.send(sender, CivColor.LightPurple+command+" "+c+CivColor.LightGray+" "+info);
		}
	}
	
	public Resident getResident() throws CivException {
		Player player = getPlayer();
		Resident res = CivGlobal.getResident(player);
		if (res == null) {
			throw new CivException("Resident "+player.getName()+" could not be found.");
		}
		return res;
	}
	
	public Player getPlayer() throws CivException {
		if (sender instanceof Player) {
			return (Player)sender;
		}
		throw new CivException("Only players can do this.");
	}
	
	public Town getSelectedTown() throws CivException {
		if (senderTownOverride != null) {
			return senderTownOverride;
		}
		
		if (sender instanceof Player) {
			Player player = (Player)sender;
			Resident res = CivGlobal.getResident(player);
			if (res != null && res.getTown() != null) {
				
				if (res.getSelectedTown() != null) {
					try {
						res.getSelectedTown().validateResidentSelect(res);
					} catch (CivException e) {
						CivMessage.send(player, CivColor.Yellow+"You can no longer use the selected town "+res.getSelectedTown().getName()+", switched back to "+res.getTown().getName());
						res.setSelectedTown(res.getTown());
						return res.getTown();
					}
					
					return res.getSelectedTown();
				} else {
					return res.getTown();
				}
			}
		}
		throw new CivException("You are not part of a town.");
	}
	
	public TownChunk getStandingTownChunk() throws CivException {
		Player player = getPlayer();
		
		TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
		if (tc == null) {
			throw new CivException("This plot is not owned.");
		}
		return tc;
	}
	
	protected String[] stripArgs(String[] someArgs, int amount) {
		if (amount >= someArgs.length) {
			return new String[0];
		}
		
		
		String[] argsLeft = new String[someArgs.length - amount];
		for (int i = 0; i < argsLeft.length; i++) {
			argsLeft[i] = someArgs[i+amount];
		}
		
		return argsLeft;
	}
	
	protected String combineArgs(String[] someArgs) {
		String combined = "";
		for (String str : someArgs) {
			combined += str + " ";
		}
		combined = combined.trim();
		return combined;
	}
	
	public void validMayor() throws CivException {
		Player player = getPlayer();
		Town town = getSelectedTown();
		
		if (!town.playerIsInGroupName("mayors", player)) {
			throw new CivException("Only mayors can use this command.");
		}
		//if (this.)
	}
	
	public void validMayorAssistantLeader() throws CivException {
		Resident resident = getResident();
		Town town = getSelectedTown();
		Civilization civ;
		
		/* 
		 * If we're using a selected town that isn't ours validate based on the mother civ.
		 */
		if (town.getMotherCiv() != null) {
			civ = town.getMotherCiv();
		} else {
			civ = getSenderCiv();
		}
		
		if (town.getMayorGroup() == null || town.getAssistantGroup() == null || 
				civ.getLeaderGroup() == null) {
			throw new CivException("ERROR: This town("+town.getName()+") or civ("+civ.getName()+") is missing a special group. Please contact and admin.");
		}
		
		if (!town.getMayorGroup().hasMember(resident) && !town.getAssistantGroup().hasMember(resident) &&
				!civ.getLeaderGroup().hasMember(resident)) {
			throw new CivException("Only mayors, assistants and civ leaders of the mother civilization can use this command.");
		}
	}
	
	public void validLeaderAdvisor() throws CivException {
		Resident res = getResident();
		Civilization civ = getSenderCiv();

		
		if (!civ.getLeaderGroup().hasMember(res) && !civ.getAdviserGroup().hasMember(res)) {
			throw new CivException("Only leaders and advisers can use this command.");
		}
	}
	
	public void validLeader() throws CivException {
		Resident res = getResident();
		Civilization civ = getSenderCiv();
		
		if (!civ.getLeaderGroup().hasMember(res)) {
			throw new CivException("Only leaders can use this command.");
		}
	}
	
	public void validPlotOwner() throws CivException {
		Resident resident = getResident();
		TownChunk tc = getStandingTownChunk();
		
		if (tc.perms.getOwner() == null) {
			validMayorAssistantLeader();
			if (tc.getTown() != resident.getTown()) {
				throw new CivException("You cannot manage a plot not in your town.");
			}
		} else {
			if (resident != tc.perms.getOwner()) {
				throw new CivException("You are not the owner of this plot.");
			}
		}	
	}
	
	public Civilization getSenderCiv() throws CivException {
		
		if (this.senderCivOverride != null) {
			return this.senderCivOverride;
		}
		
		Resident resident = getResident();
		
		if (resident.getTown() == null) {
			throw new CivException("You are not a citizen of a civilization.");
		}
				
		if (resident.getTown().getCiv() == null) {
			//This should never happen but....
			throw new CivException("You are not a citizen of a civilization.");
		}
		
		return resident.getTown().getCiv();
	}

	protected Double getNamedDouble(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException("Enter a number.");
		}
		
		try {
			Double number = Double.valueOf(args[index]);
			return number;
		} catch (NumberFormatException e) {
			throw new CivException(args[index]+" is not a number.");
		}
		
	}
	
	protected Integer getNamedInteger(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException("Enter a number.");
		}
		
		try {
			Integer number = Integer.valueOf(args[index]);
			return number;
		} catch (NumberFormatException e) {
			throw new CivException(args[index]+" is not whole a number.");
		}
		
	}
	
	protected Resident getNamedResident(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException("Enter a resident name.");
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");
				
		ArrayList<Resident> potentialMatches = new ArrayList<Resident>();
		for (Resident resident : CivGlobal.getResidents()) {
			String str = resident.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(resident);
				}
			} catch (Exception e) {
				throw new CivException("Invalid pattern.");
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException("Too many potential matches. Refine your search.");
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException("No resident matching that name.");
		}
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+"Potential Matches");
			CivMessage.send(sender, " ");
			String out = "";
			for (Resident resident : potentialMatches) {
				out += resident.getName()+", ";
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException("More than one resident matches, please clarify.");
		}
		
		return potentialMatches.get(0);
	}
	
	protected Civilization getNamedCiv(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException("Enter a civ name.");
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");
				
		ArrayList<Civilization> potentialMatches = new ArrayList<Civilization>();
		for (Civilization civ : CivGlobal.getCivs()) {
			String str = civ.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(civ);
				}
			} catch (Exception e) {
				throw new CivException("Invalid pattern.");
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException("Too many potential matches. Refine your search.");
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException("No civ matching the name '"+args[index]+"'");
		}
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+"Potential Matches");
			CivMessage.send(sender, " ");
			String out = "";
			for (Civilization civ : potentialMatches) {
				out += civ.getName()+", ";
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException("More than one civ matches, please clarify.");
		}
		
		return potentialMatches.get(0);
	}
	
	protected Civilization getNamedCapturedCiv(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException("Enter a civ name.");
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");
				
		ArrayList<Civilization> potentialMatches = new ArrayList<Civilization>();
		for (Civilization civ : CivGlobal.getConqueredCivs()) {
			String str = civ.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(civ);
				}
			} catch (Exception e) {
				throw new CivException("Invalid pattern.");
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException("Too many potential matches. Refine your search.");
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException("No civ matching the name '"+args[index]+"'");
		}
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+"Potential Matches");
			CivMessage.send(sender, " ");
			String out = "";
			for (Civilization civ : potentialMatches) {
				out += civ.getName()+", ";
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException("More than one civ matches, please clarify.");
		}
		
		return potentialMatches.get(0);
	}
//	protected Town getNamedTown(int index) throws CivException {
//		if (args.length < (index+1)) {
//			throw new CivException("Enter a town name");
//		}
//		
//		Town town = CivGlobal.getTown(args[index]);
//		if (town == null) {
//			throw new CivException("No town named:"+args[index]);
//		}
//		
//		return town;
//	}
	
	protected Town getNamedTown(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException("Enter a town name.");
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");
				
		ArrayList<Town> potentialMatches = new ArrayList<Town>();
		for (Town town : CivGlobal.getTowns()) {
			String str = town.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(town);
				}
			} catch (Exception e) {
				throw new CivException("Invalid pattern.");
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException("Too many potential matches. Refine your search.");
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException("No town matching that name.");
		}
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+"Potential Matches");
			CivMessage.send(sender, " ");
			String out = "";
			for (Town town : potentialMatches) {
				out += town.getName()+", ";
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException("More than one town matches, please clarify.");
		}
		
		return potentialMatches.get(0);
	}
	
	public String getNamedString(int index, String message) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException(message);
		}
		
		return args[index];
	}
	
	@SuppressWarnings("deprecation")
	protected OfflinePlayer getNamedOfflinePlayer(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException("Enter a player name");
		}
		
		OfflinePlayer offplayer = Bukkit.getOfflinePlayer(args[index]);
		if (offplayer == null) {
			throw new CivException("No player named:"+args[index]);
		}
		
		return offplayer;
	}
	
	public String makeInfoString(HashMap<String, String> kvs, String lowColor, String highColor) {
		
		String out = "";
		for (String key : kvs.keySet()) {
			out += lowColor+key+": "+highColor+kvs.get(key)+" ";
		}
		
		return out;
	}
	
	protected PermissionGroup getNamedPermissionGroup(Town town, int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException("Enter a group name");
		}
		
		PermissionGroup grp = CivGlobal.getPermissionGroupFromName(town, args[index]);
		if (grp == null) {
			throw new CivException("No group named:"+args[index]+" in town "+town.getName());
		}
		
		return grp;
	}
	
	protected void validCampOwner() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasCamp()) {
			throw new CivException("You are not currently in a camp.");
		}
		
		if (resident.getCamp().getOwner() != resident) {
			throw new CivException("Only the owner of the camp("+resident.getCamp().getOwnerName()+") is allowed to do this.");
		}
	}
	
	protected Camp getCurrentCamp() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasCamp()) {
			throw new CivException("You are not currently in a camp.");
		}
		
		return resident.getCamp();
	}
	
	protected Camp getNamedCamp(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException("Enter a camp name.");
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");
		
		ArrayList<Camp> potentialMatches = new ArrayList<Camp>();
		for (Camp camp : CivGlobal.getCamps()) {
			String str = camp.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(camp);
				}
			} catch (Exception e) {
				throw new CivException("Invalid pattern.");
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException("Too many potential matches. Refine your search.");
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException("No camp matching that name.");
		}
		
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+"Potential Matches");
			CivMessage.send(sender, " ");
			String out = "";
			for (Camp camp : potentialMatches) {
				out += camp.getName()+", ";
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException("More than one camp matches, please clarify.");
		}
		
		return potentialMatches.get(0);
	}
	
	protected ArenaTeam getNamedTeam(int index) throws CivException {
		if (args.length < (index+1)) {
			throw new CivException("Enter a team name.");
		}
		
		String name = args[index].toLowerCase();
		name = name.replace("%", "(\\w*)");
				
		ArrayList<ArenaTeam> potentialMatches = new ArrayList<ArenaTeam>();
		for (ArenaTeam team : ArenaTeam.arenaTeams.values()) {
			String str = team.getName().toLowerCase();
			try {
				if (str.matches(name)) {
					potentialMatches.add(team);
				}
			} catch (Exception e) {
				throw new CivException("Invalid pattern.");
			}
			
			if (potentialMatches.size() > MATCH_LIMIT) {
				throw new CivException("Too many potential matches. Refine your search.");
			}
		}
		
		if (potentialMatches.size() == 0) {
			throw new CivException("No team matching that name.");
		}
		
		if (potentialMatches.size() != 1) {
			CivMessage.send(sender, CivColor.LightPurple+ChatColor.UNDERLINE+"Potential Matches");
			CivMessage.send(sender, " ");
			String out = "";
			for (ArenaTeam team : potentialMatches) {
				out += team.getName()+", ";
			}
		
			CivMessage.send(sender, CivColor.LightBlue+ChatColor.ITALIC+out);
			throw new CivException("More than one team matches, please clarify.");
		}
		
		return potentialMatches.get(0);
	}
	
}
