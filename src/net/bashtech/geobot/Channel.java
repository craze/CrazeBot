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
	
	private ArrayList<String> permittedUsers = new ArrayList<String>();

	
	public Channel(String name){
		config = new PropertiesFile(name+".properties");
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
			commandsValue += pairs.getValue() + ",";
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
				commandsValue += pairs.getValue() + ",";
			}
			
			config.setString("commandsKey", commandsKey);
			config.setString("commandsValue", commandsValue);
		}

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
		
		for(String s:regulars){
			if(s.equalsIgnoreCase(name)){
				return true;
			}
		}
		
		return false;
	}
	
	public void addRegular(String name){
		regulars.add(name);
		
		String regularsString = "";
		
		for(String s:regulars){
			regularsString += s + ",";
		}
		
		config.setString("regulars", regularsString);
	}
	
	public void removeRegular(String name){
		for(String s:regulars){
			if(s.equalsIgnoreCase(name)){
				regulars.remove(s);
			}
		}
		
		String regularsString = "";
		
		for(String s:regulars){
			regularsString += s + ",";
		}
		
		config.setString("regulars", regularsString);
	}
	
	public void permitUser(String name){
		
		for(String s:permittedUsers){
			if(s.equalsIgnoreCase(name)){
				return;
			}
		}
		
		permittedUsers.add(name);
	}
	
	public boolean linkPermissionCheck(String name){
		
		if(this.isRegular(name)){
			return true;
		}
		
		for(String s:permittedUsers){
			if(s.equalsIgnoreCase(name)){
				permittedUsers.remove(s);
				return true;
			}
		}
		
		return false;
	}
	
	//###################################################
	
	private void loadProperties(String name){
		try {
			config.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(!config.keyExists("server")) {
			config.setString("server", name.substring(1, name.length())+".jtvirc.com");
			
		}
		
		if(!config.keyExists("port")) {
			config.setInt("port", 6667);
		}
		
		if(!config.keyExists("channel")) {
			config.setString("channel", name);
		}
		
		if(!config.keyExists("filterCaps")) {
			config.setBoolean("filterCaps", true);
		}
		
		if(!config.keyExists("filterCapsLimit")) {
			config.setInt("filterCapsLimit", 4);
		}
		
		if(!config.keyExists("filterLinks")) {
			config.setBoolean("filterLinks", true);
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
		
		server = config.getString("server");
		channel = config.getString("channel");
		port = Integer.parseInt(config.getString("port"));
		
		filterCaps = Boolean.parseBoolean(config.getString("filterCaps"));
		filterCapsLimit = Integer.parseInt(config.getString("filterCapsLimit"));

		filterLinks = Boolean.parseBoolean(config.getString("filterLinks"));
		
		topic  = config.getString("topic");
		
		String[] commandsKey = config.getString("commandsKey").split(",");
		String[] commandsValue = config.getString("commandsValue").split(",");
		
		for(int i = 0; i < commandsKey.length; i++){
			if(commandsKey[i].length() > 1){
				commands.put(commandsKey[i], commandsValue[i]);
			}
		}
		
		String[] regularsRaw = config.getString("regulars").split(",");
		
		for(int i = 0; i < regularsRaw.length; i++){
			if(regularsRaw[i].length() > 1){
				regulars.add(regularsRaw[i]);
			}
		}
		
	}

}
