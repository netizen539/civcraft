package com.avrgaming.civcraft.command.team;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.arena.Arena;
import com.avrgaming.civcraft.arena.ArenaManager;
import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.JoinTeamResponse;
import com.avrgaming.civcraft.util.CivColor;

public class TeamCommand  extends CommandBase {

	@Override
	public void init() {
		command = "/team";
		displayName = "Team";
		
		commands.put("info", "Lists information about the current team you're in.");
		commands.put("show", "[name] Shows information about the named team.");
		commands.put("create", "[name] Creates a team with the given name.");
		commands.put("leave", "Leaves your current team.");
		commands.put("disband", "Disbands your current team. You must be the team leader to do this.");
		commands.put("add", "[resident] Add a resident to your team.");
		commands.put("remove", "[resident] removes a resident from your team.");
		commands.put("changeleader", "[resident] - Gives team leadership to another team member.");
		commands.put("arena", "Join the queue to fight the arena! Will take us out of the queue if we're already in it.");
		commands.put("top5", "Shows top 5 teams in the game!");
		commands.put("top10", "Shows top 10 teams in the game!");
		commands.put("list", "List all teams in the game.");
		commands.put("surrender", "Give up on the current match.");
	}
	
	public void surrender_cmd() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasTeam()) {
			throw new CivException("You must be part of a team to use this command.");
		}
		
		if (!resident.isTeamLeader()) {
			throw new CivException("Only leaders can surrender during a match.");
		}
		
		ArenaTeam team = resident.getTeam();
		Arena arena = team.getCurrentArena();
		
		if (arena == null) {
			throw new CivException("Your team is not currently in arena match");
		}
		
		ArenaTeam otherTeam = null;
		for (ArenaTeam t : arena.getTeams()) {
			if (t != team) {
				otherTeam = t;
				break;
			}
		}
		
		if (otherTeam == null) {
			throw new CivException("Error, couldn't find other team to surrender to.");
		}
		
		ArenaManager.declareVictor(arena, team, otherTeam);
		CivMessage.sendSuccess(sender, "Surrendered.");

	}
	
	public void arena_cmd() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasTeam()) {
			throw new CivException("You're already not part of a team.");
		}
		
		if (!resident.isTeamLeader()) {
			throw new CivException("Only leaders can add their team to the arena queue.");
		}
		
		ArenaTeam team = resident.getTeam();
		
		if (team.getCurrentArena() != null) {
			throw new CivException("Cannot join the arena queue while inside the arena.");
		}

		for (ArenaTeam t : ArenaManager.teamQueue) {
			if (t == team) {
				ArenaManager.teamQueue.remove(t);
				CivMessage.sendSuccess(sender, "Removed our team from the queue.");
				return;
			}
		}
		
		ArenaManager.addTeamToQueue(team);
		CivMessage.sendSuccess(sender, "Added our team to the queue.");
	}
	
	
	public void list_cmd() {
		CivMessage.sendHeading(sender, "Teams");
		String out = "";
		
		for (ArenaTeam team : ArenaTeam.arenaTeams.values()) {
			out += team.getName()+", ";
		}
		
		CivMessage.send(sender, out);
	}
	
	
	public void top5_cmd() {
		CivMessage.sendHeading(sender, "Top 5 Teams");
		
		for (int i = 0; ((i < 5) && (i < ArenaTeam.teamRankings.size())); i++) {
			ArenaTeam team = ArenaTeam.teamRankings.get(i);
			CivMessage.send(sender, CivColor.Green+team.getName()+": "+CivColor.LightGreen+team.getLadderPoints());
		}
	}
	
	public void top10_cmd() {
		CivMessage.sendHeading(sender, "Top 10 Teams");
		
		for (int i = 0; ((i < 10) && (i < ArenaTeam.teamRankings.size())); i++) {
			ArenaTeam team = ArenaTeam.teamRankings.get(i);
			CivMessage.send(sender, CivColor.Green+team.getName()+": "+CivColor.LightGreen+team.getLadderPoints());
		}
	}
	
	public void printTeamInfo(ArenaTeam team) {
		CivMessage.sendHeading(sender, "Team "+team.getName());
		CivMessage.send(sender, CivColor.Green+"Points: "+CivColor.LightGreen+team.getLadderPoints()+
								CivColor.Green+" Leader: "+CivColor.LightGreen+team.getLeader().getName());
		CivMessage.send(sender, CivColor.Green+"Members: "+CivColor.LightGreen+team.getMemberListSaveString());
	}

	public void info_cmd() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasTeam()) {
			throw new CivException("You're not currently part of a team.");
		}
		
		ArenaTeam team = resident.getTeam();
		printTeamInfo(team);
	}
	
	public void show_cmd() throws CivException {
		ArenaTeam team = getNamedTeam(1);
		printTeamInfo(team);
	}
	
	public void create_cmd() throws CivException {
		String teamName = getNamedString(1, "Enter a name for your team.");
		Resident resident = getResident();
		
		if (resident.isProtected()) {
			throw new CivException("You can not form a team while protected.");
		}
		
		if (resident.hasTeam()) {
			throw new CivException("You can only be on one team at time. Leave your current team first.");
		}
		
		
		ArenaTeam.createTeam(teamName, resident);
		CivMessage.sendSuccess(sender, "Team Successfully Created.");
	}
	
	public void leave_cmd() throws CivException {
		Resident resident = getResident();
		
		if (!resident.hasTeam()) {
			throw new CivException("You're already not part of a team.");
		}
		
		if (resident.isTeamLeader()) {
			throw new CivException("Leaders cannot leave their own team. They must change the leader or disband the team first.");
		}
		
		ArenaTeam team = resident.getTeam();
		
		if (team.getCurrentArena() != null) {
			throw new CivException("Cannot leave your team while it is inside the arena.");
		}
		
		ArenaTeam.removeMember(team.getName(), resident);
		CivMessage.sendSuccess(sender, "Left Team "+team.getName());
		CivMessage.sendTeam(team, resident.getName()+" has left the team.");
	}
	
	public void disband_cmd() throws CivException {
		Resident resident = getResident();
		
		if (!resident.isTeamLeader()) {
			throw new CivException("You must have a team and be it's leader to disband your team.");
		}
		
		if (resident.getTeam().getCurrentArena() != null) {
			throw new CivException("Cannot disband your team while it is inside the arena.");
		}
		
		String teamName = resident.getTeam().getName();
		ArenaTeam.deleteTeam(teamName);
		ArenaTeam.arenaTeams.remove(teamName);
		CivMessage.sendSuccess(sender, "Disbanded team: "+teamName);
	}
	
	public void add_cmd() throws CivException {
		Resident resident = getResident();
		Resident member = getNamedResident(1);
		
		if (!resident.isTeamLeader()) {
			throw new CivException("You must have a team and be it's leader to add members to your team.");
		}
		
		if (member.hasTeam()) {
			throw new CivException(member.getName()+" is already on a team.");
		}
		
		if (resident.getTeam().getCurrentArena() != null) {
			throw new CivException("Cannot add players to team while inside the arena.");
		}
		
		try {
			Player player = CivGlobal.getPlayer(member);
			
			if (member.isProtected()) {
				throw new CivException(player.getName()+" is protected and unable to join a team");
			}
			
			ArenaTeam team = resident.getTeam();
			JoinTeamResponse join = new JoinTeamResponse();
			join.team = team;
			join.resident = member;
			join.sender = (Player)sender;
					
			CivGlobal.questionPlayer(CivGlobal.getPlayer(resident), player, 
					"Would you like to join team "+team.getName()+"?",
					30000, join);
			
		} catch (CivException e) {
			throw new CivException(e.getMessage());
		}
				
		CivMessage.sendSuccess(sender, "Sent invitation to "+member.getName());
	}
	
	public void remove_cmd() throws CivException {
		Resident resident = getResident();
		Resident member = getNamedResident(1);
		
		if (!resident.isTeamLeader()) {
			throw new CivException("You must have a team and be it's leader to remove members to your team.");
		}
		
		if (resident.getTeam().getCurrentArena() != null) {
			throw new CivException("Cannot remove players from the team while inside the arena.");
		}
		
		ArenaTeam.removeMember(resident.getTeam().getName(), member);
		CivMessage.sendSuccess(sender, "Removed Team Member "+member.getName());
		CivMessage.sendTeam(resident.getTeam(), member.getName()+" has left the team.");

	}
	
	public void changeleader_cmd() throws CivException {
		Resident resident = getResident();
		Resident member = getNamedResident(1);
		
		if (!resident.isTeamLeader()) {
			throw new CivException("You must have a team and be it's leader to change team leaders.");
		}
		
		ArenaTeam team = resident.getTeam();
		
		if (team.getCurrentArena() != null) {
			throw new CivException("Cannot change team leaders while inside the arena.");
		}
		
		if (!team.hasMember(member)) {
			throw new CivException(member.getName()+" must already be added to your team in order to become it's leader.");
		}
		
		team.setLeader(member);
		team.save();
		
		CivMessage.sendSuccess(sender, "Changed team leader to "+member.getName());
		CivMessage.sendSuccess(member, "You are now leader of team "+team.getName());
		CivMessage.sendTeam(team, resident.getName()+" has changed the team leader to "+member.getName());
		
	}
	
	@Override
	public void doDefaultAction() throws CivException {
		showHelp();
		
	}

	@Override
	public void showHelp() {
		showBasicHelp();
	}

	@Override
	public void permissionCheck() throws CivException {

	}

	
	
}
