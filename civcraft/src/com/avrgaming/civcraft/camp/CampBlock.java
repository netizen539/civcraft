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
package com.avrgaming.civcraft.camp;

import com.avrgaming.civcraft.util.BlockCoord;

public class CampBlock {
	//XXX TODO merge this with structure block?
	private BlockCoord coord;
	private Camp camp;
	private boolean friendlyBreakable = false;
	
	public CampBlock(BlockCoord coord, Camp camp) {
		this.coord = coord;
		this.camp = camp;
	}
	
	public CampBlock(BlockCoord coord, Camp camp, boolean friendlyBreakable) {
		this.coord = coord;
		this.camp = camp;
		this.friendlyBreakable = friendlyBreakable;
	}

	public BlockCoord getCoord() {
		return coord;
	}
	public void setCoord(BlockCoord coord) {
		this.coord = coord;
	}
	public Camp getCamp() {
		return camp;
	}
	public void setCamp(Camp camp) {
		this.camp = camp;
	}
	
	public int getX() {
		return this.coord.getX();
	}
	
	public int getY() {
		return this.coord.getY();
	}
	
	public int getZ() {
		return this.coord.getZ();
	}
	
	public String getWorldname() {
		return this.coord.getWorldname();
	}
	
	public boolean canBreak(String playerName) {
		if (this.friendlyBreakable == false) {
			return false;
		}
		
		if (camp.hasMember(playerName)) {
			return true;
		}
		
		return false;
	}
	
}
