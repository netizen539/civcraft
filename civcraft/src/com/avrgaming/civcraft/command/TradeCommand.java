package com.avrgaming.civcraft.command;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.TradeRequest;
import com.avrgaming.civcraft.trade.TradeInventoryListener;
import com.avrgaming.civcraft.util.CivColor;

public class TradeCommand extends CommandBase {

	public static int TRADE_TIMEOUT = 30000;
	
	@Override
	public void init() {
		command = "/trade";
		displayName = "Trade";
		sendUnknownToDefault = true;
	}

	@Override
	public void doDefaultAction() throws CivException {
		Resident resident = getNamedResident(0);
		Resident trader = getResident();
		
		if (resident.isInsideArena() || trader.isInsideArena()) {
			throw new CivException("You cannot trade items when a player is inside a PvP Arena.");
		}
		
		double max_trade_distance;
		try {
			max_trade_distance = CivSettings.getDouble(CivSettings.civConfig, "global.max_trade_distance");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return;
		}
		
		Player traderPlayer = CivGlobal.getPlayer(trader);
		Player residentPlayer = CivGlobal.getPlayer(resident);
		
		if (trader == resident) {
			throw new CivException("You cannot trade with yourself.");
		}
		
		if (traderPlayer.getLocation().distance(residentPlayer.getLocation()) > max_trade_distance) {
			throw new CivException(resident.getName()+" is too far away to trade with.");
		}
		
		if (TradeInventoryListener.tradeInventories.containsKey(TradeInventoryListener.getTradeInventoryKey(resident))) {
			throw new CivException(resident.getName()+" is already trading with someone. Please wait.");
		}
		
		TradeRequest tradeRequest = new TradeRequest();
		tradeRequest.resident = resident;
		tradeRequest.trader = trader;
		
		CivGlobal.questionPlayer(traderPlayer, residentPlayer, 
				"Would you like to trade with "+traderPlayer.getName()+"?",
				TRADE_TIMEOUT, tradeRequest);
		CivMessage.sendSuccess(sender, "Trade Invitation Sent");
	}

	@Override
	public void showHelp() {
		CivMessage.send(sender, CivColor.LightPurple+command+" "+CivColor.Yellow+"[resident name] "+
				CivColor.LightGray+"Opens trading window with this player.");
	}

	@Override
	public void permissionCheck() throws CivException {		
	}

}
