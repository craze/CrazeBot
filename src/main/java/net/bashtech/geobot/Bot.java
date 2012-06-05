package net.bashtech.geobot;
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetAddress;
import java.net.URL;
import java.net.URLConnection;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


import com.google.gson.Gson;
import net.bashtech.geobot.JSONObjects.JTVStreamSummary;
import net.bashtech.geobot.JSONObjects.LastFmRecentTracks;
import net.bashtech.geobot.JSONObjects.SteamData;
import net.bashtech.geobot.modules.BotModule;

import org.jibble.pircbot.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class Bot extends PircBot {	

	private BotManager botManager;
	private Pattern[] linkPatterns;
	private int lastPing = -1;
	private Set<String> tmiServers;
	private int[] warningTODuration = {10, 30, 60, 600};
	private String[] warningText = {"first warning (10 sec t/o)", "second warning (30 sec t/o)", "final warning (1 min t/o)", "(10 min timeout)"};
	
	public Bot(BotManager bm, String server, int port){
		botManager = bm;
		
		linkPatterns = new Pattern[3];
		linkPatterns[0] = Pattern.compile(".*http://.*");
		linkPatterns[1] = Pattern.compile(".*(\\.|\\(dot\\))(com|org|net|tv|ca|xxx|cc|de|eu|fm|gov|info|io|jobs|me|mil|mobi|name|rn|tel|travel|tz|uk|co|us|be|sh|ly|in)(\\s+|/|$).*");
		linkPatterns[2] = Pattern.compile(".*(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\s+|:|/|$).*");
		
		tmiServers = new HashSet<String>();
		tmiServers.add("199.9.250.142");
		tmiServers.add("199.9.250.146");
		tmiServers.add("199.9.250.147");
		
		
		this.setName(bm.nick);
		this.setLogin("GeoBot");
		this.setVersion("2.0");
		
		this.setVerbose(botManager.verboseLogging);
		try {
			this.connect(server, port, bm.password);
		} catch (NickAlreadyInUseException e) {
			System.out.println("ERROR: Nickname already in use - " + this.getNick() + " " + this.getServer());
		} catch (IOException e) {
			System.out.println("ERROR: Unable to connect to server - " + this.getNick() + " " + this.getServer());
		} catch (IrcException e) {
			System.out.println("ERROR: Error connecting to server - " + this.getNick() + " " + this.getServer());
		}
	}
	
	@Override
	protected void onConnect() {
		//Force TMI to send USERCOLOR AND SPECIALUSER messages.
		this.sendRawLine("JTVCLIENT");
	}
	
	

	@Override
	protected void onPrivateMessage(String sender, String login,
			String hostname, String message) {
		String[] msg = message.trim().split(" ");
		
		if(msg.length > 0){
			if(msg[0].equalsIgnoreCase("SPECIALUSER")){
				String user = msg[1];
				String tag = msg[2];
				
				if(tag.equalsIgnoreCase("admin") || tag.equalsIgnoreCase("staff"))
					botManager.addTagAdmin(user);
			}else if(msg[0].equalsIgnoreCase("USERCOLOR")){
				String user = msg[1];
				String color = msg[2];
			}else if(msg[0].equalsIgnoreCase("CLEARCHAT")){
				if(msg.length > 1){
					String user = msg[1];
					if(!botManager.verboseLogging)
						System.out.println("RAW: CLEARCHAT " + user);
				}else{
					if(!botManager.verboseLogging)
						System.out.println("RAW: CLEARCHAT");
				}

			}
		}
	}

	@Override
	protected void onAction(String sender, String login, String hostname, String target, String action) {
		this.onMessage(target, sender, login, hostname, action);
	}

	@Override
	public void onMessage(String channel, String sender, String login, String hostname, String message){
				if(!botManager.verboseLogging)
					System.out.println("MESSAGE: " + channel + " " + sender + " : " + message);
				
				Channel channelInfo = botManager.getChannel(channel);
				
				if(channelInfo == null)
					return;
				
				//Call modules
				for(BotModule b:botManager.getModules()){
					b.onMessage(channelInfo, sender, login, hostname, message);
				}
						
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
				}catch(Exception e){}
				
				//Check for user level based on other factors.
				if(botManager.isAdmin(sender))
					isAdmin = true;
				if(botManager.isTagAdmin(sender))
					isAdmin = true;
				if(botManager.network.equalsIgnoreCase("jtv") && channel.equalsIgnoreCase("#" + sender))
					isOwner = true;
				if(channelInfo.isModerator(sender))
					isOp = true;
				if(channelInfo.isOwner(sender))
					isOwner = true;
				if(channelInfo.isRegular(sender))
					isRegular = true;
				
				//Give users all the ranks below them
				if(isAdmin){
					System.out.println("DEBUG: " + sender + " is admin.");
					isOwner = true;
					isOp = true;
					isRegular = true;
				}else if(isOwner){
					System.out.println("DEBUG: " + sender + " is owner.");
					isOp = true;
					isRegular = true;
				}else if(isOp){
					System.out.println("DEBUG: " + sender + " is op.");
					isRegular = true;
				}else if(isRegular){
					System.out.println("DEBUG: " + sender + " is regular.");
				}
				
				
				//!leave - Owner
 				if ((msg[0].equalsIgnoreCase("!leave") || msg[0].equalsIgnoreCase("!remove")) && isOwner) {
 						sendMessage(channel, "Channel "+ channel +" parting...");
 						botManager.removeChannel(channel);
 				}
 				
 				
				// ********************************************************************************
				// ********************************** Filters *************************************
				// ********************************************************************************
				
 				//Global banned word filter
 				if(!isOp && this.isGlobalBannedWord(message)){
 					this.secondaryTO(channel, sender, 600);
 					System.out.println("NOTICE: Global banned word timeout: " + sender + " in " + channel + " : " + message);
 				}
 					
 				
				//Filter feature check
 				if(channelInfo.useFilters){
					// Cap filter
					int capsNumber = getCapsNumber(message);
					int capsPercent = getCapsPercent(message);
					//System.out.println("DEBUG: Message Length= " + message.length());
					//System.out.println("DEBUG: Caps percent= " + capsPercent);
					//System.out.println("DEBUG: Caps number= " + capsNumber);
					if(channelInfo.getFilterCaps() && !(isOp) && message.length() >= channelInfo.getfilterCapsMinCharacters() && capsPercent >= channelInfo.getfilterCapsPercent() && capsNumber >= channelInfo.getfilterCapsMinCapitals()){
						int warningCount = 0;
						if(botManager.network.equalsIgnoreCase("jtv")){
							channelInfo.incWarningCount(sender, FilterType.CAPS);
							warningCount = channelInfo.getWarningCount(sender, FilterType.CAPS);
							
							//this.sendMessage(channel, ".timeout " + sender + " " + getWarningTODuration(warningCount));
							this.secondaryTO(channel, sender, this.getWarningTODuration(warningCount));
						}else{
							this.kick(channel, sender, "Too many caps");
						}
						if(channelInfo.checkSignKicks())
							sendMessage(channel, channelInfo.getBullet() +  " " + sender + ", please don't shout or talk in all caps - " + this.getWarningText(warningCount));
					}
					
					// Link filter
					if(channelInfo.getFilterLinks() && !(isOp || isRegular) && this.containsLink(message,channelInfo) ){
						boolean result = channelInfo.linkPermissionCheck(sender);
						int warningCount = 0;
						if(result){
							sendMessage(channel, "> Link permitted. (" + sender + ")" );
						}else{
							if(botManager.network.equalsIgnoreCase("jtv")){
								channelInfo.incWarningCount(sender, FilterType.LINK);
								warningCount = channelInfo.getWarningCount(sender, FilterType.LINK);
								
								//this.sendMessage(channel, ".timeout " + sender + " " + this.getWarningTODuration(warningCount));
								this.secondaryTO(channel, sender, this.getWarningTODuration(warningCount));
							}else{
								this.kick(channel, sender, "Unauthorized link");
							}
							
							if(channelInfo.checkSignKicks())
								sendMessage(channel, channelInfo.getBullet() +  " " + sender + ", please ask a moderator before posting links - " + this.getWarningText(warningCount));
						}
						
					}
					
					//Offensive filter
					if(!isOp && channelInfo.getFilterOffensive()){
						if(channelInfo.isOffensive(message)){
							int warningCount = 0;
							if(botManager.network.equalsIgnoreCase("jtv")){
								channelInfo.incWarningCount(sender, FilterType.OFFENSIVE);
								warningCount = channelInfo.getWarningCount(sender, FilterType.OFFENSIVE);
								
								//this.sendMessage(channel, ".timeout " + sender + " " + this.getWarningTODuration(warningCount));
								this.secondaryTO(channel, sender, this.getWarningTODuration(warningCount));
							}else{
								this.kick(channel, sender, "Offensive language");
							}
						}
					}
					
					//Emote filter
					if(!isOp && channelInfo.getFilterEmotes()){
						if(countEmotes(message) > channelInfo.getFilterEmotesMax()){
							int warningCount = 0;
							if(botManager.network.equalsIgnoreCase("jtv")){
								channelInfo.incWarningCount(sender, FilterType.EMOTES);
								warningCount = channelInfo.getWarningCount(sender, FilterType.EMOTES);
								
								//this.sendMessage(channel, ".timeout " + sender + " " + this.getWarningTODuration(warningCount));
								this.secondaryTO(channel, sender, this.getWarningTODuration(warningCount));
								
								if(channelInfo.checkSignKicks())
									sendMessage(channel, channelInfo.getBullet() +  " " + sender + ", please don't spam emotes - " + this.getWarningText(warningCount));
							}
							
							
						}	
					}
 				}

				
				//Check channel mode.
				if(channelInfo.getMode() == 0 && !isOwner)
					return;
				if(channelInfo.getMode() == 1 && !isOp)
					return;

				// ********************************************************************************
				// ***************************** Poll Voting **************************************
				// ********************************************************************************
				if(channelInfo.getPoll() != null && channelInfo.getPoll().getStatus()){
					//Poll is open and accepting votes.
					channelInfo.getPoll().vote(sender, msg[0]);
				}
				// ********************************************************************************
				// ***************************** Giveaway Voting **********************************
				// ********************************************************************************
				if(channelInfo.getGiveaway() != null && channelInfo.getGiveaway().getStatus()){
					//Giveaway is open and accepting entries.
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
						System.out.println("DEBUG: Matched command !time");
						String time = new java.util.Date().toString();
						sendMessage(channel, sender + ": The time is now " + time);
						//return;
				}
				
				// !bothelp - All
				if (msg[0].equalsIgnoreCase("!bothelp")) {
						System.out.println("DEBUG: Matched command !bothelp");
						sendMessage(channel, channelInfo.getBullet() + " Command help is available at http://bashtech.net/wiki/Geobot#Commands");
						//return;
				}
				
				// !viewers - All
				if (msg[0].equalsIgnoreCase("!viewers") || msg[0].equalsIgnoreCase("!lurkers")) {
					if(!botManager.network.equalsIgnoreCase("jtv"))
						return;
					System.out.println("DEBUG: Matched command !viewers");
					try {
						sendMessage(channel, channelInfo.getBullet() + " " + this.getViewers(channelInfo) + " viewers.");
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//return;
				}
				
//				// !bitrate - All
//				if (msg[0].equalsIgnoreCase("!bitrate")) {
//					if(!botManager.network.equalsIgnoreCase("jtv"))
//						return;
//					System.out.println("DEBUG: Matched command !bitrate");
//					try {
//						sendMessage(channel, channelInfo.getBullet() + " Streaming at " + this.getBitRate(channelInfo) + " Kbps.");
//					} catch (IOException e) {
//						// TODO Auto-generated catch block
//						e.printStackTrace();
//					}
//					//return;
//				}
				
				// !music - All
				if (msg[0].equalsIgnoreCase("!music") || msg[0].equalsIgnoreCase("!lastfm")) {
					System.out.println("DEBUG: Matched command !music");
					try {
						sendMessage(channel, channelInfo.getBullet() + " " + this.getLastFMLastPlayed(channelInfo));
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//return;
				}
				
				// !steam - All
				if (msg[0].equalsIgnoreCase("!steam")) {
					System.out.println("DEBUG: Matched command !steam");
					if(channelInfo.getSteam().length() > 1){
						try {
						if(channelInfo.getSteam().length() > 1){
							SteamData data = this.getSteamData(channelInfo);
							sendMessage(channel, channelInfo.getBullet() + " Steam Profile: " + data.profileurl + (data.game != null ? ", Game: " + data.game : "") + (data.server != null ? ", Server: " + data.server : "") );
							}
						} catch (IOException e) {
							// TODO Auto-generated catch block
							e.printStackTrace();
						}
					}else{
						sendMessage(channel, channelInfo.getBullet() + " Function not configured.");
					}
					
				}
				
				// !game - All
				if (msg[0].equalsIgnoreCase("!game")) {
					System.out.println("DEBUG: Matched command !game");
					
					String game = this.getGame(channelInfo);
					
					if(game.length() > 0){
						sendMessage(channel, channelInfo.getBullet() + " Current game: " + game);
					}else{
						sendMessage(channel, channelInfo.getBullet() + " Unable to query TwitchTV or channel is not in gaming category.");

					}
					
					//return;
				}
				
				// !status - All
				if (msg[0].equalsIgnoreCase("!status")) {
					System.out.println("DEBUG: Matched command !status");
					
					String status = this.getStatus(channelInfo);
					
					if(status.length() > 0){
						sendMessage(channel, channelInfo.getBullet() + " Current status: " + status);
					}else{
						sendMessage(channel, channelInfo.getBullet() + " Unable to query TwitchTV API.");

					}
					
					//return;
				}
				
				// !{botname} - All
				if (msg[0].equalsIgnoreCase("!" + this.getNick()) && (isRegular || isOp)) {
					System.out.println("DEBUG: Matched command !" + this.getNick());
					sendMessage(channel, channelInfo.getBullet() + " Commands: " + channelInfo.getCommandList());

					//return;
				}
				
				// !throw - All
				if((msg[0].equalsIgnoreCase("!throw") || msg[0].equalsIgnoreCase("!flip")) && (channelInfo.checkThrow() || isOp)){
					System.out.println("DEBUG: Matched command !throw");
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
					System.out.println("DEBUG: Matched command !topic");
					if(msg.length < 2 || !isOp){
						if(channelInfo.getTopic().equalsIgnoreCase("")){
							sendMessage(channel, channelInfo.getBullet() + " No topic is set.");
						}else{
							this.sendMessage(channel, channelInfo.getBullet() + " Topic: " + channelInfo.getTopic() + " (Set " + channelInfo.getTopicTime() + " ago)");
						}
					}else if(msg.length > 1 && isOp){
						if(msg[1].equalsIgnoreCase("unset")){
							channelInfo.setTopic("");
							sendMessage(channel, channelInfo.getBullet() + " No topic is set.");
						}else{
							channelInfo.setTopic(message.substring(7));
							this.sendMessage(channel, channelInfo.getBullet() + " Topic: " + channelInfo.getTopic() + " (Set " + channelInfo.getTopicTime() + " ago)");
						}

					}
					//return;
				}
				
				// !command - Ops
 				if(msg[0].equalsIgnoreCase("!command")){
 					System.out.println("DEBUG: Matched command !command");
					if(msg.length < 3 && isOp){
						this.sendMessage(channel, channelInfo.getBullet() + " !command add/delete name string");
					}else if(msg.length > 2 && isOp){
						if(msg[1].equalsIgnoreCase("add") && msg.length > 3){
							String key = "!" + msg[2];
							String value = fuseArray(msg, 3);
							if(!value.contains(",,")){
								channelInfo.setCommand(key, value);
								this.sendMessage(channel, channelInfo.getBullet() + " " + channelInfo.getCommand(key));
							}else{
								sendMessage(channel, channelInfo.getBullet() + " Command cannot contain double commas (\",,\").");
							}
								
						}else if(msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")){
							String key = "!" + msg[2];
							channelInfo.removeCommand(key);	
							this.sendMessage(channel, channelInfo.getBullet() + " Command " + key + " removed.");

							}
					}
				}
 				
				// !poll - Ops
				if(msg[0].equalsIgnoreCase("!poll") && isOp){
					System.out.println("DEBUG: Matched command !poll");
					if(msg.length >= 2){
						if(msg[1].equalsIgnoreCase("create")){
							String[] options = new String[msg.length - 2];
							int oc = 0;
							for(int c=2;c<msg.length;c++){
								options[oc] = msg[c];
								oc++;
							}
							channelInfo.setPoll(new Poll(options));
							sendMessage(channel, channelInfo.getBullet() + " Poll created. Do '!poll start' to start voting.");
						}else if(msg[1].equalsIgnoreCase("start")){
							if(channelInfo.getPoll() != null){
								if(channelInfo.getPoll().getStatus()){
									sendMessage(channel, channelInfo.getBullet() + " Poll is alreay running.");
								}else{
									channelInfo.getPoll().setStatus(true);
									sendMessage(channel, channelInfo.getBullet() + " Poll started.");
								}
							}
						}else if(msg[1].equalsIgnoreCase("stop")){ 
							if(channelInfo.getPoll() != null){
								if(channelInfo.getPoll().getStatus()){
									channelInfo.getPoll().setStatus(false);
									sendMessage(channel, channelInfo.getBullet() + " Poll stopped.");
								}else{
									sendMessage(channel, channelInfo.getBullet() + " Poll is not running.");
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
					System.out.println("DEBUG: Matched command !giveaway");
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
								sendMessage(channel,channelInfo.getBullet() + " Giveaway created. Do '!giveaway start' to start." + " Range 1-" + channelInfo.getGiveaway().getMax() + ".");
							}
							//sendMessage(channel,"> Giveaway created. Do '!giveaway start' to start." + " Range 1-" + channelInfo.getGiveaway().getMax() + ".");
							
						}else if(msg[1].equalsIgnoreCase("start")){
							if(channelInfo.getGiveaway() != null){
								if(channelInfo.getGiveaway().getStatus()){
									sendMessage(channel, channelInfo.getBullet() + " Giveaway is alreay running.");
								}else{
									channelInfo.getGiveaway().setStatus(true);
									sendMessage(channel, channelInfo.getBullet() + " Giveaway started.");
								}
							}
						}else if(msg[1].equalsIgnoreCase("stop")){ 
							if(channelInfo.getGiveaway() != null){
								if(channelInfo.getGiveaway().getStatus()){
									channelInfo.getGiveaway().setStatus(false);
									sendMessage(channel, channelInfo.getBullet() + " Giveaway stopped.");
								}else{
									sendMessage(channel, channelInfo.getBullet() + " Giveaway is not running.");
								}
							}
						}else if(msg[1].equalsIgnoreCase("results")){
							if(channelInfo.getGiveaway() != null){
								String[] results = channelInfo.getGiveaway().getResults();
								for(int c=0;c<results.length;c++){
									sendMessage(channel, results[c]);
								}
							}else{
								sendMessage(channel, channelInfo.getBullet() + " No giveaway in memory.");
							}
						
					   }
					}
				}
				
				// !random - Ops
				if(msg[0].equalsIgnoreCase("!random")&& isOp){
					System.out.println("DEBUG: Matched command !random");
					if(msg.length >= 2){
						if(msg[1].equalsIgnoreCase("user")){
							User[] userList = this.getUsers(channel);
							if(userList.length > 0){
								Random rand = new Random();
								int randIndex = rand.nextInt(userList.length);
								sendMessage(channel,channelInfo.getBullet() + " Random user: " + userList[randIndex].getNick());
							}
						}else if(msg[1].equalsIgnoreCase("coin")){
							Random rand = new Random();
							boolean coin = rand.nextBoolean();
							if(coin == true)
								sendMessage(channel,channelInfo.getBullet() + " Heads!");
							else
								sendMessage(channel,channelInfo.getBullet() + " Tails!");
						}
					}
				}
	
				// ********************************************************************************
				// ***************************** Moderation Commands ******************************
				// ********************************************************************************
				
 				//Moderation commands - Ops
 				if(isOp && botManager.network.equalsIgnoreCase("jtv")){
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
 	 						sendMessage(channel, ".ban " + msg[1].toLowerCase());
 	 					} 
 	 					if(msg[0].equalsIgnoreCase("-b")){
 	 						sendMessage(channel, ".unban " + msg[1].toLowerCase()); 
 	 					} 
 	 					if(msg[0].equalsIgnoreCase("+k")){
 	 						sendMessage(channel, ".timeout " + msg[1].toLowerCase());
 	 					}
 	 					if(msg[0].equalsIgnoreCase("+p")){
 	 						sendMessage(channel, ".timeout " + msg[1].toLowerCase() + " 10");
 	 					}
 	 					if(msg[0].equalsIgnoreCase("+pp")){
 	 						sendMessage(channel, ".ban " + msg[1].toLowerCase());
 	 						sendMessage(channel, ".unban " + msg[1].toLowerCase());
 	 					}
 					}
 					
 				}
 				
				// !clear - Ops
				if(msg[0].equalsIgnoreCase("!clear") && isOp){
					System.out.println("DEBUG: Matched command !clear");
					this.sendMessage(channel, ".clear");
				}
 				
 				// !links - Owner
 				if(msg[0].equalsIgnoreCase("!links") && isOwner){
 					System.out.println("DEBUG: Matched command !links");
 					if(msg.length == 2){
 						if(msg[1].equalsIgnoreCase("on")){
 							channelInfo.setFilterLinks(true);
 							this.sendMessage(channel, channelInfo.getBullet() + " Link filter: " + channelInfo.getFilterLinks());
 						}else if(msg[1].equalsIgnoreCase("off")){
 							channelInfo.setFilterLinks(false);
 							this.sendMessage(channel, channelInfo.getBullet() + " Link filter: " + channelInfo.getFilterLinks());
 						}
 					}
 				}
 				
 				// !permit - Allows users to post 1 link
 				if(msg[0].equalsIgnoreCase("!permit") && channelInfo.getFilterLinks() && channelInfo.useFilters){
 					System.out.println("DEBUG: Matched command !permit");
 					if(msg.length > 1 && isOp ){
 						if(!channelInfo.isRegular(msg[1])){
 							channelInfo.permitUser(msg[1]);
 	 						sendMessage(channel, channelInfo.getBullet() + " " + msg[1] + " may now post 1 link.");
 						}else{
 							sendMessage(channel, channelInfo.getBullet() + " " + msg[1] + " is a regular and does not need to be permitted.");
 						}
 					}
 				}
 				
 				
 				// !pd - Owner
 				if(msg[0].equalsIgnoreCase("!pd")){
 					System.out.println("DEBUG: Matched command !pd");
 					if(msg.length  > 2 && isOwner){
 						if(msg[1].equalsIgnoreCase("add")){
 							if(channelInfo.isDomainPermitted(msg[2])){
 								sendMessage(channel,channelInfo.getBullet() + " Domain already exists. " + "(" + msg[2] + ")");
 							}else{
 								channelInfo.addPermittedDomain(msg[2]);
 								sendMessage(channel,channelInfo.getBullet() + " Domain added. "+ "(" + msg[2] + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")){
 							if(channelInfo.isDomainPermitted(msg[2])){
 								channelInfo.removePermittedDomain(msg[2]);
 								sendMessage(channel,channelInfo.getBullet() + " Domain removed. "+ "(" + msg[2] + ")");
 							}else{
 								sendMessage(channel,channelInfo.getBullet() + " Domain does not exist. "+ "(" + msg[2] + ")");
 							}
 						}
 					}else if(msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner){
 						String tempList = channelInfo.getBullet() + " Permitted domains: ";
 						for(String s:channelInfo.getpermittedDomains()){
 							tempList += s + ", ";
 						}
 						sendMessage(channel, tempList);
 					}
 				}
 				
 				// !offensive - Owner
 				if(msg[0].equalsIgnoreCase("!offensive")){
 					System.out.println("DEBUG: Matched command !offensive");
 					if(msg.length > 1 && isOwner){
 						if(msg[1].equalsIgnoreCase("on")){
							channelInfo.setFilterOffensive(true);
							sendMessage(channel, channelInfo.getBullet() + " Offensive word filter is on");
 						}else if(msg[1].equalsIgnoreCase("off")){
							channelInfo.setFilterOffensive(false);
							sendMessage(channel, channelInfo.getBullet() + " Offensive word filter is off");
 						}
 					}else if(msg.length  > 2 && isOwner){
 						if(msg[1].equalsIgnoreCase("add")){
 							String phrase = fuseArray(msg, 2);
 							if(phrase.contains(",,")){
 								sendMessage(channel,channelInfo.getBullet() + " Cannot contain double commas (,,)");
 							}else if(channelInfo.isOffensive(fuseArray(msg, 2))){
 								sendMessage(channel,channelInfo.getBullet() + " Word already exists. " + "(" + phrase + ")");
 							}else{
 								channelInfo.addOffensive(phrase);
 								sendMessage(channel,channelInfo.getBullet() + " Word added. "+ "(" + phrase + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")){
 							String phrase = fuseArray(msg, 2);
 							if(channelInfo.isOffensive(phrase)){
 								channelInfo.removeOffensive(phrase);
 								sendMessage(channel,channelInfo.getBullet() + " Word removed. "+ "(" + phrase + ")");
 							}else{
 								sendMessage(channel,channelInfo.getBullet() + " Word does not exist. "+ "(" + phrase + ")");
 							}
 						}
 					}else if(msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner){
 						String tempList = channelInfo.getBullet() + " Offsenive words: ";
 						for(String s:channelInfo.getOffensive()){
 							tempList += s + ", ";
 						}
 						sendMessage(channel, tempList);
 					}
 				}
				
 				// !caps - Owner
 				if(msg[0].equalsIgnoreCase("!caps") && isOwner){
 					System.out.println("DEBUG: Matched command !caps");
 					if(msg.length > 1){
 						if(msg[1].equalsIgnoreCase("on")){
 							channelInfo.setFilterCaps(true);
 							this.sendMessage(channel, channelInfo.getBullet() + " Caps filter: " + channelInfo.getFilterCaps());
 						}else if(msg[1].equalsIgnoreCase("off")){
 							channelInfo.setFilterCaps(false);
 							this.sendMessage(channel, channelInfo.getBullet() + " Caps filter: " + channelInfo.getFilterCaps());
 						}else if(msg[1].equalsIgnoreCase("percent")){
 							if(msg.length > 2){
 								channelInfo.setfilterCapsPercent(Integer.parseInt(msg[2]));
 	 							this.sendMessage(channel, channelInfo.getBullet() + " Caps filter percent: " + channelInfo.getfilterCapsPercent());
 							}
 						}else if(msg[1].equalsIgnoreCase("minchars")){
 							if(msg.length > 2 && isInteger(msg[2])){
 								channelInfo.setfilterCapsMinCharacters(Integer.parseInt(msg[2]));
 	 							this.sendMessage(channel, channelInfo.getBullet() + " Caps filter min characters: " + channelInfo.getfilterCapsMinCharacters());
 							}
 						}else if(msg[1].equalsIgnoreCase("mincaps")){
 							if(msg.length > 2 && isInteger(msg[2])){
 								channelInfo.setfilterCapsMinCapitals(Integer.parseInt(msg[2]));
 	 							this.sendMessage(channel, channelInfo.getBullet() + " Caps filter min caps: " + channelInfo.getfilterCapsMinCapitals());
 							}
 						}else if(msg[1].equalsIgnoreCase("status")){
 							sendMessage(channel, channelInfo.getBullet() + " Caps filter=" + channelInfo.getFilterCaps() + ", percent=" + channelInfo.getfilterCapsPercent() + ", minchars=" + channelInfo.getfilterCapsMinCharacters() + ", mincaps= " + channelInfo.getfilterCapsMinCapitals());
 						}
 					}
 				}
 				
 				// !emotes - Owner
 				if(msg[0].equalsIgnoreCase("!emotes") && isOwner){
 					System.out.println("DEBUG: Matched command !emotes");
 					if(msg.length > 1){
 						if(msg[1].equalsIgnoreCase("on")){
 							channelInfo.setFilterEmotes(true);
 							this.sendMessage(channel, channelInfo.getBullet() + " Emotes filter: " + channelInfo.getFilterEmotes());
 						}else if(msg[1].equalsIgnoreCase("off")){
 							channelInfo.setFilterEmotes(false);
 							this.sendMessage(channel, channelInfo.getBullet() + " Emotes filter: " + channelInfo.getFilterEmotes());
 						}else if(msg[1].equalsIgnoreCase("max")){
 							if(msg.length > 2 && isInteger(msg[2])){
 								channelInfo.setFilterEmotesMax(Integer.parseInt(msg[2]));
 	 							this.sendMessage(channel, channelInfo.getBullet() + " Emotes filter max: " + channelInfo.getFilterEmotesMax());
 							}
 						}else if(msg[1].equalsIgnoreCase("status")){
 							sendMessage(channel, channelInfo.getBullet() + " Emotes filter=" + channelInfo.getFilterEmotes() + ", max=" + channelInfo.getFilterEmotesMax());
 						}
 					}
 				}
 				
 				// !regular - Owner
 				if(msg[0].equalsIgnoreCase("!regular")){
 					System.out.println("DEBUG: Matched command !regular");
 					if(msg.length  > 2 && isOwner){
 						if(msg[1].equalsIgnoreCase("add")){
 							if(channelInfo.isRegular(msg[2])){
 								sendMessage(channel,channelInfo.getBullet() + " User already exists." + "(" + msg[2] + ")");
 							}else{
 								channelInfo.addRegular(msg[2]);
 								sendMessage(channel,channelInfo.getBullet() + " User added. " + "(" + msg[2] + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")){
 							if(channelInfo.isRegular(msg[2])){
 								channelInfo.removeRegular(msg[2]);
 								sendMessage(channel,channelInfo.getBullet() + " User removed." + "(" + msg[2] + ")");
 							}else{
 								sendMessage(channel,channelInfo.getBullet() + " User does not exist. " + "(" + msg[2] + ")");
 							}
 						}
 					}else if(msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner){
 						String tempList = channelInfo.getBullet() + " Regulars: ";
 						for(String s:channelInfo.getRegulars()){
 							tempList += s + ", ";
 						}
 						sendMessage(channel, tempList);
 					}
 				}
 				
 				// !mod - Owner
 				if(msg[0].equalsIgnoreCase("!mod")){
 					System.out.println("DEBUG: Matched command !mod");
 					if(msg.length  > 2 && isOwner){
 						if(msg[1].equalsIgnoreCase("add")){
 							if(channelInfo.isModerator(msg[2])){
 								sendMessage(channel,channelInfo.getBullet() + " User already exists. " + "(" + msg[2] + ")");
 							}else{
 								channelInfo.addModerator(msg[2]);
 								sendMessage(channel,channelInfo.getBullet() + " User added. "+ "(" + msg[2] + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")){
 							if(channelInfo.isModerator(msg[2])){
 								channelInfo.removeModerator(msg[2]);
 								sendMessage(channel,channelInfo.getBullet() + " User removed. "+ "(" + msg[2] + ")");
 							}else{
 								sendMessage(channel,channelInfo.getBullet() + " User does not exist. "+ "(" + msg[2] + ")");
 							}
 						}
 					}else if(msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner){
 						String tempList = channelInfo.getBullet() + " Moderators: ";
 						for(String s:channelInfo.getModerators()){
 							tempList += s + ", ";
 						}
 						sendMessage(channel, tempList);
 					}
 				}
 				
 				// !owner - Owner
 				if(msg[0].equalsIgnoreCase("!owner")){
 					System.out.println("DEBUG: Matched command !owner");
 					if(msg.length  > 2 && isOwner){
 						if(msg[1].equalsIgnoreCase("add")){
 							if(channelInfo.isOwner(msg[2])){
 								sendMessage(channel,channelInfo.getBullet() + " User already exists. " + "(" + msg[2] + ")");
 							}else{
 								channelInfo.addOwner(msg[2]);
 								sendMessage(channel,channelInfo.getBullet() + " User added. "+ "(" + msg[2] + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")){
 							if(channelInfo.isOwner(msg[2])){
 								channelInfo.removeOwner(msg[2]);
 								sendMessage(channel,channelInfo.getBullet() + " User removed. "+ "(" + msg[2] + ")");
 							}else{
 								sendMessage(channel,channelInfo.getBullet() + " User does not exist. "+ "(" + msg[2] + ")");
 							}
 						}
 					}else if(msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner){
 						String tempList = channelInfo.getBullet() + " Owners: ";
 						for(String s:channelInfo.getOwners()){
 							tempList += s + ", ";
 						}
 						sendMessage(channel, tempList);
 					}
 				}
 				
 				// !set - Owner
 				if(msg[0].equalsIgnoreCase("!set")){
 					System.out.println("DEBUG: Matched command !set");
 					if(msg.length > 0 && isOwner){
 						if(msg.length == 1){
 							//Display current settings
 						}else if(msg[1].equalsIgnoreCase("topic")){
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setTopicFeature(true);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Topic is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setTopicFeature(false);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Topic is off");
 							}
 								
 						}else if(msg[1].equalsIgnoreCase("filters")){
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setFiltersFeature(true);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Filters is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setFiltersFeature(false);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Filters is off");
 							}
						}else if(msg[1].equalsIgnoreCase("throw")){
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setThrow(true);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: !throw is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setThrow(false);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: !throw is off");
 							}
						}else if(msg[1].equalsIgnoreCase("signkicks")){
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setSignKicks(true);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Sign-kicks is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setSignKicks(false);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Sign-kicks is off");
 							}
						}else if(msg[1].equalsIgnoreCase("joinparts")){
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setAnnounceJoinParts(true);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Join/Part announcing is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setAnnounceJoinParts(false);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Join/Part announcing is off");
 							}
						}else if(msg[1].equalsIgnoreCase("lastfm")){
 							if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setLastfm("");
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Lastfm is off.");
 							}else{
 								channelInfo.setLastfm(msg[2]);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Lastfm user set to " + msg[2]);
 							}
						}else if(msg[1].equalsIgnoreCase("steam")){
 							if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setSteam("");
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Steam is off.");
 							}else{
 								channelInfo.setSteam(msg[2]);
 								sendMessage(channel, channelInfo.getBullet() + " Feature: Steam id set to " + msg[2]);
 							}
						}else if(msg[1].equalsIgnoreCase("mode")){
							if(msg.length < 3){
								sendMessage(channel, channelInfo.getBullet() + " Mode set to " + channelInfo.getMode() + "");
							}else if((msg[2].equalsIgnoreCase("0") || msg[2].equalsIgnoreCase("owner")) && isOwner){
 								channelInfo.setMode(0);
 								sendMessage(channel, channelInfo.getBullet() + " Mode set to admin/owner only.");
 							}else if(msg[2].equalsIgnoreCase("1") || msg[2].equalsIgnoreCase("mod")){
 								channelInfo.setMode(1);
 								sendMessage(channel, channelInfo.getBullet() + " Mode set to admin/owner/mod only.");
							}else if(msg[2].equalsIgnoreCase("2") || msg[2].equalsIgnoreCase("everyone")){
 								channelInfo.setMode(2);
 								sendMessage(channel, channelInfo.getBullet() + " Mode set to everyone.");
 							}
						}else if(msg[1].equalsIgnoreCase("chatlogging")){
 							if(msg[2].equalsIgnoreCase("on")){
 								channelInfo.setLogging(true);
 								sendMessage(channel, channelInfo.getBullet() + " Chat logging is on");
 							}else if(msg[2].equalsIgnoreCase("off")){
 								channelInfo.setLogging(false);
 								sendMessage(channel, channelInfo.getBullet() + " Chat logging is off");
 							}
						}
 					}
 				}
 				
 				
 				//!modchan - Mod
 				if (msg[0].equalsIgnoreCase("!modchan") && isOp) {
 						System.out.println("DEBUG: Matched command !modchan");
 						if(channelInfo.getMode() == 2){
 							channelInfo.setMode(1);
 							sendMessage(channel, channelInfo.getBullet() + " Mode set to admin/owner/mod only.");
 						}else if(channelInfo.getMode() == 1){
 							channelInfo.setMode(2);
 							sendMessage(channel, channelInfo.getBullet() + " Mode set to everyone.");
 						}else{
 							sendMessage(channel, channelInfo.getBullet() + " Mode can only be changed by bot admin.");
 						}
 				}
 				
 				
 				//!join
 				if (msg[0].equalsIgnoreCase("!join") && botManager.publicJoin) {
 							System.out.println("DEBUG: Matched command !join");
// 							String serverIP = this.getIP(sender + ".jtvirc.com");
// 							if(!tmiServers.contains(serverIP)){
// 								sendMessage(channel, "Sorry, public join is only available for channels using TMI. Your server is " + serverIP + ".");
// 								return;
// 							}

							sendMessage(channel, channelInfo.getBullet() + " Joining channel #"+ sender +".");
							boolean joinStatus = botManager.addChannel("#" + sender, 2);
							if(joinStatus){
								sendMessage(channel, channelInfo.getBullet() + " Channel #"+ sender +" joined.");
							}else{
								sendMessage(channel, channelInfo.getBullet() + " Already in channel #"+ sender +".");
							}
				}
 				
 				if (msg[0].equalsIgnoreCase("!rejoin")) {
 					System.out.println("DEBUG: Matched command !rejoin");
 					if(msg.length > 1 && isAdmin){
 						if(msg[1].contains("#")){
							sendMessage(channel, channelInfo.getBullet() + " Rejoining channel "+ msg[1] +".");
							boolean joinStatus = botManager.rejoinChannel(msg[1]);
							if(joinStatus){
								sendMessage(channel, channelInfo.getBullet() + " Channel "+ msg[1] +" rejoined.");
							}else{
								sendMessage(channel, channelInfo.getBullet() + " Bot is not assigned to channel "+ msg[1] +"."); 							
							}
							
						}else{
							sendMessage(channel, channelInfo.getBullet() + " Invalid channel format. Must be in format #channelname.");
						}
 					}else{
						sendMessage(channel, "Rejoining channel #"+ sender +".");
						boolean joinStatus = botManager.rejoinChannel("#"+sender);
						if(joinStatus){
							sendMessage(channel, channelInfo.getBullet() + " Channel #"+ sender +" rejoined.");
						}else{
							sendMessage(channel, channelInfo.getBullet() + " Bot is not assigned to channel #"+ sender +"."); 							
						}
 					}

				}
 				
				// ********************************************************************************
				// **************************** Administration Commands ***************************
				// ********************************************************************************
 				
 				if (msg[0].equalsIgnoreCase("!bm-channels") && isAdmin) {
 					sendMessage(channel, channelInfo.getBullet() + " Currently in " + botManager.channelList.size() + " channels.");
 					
 					String channelString = "";
 					for (Map.Entry<String, Channel> entry : botManager.channelList.entrySet())
 					{
 						channelString += entry.getValue().getChannel() + ", ";
 					}
 					
 					sendMessage(channel, channelInfo.getBullet() + " Chanenls: " + channelString);
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-join") && msg.length > 1 && isAdmin) {

 						if(msg[1].contains("#")){
 							sendMessage(channel, channelInfo.getBullet() + " Joining channel "+ msg[1] +".");
 							boolean joinStatus = botManager.addChannel(msg[1],0);
 							if(joinStatus){
 								sendMessage(channel, channelInfo.getBullet() + " Channel "+ msg[1] +" joined.");
 							}else{
 								sendMessage(channel, channelInfo.getBullet() + " Already in channel "+ msg[1] +"."); 							
 							}
 							
 						}else{
 							sendMessage(channel, channelInfo.getBullet() + " Invalid channel format. Must be in format #channelname.");
 						}
 						
 
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-leave") && msg.length > 1 && isAdmin) {
 					if(msg[1].contains("#")){
 						sendMessage(channel, channelInfo.getBullet() + " Channel "+ msg[1] +" parting...");
 						botManager.removeChannel(msg[1]);
 						sendMessage(channel, channelInfo.getBullet() + " Channel "+ msg[1] +" parted.");
 					}else{
 						sendMessage(channel, channelInfo.getBullet() + " Invalid channel format. Must be in format #channelname.");
 					}
 						
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-rejoin") && isAdmin) {
 					sendMessage(channel, channelInfo.getBullet() + " Rejoining all channels.");
 					botManager.rejoinChannels();
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-softreconnect") && isAdmin) {
 					sendMessage(channel, channelInfo.getBullet() + " Reconnecting all servers.");
 					botManager.reconnectAllBotsSoft();
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-hardreconnect") && isAdmin) {
 					sendMessage(channel, channelInfo.getBullet() + " Reconnecting all servers.");
 					botManager.reconnectAllBotsHard();
 				}
 				
// 				if (msg[0].equalsIgnoreCase("!bm-global") && isAdmin) {
// 					botManager.sendGlobal(message.substring(11), sender);
// 				}
 				
				// ********************************************************************************
				// ***************************** Info/Catch-all Command ***************************
				// ********************************************************************************
 				
				if(msg[0].substring(0,1).equalsIgnoreCase("!") && !channelInfo.getCommand(msg[0]).equalsIgnoreCase("invalid")){
					System.out.println("DEBUG: Matched command " + msg[0]);
					sendMessage(channel, channelInfo.getBullet() + " " + channelInfo.getCommand(msg[0]));
				}

	}


	@Override
	public void onDisconnect(){
		//pjTimer.cancel();
		lastPing = -1;
		try {
			System.out.println("INFO: Internal reconnection: " + this.getServer());
			String[] channels = this.getChannels();
			this.reconnect();
			for(int i=0;i<channels.length;i++){
				this.joinChannel(channels[i]);
			}
		} catch (NickAlreadyInUseException e) {
			System.out.println("ERROR: Nickname already in use - " + this.getNick() + " " + this.getServer());
		} catch (IOException e) {
			System.out.println("ERROR: Unable to connect to server - " + this.getNick() + " " + this.getServer());
		} catch (IrcException e) {
			System.out.println("ERROR: Error connecting to server - " + this.getNick() + " " + this.getServer());
		}
		
	}
	
    public void onJoin(String channel, String sender, String login, String hostname){ 

		
		Channel channelInfo = botManager.getChannel(channel);
		
		if(channelInfo == null)
			return;
		
		//Call modules
		for(BotModule b:botManager.getModules()){
			b.onJoin(channelInfo, sender, login, hostname);
		}
		
		if(!channelInfo.getAnnounceJoinParts() || this.getNick().equalsIgnoreCase(sender))
			return;
		
		sendMessage(channel, "> " + sender + " entered the room.");
    }

    public void onPart(String channel, String sender, String login, String hostname) {	
		Channel channelInfo = botManager.getChannel(channel);
		
		if(channelInfo == null)
			return;
		
		//Call modules
		for(BotModule b:botManager.getModules()){
			b.onPart(channelInfo, sender, login, hostname);
		}
		
		if(!channelInfo.getAnnounceJoinParts() || this.getNick().equalsIgnoreCase(sender))
			return;
		
		sendMessage(channel, "> " + sender + " left the room.");
    }
    
    
    
	@Override
	protected boolean onMessageSend(String target, String message) {
		if(!botManager.verboseLogging)
			System.out.println("MESSAGE: " + target + " " + getNick() + " : " + message);
		
		Channel channelInfo = botManager.getChannel(target);
		
		if(channelInfo != null){
			//Call modules
			for(BotModule b:botManager.getModules()){
				b.onSelfMessage(channelInfo, this.getNick(), message);
			}
		}
	
		return super.onMessageSend(target, message);
	}

	@Override
    public void onServerPing(String response) {
		super.onServerPing(response);
		//System.out.println("DEBUG: Ping received at " + (int) (System.currentTimeMillis()/1000));
		lastPing = (int) (System.currentTimeMillis()/1000);
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
	
    public void log(String line) {
    	super.log(line);
    	
    	if(botManager.useGUI){
    		botManager.getGUI().log(line);
    	}
    }
	
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
			for(Pattern pattern: linkPatterns){
				//System.out.println("Checking " + m + " against " + pattern.pattern());
				Matcher match = pattern.matcher(m);
				if(match.matches()){
					System.out.println("DEBUG: Link match on " + pattern.pattern());
					if(!ch.checkPermittedDomain(m))
						return true;	
				}
			}
		}

		
		return false;
	}
	
	private int countEmotes(String message){
		String str = message;
		int count=0;
		for(String findStr: botManager.emoteSet){
			int lastIndex = 0;
			while(lastIndex != -1){

				lastIndex = str.indexOf(findStr,lastIndex);

			       if( lastIndex != -1){
			             count ++;
			             lastIndex+=findStr.length();
			      }	
			}
		}
		return count;
		
	}
	
	public boolean isGlobalBannedWord(String message){

		for(Pattern reg:botManager.globalBannedWords){
			Matcher match = reg.matcher(message.toLowerCase());
			if(match.matches()){
				System.out.println("DEBUG: Global banned word matched: " + reg.toString());
				return true;
			}
		}
		return false;
	}
	
	private String getWarningText(int count){
		if(count > 4)
			return warningText[3];
		else
			return warningText[count-1];	
	}
	
	private int getWarningTODuration(int count){
		if(count > 4)
			return warningTODuration[3];
		else
			return warningTODuration[count-1];
	}
	
	private void tenSecondUnban(final String channel, final String name){
		Timer timer = new Timer();
		
		int delay = 30000;
		
		timer.schedule(new TimerTask()
	       {
	        public void run() {
				if(botManager.network.equalsIgnoreCase("jtv"))
		        	Bot.this.unBan(channel,name + "!*@*.*");
				else
					Bot.this.unBan(channel,name);
	        }
	      },delay);

	}
	
	private void secondaryTO(final String channel, final String name, final int duration){
		Timer timer = new Timer();
		
		int delay = 1000;
		
		timer.schedule(new TimerTask()
	       {
	        public void run() {
	        	Bot.this.sendMessage(channel,".timeout " + name + " " + duration);
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
	
//	private int getBitRate(Channel channelInfo) throws IOException{
//		URL url = new URL("http://api.justin.tv/api/stream/summary.json?channel=" + channelInfo.getChannel().substring(1));
//		URLConnection conn = url.openConnection();
//		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
//		BufferedReader d = new BufferedReader(new InputStreamReader(in));
//		String jsonIn = "";
//		while(d.ready())
//		{
//			jsonIn = d.readLine();
//		}
//		
//		JTVStreamSummary data = new Gson().fromJson(jsonIn, JTVStreamSummary.class);
//		
//		return data.average_bitrate;
//	}
	
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
			
		LastFmRecentTracks data = new Gson().fromJson(jsonIn, LastFmRecentTracks.class);
		
		if(data.playing == true){
			return "Listening to: " + data.title + " by " + data.artist + " " + data.url;
		}else{
			return "Recently listened to: " + data.title + " by " + data.artist + " " + data.url;
		}
			
	}
	
	private SteamData getSteamData(Channel channelInfo) throws IOException{
		URL url = new URL("http://bashtech.net/app-support/geobot/steam.php?user=" + channelInfo.getSteam());
		URLConnection conn = url.openConnection();
		DataInputStream in = new DataInputStream ( conn.getInputStream (  )  ) ;
		BufferedReader d = new BufferedReader(new InputStreamReader(in));
		String jsonIn = "";
		while(d.ready())
		{
			jsonIn = d.readLine();
		}
		
		SteamData data = new Gson().fromJson(jsonIn, SteamData.class);
		
		return data;
	}
	
	private String getMetaInfo(String key, Channel channelInfo) throws IllegalArgumentException, IOException, SAXException, ParserConfigurationException{
		URL feedSource = new URL("http://twitch.tv/meta/" + channelInfo.getChannel().substring(1)+ ".xml");
		URLConnection uc = feedSource.openConnection();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(uc.getInputStream());
		doc.getDocumentElement().normalize();

		NodeList nList = doc.getElementsByTagName("meta");
 
		for (int temp = 0; temp < nList.getLength(); temp++) {
 
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
 
		      Element eElement = (Element) nNode;
 
		      return getTagValue(key, eElement);
 
		   }
		}
		
		return "";

	}
	
	private String getGame(Channel channelInfo){
		String game = "";
		try {
			game =  getMetaInfo("meta_game", channelInfo);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	
		return game;
	}
	
	private String getStatus(Channel channelInfo){
		String game = "";
		try {
			game =  getMetaInfo("status", channelInfo);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return game;
	}
	
	private static String getTagValue(String sTag, Element eElement) {
		NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();
	 
	    Node nValue = (Node) nlList.item(0);
	 
		return nValue.getNodeValue();
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
	
	public boolean checkStalePing(){
		if(lastPing == -1)
			return false;
		
		int difference = ((int) (System.currentTimeMillis()/1000)) - lastPing;
		
		if(difference > botManager.pingInterval){
			System.out.println("DEBUG: Ping is stale. Last ping= " + lastPing + " Difference= " + difference);
			lastPing = -1;
			return true;
		}
		
		return false;
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
					Bot.this.sendMessage(channelInfo.getChannel(), "> Giveaway over.");
				}
			}
        }
	}
	
	public String getIP(String server) {
		InetAddress ip;
		
		try {
			ip = InetAddress.getByName(server);
		} catch (UnknownHostException e) {
			e.printStackTrace();
			return server;
		}
		
		return ip.getHostAddress();
	}
	
	private String fuseArray(String[] array, int start){
		String fused = "";
		for(int c = start; c < array.length; c++)
			fused += array[c] + " ";
		
		return fused.trim();
	
	}

}
