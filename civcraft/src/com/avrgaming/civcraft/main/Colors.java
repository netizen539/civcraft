// TODO refactor this, convert to Color codes and contain all colors.
package com.avrgaming.civcraft.main;

import org.bukkit.ChatColor;

public class Colors {
	public static final String Black = "\u00A70";
	public static final String Navy = "\u00A71";
	public static final String Green = "\u00A72";
	public static final String Blue = "\u00A73";
	public static final String Red = "\u00A74";
	public static final String Purple = "\u00A75";
	public static final String Gold = "\u00A76";
	public static final String LightGray = "\u00A77";
	public static final String Gray = "\u00A78";
	public static final String DarkPurple = "\u00A79";
	public static final String LightGreen = "\u00A7a";
	public static final String LightBlue = "\u00A7b";
	public static final String Rose = "\u00A7c";
	public static final String LightPurple = "\u00A7d";
	public static final String Yellow = "\u00A7e";
	public static final String White = "\u00A7f";

	public static String strip(String line) {

		for (ChatColor cc : ChatColor.values())
			line.replaceAll(cc.toString(), "");
		return line;
	}

	public static String valueOf(String color) {
		switch (color.toLowerCase()) {
		case "black":
			return Black;
		case "navy":
			return Navy;
		case "green":
			return Green;
		case "blue":
			return Blue;
		case "red":
			return Red;
		case "purple":
			return Purple;
		case "gold":
			return Gold;
		case "lightgray":
			return LightGray;
		case "gray":
			return Gray;
		case "darkpurple":
			return DarkPurple;
		case "lightgreen":
			return LightGreen;
		case "lightblue":
			return LightBlue;
		case "rose":
			return Rose;
		case "lightpurple":
			return LightPurple;
		case "yellow":
			return Yellow;
		case "white":
			return White;
		default:
			return White;
		}		
	}
}
