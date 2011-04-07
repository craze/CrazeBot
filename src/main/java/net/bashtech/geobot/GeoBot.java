package net.bashtech.geobot;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLConnection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;
import org.jibble.pircbot.*;

public class GeoBot extends PircBot {
	private boolean isGlobalChannel = false;
	private GlobalChannel globalChannel;
	private Channel channelInfo;
	
	private Timer pjTimer;
	
	private Map<String,Long> previousCommands = new HashMap<String,Long>();
	
	public GeoBot(GlobalChannel g, Channel c){
		globalChannel = g;
		channelInfo = c;
		this.setName(globalChannel.getNick());
		
		this.setVerbose(true);
		try {
			this.connect(channelInfo.getServer(), channelInfo.getPort(), globalChannel.getPassword());
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
		this.joinChannel(channelInfo.getChannel());
		
		autoPartandRejoin(channelInfo.getChannel());
		
	}
	
	public GeoBot(GlobalChannel g, boolean gCheck){
		isGlobalChannel = gCheck;
		globalChannel = g;
		this.setName(globalChannel.getNick());
		
		this.setVerbose(true);
		try {
			this.connect(globalChannel.getServer(), globalChannel.getPort(), globalChannel.getPassword());
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
		this.joinChannel(globalChannel.getChannel());
		
		autoPartandRejoin(globalChannel.getChannel());
	}
	
	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message){
		
		if(sender.equalsIgnoreCase(this.getNick())){
			System.out.println("Message from bot");
		}
			
			String[] msg = split(message.trim());
			User user = matchUser(sender, channel);
			
			//System.out.println(user.toString());
			
			boolean isOp = false;
			try{
				if(user.getPrefix().equalsIgnoreCase("@"))
					isOp = true;
				if(user.isOp())
					isOp = true;
			}catch(Exception e){
				System.out.println("Prefix exception.");
			}

			if(channel.equalsIgnoreCase("#" + sender))
				isOp = true;
			

			
			if(!isGlobalChannel){
				
				if(channelInfo.isModerator(sender)){
					isOp = true;
				}
				
				if(isOp)
					System.out.println("User is op");
				
				//Normal channel stuff
				// !time - All
				if (message.trim().equalsIgnoreCase("!time")) {
						System.out.println("Matched command !time");
						String time = new java.util.Date().toString();
						sendMessage(channel, sender + ": The time is now " + time);
						//return;
				}
				
				// !viewers - All
				if (message.trim().equalsIgnoreCase("!viewers")) {
					System.out.println("Matched command !viewers");
					try {
						sendMessage(channel, "> " + this.getViewers() + " viewers.");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//return;
				}
				
				// !bitrate - All
				if (message.trim().equalsIgnoreCase("!bitrate")) {
					System.out.println("Matched command !bitrate");
					try {
						sendMessage(channel, "> Streaming at " + this.getBitRate() + " Kbps.");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//return;
				}
				
				// !{botname} - All
				if (message.trim().equalsIgnoreCase("!" + this.getNick())) {
					System.out.println("Matched command !" + this.getNick());
					sendMessage(channel, "> Commands: " + channelInfo.getCommandList());

					//return;
				}
				
				// !clear - Ops
				if(message.trim().equalsIgnoreCase("!clear") && isOp){
					System.out.println("Matched command !clear");
					this.sendMessage(channel, "/clear");
					//return;
				}
				
				// !topic
				if(msg[0].equalsIgnoreCase("!topic") && channelInfo.useTopic){
					System.out.println("Matched command !topic");
					if(msg.length < 2 || !isOp){
						this.sendMessage(channel, "> Topic: " + channelInfo.getTopic());
					}else if(msg.length > 1 && isOp){
						channelInfo.setTopic(message.substring(7));
						this.sendMessage(channel, "> Topic: " + channelInfo.getTopic());
						//Below only works if bot is the channel owner
						//this.sendMessage(channel, "/title " + channelInfo.getTopic());
					}
					//return;
				}
				
				// !command - Sets commands
 				if(msg[0].equalsIgnoreCase("!command")){
 					System.out.println("Matched command !command");
					if(msg.length < 3 && isOp){
						this.sendMessage(channel, "> !command add/delete name string");
					}else if(msg.length > 2 && isOp){
						if(msg[1].equalsIgnoreCase("add")){
							String key = "!" + msg[2];
							String value = "";
							
							for(int i = 3; i < msg.length; i++){
								value = value + msg[i] + " ";
							}
							if(!value.contains(",,")){
								channelInfo.setCommand(key, value);
								this.sendMessage(channel, "> " + channelInfo.getCommand(key));
							}else{
								sendMessage(channel, "Command cannot contain double commas (\",,\").");
							}
								
						}else if(msg[1].equalsIgnoreCase("delete")){
							String key = "!" + msg[2];
							channelInfo.removeCommand(key);	
							this.sendMessage(channel, "> Command " + key + " removed.");

							}
					}
				}
 				
 				// !links - Turns on/off link filter
 				if(msg[0].equalsIgnoreCase("!links") && isOp){
 					System.out.println("Matched command !links");
 					if(msg.length == 2){
 						if(msg[1].equalsIgnoreCase("on")){
 							channelInfo.setFilterLinks(true);
 							this.sendMessage(channel, "> Link filter: " + channelInfo.getFilterLinks());
 						}else if(msg[1].equalsIgnoreCase("off")){
 							channelInfo.setFilterLinks(false);
 							this.sendMessage(channel, "> Link filter: " + channelInfo.getFilterLinks());
 						}
 					}
 				}
				
 				// !caps - Turns on/off caps filter and sets limit.
 				if(msg[0].equalsIgnoreCase("!caps") && isOp){
 					System.out.println("Matched command !caps");
 					if(msg.length > 1){
 						if(msg[1].equalsIgnoreCase("on")){
 							channelInfo.setFilterCaps(true);
 							this.sendMessage(channel, "> Caps filter: " + channelInfo.getFilterCaps());
 						}else if(msg[1].equalsIgnoreCase("off")){
 							channelInfo.setFilterCaps(false);
 							this.sendMessage(channel, "> Caps filter: " + channelInfo.getFilterCaps());
 						}else if(msg[1].equalsIgnoreCase("limit")){
 							if(msg.length > 2){
 								channelInfo.setFilterCapsLimit(Integer.parseInt(msg[2]));
 	 							this.sendMessage(channel, "> Caps filter limit: " + channelInfo.getFilterCapsLimit());
 							}
 						}
 					}
 				}
 				
 				// !regular - Add regulars
 				if(msg[0].equalsIgnoreCase("!regular")){
 					System.out.println("Matched command !regular");
 					if(msg.length  > 2 && isOp){
 						if(msg[1].equalsIgnoreCase("add")){
 							if(channelInfo.isRegular(msg[2])){
 								sendMessage(channel,"> User already exists." + "(" + msg[2] + ")");
 							}else{
 								channelInfo.addRegular(msg[2]);
 								sendMessage(channel,"> User added." + "(" + msg[2] + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete")){
 							if(channelInfo.isRegular(msg[2])){
 								channelInfo.removeRegular(msg[2]);
 								sendMessage(channel,"> User removed." + "(" + msg[2] + ")");
 							}else{
 								sendMessage(channel,"> User does not exist." + "(" + msg[2] + ")");
 							}
 						}
 					}
 				}
 				
 				// !mod - Add moderators
 				if(msg[0].equalsIgnoreCase("!mod")){
 					System.out.println("Matched command !mod");
 					if(msg.length  > 2 && isOp){
 						if(msg[1].equalsIgnoreCase("add")){
 							if(channelInfo.isModerator(msg[2])){
 								sendMessage(channel,"> User already exists." + "(" + msg[2] + ")");
 							}else{
 								channelInfo.addModerator(msg[2]);
 								sendMessage(channel,"> User added."+ "(" + msg[2] + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete")){
 							if(channelInfo.isModerator(msg[2])){
 								channelInfo.removeModerator(msg[2]);
 								sendMessage(channel,"> User removed. "+ "(" + msg[2] + ")");
 							}else{
 								sendMessage(channel,"> User does not exist."+ "(" + msg[2] + ")");
 							}
 						}
 					}
 				}
 				
 				// !permit - Allows users to post 1 link
 				if(msg[0].equalsIgnoreCase("!permit") && channelInfo.useFilters){
 					System.out.println("Matched command !permit");
 					if(msg.length > 1 && isOp ){
 						channelInfo.permitUser(msg[1]);
 						sendMessage(channel, "> " + msg[1] + " may now post 1 link.");
 					}
 				}
 				
 				// !set - Allows you to turn off features of the bot.
 				if(msg[0].equalsIgnoreCase("!set")){
 					System.out.println("Matched command !set");
 					if(msg.length > 0 && isOp){
 						if(msg.length == 1){
 							//Display current settings
 						}else if(msg[1].equalsIgnoreCase("topic")){
 							//Topic
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setTopicFeature(true);
 								sendMessage(channel, "> Feature: Topic is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setTopicFeature(false);
 								sendMessage(channel, "> Feature: Topic is off");
 							}
 								
 						}else if(msg[1].equalsIgnoreCase("filters")){
 							//filters
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setFiltersFeature(true);
 								sendMessage(channel, "> Feature: Filters is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setFiltersFeature(false);
 								sendMessage(channel, "> Feature: Filters is off");
 							}
 						}
 					}
 				}
 				
 				//Command catch all
				if(message.trim().substring(0,1).equalsIgnoreCase("!") && !channelInfo.getCommand(message).equalsIgnoreCase("invalid")){
					System.out.println("Matched command " + message.trim());
					sendMessage(channel, "> " + channelInfo.getCommand(message));
				}
 				
				//Filter feature check
 				if(!channelInfo.useFilters)
 					return;
 				
				// Cap filter
				if(channelInfo.getFilterCaps() && countCapitals(message) > channelInfo.getFilterCapsLimit() && !(isOp || channelInfo.isRegular(sender))){
					this.kick(channel, sender);
					tenSecondUnban(channel, sender);
				}
				
				// Link filter
				if(channelInfo.getFilterLinks() && this.containsLink(message) && !(channelInfo.linkPermissionCheck(sender) || isOp )){
					this.kick(channel, sender);
					tenSecondUnban(channel, sender);
				}
				
			

	
			}else{
				System.out.println("Input from global channel...");
				if(isOp)
					System.out.println("User is op");
				//Global channel stuff
				if (msg[0].equalsIgnoreCase("!join") && msg.length > 1 && isOp) {
					try {
						if(msg[1].contains("#")){
							sendMessage(channel, "Channel "+ msg[1] +" joining...");
							globalChannel.addChannel(msg[1]);
							sendMessage(channel, "Channel "+ msg[1] +" joined.");
						}else{
							sendMessage(channel, "Invalid channel format. Must be in format #channelname.");
						}
						
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
				}
				
				if (msg[0].equalsIgnoreCase("!leave") && msg.length > 1 && isOp) {
					if(msg[1].contains("#")){
						sendMessage(channel, "Channel "+ msg[1] +" parting...");
						globalChannel.removeChannel(split(message)[1]);
						sendMessage(channel, "Channel "+ msg[1] +" parted.");
					}else{
						sendMessage(channel, "Invalid channel format. Must be in format #channelname.");
					}
						
				}
			}
			
			
			
	}
	
	@Override
	public boolean onMessageSend(String target, String message){
		long epoch = System.currentTimeMillis()/1000;
		
		//Clean up
		Iterator<Map.Entry<String, Long>> i = previousCommands.entrySet().iterator();  
		while (i.hasNext()) {  
		    Map.Entry<String, Long> entry = i.next();  
		    if (epoch - (long)entry.getValue() > 30) {  
		        i.remove();  
		    }  
		} 

		// Log message to previous commands
		if(previousCommands.containsKey(message.toLowerCase())){
			//Command was issued before
			int timeDifference = (int) (epoch - previousCommands.get(message.toLowerCase()));
			if( timeDifference < 30){
				//Command was issued in the last 30 seconds
				previousCommands.put(message.toLowerCase(), epoch);
				System.out.println("Message not sent. Message repeated " + timeDifference + " seconds.");
				return false;
			}
		}
		previousCommands.put(message.toLowerCase(), epoch);
		
		return true;
		
	}
	
	@Override
	public void onDisconnect(){
		//pjTimer.cancel();
		for(Channel c:globalChannel.getChannelList()){
			if(c.getChannel() == channelInfo.getChannel()){
				System.out.println("Channel disconnected abnormally. Reconnecting...\n");
				try {
					this.reconnect();
					this.joinChannel(channelInfo.getChannel());
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
				
			}
		}
		
	}
	
	private User matchUser(String nick, String channel){
		User[] userList = this.getUsers(channel);
		
		for(int i = 0; i < userList.length; i++){
			if(userList[i].equals(nick)){
				return userList[i];
			}
		}
		return null;
		
	}
	
//#################################################################################
	
	private int countCapitals(String s){
		int caps = 0;
		int max = 0;
		//boolean con = true;
		for (int i=0; i<s.length(); i++)
		{
			if (Character.isUpperCase(s.charAt(i))){
					caps++;
			}else{
				if(caps > 0 && caps > max)
						max = caps;
				caps = 0;
			}
		}
		if(caps > max)
			return caps;
		else
			return max;
	}
	
	private boolean containsLink(String m){
		m = m.toLowerCase();
		if(m.contains(".com") || m.contains(".org") || m.contains(".net") || m.contains(".tv") || m.contains(".ca") || m.contains(".cc") ||
		   m.contains(".de") || m.contains(".eu") || m.contains(".fm") || m.contains(".gov") || m.contains(".info") || m.contains(".io") ||
		   m.contains(".jobs") || m.contains(".me") || m.contains(".mil") || m.contains(".mobi") || m.contains(".name") || m.contains("rn") ||
		   m.contains(".tel") || m.contains(".travel") || m.contains(".tz") || m.contains(".uk") || m.contains(".us")){
			return true;
		}
		
		return false;
	}
	
	private String[] split(String s){
		return s.split(" ");
	}
	
	public Channel getChannel(){
		return channelInfo;
	}
	
	private void tenSecondUnban(final String channel, final String name){
		Timer timer = new Timer();
		
		int delay = 30000;
		
		timer.schedule(new TimerTask()
	       {
	        public void run() {
	        	GeoBot.this.unBan(channel,name + "!" + name + "@*.*");
	        }
	      },delay);

	}
	
	private void autoPartandRejoin(final String channel){
		
		pjTimer = new Timer();
		
		int delay = 1800000;
		
		pjTimer.scheduleAtFixedRate(new TimerTask()
	       {
	        public void run() {
	        	System.out.println("Parting and rejoining " + channel);

	        	GeoBot.this.partChannel(channel);
	        	GeoBot.this.joinChannel(channel);
	        }
	      },delay,delay);

	}
	
	private int getViewers() throws IOException{
		URL url = new URL("http://api.justin.tv/api/stream/summary.json?channel=" + channelInfo.getChannel().substring(1));
		URLConnection conn = url.openConnection();
		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));
		String jsonIn = "";
		while(d.ready())
		{
			jsonIn = d.readLine();
		}
		
		JTVStreamSummary data = new Gson().fromJson(jsonIn, JTVStreamSummary.class);
		
		return data.viewers_count;
	}
	
	private int getBitRate() throws IOException{
		URL url = new URL("http://api.justin.tv/api/stream/summary.json?channel=" + channelInfo.getChannel().substring(1));
		URLConnection conn = url.openConnection();
		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));
		String jsonIn = "";
		while(d.ready())
		{
			jsonIn = d.readLine();
		}
		
		JTVStreamSummary data = new Gson().fromJson(jsonIn, JTVStreamSummary.class);
		
		return data.average_bitrate;
	}

}
