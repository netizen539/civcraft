package com.avrgaming.civcraft.arena;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

import org.bukkit.Bukkit;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.WorldType;
import org.bukkit.World.Environment;

import com.avrgaming.civcraft.config.ConfigArena;
import com.avrgaming.civcraft.config.ConfigArenaTeam;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;

public class ArenaManager {

	public static HashMap<BlockCoord, ArenaControlBlock> arenaControlBlocks = new HashMap<BlockCoord, ArenaControlBlock>();
	public static HashMap<String, Arena> activeArenas = new HashMap<String, Arena>();
	
	public static void createArena(ConfigArena arena) throws CivException {
		
		/* Copy world from source to prepare it. */
		File srcFolder = new File(arena.world_source);
		
		if (!srcFolder.exists()) {
			throw new CivException("No world source found at:"+arena.world_source);
		}
		
		Arena activeArena = new Arena(arena);
		String instanceWorldName = arena.world_source+"_"+"instance_"+activeArena.instanceID;
		
		File destFolder = new File(instanceWorldName);		
		try {
			copyFolder(srcFolder, destFolder);
		} catch (IOException e) {
			e.printStackTrace();
			return;
		}
		
		World world = createArenaWorld(arena, instanceWorldName);
		createArenaControlPoints(arena, world);
		
		activeArenas.put(instanceWorldName, activeArena);
		
	}
	
	private static void createArenaControlPoints(ConfigArena arena, World world) throws CivException {
		
		// XXX Create control points instead of full on structures. No block will be movable so we dont need
		// to store/create a billionty structure blocks. Only need to create ArenaControlPoint objects and determine
		// if they can be broken.
		
		for (ConfigArenaTeam team : arena.teams) {
			for (BlockCoord c : team.controlPoints) {
				ArenaControlBlock acb = new ArenaControlBlock(c);
				arenaControlBlocks.put(c, acb);
			}
		}
		
	}
	
	private static World createArenaWorld(ConfigArena arena, String name) {
		WorldCreator wc = new WorldCreator(name);
		wc.environment(Environment.NORMAL);
		wc.type(WorldType.FLAT);
		wc.generateStructures(false);
		
		World world = Bukkit.getServer().createWorld(wc);
		world.setSpawnFlags(false, false);
		ChunkCoord.addWorld(world);
		
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
}
