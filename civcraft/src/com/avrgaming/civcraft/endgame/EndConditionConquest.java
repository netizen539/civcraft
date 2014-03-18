package com.avrgaming.civcraft.endgame;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.wonders.Wonder;

public class EndConditionConquest extends EndGameCondition {

	int daysAfterStart;
	double percentCaptured;
	double percentCapturedWithWonder;

	Date startDate = null;
	
	@Override
	public void onLoad() {
		daysAfterStart = Integer.valueOf(this.getString("days_after_start"));
		percentCaptured = Double.valueOf(this.getString("percent_captured"));
		percentCapturedWithWonder = Double.valueOf(this.getString("percent_captured_with_wonder"));
		getStartDate();
	}
	
	private void getStartDate() {
		String key = "endcondition:conquest:startdate";
		
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);
		if (entries.size() == 0) {
			/* Start date is now! */
			startDate = new Date();
			CivGlobal.getSessionDB().add(key, ""+startDate.getTime(), 0, 0, 0);
			return;
		} else {
			long time = Long.valueOf(entries.get(0).value);
			startDate = new Date(time);
		}
 	}
	
	private boolean isAfterStartupTime() {
		Calendar startCal = Calendar.getInstance();
		startCal.setTime(startDate);
		
		Calendar now = Calendar.getInstance();
		
		startCal.add(Calendar.DATE, daysAfterStart);
		
		if (now.after(startCal)) {
			return true;
		}
		return false;
	}
	
	@Override
	public String getSessionKey() {
		return "endgame:conquer";
	}
	
	@Override
	public boolean check(Civilization civ) {
		if (!isAfterStartupTime()) {
			return false;
		}
		
		boolean hasChichenItza = false;
		for (Town town : civ.getTowns()) {
			if (town.getMotherCiv() != null) {
				continue;
			}
			
			for (Wonder wonder :town.getWonders()) {
				if (wonder.isActive()) {
					if (wonder.getConfigId().equals("w_chichen_itza")) {
						hasChichenItza = true;
						break;
					}
				}
			}
			
			if (hasChichenItza) {
				break;
			}
		}
		
		if (!hasChichenItza) {	
			if (civ.getPercentageConquered() < percentCaptured) {
				return false;
			}
		} else {
			if (civ.getPercentageConquered() < percentCapturedWithWonder) {
				return false;
			}
		}
				
		if (civ.isConquered()) {
			return false;
		}
		
		return true;
	}

	@Override
	protected void onWarDefeat(Civilization civ) {
		this.onFailure(civ);
	}

}
