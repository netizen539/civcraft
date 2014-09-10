package com.avrgaming.anticheat;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Random;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.ShortBufferException;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.plugin.messaging.PluginMessageListener;

import com.avrgaming.civcraft.config.CivSettings;
import com.avrgaming.civcraft.config.ConfigValidMod;
import com.avrgaming.civcraft.exception.CivException;
import com.avrgaming.civcraft.exception.InvalidConfiguration;
import com.avrgaming.civcraft.main.CivCraft;
import com.avrgaming.civcraft.main.CivGlobal;
import com.avrgaming.civcraft.main.CivLog;
import com.avrgaming.civcraft.main.CivMessage;
import com.avrgaming.civcraft.object.Resident;
import com.avrgaming.civcraft.threading.TaskMaster;
import com.avrgaming.civcraft.util.CivColor;
import com.avrgaming.civcraft.util.TimeTools;
import com.avrgaming.civcraft.war.War;
import com.avrgaming.civcraft.war.WarAntiCheat;

public class ACManager implements PluginMessageListener {

	static String versionNumber;
	static String key;
	static HashMap<String, Long> acceptedMods = new HashMap<String, Long>();
	static HashMap<String, Long> ivSpecs = new HashMap<String, Long>();
	static byte[] decrypted;
	static boolean enabled = true;
	
	public static void init() {
        Bukkit.getMessenger().registerOutgoingPluginChannel(CivCraft.getPlugin(), "CAC");
        Bukkit.getMessenger().registerIncomingPluginChannel(CivCraft.getPlugin(), "CAC", new ACManager());
        
        try {
			versionNumber = CivSettings.getString(CivSettings.nocheatConfig, "civcraft_ac_version");
			key = CivSettings.getString(CivSettings.nocheatConfig, "civcraft_ac_key");
			
			String enabledString = CivSettings.getString(CivSettings.nocheatConfig, "civcraft_ac_enabled");
			if (enabledString != null && enabledString.equalsIgnoreCase("false")) {
				enabled = false;
			}
			
			decrypted = new byte[32768];
		} catch (InvalidConfiguration e) {
			e.printStackTrace();
		}
	}
	
	private static void generateIvSpec(Player player, ByteBuffer buffer) {
		Random rand = new Random();
		long r = rand.nextLong();
		
		ivSpecs.put(player.getName(), Long.valueOf(r));
		buffer.putLong(r);
	}
	
	private static void writeKey(ByteBuffer buffer) {
		for (int i = 0; i < key.length(); i++) {
			buffer.putChar(key.charAt(i));
		}
	}
	
	
	/*
	 * Sends a CivCraftAC challenge packet to the player.
	 */
	public static void sendChallenge(Player player) {
		class SyncTask implements Runnable {
			String name;
			
			public SyncTask(String name) {
				this.name = name;
			}
			
			@Override
			public void run() {
				Player player;
				try {
					player = CivGlobal.getPlayer(name);
					ByteBuffer buffer = ByteBuffer.allocate(8 + (8*2));
					generateIvSpec(player, buffer);
					writeKey(buffer);
					
					player.sendPluginMessage(CivCraft.getPlugin(), "CAC", buffer.array());
				} catch (CivException e) {
				}
			}
		}
		
		TaskMaster.syncTask(new SyncTask(player.getName()), TimeTools.toTicks(3));
		
		if (War.isWarTime() && !player.isOp()) {
			
			class WarCheckTask implements Runnable {
				String name;
				
				public WarCheckTask(String name) {
					this.name = name;
				}
				
				@Override
				public void run() {
					try {
						Player player = CivGlobal.getPlayer(name);
						Resident resident = CivGlobal.getResident(player);
						
						if (!resident.isUsesAntiCheat()) {
							WarAntiCheat.onWarTimePlayerCheck(resident);
						}
						
					} catch (CivException e) {
					}
					
				}
			}
			
			TaskMaster.syncTask(new WarCheckTask(player.getName()), TimeTools.toTicks(30));
		}
		
		Resident resident = CivGlobal.getResident(player);
		if (resident != null && resident.isInsideArena()) {
			class ArenaCheckTask implements Runnable {
				String name;
				
				public ArenaCheckTask(String name) {
					this.name = name;
				}
				
				@Override
				public void run() {
					try {
						Player player = CivGlobal.getPlayer(name);
						Resident resident = CivGlobal.getResident(player);
						
						if (!resident.isUsesAntiCheat()) {
							
							/* Player is rejoining but doesnt have anti-cheat installed. */
							resident.teleportHome();
							resident.restoreInventory();
							resident.setInsideArena(false);
							resident.save();
							
							CivMessage.send(resident, CivColor.LightGray+"You've been teleported home since you cannot be inside an arena without anti-cheat.");
						}
						
					} catch (CivException e) {
					}
					
				}
				
			}
			
			TaskMaster.syncTask(new ArenaCheckTask(player.getName()), TimeTools.toTicks(30));
		}
	}

	
	@Override
	public void onPluginMessageReceived(String channel, Player player, byte[] messageRaw) {
		byte[] message = new byte[messageRaw.length-1];
		for (int i = 1; i < messageRaw.length; i++) {
			message[i-1] = messageRaw[i];
		}
		
//		// wrap key data in Key/IV specs to pass to cipher
//		for (byte b : ACManager.key.getBytes()) {
//			CivLog.debug("KeyByte:"+b);
//		}
		
		SecretKeySpec key = new SecretKeySpec(ACManager.key.getBytes(), "DES");
		Long iv = ivSpecs.get(player.getName());
		if (iv == null) {
			CivMessage.sendError(player, "Invalid Auth Message(0xFFFFEEC1)");
			return;
		}
		
		IvParameterSpec ivSpec = new IvParameterSpec(ByteBuffer.allocate(8).putLong(iv).array());
		// create the cipher with the algorithm you choose
		// see javadoc for Cipher class for more info, e.g.
		try {
			Cipher cipher = Cipher.getInstance("DES/CBC/PKCS5Padding");
			cipher.init(Cipher.DECRYPT_MODE, key, ivSpec);
			decrypted = new byte[cipher.getOutputSize(message.length)];
			int dec_len = cipher.update(message, 0, message.length, decrypted, 0);
			dec_len += cipher.doFinal(decrypted, dec_len);
		} catch (NoSuchAlgorithmException | NoSuchPaddingException | 
				InvalidKeyException | InvalidAlgorithmParameterException | 
				ShortBufferException | IllegalBlockSizeException | BadPaddingException e1) {
			e1.printStackTrace();
			CivMessage.sendError(player, "Invalid Auth Message(0xFFFFEEA1)");
			return;
		}
		
		String decoded = "";
		for (byte b : decrypted) {
			if (b != 0) {
				//CivLog.debug("byte:"+b+" char:"+((char)b));
				decoded += (char)b;
			}
		}
				
		try {
			validate(player, decoded);
			Resident resident = CivGlobal.getResident(player);
			if (resident != null) {
				resident.setUsesAntiCheat(true);
			}
			
		} catch (CivException e) {
			CivMessage.sendError(player, "[CivCraft Anti-Cheat] Couldn't Verify your client");
			CivMessage.sendError(player, e.getMessage());
			CivLog.info("Failed to validate player:"+player.getName()+" Message:"+e.getMessage());
			//e.printStackTrace();
			return;
		}
		
		
		CivMessage.sendSuccess(player, "You've been validated by CivCraft Anti-Cheat");
	}
	
	public void validate(Player player, String decodedMessage) throws CivException {
		String[] mods = decodedMessage.split(",");
		if (mods.length < 1) {
			throw new CivException("Invalid Auth Message(0xB4D132VF)");
		}
		
		String[] versionArray = mods[0].split(":");
		if (versionArray.length != 3) {
			throw new CivException("Invalid Auth Message(0xAF421FFF).");
		}
		
		boolean validTrap = Boolean.valueOf(versionArray[2]);
		if (validTrap) {
			try(PrintWriter out = new PrintWriter(new BufferedWriter(new FileWriter("anticheatbypassers.txt", true)))) {
			    out.println(player.getName());
			}catch (IOException e) {
			    //exception handling left as an exercise for the reader
			}
			return;
		}
		
		if (!versionArray[1].equals(versionNumber)) {
			throw new CivException("Old Version");
		}
		
		for (int i = 1; i < mods.length; i++) {
			String[] modArray = mods[i].split(":");
			if (modArray.length < 1) {
				throw new CivException("Invalid Auth Message(0xFFFFFFFF).");
			}
			
			ConfigValidMod mod = CivSettings.validMods.get(modArray[0]);
			if (mod == null) {
				throw new CivException("Unapproved Mod: "+modArray[0]+" ("+modArray[1]+")");
			} else {
				
				boolean valid = false;
				for (Long checksum : mod.checksums) {
					if (Long.valueOf(modArray[1]).equals(checksum)){
						valid = true;
						break;
					}
				}
				
				if (!valid) {
					if (modArray[0].equals("net.minecraft.client.main.Main")) {
						modArray[0] = "Minecraft Client";
					}
					throw new CivException(modArray[0]+" ("+modArray[1]+")"+" failed authorization check.");
				}
			}
		}
		
	}

	public static boolean isEnabled() {
		return enabled;
	}
	
	
}
