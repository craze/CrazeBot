package net.bashtech.geobot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class BotManager {
	
	String nick;
	String server;
	int port;
	String password;
	
	ArrayList<GeoBot> botList;
	Map<String,Channel> channelList;
	
	private PropertiesFile config;

	public BotManager(){
		botList = new ArrayList<GeoBot>();
		channelList = new HashMap<String,Channel>();
		
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
	}
	
	public Channel getChannel(String channel){
		if(channelList.containsKey(channel.toLowerCase())){
			System.out.println("DEBUG: Matched channel");

			return channelList.get(channel.toLowerCase());
		}else{
			return null;
		}
	}
}
