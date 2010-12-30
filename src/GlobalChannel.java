import java.io.IOException;
import java.util.ArrayList;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;


public class GlobalChannel {
	
	private String server;
	private int port;
	private String channel;
	private String password;
	
	private ArrayList<Channel> channelList;

	PropertiesFile config;
	
	ArrayList<GeoBot> botList;

	public GlobalChannel(ArrayList botListP){
		channelList = new ArrayList<Channel>();
		loadGlobalProfile();
		
		botList = botListP;
	}
	
	private void loadGlobalProfile(){
		config = new PropertiesFile("global.properties");
		try {
			config.load();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if(!config.keyExists("server")) {
			config.setString("server", "giantzombie.jtvirc.com");
		}
		if(!config.keyExists("channel")) {
			config.setString("channel", "#giantzombie");
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
		
		server = config.getString("server");
		channel = config.getString("channel");
		port = Integer.parseInt(config.getString("port"));
		
		password = config.getString("password");
		
		for(String s:config.getString("channelList").split(",")) {
			if(s.length() > 1){
				channelList.add(new Channel(s));
			}
		}
	}
	
	public String getPassword(){
		return password;
	}
	
	public String getChannel(){
		return channel;
	}
	
	public String getServer(){
		return server;
	}
	
	public int getPort(){
		return port;
	}
	
	public ArrayList<Channel> getChannelList(){
		return channelList;
	}
	
	public void addChannel(String name) throws NickAlreadyInUseException, IOException, IrcException{
		channelList.add(new Channel(name));
		String channelListString = "";
		
		for(Channel c:channelList) {
			channelListString += c.getChannel() + ",";
		}
		config.setString("channelList", channelListString);
		
		//Create new Bot instance and connect
		Channel c = channelList.get(channelList.size() - 1);
		botList.add(new GeoBot(this, false));
		botList.get(botList.size() - 1).setVerbose(true);
		botList.get(botList.size() - 1).connect(c.getServer(), c.getPort(), this.getPassword());
		botList.get(botList.size() - 1).joinChannel(c.getChannel());
	}
	
	public void removeChannel(String name){
		for(int i = 0; i < botList.size(); i++){
			if(name.equalsIgnoreCase(botList.get(i).getChannel().getChannel())){
				channelList.remove(i);
				botList.get(i).disconnect();
				botList.get(i).dispose();
			}
		}
		
		for(int i = 0; i < channelList.size(); i++){
			if(name.equalsIgnoreCase(channelList.get(i).getChannel())){
				channelList.remove(i);
			}
		}
		
		String channelListString = "";

		for(Channel c:channelList) {
			channelListString += c.getChannel() + ",";
		}
		config.setString("channelList", channelListString);
	}
}
