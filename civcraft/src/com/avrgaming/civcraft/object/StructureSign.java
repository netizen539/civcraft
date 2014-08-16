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
package com.avrgaming.civcraft.object;

import org.bukkit.block.Sign;
import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.util.BlockCoord;

public class StructureSign {

	private String text;
	private Buildable owner;
	private String type;
	private String action;
	private BlockCoord coord;
	private int direction;
	private boolean allowRightClick = false;
	
	public StructureSign(BlockCoord coord, Buildable owner) {
		this.coord = coord;
		this.owner = owner;
	}

	public String getText() {
		return text;
	}

	public void setText(String text) {
		this.text = text;
	}

	public Buildable getOwner() {
		return owner;
	}

	public void setOwner(Buildable owner) {
		this.owner = owner;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}

	public String getAction() {
		return action;
	}

	public void setAction(String action) {
		this.action = action;
	}

	public BlockCoord getCoord() {
		return coord;
	}

	public void setCoord(BlockCoord coord) {
		this.coord = coord;
	}

	public int getDirection() {
		return direction;
	}

	public void setDirection(int direction) {
		this.direction = direction;
	}

	public void setText(String[] message) {
		this.text = "";
		for (String str : message) {
			text += str+"\n";
		}
	}
	
	public void update() {
		if (coord.getBlock().getState() instanceof Sign) {
			Sign sign = (Sign)coord.getBlock().getState();
			String[] lines = this.text.split("\\n");
			
			for (int i = 0; i < 4; i++) {
				if (i < lines.length) {
					sign.setLine(i, lines[i]);
				} else {
					sign.setLine(i, "");
				}
			}
			sign.update();
		}
	}

	public boolean isAllowRightClick() {
		return allowRightClick;
	}

	public void setAllowRightClick(boolean allowRightClick) {
		this.allowRightClick = allowRightClick;
	}

}
