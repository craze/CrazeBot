package net.bashtech.geobot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;


public class Channel {
	public PropertiesFile config;
	
	private String server;
	private String channel;
	private int port = 6667;
	
	private HashMap<String, String> commands = new HashMap<String, String>();
	
	private boolean filterCaps;
	private int filterCapsPercent;
	private int filterCapsMinCharacters;
	private int filterCapsMinCapitals;
	
	private boolean filterLinks;
	
	private String topic;
	private int topicTime;
	
	private Set<String> regulars = new HashSet<String>();
	
	private Set<String> moderators = new HashSet<String>();
	
	private Set<String> permittedUsers = new HashSet<String>();
	
	private ArrayList<String> permittedDomains = new ArrayList<String>();
	
	public boolean useTopic = true;
	public boolean useFilters = true;
	
	public boolean publicCommands = true;
	
	private Poll currentPoll;
	
	private Giveaway currentGiveaway;
	
	private boolean enableThrow;
	
	private boolean signKicks;
	
	private boolean announceJoinParts; 
	
	private String lastfm;
	
	private String steamID;
	
	private int mode; //0: Admin/owner only; 1: Mod Only; 2: Everyone
	
	private int bulletInt;
	private char bullet[] = {'>','+', '-', '~'};

		
	public Channel(String name){
		config = new PropertiesFile(name+".properties");
		server = null;
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
		topicTime = (int) (System.currentTimeMillis()/1000);
		config.setInt("topicTime", topicTime);
	}
	
	public String getTopicTime(){
		int difference = (int) (System.currentTimeMillis()/1000) - topicTime;
		String returnString = "";
		
		if(difference >= 86400){
			int days = (int)(difference / 86400);
			returnString += days + "d ";
			difference -= days * 86400;
		}
		if(difference >= 3600){
			int hours = (int)(difference / 3600 );
			returnString += hours + "h ";
			difference -= hours * 3600;
		}
	
			int seconds = (int)(difference / 60 );
			returnString += seconds + "m";
			difference -= seconds * 60;
		
		
		return returnString;
	}
	
	public boolean getFilterCaps(){
		return filterCaps;
	}
		
	public int getfilterCapsPercent(){
		return filterCapsPercent;
	}
	
	public int getfilterCapsMinCharacters(){
		return filterCapsMinCharacters;
	}
	
	public int getfilterCapsMinCapitals(){
		return filterCapsMinCapitals;
	}
		
	public void setFilterCaps(boolean caps){
		filterCaps = caps;
		config.setBoolean("filterCaps", filterCaps);
	}
	
	public void setfilterCapsPercent(int caps){
		filterCapsPercent = caps;
		config.setInt("filterCapsPercent", filterCapsPercent);
	}
	
	public void setfilterCapsMinCharacters(int caps){
		filterCapsMinCharacters = caps;
		config.setInt("filterCapsMinCharacters", filterCapsMinCharacters);
	}
	
	public void setfilterCapsMinCapitals(int caps){
		filterCapsMinCapitals = caps;
		config.setInt("filterCapsMinCapitals", filterCapsMinCapitals);
	}
	
	public void setFilterLinks(boolean links){
		filterLinks = links;
		config.setBoolean("filterLinks", links);
	}
	
	public boolean getFilterLinks(){
		return filterLinks;
	}
	
	public void setAnnounceJoinParts(boolean bol){
		announceJoinParts = bol;
		config.setBoolean("announceJoinParts", bol);
	}
	
	public boolean getAnnounceJoinParts(){
		return announceJoinParts;
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
			regulars.add(name.toLowerCase());
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
			if(regulars.contains(name.toLowerCase()))
				regulars.remove(name.toLowerCase());
		}		
		String regularsString = "";
		
		synchronized (regulars) { 
			for(String s:regulars){
				regularsString += s + ",";
			}
		}
		
		config.setString("regulars", regularsString);
	}
	
	public Set<String> getRegulars(){
		return regulars;
	}
	
	public void permitUser(String name){
		synchronized (permittedUsers) { 
			if(permittedUsers.contains(name.toLowerCase()))
				return;
		}
		
		synchronized (permittedUsers) { 
			permittedUsers.add(name.toLowerCase());
		}
	}
	
	public boolean linkPermissionCheck(String name){
		
		if(this.isRegular(name)){
			return true;
		}
		
		synchronized (permittedUsers) {
			if(permittedUsers.contains(name.toLowerCase())){
				permittedUsers.remove(name.toLowerCase());
				return true;
			}
		}
		
		return false;
	}
	
	//###################################################
	
	public boolean isModerator(String name){		
		synchronized (moderators) { 
			if(moderators.contains(name.toLowerCase()))
				return true;
		}
		
		return false;
	}
	
	public void addModerator(String name){
		synchronized (moderators) {
			moderators.add(name.toLowerCase());
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
			if(moderators.contains(name.toLowerCase()))
				moderators.remove(name.toLowerCase());
		}
		
		String moderatorsString = "";
		
		synchronized (moderators) { 
			for(String s:moderators){
				moderatorsString += s + ",";
			}
		}
		
		config.setString("moderators", moderatorsString);
	}
	
	public Set<String> getModerators(){
		return moderators;
	}
	
	//###################################################
	
//	public boolean isPermittedDomain(String name){		
//
//	}
	
	public void addPermittedDomain(String name){
		synchronized (permittedDomains) {
			permittedDomains.add(name.toLowerCase());
		}
		
		String permittedDomainsString = "";
		
		synchronized (permittedDomains) { 
			for(String s:permittedDomains){
				permittedDomainsString += s + ",";
			}
		}
		
		config.setString("permittedDomains", permittedDomainsString);
	}
	
	public void removePermittedDomain(String name){
		synchronized (permittedDomains) {
			for(int i=0;i<permittedDomains.size();i++){
				if(permittedDomains.get(i).equalsIgnoreCase(name)){
					permittedDomains.remove(i);
				}
			}
		}
		
		String permittedDomainsString = "";
		
		synchronized (permittedDomains) { 
			for(String s:permittedDomains){
				permittedDomainsString += s + ",";
			}
		}
		
		config.setString("permittedDomains", permittedDomainsString);
	}
	
	public boolean isDomainPermitted(String domain){
		for(String d:permittedDomains){
			if(d.equalsIgnoreCase(domain)){
				return true;
			}
		}
		
		return false;
	}
	
	public ArrayList<String> getpermittedDomains(){
		return permittedDomains;
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
	
	public boolean checkSignKicks(){
		return signKicks;
	}
	
	public void setSignKicks(boolean setting){
		this.signKicks = setting;
		config.setBoolean("signKicks", this.signKicks);
	}
	
	// ##################################################
	
	public boolean checkPermittedDomain(String message){
		//Allow base domain w/o a path
		if(message.matches(".*(twitch\\.tv|twitchtv\\.com|justin\\.tv)")){
			System.out.println("INFO: Permitted domain match on jtv/ttv base domain.");
			return true;
		}
		
		for(String d:permittedDomains){
			d = d.replaceAll("\\.", "\\\\.");

			String test = ".*(\\.|^|//)" + d + "(/|$).*";
			if(message.matches(test)){
				System.out.println("DEBUG: Matched permitted domain: " + test);
				return true;
			}
			
		}
		
		return false;
	}
	
	// #################################################
	
	public String getLastfm(){
		return lastfm;
	}
	
	public void setLastfm(String string){
		lastfm = string;
		config.setString("lastfm", lastfm);
	}
	
	// #################################################
	
	
	public String getSteam(){
		return steamID;
	}
	
	public void setSteam(String string){
		steamID = string;
		config.setString("steamID", steamID);
	}
	
	// #################################################

	private void loadProperties(String name){
		try {
			config.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(server == null || server.length() < 1){
			if(BotManager.getInstance().network.equalsIgnoreCase("jtv")){
				server = name.substring(1) + ".jtvirc.com";
			}else{
				server = BotManager.getInstance().server;
			}
		}

		System.out.println("DEBUG: Setting server " + server);
		if(!config.keyExists("server")) {
			config.setString("server", server);	
		}
		
		if(!config.keyExists("port")) {
			config.setString("port", "" + BotManager.getInstance().port);
		}
		
		if(!config.keyExists("channel")) {
			config.setString("channel", name);
		}
		
		if(!config.keyExists("filterCaps")) {
			config.setBoolean("filterCaps", false);
		}
		
		if(!config.keyExists("filterCapsPercent")) {
			config.setInt("filterCapsPercent", 50);
		}
		
		if(!config.keyExists("filterCapsMinCharacters")) {
			config.setInt("filterCapsMinCharacters", 0);
		}
		
		if(!config.keyExists("filterCapsMinCapitals")) {
			config.setInt("filterCapsMinCapitals", 6);
		}
		
		if(!config.keyExists("filterLinks")) {
			config.setBoolean("filterLinks", false);
		}
		
		if(!config.keyExists("topic")) {
			config.setString("topic", "");
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
			config.setString("moderators", "");
		}
		
		if(!config.keyExists("useTopic")) {
			config.setBoolean("useTopic", true);
		}
		
		if(!config.keyExists("useFilters")) {
			config.setBoolean("useFilters", false);
		}
		
		if(!config.keyExists("enableThrow")) {
			config.setBoolean("enableThrow", true);
		}
		
		if(!config.keyExists("permittedDomains")) {
			config.setString("permittedDomains", "");
		}
		
		if(!config.keyExists("signKicks")) {
			config.setBoolean("signKicks", false);
		}
		
		if(!config.keyExists("topicTime")) {
			config.setInt("topicTime", 0);
		}
		
		if(!config.keyExists("mode")) {
			config.setInt("mode", 2);
		} 
		
		if(!config.keyExists("announceJoinParts")) {
			config.setBoolean("announceJoinParts", false);
		}
		
		if(!config.keyExists("lastfm")) {
			config.setString("lastfm", "");
		}
		
		if(!config.keyExists("steamID")) {
			config.setString("steamID", "");
		}
		

		server = config.getString("server");
		
		if(server == null || server.equalsIgnoreCase("")){
			server = BotManager.getInstance().server;
		}
		
		channel = config.getString("channel");
		port = Integer.parseInt(config.getString("port"));
		
		filterCaps = Boolean.parseBoolean(config.getString("filterCaps"));
		filterCapsPercent = Integer.parseInt(config.getString("filterCapsPercent"));
		filterCapsMinCharacters = Integer.parseInt(config.getString("filterCapsMinCharacters"));
		filterCapsMinCapitals = Integer.parseInt(config.getString("filterCapsMinCapitals"));

		announceJoinParts = Boolean.parseBoolean(config.getString("announceJoinParts"));

	
		filterLinks = Boolean.parseBoolean(config.getString("filterLinks"));
		
		topic  = config.getString("topic");
		topicTime = config.getInt("topicTime");
		
		useTopic = Boolean.parseBoolean(config.getString("useTopic"));
		useFilters = Boolean.parseBoolean(config.getString("useFilters"));
		enableThrow = Boolean.parseBoolean(config.getString("enableThrow"));

		signKicks = Boolean.parseBoolean(config.getString("signKicks"));
		
		lastfm = config.getString("lastfm");
		
		steamID = config.getString("steamID");
		
		setMode(config.getInt("mode"));

		
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
					regulars.add(regularsRaw[i].toLowerCase());
				}
			}
		}
		
		String[] moderatorsRaw = config.getString("moderators").split(",");
		
		synchronized (moderators) {
			for(int i = 0; i < moderatorsRaw.length; i++){
				if(moderatorsRaw[i].length() > 1){
					moderators.add(moderatorsRaw[i].toLowerCase());
				}
			}
		}
		
		String[] domainsRaw = config.getString("permittedDomains").split(",");
		
		synchronized (permittedDomains) {
			for(int i = 0; i < domainsRaw.length; i++){
				if(domainsRaw[i].length() > 1){
//					permittedDomains.add(domainsRaw[i].toLowerCase().replaceAll("\\.", "\\\\."));
					permittedDomains.add(domainsRaw[i].toLowerCase());
					
				}
			}
		}
	
	}

	public void setMode(int mode) {
		this.mode = mode;
		config.setInt("mode", this.mode);
	}

	public int getMode() {
		return mode;
	}
	
	public char getBullet(){
		char rt;
		switch(bulletInt){
			case 0:
				rt = bullet[bulletInt];
				bulletInt++;
				return rt;
			case 1:
				rt = bullet[bulletInt];
				bulletInt++;
				return rt;
			case 2:
				rt = bullet[bulletInt];
				bulletInt++;
				return rt;
			case 3:
				rt = bullet[bulletInt];
				bulletInt = 0;
				return rt;
		}
		
		return '>';
			
	}

}
