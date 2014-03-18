package com.avrgaming.civcraft.endgame;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.structure.wonders.Wonder;

public class EndConditionDiplomacy extends EndGameCondition {

	public static int vote_cooldown_hours;
	
	@Override
	public void onLoad() {
		vote_cooldown_hours = Integer.valueOf(this.getString("vote_cooldown_hours"));
	}

	@Override
	public boolean check(Civilization civ) {

		boolean hasCouncil = false;
		for (Town town : civ.getTowns()) {
			if (town.getMotherCiv() != null) {
				continue;
			}
			
			for (Wonder wonder :town.getWonders()) {
				if (wonder.isActive()) {
					if (wonder.getConfigId().equals("w_council_of_eight")) {
						hasCouncil = true;
						break;
					}
				}
			}
			
			if (hasCouncil) {
				break;
			}
		}
		
		if (!hasCouncil) {
			return false;
		}
		
		if (civ.isAdminCiv()) {
			return false;
		}
		
		if (civ.isConquered()) {
			return false;
		}
		
		return true;
	}

	@Override
	public String getSessionKey() {
		return "endgame:diplomacy";
	}

	@Override
	protected void onWarDefeat(Civilization civ) {
		for (Town town : civ.getTowns()) {
			if (town.getMotherCiv() != null) {
				continue;
			}
			
			for (Wonder wonder :town.getWonders()) {
				if (wonder.getConfigId().equals("w_council_of_eight")) {
					if (wonder.isActive()) {
						wonder.fancyDestroyStructureBlocks();
						wonder.getTown().removeWonder(wonder);
						try {
							wonder.delete();
						} catch (SQLException e) {
							e.printStackTrace();
						}
					}
					break;
				}
			}
		}
		
		deleteAllVotes(civ);
		this.onFailure(civ);
	}
	
	@Override
	public void onVictoryReset(Civilization civ) {
		deleteAllVotes(civ);
	}
	
	public static boolean canPeopleVote() {
		for (Wonder wonder : CivGlobal.getWonders()) {
			if (wonder.isActive() && wonder.getConfigId().equals("w_council_of_eight")) {
				return true;
			}
		}
		
		return false;
	}
	
	@Override
	public boolean finalWinCheck(Civilization civ) {
		Integer votes = getVotesFor(civ);
		
		for (Civilization otherCiv : CivGlobal.getCivs()) {
			if (otherCiv == civ) {
				continue;
			}
			
			Integer otherVotes = getVotesFor(otherCiv);
			if (otherVotes > votes) {
				CivMessage.global(civ.getName()+" doesn't have enough votes for a diplomatic victory! The rival civilization of "+otherCiv.getName()+" has more!");
				return false;
			}
		}
		
		return true;
	}
	
	public static String getVoteSessionKey(Civilization civ) {
		return "endgame:diplomacyvote:"+civ.getId();
	}
	
	public static void deleteAllVotes(Civilization civ) {
		CivGlobal.getSessionDB().delete_all(getVoteSessionKey(civ));
	}
	
	public static void addVote(Civilization civ, Resident resident) {
		/* validate that we can vote. */
		if (!canVoteNow(resident)) {
			return;
		}
		
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getVoteSessionKey(civ));
		if (entries.size() == 0) {
			CivGlobal.getSessionDB().add(getVoteSessionKey(civ), ""+1, civ.getId(), 0, 0);
		} else {
			Integer votes = Integer.valueOf(entries.get(0).value);
			votes++;
			CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, ""+votes);			
		}
		
		CivMessage.sendSuccess(resident, "Added a vote for "+civ.getName());
	}

	public static void setVotes(Civilization civ, Integer votes) {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getVoteSessionKey(civ));
		if (entries.size() == 0) {
			CivGlobal.getSessionDB().add(getVoteSessionKey(civ), ""+votes, civ.getId(), 0, 0);
		} else {
			CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, ""+votes);			
		}		
	}
	
	public static Integer getVotesFor(Civilization civ) {
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getVoteSessionKey(civ));
		if (entries.size() == 0) {
			return 0;
		}
		
		return Integer.valueOf(entries.get(0).value);
	}

	private static boolean canVoteNow(Resident resident) {
		String key = "endgame:residentvote:"+resident.getName();
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(key);
		if (entries.size() == 0) {
			CivGlobal.getSessionDB().add(key, (new Date()).getTime()+"", 0, 0, 0);
			return true;
		} else {
			Date then = new Date(Long.valueOf(entries.get(0).value));
			Date now = new Date();
			if (now.getTime() > (then.getTime() + (vote_cooldown_hours*60*60*1000))) {
				CivGlobal.getSessionDB().update(entries.get(0).request_id, entries.get(0).key, now.getTime()+"");
				return true;
			}
		}

		CivMessage.sendError(resident, "You must wait 24 hours before casting another vote.");
		return false;
	}

}
