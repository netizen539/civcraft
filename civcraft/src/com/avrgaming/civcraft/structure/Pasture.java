package com.avrgaming.civcraft.structure;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Map;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import org.bukkit.Location;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.sessiondb.SessionEntry;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.threading.tasks.LoadPastureEntityTask;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.util.ChunkCoord;

public class Pasture extends Structure {

	/* Global pasture chunks */
	public static Map<ChunkCoord, Pasture> pastureChunks = new ConcurrentHashMap<ChunkCoord, Pasture>();
	public static Map<UUID, Pasture> pastureEntities = new ConcurrentHashMap<UUID, Pasture>();
	
	/* Chunks bound to this pasture. */
	public HashSet<ChunkCoord> chunks = new HashSet<ChunkCoord>();
	public HashSet<UUID> entities = new HashSet<UUID>();
	public ReentrantLock lock = new ReentrantLock(); 
	
	private int pendingBreeds = 0;
	
	protected Pasture(Location center, String id, Town town)
			throws CivException {
		super(center, id, town);
	}

	public Pasture(ResultSet rs) throws SQLException, CivException {
		super(rs);
	}

	public int getMobCount() {
		return entities.size();
	}

	public int getMobMax() {
		int max;
		try {
			max = CivSettings.getInteger(CivSettings.structureConfig, "pasture.max_mobs");
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
			return 0;
		}
		return max;
	}

	public boolean processMobBreed(Player player, EntityType type) {
				
		if (!this.isActive()) {
			CivMessage.sendError(player, "Pasture is destroyed or currently building. Cannot breed yet.");
			return false;
		}
		
		if (this.getMobCount() >= this.getMobMax()) {
			CivMessage.sendError(player, "Pasture is the maximum number of mobs that it can support. Slaughter some before you breed.");
			return false;
		}
		
		if ((getPendingBreeds() + this.getMobCount()) >= this.getMobMax()) {
			CivMessage.sendError(player, "Pasture has too many breed events pending. Pasture is probably at the maximum number of mobs it can support. Slaughter some before you breed.");
			return false;
		}
		
		return true;
	}
	
	public void bindPastureChunks() {
		for (BlockCoord bcoord : this.structureBlocks.keySet()) {
			ChunkCoord coord = new ChunkCoord(bcoord);
			this.chunks.add(coord);
			pastureChunks.put(coord, this);
		}
	}
	
	public void unbindPastureChunks() {
		for (ChunkCoord coord : this.chunks) {
			pastureChunks.remove(coord);
		}
		
		this.entities.clear();
		this.chunks.clear();
		
		LinkedList<UUID> removeUs = new LinkedList<UUID>();
		for (UUID id : pastureEntities.keySet()) {
			Pasture pasture = pastureEntities.get(id);
			if (pasture == this) {
				removeUs.add(id);
			}
		}
		
		for (UUID id : removeUs) {
			pastureEntities.remove(id);
		}
		
	}
	
	@Override
	public void onComplete() {
		bindPastureChunks();
	}
	
	@Override
	public void onLoad() throws CivException {
		bindPastureChunks();
		loadEntities();
	}
	
	@Override
	public void delete() throws SQLException {
		super.delete();
		unbindPastureChunks();
		clearEntities();
	}
	
	public void clearEntities() {
		// TODO Clear entities bound to us?
	}

	public void onBreed(LivingEntity entity) {
		saveEntity(entity.getWorld().getName(), entity.getUniqueId());
		setPendingBreeds(getPendingBreeds() - 1);
	}
	
	public String getEntityKey() {
		return "pasture:"+this.getId();
	}
	
	public String getValue(String worldName, UUID id) {
		return worldName+":"+id;
	}
	
	public void saveEntity(String worldName, UUID id) {
		class AsyncTask implements Runnable {
			Pasture pasture;
			UUID id;
			String worldName;
			
			public AsyncTask(Pasture pasture, UUID id, String worldName) {
				this.pasture = pasture;
				this.id = id;
				this.worldName = worldName;
			}
			
			@Override
			public void run() {
				pasture.sessionAdd(getEntityKey(), getValue(worldName, id));
				lock.lock();
				try {
					entities.add(id);
					pastureEntities.put(id, pasture);
				} finally {
					lock.unlock();
				}
			}
		}
		
		TaskMaster.asyncTask(new AsyncTask(this, id, worldName), 0);
	}
	
	public void loadEntities() {
		Queue<SessionEntry> entriesToLoad = new LinkedList<SessionEntry>();
		ArrayList<SessionEntry> entries = CivGlobal.getSessionDB().lookup(getEntityKey());
		entriesToLoad.addAll(entries);
		TaskMaster.syncTask(new LoadPastureEntityTask(entriesToLoad, this));
	}
	
	public void onEntityDeath(LivingEntity entity) {
		class AsyncTask implements Runnable {
			LivingEntity entity;
			
			public AsyncTask(LivingEntity entity) {
				this.entity = entity;
			}
			
			
			@Override
			public void run() {
				lock.lock();
				try {
					entities.remove(entity.getUniqueId());
					pastureEntities.remove(entity.getUniqueId());
				} finally {
					lock.unlock();
				}
			}
			
		}
		
		TaskMaster.asyncTask(new AsyncTask(entity), 0);
	}

	public int getPendingBreeds() {
		return pendingBreeds;
	}

	public void setPendingBreeds(int pendingBreeds) {
		this.pendingBreeds = pendingBreeds;
	}
	
}
