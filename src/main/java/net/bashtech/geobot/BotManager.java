package net.bashtech.geobot;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Pattern;

import net.bashtech.geobot.gui.BotGUI;
import net.bashtech.geobot.modules.BotModule;
import net.bashtech.geobot.modules.Logger;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;

public class BotManager {
	
	static BotManager instance;
	
	String nick;
	String server;
	int port;
	String password;
	String network;
	String localAddress;
	boolean publicJoin;
	boolean monitorPings;
	int pingInterval;
	boolean useGUI;
	BotGUI gui;
	Map<String,Channel> channelList;
	Set<String> admins;
	private Timer pjTimer;
	private PropertiesFile config;
	private Set<BotModule> modules;
	private Set<String> tagAdmins;
	List<String> emoteSet;
	List<Pattern> globalBannedWords;
	boolean verboseLogging;
	

	public BotManager(){
		BotManager.setInstance(this);
		channelList = new HashMap<String,Channel>();
		admins = new HashSet<String>();
		modules = new HashSet<BotModule>();
		tagAdmins = new HashSet<String>();
		emoteSet = new LinkedList<String>();
		globalBannedWords = new LinkedList<Pattern>();
		
		loadGlobalProfile();
		
		if(useGUI){
			gui = new BotGUI();
		}
			
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			System.out.println("DEBUG: Joining channel " + entry.getValue().getChannel());
			Bot tempBot = new Bot(this, server, port, entry.getValue());
			tempBot.joinChannel(entry.getValue().getChannel());
			entry.getValue().setBot(tempBot);
			System.out.println("DEBUG: Joined channel " + entry.getValue().getChannel());
		}

		Timer reconnectTimer = new Timer();
		reconnectTimer.scheduleAtFixedRate(new ReconnectTimer(channelList), 30 * 1000, 30 * 1000);
		System.out.println("Reconnect timer scheduled.");
		
		//this.autoPartandRejoin();
		
		// Load modules
		this.registerModule(new Logger());
	}
	

	private synchronized void loadGlobalProfile(){
		config = new PropertiesFile("global.properties");
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
		
		if(!config.keyExists("network")) {
			config.setString("network", "jtv");
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
		
		if(!config.keyExists("verboseLogging")) {
			config.setBoolean("verboseLogging", false);
		}
				
		nick = config.getString("nick");
		server = config.getString("server");
		network = config.getString("network");
		port = Integer.parseInt(config.getString("port"));
		localAddress = config.getString("localAddress");
		password = config.getString("password");
		useGUI = config.getBoolean("useGUI");
		monitorPings = config.getBoolean("monitorPings");
		pingInterval = config.getInt("pingInterval");
		publicJoin = config.getBoolean("publicJoin");
		verboseLogging = config.getBoolean("verboseLogging");
		
		for(String s:config.getString("channelList").split(",")) {
			System.out.println("DEBUG: Adding channel " + s);
			if(s.length() > 1){
				channelList.put(s.toLowerCase(),new Channel(s));
			}
		}
		
		for(String s:config.getString("adminList").split(",")) {
			//System.out.println("DEBUG: Adding admin " + s);
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
	
	public Channel getChannel(String channel){
		if(channelList.containsKey(channel.toLowerCase())){

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
	
	public boolean addChannel(String name, int mode){
		if(channelList.containsKey(name.toLowerCase())){
			System.out.println("INFO: Already in channel " + name);
			return false;
		}
		Channel tempChan = new Channel(name.toLowerCase(), mode);
		
		channelList.put(name.toLowerCase(), tempChan);

		System.out.println("DEBUG: Joining channel " + tempChan.getChannel());
		Bot tempBot = new Bot(this, server, port, tempChan);
		tempBot.joinChannel(tempChan.getChannel());
		tempChan.setBot(tempBot);
		System.out.println("DEBUG: Joined channel " + tempChan.getChannel());

		writeChannelList();
		return true;
	}

	public void removeChannel(String name){
		if(!channelList.containsKey(name.toLowerCase())){
			System.out.println("DEBUG: Not in channel " + name);
			return;
		}
		
		Channel tempChan = channelList.get(name.toLowerCase());
		Bot tempBot = tempChan.getBot();
		tempBot.partChannel(name.toLowerCase());
		tempBot.disconnect();
		tempBot.dispose();
		channelList.remove(name.toLowerCase());
		
		writeChannelList();
	}
	
	public boolean rejoinChannel(String name){
		if(!channelList.containsKey(name.toLowerCase())){
			System.out.println("DEBUG: Not in channel " + name);
			return false;
		}
		
		
		Channel tempChan = channelList.get(name.toLowerCase());
		try {
			tempChan.getBot().disconnect();
			tempChan.getBot().reconnect();
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		/*try {
			Thread.currentThread().sleep(20000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		tempChan.getBot().joinChannel(name);*/
		
		return true;
	}
	
	public synchronized void rejoinChannels(){
		System.out.println("DEBUG: Rejoining channels");
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			if((entry.getValue().getGiveaway() != null && entry.getValue().getGiveaway().getStatus()) || (entry.getValue().getPoll() != null && entry.getValue().getPoll().getStatus()))
				continue;
			
			System.out.println("DEBUG: Parting channel " + entry.getValue().getChannel());
			entry.getValue().getBot().partChannel(entry.getValue().getChannel());
			System.out.println("DEBUG: Joining channel " + entry.getValue().getChannel());
			entry.getValue().getBot().joinChannel(entry.getValue().getChannel());
		}

	}
	
	
	public synchronized void reconnectAllBotsSoft(){
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{
			Bot temp = entry.getValue().getBot();
			System.out.println("DEBUG: Disconnecting " + temp.getServer());
			temp.disconnect();
			System.out.println("DEBUG: " + temp.getServer() + " disconnected.");
		}
	}
	
	@SuppressWarnings("static-access")
	public synchronized void reconnectAllBotsHard(){
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{
			Bot temp = entry.getValue().getBot();
			System.out.println("DEBUG: Disconnecting " + temp.getServer());
			temp.disconnect();
			System.out.println("DEBUG: " + temp.getServer() + " disconnected.");
		}
		System.out.println("DEBUG: Waiting....");
		try {
			Thread.currentThread().sleep(20000);
		} catch (InterruptedException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		System.out.println("DEBUG: Done waiting. Kappa");
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{
			Bot temp = entry.getValue().getBot();
			
			if(temp.isConnected())
				continue;
			
			System.out.println("DEBUG: Reconnecting " + temp.getServer());
			try {
				temp.reconnect();
			} catch (NickAlreadyInUseException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			} catch (IrcException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			System.out.println("DEBUG: " + temp.getServer() + " reconnected.");
		}
		
		rejoinChannels();
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
		
		//int delay = 1800000;
		int delay = 3600000;
		
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
			entry.getValue().clearWarnings();
		
		pjTimer.scheduleAtFixedRate(new TimerTask()
	       {
	        public void run() {
	        	BotManager.this.rejoinChannels();
	        }
	      },delay,delay);

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

		//System.out.println("DEBUG: Sending global message: " + message);
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			Bot tempbot = (Bot)entry.getValue().getBot();

			for(String channel: tempbot.getChannels()){
				if(channelList.get(channel).getMode() == 0)
					continue;
				
				String globalMsg = "> Global: " + message + " (from " + sender + " to " + channel + ")";
				tempbot.sendMessage(channel, globalMsg);
			}
		}
	}
	
	public boolean isTagAdmin(String name){
		return tagAdmins.contains(name.toLowerCase());
	}
	
	public void addTagAdmin(String name){
		tagAdmins.add(name.toLowerCase());
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