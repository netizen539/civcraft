package com.avrgaming.civregister;

import java.sql.SQLException;

import org.bukkit.plugin.java.JavaPlugin;

public class CivRegister extends JavaPlugin {

	@Override
	public void onEnable() {
		/* Load configuration */
		loadConfig();
		
		/* Initialize SQL */
		try {
			SQL.init(this);
		} catch (SQLException e) {
			e.printStackTrace();
		}
		
		/* Register commands. */
		getCommand("reg").setExecutor(new RegCommand());
		getCommand("resetpassword").setExecutor(new ResetPasswordCommand());
	}
	
	@Override
	public void onDisable() {
		
	}
	
	public void loadConfig() {
		this.saveDefaultConfig();
	}
	
}
