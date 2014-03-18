package com.avrgaming.civcraft.object;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;

import com.avrgaming.civcraft.main.CivMessage;

public class AttrSource {
	/* Contains a list of sources and the total. */
	public HashMap<String, Double> sources;
	public double total;
	AttrSource rate;

	public AttrSource(HashMap<String, Double> sources, double total, AttrSource rate) {
		this.sources = sources;
		this.total = total;
		this.rate = rate;
	}
	
	public ArrayList<String> getSourceDisplayString(String sourceColor, String valueColor) {
		ArrayList<String> out = new ArrayList<String>();
		DecimalFormat df = new DecimalFormat();
		
		out.add(CivMessage.buildSmallTitle("Sources"));
		
		for (String source : sources.keySet()) {
			out.add(sourceColor+source+": "+valueColor+df.format(sources.get(source)));
		}
		
		return out;
	}
	
	public ArrayList<String> getRateDisplayString(String sourceColor, String valueColor) {
		ArrayList<String> out = new ArrayList<String>();
		DecimalFormat df = new DecimalFormat();
		
		if (rate != null) {			
			out.add(CivMessage.buildSmallTitle("Rates"));
			
			for (String source : rate.sources.keySet()) {
				out.add(sourceColor+source+": "+valueColor+df.format(rate.sources.get(source)*100)+"%");
			}
		}
		return out;
	}
	
	public ArrayList<String> getTotalDisplayString(String sourceColor, String valueColor) {
		ArrayList<String> out = new ArrayList<String>();
		DecimalFormat df = new DecimalFormat();
		
		out.add(CivMessage.buildSmallTitle("Totals"));
		out.add(sourceColor+"Total: "+valueColor+df.format(this.total)+sourceColor);
		return out;
	}
}
