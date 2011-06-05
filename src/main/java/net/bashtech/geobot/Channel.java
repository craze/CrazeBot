package net.bashtech.geobot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;


public class Channel {
	public PropertiesFile config;
	
	private String server;
	private String channel;
	private int port = 6667;
	
	private HashMap<String, String> commands = new HashMap<String, String>();
	
	private boolean filterCaps;
	private int filterCapsLimit;
	
	private boolean filterLinks;
	
	private String topic;
	
	private ArrayList<String> regulars = new ArrayList<String>();
	
	private ArrayList<String> moderators = new ArrayList<String>();
	
	private ArrayList<String> permittedUsers = new ArrayList<String>();
	
	//Checks for disabled features.
	public boolean useTopic = true;
	public boolean useFilters = true;
	
	public boolean publicCommands = true;
	
	private Poll currentPoll;
	
	private Giveaway currentGiveaway;
	
	private boolean enableThrow;
	
	public Channel(String name){
		config = new PropertiesFile(name+".properties");
		loadProperties(name);
	}
	
	public Channel(String name, String server2){
		config = new PropertiesFile(name+".properties");
		server = server2;
		loadProperties(name);
	}

	//#############################################################

	
	public String getServer() {
		return server;
	}

	public int getPort() {
		return port;
	}

	public String getChannel() {
		return channel;
	}
	
	
	//##############################################################
	
	public String getCommand(String key){
		if(commands.containsKey(key)){
			return commands.get(key);
		}else{
			return "invalid";
		}
	}
	
	public void setCommand(String key, String command){
		if(commands.containsKey(key)){
			commands.remove(key);
			commands.put(key, command);
		}else{
			commands.put(key, command);
		}
		
		String commandsKey = "";
		String commandsValue = "";
		
		Iterator itr = commands.entrySet().iterator();
		
		while(itr.hasNext()){
			Map.Entry pairs = (Map.Entry)itr.next();
			commandsKey += pairs.getKey() + ",";
			commandsValue += pairs.getValue() + ",,";
		}
		
		config.setString("commandsKey", commandsKey);
		config.setString("commandsValue", commandsValue);

	}
	
	public void removeCommand(String key){
		if(commands.containsKey(key)){
			commands.remove(key);
			
			String commandsKey = "";
			String commandsValue = "";
			
			Iterator itr = commands.entrySet().iterator();
			
			while(itr.hasNext()){
				Map.Entry pairs = (Map.Entry)itr.next();
				commandsKey += pairs.getKey() + ",";
				commandsValue += pairs.getValue() + ",,";
			}
			
			config.setString("commandsKey", commandsKey);
			config.setString("commandsValue", commandsValue);
		}

	}
	
	public String getCommandList(){
		String commandKeys = "";
		
		Iterator itr = commands.entrySet().iterator();
		
		while(itr.hasNext()){
			Map.Entry pairs = (Map.Entry)itr.next();
			commandKeys += pairs.getKey() + ", ";
		}
		
		return commandKeys;
		
	}

	//#####################################################
	
	public String getTopic(){
		return topic;
	}
	
	public void setTopic(String s){
		topic = s;
		config.setString("topic", topic);
	}
	
	public boolean getFilterCaps(){
		return filterCaps;
	}
	
	public void setFilterCaps(boolean caps){
		filterCaps = caps;
		config.setBoolean("filterCaps", filterCaps);
	}
	
	public int getFilterCapsLimit(){
		return filterCapsLimit;
	}
	
	public void setFilterCapsLimit(int limit){
		filterCapsLimit = limit;
		config.setInt("filterCapsLimit", filterCapsLimit);
	}
	
	public void setFilterLinks(boolean links){
		filterLinks = links;
		config.setBoolean("filterLinks", links);
	}
	
	public boolean getFilterLinks(){
		return filterLinks;
	}
	
	//###################################################
	
	public boolean isRegular(String name){
		//boolean flag = false;
		synchronized (regulars) { 
			for(String s:regulars){
				if(s.equalsIgnoreCase(name)){
					return true;
					//flag = true;
				}
			}
		}
		//return flag;
		return false;
	}
	
	public void addRegular(String name){
		synchronized (regulars) { 
			regulars.add(name);
		}
		
		String regularsString = "";
		
		synchronized (regulars) { 
			for(String s:regulars){
				regularsString += s + ",";
			}
		}
		
		config.setString("regulars", regularsString);
	}
	
	public void removeRegular(String name){
		synchronized (regulars) { 
			for(int c = 0; c < regulars.size(); c++){
				if(regulars.get(c).equalsIgnoreCase(name)){
					regulars.remove(c);
				}

			}

		}		
		String regularsString = "";
		
		synchronized (regulars) { 
			for(String s:regulars){
				regularsString += s + ",";
			}
		}
		
		config.setString("regulars", regularsString);
	}
	
	public void permitUser(String name){
		synchronized (permittedUsers) { 
			for(String s:permittedUsers){
				if(s.equalsIgnoreCase(name)){
					return;
				}
			}
		}
		
		synchronized (permittedUsers) { 
			permittedUsers.add(name);
		}
	}
	
	public boolean linkPermissionCheck(String name){
		
		if(this.isRegular(name)){
			return true;
		}
		
		synchronized (permittedUsers) {
			for(int c = 0; c < permittedUsers.size(); c++){
				if(permittedUsers.get(c).equalsIgnoreCase(name)){
					permittedUsers.remove(c);
					return true;
				}

			}
//			for(String s:permittedUsers){
//				if(s.equalsIgnoreCase(name)){
//					permittedUsers.remove(s);
//					return true;
//				}
//			}
		}
		
		return false;
	}
	
	//###################################################
	
	public boolean isModerator(String name){		
		synchronized (moderators) { 
			for(String s:moderators){
				if(s.equalsIgnoreCase(name)){
					return true;
				}
			}
		}
		
		return false;
	}
	
	public void addModerator(String name){
		synchronized (moderators) {
			moderators.add(name);
		}
		
		String moderatorsString = "";
		
		synchronized (moderators) { 
			for(String s:moderators){
				moderatorsString += s + ",";
			}
		}
		
		config.setString("moderators", moderatorsString);
	}
	
	public void removeModerator(String name){
		synchronized (moderators) {
			for(int c = 0; c < moderators.size(); c++){
				if(moderators.get(c).equalsIgnoreCase(name)){
					moderators.remove(c);
				}

			}
//			for(String s:moderators){
//				if(s.equalsIgnoreCase(name)){
//					moderators.remove(s);
//				}
//			}
		}
		
		String moderatorsString = "";
		
		synchronized (moderators) { 
			for(String s:moderators){
				moderatorsString += s + ",";
			}
		}
		
		config.setString("moderators", moderatorsString);
	}
	
	// ##################################################
	
	public void setTopicFeature(boolean setting){
		this.useTopic = setting;
		config.setBoolean("useTopic", this.useTopic);

	}
	
	public void setFiltersFeature(boolean setting){
		this.useFilters = setting;
		config.setBoolean("useFilters", this.useFilters);
	}
	
	public Poll getPoll(){
		return currentPoll;
	}
	
	public void setPoll(Poll _poll){
		currentPoll = _poll;
	}
	
	public Giveaway getGiveaway(){
		return currentGiveaway;
	}
	
	public void setGiveaway(Giveaway _gw){
		currentGiveaway = _gw;
	}
	
	public boolean checkThrow(){
		return enableThrow;
	}
	
	public void setThrow(boolean setting){
		this.enableThrow = setting;
		config.setBoolean("enableThrow", this.enableThrow);
	}
	
	// ##################################################
	
	private void loadProperties(String name){
		try {
			config.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		System.out.println("DEBUG: Setting server " + server);
		if(!config.keyExists("server")) {
			config.setString("server", server);
			
		}
		
		if(!config.keyExists("port")) {
			config.setString("port", "6667");
		}
		
		if(!config.keyExists("channel")) {
			config.setString("channel", name);
		}
		
		if(!config.keyExists("filterCaps")) {
			config.setBoolean("filterCaps", false);
		}
		
		if(!config.keyExists("filterCapsLimit")) {
			config.setInt("filterCapsLimit", 4);
		}
		
		if(!config.keyExists("filterLinks")) {
			config.setBoolean("filterLinks", false);
		}
		
		if(!config.keyExists("topic")) {
			config.setString("topic", "GiantZombies will rule the world.");
		}
		
		if(!config.keyExists("commandsKey")) {
			config.setString("commandsKey", "");
		}
		
		if(!config.keyExists("commandsValue")) {
			config.setString("commandsValue", "");
		}
		
		if(!config.keyExists("regulars")) {
			config.setString("regulars", "");
		}
		
		if(!config.keyExists("moderators")) {
			config.setString("moderators", name.substring(1, name.length()) + ",");
		}
		
		if(!config.keyExists("useTopic")) {
			config.setBoolean("useTopic", false);
		}
		
		if(!config.keyExists("useFilters")) {
			config.setBoolean("useFilters", false);
		}
		
		if(!config.keyExists("enableThrow")) {
			config.setBoolean("enableThrow", true);
		}
		
		server = config.getString("server");
		channel = config.getString("channel");
		port = Integer.parseInt(config.getString("port"));
		
		filterCaps = Boolean.parseBoolean(config.getString("filterCaps"));
		filterCapsLimit = Integer.parseInt(config.getString("filterCapsLimit"));

		filterLinks = Boolean.parseBoolean(config.getString("filterLinks"));
		
		topic  = config.getString("topic");
		
		useTopic = Boolean.parseBoolean(config.getString("useTopic"));
		useFilters = Boolean.parseBoolean(config.getString("useFilters"));
		enableThrow = Boolean.parseBoolean(config.getString("enableThrow"));

		
		String[] commandsKey = config.getString("commandsKey").split(",");
		String[] commandsValue = config.getString("commandsValue").split(",,");

		for(int i = 0; i < commandsKey.length; i++){
			if(commandsKey[i].length() > 1){
				commands.put(commandsKey[i], commandsValue[i]);
			}
		}
		
		String[] regularsRaw = config.getString("regulars").split(",");
		
		synchronized (regulars) {
			for(int i = 0; i < regularsRaw.length; i++){
				if(regularsRaw[i].length() > 1){
					regulars.add(regularsRaw[i]);
				}
			}
		}
		
		String[] moderatorsRaw = config.getString("moderators").split(",");
		
		synchronized (moderators) {
			for(int i = 0; i < moderatorsRaw.length; i++){
				if(moderatorsRaw[i].length() > 1){
					moderators.add(moderatorsRaw[i]);
				}
			}
		}
	}

}
