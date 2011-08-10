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

import net.bashtech.geobot.JSONObjects.JTVStreamSummary;
import net.bashtech.geobot.JSONObjects.LastFmRecentTracks;
import net.bashtech.geobot.modules.BotModule;

import org.jibble.pircbot.*;

public class GeoBot extends PircBot {	
	//private Timer pjTimer;
	
	private BotManager botManager;
	
	//private Map<String,Long> previousCommands = new HashMap<String,Long>();
	
	private String[] linksMasks = {".*http://.*",".*(\\.|\\(dot\\))(com|org|net|tv|ca|xxx|cc|de|eu|fm|gov|info|io|jobs|me|mil|mobi|name|rn|tel|travel|tz|uk|co|us|be)(\\s+|/|$).*",
			".*(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\s+|:|/|$).*"};
	
	/*private String[] linksMasks = {".*http://.*",".*\\.com(\\s+|/).*",".*\\.org(\\s+|/).*",".*\\.net(\\s+|/).*",".*\\.tv(\\s+|/).*",".*\\.ca(\\s+|/).*",".*\\.xxx(\\s+|/).*",".*\\.cc(\\s+|/).*",".*\\.de(\\s+|/).*",
								   ".*\\.eu(\\s+|/).*",".*\\.fm(\\s+|/).*",".*\\.gov(\\s+|/).*",".*\\.info(\\s+|/).*",".*\\.io(\\s+|/).*",".*\\.jobs(\\s+|/).*",".*\\.me(\\s+|/).*",".*\\.mil(\\s+|/).*",
			                       ".*\\.mobi(\\s+|/).*",".*\\.name(\\s+|/).*",".*\\.rn(\\s+|/).*",".*\\.tel(\\s+|/).*",".*\\.travel(\\s+|/).*",".*\\.tz(\\s+|/).*",".*\\.uk(\\s+|/).*",".*\\.us(\\s+|/).*",".*\\.be(\\s+|/).*"};*/
	
	
	public GeoBot(BotManager bm, String server, int port){
		System.out.println("DEBUG: Bot created.");
		botManager = bm;
		
		this.setName(bm.nick);
		this.setLogin("GeoBot");
		this.setVersion("1.0");
		
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

//	public void onPrivateMessage(String sender, String login, String hostname, String message) {
//		String[] msg = split(message.trim());
//
//		if (msg[0].equalsIgnoreCase("!join") && msg.length > 1 && botManager.isAdmin(sender)) {
//			try {
//				if(msg[1].contains("#")){
//					sendMessage(sender, "Channel "+ msg[1] +" joining...");
//					botManager.addChannel(msg[1], msg[2]);
//					sendMessage(sender, "Channel "+ msg[1] +" joined.");
//				}else{
//					sendMessage(sender, "Invalid channel format. Must be in format #channelname.");
//				}
//				
//			} catch (NickAlreadyInUseException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IOException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			} catch (IrcException e) {
//				// TODO Auto-generated catch block
//				e.printStackTrace();
//			}
//		}
//		
//		if (msg[0].equalsIgnoreCase("!leave") && msg.length > 1 && botManager.isAdmin(sender)) {
//			if(msg[1].contains("#")){
//				sendMessage(sender, "Channel "+ msg[1] +" parting...");
//				botManager.removeChannel(split(message)[1]);
//				sendMessage(sender, "Channel "+ msg[1] +" parted.");
//			}else{
//				sendMessage(sender, "Invalid channel format. Must be in format #channelname.");
//			}
//				
//		}
//		
//		if (msg[0].equalsIgnoreCase("!rejoin") && botManager.isAdmin(sender)) {
//			sendMessage(sender, "Rejoining all channels.");
//			botManager.rejoinChannels();
//		}
//		
//		System.out.println("DEBUG: PM from " + sender + " message=" + message);
//		
//	}
	
	
	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message){
				//Call modules
				for(BotModule b:BotManager.getInstance().getModules()){
					b.onMessage(channel, sender, login, hostname, message);
				}
		
		
				//System.out.println("DEBUG: " + channel + " " + sender + " " + message);
				
				Channel channelInfo = botManager.getChannel(channel);
				
				if(channelInfo == null)
					return;
						
				//Ignore messages from self.
				if(sender.equalsIgnoreCase(this.getNick())){
					//System.out.println("Message from bot");
					return;
				}
				
				//Split message on spaces.
				String[] msg = message.trim().split(" ");
				
				//Try to match to user in userlist.
				User user = matchUser(sender, channel);
				if(user == null){
					user = new User("", sender);
				}
				
				// ********************************************************************************
				// ****************************** User Ranks **************************************
				// ********************************************************************************
				
				boolean isAdmin = false;
				boolean isOwner = false;
				boolean isOp = false;
				boolean isRegular = false;
				
				//Check user level based on IRC mode.
				try{
					if(user.getPrefix().equalsIgnoreCase("~"))
						isOwner = true;
					if(user.getPrefix().equalsIgnoreCase("@") || user.isOp())
						isOp = true;
					if(user.getPrefix().equalsIgnoreCase("+"))
						isRegular = true;
				}catch(Exception e){
					System.out.println("Prefix exception.");
				}
				
				//Check for user level based on other factors.
				if(botManager.isAdmin(sender))
					isAdmin = true;
				if(channel.equalsIgnoreCase("#" + sender))
					isOwner = true;
				if(channelInfo.isModerator(sender))
					isOp = true;
				if(channelInfo.isRegular(sender))
					isRegular = true;
				
				//Give users all the ranks below them
				if(isAdmin){
					System.out.println(sender + " is admin.");
					isOwner = true;
					isOp = true;
					isRegular = true;
				}else if(isOwner){
					System.out.println(sender + " is owner.");
					isOp = true;
					isRegular = true;
				}else if(isOp){
					System.out.println(sender + " is op.");
					isRegular = true;
				}else if(isRegular){
					System.out.println(sender + " is regular.");
				}

				// ********************************************************************************
				// ***************************** Poll Voting **************************************
				// ********************************************************************************
				if(channelInfo.getPoll() != null && channelInfo.getPoll().getStatus()){
					//Poll is open and accepting votes.
					channelInfo.getPoll().vote(sender, msg[0]);
					System.out.println("DEBUG: Voted.");
				}
				// ********************************************************************************
				// ***************************** Giveaway Voting **********************************
				// ********************************************************************************
				if(channelInfo.getGiveaway() != null && channelInfo.getGiveaway().getStatus()){
					//Giveaway is open and accepting entries.
					System.out.println("DEBUG: Attempting entry.");
					channelInfo.getGiveaway().submitEntry(sender, msg[0]);
				}
				// ********************************************************************************
				// ********************************************************************************
				// ********************************************************************************
				
				
				// ********************************************************************************
				// ********************************* Commands *************************************
				// ********************************************************************************
				
				// !time - All
				if (msg[0].equalsIgnoreCase("!time")) {
						System.out.println("Matched command !time");
						String time = new java.util.Date().toString();
						sendMessage(channel, sender + ": The time is now " + time);
						//return;
				}
				
				// !bothelp - All
				if (msg[0].equalsIgnoreCase("!bothelp")) {
						System.out.println("Matched command !bothelp");
						sendMessage(channel, "> Command help is available at http://bashtech.net/wiki/Geobot#Commands");
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
				
				// !music - All
				if (msg[0].equalsIgnoreCase("!music") || msg[0].equalsIgnoreCase("!lastfm")) {
					System.out.println("Matched command !music");
					try {
						sendMessage(channel, "> " + this.getLastFMLastPlayed(channelInfo));
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
				
				// !throw - All
				if((msg[0].equalsIgnoreCase("!throw") || msg[0].equalsIgnoreCase("!flip")) && (channelInfo.checkThrow() || isOp)){
					System.out.println("Matched command !throw");
					if(msg.length > 1){
						String throwMessage = "";
						for(int i=1;i<msg.length;i++){
							throwMessage += msg[i] + " ";
						}
						this.sendMessage(channel, "(╯°‿°）╯︵" + throwMessage);
					}
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
				
				// !command - Ops
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
	
				// ********************************************************************************
				// ***************************** Moderation Commands ******************************
				// ********************************************************************************
				
 				//Moderation commands - Ops
 				if(isOp && botManager.getInstance().network.equalsIgnoreCase("jtv")){
 					if(msg[0].equalsIgnoreCase("+m")){
 						sendMessage(channel, ".slow");
 					}
 					if(msg[0].equalsIgnoreCase("-m")){
 						sendMessage(channel, ".slowoff");
 					}
 					if(msg[0].equalsIgnoreCase("+f")){
 						sendMessage(channel, ".followers");
 					} 
 					if(msg[0].equalsIgnoreCase("-f")){
 						sendMessage(channel, ".followersoff");
 					} 
 					if(msg[0].equalsIgnoreCase("+s")){
 						sendMessage(channel, ".subscribers");
 					} 
 					if(msg[0].equalsIgnoreCase("-s")){
 						sendMessage(channel, ".subscribersoff");
 					} 
 					if(msg.length > 0){
 	 					if(msg[0].equalsIgnoreCase("+b")){
 	 						sendMessage(channel, ".ban " + msg[1]);
 	 					} 
 	 					if(msg[0].equalsIgnoreCase("-b")){
 	 						sendMessage(channel, ".unban " + msg[1]); 
 	 					} 
 	 					if(msg[0].equalsIgnoreCase("+k")){
 	 						sendMessage(channel, ".timeout " + msg[1]);
 	 					} 
 					}
 					
 				}
 				
				// !clear - Ops
				if(msg[0].equalsIgnoreCase("!clear") && isOp){
					System.out.println("Matched command !clear");
					this.sendMessage(channel, ".clear");
				}
 				
 				// !links - Owner
 				if(msg[0].equalsIgnoreCase("!links") && isOwner){
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
 				
 				
 				// !pd - Owner
 				if(msg[0].equalsIgnoreCase("!pd")){
 					System.out.println("Matched command !pd");
 					if(msg.length  > 2 && isOwner){
 						if(msg[1].equalsIgnoreCase("add")){
 							if(channelInfo.isDomainPermitted(msg[2])){
 								sendMessage(channel,"> Domain already exists. " + "(" + msg[2] + ")");
 							}else{
 								channelInfo.addPermittedDomain(msg[2]);
 								sendMessage(channel,"> Domain added. "+ "(" + msg[2] + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete")){
 							if(channelInfo.isDomainPermitted(msg[2])){
 								channelInfo.removePermittedDomain(msg[2]);
 								sendMessage(channel,"> Domain removed. "+ "(" + msg[2] + ")");
 							}else{
 								sendMessage(channel,"> Domain does not exist. "+ "(" + msg[2] + ")");
 							}
 						}
 					}else if(msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner){
 						String tempList = "> Permitted domains: ";
 						for(String s:channelInfo.getpermittedDomains()){
 							tempList += s + ", ";
 						}
 						sendMessage(channel, tempList);
 					}
 				}
				
 				// !caps - Owner
 				if(msg[0].equalsIgnoreCase("!caps") && isOwner){
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
 								channelInfo.setfilterCapsPercent(Integer.parseInt(msg[2]));
 	 							this.sendMessage(channel, "> Caps filter percent: " + channelInfo.getfilterCapsPercent());
 							}
 						}else if(msg[1].equalsIgnoreCase("minchars")){
 							if(msg.length > 2){
 								channelInfo.setfilterCapsMinCharacters(Integer.parseInt(msg[2]));
 	 							this.sendMessage(channel, "> Caps filter min characters: " + channelInfo.getfilterCapsMinCharacters());
 							}
 						}else if(msg[1].equalsIgnoreCase("mincaps")){
 							if(msg.length > 2){
 								channelInfo.setfilterCapsMinCapitals(Integer.parseInt(msg[2]));
 	 							this.sendMessage(channel, "> Caps filter min caps: " + channelInfo.getfilterCapsMinCapitals());
 							}
 						}else if(msg[1].equalsIgnoreCase("status")){
 							sendMessage(channel, "> Caps filter=" + channelInfo.getFilterCaps() + ", percent=" + channelInfo.getfilterCapsPercent() + ", minchars=" + channelInfo.getfilterCapsMinCharacters() + ", mincaps= " + channelInfo.getfilterCapsMinCapitals());
 						}
 					}
 				}
 				
 				// !regular - Owner
 				if(msg[0].equalsIgnoreCase("!regular")){
 					System.out.println("Matched command !regular");
 					if(msg.length  > 2 && isOwner){
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
 								sendMessage(channel,"> User does not exist. " + "(" + msg[2] + ")");
 							}
 						}
 					}else if(msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner){
 						String tempList = "> Regulars: ";
 						for(String s:channelInfo.getRegulars()){
 							tempList += s + ", ";
 						}
 						sendMessage(channel, tempList);
 					}
 				}
 				
 				// !mod - Owner
 				if(msg[0].equalsIgnoreCase("!mod")){
 					System.out.println("Matched command !mod");
 					if(msg.length  > 2 && isOwner){
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
 								sendMessage(channel,"> User does not exist. "+ "(" + msg[2] + ")");
 							}
 						}
 					}else if(msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner){
 						String tempList = "> Moderators: ";
 						for(String s:channelInfo.getModerators()){
 							tempList += s + ", ";
 						}
 						sendMessage(channel, tempList);
 					}
 				}
 				
 				// !set - Owner
 				if(msg[0].equalsIgnoreCase("!set")){
 					System.out.println("Matched command !set");
 					if(msg.length > 0 && isOwner){
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
						}else if(msg[1].equalsIgnoreCase("joinparts")){
 							//filters
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setAnnounceJoinParts(true);
 								sendMessage(channel, "> Feature: Join/Part announcing is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setAnnounceJoinParts(false);
 								sendMessage(channel, "> Feature: Join/Part announcing is off");
 							}
						}else if(msg[1].equalsIgnoreCase("lastfm")){
 							//filters
 							if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setLastfm("");
 								sendMessage(channel, "> Feature: Lastfm is off.");
 							}else{
 								channelInfo.setLastfm(msg[2]);
 								sendMessage(channel, "> Feature: Lastfm user set to " + msg[2]);
 							}
						}
 					}
 				}
 				
 				//!leave - Owner
 				if ((msg[0].equalsIgnoreCase("!leave") || msg[0].equalsIgnoreCase("!remove")) && isOwner) {
 						sendMessage(channel, "Channel "+ channel +" parting...");
 						botManager.removeChannel(channel);
 				}
 				
 				//!join
 				if (msg[0].equalsIgnoreCase("!join") && BotManager.getInstance().publicJoin) {

							sendMessage(channel, "Joining channel #"+ sender +".");
							boolean joinStatus = botManager.addChannel("#" + sender, null);
							if(joinStatus){
								sendMessage(channel, "Channel #"+ sender +" joined.");
							}else{
								sendMessage(channel, "Already in channel #"+ sender +".");
							}
				}
 				
 				if (msg[0].equalsIgnoreCase("!rejoin") && msg.length > 1) {

						if(msg[1].contains("#")){
							sendMessage(channel, "Rejoining channel "+ msg[1] +".");
							boolean joinStatus = botManager.rejoinChannel(msg[1]);
							if(joinStatus){
								sendMessage(channel, "Channel "+ msg[1] +" rejoined.");
							}else{
								sendMessage(channel, "Bot is not assigned to channel "+ msg[1] +"."); 							
							}
							
						}else{
							sendMessage(channel, "Invalid channel format. Must be in format #channelname.");
						}
						

				}
 				
				// ********************************************************************************
				// **************************** Administration Commands ***************************
				// ********************************************************************************
 				
 				if (msg[0].equalsIgnoreCase("!bm-join") && msg.length > 1 && isAdmin) {

 						if(msg[1].contains("#")){
 							sendMessage(channel, "Joining channel "+ msg[1] +".");
 							boolean joinStatus = botManager.addChannel(msg[1], (msg.length == 3 ? msg[2] : null));
 							if(joinStatus){
 								sendMessage(channel, "Channel "+ msg[1] +" joined.");
 							}else{
 								sendMessage(channel, "Already in channel "+ msg[1] +"."); 							
 							}
 							
 						}else{
 							sendMessage(channel, "Invalid channel format. Must be in format #channelname.");
 						}
 						
 
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-leave") && msg.length > 1 && isAdmin) {
 					if(msg[1].contains("#")){
 						sendMessage(channel, "Channel "+ msg[1] +" parting...");
 						botManager.removeChannel(msg[1]);
 						sendMessage(channel, "Channel "+ msg[1] +" parted.");
 					}else{
 						sendMessage(channel, "Invalid channel format. Must be in format #channelname.");
 					}
 						
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-rejoin") && isAdmin) {
 					sendMessage(channel, "Rejoining all channels.");
 					botManager.rejoinChannels();
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-softreconnect") && isAdmin) {
 					sendMessage(channel, "Reconnecting all servers.");
 					botManager.reconnectAllBotsSoft();
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-hardreconnect") && isAdmin) {
 					sendMessage(channel, "Reconnecting all servers.");
 					botManager.reconnectAllBotsHard();
 				}
 				
				// ********************************************************************************
				// ***************************** Info/Catch-all Command ***************************
				// ********************************************************************************
 				
				if(msg[0].substring(0,1).equalsIgnoreCase("!") && !channelInfo.getCommand(msg[0]).equalsIgnoreCase("invalid")){
					System.out.println("Matched command " + msg[0]);
					sendMessage(channel, "> " + channelInfo.getCommand(msg[0]));
				}
 				
				// ********************************************************************************
				// ********************************** Filters *************************************
				// ********************************************************************************
				
				//Filter feature check
 				if(!channelInfo.useFilters)
 					return;
 				
				// Cap filter
 				int capsNumber = getCapsNumber(message);
 				int capsPercent = getCapsPercent(message);
 				System.out.println("DEBUG: Message Length= " + message.length());
 				System.out.println("DEBUG: Caps percent= " + capsPercent);
 				System.out.println("DEBUG: Caps number= " + capsNumber);
				if(channelInfo.getFilterCaps() && !(isOp || isRegular) && message.length() >= channelInfo.getfilterCapsMinCharacters() && capsPercent >= channelInfo.getfilterCapsPercent() && capsNumber >= channelInfo.getfilterCapsMinCapitals()){
					if(botManager.network.equalsIgnoreCase("jtv")){
						this.sendMessage(channel, ".timeout " + sender + " 10");
					}else if(botManager.network.equalsIgnoreCase("ngame")){
						this.ban(channel, sender);
						tenSecondUnban(channel, sender);
					}else{
						this.kick(channel, sender, "Too many caps.");
						tenSecondUnban(channel, sender);
					}
					if(channelInfo.checkSignKicks())
						sendMessage(channel, "> " + sender + ", please don't shout or talk in all caps. (10 sec. timeout)");
				}
				
				// Link filter
				if(channelInfo.getFilterLinks() && !(isOp || isRegular) && this.containsLink(message,channelInfo) ){
					boolean result = channelInfo.linkPermissionCheck(sender);
					if(result){
						sendMessage(channel, "> Link permitted. (" + sender + ")" );
					}else{
						if(botManager.network.equalsIgnoreCase("jtv")){
							this.sendMessage(channel, ".timeout " + sender + " 10");
						}else if(botManager.network.equalsIgnoreCase("ngame")){
							this.ban(channel, sender);
							tenSecondUnban(channel, sender);
						}else{
							this.kick(channel, sender, "Unauthorized link.");
							tenSecondUnban(channel, sender);
						}
						
						if(channelInfo.checkSignKicks())
							sendMessage(channel, "> " + sender + ", please ask a moderator before posting links. (10 sec. timeout)");
					}
					
				}

			
			
	}

	
	@Override
	public void onDisconnect(){
		//pjTimer.cancel();

		try {
			System.out.println("Internal reconnection: " + this.getServer());
			String[] channels = this.getChannels();
			this.reconnect();
			for(int i=0;i<channels.length;i++){
				this.joinChannel(channels[i]);
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
	
    public void onJoin(String channel, String sender, String login, String hostname){ 
		//Call modules
		for(BotModule b:BotManager.getInstance().getModules()){
			b.onJoin(channel, sender, login, hostname);
		}
		
		Channel channelInfo = botManager.getChannel(channel);
		
		if(channelInfo == null || !channelInfo.getAnnounceJoinParts() || this.getNick().equalsIgnoreCase(sender))
			return;
		
		sendMessage(channel, "> " + sender + " entered the room.");
    }

    public void onPart(String channel, String sender, String login, String hostname) {
		//Call modules
		for(BotModule b:BotManager.getInstance().getModules()){
			b.onPart(channel, sender, login, hostname);
		}
		
		Channel channelInfo = botManager.getChannel(channel);
		
		if(channelInfo == null || !channelInfo.getAnnounceJoinParts() || this.getNick().equalsIgnoreCase(sender))
			return;
		
		sendMessage(channel, "> " + sender + " left the room.");
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
	
	private boolean containsLink(String message, Channel ch){
		String [] splitMessage  = message.toLowerCase().split(" ");
		for(String m: splitMessage){
			for(String mask: linksMasks){
				if(m.matches(mask)){
					System.out.println("DEBUG: Link match on " + mask);
					if(!ch.checkPermittedDomain(m))
						return true;	
				}
			}
		}

		
		return false;
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
	
	private String getLastFMLastPlayed(Channel channelInfo) throws IOException{
		if(channelInfo.getLastfm().length() < 1)
			return "Function not configured.";
		
		URL url = new URL("http://bashtech.net/app-support/geobot/lastfm.php?action=lastplayed&user=" + channelInfo.getLastfm());
		URLConnection conn = url.openConnection();
		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));
		String jsonIn = "";
		while(d.ready())
		{
			jsonIn = d.readLine();
		}
		
//		mapper = new ObjectMapper();
//		Map<String,Object> userData = mapper.readValue(jsonIn, Map.class);
//		Map second = (Map) userData.get("recenttracks");
		
		LastFmRecentTracks data = new Gson().fromJson(jsonIn, LastFmRecentTracks.class);
		
		if(data.playing == true){
			return "Listening to: " + data.title + " by " + data.artist + " " + data.url;
		}else{
			return "Recently listened to: " + data.title + " by " + data.artist + " " + data.url;
		}
			
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
