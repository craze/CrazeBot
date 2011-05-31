package net.bashtech.geobot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.Timer;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;

public class BotManager {
	
	String nick;
	String server;
	int port;
	String password;
	
	ArrayList<GeoBot> botList;
	Map<String,Channel> channelList;
	
	Set<String> admins;
	
	private PropertiesFile config;

	public BotManager(){
		botList = new ArrayList<GeoBot>();
		channelList = new HashMap<String,Channel>();
		admins = new HashSet<String>();
		
		loadGlobalProfile();
		
		GeoBot temp = new GeoBot(this);
		
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{
			System.out.println("DEBUG: Joining channel " + entry.getValue().getChannel());
			temp.joinChannel(entry.getValue().getChannel());
			System.out.println("DEBUG: Joined channel " + entry.getValue().getChannel());

		}

		
		botList.add(temp);
		
		//Start reconnect timer
		
		//Timer reconnectTimer = new Timer();
		//reconnectTimer.scheduleAtFixedRate(new ReconnectTimer(botList), 30 * 1000, 30 * 1000);
		//System.out.println("Reconnect timer scheduled.");
	}
	
	private void loadGlobalProfile(){
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
		
		nick = config.getString("nick");
		server = config.getString("server");
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
	
	public void addChannel(String name) throws NickAlreadyInUseException, IOException, IrcException{
		if(channelList.containsKey(name.toLowerCase())){
			System.out.println("INFO: All ready in channel " + name);
			return;
		}
		
		botList.get(0).joinChannel(name);
		
		channelList.put(name, new Channel(name));
		
		writeChannelList();
	}

	public void removeChannel(String name){
		if(!channelList.containsKey(name.toLowerCase())){
			System.out.println("INFO: Not in channel " + name);
			return;
		}
		
		botList.get(0).partChannel(name);
		
		channelList.remove(name.toLowerCase());
		
		writeChannelList();
	}
	
	public void rejoinChannels(){
		String[] inChannels = botList.get(0).getChannels();
		
		for(String channel: inChannels){
			System.out.println("INFO: Parting channel " + channel);
			botList.get(0).partChannel(channel);
		}
		
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			System.out.println("INFO: Joining channel " + entry.getKey());
			botList.get(0).joinChannel(entry.getKey());
		}
	}
	
	private void writeChannelList(){
		String channelString = "";
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			channelString += entry.getKey() + ","; 
		}
		
		config.setString("channelList", channelString);
	}
	
}