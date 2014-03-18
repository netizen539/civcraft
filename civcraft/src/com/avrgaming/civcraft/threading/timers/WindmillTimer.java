package com.avrgaming.civcraft.threading.timers;

import java.util.Iterator;
import java.util.Map.Entry;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.structure.Windmill;
import com.avrgaming.civcraft.util.BlockCoord;
import com.avrgaming.civcraft.war.War;

public class WindmillTimer implements Runnable {

	@Override
	public void run() {
		if (War.isWarTime()) {
			return;
		}
		
		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();
		while(iter.hasNext()) {
			Structure struct = iter.next().getValue();
			if (struct instanceof Windmill) {
				((Windmill)struct).processWindmill();
			}
		}
	}

}
