package com.avrgaming.civcraft.randomevents.components;

import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.plugin.PluginManager;

import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.randomevents.RandomEventComponent;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;

public class BlockBreak extends RandomEventComponent implements Listener {
	
	BlockCoord bcoord;
	boolean blockBroken = false;
	boolean brokenByTown = false;
	
	@Override
	public void onStart() {
		/* Register a listener to watch for killed mobs. */
		final PluginManager pluginManager = CivCraft.getPlugin().getServer().getPluginManager();
		pluginManager.registerEvents(this, CivCraft.getPlugin());
		
	}
	
	@Override
	public void onCleanup() {
		HandlerList.unregisterAll(this);	
	}
	
	@EventHandler(priority = EventPriority.MONITOR)
	public void onBlockBreak(BlockBreakEvent event) {
		if (blockBroken) {
			return;
		}
		
		String varname = this.getString("varname");
		String locString = this.getParent().componentVars.get(varname);
		if (locString == null) {
			return;
		}
	
		bcoord = new BlockCoord(this.getParent().componentVars.get(varname));
		
		BlockCoord breakCoord = new BlockCoord(event.getBlock());

		if (breakCoord.getX() == bcoord.getX() &&
		    breakCoord.getY() == bcoord.getY() &&
		    breakCoord.getZ() == bcoord.getZ()) {
			
			Resident resident = CivGlobal.getResident(event.getPlayer().getName());
			if (resident.getTown() == this.getParentTown()) {
				brokenByTown = true;
				CivMessage.send(event.getPlayer(), CivColor.LightGreen+"You seem to have found something interesting....");
			}
			
			blockBroken = true;
			
			this.getParent().componentVars.put(getString("playername_var"), event.getPlayer().getName());
		}
	}
	
	@Override
	public void process() {
	}
	
	@Override
	public boolean onCheck() { 
		return blockBroken && brokenByTown;
	}
}
