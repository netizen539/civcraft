package com.avrgaming.civcraft.command.admin;

import com.avrgaming.civcraft.arena.Arena;
import com.avrgaming.civcraft.arena.ArenaManager;
import com.avrgaming.civcraft.arena.ArenaTeam;
import com.avrgaming.civcraft.command.CommandBase;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.util.CivColor;

public class AdminArenaCommand extends CommandBase {

	@Override
	public void init() {
		command = "/ad arena";
		displayName = "Admin Arena";
				
		commands.put("list", "Lists all active arenas and which teams are in them.");
		commands.put("end", "[name] end the arena with this id.");
		commands.put("messageall", "[msg] send a message to all arenas.");
		commands.put("message", "[id] [msg] send a message to this arena.");
		commands.put("enable", "Enable arenas globally.");
		commands.put("disable", "Disable arenas globally.");
	}

	public void enable_cmd() {
		ArenaManager.enabled = true;
		CivMessage.sendSuccess(sender, "Arenas Enabled");
	}
	
	public void disable_cmd() {
		ArenaManager.enabled = false;
		CivMessage.sendSuccess(sender, "Arenas Disabled");
	}
	
	public void list_cmd() {
		CivMessage.sendHeading(sender, "Active Arenas");
		for (Arena arena : ArenaManager.activeArenas.values()) {
			String teams = "";
			for (ArenaTeam team : arena.getTeams()) {
				teams += team.getName()+", ";
			}

			CivMessage.send(sender, arena.getInstanceName()+": Teams: "+teams);
		}
	}
	
	public void messageall_cmd() {
		String message = this.combineArgs(this.stripArgs(args, 1));
		for (Arena arena : ArenaManager.activeArenas.values()) {
			CivMessage.sendArena(arena, CivColor.Rose+"ADMIN:"+CivColor.RESET+message);
		}
		CivMessage.send(sender, CivColor.Rose+"ADMIN:"+CivColor.RESET+message);
	}
	
	public void message_cmd() throws CivException {
		String id = getNamedString(1, "Enter arena instance name");
		String message = this.combineArgs(this.stripArgs(args, 2));

		Arena arena = ArenaManager.activeArenas.get(id);
		if (arena == null) {
			throw new CivException("No arena with that id found.");
		}
		
		CivMessage.sendArena(arena, CivColor.Rose+"ADMIN:"+CivColor.RESET+message);
		CivMessage.send(sender, CivColor.Rose+"ADMIN:"+CivColor.RESET+message);

	}
	
	public void end_cmd() throws CivException {
		String id = getNamedString(1, "Enter arena instance name");
		
		Arena arena = ArenaManager.activeArenas.get(id);
		if (arena == null) {
			throw new CivException("No arena with that id found.");
		}
		
		CivMessage.sendArena(arena, CivColor.Rose+"An Admin is ending this arena in a draw.");
		ArenaManager.declareDraw(arena);
		
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
