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
package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Random;

import org.bukkit.Color;
import org.bukkit.FireworkEffect;
import org.bukkit.Location;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.Sign;
import org.bukkit.entity.Entity;
import org.bukkit.entity.ItemFrame;
import org.bukkit.inventory.ItemStack;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidNameException;
import com.avrgaming.civcraft.items.BonusGoodie;
import com.avrgaming.civcraft.main.CivData;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.StructureBlock;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TradeGood;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.FireworkEffectPlayer;
import com.avrgaming.civcraft.util.ItemFrameStorage;
import com.avrgaming.civcraft.util.ItemManager;

public class TradeOutpost extends Structure {

	protected BlockCoord tradeGoodCoord;
	protected BlockCoord tradeOutpostTower = null;	
	protected ItemFrameStorage frameStore = null;
	protected TradeGood good = null;
	protected BonusGoodie goodie = null;
	
	
	protected TradeOutpost(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
		loadSettings();
	}

	public TradeOutpost(ResultSet rs) throws SQLException, CivException {
		super(rs);
		loadSettings();
	}

	public void loadSettings() {
	}
	
	public void checkForTradeGood(BlockCoord coord) {
		
	}

	public BlockCoord getTradeGoodCoord() {
		return tradeGoodCoord;
	}

	public void setTradeGoodCoord(BlockCoord tradeGoodCoord) {
		this.tradeGoodCoord = tradeGoodCoord;
	}

	@Override
	public String getDynmapDescription() {
		return null;
	}
	
	@Override
	public String getMarkerIconName() {
		return "scales";
	}
	
	@Override
	public void onDemolish() throws CivException {
		
		/* 
		 * If the trade goodie is not in our frame, we should not allow
		 * the trade outpost to be demolished. As it may result in an inconsistent state.
		 */
		if (this.frameStore == null) {
			return;
		}
		
		ItemStack frameItem = this.frameStore.getItem();
		if (frameItem != null) {
			BonusGoodie goodie = CivGlobal.getBonusGoodie(frameItem);
			if (goodie != null) {
				if (goodie.getOutpost() == this) {
					return;
				}
			}
		}
		
		throw new CivException("Cannot demolish when bonus goodie is not in item frame.");
	}
	
	public void build_trade_outpost(Location centerLoc) throws CivException {
		
		/* Add trade good to town. */
		TradeGood good = CivGlobal.getTradeGood(tradeGoodCoord);
		if (good == null) {
			throw new CivException("Couldn't find trade good at location:"+good);
		}
		
		if (good.getInfo().water) {
			throw new CivException("Trade Outposts cannot be built on water goods.");
		}
		
		if (good.getTown() != null) {
			throw new CivException("Good is already claimed.");
		}
		
		good.setStruct(this);
		good.setTown(this.getTown());
		good.setCiv(this.getTown().getCiv());
		
		/* Save the good *afterwards* so the structure id is properly set. */
		this.setGood(good);
	}

	public void build_trade_outpost_tower() throws CivException {
		/* Add trade good to town. */
		
		/* this.good is set by the good's load function or by the onBuild function. */
		TradeGood good = this.good;
		if (good == null) {
			throw new CivException("Couldn't find trade good at location:"+good);
		}
		
		/* Build the 'trade good tower' */
		/* This is always set on post build using the post build sync task. */
		if (tradeOutpostTower == null) {
			throw new CivException("Couldn't find trade outpost tower.");
		}
		
		Location centerLoc = tradeOutpostTower.getLocation();
		
		/* Build the bedrock tower. */
		for (int i = 0; i < 3; i++) {
			Block b = centerLoc.getBlock().getRelative(0, i, 0);
			ItemManager.setTypeId(b, CivData.BEDROCK); ItemManager.setData(b, 0);
			
			StructureBlock sb = new StructureBlock(new BlockCoord(b), this);
			this.addStructureBlock(sb.getCoord(), false);
			//CivGlobal.addStructureBlock(sb.getCoord(), this);
		}
		
		/* Place the sign. */
		Block b = centerLoc.getBlock().getRelative(1, 2, 0);
		ItemManager.setTypeId(b, CivData.WALL_SIGN); 
		ItemManager.setData(b, CivData.DATA_SIGN_EAST);
		Sign s = (Sign)b.getState();
		s.setLine(0, good.getInfo().name);
		s.update();
		StructureBlock sb = new StructureBlock(new BlockCoord(b), this);
		//CivGlobal.addStructureBlock(sb.getCoord(), this);
		this.addStructureBlock(sb.getCoord(), false);
		
		/* Place the itemframe. */
		b = centerLoc.getBlock().getRelative(0,1,0);
		this.addStructureBlock(new BlockCoord(b), false);
		Block b2 = b.getRelative(1, 0, 0);
		Entity entity = CivGlobal.getEntityAtLocation(b2.getLocation());
		this.addStructureBlock(new BlockCoord(b2), false);
		
		if (entity == null || (!(entity instanceof ItemFrame))) {
			this.frameStore = new ItemFrameStorage(b.getLocation(), BlockFace.EAST);	
		} else {
			this.frameStore = new ItemFrameStorage((ItemFrame)entity, b.getLocation());
		}
		
		this.frameStore.setBuildable(this);
	}
	
    public ItemFrameStorage getItemFrameStore() {
		return this.frameStore;
	}
	
	public BlockCoord getTradeOutpostTower() {
		return tradeOutpostTower;
	}

	public void setTradeOutpostTower(BlockCoord tradeOutpostTower) {
		this.tradeOutpostTower = tradeOutpostTower;
	}

	public TradeGood getGood() {
		return good;
	}

	public void setGood(TradeGood good) {
		this.good = good;
	}

	public BonusGoodie getGoodie() {
		return goodie;
	}

	public void setGoodie(BonusGoodie goodie) {
		this.goodie = goodie;
	}
	
	@Override
	public void delete() throws SQLException {
		if (this.goodie != null) {
			this.goodie.delete();
		}
		
		super.delete();
	}
	
	@Override
	public void onDestroy() {
		super.onDestroy();
		
		if (this.goodie != null) {
			try {
				this.goodie.delete();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}
	
	@Override
	public void onComplete() {
		class SyncTask implements Runnable {

			@Override
			public void run() {
				try {
					createTradeGood();
				} catch (CivException e) {
					e.printStackTrace();
				}
			}
		}
		TaskMaster.syncTask(new SyncTask(), 20);
	}
	
	public void createTradeGood() throws CivException {
		
		if (!this.isActive()) {
			return;
		}
		
		/* Add custom item. 
		 * This function is called on reload. The constructor either loads 
		 * the good or creates a new one at the trade outpost if no good could
		 * be found.
		 * */
		try {
			this.goodie = new BonusGoodie(this);
			
			if (this.goodie.getFrame() == null) {
				//goodie not in a frame, skip it.
				return;
			}
			
			TownHall townhall = this.goodie.getFrame().getTown().getTownHall();
			if (townhall != null) {
				for (ItemFrameStorage ifs : townhall.getGoodieFrames()) {
					if (ifs.getFrameID() == this.goodie.getFrame().getFrameID()) {
						townhall.getTown().loadGoodiePlaceIntoFrame(townhall, goodie);
					}
				}
			}
		} catch (SQLException e) {
			e.printStackTrace();
			throw new CivException("Internal database error.");
		} catch (InvalidNameException e) {
			e.printStackTrace();
			throw new CivException("Invalid name exception.");
		}
	}
	
	@Override
	public void onLoad() throws CivException {
		createTradeGood();
	}
	
	public void fancyDestroyStructureBlocks() {
		for (BlockCoord coord : this.structureBlocks.keySet()) {
			
			if (CivGlobal.getStructureChest(coord) != null) {
				continue;
			}
			
			if (CivGlobal.getStructureSign(coord) != null) {
				continue;
			}
			
			if (ItemManager.getId(coord.getBlock()) == CivData.BEDROCK || ItemManager.getId(coord.getBlock()) == CivData.AIR) {
				//Be a bit more careful not to destroy any of the item frames..
				continue;
			}
			
			Random rand = new Random();
			
			// Each block has a 10% chance to turn into gravel
			if (rand.nextInt(100) <= 10) {
				ItemManager.setTypeId(coord.getBlock(), CivData.GRAVEL);
				continue;
			}
			
			// Each block has a 50% chance of starting a fire
			if (rand.nextInt(100) <= 50) {
				ItemManager.setTypeId(coord.getBlock(), CivData.FIRE);
				continue;
			}
			
			// Each block has a 1% chance of launching an explosion effect
			if (rand.nextInt(100) <= 1) {
				FireworkEffect effect = FireworkEffect.builder().with(org.bukkit.FireworkEffect.Type.BURST).withColor(Color.ORANGE).withColor(Color.RED).withTrail().withFlicker().build();
				FireworkEffectPlayer fePlayer = new FireworkEffectPlayer();
				for (int i = 0; i < 3; i++) {
					try {
						fePlayer.playFirework(coord.getBlock().getWorld(), coord.getLocation(), effect);
					} catch (Exception e) {
						e.printStackTrace();
					}
				}			
			}
		}
	}
	
}
