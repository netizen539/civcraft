package com.avrgaming.civcraft.sessiondb;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.locks.ReentrantLock;

import com.avrgaming.civcraft.database.SQL;

public class SessionDBAsyncTimer implements Runnable {

	private static final int UPDATE_AMOUNT = 30;
	public static ReentrantLock lock = new ReentrantLock();
	public static Queue<SessionAsyncRequest> requestQueue = new LinkedList<SessionAsyncRequest>();
	
	
	@SuppressWarnings("resource")
	@Override
	public void run() {
		
		Connection gameConnection = null;
		Connection globalConnection = null;
		
		for (int i = 0; i < UPDATE_AMOUNT; i++) {
			try {
				lock.lock();
				try {
					SessionAsyncRequest request = requestQueue.poll();
					if (request == null) {
						return;
					}
					
					if (request != null) {
						Connection cntx;
						switch (request.database) {
						case GAME:
							if (gameConnection == null || gameConnection.isClosed()) {
								gameConnection = SQL.getGameConnection();
							}
							cntx = gameConnection;
							break;
						case GLOBAL:
							if (globalConnection == null || globalConnection.isClosed()) {
								globalConnection = SQL.getGlobalConnection();
							}
							cntx = globalConnection;
							break;
						default:
							return;
						}
						
						switch (request.op) {
						case ADD:
							performAdd(request, cntx);
							break;
						case DELETE:
							performDelete(request, cntx);
							break;
						case DELETE_ALL:
							performDeleteAll(request, cntx);
							break;
						case UPDATE:
							performUpdate(request, cntx);
							break;
						case UPDATE_INSERT:
							performUpdateInsert(request, cntx);
							break;
						}
					}
				} catch (Exception e){
					e.printStackTrace();
				} finally {
					lock.unlock();
				}
			} finally {
				try {
					if (gameConnection != null) {
						gameConnection.close();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
				
				try {
					if (globalConnection != null) {
						globalConnection.close();
					}
				} catch (SQLException e) {
					e.printStackTrace();
				}
			}
		}
		
	}
	
	public void performAdd(SessionAsyncRequest request, Connection cntx) throws Exception {
		String code;
		
		code = "INSERT INTO `" + request.tb_prefix + "SESSIONS` (`request_id`, `key`, `value`, `time`, `civ_id`, `town_id`, `struct_id`) VALUES (?, ?, ?, ?, ?, ?, ?)";
		PreparedStatement s = cntx.prepareStatement(code, Statement.RETURN_GENERATED_KEYS);
		s.setNull(1, Types.INTEGER);
		s.setString(2, request.entry.key);
		s.setString(3, request.entry.value);
		s.setLong(4, request.entry.time);
		s.setInt(5, request.entry.civ_id);
		s.setInt(6, request.entry.town_id);
		s.setInt(7, request.entry.struct_id);
		
			
		int rs = s.executeUpdate();
		if (rs == 0) {
			throw new Exception("Could not execute SQL code:"+code);
		}
		
		ResultSet res = s.getGeneratedKeys();
		while(res.next()) {
			//Grab the first one...
			request.entry.request_id = res.getInt(1);
		}
		res.close();
		s.close();
	
		return;
	}
	
	private void performUpdate(SessionAsyncRequest request, Connection cntx) throws Exception {
		String code;
		code = "UPDATE `"+ request.tb_prefix + "SESSIONS` SET `value`= ? WHERE `request_id` = ?";
		PreparedStatement s = cntx.prepareStatement(code);
		s.setString(1, request.entry.value);
		s.setInt(2, request.entry.request_id);

		int rs = s.executeUpdate();
		s.close();
		if (rs == 0) {
			throw new Exception("Could not execute SQL code:"+code+" value="+request.entry.value+" reqid="+request.entry.request_id);
		}
		
		return;		
	}

	private void performUpdateInsert(SessionAsyncRequest request, Connection cntx) throws Exception { 
		String code;
		code = "UPDATE `"+ request.tb_prefix + "SESSIONS` SET `value`= ? WHERE `request_id` = ?";
		PreparedStatement s = cntx.prepareStatement(code);
		s.setString(1, request.entry.value);
		s.setInt(2, request.entry.request_id);
	
		int rs = s.executeUpdate();
		s.close();
		if (rs == 0) {
			throw new Exception("Could not execute SQL code:"+code);
		}
		
		return;		
	}

	private void performDeleteAll(SessionAsyncRequest request, Connection cntx) throws Exception {
		String code = "DELETE FROM `"+ request.tb_prefix + "SESSIONS` WHERE `key` = ?";
		PreparedStatement s = cntx.prepareStatement(code);
		s.setString(1, request.entry.key);
		s.executeUpdate();
		s.close();

		return;		
	}


	private void performDelete(SessionAsyncRequest request, Connection cntx) throws Exception {
		String code;
		
		code = "DELETE FROM `"+ request.tb_prefix + "SESSIONS` WHERE `request_id` = ?";
		PreparedStatement s = cntx.prepareStatement(code);
		s.setInt(1, request.entry.request_id);

		int rs = s.executeUpdate();
		s.close();
		if (rs == 0) {
			throw new Exception("Could not execute SQL code:"+code+" where entry id:"+request.entry.request_id);
		}
	
		return;		
	}

}
