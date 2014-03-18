package com.avrgaming.dynmap.civcraft;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;
import java.util.Map.Entry;

import org.dynmap.DynmapAPI;
import org.dynmap.markers.AreaMarker;
import org.dynmap.markers.Marker;
import org.dynmap.markers.MarkerAPI;
import org.dynmap.markers.MarkerSet;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.object.CultureChunk;
import com.avrgaming.civcraft.object.Town;
import com.avrgaming.civcraft.object.TownChunk;
import com.avrgaming.civcraft.structure.Structure;
import com.avrgaming.civcraft.util.BlockCoord;

public class CivCraftUpdateTask implements Runnable {

	DynmapAPI api;
	MarkerAPI markerapi;
	MarkerSet townBorderSet;
	MarkerSet cultureSet;
	MarkerSet structureSet;
	
	public final int CHUNKSIZE = 16;
    enum direction { XPLUS, ZPLUS, XMINUS, ZMINUS };
    /* Used only for internal storage? */
    private Map<String, AreaMarker> renderTownBorderAreas = new HashMap<String, AreaMarker>();
    private Map<String, AreaMarker> renderCultureAreas = new HashMap<String, AreaMarker>();
    private Map<String, Marker> structureMarkers = new HashMap<String, Marker>();


	public CivCraftUpdateTask(DynmapAPI api, MarkerAPI markerapi, MarkerSet townset, MarkerSet cultureset, MarkerSet structureSet) {
		this.api = api;
		this.markerapi = markerapi;
		this.townBorderSet = townset;
		this.cultureSet = cultureset;
		this.structureSet = structureSet;
	}
	
	  /**
     * Find all contiguous blocks, set in target and clear in source
     */
    private int floodFillTarget(TileFlags src, TileFlags dest, int x, int y) {
        int cnt = 0;
        ArrayDeque<int[]> stack = new ArrayDeque<int[]>();
        stack.push(new int[] { x, y });
        
        while(stack.isEmpty() == false) {
            int[] nxt = stack.pop();
            x = nxt[0];
            y = nxt[1];
            if(src.getFlag(x, y)) { /* Set in src */
                src.setFlag(x, y, false);   /* Clear source */
                dest.setFlag(x, y, true);   /* Set in destination */
                cnt++;
                if(src.getFlag(x+1, y))
                    stack.push(new int[] { x+1, y });
                if(src.getFlag(x-1, y))
                    stack.push(new int[] { x-1, y });
                if(src.getFlag(x, y+1))
                    stack.push(new int[] { x, y+1 });
                if(src.getFlag(x, y-1))
                    stack.push(new int[] { x, y-1 });
            }
        }
        return cnt;
    }
	
	public void handleTownBorders(Town town, Map<String, AreaMarker> newmap) {
 	   //DynmapCivcraftPlugin.log.info("handle town:"+town.getName());

		Collection<TownChunk> blocks = town.getTownChunks();
		int poly_index = 0; /* RJ this needed? */
		
	   	if(blocks.isEmpty()) {
	  	 // DynmapCivcraftPlugin.log.info("no blocks");
    	    return;
	   	}

    	HashMap<String, TileFlags> blkmaps = new HashMap<String, TileFlags>();
        LinkedList<TownChunk> nodevals = new LinkedList<TownChunk>();
        String currentWorldName = null;
        TileFlags curblks = null;

        /* Loop through blocks: set flags on blockmaps for worlds */
    	for(TownChunk b : blocks) {
    	    if(!b.getChunkCoord().getWorldname().equals(currentWorldName)) { /* Not same world */
    	        String worldname = b.getChunkCoord().getWorldname();
    	            curblks = blkmaps.get(worldname);  /* Find existing */
    	            if(curblks == null) {
    	                curblks = new TileFlags();
    	                blkmaps.put(worldname, curblks);   /* Add fresh one */
    	            }
    	        currentWorldName = b.getChunkCoord().getWorldname();
    	    }
	        curblks.setFlag(b.getChunkCoord().getX(), b.getChunkCoord().getZ(), true); /* Set flag for block */
	        nodevals.addLast(b);
    	    
    	}
        /* Loop through until we don't find more areas */
        while(nodevals != null) {
            LinkedList<TownChunk> ournodes = null;
            LinkedList<TownChunk> newlist = null;
            TileFlags ourblks = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for(TownChunk node : nodevals) {
                int nodex = node.getChunkCoord().getX();
                int nodez = node.getChunkCoord().getZ();
                if(ourblks == null) {   /* If not started, switch to world for this block first */
                    if(node.getChunkCoord().getWorldname().equals(currentWorldName) == false) {
                        currentWorldName = node.getChunkCoord().getWorldname();
                        curblks = blkmaps.get(currentWorldName);
                    }
                }
                /* If we need to start shape, and this block is not part of one yet */
            	//DynmapCivcraftPlugin.log.info("flag was::"+curblks.getFlag(nodex, nodez));
                if((ourblks == null) && curblks.getFlag(nodex, nodez)) {
                    ourblks = new TileFlags();  /* Create map for shape */
                    ournodes = new LinkedList<TownChunk>();
                    floodFillTarget(curblks, ourblks, nodex, nodez);   /* Copy shape */
                    ournodes.add(node); /* Add it to our node list */
                    minx = nodex; minz = nodez;
                }
                /* If shape found, and we're in it, add to our node list */
                else if((ourblks != null) && (node.getChunkCoord().getWorldname().equals(currentWorldName)) &&
                    (ourblks.getFlag(nodex, nodez))) {
                    ournodes.add(node);
                    if(nodex < minx) {
                        minx = nodex; minz = nodez;
                    }
                    else if((nodex == minx) && (nodez < minz)) {
                        minz = nodez;
                    }
                }
                else {  /* Else, keep it in the list for the next polygon */
                    if(newlist == null) newlist = new LinkedList<TownChunk>();
                    newlist.add(node);
                	//DynmapCivcraftPlugin.log.info("adding node:"+node.getChunkCoord());
                }
            }
            nodevals = newlist; /* Replace list (null if no more to process) */ 
            if(ourblks != null) {
                /* Trace outline of blocks - start from minx, minz going to x+ */
                int init_x = minx;
                int init_z = minz;
                int cur_x = minx;
                int cur_z = minz;
                direction dir = direction.XPLUS;
                ArrayList<int[]> linelist = new ArrayList<int[]>();
                linelist.add(new int[] { init_x, init_z } ); // Add start point
            	//DynmapCivcraftPlugin.log.info("starting at:"+init_x+","+init_z);
                while((cur_x != init_x) || (cur_z != init_z) || (dir != direction.ZMINUS)) {
                    switch(dir) {
                        case XPLUS: /* Segment in X+ direction */
                            if(!ourblks.getFlag(cur_x+1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.ZPLUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x+1, cur_z-1)) {  /* Straight? */
                                cur_x++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.ZMINUS;
                                cur_x++; cur_z--;
                            }
                            break;
                        case ZPLUS: /* Segment in Z+ direction */
                            if(!ourblks.getFlag(cur_x, cur_z+1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.XMINUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x+1, cur_z+1)) {  /* Straight? */
                                cur_z++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.XPLUS;
                                cur_x++; cur_z++;
                            }
                            break;
                        case XMINUS: /* Segment in X- direction */
                            if(!ourblks.getFlag(cur_x-1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.ZMINUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x-1, cur_z+1)) {  /* Straight? */
                                cur_x--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.ZPLUS;
                                cur_x--; cur_z++;
                            }
                            break;
                        case ZMINUS: /* Segment in Z- direction */
                            if(!ourblks.getFlag(cur_x, cur_z-1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.XPLUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x-1, cur_z-1)) {  /* Straight? */
                                cur_z--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.XMINUS;
                                cur_x--; cur_z--;
                            }
                            break;
                    }
                }
                /* Build information for specific area */
                String polyid = town.getName() + "_townborder_" + poly_index;
            	//DynmapCivcraftPlugin.log.info("building area: " + polyid);

                int sz = linelist.size();
                double[] x = new double[sz];
                double[] z = new double[sz];
                for(int i = 0; i < sz; i++) {
                    int[] line = linelist.get(i);
                    x[i] = (double)line[0] * (double)CHUNKSIZE;
                    z[i] = (double)line[1] * (double)CHUNKSIZE;
                }
                /* Find existing one */
                AreaMarker m = renderTownBorderAreas.remove(polyid); /* Existing area? */
                if(m == null) {
                //	DynmapCivcraftPlugin.log.info("NO AREA, ADDING A NEW ONE");
                	m = townBorderSet.createAreaMarker(polyid, town.getName(), false, currentWorldName, x, z, false);
	                if(m == null) {
	               // 	DynmapCivcraftPlugin.log.info("error adding area marker " + polyid);
	                	continue;
	                }
                }
                else {
                	//DynmapCivcraftPlugin.log.info("FOUND EXISTING, UPDATING.");
                    m.setCornerLocations(x, z); /* Replace corner locations */
                    m.setLabel(town.getName());   /* Update label */
                    m.setDescription(town.getDynmapDescription());
                }
            
                /* Set line and fill properties */
                /* Add to map */
                m.setLineStyle(1, 0.7, 0xFF0000);
                m.setFillStyle(0.4, 0xFF0000);
                newmap.put(polyid, m);
                poly_index++;
            }
        }
		
		
		//List<Double> xList = new LinkedList<Double>();
		
		
		//townBorderSet.createAreaMarker(town.getName()+"-border-marker", town.getName(), false, 
		//		"world", xArray, zArray, false);

	}
	
	public void updateTownBorders() {
		/* build new maps */
        Map<String, AreaMarker> newTownBordermap = new HashMap<String,AreaMarker>(); 
        Map<String, AreaMarker> newCultureMap = new HashMap<String,AreaMarker>(); 

      //  for (AreaMarker am : renderCultureAreas.values()) {
      //  	am.
      //  }
     
		for (Town t : CivGlobal.getTowns()) {
			//deleteInvalidCultureMarkers(t);
			handleCulture(t, newCultureMap);
			handleTownBorders(t, newTownBordermap);
		}
		
		renderTownBorderAreas = newTownBordermap;
		renderCultureAreas = newCultureMap;
	}
	
	
	
	/*private void deleteInvalidCultureMarkers(Town t) {

		ArrayList<String> keysToRemove = new ArrayList<String>();
		for (String key : renderCultureAreas.keySet()) {
			String[] split = key.split(":");
			
			if (split[1].equalsIgnoreCase(t.getName())) {
				String[] coordSplit = split[2].split(",");
				ChunkCoord coord = new ChunkCoord(coordSplit[0], 
						Integer.valueOf(coordSplit[1]), 
						Integer.valueOf(coordSplit[2]));
				
				if (t.getCultureChunk(coord) == null) {
					AreaMarker am = renderCultureAreas.get(key);
					am.deleteMarker();
					keysToRemove.add(key);					
				}
			}
		}
		
		for (String key : keysToRemove) {
			renderCultureAreas.remove(key);
		}
		
	}*/

	private void handleCulture(Town town, Map<String, AreaMarker> newCultureMap) {

		Collection<CultureChunk> blocks = town.getCultureChunks();
		int poly_index = 0; /* RJ this needed? */
		
	   	if(blocks.isEmpty()) {
	  	 // DynmapCivcraftPlugin.log.info("no blocks");
    	    return;
	   	}

    	HashMap<String, TileFlags> blkmaps = new HashMap<String, TileFlags>();
        LinkedList<CultureChunk> nodevals = new LinkedList<CultureChunk>();
        String currentWorldName = null;
        TileFlags curblks = null;

        /* Loop through blocks: set flags on blockmaps for worlds */
    	for(CultureChunk cb : blocks) {
    	    if(!cb.getChunkCoord().getWorldname().equals(currentWorldName)) { /* Not same world */
    	        String worldname = cb.getChunkCoord().getWorldname();
    	            curblks = blkmaps.get(worldname);  /* Find existing */
    	            if(curblks == null) {
    	                curblks = new TileFlags();
    	                blkmaps.put(worldname, curblks);   /* Add fresh one */
    	            }
    	        currentWorldName = cb.getChunkCoord().getWorldname();
    	    }
	        curblks.setFlag(cb.getChunkCoord().getX(), cb.getChunkCoord().getZ(), true); /* Set flag for block */
	        nodevals.addLast(cb);
    	    
    	}
        /* Loop through until we don't find more areas */
        while(nodevals != null) {
            LinkedList<CultureChunk> ournodes = null;
            LinkedList<CultureChunk> newlist = null;
            TileFlags ourblks = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for(CultureChunk node : nodevals) {
                int nodex = node.getChunkCoord().getX();
                int nodez = node.getChunkCoord().getZ();
                if(ourblks == null) {   /* If not started, switch to world for this block first */
                    if(node.getChunkCoord().getWorldname().equals(currentWorldName) == false) {
                        currentWorldName = node.getChunkCoord().getWorldname();
                        curblks = blkmaps.get(currentWorldName);
                    }
                }
                /* If we need to start shape, and this block is not part of one yet */
            	//DynmapCivcraftPlugin.log.info("flag was::"+curblks.getFlag(nodex, nodez));
                if((ourblks == null) && curblks.getFlag(nodex, nodez)) {
                    ourblks = new TileFlags();  /* Create map for shape */
                    ournodes = new LinkedList<CultureChunk>();
                    floodFillTarget(curblks, ourblks, nodex, nodez);   /* Copy shape */
                    ournodes.add(node); /* Add it to our node list */
                    minx = nodex; minz = nodez;
                }
                /* If shape found, and we're in it, add to our node list */
                else if((ourblks != null) && (node.getChunkCoord().getWorldname().equals(currentWorldName)) &&
                    (ourblks.getFlag(nodex, nodez))) {
                    ournodes.add(node);
                    if(nodex < minx) {
                        minx = nodex; minz = nodez;
                    }
                    else if((nodex == minx) && (nodez < minz)) {
                        minz = nodez;
                    }
                }
                else {  /* Else, keep it in the list for the next polygon */
                    if(newlist == null) newlist = new LinkedList<CultureChunk>();
                    newlist.add(node);
                	//DynmapCivcraftPlugin.log.info("adding node:"+node.getChunkCoord());
                }
            }
            nodevals = newlist; /* Replace list (null if no more to process) */ 
            if(ourblks != null) {
                /* Trace outline of blocks - start from minx, minz going to x+ */
                int init_x = minx;
                int init_z = minz;
                int cur_x = minx;
                int cur_z = minz;
                direction dir = direction.XPLUS;
                ArrayList<int[]> linelist = new ArrayList<int[]>();
                linelist.add(new int[] { init_x, init_z } ); // Add start point
            	//DynmapCivcraftPlugin.log.info("starting at:"+init_x+","+init_z);
                while((cur_x != init_x) || (cur_z != init_z) || (dir != direction.ZMINUS)) {
                    switch(dir) {
                        case XPLUS: /* Segment in X+ direction */
                            if(!ourblks.getFlag(cur_x+1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.ZPLUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x+1, cur_z-1)) {  /* Straight? */
                                cur_x++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.ZMINUS;
                                cur_x++; cur_z--;
                            }
                            break;
                        case ZPLUS: /* Segment in Z+ direction */
                            if(!ourblks.getFlag(cur_x, cur_z+1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.XMINUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x+1, cur_z+1)) {  /* Straight? */
                                cur_z++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.XPLUS;
                                cur_x++; cur_z++;
                            }
                            break;
                        case XMINUS: /* Segment in X- direction */
                            if(!ourblks.getFlag(cur_x-1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.ZMINUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x-1, cur_z+1)) {  /* Straight? */
                                cur_x--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.ZPLUS;
                                cur_x--; cur_z++;
                            }
                            break;
                        case ZMINUS: /* Segment in Z- direction */
                            if(!ourblks.getFlag(cur_x, cur_z-1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.XPLUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x-1, cur_z-1)) {  /* Straight? */
                                cur_z--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.XMINUS;
                                cur_x--; cur_z--;
                            }
                            break;
                    }
                }
                /* Build information for specific area */
                String polyid = town.getName() + "_townborder_" + poly_index;
            	//DynmapCivcraftPlugin.log.info("building area: " + polyid);

                int sz = linelist.size();
                double[] x = new double[sz];
                double[] z = new double[sz];
                for(int i = 0; i < sz; i++) {
                    int[] line = linelist.get(i);
                    x[i] = (double)line[0] * (double)CHUNKSIZE;
                    z[i] = (double)line[1] * (double)CHUNKSIZE;
                }
                /* Find existing one */
                AreaMarker m = renderCultureAreas.remove(polyid); /* Existing area? */
                if(m == null) {
                //	DynmapCivcraftPlugin.log.info("NO AREA, ADDING A NEW ONE");
                	m = cultureSet.createAreaMarker(polyid, town.getName(), false, currentWorldName, x, z, false);
	                if(m == null) {
	               // 	DynmapCivcraftPlugin.log.info("error adding area marker " + polyid);
	                	continue;
	                }
                }
                else {
                	//DynmapCivcraftPlugin.log.info("FOUND EXISTING, UPDATING.");
                    m.setCornerLocations(x, z); /* Replace corner locations */
                    m.setLabel(town.getName());   /* Update label */
                    m.setDescription(town.getCiv().getCultureDescriptionString());
                }
            
                /* Set line and fill properties */
                /* Add to map */
                m.setLineStyle(0, 0.0, 0xFFFFFF);
                m.setFillStyle(0.4, town.getCiv().getColor());
                newCultureMap.put(polyid, m);
                poly_index++;
            }
        }
		
		
// Too laggy....		
//		/* Experimental... rather than trace a polygon, we will make a new polygon for
//		 * every single culture block. this should allow shapes with holes in them
//		 * so that towns surrounded by another towns culture will render correctly.
//		 */
//		
//		for (CultureChunk cc : t.getCultureChunks()) {
//			double[] xArray = new double[4];
//			double[] zArray = new double[4];
//			
//			// Build a simple square.
//			xArray[0] = cc.getChunkCoord().getX() * CHUNKSIZE;
//			zArray[0] = cc.getChunkCoord().getZ() * CHUNKSIZE;
//
//			xArray[1] = (cc.getChunkCoord().getX() * CHUNKSIZE) + CHUNKSIZE;
//			zArray[1] = cc.getChunkCoord().getZ() * CHUNKSIZE;
//
//			xArray[2] = (cc.getChunkCoord().getX() * CHUNKSIZE) + CHUNKSIZE;
//			zArray[2] = (cc.getChunkCoord().getZ() * CHUNKSIZE) + CHUNKSIZE;
//
//			xArray[3] = cc.getChunkCoord().getX() * CHUNKSIZE;
//			zArray[3] = (cc.getChunkCoord().getZ() * CHUNKSIZE) + CHUNKSIZE;
//
//			String polyid = "culture:"+t.getName()+":"+cc.getChunkCoord();
//			AreaMarker m = renderCultureAreas.remove(polyid); /* Existing area? */
//            if(m == null) {
//             //	DynmapCivcraftPlugin.log.info("Added culture block:"+polyid);
//            	m = cultureSet.createAreaMarker(polyid, t.getName(), false, 
//            			cc.getChunkCoord().getWorldname(), xArray, zArray, false);
//            	
//	            if(m == null) {
//	            	continue;
//	            }
//             }
//             else {
//             	//DynmapCivcraftPlugin.log.info("FOUND EXISTING, UPDATING.");
//                 m.setCornerLocations(xArray, zArray); /* Replace corner locations */
//                 m.setLabel(t.getName());   /* Update label */
//                 m.setDescription(t.getCiv().getCultureDescriptionString());
//             }
//            
//            m.setLineStyle(0, 0.0, 0xFFFFFF);
//            m.setFillStyle(0.4, t.getCiv().getColor());
//            newCultureMap.put(polyid, m);
//         	//DynmapCivcraftPlugin.log.info("rendering:"+newCultureMap.size());
//            
//		}
		
	}

	@Override
	public void run() {
		//double[] xArray = {0, 20, 20, 0};
		//double[] zArray = {0, 0, 20, 20};
		
	//	townBorderSet.createAreaMarker("testmaker", "testareamarker", false, "world", xArray, zArray, false);
		updateTownBorders();
		updateStructures();
		removeOldMarkers();
		//AreaMarker am = new AreaMarker();
		
	}

	private void removeOldMarkers() {
		ArrayList<String> removeKeys = new ArrayList<String>();
		
		for (String key : this.structureMarkers.keySet()) {
			BlockCoord coord = new BlockCoord(key);
			
			Structure struct = CivGlobal.getStructure(coord);
			if (struct == null) {				
				Marker m = this.structureMarkers.get(key);
				m.deleteMarker();
				removeKeys.add(key);
			}
		}
		
		for (String key : removeKeys) {
			this.structureMarkers.remove(key);
		}
		
	}

	private void updateStructures() {
		
		Iterator<Entry<BlockCoord, Structure>> iter = CivGlobal.getStructureIterator();

		while(iter.hasNext()) {
			Structure struct = iter.next().getValue();
			Marker m = this.structureMarkers.get(struct.getCorner().toString());
			
			if (m == null) {
			
				int centerX = struct.getCorner().getX() + (struct.getTemplateX() / 2);
				int centerY = struct.getCorner().getY() + (struct.getTemplateY() / 2);
				int centerZ = struct.getCorner().getZ() + (struct.getTemplateZ() / 2);
				

				
				m = structureSet.createMarker("struct-"+struct.getId(), struct.getDisplayName(), false, 
						struct.getCorner().getWorldname(), 
						centerX, centerY, centerZ, 
						markerapi.getMarkerIcon(struct.getMarkerIconName()), false);
								
				if (m == null) {
					DynmapCivcraftPlugin.log.info("Unable to create marker for:"+struct.getDisplayName()+" at "+struct.getCorner().toString());
					continue;
				}				
			}
			
//			if (m == null) {
	//         	DynmapCivcraftPlugin.log.info("marker was null");
	  //       	return;
		//	}
			
			//if (struct == null) {
	       //  	DynmapCivcraftPlugin.log.info("struct was null");
	       //  	return;
			//}
			
			if (struct.getDisplayName() == null) {
	         	DynmapCivcraftPlugin.log.info("display name was null");
	         	return;
			}
			
			//if (struct != null) {
				m.setLabel(struct.getDisplayName());
				m.setDescription(struct.getDynmapDescription());
			//}
				structureMarkers.put(struct.getCorner().toString(), m);
		}
	}

}
