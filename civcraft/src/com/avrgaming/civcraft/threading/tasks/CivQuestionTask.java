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
package com.avrgaming.civcraft.threading.tasks;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.QuestionResponseInterface;
import com.avrgaming.civcraft.util.CivColor;

public class CivQuestionTask implements Runnable {
	
	Civilization askedCiv; /* player who is being asked a question. */
	Civilization questionCiv; /* player who has asked the question. */
	String question; /* Question being asked. */
	long timeout; /* Timeout after question expires. */
	QuestionResponseInterface finishedFunction;
	
	private String response = new String(); /* Response to the question. */
	private Boolean responded = new Boolean(false); /*Question was answered. */
	
	public CivQuestionTask(Civilization askedciv, Civilization questionciv, String question, long timeout, 
			QuestionResponseInterface finishedFunction) {
		
		this.askedCiv = askedciv;
		this.questionCiv = questionciv;
		this.question = question;
		this.timeout = timeout;
		this.finishedFunction = finishedFunction;
		
	}
	
	public void askPlayer(Player player) {
		CivMessage.send(player, CivColor.LightGray+"Request from: "+CivColor.LightBlue+questionCiv.getName());
		CivMessage.send(player, question);
		CivMessage.send(player, CivColor.LightGray+"Respond by typing "+CivColor.LightBlue+"/civ dip respond yes"+CivColor.LightGray+" or "+CivColor.LightBlue+"/civ dip respond no");
	}
	
	public void notifyExpired(Player player) {
		CivMessage.send(player, CivColor.LightGray+"The offer from "+questionCiv.getName()+" expired.");
	}
	
	
	@Override
	public void run() {
		for (Resident res : askedCiv.getLeaderGroup().getMemberList()) {
			try {
				askPlayer(CivGlobal.getPlayer(res));
			} catch (CivException e) {
			}
		}
		
		for (Resident res : askedCiv.getAdviserGroup().getMemberList()) {
			try {
				askPlayer(CivGlobal.getPlayer(res));
			} catch (CivException e) {
			}
		}
		
		try {
			synchronized(this) {
				this.wait(timeout);
			}
		} catch (InterruptedException e) {
			cleanup();
			return;
		}
		
		if (responded) {
			finishedFunction.processResponse(response);
			cleanup();
			return;
		}
		
		for (Resident res : askedCiv.getLeaderGroup().getMemberList()) {
			try {
				notifyExpired(CivGlobal.getPlayer(res));
			} catch (CivException e) {
			}
		}
		
		for (Resident res : askedCiv.getAdviserGroup().getMemberList()) {
			try {
				notifyExpired(CivGlobal.getPlayer(res));
			} catch (CivException e) {
			}
		}
		
		CivMessage.sendCiv(questionCiv, CivColor.LightGray+askedCiv.getName()+" gave no response to our offer.");
		cleanup();		
	}
	
	public Boolean getResponded() {
		synchronized(responded) {
			return responded;
		}
	}

	public void setResponded(Boolean response) {
		synchronized(this.responded) {
			this.responded = response;
		}
	}

	public String getResponse() {
		synchronized(response) {
			return response;
		}
	}

	public void setResponse(String response) {
		synchronized(this.response) {
			setResponded(true);
			this.response = response;
		}
	}
	
	/* When this task finishes, remove itself from the hashtable. */
	private void cleanup() {
		CivGlobal.removeRequest(askedCiv.getName());
	}
}
