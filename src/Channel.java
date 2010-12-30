import java.io.IOException;
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
	
	public Channel(String name){
		config = new PropertiesFile(name+".properties");
		loadProperties(name);
	}
	
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
		String commandsValues = "";
		
		Iterator itr = commands.entrySet().iterator();
		
		while(itr.hasNext()){
			Map.Entry pairs = (Map.Entry)itr.next();
			commandsKey += pairs.getKey() + ",";
			commandsValues += pairs.getValue() + ",";
		}
		
		config.setString("commandsKey", commandsKey);
		config.setString("commandValues", commandsValues);

	}
	
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
	
	public void setFilterCaps(String s){
		filterCaps = Boolean.parseBoolean(s);
		config.setBoolean("filterCaps", filterCaps);
	}
	
	public int getFilterCapsLimit(){
		return filterCapsLimit;
	}
	
	public void setFilterCapsLimit(String s){
		filterCapsLimit = Integer.parseInt(s);
		config.setInt("filterCapsLimit", filterCapsLimit);
	}
	
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
		
		if(!config.keyExists("commandsValues")) {
			config.setString("commandsValues", "");
		}
		server = config.getString("server");
		channel = config.getString("channel");
		port = Integer.parseInt(config.getString("port"));
		
		filterCaps = Boolean.parseBoolean(config.getString("filterCaps"));
		filterCapsLimit = Integer.parseInt(config.getString("filterCapsLimit"));

		filterLinks = Boolean.parseBoolean(config.getString("filterLinks"));
		
		topic  = config.getString("topic");
		
		String[] commandsKey = config.getString("commandsKey").split(",");
		String[] commandsValues = config.getString("commandsValues").split(",");
		
		for(int i = 0; i < commandsKey.length; i++){
			if(commandsKey[i].length() > 1){
				commands.put(commandsKey[i], commandsValues[i]);
			}
		}

		
	}

	public String getServer() {
		return server;
	}

	public int getPort() {
		return port;
	}

	public String getChannel() {
		return channel;
	}

}
