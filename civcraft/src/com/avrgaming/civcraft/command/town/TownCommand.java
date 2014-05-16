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
package com.avrgaming.civcraft.command.town;


import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.block.Biome;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigBuildableInfo;
import com.avrgaming.civcraft.config.ConfigCultureBiomeInfo;
import com.avrgaming.civcraft.config.ConfigCultureLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.lorestorage.LoreGuiItem;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.questions.JoinTownResponse;
import com.avrgaming.civcraft.structure.Capitol;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.TownHall;
import com.avrgaming.civcraft.tutorial.CivTutorial;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.global.perks.Perk;
import com.avrgaming.global.perks.components.CustomTemplate;

public class TownCommand extends CommandBase {
	
	public static final long INVITE_TIMEOUT = 30000; //30 seconds
	
	public void init() {
		command = "/town";
		displayName = "Town";
		
		commands.put("claim", "Claim the plot you are standing in for this town.");
		commands.put("unclaim", "Unclaim the plot you are standing on, no refunds.");
		commands.put("group", "Manage town permission groups.");
		commands.put("upgrade", "Manage town upgrades.");
		commands.put("info", "Show information about this town.");
		commands.put("add", "[name] - invites resident to town.");
		commands.put("members", "Show a list of members in this town.");
		commands.put("deposit", "[amount] - deposits this amount into the town's treasury.");
		commands.put("withdraw","[amount] - withdraws this amount from the town's treasury.");
		commands.put("set", "Change various town properties.");
		commands.put("leave", "leaves the town you are currently in.");
		commands.put("show", "[name] show info for town of this name.");
		commands.put("evict", "[name] - evicts the resident named from town");
		commands.put("list", "shows a list of all towns in the world.");
		commands.put("reset", "Resets certain structures, action depends on structure.");
		commands.put("top5", "Shows the top 5 towns in the world.");
		commands.put("disbandtown", "Disbands this town, requres leader to type disbandtown as well.");
		commands.put("outlaw", "Manage town outlaws.");
		commands.put("leavegroup", "[town] [group] - Leaves the group in [town] named [group]");
		commands.put("select", "[town] - Switches your control to this town, if you have the proper permissions.");
//		commands.put("capture", "[town] - instantly captures this town if they have a missing or illegally placed town hall during WarTime.");
		commands.put("capitulate", " gives this town over to the currently owner civ. It will no longer remember its native civilization and will not revolt.");
		commands.put("survey", "Surveys the land, estimates what kinds of bonuses you would get from building here.");
		commands.put("templates", "Displays all templates bound to this town.");
		commands.put("event", "Displays information about the current random event going down.");
		commands.put("claimmayor", "claim yourself as mayor of this town. All current mayors must be inactive.");
		commands.put("movestructure", "[coord] [town] moves the structure specified by the coord to the specfied town.");
		commands.put("enablestructure", "[coord] attempts to enable the specified structure if its currently disabled.");
	}
	
	public void enablestructure_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident resident = getResident();
		String coordString = getNamedString(1, "Coordinate of structure. Example: world,555,65,444");
		Structure struct;
		try {
			struct = CivGlobal.getStructure(new BlockCoord(coordString));
		} catch (Exception e) {
			throw new CivException("Invalid structure coordinate. Example: world,555,65,444");
		}
		
		if (War.isWarTime()) {
			throw new CivException("Cannot move structures during war time.");
		}
		
		if (struct == null) {
			throw new CivException("Structure at:"+coordString+" is not found.");
		}
		
		if (!resident.getCiv().getLeaderGroup().hasMember(resident)) {
			throw new CivException("You must be the civ's leader in order to do this.");
		}
		
		if (!town.isStructureAddable(struct)) {
			throw new CivException("Structure still puts town over limits, cannot be re-enabled.");
		}
		
		/* Readding structure will make it valid. */
		town.removeStructure(struct);
		town.addStructure(struct);
		CivMessage.sendSuccess(sender, "Re-enabled structure.");
	}
	
	public void movestructure_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident resident = getResident();
		String coordString = getNamedString(1, "Coordinate of structure. Example: world,555,65,444");
		Town targetTown = getNamedTown(2);
		Structure struct;
		
		try {
			struct = CivGlobal.getStructure(new BlockCoord(coordString));
		} catch (Exception e) {
			throw new CivException("Invalid structure coordinate. Example: world,555,65,444");
		}

		if (struct instanceof TownHall || struct instanceof Capitol) {
			throw new CivException("Cannot move town halls or capitols.");
		}
		
		if (War.isWarTime()) {
			throw new CivException("Cannot move structures during war time.");
		}
		
		if (struct == null) {
			throw new CivException("Structure at:"+coordString+" is not found.");
		}
		
		if (!resident.getCiv().getLeaderGroup().hasMember(resident)) {
			throw new CivException("You must be the civ's leader in order to do this.");
		}
		
		if (town.getCiv() != targetTown.getCiv()) {
			throw new CivException("You can only move structures between towns in your own civ.");
		}
		
		town.removeStructure(struct);
		targetTown.addStructure(struct);
		struct.setTown(targetTown);
		struct.save();
		
		CivMessage.sendSuccess(sender, "Moved structure "+coordString+" to town "+targetTown.getName());
	}
	
	public void claimmayor_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident resident = getResident();
		
		if (resident.getTown() != town) {
			throw new CivException("You can only claim mayorship in the town you are in. Use /town select to select your home town.");
		}
		
		if (!town.areMayorsInactive()) {
			throw new CivException("At least one mayor is not inactive in this town. Cannot claim mayorship.");
		}
		
		town.getMayorGroup().addMember(resident);
		town.getMayorGroup().save();
		CivMessage.sendSuccess(sender, "You are now a mayor in "+town.getName());
		CivMessage.sendTown(town, resident.getName()+" has assumed control of the town due to inactive mayorship.");
	}
	
	public void event_cmd() throws CivException {
		TownEventCommand cmd = new TownEventCommand();	
		cmd.onCommand(sender, null, "event", this.stripArgs(args, 1));
	}
	
	public void templates_cmd() throws CivException {
		Player player = getPlayer();
		Town town = getSelectedTown();
		Inventory inv = Bukkit.getServer().createInventory(player, CivTutorial.MAX_CHEST_SIZE*9, town.getName()+" Perks");

		for (ConfigBuildableInfo info : CivSettings.structures.values()) {
			for (Perk p : CustomTemplate.getTemplatePerksForBuildable(town, info.template_base_name)) {
				
				ItemStack stack = LoreGuiItem.build(p.configPerk.display_name, 
						p.configPerk.type_id, 
						p.configPerk.data, 
						CivColor.Gray+"Provided by: "+CivColor.LightBlue+p.provider);
				inv.addItem(stack);
			}
		}
		
		player.openInventory(inv);
	}
	
	public static ArrayList<String> survey(Location loc) {
		ChunkCoord start = new ChunkCoord(loc);
		ConfigCultureLevel lvl = CivSettings.cultureLevels.get(1);

		ArrayList<String> outList = new ArrayList<String>();
		
		Queue<ChunkCoord> closedSet = new LinkedList<ChunkCoord>();	
		Queue<ChunkCoord> openSet = new LinkedList<ChunkCoord>();
		openSet.add(start);
		/* Try to get the surrounding chunks and get their biome info. */
		//Enqueue all neighbors.
		while (!openSet.isEmpty()) {
			ChunkCoord node = openSet.poll();
			
			if (closedSet.contains(node)) {
				continue;
			}
			
			if (node.manhattanDistance(start) > lvl.chunks) {
				continue;
			//	break;
			}
			
			closedSet.add(node);
			
			//Enqueue all neighbors.
			int[][] offset = { { -1, 0 }, { 1, 0 }, { 0, -1 }, { 0, 1 } };
			for (int i = 0; i < 4; i++) {
				ChunkCoord nextCoord = new ChunkCoord(node.getWorldname(), 
						node.getX() + offset[i][0], 
						node.getZ() + offset[i][1]);
				
				if (closedSet.contains(nextCoord)) {
					continue;
				}
				
				openSet.add(nextCoord);
			}
		}
		
		HashMap<String, Integer> biomes = new HashMap<String, Integer>();
		
	//	double coins = 0.0;
		double hammers = 0.0;
		double growth = 0.0;
		double happiness = 0.0;
		double beakers = 0.0;
		DecimalFormat df = new DecimalFormat();
		
		for (ChunkCoord c : closedSet) {
			/* Increment biome counts. */
			Biome biome = c.getChunk().getWorld().getBiome(c.getX()*16, c.getZ()*16);
			
			if (!biomes.containsKey(biome.name())) {
				biomes.put(biome.name(), 1);
			} else {
				Integer value = biomes.get(biome.name());
				biomes.put(biome.name(), value+1);
			}
			
			ConfigCultureBiomeInfo info = CivSettings.getCultureBiome(biome.name());
			
		//	coins += info.coins;
			hammers += info.hammers;
			growth += info.growth;
			happiness += info.happiness;
			beakers += info.beakers;
		}
		
		outList.add(CivColor.LightBlue+"Biome Counts");
		//int totalBiomes = 0;
		String out = "";
		for (String biome : biomes.keySet()) {
			Integer count = biomes.get(biome);
			out += CivColor.Green+biome+": "+CivColor.LightGreen+count+CivColor.Green+", ";
			//totalBiomes += count;
		}
		outList.add(out);
	//	outList.add(CivColor.Green+"Biome Count: "+CivColor.LightGreen+totalBiomes);
		
		outList.add(CivColor.LightBlue+"Totals");
		outList.add(CivColor.Green+" Happiness:"+CivColor.LightGreen+df.format(happiness)+
				CivColor.Green+" Hammers:"+CivColor.LightGreen+df.format(hammers)+
				CivColor.Green+" Growth:"+CivColor.LightGreen+df.format(growth)+
				CivColor.Green+" Beakers:"+CivColor.LightGreen+df.format(beakers));
		return outList;
	}
	
	public void survey_cmd() throws CivException {
		Player player = getPlayer();
		CivMessage.send(player, survey(player.getLocation()));
	}
	
	public void capitulate_cmd() throws CivException {
		this.validMayor();
		Town town = getSelectedTown();
		
		if (town.getMotherCiv() == null) {
			throw new CivException("Cannot capitulate unless captured by another civilization.");
		}
		
		if (town.getMotherCiv().getCapitolName().equals(town.getName())) {
			throw new CivException("Cannot capitulate your capitol town. Use /civ capitulate instead to capitulate your entire civ.");
		}
		
		if (args.length < 2 || !args[1].equalsIgnoreCase("yes")) {
			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"Capitualting means that this town will become a normal town in "+town.getCiv().getName()+" and can no longer revolt. Are you sure?");
			CivMessage.send(sender, CivColor.Yellow+ChatColor.BOLD+"If you're sure, type /town capitulate yes");
			return;
		}
		
		/* Town is capitulating, no longer need a mother civ. */
		town.setMotherCiv(null);
		town.save();
		
		CivMessage.global("The conquered town of "+town.getName()+" has capitualted to "+town.getCiv().getName()+" and can no longer revolt.");	
	}
	
//	public void capture_cmd() throws CivException {
//		this.validLeaderAdvisor();
//		
//		if (!War.isWarTime()) {
//			throw new CivException("Can only use this command during war time.");
//		}
//		
//		Town town = getNamedTown(1);
//		Civilization civ = getSenderCiv();
//		
//		if (town.getCiv().isAdminCiv()) {
//			throw new CivException("Cannot capture spawn town.");
//		}
//		
//		TownHall townhall = town.getTownHall();
//		if (townhall != null && townhall.isValid()) {
//			throw new CivException("Cannot capture, this town has a valid town hall.");
//		}
//		
//		if (town.claimed) {
//			throw new CivException("Town has already been claimed this war time.");
//		}
//		
//		if (town.getMotherCiv() != null) {
//			throw new CivException("Cannot capture a town already captured by another civ!");
//		}
//		
//		if (town.isCapitol()) {
//			town.getCiv().onDefeat(civ);
//			CivMessage.global("The capitol civilization of "+town.getCiv().getName()+" had an illegal or missing town hall and was claimed by "+civ.getName());
//		} else {
//			town.onDefeat(civ);
//			CivMessage.global("The town of "+town.getName()+" had an illegal or missing town hall and was claimed by "+civ.getName());
//		}
//		
//		town.claimed = true;
//		
//	}
	
	public void select_cmd() throws CivException {
		Resident resident = getResident();
		Town selectTown = getNamedTown(1);
		
		if (resident.getSelectedTown() == null) {
			if (resident.getTown() == selectTown) {
				throw new CivException("You already have "+selectTown.getName()+" selected.");
			}
		}
		
		if (resident.getSelectedTown() == selectTown) {
			throw new CivException("You already have "+selectTown.getName()+" selected.");
		}
		
		selectTown.validateResidentSelect(resident);
				
		resident.setSelectedTown(selectTown);
		CivMessage.sendSuccess(sender, "You have selected "+selectTown.getName()+".");
	}
	
	public void leavegroup_cmd() throws CivException {
		Town town = getNamedTown(1);
		PermissionGroup grp = getNamedPermissionGroup(town, 2);
		Resident resident = getResident();
		
		if (!grp.hasMember(resident)) {
			throw new CivException("You are not a member of the group "+grp.getName()+" in town "+town.getName());
		}
		
		if (grp == town.getMayorGroup() && grp.getMemberCount() == 1) {
			throw new CivException("You cannot leave the mayor group if you're the last mayor.");
		}
		
		if (grp == town.getCiv().getLeaderGroup() && grp.getMemberCount() == 1) {
			throw new CivException("You cannot leave the leaders group if you're the last leader.");
		}
		
		grp.removeMember(resident);
		grp.save();
		CivMessage.sendSuccess(sender, "You are no longer a member of the "+grp.getName()+" group in town "+town.getName());
	}

	public void outlaw_cmd() {
		TownOutlawCommand cmd = new TownOutlawCommand();	
		cmd.onCommand(sender, null, "outlaw", this.stripArgs(args, 1));
	}
	
	public void disbandtown_cmd() throws CivException {
		this.validMayor();
		Town town = this.getSelectedTown();
		
		if (town.getMotherCiv() != null) {
			throw new CivException("You cannot disband a town that is currently captured.");
		}
		
		if (town.isCapitol()) {
			throw new CivException("You cannot disband the capitol town.");
		}
		
		if (town.mayorWantsToDisband) {
			town.mayorWantsToDisband = false;
			CivMessage.send(sender, "No longer want to disband.");
			return;
		}
		
		town.mayorWantsToDisband = true;		
		
		
		if (town.leaderWantsToDisband && town.mayorWantsToDisband) {
			CivMessage.sendCiv(town.getCiv(), "Town "+town.getName()+" is being disbanded by agreement from the civ leader and the mayor");
			town.disband();
		}
		
		CivMessage.send(sender, "Waiting on leader to type /civ disbandtown");
	}
	
	public void top5_cmd() {	
		CivMessage.sendHeading(sender, "Top 5 Towns");
//		TreeMap<Integer, Town> scores = new TreeMap<Integer, Town>();
//		
//		for (Town town : CivGlobal.getTowns()) {
//			if (town.getCiv().isAdminCiv()) {
//				continue;
//			}
//			scores.put(town.getScore(), town);
//		}
		
		synchronized(CivGlobal.townScores) {
			int i = 1;
			for (Integer score : CivGlobal.townScores.descendingKeySet()) {
				CivMessage.send(sender, i+") "+CivColor.Gold+CivGlobal.townScores.get(score).getName()+CivColor.White+" - "+score+" points");
				i++;
				if (i > 5) {
					break;
				}
			}
		}
		
	}
	
	public void list_cmd() {
		String out = "";
		
		CivMessage.sendHeading(sender, "Towns in the World");
		for (Town town : CivGlobal.getTowns()) {
			out += town.getName()+"("+town.getCiv().getName()+")"+", ";
		}
		
		CivMessage.send(sender, out);
	}
	
	public void evict_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident resident = getResident();
		
		if (args.length < 2) {
			throw new CivException("Enter the name of who you want to evict.");
		}
		
		Resident residentToKick = getNamedResident(1);
		
		if (residentToKick.getTown() != town) {
			throw new CivException(args[1]+" is not a member of this town.");
		}
		
		if (!town.isInGroup("mayors", resident) && !town.isInGroup("assistants", resident)) {
			throw new CivException("Only mayors and assistants of this town can evict residents.");
		}
		
		if (town.isInGroup("mayors", residentToKick) || town.isInGroup("assistants", residentToKick)) {
			throw new CivException("Mayors and assistants cannot be evicted from town, demote them first.");
		}
		
		if (!residentToKick.isLandOwner()) {
			town.removeResident(residentToKick);

			try {
				CivMessage.send(CivGlobal.getPlayer(residentToKick), CivColor.Yellow+"You have been evicted from town!");
			} catch (CivException e) {
				//Player not online.
			}
			CivMessage.sendTown(town, residentToKick.getName()+" has been evicted from town by "+resident.getName());
			return;
		}
		
		residentToKick.setDaysTilEvict(CivSettings.GRACE_DAYS);
		residentToKick.warnEvict();
		residentToKick.save();
		CivMessage.sendSuccess(sender, args[1]+" will be evicted from town in "+CivSettings.GRACE_DAYS+" days.");
	}
	
	public void show_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("You need to enter the town name you wish to look at.");
		}
		
		Town town = getNamedTown(1);
		if (sender instanceof Player) {
			TownInfoCommand.show(sender, getResident(), town, town.getCiv(), this);
		} else {
			TownInfoCommand.show(sender, null, town, town.getCiv(), this);
		}

		try {
			Civilization civ = getSenderCiv();
			if (town.getCiv() != civ) {
				if (sender instanceof Player) {
					Player player = (Player)sender;	
					Location ourCapLoc = civ.getCapitolTownHallLocation();
					
					if (ourCapLoc == null) {
						return;
					}
					
					double potentialDistanceLow;
					double potentialDistanceHigh;
					try {
						if (town.getTownHall() != null) {
							Location theirTownHallLoc = town.getTownHall().getCenterLocation().getLocation();
							potentialDistanceLow = civ.getDistanceUpkeepAtLocation(ourCapLoc, theirTownHallLoc, true);
							potentialDistanceHigh = civ.getDistanceUpkeepAtLocation(ourCapLoc, theirTownHallLoc, false);
							
							CivMessage.send(player, CivColor.Yellow+"Your civilization would pay "+potentialDistanceLow+" if it or owned it."); 
							CivMessage.send(player, CivColor.Yellow+"Your civilization would pay 0 upkeep if you conquered it and it remains uncapitulated"); 
							CivMessage.send(player, CivColor.Yellow+"If this town's culture is not connected to your captial's culture and it was owned fully by your civ, you would pay "+potentialDistanceHigh+" coins in distance upkeep if you owned it.");
						} else {
							CivMessage.send(player, CivColor.Yellow+"This town has no town hall! Cannot calculate distance upkeep to it.");
						}
					} catch (InvalidConfiguration e) {
						e.printStackTrace();
						CivMessage.sendError(sender, "Internal Configuration error.");
						return;
					}
				}
			}
		} catch (CivException e) {
			// Playe not part of a civ, thats ok dont show anything.
		}
		
	}
	
	public void leave_cmd() throws CivException {
		Town town = getSelectedTown();
		Resident resident = getResident();
		
		if (town != resident.getTown()) {
			throw new CivException("You must have your own town selected in order to leave it.");
		}
		
		if (town.getMayorGroup().getMemberCount() == 1 &&
				town.getMayorGroup().hasMember(resident)) {
			throw new CivException("You cannot leave town since you are it's only mayor.");
		}
		
		town.removeResident(resident);
		if (resident.isCivChat()) {
			resident.setCivChat(false);
		}
		
		if (resident.isTownChat()) {
			resident.setTownChat(false);
			CivMessage.send(sender, CivColor.LightGray+"You've been removed from town chat since you've left the town.");		
		}
		
		CivMessage.sendSuccess(sender, "You left the town of "+town.getName());
		CivMessage.sendTown(town, resident.getName()+" has left the town.");
		
		town.save();
		resident.save();
	}
	
	public void set_cmd() {
		TownSetCommand cmd = new TownSetCommand();	
		cmd.onCommand(sender, null, "set", this.stripArgs(args, 1));
	}
	
	public void reset_cmd() throws CivException {
		TownResetCommand cmd = new TownResetCommand();	
		cmd.onCommand(sender, null, "reset", this.stripArgs(args, 1));
	}
	
	public void upgrade_cmd() throws CivException {
		TownUpgradeCommand cmd = new TownUpgradeCommand();	
		cmd.onCommand(sender, null, "upgrade", this.stripArgs(args, 1));
	}
	
	public void withdraw_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter the amount you want to withdraw.");
		}
		
		Town town = getSelectedTown();
		Player player = getPlayer();
		Resident resident = getResident();
		
		if (!town.playerIsInGroupName("mayors", player)) {
			throw new CivException("Only mayors can use this command.");
		}
		
		try {
			Double amount = Double.valueOf(args[1]);
			if (amount < 1) {
				throw new CivException("Cannot withdraw less than 1");
			}
			amount = Math.floor(amount);
			
			if(!town.getTreasury().payTo(resident.getTreasury(), Double.valueOf(args[1]))) {
				throw new CivException("The town does not have that much.");
			}
		} catch (NumberFormatException e) {
			throw new CivException(args[1]+" is not a valid number.");
		}
		
		CivMessage.sendSuccess(sender, "Withdrew "+args[1]+" coins.");
	}
	
	public void deposit_cmd() throws CivException {
		if (args.length < 2) {
			throw new CivException("Enter the amount you want to deposit.");
		}
		
		Resident resident = getResident();
		Town town = getSelectedTown();
		Double amount = getNamedDouble(1);
		
		try {
			if (amount < 1) {
				throw new CivException("Cannot deposit less than 1");
			}
			amount = Math.floor(amount);
			town.depositFromResident(amount, resident);

		} catch (NumberFormatException e) {
			throw new CivException(args[1]+" is not a valid number.");
		}
		
		CivMessage.sendSuccess(sender, "Deposited "+args[1]+" coins.");
	}
	
	public void add_cmd() throws CivException {
		this.validMayorAssistantLeader();

		Resident newResident = getNamedResident(1);
		Player player = getPlayer();
		Town town = getSelectedTown();
		
		if (War.isWarTime()) {
			throw new CivException("Cannot invite players to town during WarTime.");
		}

		if (War.isWithinWarDeclareDays() && town.getCiv().getDiplomacyManager().isAtWar()) {
			throw new CivException("Cannot invite players to a civ that is at war within "+War.getTimeDeclareDays()+" days before WarTime.");
		}
		
		if (newResident.hasCamp()) {
			try {
				Player resPlayer = CivGlobal.getPlayer(newResident);
				CivMessage.send(resPlayer, CivColor.Yellow+player.getName()+" tried to invite you to the town of "+town.getName()+
						" but cannot since you are in a camp. Leave camp first using /camp leave");
			} catch(CivException e) {
				//player not online
			}
			throw new CivException("You cannot invite "+newResident.getName()+" to town since he is part of a camp. Have him leave camp first with /camp leave.");
		}
		
		if (town.hasResident(newResident)) {
			throw new CivException(newResident.getName()+" is already a member of town.");
		}
		
		if (newResident.getTown() != null) {
			throw new CivException(newResident.getName()+" is already in town "+newResident.getTown().getName());
		}
		
		JoinTownResponse join = new JoinTownResponse();
		join.town = town;
		join.resident = newResident;
		join.sender = player;
		
		newResident.validateJoinTown(town);
		
		CivGlobal.questionPlayer(player, CivGlobal.getPlayer(newResident), 
				"Would you like to join the town of "+town.getName()+"?",
				INVITE_TIMEOUT, join);
		
		CivMessage.sendSuccess(sender, CivColor.LightGray+"Invited to "+args[1]+" to town "+town.getName());
	}
	
	public void info_cmd() throws CivException {
		TownInfoCommand cmd = new TownInfoCommand();	
		cmd.onCommand(sender, null, "info", this.stripArgs(args, 1));
	}
	
//	public void new_cmd() throws CivException {
//		if (!(sender instanceof Player)) {
//			return;
//		}
//		
//		Resident resident = CivGlobal.getResident((Player)sender);
//		
//		if (resident == null || !resident.hasTown()) {
//			throw new CivException("You are not part of a civilization.");
//		}
//		
//		ConfigUnit unit = Unit.getPlayerUnit((Player)sender);
//		if (unit == null || !unit.id.equals("u_settler")) {			
//			throw new CivException("You must be a settler in order to found a town.");
//		}
//		
//		CivMessage.sendHeading(sender, "Founding A New Town");
//		CivMessage.send(sender, CivColor.LightGreen+"This looks like a good place to settle!");
//		CivMessage.send(sender, " ");
//		CivMessage.send(sender, CivColor.LightGreen+ChatColor.BOLD+"What shall your new Town be called?");
//		CivMessage.send(sender, CivColor.LightGray+"(To cancel, type 'cancel')");
//		
//		resident.setInteractiveMode(new InteractiveTownName());
//
//	}
	
	public void claim_cmd() throws CivException {
		
		Player player = getPlayer();
		Town town = this.getSelectedTown();
		
		if (!town.playerIsInGroupName("mayors", player) && !town.playerIsInGroupName("assistants", player)) {
			throw new CivException("Only mayors and assistants can use this command.");
		}
		
//		boolean outpost = false;
//		if (args.length >= 2 && args[1].equalsIgnoreCase("outpost")) {
//			outpost = true;
//			CivMessage.send(player, "Claiming an outpost!");
//		}
		
		TownChunk.claim(town, player, false);
	}
	
	public void unclaim_cmd() throws CivException {
		Town town = getSelectedTown(); 
		Player player = getPlayer();
		Resident resident = getResident();
		TownChunk tc = this.getStandingTownChunk();
		
		
		if (!town.playerIsInGroupName("mayors", player) && !town.playerIsInGroupName("assistants", player)) {
			throw new CivException("Only mayors and assistants can use this command.");
		}
		
		if (town.getTownChunks().size() <= 1) {
			throw new CivException("Cannot unclaim your last town chunk.");
		}
		
		if (tc.getTown() != resident.getTown()) {
			throw new CivException("You cannot unclaim a town chunk that isn't yours.");
		}
		
		if (tc.perms.getOwner() != null && tc.perms.getOwner() != resident) {
			throw new CivException("You cannot unclaim a chunk that belongs to another resident.");
		}
		
		TownChunk.unclaim(tc);
		if (tc.isOutpost()) {
			CivMessage.sendSuccess(sender, "Unclaimed Outpost at "+tc.getCenterString());
		} else {
			CivMessage.sendSuccess(sender, "Unclaimed "+tc.getCenterString());
		}
		
	}
	
	public void group_cmd() throws CivException {
		TownGroupCommand cmd = new TownGroupCommand();	
		cmd.onCommand(sender, null, "group", this.stripArgs(args, 1));
	}
	
	
	public void members_cmd() throws CivException {
		Town town = this.getSelectedTown();
		
		CivMessage.sendHeading(sender, town.getName()+" Members");
		String out = "";
		for (Resident res : town.getResidents()) {
			out += res.getName() + ", ";
		}
		CivMessage.send(sender, out);
	}
	
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() {		
		return;
	}

	@Override
	public void doDefaultAction() {
		//TODO make this an info command.
		showHelp();
	}
		
}
