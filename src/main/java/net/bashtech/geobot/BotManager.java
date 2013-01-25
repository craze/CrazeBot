/*
 * Copyright 2012 Andrew Bashore
 * This file is part of GeoBot.
 * 
 * GeoBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * GeoBot is distributed in the hope that it will be useful
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GeoBot.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.bashtech.geobot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.regex.Pattern;

import net.bashtech.geobot.gui.BotGUI;
import net.bashtech.geobot.modules.BotModule;
import net.bashtech.geobot.modules.Logger;

public class BotManager {
	
	static BotManager instance;
	
	String nick;
	String server;
	int port;
	String password;
	String localAddress;
	boolean publicJoin;
	boolean monitorPings;
	int pingInterval;
	boolean useGUI;
	BotGUI gui;
	Map<String,Channel> channelList;
	Set<String> admins;
	private PropertiesFile config;
	private Set<BotModule> modules;
	private Set<String> tagAdmins;
	private Set<String> tagStaff;
	List<String> emoteSet;
	List<Pattern> globalBannedWords;
	boolean verboseLogging;
	public int senderInstances;
	ReceiverBot receiverBot;
	SenderBotBalancer sbb;
	public String commercialPasscode;
	
	private String _propertiesFile;
	

	public BotManager(String propertiesFile){
		BotManager.setInstance(this);
		_propertiesFile = propertiesFile;
		channelList = new HashMap<String,Channel>();
		admins = new HashSet<String>();
		modules = new HashSet<BotModule>();
		tagAdmins = new HashSet<String>();
		tagStaff = new HashSet<String>();
		emoteSet = new LinkedList<String>();
		globalBannedWords = new LinkedList<Pattern>();
		
		loadGlobalProfile();
		
		if(useGUI){
			gui = new BotGUI();
		}
		
		receiverBot = new ReceiverBot(server, port);
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			System.out.println("DEBUG: Joining channel " + entry.getValue().getChannel());
			receiverBot.joinChannel(entry.getValue().getChannel());
		}
		
		//Spinup senders
		sbb = new SenderBotBalancer();
		sbb.setInstanceNumber(senderInstances);
		sbb.spinUp();
		
		//Start timer to check for bot disconnects
		Timer reconnectTimer = new Timer();
		reconnectTimer.scheduleAtFixedRate(new ReconnectTimer(channelList), 30 * 1000, 30 * 1000);
		System.out.println("Reconnect timer scheduled.");
		
		// Load modules
		this.registerModule(new Logger());
	}
	

	private void loadGlobalProfile(){
		config = new PropertiesFile(_propertiesFile);
		
		System.out.println("DEBUG: Reading global file > " + _propertiesFile);
		try {
			config.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(!config.keyExists("nick")) {
			config.setString("nick", "");
		}
		if(!config.keyExists("server")) {
			config.setString("server", "");
		}
		
		if(!config.keyExists("password")) {
			config.setString("password", "");
		}
		
		if(!config.keyExists("port")) {
			config.setInt("port", 6667);
		}
		
		if(!config.keyExists("channelList")) {
			config.setString("channelList", "");
		}
		
		if(!config.keyExists("adminList")) {
			config.setString("adminList", "");
		}

		if(!config.keyExists("publicJoin")) {
			config.setBoolean("publicJoin", false);
		}
		
		if(!config.keyExists("monitorPings")) {
			config.setBoolean("monitorPings", false);
		}
		
		if(!config.keyExists("pingInterval")) {
			config.setInt("pingInterval", 350);
		}
		
		if(!config.keyExists("useGUI")) {
			config.setBoolean("useGUI", false);
		}
		
		if(!config.keyExists("localAddress")) {
			config.setString("localAddress", "");
		}
		
		if(!config.keyExists("commercialPasscode")) {
			config.setString("commercialPasscode", "");
		}
		
		if(!config.keyExists("verboseLogging")) {
			config.setBoolean("verboseLogging", false);
		}
		
		if(!config.keyExists("senderInstances")) {
			config.setInt("senderInstances", 10);
		}
				
		nick = config.getString("nick");
		server = config.getString("server");
		port = Integer.parseInt(config.getString("port"));
		localAddress = config.getString("localAddress");
		password = config.getString("password");
		useGUI = config.getBoolean("useGUI");
		monitorPings = config.getBoolean("monitorPings");
		pingInterval = config.getInt("pingInterval");
		publicJoin = config.getBoolean("publicJoin");
		verboseLogging = config.getBoolean("verboseLogging");
		senderInstances = config.getInt("senderInstances");
		commercialPasscode = config.getString("commercialPasscode");
		
		for(String s:config.getString("channelList").split(",")) {
			System.out.println("DEBUG: Adding channel " + s);
			if(s.length() > 1){
				channelList.put(s.toLowerCase(),new Channel(s));
			}
		}
		
		for(String s:config.getString("adminList").split(",")) {
			if(s.length() > 1){
				admins.add(s.toLowerCase());
			}
		}
		
		loadEmotes();
		loadGlobalBannedWords();
		
		if(server.length() < 1){
			System.exit(1);
		}
	}
	
	public synchronized Channel getChannel(String channel){
		if(channelList.containsKey(channel.toLowerCase())){

			return channelList.get(channel.toLowerCase());
		}else{
			return null;
		}
	}
	
	public synchronized boolean addChannel(String name, int mode){
		if(channelList.containsKey(name.toLowerCase())){
			System.out.println("INFO: Already in channel " + name);
			return false;
		}
		Channel tempChan = new Channel(name.toLowerCase(), mode);
		
		channelList.put(name.toLowerCase(), tempChan);

		System.out.println("DEBUG: Joining channel " + tempChan.getChannel());
		receiverBot.joinChannel(tempChan.getChannel());

		System.out.println("DEBUG: Joined channel " + tempChan.getChannel());

		writeChannelList();
		return true;
	}

	public synchronized void removeChannel(String name){
		if(!channelList.containsKey(name.toLowerCase())){
			System.out.println("DEBUG: Not in channel " + name);
			return;
		}
		
		Channel tempChan = channelList.get(name.toLowerCase());
		receiverBot.partChannel(name.toLowerCase());

		channelList.remove(name.toLowerCase());
		
		writeChannelList();
	}
	
	public synchronized void reloadChannel(String name){
		if(!channelList.containsKey(name.toLowerCase())){
			System.out.println("DEBUG: Not in channel " + name);
			return;
		}
		
		channelList.get(name.toLowerCase()).reload();
	}
	
	public boolean rejoinChannel(String name){
		if(!channelList.containsKey(name.toLowerCase())){
			System.out.println("DEBUG: Not in channel " + name);
			return false;
		}
		
		Channel tempChan = channelList.get(name.toLowerCase());
		receiverBot.partChannel(tempChan.getChannel());
		receiverBot.joinChannel(tempChan.getChannel());

			
		return true;
	}

	
	public synchronized void reconnectAllBotsSoft(){
		receiverBot.disconnect();
	}
	
	
	private synchronized void writeChannelList(){
		String channelString = "";
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			channelString += entry.getKey() + ","; 
		}
		
		config.setString("channelList", channelString);
	}
	
	public static void setInstance(BotManager bm){
		if(instance == null){
			instance = bm;
		}
	}
	
	public static BotManager getInstance(){
		return instance;
	}
	
	public void registerModule(BotModule module){
		modules.add(module);
	}
	
	public Set<BotModule> getModules(){
		return modules;
	}
	
	public BotGUI getGUI(){
		return gui;
	}
	
	public String getLocalAddress(){
		return localAddress;
	}
	
	public void sendGlobal(String message, String sender){
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			Channel tempChannel = (Channel)entry.getValue();
			if(tempChannel.getMode() == 0)
				continue;

			String globalMsg = "> Global: " + message + " (from " + sender + " to " + tempChannel.getChannel() + ")";
			SenderBotBalancer.getInstance().sendMessage(tempChannel.getChannel(), globalMsg);			
		}
	}
	
	public boolean isAdmin(String nick){
		if(admins.contains(nick.toLowerCase()))
			return true;
		else
			return false;
	}
	
	public boolean isTagAdmin(String name){
		return tagAdmins.contains(name.toLowerCase());
	}
	
	public boolean isTagStaff(String name){
		return tagStaff.contains(name.toLowerCase());
	}
	
	public void addTagAdmin(String name){
		tagAdmins.add(name.toLowerCase());
	}
	
	public void addTagStaff(String name){
		tagStaff.add(name.toLowerCase());
	}
	
	private void loadEmotes(){
		emoteSet.clear();
		File f = new File("emotes.cfg");
		if(!f.exists())
			try {
				f.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				Scanner in = new Scanner(f, "UTF-8");
				
				while (in.hasNextLine()){
					emoteSet.add(in.nextLine().trim());
			      }
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}
	
	private void loadGlobalBannedWords(){
		File f = new File("globalbannedwords.cfg");
		if(!f.exists())
			try {
				f.createNewFile();
			} catch (IOException e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			try {
				Scanner in = new Scanner(f, "UTF-8");
				
				while (in.hasNextLine()){
					String line = ".*" + Pattern.quote(in.nextLine().trim().toLowerCase()) + ".*";
					System.out.println(line);
					Pattern tempP = Pattern.compile(line);
					globalBannedWords.add(tempP);
			      }
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

	}
	
	
}