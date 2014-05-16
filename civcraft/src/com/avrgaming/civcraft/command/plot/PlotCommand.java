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
package com.avrgaming.civcraft.command.plot;

import java.text.SimpleDateFormat;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.permission.PermissionGroup;
import com.avrgaming.civcraft.structure.farm.FarmChunk;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;

public class PlotCommand extends CommandBase {

	@Override
	public void init() {
		command = "/plot";
		displayName = "Plot";
		
		commands.put("info", "Show info for the plot you're standing on.");
		commands.put("toggle", "[mobs]|[fire] toggles mob spawning or fire in this plot.");
		commands.put("perm", "View/Modify permissions.");
		commands.put("fs", "[amount] - puts plot up for sale for this amount.");
		commands.put("nfs", "Makes plot not for sale.");
		commands.put("buy", "Buys the plot your standing on.");
		commands.put("addgroup", "[name] - adds this group to the plot.");
		commands.put("setowner", "[name|none] Sets the owner on this plot(gives it away).");
		commands.put("farminfo", "Special information about this plot if it is a farm plot.");
		commands.put("removegroup", "[name] - removes this group from the plot.");
		commands.put("cleargroups", "Clears all groups from this plot.");
	}
	
	public void farminfo_cmd() throws CivException {
		Player player = getPlayer();
		
		ChunkCoord coord = new ChunkCoord(player.getLocation());
		FarmChunk fc = CivGlobal.getFarmChunk(coord);
		
		if (fc == null) {
			throw new CivException("This chunk is not a farm chunk.");
		}
		
		if (fc.getStruct().isActive() == false) {
			throw new CivException("This chunk is a farm, but the structure is not finished building yet.");
		}
		
		String dateString = "Never";
		
		if (fc.getLastGrowDate() != null) {
			SimpleDateFormat sdf = new SimpleDateFormat("M/d/y k:m:s z");
			dateString = sdf.format(fc.getLastGrowDate());
		}
		
		CivMessage.sendHeading(sender, "Farm Plot Info");
		CivMessage.send(sender, CivColor.Green+"Last Grow Time: "+CivColor.LightGreen+dateString);
		CivMessage.send(sender, CivColor.Green+"Last Grow Amount: "+CivColor.LightGreen+fc.getLastGrowTickCount());
		CivMessage.send(sender, CivColor.Green+"Growth Ticks While Unloaded: "+CivColor.LightGreen+fc.getMissedGrowthTicksStat());
		CivMessage.send(sender, CivColor.Green+"Last Effective Growth Rate: "+CivColor.LightGreen+df.format(fc.getFarm().getLastEffectiveGrowthRate()*100)+"%");
		
		String success = "no";
		if (fc.getLastRandomInt() < fc.getLastChanceForLast()) {
			success = "yes";
		}
		
		CivMessage.send(sender, CivColor.Green+"Last Extra Grow Chance: "+CivColor.LightGreen+fc.getLastChanceForLast()+" vs "+CivColor.LightGreen+fc.getLastRandomInt()+" success? "+CivColor.LightGreen+success);
		
		String out = "";
		for (BlockCoord bcoord : fc.getLastGrownCrops()) {
			out += bcoord.toString()+", ";
		}
		
		CivMessage.send(sender, CivColor.Green+"Crops Grown: "+CivColor.LightGreen+out);
		
		
	}
	
	public void setowner_cmd() throws CivException {
		TownChunk tc = this.getStandingTownChunk();
		validPlotOwner();
		
		if (args.length < 2) {
			throw new CivException("You must specifiy and owner.");
		}
		
		if (args[1].equalsIgnoreCase("none")) {
			tc.perms.setOwner(null);
			tc.save();
			CivMessage.sendSuccess(sender, "Set plot owner to none, returned plot to town.");
			return;
		}
		
		Resident resident = getNamedResident(1);

		if (resident.getTown() != tc.getTown()) {
			throw new CivException("Resident must be a member of this town.");
		}
		
		tc.perms.setOwner(resident);
		tc.save();
		
		CivMessage.sendSuccess(sender, "Plot is now owned by "+args[1]);
		
	}
	
	public void removegroup_cmd() throws CivException {
		TownChunk tc= this.getStandingTownChunk();
		validPlotOwner();
		
		if (args.length < 2) {
			throw new CivException("You must specify a group name.");
		}
		
		if (args[1].equalsIgnoreCase("none")) {
			throw new CivException("To clear the groups use the 'cleargroups' command instead.");
		}
		
		PermissionGroup grp = tc.getTown().getGroupByName(args[1]);
		if (grp == null) {
			throw new CivException("Could not find group named "+args[1]+" in this town.");
		}
		
		tc.perms.removeGroup(grp);
		tc.save();
		
		CivMessage.sendSuccess(sender, "Removed plot group "+grp.getName());
	}
	
	public void cleargroups_cmd() throws CivException {
		TownChunk tc= this.getStandingTownChunk();
		validPlotOwner();
		
		tc.perms.clearGroups();
		tc.save();
		CivMessage.sendSuccess(sender, "Cleared the plot's groups.");
		return;
	}

	public void addgroup_cmd() throws CivException {
		TownChunk tc= this.getStandingTownChunk();
		validPlotOwner();
		
		if (args.length < 2) {
			throw new CivException("You must specify a group name.");
		}
		
		if (args[1].equalsIgnoreCase("none")) {
			throw new CivException("To clear the groups use the 'cleargroups' command instead.");
			
		}
		
		PermissionGroup grp = tc.getTown().getGroupByName(args[1]);
		if (grp == null) {
			throw new CivException("Could not find group named "+args[1]+" in this town.");
		}
		
		tc.perms.addGroup(grp);
		tc.save();
		
		CivMessage.sendSuccess(sender, "Added plot group "+grp.getName());
	}
	
	public void buy_cmd() throws CivException {
		TownChunk tc = this.getStandingTownChunk();
		Resident resident = getResident();
		
		if (tc.isOutpost()) {
			throw new CivException("Cannot buy outposts.");
		}
		
		if (resident.getTown() != tc.getTown()) {
			throw new CivException("You cannot buy this plot, you are not a member of this town.");
		}
		
		if (tc.isForSale() == false) {
			throw new CivException("This plot is not for sale.");
		}
		
		tc.purchase(resident);
		CivMessage.sendSuccess(sender, "Purchased plot "+tc.getChunkCoord()+" for "+tc.getValue()+" coins.");
	}
	
	public void fs_cmd() throws CivException {
		TownChunk tc = this.getStandingTownChunk();
		this.validPlotOwner();
		
		if (tc.isOutpost()) {
			throw new CivException("Cannot sell outposts.");
		}
		
		if (args.length < 2) {
			throw new CivException("You must specify a price.");
		}
		
		try {
			double price = Double.valueOf(args[1]);
			tc.setForSale(true);
			tc.setPrice(price);
			tc.save();
		} catch (NumberFormatException e) {
			throw new CivException(args[1]+" could not be read as a number.");
		}
		
		CivMessage.sendTown(tc.getTown(), "Placed plot "+tc.getCenterString()+" up for sale at "+args[1]+" coins.");
	}
	
	
	public void nfs_cmd() throws CivException {
		TownChunk tc = this.getStandingTownChunk();
		this.validPlotOwner();
	
		try {
			tc.setForSale(false);
			tc.save();
		} catch (NumberFormatException e) {
			throw new CivException(args[1]+" could not be read as a number.");
		}
		
		CivMessage.sendTown(tc.getTown(), "Plot "+tc.getCenterString()+" is no longer up for sale.");
	}
	
	public void toggle_cmd() throws CivException {
		TownChunk tc = this.getStandingTownChunk();
		this.validPlotOwner();
		
		if (args.length < 2) {
			throw new CivException("Please specifiy mobs or fire to toggle.");
		}
		
		if (args[1].equalsIgnoreCase("mobs")) {
			if (tc.perms.isMobs()) {
				tc.perms.setMobs(false);
			} else {
				tc.perms.setMobs(true);
			}
			
			CivMessage.sendSuccess(sender, "Set mob spawning on this plot to "+tc.perms.isMobs());
			
		} else if (args[1].equalsIgnoreCase("fire")) {
			if (tc.perms.isFire()) {
				tc.perms.setFire(false);
			} else {
				tc.perms.setFire(true);
			}
			CivMessage.sendSuccess(sender, "Set fire on this plot to "+tc.perms.isFire());
		}
		tc.save();
	}
	
	public void perm_cmd() throws CivException {
		PlotPermCommand cmd = new PlotPermCommand();	
		cmd.onCommand(sender, null, "perm", this.stripArgs(args, 1));
	}
	
	private void showCurrentPermissions(TownChunk tc) {
		CivMessage.send(sender, CivColor.Green+"Build: "+CivColor.LightGreen+tc.perms.getBuildString());
		CivMessage.send(sender, CivColor.Green+"Destroy: "+CivColor.LightGreen+tc.perms.getDestroyString());
		CivMessage.send(sender, CivColor.Green+"Interact: "+CivColor.LightGreen+tc.perms.getInteractString());
		CivMessage.send(sender, CivColor.Green+"Item Use: "+CivColor.LightGreen+tc.perms.getItemUseString());
	}
	
	private void showPermOwnership(TownChunk tc) {
		String out = CivColor.Green+"Town: "+CivColor.LightGreen+tc.getTown().getName();
		out += CivColor.Green+" Owner: "+CivColor.LightGreen;
		if (tc.perms.getOwner() != null) {
			out += tc.perms.getOwner().getName();
		} else {
			out += "none";
		}
		
		out += CivColor.Green+" Group: "+CivColor.LightGreen;
		if (tc.perms.getGroups().size() != 0) {
			out += tc.perms.getGroupString();
		} else {
			out += "none";
		}
		
		CivMessage.send(sender, out);
	}
	
	/*private void showPermCmdHelp() {
		CivMessage.send(sender, CivColor.LightGray+"/plot perm set <type> <groupType> [on|off] ");
		CivMessage.send(sender, CivColor.LightGray+"    types: [build|destroy|interact|itemuse|reset]");
		CivMessage.send(sender, CivColor.LightGray+"    groupType: [owner|group|others]");
	}*/
	
	public void info_cmd() throws CivException {
		if (sender instanceof Player) {
			Player player = (Player)sender;
			
			TownChunk tc = CivGlobal.getTownChunk(player.getLocation());
			if (tc == null) {
				throw new CivException("Plot is not owned.");
			}
			
			CivMessage.sendHeading(sender, "Plot Information");
			showPermOwnership(tc);
			showCurrentPermissions(tc);
			showToggles(tc);
			showPriceInfo(tc);

		}
	}
	
	private void showToggles(TownChunk tc) {
		CivMessage.send(sender, CivColor.Green+"Mobs: "+CivColor.LightGreen+tc.perms.isMobs()+" "+
								CivColor.Green+"Fire: "+CivColor.LightGreen+tc.perms.isFire());
	}

	private void showPriceInfo(TownChunk tc) {
		String out = "";
		if (tc.isForSale()) {
			out += CivColor.Yellow+" [For Sale at "+tc.getPrice()+" coins] ";
		}
		CivMessage.send(sender, CivColor.Green+"Value: "+CivColor.LightGreen+tc.getValue()+out);
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() {
		return;
	}

	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
		//info_cmd();
		//CivMessage.send(sender, CivColor.LightGray+"Subcommands available: See /plot help");
	}

}
