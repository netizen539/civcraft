package com.avrgaming.civcraft.sessiondb;

import com.avrgaming.civcraft.threading.TaskMaster;

public class SessionAsyncRequest {
	String tb_prefix;
	public SessionEntry entry;
	public Operation op;
	public Database database;
		
	public enum Operation {
		ADD,
		DELETE,
		DELETE_ALL,
		UPDATE, 
		UPDATE_INSERT
	}
	
	public enum Database {
		GAME,
		GLOBAL
	}
	
	public SessionAsyncRequest(Operation op, Database data, String prefix, SessionEntry entry) {
		this.op = op;
		this.database = data;
		this.tb_prefix = prefix;
		this.entry = entry;
	}
	
	public void queue() {
		if (SessionDBAsyncTimer.lock.tryLock()) {
			try {
				SessionDBAsyncTimer.requestQueue.add(this);
			} finally {
				SessionDBAsyncTimer.lock.unlock();
			}
		} else {
			/* Couldn't get lock wait using async task. */
			class AsyncSessionDBRequestWaitTask implements Runnable {
				SessionAsyncRequest request;
				
				public AsyncSessionDBRequestWaitTask(SessionAsyncRequest request) {
					this.request = request;
				}
				
				@Override
				public void run() {
					SessionDBAsyncTimer.lock.lock();
					try {
						SessionDBAsyncTimer.requestQueue.add(request);
					} finally {
						SessionDBAsyncTimer.lock.unlock();
					}
				}
			}
			
			TaskMaster.asyncTask(new AsyncSessionDBRequestWaitTask(this), 0);
		}
	}
	
}
