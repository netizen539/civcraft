// $Id$
/*
 * Very bad image drawer
 * Copyright (C) 2011 sk89q <http://www.sk89q.com>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
*/

importPackage(Packages.java.io);
importPackage(Packages.java.awt);
importPackage(Packages.com.sk89q.worldedit);
importPackage(Packages.com.sk89q.worldedit.blocks);
importPackage(Packages.com.sk89q.worldedit.regions);

context.checkArgs(1, 3, "<filename>");

//var f = new File(argv[1]);

var session = context.remember();
var WALLSIGN = 68;
var SIGNPOST = 63;

var f = new FileWriter("templates/"+argv[1]);
var region = context.getSession().getRegion();

var r_x = region.getWidth();
var r_y = region.getHeight();
var r_z = region.getLength();

var iter = new RegionIterator(region);

try {
	f.write(r_x+";"+r_y+";"+r_z+"\n");
	for (var z = 0; z < r_z; z++) {
		for ( var y = 0; y < r_y; y++) {
			for (var x = 0; x < r_x; x++) {
				var blkVect = iter.next();
				var blkId = session.getBlockType(blkVect);
				var blkData = session.getBlockData(blkVect);
				
				f.write(x+":"+y+":"+z+",");
				f.write(blkId+":"+blkData);
				if (blkId === WALLSIGN || blkId == SIGNPOST) {
					var sign =  session.rawGetBlock(blkVect);
					f.write(",");
					f.write(sign.getText()[0]+",");
					f.write(sign.getText()[1]+",");
					f.write(sign.getText()[2]+",");
					f.write(sign.getText()[3]+"\n");
				}
				else {
					f.write("\n");

				}
			}
		}
	}



}
finally
{
	f.close();
}


/*var origin = player.getBlockIn();

for (var x = 0; x < width; x++) {
	for (var y = 0; y < height; y++) {
	    var c = new Color(img.getRGB(x, y));
	    var data = findClosestWoolColor(c,colors);
			// Added this to enable the user to create images upright
	    // rather than flat on the ground
			if (!upright) {
		sess.setBlock(origin.add(x, 0, y), new BaseBlock(35, data));
			} else {
		sess.setBlock(origin.add(x, height - y, 0), new BaseBlock(35, data));
			}
	}
}*/
