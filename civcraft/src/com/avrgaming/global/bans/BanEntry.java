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
package com.avrgaming.global.bans;

public class BanEntry {
	public String name = "";
	public String server = "";
	public String reason = "";
	public String banned_by = "";
	public String unbanned_by = "";
	public String unbanned_reason = "";
	public boolean banned = false;
	public boolean muted = false;
	public String muted_by = "";
	public String muted_reason = "";
	public long mute_expires = 0;
	public int banned_count = 0;
	public long time = 0;
	public long expires = 0;
	
}
