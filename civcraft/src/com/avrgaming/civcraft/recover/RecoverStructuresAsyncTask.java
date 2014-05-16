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
package com.avrgaming.civcraft.recover;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map.Entry;

import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.template.Template;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.ItemManager;
import com.avrgaming.civcraft.util.SimpleBlock.Type;

public class RecoverStructuresAsyncTask implements Runnable {

	
	public static HashSet<Integer> ignoreBlocks = new HashSet<Integer>();
	boolean listOnly = false;
	CommandSender sender;
	
	public RecoverStructuresAsyncTask(CommandSender sender, boolean listonly) {
		this.sender = sender;
		this.listOnly = listonly;
	}
	
	public boolean isBrokenStructure(Structure struct) {
		
		Template tpl;
		try {
			//tpl.load_template(struct.getSavedTemplatePath());
			tpl = Template.getTemplate(struct.getSavedTemplatePath(), null);
		} catch (IOException | CivException e) {
			e.printStackTrace();
			return false;
		}
		
		
		Block cornerBlock = struct.getCorner().getBlock();
		for (int x = 0; x < tpl.size_x; x++) {
			for (int y = 0; y < tpl.size_y; y++) {
				for (int z = 0; z < tpl.size_z; z++) {
					Block nextBlock = cornerBlock.getRelative(x, y, z);
					
					if (ignoreBlocks.contains(ItemManager.getId(nextBlock))) {
						continue;
					}
					
					if (ignoreBlocks.contains(tpl.blocks[x][y][z].getType())) {
						continue;
					}
					
					if (tpl.blocks[x][y][z].specialType != Type.NORMAL) {
						continue;
					}
					
					if (tpl.blocks[x][y][z].isAir()) {
						continue;
					}
					
					if (ItemManager.getId(nextBlock) != tpl.blocks[x][y][z].getType()) { // ||
					//	nextBlock.getData() != tpl.blocks[x][y][z].getData()) {
					//	CivLog.debug("\tBLOCK:"+nextBlock.getTypeId() + " is not "+tpl.blocks[x][y][z].getType());
						return true;
					}
				}
			}
		}
		
		return false;
		
	}
	
	
	
	@Override
	public void run() {
		
		// A bit of a hack, but some of the command blocks add things to structures
		// that are not on the same location as the sign (e.g. the control block makes
		// a little tower.. so if we ignore these block IDs when searching we wont flag
		// these structures accidentally.
		// XXX shouldn't broken structures be mostly air anyway?
		
		ignoreBlocks.add(CivData.OBSIDIAN);
		ignoreBlocks.add(CivData.FENCE);
		ignoreBlocks.add(CivData.LADDER);
		
		ArrayList<Structure> repairStructures = new ArrayList<Structure>();
		
		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
		while (iter.hasNext()) {
			
			Structure struct = iter.next().getValue();
			if (struct.getSavedTemplatePath() == null) {
				continue;
			}
			
			if (!struct.canRestoreFromTemplate()) {
				continue;
			}
			
			try {
				if (isBrokenStructure(struct)) {
					if (listOnly) {
						CivMessage.send(sender, struct.getDisplayName()+" at "+CivColor.Yellow+struct.getCorner());
					}
					
					//CivLog.debug("\tIS BROKEN");
					if (!listOnly) {
						repairStructures.add(struct);
					}
				}
			} catch (Exception e) {
				e.printStackTrace();
			}
			
		}
		TaskMaster.syncTask(new RecoverStructureSyncTask(sender, repairStructures));		
	}

}
