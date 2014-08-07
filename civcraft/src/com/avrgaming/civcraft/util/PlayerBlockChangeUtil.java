package com.avrgaming.civcraft.util;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.TreeMap;

import net.minecraft.server.v1_7_R4.PacketPlayOutMultiBlockChange;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;

public class PlayerBlockChangeUtil {
	/*
	 * This class sends fake block updates to residents.
	 * It attempts to package all pending block updates. It allows the user to add blocks that
	 * need to change to a list, then calculates which chunks need to be updated and sent to the
	 * player.
	 */
	
	/* A hashmap for each player. */
//	private HashMap<String, HashMap<BlockCoord, SimpleBlock>> blocksToUpdate = new HashMap<String, HashMap<BlockCoord, SimpleBlock>>();
	
	/* 
	 * This hashmap contains the blocks in each chunk to update. The Simple block's x, y, and z value are now chunk offsets.
	 */
	HashMap<String, HashMap<ChunkCoord, LinkedList<SimpleBlock>>> blocksInChunkToUpdate = new HashMap<String, HashMap<ChunkCoord, LinkedList<SimpleBlock>>>();
	
	
	TreeMap<String, PacketPlayOutMultiBlockChange> preparedPackets = new TreeMap<String, PacketPlayOutMultiBlockChange>();
	//private static ReentrantLock taskLock = new ReentrantLock();

	
	
	public void addUpdateBlock(String playerName, BlockCoord bcoord, int type_id, int data) {
//		HashMap<BlockCoord, SimpleBlock> blocks = blocksToUpdate.get(playerName);
//		if (blocks == null) {
//			blocks = new HashMap<BlockCoord, SimpleBlock>();
//		}
//		
//		
//		SimpleBlock sb = new SimpleBlock(type_id, data);
//		blocks.put(bcoord, sb);
//		blocksToUpdate.put(playerName, blocks);
		
		HashMap<ChunkCoord, LinkedList<SimpleBlock>> blocksInChunk = blocksInChunkToUpdate.get(playerName);
		if (blocksInChunk == null) {
			blocksInChunk = new HashMap<ChunkCoord, LinkedList<SimpleBlock>>();
		}
		
		/* Add to chunk table. */
		ChunkCoord coord = new ChunkCoord(bcoord);
		SimpleBlock sb2 = new SimpleBlock(type_id, data);
		sb2.worldname = bcoord.getWorldname();
		sb2.x = bcoord.getX();
		sb2.y = bcoord.getY();
		sb2.z = bcoord.getZ();
		
		LinkedList<SimpleBlock> blocks = blocksInChunk.get(coord);
		if (blocks == null) {
			blocks = new LinkedList<SimpleBlock>();
		}
		
		blocks.add(sb2);
		blocksInChunk.put(coord, blocks);
		blocksInChunkToUpdate.put(playerName, blocksInChunk);
	}
	
	// Each updated block is stored sequentially in 4 byte sized blocks.
	// The content of these bytes are as follows:
	//
	// Byte index:  |       Zero        |       One       |       Two       |      Three        |
	// Bit index:   |  0 - 3  |  4 - 7  |    8   -   15   |   16        -       27   |  28 - 31 |  
	// Content:     |    x    |    z    |        y        |         block id         |   data   | 
	//                                                    |     (8)         |   (4)       (4)
	
//	/* This gets byte 0. */
//	private byte getHexFromXZ(int x, int z) {
//		byte hex = 0;
//		hex += x;
//		hex = (byte) (hex << 4);
//		hex += z;
//		
//		return hex;
//	}
//	
//	/* This gets byte 2 */
//	private byte getBlockIDUpperByte(int blockId) {
//		int blockMasked = blockId >> 4; //Shift off first 4 bits which exist in byte 3. */
//		byte hex = (byte)(blockMasked); //This should slice the hex into what we want...
//		return hex;
//	}
//	
//	/* This gets byte 3 */
//	private byte getLastByte(int blockId, byte data) {
//		byte hex = 0;
//		int firstFour = blockId & 0xF;
//		
//		hex += firstFour;
//		hex = (byte) (hex << 4);
//		
//		int lastFour = data & 0xF;
//		hex += lastFour;
//		return hex;
//	}
	
	public void sendUpdate(String playerName) {
		//int count = 0;
		HashMap<ChunkCoord, LinkedList<SimpleBlock>> blocksInChunk = blocksInChunkToUpdate.get(playerName);
		if (blocksInChunk == null) {
			return;
		}	
		
		for (ChunkCoord chunk : blocksInChunk.keySet()) {
			LinkedList<SimpleBlock> blocks = blocksInChunk.get(chunk);
			
			Player player;
			try {
				player = CivGlobal.getPlayer(playerName);
			} catch (CivException e) {
				e.printStackTrace();
				return;
			}
			
			for (SimpleBlock sb : blocks) {
			//	count++;
				ItemManager.sendBlockChange(player, sb.getLocation(), sb.getType(), sb.getData());
			}
		}		
	}
	
//	@SuppressWarnings("deprecation")
//	public void sendUpdate(String playerName) {
//		
//		Player player;
//		try {
//			player = CivGlobal.getPlayer(playerName);
//		} catch (CivException e) {
//			return; // Player no longer online?
//		}
//		
//		HashMap<ChunkCoord, LinkedList<SimpleBlock>> blocksInChunk = blocksInChunkToUpdate.get(playerName);
//		if (blocksInChunk == null) {
//			return;
//		}		
//		
//		/* Loop through each chunk that is going to update. */
//		for (ChunkCoord coord : blocksInChunk.keySet()) {
//			LinkedList<SimpleBlock> blocks = blocksInChunk.get(coord);
//			if (blocks == null) {
//				continue;
//			}
//			
//			//CraftWorld craftWorld;
//			//CraftBukkit bukkit;
//			//player.getw
//			net.minecraft.server.v1_7_R4.World w = ((CraftWorld) player.getWorld()).getHandle();
//			BlockCoord playerCoord = new BlockCoord(player.getLocation());
//			
//			Double viewDistanceSquared = (double)Math.pow(Bukkit.getServer().getViewDistance()*16, 2);
//			String distanceKey = "";
//			
//			while (blocks.size() > 0) {
//				/* Compiling this into a mulit-block change packet. */
//
//				byte[] dirtyBlocks = new byte[64*4];
//				int i = 0;
//				int count = 0;
//				Iterator<SimpleBlock> iter = blocks.iterator();
//				Double distance = null;
//				
//				while (iter.hasNext()) {
//					SimpleBlock sb = iter.next();
//					/* Calc distance to player using first SB */
//					BlockCoord abs = new BlockCoord();
//					abs.setWorldname(playerCoord.getWorldname());
//					abs.setX((int)Math.floor(coord.getX() * 16) + sb.x);
//					abs.setY(sb.y);
//					abs.setZ((int)Math.floor(coord.getZ() * 16) + sb.z);
//					distance = abs.distanceSquared(playerCoord);
//					distanceKey = distance+":"+abs.getX()+":"+abs.getY()+":"+abs.getZ();
//					
//					if (distance > viewDistanceSquared) {
//						// Cull packet, too far from player.
//						CivLog.debug("distance was too far abs:"+abs+" dist:"+distance+" vs "+viewDistanceSquared);
//						iter.remove();
//						continue;
//					}
//					
//					if (count >= 64) {
//						break;
//					}
//					
//					dirtyBlocks[i] = getHexFromXZ(sb.x, sb.z); i++;
//					dirtyBlocks[i] = (byte) sb.y; i++;
//					dirtyBlocks[i] = this.getBlockIDUpperByte(sb.getType()); i++;
//					dirtyBlocks[i] = getLastByte(sb.getType(), (byte)sb.getData()); i++;
//					
//					count++;
//					iter.remove();
//				}
//				
//				if (count > 0 && distance != null) {
//					Packet52MultiBlockChange packet = new Packet52MultiBlockChange(coord.getX(), coord.getZ(),
//							new short[64],
//							63,
//							w);
//					
//					packet.c = dirtyBlocks;
//					packet.d = count;
//					preparedPackets.put(distanceKey, packet);
//				}
//			}
//		}
//		
//		blocksInChunk.clear();
//		blocksInChunkToUpdate.put(playerName, blocksInChunk);
//
//		CivLog.debug("Ready with:"+preparedPackets.size()+" packets.");			
//		
//		class SendPlayerPacketChanges implements Runnable {
//			TreeMap<String, Packet52MultiBlockChange> packets;
//			String playerName;
//			
//			public SendPlayerPacketChanges(TreeMap<String, Packet52MultiBlockChange> packets, String playerName) {
//				this.packets = packets;
//				this.playerName = playerName;
//			}
//			
//			@Override
//			public void run() {
//				while(true) {
//					if (taskLock.tryLock()) {
//						try {
//							for (Packet52MultiBlockChange packet : packets.values()) {
//								Player player;
//								try {
//									player = CivGlobal.getPlayer(playerName);
//								} catch (CivException e) {
//									return;
//								}
//								
//								CraftPlayer craftPlayer = (CraftPlayer)player;
//								CivLog.debug("sending packet");
//								craftPlayer.getHandle().playerConnection.sendPacket(packet);	
//								
//								synchronized(this) {
//									try {
//										this.wait(500);
//									} catch (InterruptedException e) {
//										return;
//									}
//								}
//							}
//						} finally {
//							taskLock.unlock();
//						}
//						break;
//					} else {
//						synchronized(this) {
//							try {
//								CivLog.debug("Waiting....");
//								this.wait(500);
//							} catch (InterruptedException e) {
//								e.printStackTrace();
//								return;
//							}
//						}
//						/* Loop back around and try again. */
//					}
//				}
//			}
//			
//		}
//		
//		TaskMaster.asyncTask(new SendPlayerPacketChanges(this.preparedPackets, playerName), 0);
//		
//		
//		//blocksToUpdate.remove(playerName);
//	}
}
