package com.avrgaming.civcraft.config;

import java.util.LinkedList;

import com.avrgaming.civcraft.util.BlockCoord;

public class ConfigArenaTeam {
	public Integer number;
	public String name;
	public LinkedList<BlockCoord> controlPoints;
	public LinkedList<BlockCoord> revivePoints;
	public LinkedList<BlockCoord> respawnPoints;
	public LinkedList<BlockCoord> chests;
	public BlockCoord respawnSign;
}
