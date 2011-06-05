package net.bashtech.geobot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;

public class BotManager {
	
	String nick;
	String server;
	int port;
	String password;
	String network;
	
	
	Map<String,GeoBot> botList;
	Map<String,Channel> channelList;
	
	Set<String> admins;
	
	private Timer pjTimer;
	
	private PropertiesFile config;

	public BotManager(){
		botList = new HashMap<String,GeoBot>();
		channelList = new HashMap<String,Channel>();
		admins = new HashSet<String>();
		
		loadGlobalProfile();
		
		botList.put(server, new GeoBot(this, server, port));
		
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			System.out.println("DEBUG: Checking for bot on server " + entry.getValue().getServer());
			if(botList.containsKey(entry.getValue().getServer())){
				System.out.println("DEBUG: Joining channel " + entry.getValue().getChannel() + " NO CREATE.");
				botList.get(entry.getValue().getServer()).joinChannel(entry.getValue().getChannel());
				System.out.println("DEBUG: Joined channel " + entry.getValue().getChannel());

			}else{
				System.out.println("DEBUG: Joining channel " + entry.getValue().getChannel() + " CREATE");
				botList.put(entry.getValue().getServer(), new GeoBot(this, entry.getValue().getServer(), entry.getValue().getPort()));
				botList.get(entry.getValue().getServer()).joinChannel(entry.getValue().getChannel());
				System.out.println("DEBUG: Joined channel " + entry.getValue().getChannel());
			}
		}

		
		
		
		Timer reconnectTimer = new Timer();
		reconnectTimer.scheduleAtFixedRate(new ReconnectTimer(botList), 30 * 1000, 30 * 1000);
		System.out.println("Reconnect timer scheduled.");
		
		this.autoPartandRejoin();
	}
	
	private synchronized void loadGlobalProfile(){
		config = new PropertiesFile("global.properties");
		try {
			config.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(!config.keyExists("nick")) {
			config.setString("nick", "ackbot");
		}
		if(!config.keyExists("server")) {
			config.setString("server", "chat.ngame.tv");
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
		
		if(!config.keyExists("network")) {
			config.setString("network", "");
		}
		
		nick = config.getString("nick");
		server = config.getString("server");
		network = config.getString("network");
		port = Integer.parseInt(config.getString("port"));
		
		password = config.getString("password");
		
		for(String s:config.getString("channelList").split(",")) {
			System.out.println("DEBUG: Adding channel " + s);
			if(s.length() > 1){
				channelList.put(s.toLowerCase(),new Channel(s));
			}
		}
		
		for(String s:config.getString("adminList").split(",")) {
			System.out.println("DEBUG: Adding admin " + s);
			if(s.length() > 1){
				admins.add(s.toLowerCase());
			}
		}
	}
	
	public Channel getChannel(String channel){
		if(channelList.containsKey(channel.toLowerCase())){
			System.out.println("DEBUG: Matched channel");

			return channelList.get(channel.toLowerCase());
		}else{
			return null;
		}
	}
	
	public boolean isAdmin(String nick){
		if(admins.contains(nick.toLowerCase()))
			return true;
		else
			return false;
	}
	
	public void addChannel(String name, String server2) throws NickAlreadyInUseException, IOException, IrcException{
		if(channelList.containsKey(name.toLowerCase())){
			System.out.println("INFO: All ready in channel " + name);
			return;
		}
		Channel tempChan = new Channel(name,server2);
		
		channelList.put(name, tempChan);

		
		if(botList.containsKey(server2)){
			System.out.println("DEBUG: Joining channel " + tempChan.getChannel() + " NO CREATE.");
			botList.get(server2).joinChannel(tempChan.getChannel());
			System.out.println("DEBUG: Joined channel " + tempChan.getChannel());

		}else{
			System.out.println("DEBUG: Joining channel " + tempChan.getChannel() + " CREATE");
			botList.put(server2, new GeoBot(this,tempChan.getServer(), tempChan.getPort()));
			botList.get(server2).joinChannel(tempChan.getChannel());
			System.out.println("DEBUG: Joined channel " + tempChan.getChannel());
		}
		
		
		writeChannelList();
	}

	public void removeChannel(String name){
		if(!channelList.containsKey(name.toLowerCase())){
			System.out.println("INFO: Not in channel " + name);
			return;
		}
		
		Channel tempChan = channelList.get(name.toLowerCase());
		
		if(botList.containsKey(tempChan.getServer())){
			GeoBot tempBot = botList.get(tempChan.getServer());
			tempBot.partChannel(name);
		}
		
		channelList.remove(name.toLowerCase());
		
		
		writeChannelList();
	}
	
	public synchronized void rejoinChannels(){
		System.out.println("INFO: Rejoining channels");
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			if((entry.getValue().getGiveaway() != null && entry.getValue().getGiveaway().getStatus()) || (entry.getValue().getPoll() != null && entry.getValue().getPoll().getStatus()))
				continue;
			
			System.out.println("INFO: Parting channel " + entry.getKey());
			botList.get(entry.getValue().getServer()).partChannel(entry.getKey());
			System.out.println("INFO: Joining channel " + entry.getKey());
			botList.get(entry.getValue().getServer()).joinChannel(entry.getKey());
		}

	}
	
	private synchronized void writeChannelList(){
		String channelString = "";
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			channelString += entry.getKey() + ","; 
		}
		
		config.setString("channelList", channelString);
	}
	
	
	private void autoPartandRejoin(){
		
		pjTimer = new Timer();
		
		int delay = 1800000;
		
		pjTimer.scheduleAtFixedRate(new TimerTask()
	       {
	        public void run() {
	        	BotManager.this.rejoinChannels();
	        }
	      },delay,delay);

	}
	
}