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
		commands.put(key, command);
		
		String commandsKey = "";
		String commandsValues = "";
		
		Collection c = commands.values();
		Iterator itr = c.iterator();
		
		while(itr.hasNext()){
			Map.Entry pairs = (Map.Entry)itr.next();
			commandsKey += pairs.getKey() + ",";
			commandsValues += pairs.getValue() + ",";
		}
		
		config.setString("commandsKey", commandsKey);
		config.setString("commandValues", commandsValues);

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
		
		if(!config.keyExists("commandsKey")) {
			config.setString("commandsKey", "");
		}
		
		if(!config.keyExists("commandValues")) {
			config.setString("commandValues", "");
		}
		server = config.getString("server");
		channel = config.getString("channel");
		port = Integer.parseInt(config.getString("port"));
		
		filterCaps = Boolean.parseBoolean(config.getString("filterCaps"));
		filterCapsLimit = Integer.parseInt(config.getString("filterCapsLimit"));

		filterLinks = Boolean.parseBoolean(config.getString("filterLinks"));
		
		String[] commandsKey = config.getString("commandsKey").split(",");
		String[] commandsValues = config.getString("commandsValues").split(",");
		
		for(int i = 0; i < commandsKey.length; i++){
			if(commandsKey[i].length() > 1){
				commands.put(commandsKey[i], commandsKey[i]);
			}
		}

		
	}

}
