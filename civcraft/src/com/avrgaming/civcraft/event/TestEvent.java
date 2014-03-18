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
package com.avrgaming.civcraft.event;

import java.util.Calendar;

import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivMessage;

public class TestEvent implements EventInterface {

	@Override
	public void process() {
		CivMessage.global("This is a test event firing!");
	}

	@Override
	public Calendar getNextDate() throws InvalidConfiguration {
		Calendar cal = Calendar.getInstance();
		cal.add(Calendar.SECOND, 60);
		return cal;
	}

}
