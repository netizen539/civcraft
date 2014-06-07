package com.avrgaming.civcraft.util;

import java.util.Collection;
import java.util.LinkedList;

public class Paginator {
	public LinkedList<Object> page = new LinkedList<Object>();
	public boolean hasNextPage = false;
	public boolean hasPrevPage = false;
	public int displayLimit = (9*5)-1;

	public void paginate(Collection<?> source, int pageNumber) {
		int start = pageNumber*(displayLimit);
		int i = 0;
		int d = 0;
		for (Object perk : source) {
			if (i < start) {
				hasPrevPage = true;
				i++;
				continue;
			} 
			
			if (d > displayLimit) {
				hasNextPage = true;
				break;
			}
			
			page.add(perk);
			d++;
		}
	}
}
