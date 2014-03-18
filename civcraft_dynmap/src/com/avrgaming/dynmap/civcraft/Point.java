package com.avrgaming.dynmap.civcraft;

public class Point {
	Double x;
	Double z;
	
	public Point(int i, int j) {
		x = Double.valueOf(i);
		z = Double.valueOf(j);
	}

	@Override
	public int hashCode() {
		int hash = 1;
		hash = hash * 17 + x.hashCode();
		hash = hash * 31 + z.hashCode();
		return hash;
	}
}
