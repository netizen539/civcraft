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
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.DecimalFormat;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.lorestorage.LoreMaterial;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Buff;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.SimpleBlock;

public class Bank extends Structure {
	
	private int level = 1;
	private double interestRate = 0;
	
	private NonMemberFeeComponent nonMemberFeeComponent;
	
//	private static final int EMERALD_SIGN = 3;
	private static final int IRON_SIGN = 0;
	private static final int GOLD_SIGN = 1;
	private static final int DIAMOND_SIGN = 2;
	private static final int EMERALD_SIGN = 3;
	
	protected Bank(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onSave();
	}
	
	public Bank(ResultSet rs) throws SQLException, CivException {
		super(rs);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onLoad();
	}

	public double getBankExchangeRate() {
		double exchange_rate = 0.0;
		switch (level) {
		case 1:
			exchange_rate = 0.40;
			break;
		case 2:
			exchange_rate = 0.50;
			break;
		case 3:
			exchange_rate = 0.60;
			break;
		case 4:
			exchange_rate = 0.70;
			break;
		case 5:
			exchange_rate = 0.80;
			break;
		case 6:
			exchange_rate = 0.90;
			break;
		case 7:
			exchange_rate = 1;
			break;
		case 8:
			exchange_rate = 1.20;
			break;
		case 9:
			exchange_rate = 1.50;
			break;
		case 10:
			exchange_rate = 2;
			break;
		}
		
		double rate = 1;
		double addtional = rate*this.getTown().getBuffManager().getEffectiveDouble(Buff.BARTER);
		rate += addtional;
		if (rate > 1) {
			exchange_rate *= rate;
		}
		return exchange_rate;
	}
	
	@Override
	public void onBonusGoodieUpdate() {
		this.updateSignText();
	}
	
	private String getExchangeRateString() {
		return ((int)(getBankExchangeRate()*100) + "%").toString();		
	}
	
	private String getNonResidentFeeString() {
		return "Fee: "+((int)(this.nonMemberFeeComponent.getFeeRate()*100) + "%").toString();		
	}
	
	private String getSignItemPrice(int signId) {
		double itemPrice;
		if (signId == IRON_SIGN) {
			itemPrice = CivSettings.iron_rate;
		}
		else if (signId == GOLD_SIGN) {
			itemPrice = CivSettings.gold_rate;
		}
		else if (signId == DIAMOND_SIGN) {
			itemPrice = CivSettings.diamond_rate;
		} else {
			itemPrice = CivSettings.emerald_rate;
		}
		
		
		String out = "1 = ";
		out += (int)(itemPrice*getBankExchangeRate());
		out += " Coins";
		return out;
	}
	
	public void exchange_for_coins(Resident resident, int itemId, double coins) throws CivException {
		double exchange_rate = 0.0;
		String itemName;
		Player player = CivGlobal.getPlayer(resident);
		
		if (itemId == CivData.IRON_INGOT)
			itemName = "Iron";
		else if (itemId == CivData.GOLD_INGOT)
			itemName = "Gold";
		else if (itemId == CivData.DIAMOND)
			itemName = "Diamond";
		else
			itemName = "Emerald";
		
		exchange_rate = getBankExchangeRate();
				
		if (!resident.takeItemInHand(itemId, 0, 1)) {
			throw new CivException("You do not have enough "+itemName+" in your hand.");
		}
		
		Town usersTown = resident.getTown();
		
		// Resident is in his own town.
		if (usersTown == this.getTown()) {		
			DecimalFormat df = new DecimalFormat();
			resident.getTreasury().deposit((double)((int)(coins*exchange_rate)));
			CivMessage.send(player,
					CivColor.LightGreen + "Exchanged 1 "+itemName+" for "+ df.format(coins*exchange_rate)+ " coins.");	
			return;
		}
		
		// non-resident must pay the town's non-resident tax
		double giveToPlayer = (double)((int)(coins*exchange_rate));
		double giveToTown = (double)((int)giveToPlayer*this.getNonResidentFee());
		giveToPlayer -= giveToTown;
		
		giveToTown = Math.round(giveToTown);
		giveToPlayer = Math.round(giveToPlayer);
		
			this.getTown().depositDirect(giveToTown);
			resident.getTreasury().deposit(giveToPlayer);
		
		CivMessage.send(player, CivColor.LightGreen + "Exchanged 1 "+itemName+" for "+ giveToPlayer+ " coins.");
		CivMessage.send(player,CivColor.Yellow+" Paid "+giveToTown+" coins in non-resident taxes.");
		return;
		
	}
	
	@Override
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
		//int special_id = Integer.valueOf(sign.getAction());
		Resident resident = CivGlobal.getResident(player);
		
		if (resident == null) {
			return;
		}
		
		try {
			
			if (LoreMaterial.isCustom(player.getItemInHand())) {
				throw new CivException("You cannot exchange this item at the bank.");
			}
			
			switch (sign.getAction()) {
			case "iron":
				exchange_for_coins(resident, CivData.IRON_INGOT, CivSettings.iron_rate);
				break;
			case "gold":
				exchange_for_coins(resident, CivData.GOLD_INGOT, CivSettings.gold_rate);
				break;
			case "diamond":
				exchange_for_coins(resident, CivData.DIAMOND, CivSettings.diamond_rate);
				break;
			case "emerald":
				exchange_for_coins(resident, CivData.EMERALD, CivSettings.emerald_rate);
				break;
			}
		} catch (CivException e) {
			CivMessage.send(player, CivColor.Rose+e.getMessage());
		}
	}
	
	@Override
	public void updateSignText() {
		for (StructureSign sign : getSigns()) {
			
			switch (sign.getAction().toLowerCase()) {
			case "iron":
				sign.setText("Iron\n"+
						"At "+getExchangeRateString()+"\n"+
						getSignItemPrice(IRON_SIGN)+"\n"+
						getNonResidentFeeString());
				break;
			case "gold":
				sign.setText("Gold\n"+
						"At "+getExchangeRateString()+"\n"+
						getSignItemPrice(GOLD_SIGN)+"\n"+
						getNonResidentFeeString());
				break;
			case "diamond":
				sign.setText("Diamond\n"+
						getExchangeRateString()+"\n"+
						"At "+getSignItemPrice(DIAMOND_SIGN)+"\n"+
						getNonResidentFeeString());
				break;			
			case "emerald":
					sign.setText("Emerald\n"+
							getExchangeRateString()+"\n"+
							"At "+getSignItemPrice(EMERALD_SIGN)+"\n"+
							getNonResidentFeeString());
					break;
			}
				
			
			sign.update();
		}
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>Bank</u></b><br/>";
		out += "Level: "+this.level;
		return out;
	}
	
	@Override
	public String getMarkerIconName() {
		return "bank";
	}
	
	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public double getNonResidentFee() {
		return this.nonMemberFeeComponent.getFeeRate();
	}

	public void setNonResidentFee(double nonResidentFee) {
		this.nonMemberFeeComponent.setFeeRate(nonResidentFee);
	}

	public double getInterestRate() {
		return interestRate;
	}

	public void setInterestRate(double interestRate) {
		this.interestRate = interestRate;
	}
	
	@Override
	public void onLoad() {
		/* Process the interest rate. */
		if (interestRate == 0.0) {
			this.getTown().getTreasury().setPrincipalAmount(0);
			return;
		}
		
		/* Update the principal with the new value. */
		this.getTown().getTreasury().setPrincipalAmount(this.getTown().getTreasury().getBalance());
	}
	
	@Override
	public void onDailyEvent() {
		
		/* Process the interest rate. */
		double effectiveInterestRate = interestRate;
		if (effectiveInterestRate == 0.0) {
			this.getTown().getTreasury().setPrincipalAmount(0);
			return;
		}
		
		double principal = this.getTown().getTreasury().getPrincipalAmount();
		
		if (this.getTown().getBuffManager().hasBuff("buff_greed")) {
			double increase = this.getTown().getBuffManager().getEffectiveDouble("buff_greed");
			effectiveInterestRate += increase;
			CivMessage.sendTown(this.getTown(), CivColor.LightGray+"Your goodie buff 'Greed' has increased the interest our town generated.");
		}
		
		double newCoins = principal*effectiveInterestRate;

		//Dont allow fractional coins.
		newCoins = Math.floor(newCoins);
		
		if (newCoins != 0) {
			CivMessage.sendTown(this.getTown(), CivColor.LightGreen+"Our town earned "+newCoins+" coins from interest on a principal of "+principal+" coins.");
			this.getTown().getTreasury().deposit(newCoins);
			
		}
		
		/* Update the principal with the new value. */
		this.getTown().getTreasury().setPrincipalAmount(this.getTown().getTreasury().getBalance());
		
	}
	
	@Override
	public void onPostBuild(BlockCoord absCoord, SimpleBlock commandBlock) {
		this.level = getTown().saved_bank_level;
		this.interestRate = getTown().saved_bank_interest_amount;
	}

	public NonMemberFeeComponent getNonMemberFeeComponent() {
		return nonMemberFeeComponent;
	}

	public void setNonMemberFeeComponent(NonMemberFeeComponent nonMemberFeeComponent) {
		this.nonMemberFeeComponent = nonMemberFeeComponent;
	}
	
	public void onGoodieFromFrame() {
		this.updateSignText();
	}

	public void onGoodieToFrame() {
		this.updateSignText();
	}
	
}
