package com.avrgaming.civcraft.threading.timers;

import java.util.Iterator;
import java.util.Map.Entry;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.wonders.Wonder;
import com.avrgaming.civcraft.util.BlockCoord;

public class RegenTimer implements Runnable {

	@Override
	public void run() {
		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
		
		while(iter.hasNext()) {
			Structure struct = iter.next().getValue();
			struct.processRegen();
		}
		
		for (Wonder wonder : CivGlobal.getWonders()) {
			wonder.processRegen();
		}
	}

}
