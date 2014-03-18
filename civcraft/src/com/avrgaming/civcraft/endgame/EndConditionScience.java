package com.avrgaming.civcraft.endgame;

import java.util.ArrayList;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.wonders.Wonder;

public class EndConditionScience extends EndGameCondition {

	String techname;
	
	@Override
	public void onLoad() {		
		techname = this.getString("tech");
	}

	@Override
	public boolean check(Civilization civ) {
		
		if (!civ.hasTechnology(techname)) {
			return false;
		}

		if (civ.isAdminCiv()) {
			return false;
		}
		
		boolean hasGreatLibrary = false;
		for (Town town : civ.getTowns()) {
			if (town.getMotherCiv() != null) {
				continue;
			}
			
			for (Wonder wonder :town.getWonders()) {
				if (wonder.isActive()) {
					if (wonder.getConfigId().equals("w_greatlibrary")) {
						hasGreatLibrary = true;
						break;
					}
				}
			}
			
			if (hasGreatLibrary) {
				break;
			}
		}
		
		if (!hasGreatLibrary) {
			return false;
		}
	
		return true;
	}
	
	@Override
	public boolean finalWinCheck(Civilization civ) {
		Civilization rival = getMostAccumulatedBeakers();
		if (rival != civ) {
			CivMessage.global(civ.getName()+" doesn't have enough beakers for a scientific victory. The rival civilization of "+rival.getName()+" has more!");
			return false;
		}
		
		return true;
	}

	public Civilization getMostAccumulatedBeakers() {
		double most = 0;
		Civilization mostCiv = null;
		
		for (Civilization civ : CivGlobal.getCivs()) {
			double beakers = getExtraBeakersInCiv(civ);
			if (beakers > most) {
				most = beakers;
				mostCiv = civ;
			}
		}
		
		return mostCiv;
	}
	
	@Override
	public String getSessionKey() {
		return "endgame:science";
	}

	@Override
	protected void onWarDefeat(Civilization civ) {
		/* remove any extra beakers we might have. */
		CivGlobal.getSessionDB().delete_all(getBeakerSessionKey(civ));
		civ.removeTech(techname);
		CivMessage.sendCiv(civ, "We were defeated while trying to achieve a science victory! We've lost all of our accumulated beakers and our victory tech!");
		
		civ.save();
		this.onFailure(civ);
	}

	public static String getBeakerSessionKey(Civilization civ) {
		return "endgame:sciencebeakers:"+civ.getId();
	}
	
	public double getExtraBeakersInCiv(Civilization civ) {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getBeakerSessionKey(civ));
		if (entries.size() == 0) {
			return 0;
		}
		return Double.valueOf(entries.get(0).value);
	}
	
	public void addExtraBeakersToCiv(Civilization civ, double beakers) {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getBeakerSessionKey(civ));
		double current = 0;
		if (entries.size() == 0) {
			CivGlobal.getSessionDB().add(getBeakerSessionKey(civ), ""+beakers, civ.getId(), 0, 0);
			current += beakers;
		} else {
			current = Double.valueOf(entries.get(0).value);
			current += beakers;
			CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, ""+current);
		}
		//DecimalFormat df = new DecimalFormat("#.#");
		//CivMessage.sendCiv(civ, "Added "+df.format(beakers)+" beakers to our scientific victory! We now have "+df.format(current)+" beakers saved up.");
	}

	public static Double getBeakersFor(Civilization civ) {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getBeakerSessionKey(civ));
		if (entries.size() == 0) {
			return 0.0;
		} else {
			return Double.valueOf(entries.get(0).value);
		}
	}

}
