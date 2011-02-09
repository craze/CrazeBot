package net.bashtech.geobot;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.*;

public class GeoBot extends PircBot {
	private boolean isGlobalChannel = false;
	private GlobalChannel globalChannel;
	private Channel channelInfo;
	
	private Timer pjTimer;
	
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
				
				// !clear - Ops
				if(message.trim().equalsIgnoreCase("!clear") && isOp){
					System.out.println("Matched command !clear");
					this.sendMessage(channel, "/clear");
					//return;
				}
				
				// !topic
				if(msg[0].equalsIgnoreCase("!topic")){
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
 				if(msg[0].equalsIgnoreCase("!permit")){
 					System.out.println("Matched command !permit");
 					if(msg.length > 1 && isOp){
 						channelInfo.permitUser(msg[1]);
 						sendMessage(channel, "> " + msg[1] + " may now post 1 link.");
 					}
 				}
 				
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
				
			
				//Command catch all
				if(message.trim().substring(0,1).equalsIgnoreCase("!") && !channelInfo.getCommand(message).equalsIgnoreCase("invalid")){
					System.out.println("Matched command " + message.trim());
					sendMessage(channel, "> " + channelInfo.getCommand(message));
				}
	
			}else{
				System.out.println("Input from global channel...");
				if(isOp)
					System.out.println("User is op");
				//Global channel stuff
				if (msg[0].equalsIgnoreCase("!join") && msg.length > 1 && isOp) {
					try {
						if(msg[1].contains("#")){
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
						globalChannel.removeChannel(split(message)[1]);
						sendMessage(channel, "Channel "+ msg[1] +" parted.");
					}else{
						sendMessage(channel, "Invalid channel format. Must be in format #channelname.");
					}
						
				}
			}
			
			
			
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
	
	public User matchUser(String nick, String channel){
		User[] userList = this.getUsers(channel);
		
		for(int i = 0; i < userList.length; i++){
			if(userList[i].equals(nick)){
				return userList[i];
			}
		}
		return null;
		
	}
	
//#################################################################################
	
	public int countCapitals(String s){
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
		if(m.contains(".com") || m.contains(".org") || m.contains(".net") || m.contains(".tv")){
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
	
	public void tenSecondUnban(final String channel, final String name){
		Timer timer = new Timer();
		
		int delay = 30000;
		
		timer.schedule(new TimerTask()
	       {
	        public void run() {
	        	GeoBot.this.unBan(channel,name + "!" + name + "@*.*");
	        }
	      },delay);

	}
	
	public void autoPartandRejoin(final String channel){
		
		pjTimer = new Timer();
		
		int delay = 3600000;
		
		pjTimer.scheduleAtFixedRate(new TimerTask()
	       {
	        public void run() {
	        	System.out.println("Parting and rejoining " + channel);

	        	GeoBot.this.partChannel(channel);
	        	GeoBot.this.joinChannel(channel);
	        }
	      },delay,delay);

	}

}
