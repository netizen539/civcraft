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

import java.sql.SQLException;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;

public class EconCommand extends CommandBase {

	@Override
	public void init() {
		command = "/econ";
		displayName = "Econ";
		
		commands.put("add", "[player] [amount] - add money to this player");
		commands.put("set", "[player] [amount] - set money for this player");
		commands.put("sub", "[player] [amount] - subtract money for this player");
		
		commands.put("addtown", "[town] [amount] - add money to this town");
		commands.put("settown", "[town] [amount] - set money for this town");
		commands.put("subtown", "[town] [amount] - subtract money for this town");
		
		commands.put("addciv", "[civ] [amount] - add money to this civ");
		commands.put("setciv", "[civ] [amount] - set money for this civ");
		commands.put("subciv", "[civ] [amount] - subtract money for this civ");
		
		commands.put("setdebt", "[player] [amount] - sets the debt on this player to this amount.");
		commands.put("setdebttown", "[town] [amount]");
		commands.put("setdebtciv", "[civ] [amount]");
		
		commands.put("clearalldebt", "Clears all debt for everyone in the server. Residents, Towns, Civs");
		
	}
	
	public void clearalldebt_cmd() throws CivException {
		validEcon();
		
		for (Civilization civ : CivGlobal.getCivs()) {
			civ.getTreasury().setDebt(0);
			try {
				civ.saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		for (Town town : CivGlobal.getTowns()) {
			town.getTreasury().setDebt(0);
			try {
				town.saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		for (Resident res : CivGlobal.getResidents()) {
			res.getTreasury().setDebt(0);
			try {
				res.saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
		
		CivMessage.send(sender, "Cleared all debt");
	}
	
	
	public void setdebtciv_cmd() throws CivException {
		validEcon();
		
		Civilization civ = getNamedCiv(1);
		Double amount = getNamedDouble(2);
		civ.getTreasury().setDebt(amount);
		civ.save();
		
		CivMessage.sendSuccess(sender, "Set.");
	}
	
	public void setdebttown_cmd() throws CivException {
		validEcon();
		
		Town town = getNamedTown(1);
		Double amount = getNamedDouble(2);
		town.getTreasury().setDebt(amount);
		town.save();
		
		CivMessage.sendSuccess(sender, "Set.");
	}
	
	public void setdebt_cmd() throws CivException {
		validEcon();
		
		Resident resident = getNamedResident(1);
		Double amount = getNamedDouble(2);
		resident.getTreasury().setDebt(amount);
		resident.save();
		
		CivMessage.sendSuccess(sender, "Set.");
	}
	
	private void validEcon() throws CivException {
		if (!getPlayer().isOp() || !getPlayer().hasPermission(CivSettings.ECON)) {
			throw new CivException("You must be OP to use this command.");
		}		
	}
	
	public void add_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException("Provide a name and a amount");
		}
		
		Resident resident = getNamedResident(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			resident.getTreasury().deposit(amount);
			CivMessage.sendSuccess(sender, "Added "+args[2]+" to "+args[1]);
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" is not a number.");
		}
	}
	
	public void set_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException("Provide a name and a amount");
		}
		
		Resident resident = getNamedResident(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			resident.getTreasury().setBalance(amount);
			CivMessage.sendSuccess(sender, "Set "+args[2]+" to "+args[1]);
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" is not a number.");
		}
	}
	
	public void sub_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException("Provide a name and a amount");
		}
		
		Resident resident = getNamedResident(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			resident.getTreasury().withdraw(amount);
			CivMessage.sendSuccess(sender, "Subtracted "+args[2]+" to "+args[1]);
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" is not a number.");
		}
	}
	
	public void addtown_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException("Provide a name and a amount");
		}
		
		Town town = getNamedTown(1);
		
		try {
			
			Double amount = Double.valueOf(args[2]);
			town.getTreasury().deposit(amount);
			CivMessage.sendSuccess(sender, "Added "+args[2]+" to "+args[1]);
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" is not a number.");
		}
	}
	
	public void settown_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException("Provide a name and a amount");
		}
		
		Town town = getNamedTown(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			town.getTreasury().setBalance(amount);
			CivMessage.sendSuccess(sender, "Added "+args[2]+" to "+args[1]);
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" is not a number.");
		}
	}
	
	public void subtown_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException("Provide a name and a amount");
		}
		
		Town town = getNamedTown(1);

		try {
			
			Double amount = Double.valueOf(args[2]);
			town.getTreasury().withdraw(amount);
			CivMessage.sendSuccess(sender, "Added "+args[2]+" to "+args[1]);
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" is not a number.");
		}
	}

	public void addciv_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException("Provide a name and a amount");
		}
		
		Civilization civ = getNamedCiv(1);
		
		try {
			
			Double amount = Double.valueOf(args[2]);
			civ.getTreasury().deposit(amount);
			CivMessage.sendSuccess(sender, "Added "+args[2]+" to "+args[1]);
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" is not a number.");
		}
	}
	
	public void setciv_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException("Provide a name and a amount");
		}
		
		Civilization civ = getNamedCiv(1);
		
		try {
			
			Double amount = Double.valueOf(args[2]);
			civ.getTreasury().setBalance(amount);
			CivMessage.sendSuccess(sender, "Added "+args[2]+" to "+args[1]);
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" is not a number.");
		}
	}
	
	public void subciv_cmd() throws CivException {
		validEcon();
		
		if (args.length < 3) {
			throw new CivException("Provide a name and a amount");
		}
		
		Civilization civ = getNamedCiv(1);
		
		try {
			
			Double amount = Double.valueOf(args[2]);
			civ.getTreasury().withdraw(amount);
			CivMessage.sendSuccess(sender, "Added "+args[2]+" to "+args[1]);
			
		} catch (NumberFormatException e) {
			throw new CivException(args[2]+" is not a number.");
		}
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		Player player = getPlayer();
		Resident resident = CivGlobal.getResident(player);
		
		if (resident == null) {
			return;
		}
		
		CivMessage.sendSuccess(player, resident.getTreasury().getBalance()+" coins.");
		
	}

	@Override
	public void showHelp() {
		Player player;
		try {
			player = getPlayer();
		} catch (CivException e) {
			e.printStackTrace();
			return;
		}
		
		if (!player.isOp() && !player.hasPermission(CivSettings.ECON)) {
			return;
		}
		
		showBasicHelp();
		
	}

	@Override
	public void permissionCheck() throws CivException {
		
	}

}
