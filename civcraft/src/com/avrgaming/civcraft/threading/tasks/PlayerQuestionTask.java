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

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.questions.QuestionBaseTask;
import com.avrgaming.civcraft.questions.QuestionResponseInterface;
import com.avrgaming.civcraft.util.CivColor;

public class PlayerQuestionTask extends QuestionBaseTask implements Runnable {

	Player askedPlayer; /* player who is being asked a question. */
	Player questionPlayer; /* player who has asked the question. */
	String question; /* Question being asked. */
	long timeout; /* Timeout after question expires. */
//	RunnableWithArg finishedTask; /* Task to run when a response has been generated. */
	QuestionResponseInterface finishedFunction;
	
	protected String response = new String(); /* Response to the question. */
	protected Boolean responded = new Boolean(false); /*Question was answered. */
	
	public PlayerQuestionTask() {
	}
	
	public PlayerQuestionTask(Player askedplayer, Player questionplayer, String question, long timeout, 
			QuestionResponseInterface finishedFunction) {
		
		this.askedPlayer = askedplayer;
		this.questionPlayer = questionplayer;
		this.question = question;
		this.timeout = timeout;
		this.finishedFunction = finishedFunction;
		
	}
	
	@Override
	public void run() {	
		CivMessage.send(askedPlayer, CivColor.LightGray+"Question from: "+CivColor.LightBlue+questionPlayer.getName());
		CivMessage.send(askedPlayer, CivColor.LightPurple+CivColor.BOLD+question);
		CivMessage.send(askedPlayer, CivColor.LightGray+"Respond by typing "+CivColor.LightBlue+"/accept"+CivColor.LightGray+" or "+CivColor.LightBlue+"/deny");
		
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
		
		CivMessage.send(askedPlayer, CivColor.LightGray+"You failed to respond to the question from "+questionPlayer.getName()+" in time.");
		CivMessage.send(questionPlayer, CivColor.LightGray+askedPlayer.getName()+" failed to answer the question in time.");
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
		CivGlobal.removeQuestion(askedPlayer.getName());
	}
	
	
	
}
