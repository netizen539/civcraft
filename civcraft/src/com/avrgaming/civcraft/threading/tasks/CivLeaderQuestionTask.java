package com.avrgaming.civcraft.threading.tasks;

import org.bukkit.entity.Player;

import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Civilization;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.questions.QuestionBaseTask;
import com.avrgaming.civcraft.questions.QuestionResponseInterface;
import com.avrgaming.civcraft.util.CivColor;

public class CivLeaderQuestionTask extends QuestionBaseTask implements Runnable {
	Civilization askedCivilization; /* player who is being asked a question. */
	Player questionPlayer; /* player who has asked the question. */
	String question; /* Question being asked. */
	long timeout; /* Timeout after question expires. */
//	RunnableWithArg finishedTask; /* Task to run when a response has been generated. */
	QuestionResponseInterface finishedFunction;
	Resident responder;
	
	protected String response = new String(); /* Response to the question. */
	protected Boolean responded = new Boolean(false); /*Question was answered. */
	
	
	public CivLeaderQuestionTask(Civilization askedplayer, Player questionplayer, String question, long timeout, 
			QuestionResponseInterface finishedFunction) {
		
		this.askedCivilization = askedplayer;
		this.questionPlayer = questionplayer;
		this.question = question;
		this.timeout = timeout;
		this.finishedFunction = finishedFunction;
		
	}
	
	@Override
	public void run() {	
		
		for (Resident resident : askedCivilization.getLeaderGroup().getMemberList()) {
			CivMessage.send(resident, CivColor.LightGray+"Question from: "+CivColor.LightBlue+questionPlayer.getName());
			CivMessage.send(resident, CivColor.LightPurple+CivColor.BOLD+question);
			CivMessage.send(resident, CivColor.LightGray+"Respond by typing "+CivColor.LightBlue+"/accept"+CivColor.LightGray+" or "+CivColor.LightBlue+"/deny");
		}
		
		try {
			synchronized(this) {
				this.wait(timeout);
			}
		} catch (InterruptedException e) {
			CivMessage.send(questionPlayer, CivColor.LightGray+"Task interrupted before a response could be given.");
			cleanup();
			return;
		}
		
		if (responded) {
			finishedFunction.processResponse(response, responder);
			cleanup();
			return;
		}
		
		CivMessage.send(questionPlayer, CivColor.LightGray+"No Response from civilization leaders");
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
		CivGlobal.removeQuestion("civ:"+askedCivilization.getName());
	}

	public void setResponder(Resident resident) {
		this.responder = resident;
	}
}
