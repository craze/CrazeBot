//package net.bashtech.geobot;
//import java.io.IOException;
//import java.util.ArrayList;
//
//import org.jibble.pircbot.IrcException;
//import org.jibble.pircbot.NickAlreadyInUseException;
//
//
//public class GlobalChannel {
//	
//	private String nick;
//	private String server;
//	private int port;
//	private String channel;
//	private String password;
//	
//	private ArrayList<Channel> channelList;
//
//	PropertiesFile config;
//	
//	ArrayList<GeoBot> botList;
//
//	public GlobalChannel(ArrayList botListP){
//		channelList = new ArrayList<Channel>();
//		loadGlobalProfile();
//		
//		botList = botListP;
//	}
//	
//	private void loadGlobalProfile(){
//		config = new PropertiesFile("global.properties");
//		try {
//			config.load();
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		
//		if(!config.keyExists("nick")) {
//			config.setString("nick", "giantzombie");
//		}
//		if(!config.keyExists("server")) {
//			config.setString("server", "giantzombie.jtvirc.com");
//		}
//		if(!config.keyExists("channel")) {
//			config.setString("channel", "#giantzombie");
//		}
//		
//		if(!config.keyExists("password")) {
//			config.setString("password", "");
//		}
//		
//		if(!config.keyExists("port")) {
//			config.setInt("port", 6667);
//		}
//		
//		if(!config.keyExists("channelList")) {
//			config.setString("channelList", "");
//		}
//		
//		nick = config.getString("nick");
//		server = config.getString("server");
//		channel = config.getString("channel");
//		port = Integer.parseInt(config.getString("port"));
//		
//		password = config.getString("password");
//		
//		for(String s:config.getString("channelList").split(",")) {
//			if(s.length() > 1){
//				channelList.add(new Channel(s));
//			}
//		}
//	}
//	
//	public String getNick(){
//		return nick;
//	}
//	
//	public String getPassword(){
//		return password;
//	}
//	
//	public String getChannel(){
//		return channel;
//	}
//	
//	public String getServer(){
//		return server;
//	}
//	
//	public int getPort(){
//		return port;
//	}
//	
//	public ArrayList<Channel> getChannelList(){
//		return channelList;
//	}
//	
//	public void addChannel(String name) throws NickAlreadyInUseException, IOException, IrcException{
//		Channel newChannel = new Channel(name);
//		channelList.add(newChannel);
//		String channelListString = "";
//		
//		for(Channel c:channelList) {
//			channelListString += c.getChannel() + ",";
//		}
//		config.setString("channelList", channelListString);
//		
//		//Create new Bot instance and connect
//		botList.add(new GeoBot(this, newChannel));
//		botList.get(botList.size() - 1).setVerbose(true);
//		botList.get(botList.size() - 1).connect(newChannel.getServer(), newChannel.getPort(), this.getPassword());
//		botList.get(botList.size() - 1).joinChannel(newChannel.getChannel());
//	}
//	
//	public void removeChannel(String name){
//		for(int i = 0; i < botList.size(); i++){
//			if(name.equalsIgnoreCase(botList.get(i).getChannel().getChannel())){
//				channelList.remove(i);
//				botList.get(i).disconnect();
//				botList.get(i).dispose();
//				botList.remove(i);
//			}
//		}
//		
//		for(int i = 0; i < channelList.size(); i++){
//			if(name.equalsIgnoreCase(channelList.get(i).getChannel())){
//				channelList.remove(i);
//			}
//		}
//		
//		String channelListString = "";
//
//		for(Channel c:channelList) {
//			channelListString += c.getChannel() + ",";
//		}
//		config.setString("channelList", channelListString);
//	}
//}
