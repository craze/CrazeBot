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
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.Gson;
import org.jibble.pircbot.*;

public class GeoBot extends PircBot {	
	//private Timer pjTimer;
	
	private BotManager botManager;
	
	//private Map<String,Long> previousCommands = new HashMap<String,Long>();
	
	private String[] linksMasks = {".*http://.*",".*\\.(com|org|net|tv|ca|xxx|cc|de|eu|fm|gov|info|io|jobs|me|mil|mobi|name|rn|tel|travel|tz|uk|co|us|be)(\\s+|/|$).*"};
	
	/*private String[] linksMasks = {".*http://.*",".*\\.com(\\s+|/).*",".*\\.org(\\s+|/).*",".*\\.net(\\s+|/).*",".*\\.tv(\\s+|/).*",".*\\.ca(\\s+|/).*",".*\\.xxx(\\s+|/).*",".*\\.cc(\\s+|/).*",".*\\.de(\\s+|/).*",
								   ".*\\.eu(\\s+|/).*",".*\\.fm(\\s+|/).*",".*\\.gov(\\s+|/).*",".*\\.info(\\s+|/).*",".*\\.io(\\s+|/).*",".*\\.jobs(\\s+|/).*",".*\\.me(\\s+|/).*",".*\\.mil(\\s+|/).*",
			                       ".*\\.mobi(\\s+|/).*",".*\\.name(\\s+|/).*",".*\\.rn(\\s+|/).*",".*\\.tel(\\s+|/).*",".*\\.travel(\\s+|/).*",".*\\.tz(\\s+|/).*",".*\\.uk(\\s+|/).*",".*\\.us(\\s+|/).*",".*\\.be(\\s+|/).*"};*/
	
	public GeoBot(BotManager bm, String server, int port){
		System.out.println("DEBUG: Bot created.");
		botManager = bm;
		
		this.setName(bm.nick);
		
		this.setVerbose(true);
		try {
			this.connect(server, port, bm.password);
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
		
		//autoPartandRejoin();
		
	}

	public void onPrivateMessage(String sender, String login, String hostname, String message) {
		String[] msg = split(message.trim());

		if (msg[0].equalsIgnoreCase("!join") && msg.length > 1 && botManager.isAdmin(sender)) {
			try {
				if(msg[1].contains("#")){
					sendMessage(sender, "Channel "+ msg[1] +" joining...");
					botManager.addChannel(msg[1], msg[2]);
					sendMessage(sender, "Channel "+ msg[1] +" joined.");
				}else{
					sendMessage(sender, "Invalid channel format. Must be in format #channelname.");
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
		
		if (msg[0].equalsIgnoreCase("!leave") && msg.length > 1 && botManager.isAdmin(sender)) {
			if(msg[1].contains("#")){
				sendMessage(sender, "Channel "+ msg[1] +" parting...");
				botManager.removeChannel(split(message)[1]);
				sendMessage(sender, "Channel "+ msg[1] +" parted.");
			}else{
				sendMessage(sender, "Invalid channel format. Must be in format #channelname.");
			}
				
		}
		
		if (msg[0].equalsIgnoreCase("!rejoin") && botManager.isAdmin(sender)) {
			sendMessage(sender, "Rejoining all channels.");
			botManager.rejoinChannels();
		}
		
		System.out.println("DEBUG: PM from " + sender + " message=" + message);
		
	}
	
	
	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message){
		System.out.println("DEBUG: " + channel + " " + sender + " " + message);
		
		Channel channelInfo = botManager.getChannel(channel);
		
		if(channelInfo == null)
			return;
		
		System.out.println("DEBUG: " + message);

		
		if(sender.equalsIgnoreCase(this.getNick())){
			System.out.println("Message from bot");
			return;
		}
			
			String[] msg = split(message.trim());
			User user = matchUser(sender, channel);
			if(user == null){
				user = new User("", sender);
			}
			
			
			boolean isOp = false;
			boolean isRegular = false;
			try{
				if(user.getPrefix().equalsIgnoreCase("@") || user.getPrefix().equalsIgnoreCase("~"))
					isOp = true;
				if(user.isOp())
					isOp = true;
				if(user.getPrefix().equalsIgnoreCase("@") || user.getPrefix().equalsIgnoreCase("~") || user.getPrefix().equalsIgnoreCase("+"))
					isRegular = true;
			}catch(Exception e){
				System.out.println("Prefix exception.");
			}

			if(channel.equalsIgnoreCase("#" + sender))
				isOp = true;
			
			if(channelInfo.isRegular(sender))
				isRegular = true;

				
				
				if(channelInfo.isModerator(sender)){
					isOp = true;
				}
				
				if(isOp)
					System.out.println("User is op");
				if(isRegular)
					System.out.println("User is regular");
				
				if(channelInfo.getPoll() != null && channelInfo.getPoll().getStatus()){
					//Poll is open and accepting votes.
					channelInfo.getPoll().vote(sender, msg[0]);
					System.out.println("DEBUG: Voted.");
				}
				
				if(channelInfo.getGiveaway() != null && channelInfo.getGiveaway().getStatus()){
					//Giveaway is open and accepting entries.
					System.out.println("DEBUG: Attempting entry.");
					channelInfo.getGiveaway().submitEntry(sender, msg[0]);
				}
				
				// !poll - Ops
				if(msg[0].equalsIgnoreCase("!poll") && isOp){
					System.out.println("Matched command !poll");
					if(msg.length >= 2){
						if(msg[1].equalsIgnoreCase("create")){
							String[] options = new String[msg.length - 2];
							int oc = 0;
							for(int c=2;c<msg.length;c++){
								options[oc] = msg[c];
								oc++;
							}
							channelInfo.setPoll(new Poll(options));
							sendMessage(channel,"> Poll created. Do '!poll start' to start voting.");
						}else if(msg[1].equalsIgnoreCase("start")){
							if(channelInfo.getPoll() != null){
								if(channelInfo.getPoll().getStatus()){
									sendMessage(channel, "> Poll is alreay running.");
								}else{
									channelInfo.getPoll().setStatus(true);
									sendMessage(channel, "> Poll started.");
								}
							}
						}else if(msg[1].equalsIgnoreCase("stop")){ 
							if(channelInfo.getPoll() != null){
								if(channelInfo.getPoll().getStatus()){
									channelInfo.getPoll().setStatus(false);
									sendMessage(channel, "> Poll stopped.");
								}else{
									sendMessage(channel, "> Poll is not running.");
								}
							}
						}else if(msg[1].equalsIgnoreCase("results")){
							if(channelInfo.getPoll() != null){
								String[] results = channelInfo.getPoll().getResults();
								for(int c=0;c<results.length;c++){
									sendMessage(channel, results[c]);
								}
							}
						
					   }
					}
				}
				
				// !giveaway - Ops
				if((msg[0].equalsIgnoreCase("!giveaway") || msg[0].equalsIgnoreCase("!ga")) && isOp){
					System.out.println("Matched command !giveaway");
					if(msg.length >= 2){
						if(msg[1].equalsIgnoreCase("create")){
							String max = "" + 100;
							if(msg.length > 2){
								max = msg[2];
							}
							channelInfo.setGiveaway(new Giveaway(max));
							if(msg.length > 3 && channelInfo.getGiveaway().isInteger(msg[3])){
								this.startGaTimer(Integer.parseInt(msg[3]),channelInfo);
							}else{
								sendMessage(channel,"> Giveaway created. Do '!giveaway start' to start." + " Range 1-" + channelInfo.getGiveaway().getMax() + ".");
							}
							//sendMessage(channel,"> Giveaway created. Do '!giveaway start' to start." + " Range 1-" + channelInfo.getGiveaway().getMax() + ".");
							
						}else if(msg[1].equalsIgnoreCase("start")){
							if(channelInfo.getGiveaway() != null){
								if(channelInfo.getGiveaway().getStatus()){
									sendMessage(channel, "> Giveaway is alreay running.");
								}else{
									channelInfo.getGiveaway().setStatus(true);
									sendMessage(channel, "> Giveaway started.");
								}
							}
						}else if(msg[1].equalsIgnoreCase("stop")){ 
							if(channelInfo.getGiveaway() != null){
								if(channelInfo.getGiveaway().getStatus()){
									channelInfo.getGiveaway().setStatus(false);
									sendMessage(channel, "> Giveaway stopped.");
								}else{
									sendMessage(channel, "> Giveaway is not running.");
								}
							}
						}else if(msg[1].equalsIgnoreCase("results")){
							if(channelInfo.getGiveaway() != null){
								String[] results = channelInfo.getGiveaway().getResults();
								for(int c=0;c<results.length;c++){
									sendMessage(channel, results[c]);
								}
							}else{
								sendMessage(channel, "> No giveaway in memory.");
							}
						
					   }
					}
				}
				
				// !random - Ops
				if(msg[0].equalsIgnoreCase("!random")&& isOp){
					System.out.println("Matched command !random");
					if(msg.length >= 2){
						if(msg[1].equalsIgnoreCase("user")){
							User[] userList = this.getUsers(channel);
							if(userList.length > 0){
								Random rand = new Random();
								int randIndex = rand.nextInt(userList.length);
								sendMessage(channel,"> Random user: " + userList[randIndex].getNick());
							}
//						}else if(msg[1].equalsIgnoreCase("number")){
//							
						}
					}
				}
				
				// !time - All
				if (msg[0].equalsIgnoreCase("!time")) {
						System.out.println("Matched command !time");
						String time = new java.util.Date().toString();
						sendMessage(channel, sender + ": The time is now " + time);
						//return;
				}
				
				// !bothelp - Ops
				if (msg[0].equalsIgnoreCase("!bothelp") && isOp) {
						System.out.println("Matched command !bothelp");
						sendMessage(channel, "> Command help is available at https://github.com/bashtech/GeoBotIRC/wiki/Commands.");
						//return;
				}
				
				// !viewers - All
				if (msg[0].equalsIgnoreCase("!viewers")) {
					if(!botManager.network.equalsIgnoreCase("jtv"))
						return;
					System.out.println("Matched command !viewers");
					try {
						sendMessage(channel, "> " + this.getViewers(channelInfo) + " viewers.");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//return;
				}
				
				// !bitrate - All
				if (msg[0].equalsIgnoreCase("!bitrate")) {
					if(!botManager.network.equalsIgnoreCase("jtv"))
						return;
					System.out.println("Matched command !bitrate");
					try {
						sendMessage(channel, "> Streaming at " + this.getBitRate(channelInfo) + " Kbps.");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//return;
				}
				
				// !{botname} - All
				if (msg[0].equalsIgnoreCase("!" + this.getNick()) && (isRegular || isOp)) {
					System.out.println("Matched command !" + this.getNick());
					sendMessage(channel, "> Commands: " + channelInfo.getCommandList());

					//return;
				}
				
				// !clear - Ops
				if(msg[0].equalsIgnoreCase("!clear") && isOp){
					System.out.println("Matched command !clear");
					this.sendMessage(channel, ".clear");
					//return;
				}
				
				// !throw - All
				if(msg[0].equalsIgnoreCase("!throw") && channelInfo.checkThrow()){
					System.out.println("Matched command !throw");
					if(msg.length > 2){
						String throwMessage = "";
						for(int i=2;i<msg.length;i++){
							throwMessage += msg[i] + " ";
						}
						
						this.sendMessage(channel, "> " + sender + " throws " + throwMessage + "at " + msg[1]);
					}
					//return;
				}
				
				// !topic
				if(msg[0].equalsIgnoreCase("!topic") && channelInfo.useTopic){
					System.out.println("Matched command !topic");
					if(msg.length < 2 || !isOp){
						if(channelInfo.getTopic().equalsIgnoreCase("")){
							sendMessage(channel, "> No topic is set.");
						}else{
							this.sendMessage(channel, "> Topic: " + channelInfo.getTopic() + " (Set " + channelInfo.getTopicTime() + " ago)");
						}
					}else if(msg.length > 1 && isOp){
						if(msg[1].equalsIgnoreCase("unset")){
							channelInfo.setTopic("");
							sendMessage(channel, "> No topic is set.");
						}else{
							channelInfo.setTopic(message.substring(7));
							this.sendMessage(channel, "> Topic: " + channelInfo.getTopic() + " (Set " + channelInfo.getTopicTime() + " ago)");
							if(botManager.network.equalsIgnoreCase("ngame"))
								this.sendMessage(channel, ".topic " + channelInfo.getTopic());
						}

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
 						}else if(msg[1].equalsIgnoreCase("percent")){
 							if(msg.length > 2){
 								channelInfo.setFilterCapsLimit(Integer.parseInt(msg[2]));
 	 							this.sendMessage(channel, "> Caps filter percent: " + channelInfo.getFilterCapsLimit());
 							}
 						}else if(msg[1].equalsIgnoreCase("minchars")){
 							if(msg.length > 2){
 								channelInfo.filterCapsMinCharacters = Integer.parseInt(msg[2]);
 	 							this.sendMessage(channel, "> Caps filter min characters: " + channelInfo.filterCapsMinCharacters);
 							}
 						}else if(msg[1].equalsIgnoreCase("mincaps")){
 							if(msg.length > 2){
 								channelInfo.filterCapsMinCapitals = Integer.parseInt(msg[2]);
 	 							this.sendMessage(channel, "> Caps filter min captitals: " + channelInfo.filterCapsMinCapitals);
 							}
 						}else if(msg[1].equalsIgnoreCase("status")){
 							sendMessage(channel, "> Caps filter=" + channelInfo.getFilterCaps() + ", percent=" + channelInfo.filterCapsPercent + ", minchars=" + channelInfo.filterCapsMinCharacters + ", mincaps= " + channelInfo.filterCapsMinCapitals);
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
 								sendMessage(channel,"> User added. " + "(" + msg[2] + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete")){
 							if(channelInfo.isRegular(msg[2])){
 								channelInfo.removeRegular(msg[2]);
 								sendMessage(channel,"> User removed." + "(" + msg[2] + ")");
 							}else{
 								sendMessage(channel,"> User does not exist." + "(" + msg[2] + ")");
 							}
 						}
 					}else if(msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOp){
 						String tempList = "> Regulars: ";
 						for(String s:channelInfo.getRegulars()){
 							tempList += s + ", ";
 						}
 						sendMessage(channel, tempList);
 					}
 				}
 				
 				// !mod - Add moderators
 				if(msg[0].equalsIgnoreCase("!mod")){
 					System.out.println("Matched command !mod");
 					if(msg.length  > 2 && isOp){
 						if(msg[1].equalsIgnoreCase("add")){
 							if(channelInfo.isModerator(msg[2])){
 								sendMessage(channel,"> User already exists. " + "(" + msg[2] + ")");
 							}else{
 								channelInfo.addModerator(msg[2]);
 								sendMessage(channel,"> User added. "+ "(" + msg[2] + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete")){
 							if(channelInfo.isModerator(msg[2])){
 								channelInfo.removeModerator(msg[2]);
 								sendMessage(channel,"> User removed. "+ "(" + msg[2] + ")");
 							}else{
 								sendMessage(channel,"> User does not exist."+ "(" + msg[2] + ")");
 							}
 						}
 					}else if(msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOp){
 						String tempList = "> Moderators: ";
 						for(String s:channelInfo.getModerators()){
 							tempList += s + ", ";
 						}
 						sendMessage(channel, tempList);
 					}
 				}
 				
 				// !permit - Allows users to post 1 link
 				if(msg[0].equalsIgnoreCase("!permit") && channelInfo.getFilterLinks() && channelInfo.useFilters){
 					System.out.println("Matched command !permit");
 					if(msg.length > 1 && isOp ){
 						if(!channelInfo.isRegular(msg[1])){
 							channelInfo.permitUser(msg[1]);
 	 						sendMessage(channel, "> " + msg[1] + " may now post 1 link.");
 						}else{
 							sendMessage(channel, "> " + msg[1] + " is a regular and does not need to be permitted.");
 						}
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
						}else if(msg[1].equalsIgnoreCase("throw")){
 							//filters
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setThrow(true);
 								sendMessage(channel, "> Feature: !throw is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setThrow(false);
 								sendMessage(channel, "> Feature: !throw is off");
 							}
						}else if(msg[1].equalsIgnoreCase("signkicks")){
 							//filters
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setSignKicks(true);
 								sendMessage(channel, "> Feature: Sign-kicks is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setSignKicks(false);
 								sendMessage(channel, "> Feature: Sign-kicks is off");
 							}
 						}
 					}
 				}
 				
 				//!leave - Ops
 				if (msg[0].equalsIgnoreCase("!leave") && isOp) {
 						sendMessage(channel, "Channel "+ channel +" parting...");
 						botManager.removeChannel(channel);
 				}
 				
 				
 				
 				// *******************************************************************
 				
 				if (msg[0].equalsIgnoreCase("!bm-join") && msg.length > 1 && botManager.isAdmin(sender)) {
 					try {
 						if(msg[1].contains("#")){
 							sendMessage(channel, "Channel "+ msg[1] +" joining...");
 							botManager.addChannel(msg[1], msg[2]);
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
 				
 				if (msg[0].equalsIgnoreCase("!bm-leave") && msg.length > 1 && botManager.isAdmin(sender)) {
 					if(msg[1].contains("#")){
 						sendMessage(channel, "Channel "+ msg[1] +" parting...");
 						botManager.removeChannel(split(message)[1]);
 						sendMessage(channel, "Channel "+ msg[1] +" parted.");
 					}else{
 						sendMessage(channel, "Invalid channel format. Must be in format #channelname.");
 					}
 						
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-rejoin") && botManager.isAdmin(sender)) {
 					sendMessage(channel, "Rejoining all channels.");
 					botManager.rejoinChannels();
 				}
 				
 				// *******************************************************************

 				
 				//Command catch all
				if(message.trim().substring(0,1).equalsIgnoreCase("!") && !channelInfo.getCommand(message).equalsIgnoreCase("invalid")){
					System.out.println("Matched command " + message.trim());
					sendMessage(channel, "> " + channelInfo.getCommand(message));
				}
 				
				//Filter feature check
 				if(!channelInfo.useFilters)
 					return;
 				
				// Cap filter
 				int capsNumber = getCapsNumber(message);
 				int capsPercent = getCapsPercent(message);
 				System.out.println("DEBUG: Message Length= " + message.length());
 				System.out.println("DEBUG: Caps percent= " + capsPercent);
 				System.out.println("DEBUG: Caps number= " + capsNumber);
				if(channelInfo.getFilterCaps() && !(isOp || isRegular) && message.length() >= channelInfo.filterCapsMinCharacters && capsPercent >= channelInfo.filterCapsPercent && capsNumber >= channelInfo.filterCapsMinCapitals){
					if(botManager.network.equalsIgnoreCase("ngame"))
						this.ban(channel, sender);
					else
						this.kick(channel, sender, "Too many caps.");
					
					tenSecondUnban(channel, sender);
					if(channelInfo.checkSignKicks())
						sendMessage(channel, "> " + sender + ", please don't shout or talk in all caps. (10 sec. timeout)");
				}
				
				// Link filter
				if(channelInfo.getFilterLinks() && !(isOp || isRegular) && this.containsLink(message) ){
					boolean result = channelInfo.linkPermissionCheck(sender);
					if(result){
						sendMessage(channel, "> Link permitted. (" + sender + ")" );
					}else{
						if(botManager.network.equalsIgnoreCase("ngame"))
							this.ban(channel, sender);
						else
							this.kick(channel, sender, "Unauthorized link.");
						
						tenSecondUnban(channel, sender);
						if(channelInfo.checkSignKicks())
							sendMessage(channel, "> " + sender + ", please ask a moderator before posting links. (10 sec. timeout)");
					}
					
				}

			
			
	}

	
	@Override
	public void onDisconnect(){
		//pjTimer.cancel();

		try {
			this.reconnect();
			for (Map.Entry<String, Channel> entry : botManager.channelList.entrySet())
			{
				this.joinChannel(entry.getValue().getChannel());
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
	
	private int getCapsNumber(String s){
		int caps = 0;
		for (int i=0; i<s.length(); i++)
		{
			if (Character.isUpperCase(s.charAt(i))){
					caps++;
			}
		}
		
		return caps;
	}
	
	private int getCapsPercent(String s){
		int caps = 0;
		for (int i=0; i<s.length(); i++)
		{
			if (Character.isUpperCase(s.charAt(i))){
					caps++;
			}
		}
		
		return (int)(((double)caps)/s.length()*100);
	}
	
	
	
	private int countConsecutiveCapitals(String s){
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
		
		for(String mask: linksMasks){
			if(m.matches(mask)){
				System.out.println("DEBUG: Link match on " + mask);
				return true;
			}
		}
		
		return false;
	}
	
	private String[] split(String s){
		return s.split(" ");
	}
	
//	public Channel getChannel(){
//		return channelInfo;
//	}
	
	private void tenSecondUnban(final String channel, final String name){
		Timer timer = new Timer();
		
		int delay = 30000;
		
		timer.schedule(new TimerTask()
	       {
	        public void run() {
				if(botManager.network.equalsIgnoreCase("jtv"))
		        	GeoBot.this.unBan(channel,name + "!*@*.*");
				else
					GeoBot.this.unBan(channel,name);
	        }
	      },delay);

	}
	
	private void startGaTimer(int seconds, Channel channelInfo){

		
		if(channelInfo.getGiveaway() != null){
			channelInfo.getGiveaway().setTimer(new Timer());
			int delay = seconds*1000;
			
			if(!channelInfo.getGiveaway().getStatus()){
				channelInfo.getGiveaway().setStatus(true);
				sendMessage(channelInfo.getChannel(), "> Giveaway started. (" + seconds + " seconds)");
			}
			
			channelInfo.getGiveaway().getTimer().schedule(new giveawayTimer(channelInfo),delay);
		}
		
		

	}

	private int getViewers(Channel channelInfo) throws IOException{
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
	
	private int getBitRate(Channel channelInfo) throws IOException{
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
	
	public static boolean isInteger(String str) {
        if (str == null) {
                return false;
        }
        int length = str.length();
        if (length == 0) {
                return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
                if (length == 1) {
                        return false;
                }
                i = 1;
        }
        for (; i < length; i++) {
                char c = str.charAt(i);
                if (c <= '/' || c >= ':') {
                        return false;
                }
        }
        return true;
	}
	
	private class giveawayTimer extends TimerTask{
		private Channel channelInfo;
		
		public giveawayTimer(Channel channelInfo2){
			super();
			channelInfo = channelInfo2;
		}
        public void run() {
			if(channelInfo.getGiveaway() != null){
				if(channelInfo.getGiveaway().getStatus()){
					channelInfo.getGiveaway().setStatus(false);
					GeoBot.this.sendMessage(channelInfo.getChannel(), "> Giveaway over.");
//					String[] results = channelInfo.getGiveaway().getResults();
//					for(int c=0;c<results.length;c++){
//						sendMessage(GeoBot.this.channelInfo.getChannel(), results[c]);
//					}
				}
			}
        }
	}

}
