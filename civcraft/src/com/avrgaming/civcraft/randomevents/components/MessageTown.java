package com.avrgaming.civcraft.randomevents.components;


import com.avrgaming.civcraft.randomevents.RandomEventComponent;

public class MessageTown extends RandomEventComponent {

	@Override
	public void process() {
		String message = this.getString("message");
		sendMessage(message);
	}	
}
