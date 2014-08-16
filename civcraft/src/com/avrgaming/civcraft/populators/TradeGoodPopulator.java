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
package com.avrgaming.civcraft.populators;

import java.sql.SQLException;
import java.util.Random;

import org.bukkit.Chunk;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.generator.BlockPopulator;

import com.avrgaming.civcraft.config.ConfigTradeGood;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.ProtectedBlock;
import com.avrgaming.civcraft.object.StructureSign;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;
import com.avrgaming.civcraft.util.ItemManager;

public class TradeGoodPopulator extends BlockPopulator {
	
	//private static final int RESOURCE_CHANCE = 400; 
    private static final int FLAG_HEIGHT = 3;
//    private static final double MIN_DISTANCE = 400.0;
    

    public static void buildTradeGoodie(ConfigTradeGood good, BlockCoord coord, World world, boolean sync) {
    	TradeGood new_good = new TradeGood(good, coord);            
    	CivGlobal.addTradeGood(new_good);

    	BlockFace direction = null;
    	Block top = null;
    	Random random = new Random();
    	int dir = random.nextInt(4);
    	if (dir == 0) {
    		direction = BlockFace.NORTH;
    	} else if (dir == 1) {
    		direction = BlockFace.EAST;
    	} else if (dir == 2) {
    		direction = BlockFace.SOUTH;
    	} else {
    		direction = BlockFace.WEST;
    	}

    	//clear any stack goodies
    	for (int y = coord.getY(); y < 256; y++) {
    		top = world.getBlockAt(coord.getX(), y, coord.getZ());
    		if (ItemManager.getId(top) == CivData.BEDROCK) {
    			ItemManager.setTypeId(top, CivData.AIR);
    		}
    	}
    	
    	for (int y = coord.getY(); y < coord.getY() + FLAG_HEIGHT; y++) {
    		top = world.getBlockAt(coord.getX(), y, coord.getZ());
    		top.setType(Material.BEDROCK);

    		ProtectedBlock pb = new ProtectedBlock(new BlockCoord(top), ProtectedBlock.Type.TRADE_MARKER);
    		CivGlobal.addProtectedBlock(pb);
    		if (sync) {
    		try {
				pb.saveNow();
			} catch (SQLException e) {
				e.printStackTrace();
			}    
    		} else {
    			pb.save();
    		}
    	}

    	Block signBlock = top.getRelative(direction);
    	signBlock.setType(Material.WALL_SIGN);
    	//TODO make sign a structure sign?
    			//          Civ.protectedBlockTable.put(Civ.locationHash(signBlock.getLocation()), 
    	//          		new ProtectedBlock(signBlock, null, null, null, ProtectedBlock.Type.TRADE_MARKER));

    	BlockState state = signBlock.getState();

    	if (state instanceof Sign) {
    		Sign sign = (Sign)state;
    		org.bukkit.material.Sign data = (org.bukkit.material.Sign)state.getData();

    		data.setFacingDirection(direction);
    		sign.setLine(0, "Trade Resource");
    		sign.setLine(1, "----");
    		sign.setLine(2, good.name);
    		sign.setLine(3, "");
    		sign.update(true);

    		StructureSign structSign = new StructureSign(new BlockCoord(signBlock), null);
    		structSign.setAction("");
    		structSign.setType("");
    		structSign.setText(sign.getLines());
    		structSign.setDirection(ItemManager.getData(sign.getData()));
    		CivGlobal.addStructureSign(structSign);
    	}
    	if (sync) {
    	try {
			new_good.saveNow();
		} catch (SQLException e) {
			e.printStackTrace();
		}
    	} else {
    		new_good.save();
    	}
    }

    public boolean checkForDuplicateTradeGood(String worldName, int centerX, int centerY, int centerZ) {
    	/* 
    	 * Search downward to bedrock for any trade goodies here. If we find one, don't generate. 
    	 */
    	
    	BlockCoord coord = new BlockCoord(worldName, centerX, centerY, centerZ);
    	for (int y = centerY; y > 0; y--) {
    		coord.setY(y);    		
    		
    		if (CivGlobal.getTradeGood(coord) != null) {
				/* Already a trade goodie here. DONT Generate it. */
				return true;
    		}		
    	}
    	return false;
    }
    
    @Override
	public void populate(World world, Random random, Chunk source) {
    	
    	ChunkCoord cCoord = new ChunkCoord(source);
    	TradeGoodPick pick = CivGlobal.preGenerator.goodPicks.get(cCoord);
    	if (pick != null) {
			int centerX = (source.getX() << 4) + 8;
			int centerZ = (source.getZ() << 4) + 8;
			int centerY = world.getHighestBlockYAt(centerX, centerZ);
			BlockCoord coord = new BlockCoord(world.getName(), centerX, centerY, centerZ);

			if (checkForDuplicateTradeGood(world.getName(), centerX, centerY, centerZ)) {
				return;
			}
			
			// Determine if we should be a water good.
			ConfigTradeGood good;
			if (ItemManager.getBlockTypeIdAt(world, centerX, centerY-1, centerZ) == CivData.WATER || 
				ItemManager.getBlockTypeIdAt(world, centerX, centerY-1, centerZ) == CivData.WATER_RUNNING) {
				good = pick.waterPick;
			}  else {
				good = pick.landPick;
			}
			
			// Randomly choose a land or water good.
			if (good == null) {
				System.out.println("Could not find suitable good type during populate! aborting.");
				return;
			}
			
			// Create a copy and save it in the global hash table.
			buildTradeGoodie(good, coord, world, false);
    	}
 	
    }

}
