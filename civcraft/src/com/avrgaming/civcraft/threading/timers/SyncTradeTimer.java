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
package com.avrgaming.civcraft.threading.timers;

import java.text.DecimalFormat;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.util.CivColor;

public class SyncTradeTimer implements Runnable {

	public SyncTradeTimer() {
	}
	
	public void processTownsTradePayments(Town town) {
		
		//goodies = town.getEffectiveBonusGoodies();
		
		//double payment = TradeGood.getTownTradePayment(town, goodies);
		double payment = TradeGood.getTownTradePayment(town);
		DecimalFormat df = new DecimalFormat();
		
		if (payment > 0.0) {
			
			double taxesPaid = payment*town.getDepositCiv().getIncomeTaxRate();
			if (taxesPaid > 0) {
				CivMessage.sendTown(town, CivColor.LightGreen+"Generated "+CivColor.Yellow+df.format(payment)+CivColor.LightGreen+" coins from trade."+
					CivColor.Yellow+" (Paid "+df.format(taxesPaid)+" in taxes to "+town.getDepositCiv().getName()+")");
			} else {
				CivMessage.sendTown(town, CivColor.LightGreen+"Generated "+CivColor.Yellow+df.format(payment)+CivColor.LightGreen+" coins from trade.");
			}
			
			town.getTreasury().deposit(payment - taxesPaid);
			town.getDepositCiv().taxPayment(town, taxesPaid);
		}
	}
	
	@Override
	public void run() {
		if (!CivGlobal.tradeEnabled) {
			return;
		}

		CivGlobal.checkForDuplicateGoodies();
		
		for (Town town : CivGlobal.getTowns()) {
			try {
				processTownsTradePayments(town);
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
	}

}
