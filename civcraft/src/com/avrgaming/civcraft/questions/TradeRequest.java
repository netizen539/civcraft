package com.avrgaming.civcraft.questions;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.trade.TradeInventoryListener;
import com.avrgaming.civcraft.trade.TradeInventoryPair;
import com.avrgaming.civcraft.util.CivColor;

public class TradeRequest implements QuestionResponseInterface {

	public Resident resident;
	public Resident trader;
	
	@Override
	public void processResponse(String param) {
		if (param.equalsIgnoreCase("accept")) {
			TradeInventoryPair pair = new TradeInventoryPair();
			pair.inv = trader.startTradeWith(resident);
			if (pair.inv == null) {
				return;
			}
			
			pair.otherInv = resident.startTradeWith(trader);
			if (pair.otherInv == null) {
				return;
			}
			
			pair.resident = trader;
			pair.otherResident = resident;
			TradeInventoryListener.tradeInventories.put(TradeInventoryListener.getTradeInventoryKey(trader), pair);
			
			TradeInventoryPair otherPair = new TradeInventoryPair();
			otherPair.inv = pair.otherInv;
			otherPair.otherInv = pair.inv;
			otherPair.resident = pair.otherResident;
			otherPair.otherResident = pair.resident;
			TradeInventoryListener.tradeInventories.put(TradeInventoryListener.getTradeInventoryKey(resident), otherPair);
		} else {
			CivMessage.send(trader, CivColor.LightGray+resident.getName()+" denied our trade invitation.");
		}
	}

	@Override
	public void processResponse(String response, Resident responder) {
		processResponse(response);		
	}
}
