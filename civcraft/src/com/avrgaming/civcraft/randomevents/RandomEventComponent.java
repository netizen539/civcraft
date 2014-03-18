package com.avrgaming.civcraft.randomevents;

import java.util.HashMap;

import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Town;

public abstract class RandomEventComponent {
	private String name;
	private HashMap<String, String> attributes = new HashMap<String, String>();
	private RandomEvent parent;
	
	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
	public String getString(String key) {
		return attributes.get(key);
	}
	
	public double getDouble(String key) {
		return Double.valueOf(attributes.get(key));
	}
	
	public void setAttribute(String key, String value) {
		attributes.put(key, value);
	}
	
	protected Town getParentTown() {
		return parent.getTown();
	}
	
	protected RandomEvent getParent() {
		return parent;
	}
	
	protected void sendMessage(String message) {
		CivMessage.sendTown(parent.getTown(), message);
		parent.savedMessages.add(message);
	}
	

	public void createComponent(RandomEvent parent) { this.parent = parent; }
	public abstract void process();
	public boolean onCheck() { return false; }
	public void onStart() {	}
	public void onCleanup() {}
	public boolean requiresActivation() { return false;	}

}
