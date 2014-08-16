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
import java.util.ArrayList;

import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import com.avrgaming.civcraft.components.NonMemberFeeComponent;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.StoreMaterial;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.util.CivColor;

public class Store extends Structure {
	
	private int level = 1;
	
	private NonMemberFeeComponent nonMemberFeeComponent;
	
	ArrayList<StoreMaterial> materials = new ArrayList<StoreMaterial>();
	
	protected Store(Location center, String id, Town town) throws CivException {
		super(center, id, town);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onSave();
	}
	
	protected Store(ResultSet rs) throws SQLException, CivException {
		super(rs);
		nonMemberFeeComponent = new NonMemberFeeComponent(this);
		nonMemberFeeComponent.onLoad();
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
		nonMemberFeeComponent.setFeeRate(nonResidentFee);
	}
	
	private String getNonResidentFeeString() {
		return "Fee: "+((int)(nonMemberFeeComponent.getFeeRate()*100) + "%").toString();		
	}
	
	public void addStoreMaterial(StoreMaterial mat) throws CivException {
		if (materials.size() >= 4) {
			throw new CivException("Store is full.");
		}
		materials.add(mat);
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
	
	@Override
	public void updateSignText() {
		int count = 0;

		
		// iterate through materials, set signs using array...
		
		for (StoreMaterial mat : this.materials) {
			StructureSign sign = getSignFromSpecialId(count);
			if (sign == null) {
				CivLog.error("sign from special id was null, id:"+count);
				return;
			}
			
			sign.setText("Buy 64\n"+
		             mat.name+"\n"+
				     "For "+(int)mat.price+" Coins\n"+
		             getNonResidentFeeString());
			sign.update();
			count++;
		}
		
		// We've finished with all of the materials, update the empty signs to show correct text.
		for (; count < getSigns().size(); count++) {
			StructureSign sign = getSignFromSpecialId(count);
			sign.setText("Store Self\nEmpty");
			sign.update();
		}
	}
	
	@Override
	public void processSignAction(Player player, StructureSign sign, PlayerInteractEvent event) {
		int special_id = Integer.valueOf(sign.getAction());
		if (special_id < this.materials.size()) {
			StoreMaterial mat = this.materials.get(special_id);
			sign_buy_material(player, mat.name, mat.type, mat.data, 64, mat.price);
		} else {
			CivMessage.send(player, CivColor.Rose+"Store shelf empty, stock it using /town upgrade.");
		}
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
					CivMessage.send(player, CivColor.Yellow + "Paid "+ payToTown+" coins in non-resident taxes.");
				}
			
			}
			catch (CivException e) {
				CivMessage.send(player, CivColor.Rose + e.getMessage());
			}
		return;
	}

	@Override
	public String getDynmapDescription() {
		String out = "<u><b>Store</u></b><br/>";
		if (this.materials.size() == 0) {
			out += "Nothing stocked.";
		} 
		else {
			for (StoreMaterial mat : this.materials) {
				out += mat.name+" for "+mat.price+"<br/>";
			}
		}
		return out;
	}
	
	@Override
	public String getMarkerIconName() {
		return "bricks";
	}

	public void reset() {
		this.materials.clear();
		this.updateSignText();
	}
	
}
