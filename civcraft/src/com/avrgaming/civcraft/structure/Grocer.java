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

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigGrocerLevel;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class Grocer extends Structure {

	private int level = 1;

	private NonMemberFeeComponent nonMemberFeeComponent; 
	
	protected Grocer(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onSave();
	}

	public Grocer(ResultSet rs) throws SQLException, CivException {
		super(rs);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onLoad();
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>Grocer</u></b><br/>";

		for (int i = 0; i < level; i++) {
			ConfigGrocerLevel grocerlevel = CivSettings.grocerLevels.get(i+1);
			out += "<b>"+grocerlevel.itemName+"</b> Amount: "+grocerlevel.amount+ " Price: "+grocerlevel.price+" coins.<br/>";
		}
		
		return out;
	}
	
	@Override
	public String getMarkerIconName() {
		return "cutlery";
	}

	public int getLevel() {
		return level;
	}

	public void setLevel(int level) {
		this.level = level;
	}

	public double getNonResidentFee() {
		return nonMemberFeeComponent.getFeeRate();
	}

	public void setNonResidentFee(double nonResidentFee) {
		this.nonMemberFeeComponent.setFeeRate(nonResidentFee);
	}
	
	private String getNonResidentFeeString() {
		return "Fee: "+((int)(getNonResidentFee()*100) + "%").toString();		
	}
	
	private StructureSign getSignFromSpecialId(int special_id) {
		for (StructureSign sign : getSigns()) {
			int id = Integer.valueOf(sign.getAction());
			if (id == special_id) {
				return sign;
			}
		}
		return null;
	}
	
	public void sign_buy_material(Player player, String itemName, int id, byte data, int amount, double price) {
		Resident resident;
		int payToTown = (int) Math.round(price*this.getNonResidentFee());
		try {
				
				resident = CivGlobal.getResident(player.getName());
				Town t = resident.getTown();
			
				if (t == this.getTown()) {
					// Pay no taxes! You're a member.
					resident.buyItem(itemName, id, data, price, amount);
					CivMessage.send(player, CivColor.LightGreen + "Bought "+amount+" "+itemName+" for "+ price+ " coins.");
					return;
				} else {
					// Pay non-resident taxes
					resident.buyItem(itemName, id, data, price + payToTown, amount);
					getTown().depositDirect(payToTown);
					CivMessage.send(player, CivColor.LightGreen + "Bought "+amount+" "+itemName+" for "+ price+ " coins.");
					CivMessage.send(player, CivColor.Yellow + "Paid "+ payToTown+" coins in non-resident taxes.");
				}
			
			}
			catch (CivException e) {
				CivMessage.send(player, CivColor.Rose + e.getMessage());
			}
		return;
	}

	
	@Override
	public void updateSignText() {
		int count = 0;
	
		for (count = 0; count < level; count++) {
			StructureSign sign = getSignFromSpecialId(count);
			ConfigGrocerLevel grocerlevel = CivSettings.grocerLevels.get(count+1);
			
			sign.setText("Buy\n"+grocerlevel.itemName+"\n"+
						 "For "+grocerlevel.price+" Coins\n"+
					     getNonResidentFeeString());
			
			sign.update();
		}
		
		for (; count < getSigns().size(); count++) {
			StructureSign sign = getSignFromSpecialId(count);
			sign.setText("Grocer Shelf\nEmpty");
			sign.update();
		}
		
	}
	
	@Override
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
		int special_id = Integer.valueOf(sign.getAction());
		if (special_id < this.level) {
			ConfigGrocerLevel grocerlevel = CivSettings.grocerLevels.get(special_id+1);
			sign_buy_material(player, grocerlevel.itemName, grocerlevel.itemId, 
					(byte)grocerlevel.itemData, grocerlevel.amount, grocerlevel.price);
		} else {
			CivMessage.send(player, CivColor.Rose+"Grocer shelf empty, stock it using /town upgrade.");
		}
	}
	
	
}
