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
package com.avrgaming.civcraft.util;

import java.util.ArrayList;

import org.bukkit.inventory.meta.BookMeta;

public class BookUtil {
	
	public static final int LINES_PER_PAGE = 12; 
	public static final int CHARS_PER_LINE = 18;  
	
	/*
	 * grumble grumble stupid books I just want to send text to you
	 * in a stream and have you put it in pages why I gotta paginate it myself 
	 * grumble grumble.
	 */
	
	public static void paginate(BookMeta meta, String longString) {	
		/* Break page into lines and pass into longString. */
		int count = 0;
		
		ArrayList<String> lines = new ArrayList<String>();
		
		String line = "";
		for (char c : longString.toCharArray()) {
			count++;
			if (c == '\n' || count > CHARS_PER_LINE) {
				lines.add(line);
				line = "";
				count = 0;
			}
			if (c != '\n') {
				line += c;
			}
		}
		
		linePageinate(meta, lines);
	}
	
	public static void linePageinate(BookMeta meta, ArrayList<String> lines) {
		/*
		 * 13 writeable lines per page, iterate through each line
		 * and place into the page, when the line count equals 14
		 * set it back to 0 and add page.
		 */
		
		int count = 0;
		String page = "";
		for (String line : lines) {
			count++;
			if (count > LINES_PER_PAGE) {
				meta.addPage(page);
				count = 0;
				page = "";
			}
			page += line+"\n";			
		}
		
		meta.addPage(page);
	}
	
}
