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
package com.avrgaming.civcraft.components;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.structure.Buildable;
import com.avrgaming.civcraft.threading.TaskMaster;

public class Component {

	public static ConcurrentHashMap<String, ArrayList<Component>> componentsByType = new ConcurrentHashMap<String, ArrayList<Component>>();
	
	public static ReentrantLock componentsLock = new ReentrantLock();
	
	/* 
	 * Allow components to be specified in YAMLs. To do this each component must be given
	 * a name and some attributes. Examples:
	 * 
	 * components:
	 *     - name: 'AttributeStatic'
	 *       type: 'direct'
	 *       attribute: 'beakers'
	 *       value: '50.0'
	 * 
	 * We use the power of YAML to find the key-values other than name and populate them here.
	 * We then register that component to the structure automatically on construction.
	 * 
	 */
	private String name;
	private Buildable buildable;
	private HashMap<String, String> attributes = new HashMap<String, String>();
	protected String typeName = null;
	
	public void createComponent(Buildable buildable) {
		this.createComponent(buildable, false);
	}
	
	public void createComponent(Buildable buildable, boolean async) {
		if (typeName == null) {
			if (async) {
				TaskMaster.asyncTask(new RegisterComponentAsync(buildable, this, this.getClass().getName(), true), 0);
			} else {
				new RegisterComponentAsync(buildable, this, this.getClass().getName(), true).run();
			}
		} else {
			if (async) {
				TaskMaster.asyncTask(new RegisterComponentAsync(buildable, this, typeName, true), 0);
			} else {
				new RegisterComponentAsync(buildable, this, typeName, true).run();
			}
		}
		this.buildable = buildable;
	}
	
	public void destroyComponent() {
		TaskMaster.asyncTask(new RegisterComponentAsync(null, this, this.getClass().getName(), false), 0);
	}

	public void onLoad() {
	}

	public void onSave() {
	}

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

	public Buildable getBuildable() {
		return buildable;
	}

	public void setBuildable(Buildable buildable) {
		this.buildable = buildable;
	}
	
	public boolean isActive() {
		if (buildable != null) {
			return buildable.isActive();
		} else {
			return false;
		}
	}
	
}
