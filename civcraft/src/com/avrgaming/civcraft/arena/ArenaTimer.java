package com.avrgaming.civcraft.arena;

public class ArenaTimer implements Runnable {

	@Override
	public void run() {

		for (Arena arena : ArenaManager.activeArenas.values()) {
			arena.decrementTimer();
		}
		
	}

}
