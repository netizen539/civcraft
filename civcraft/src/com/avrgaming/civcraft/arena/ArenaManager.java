package com.avrgaming.civcraft.arena;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.Random;

import net.minecraft.util.org.apache.commons.io.FileUtils;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.block.Sign;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.DisplaySlot;
import org.bukkit.scoreboard.Objective;
import org.bukkit.scoreboard.Score;
import org.bukkit.scoreboard.ScoreboardManager;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigArena;
import com.avrgaming.civcraft.config.ConfigArenaTeam;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.TimeTools;

public class ArenaManager implements Runnable {

	public static HashMap<BlockCoord, ArenaControlBlock> arenaControlBlocks = new HashMap<BlockCoord, ArenaControlBlock>();
	public static HashMap<BlockCoord, Arena> chests = new HashMap<BlockCoord, Arena>();
	public static HashMap<BlockCoord, Arena> respawnSigns = new HashMap<BlockCoord, Arena>();
	public static HashMap<String, Arena> activeArenas = new HashMap<String, Arena>();
		
	public static Queue<ArenaTeam> teamQueue = new LinkedList<ArenaTeam>();
	public static final int MAX_INSTANCES = 1;
	public static ScoreboardManager scoreboardManager = Bukkit.getScoreboardManager();
	public static boolean enabled = true;
	
	@Override
	public void run() {
		
		/*
		 * Set up an arena if there is room to do so.
		 */
		if (activeArenas.size() < MAX_INSTANCES && enabled) {
			ArenaTeam team1 = teamQueue.poll();
			if (team1 == null) {
				/* No teams waiting in queue. Do nothing. */
				return;
			}
			
			ArenaTeam team2 = teamQueue.poll();
			if (team2 == null) {
				/* We need another team to start a match, requeue our team and wait. */
				CivMessage.sendTeam(team1, "No other teams waiting in queue yet, please wait.");
				teamQueue.add(team1);
				return;
			}
			
			/* Choose a random arena. */
			Random rand = new Random();
			int index = rand.nextInt(CivSettings.arenas.size());
			
			int i = 0;
			ConfigArena arena = null;
			
			for (ConfigArena a : CivSettings.arenas.values()) {
				if (i == index) {
					arena = a;
					break;
				}
				i++;
			}
			
			if (arena == null) {
				CivLog.error("Couldn't find an arena configured....");
				return;
			}
			
			/* Create a new arena. */
			try {
				Arena activeArena = createArena(arena);
				CivMessage.sendTeam(team1, "Teleporting our team to the arena in 10 seconds...");
				CivMessage.sendTeam(team2, "Teleporting our team to the arena in 10 seconds...");

				class SyncTask implements Runnable {
					Arena arena;
					ArenaTeam team1;
					ArenaTeam team2;
					
					public SyncTask(Arena arena, ArenaTeam team1, ArenaTeam team2) {
						this.arena = arena;
						this.team1 = team1;
						this.team2 = team2;
					}

					@Override
					public void run() {
						try {
							addTeamToArena(team1, team2, arena);
							addTeamToArena(team2, team1, arena);
							startArenaMatch(arena, team1, team2);
						} catch (CivException e) {
							CivMessage.sendTeam(team1, "An error has occured and your team has been kicked from the arena queue.");
							CivMessage.sendTeam(team2, "An error has occured and your team has been kicked from the arena queue.");

							CivMessage.sendTeam(team1, "Error:"+e.getMessage());
							CivMessage.sendTeam(team2, "Error:"+e.getMessage());

							e.printStackTrace();
						}
						
					}
				}
				
				TaskMaster.syncTask(new SyncTask(activeArena, team1,team2), TimeTools.toTicks(10));
			} catch (CivException e) {
				e.printStackTrace();
				return;
			}

		}

		/*
		 * Iterate through all of the teams still waiting in the queue and notify 
		 * them of their position in line. 
		 */
		int i = 0;
		for (ArenaTeam team : teamQueue) {
			if (!enabled) {
				CivMessage.sendTeam(team, "Arenas are disabled via and admin. Please wait for them to be re-enabled.");			
			} else {
				if (i < 2) {
					CivMessage.sendTeam(team, "Waiting to join arena. We are next! All arena instances are busy.");			
				} else {
					CivMessage.sendTeam(team, "Waiting to join arena. There are "+i+" teams ahead of us in line.");
				}
			}
			i++;
		}
		
	}
	
	public static void startArenaMatch(Arena activeArena, ArenaTeam team1, ArenaTeam team2) {

		/* Set up objectives.. */
		Objective points1 = activeArena.getScoreboard(team1.getName()).registerNewObjective("teampoints1", "dummy");
		Objective points2 = activeArena.getScoreboard(team2.getName()).registerNewObjective("teampoints2", "dummy");

	//	Objective names1 = activeArena.getScoreboard(team1.getName()).registerNewObjective("team1", "dummy");
	//	Objective names2 = activeArena.getScoreboard(team2.getName()).registerNewObjective("team2", "dummy");

		points1.setDisplaySlot(DisplaySlot.SIDEBAR);
		points1.setDisplayName("Team Hitpoints");
		points2.setDisplaySlot(DisplaySlot.SIDEBAR);
		points2.setDisplayName("Team Hitpoints");
		
		Score score1Team1 = points1.getScore(team1.getTeamScoreboardName());
		Score score1Team2 = points1.getScore(team2.getTeamScoreboardName());
		Score timeout1 = points1.getScore("Time Left");
		try {
			timeout1.setScore(CivSettings.getInteger(CivSettings.arenaConfig, "timeout"));
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (InvalidConfiguration e1) {
			e1.printStackTrace();
		}
		
		score1Team1.setScore(activeArena.config.teams.get(0).controlPoints.size()*activeArena.config.control_block_hp);
		score1Team2.setScore(activeArena.config.teams.get(1).controlPoints.size()*activeArena.config.control_block_hp);
		
		Score score2Team1 = points2.getScore(team1.getTeamScoreboardName());
		Score score2Team2 = points2.getScore(team2.getTeamScoreboardName());
		Score timeout2 = points1.getScore("Time Left");
		try {
			timeout2.setScore(CivSettings.getInteger(CivSettings.arenaConfig, "timeout"));
		} catch (IllegalStateException e1) {
			e1.printStackTrace();
		} catch (InvalidConfiguration e1) {
			e1.printStackTrace();
		}
		
		score2Team1.setScore(activeArena.config.teams.get(0).controlPoints.size()*activeArena.config.control_block_hp);
		score2Team2.setScore(activeArena.config.teams.get(1).controlPoints.size()*activeArena.config.control_block_hp);
		
	//	names1.setDisplaySlot(DisplaySlot.BELOW_NAME);
	//	names1.setDisplayName(team1.getTeamColor()+team1.getName());
		
	//	names2.setDisplaySlot(DisplaySlot.BELOW_NAME);
	//	names2.setDisplayName(team2.getTeamColor()+team2.getName());
		
		activeArena.objectives.put(team1.getName()+";score", points1);
		activeArena.objectives.put(team2.getName()+";score", points2);
		
		/* Save and clear inventories */
		for (Resident resident : team1.teamMembers) {
			resident.saveInventory();
			resident.clearInventory();
			resident.setInsideArena(true);
			resident.save();
			
			try {
				Player player = CivGlobal.getPlayer(resident);
				player.setScoreboard(activeArena.getScoreboard(resident.getTeam().getName()));
			} catch (CivException e) {
				//Player offline.
			}
		}
		
		for (Resident resident : team2.teamMembers) {
			resident.saveInventory();
			resident.clearInventory();
			resident.setInsideArena(true);
			resident.save();
			
			try {
				Player player = CivGlobal.getPlayer(resident);
				player.setScoreboard(activeArena.getScoreboard(resident.getTeam().getName()));
			} catch (CivException e) {
				//Player offline.
			}
		}
		
		CivMessage.sendArena(activeArena, "Arena Match Started!");
	}
	
	public static void addTeamToQueue(ArenaTeam team) throws CivException {
		if (teamQueue.contains(team)) {
			throw new CivException("Your team is already in the queue.");
		}
		
		for (Resident resident : team.teamMembers) {
			try {
				CivGlobal.getPlayer(resident);
			} catch (CivException e) {
				continue;
			}

			
			if (!resident.isUsesAntiCheat()) {
				throw new CivException("Cannot join arena: "+resident.getName()+" is not validated by CivCraft's anti-cheat.");
			}
		}
		
		CivMessage.sendTeam(team, "Added our team to the queue...");
		if (teamQueue.size() > 2) {
			CivMessage.sendTeam(team, "There are "+teamQueue.size()+" teams ahead of us in line.");
		}
		teamQueue.add(team);
	}
	
	public static void addTeamToArena(ArenaTeam team, ArenaTeam otherTeam, Arena arena) throws CivException {	
		arena.addTeam(team);
		team.setCurrentArena(arena);
		
		CivMessage.sendTeamHeading(team, "Arena Match");
		CivMessage.sendTeam(team, "Arena: "+CivColor.Yellow+CivColor.BOLD+arena.config.name);
		CivMessage.sendTeam(team, CivColor.LightGreen+CivColor.BOLD+""+team.getName()+CivColor.RESET+" VS "+CivColor.Rose+CivColor.BOLD+otherTeam.getName());
		CivMessage.sendTeam(team, "Our Score: "+CivColor.LightGreen+team.getLadderPoints()+" "+getFavoredString(team, otherTeam));
		CivMessage.sendTeam(team, "Their Score: "+CivColor.LightGreen+otherTeam.getLadderPoints()+" "+getFavoredString(otherTeam, team));
		CivMessage.sendTeam(team, "Their team members: "+otherTeam.getMemberListSaveString());
	}
	
	public static Arena createArena(ConfigArena arena) throws CivException {
		
		/* Copy world from source to prepare it. */
		File srcFolder = new File("arenas/"+arena.world_source);
		
		if (!srcFolder.exists()) {
			throw new CivException("No world source found at:"+arena.world_source);
		}
				
		Arena activeArena = new Arena(arena);
		String instanceWorldName = activeArena.getInstanceName();
				
		File destFolder = new File(instanceWorldName);
		
		try {
			FileUtils.deleteDirectory(destFolder);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		try {
			copyFolder(srcFolder, destFolder);
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
		
		World world = createArenaWorld(arena, instanceWorldName);
		createArenaControlPoints(arena, world, activeArena);
		
		activeArenas.put(instanceWorldName, activeArena);
		return activeArena;
	}
	
	public static void destroyArena(String instanceName) throws CivException {
		Arena arena = activeArenas.get(instanceName);
		if (arena == null) {
			throw new CivException("No arena with instance name:"+instanceName);
		}
		
		LinkedList<BlockCoord> removeUs = new LinkedList<BlockCoord>();
		for (BlockCoord bcoord : arenaControlBlocks.keySet()) {
			if (bcoord.getWorldname().equals(instanceName)) {
				removeUs.add(bcoord);
			}
		}
		for (BlockCoord bcoord : removeUs) {
			arenaControlBlocks.remove(bcoord);
		}
		removeUs.clear();
		
		for (BlockCoord bcoord : respawnSigns.keySet()) {
			if (bcoord.getWorldname().equals(instanceName)) {
				removeUs.add(bcoord);
			}
		}
		for (BlockCoord bcoord : removeUs) {
			respawnSigns.remove(bcoord);
		}
		removeUs.clear();
		
		for (BlockCoord bcoord : chests.keySet()) {
			if (bcoord.getWorldname().equals(instanceName)) {
				removeUs.add(bcoord);
			}
		}
		for (BlockCoord bcoord : removeUs) {
			chests.remove(bcoord);
		}
		
		arena.returnPlayers();
		arena.clearTeams();

		activeArenas.remove(instanceName);
		Bukkit.getServer().unloadWorld(instanceName, false);
		
		try {
			FileUtils.deleteDirectory(new File(instanceName));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private static void createArenaControlPoints(ConfigArena arena, World world, Arena activeArena) throws CivException {
		
		// Create control points instead of full on structures. No block will be movable so we dont need
		// to store/create a billionty structure blocks. Only need to create ArenaControlPoint objects and determine
		// if they can be broken.
		
		for (ConfigArenaTeam team : arena.teams) {
			for (BlockCoord c : team.controlPoints) 
			{
				BlockCoord bcoord = new BlockCoord(world.getName(), c.getX(), c.getY(), c.getZ());
				ArenaControlBlock acb = new ArenaControlBlock(bcoord, team.number, arena.control_block_hp, activeArena);
				arenaControlBlocks.put(bcoord, acb);
			}
			
			/* Create teleport signs. */
			BlockCoord coord = team.respawnSign;
			Location loc = coord.getCenteredLocation();
			loc.setWorld(world);
			
			if (loc.getBlock().getType().equals(Material.SIGN_POST) ||
			    loc.getBlock().getType().equals(Material.WALL_SIGN)) {
				Sign sign = (Sign)loc.getBlock().getState();
				sign.setLine(0, "");
				sign.setLine(1, "Respawn");
				sign.setLine(2, "At Arena");
				sign.setLine(3, "");

				sign.update();
				respawnSigns.put(new BlockCoord(loc), activeArena);
			} else {
				CivLog.error("Couldn't find sign for respawn sign for arena:"+arena.name);
			}

			for (BlockCoord c : team.chests) {
				BlockCoord bcoord = new BlockCoord(world.getName(), c.getX(), c.getY(), c.getZ());
				chests.put(bcoord, activeArena);
				
				ItemManager.setTypeId(bcoord.getBlock(), ItemManager.getId(Material.ENDER_CHEST));
			}
			
		}
		
	}
	
	private static World createArenaWorld(ConfigArena arena, String name) {
		World world;
		world = Bukkit.getServer().getWorld(name);
		if (world == null) {
			WorldCreator wc = new WorldCreator(name);
			wc.environment(Environment.NORMAL);
			wc.type(WorldType.FLAT);
			wc.generateStructures(false);
			
			world = Bukkit.getServer().createWorld(wc);
			world.setAutoSave(false);
			world.setSpawnFlags(false, false);
			world.setKeepSpawnInMemory(false);
			ChunkCoord.addWorld(world);
		}
		
		return world;
	}
		
	
    private static void copyFolder(File src, File dest) throws IOException {
    	if(src.isDirectory()){
 
    		//if directory not exists, create it
    		if(!dest.exists()){
    		   dest.mkdir();
    		   System.out.println("Directory copied from " 
                              + src + "  to " + dest);
    		}
 
    		//list all the directory contents
    		String files[] = src.list();
 
    		for (String file : files) {
    		   //construct the src and dest file structure
    		   File srcFile = new File(src, file);
    		   File destFile = new File(dest, file);
    		   //recursive copy
    		   copyFolder(srcFile,destFile);
    		}
 
    	}else{
    		//if file, then copy it
    		//Use bytes stream to support all file types
    		InputStream in = new FileInputStream(src);
    	        OutputStream out = new FileOutputStream(dest); 
 
    	        byte[] buffer = new byte[1024];
 
    	        int length;
    	        //copy the file content in bytes 
    	        while ((length = in.read(buffer)) > 0){
    	    	   out.write(buffer, 0, length);
    	        }
 
    	        in.close();
    	        out.close();
    	        System.out.println("File copied from " + src + " to " + dest);
    	}
    }

    public static String getFavoredString(ArenaTeam target, ArenaTeam other) {
    	if (target.getLadderPoints() < other.getLadderPoints()) {
    		return "";
    	}
    	
		try {
	    	int slightly_favored_points = CivSettings.getInteger(CivSettings.arenaConfig, "slightly_favored_points");
			int favored_points = CivSettings.getInteger(CivSettings.arenaConfig, "favored_points");
			
			int diff = target.getLadderPoints() - other.getLadderPoints();
			if (diff > favored_points) {
				return CivColor.Rose+"Favored";
			} else if (diff > slightly_favored_points) {
				return CivColor.Yellow+"Slightly Favored";
			} 
			
			return "";
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
		
		return "";
    }
    
    
	public static void declareVictor(Arena arena, ArenaTeam loser, ArenaTeam winner) {
		
		
		class SyncTask implements Runnable {
			Arena arena;
			ArenaTeam loser;
			ArenaTeam winner;
			
			public SyncTask (Arena arena, ArenaTeam loser, ArenaTeam winner) {
				this.arena = arena;
				this.loser = loser;
				this.winner = winner;
			}

			@Override
			public void run() {
				try {
					try {
						int base_points = CivSettings.getInteger(CivSettings.arenaConfig, "base_ladder_points");
						int slightly_favored_points = CivSettings.getInteger(CivSettings.arenaConfig, "slightly_favored_points");
						int favored_points = CivSettings.getInteger(CivSettings.arenaConfig, "favored_points");
						double slightly_favored_modifier = CivSettings.getDouble(CivSettings.arenaConfig, "slightly_favored_modifier");
						double favored_modifier = CivSettings.getDouble(CivSettings.arenaConfig, "favored_modifier");

						/* Calculate points. */
						int winnerDifference = winner.getLadderPoints() - loser.getLadderPoints();
						int points = base_points;
						
						if (winnerDifference > favored_points) {
							/* Winner was favored. */
							points = (int) (base_points * favored_modifier);
						} else if (winnerDifference > slightly_favored_points) {
							/* Winner was slightly favored. */
							points = (int) (base_points * slightly_favored_modifier);
						} else if (winnerDifference > 0) {
							/* Winner and loser were evenly matched. */
							points = base_points;
						} else if (winnerDifference < -favored_points) {
							/* Loser was favored. */
							points = base_points + (int) (base_points * (1 - favored_modifier));
						} else if (winnerDifference < -slightly_favored_points) {
							/* Loser was slightly favored. */
							points = base_points + (int) (base_points * (1 - slightly_favored_modifier));
						}
						
						winner.setLadderPoints(winner.getLadderPoints() + points);
						loser.setLadderPoints(loser.getLadderPoints() - points);
						
						winner.save();
						loser.save();
						
						CivMessage.global(CivColor.LightGreen+CivColor.BOLD+winner.getName()+"(+"+points+")"+CivColor.RESET+" defeated "+
								CivColor.Rose+CivColor.BOLD+loser.getName()+"(-"+points+")"+CivColor.RESET+" in Arena!");
						
					} catch (InvalidConfiguration e) {
						e.printStackTrace();
					}
					
					ArenaManager.destroyArena(arena.getInstanceName());
				} catch (CivException e) {
					e.printStackTrace();
				}
			}
			
		}
		
		CivMessage.sendArena(arena, CivColor.LightGreen+CivColor.BOLD+winner.getName()+
									CivColor.RESET+" has defeated "+
									CivColor.Rose+CivColor.BOLD+loser.getName());
		CivMessage.sendArena(arena, "Leaving arena in 10 seconds...");
		TaskMaster.syncTask(new SyncTask(arena, loser, winner), TimeTools.toTicks(10));
	}
	
	public static void declareDraw(Arena arena) {
		
		
		class SyncTask implements Runnable {
			Arena arena;
			
			public SyncTask (Arena arena) {
				this.arena = arena;
			}

			@Override
			public void run() {
				try {
					ArenaManager.destroyArena(arena.getInstanceName());
				} catch (CivException e) {
					e.printStackTrace();
				}
			}
		}
		
		CivMessage.sendArena(arena, "Leaving arena in 10 seconds...");
		TaskMaster.syncTask(new SyncTask(arena), TimeTools.toTicks(10));
	}


}
