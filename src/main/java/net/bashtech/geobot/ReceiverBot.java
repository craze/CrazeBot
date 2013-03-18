/*
 * Copyright 2012 Andrew Bashore
 * This file is part of GeoBot.
 * 
 * GeoBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * GeoBot is distributed in the hope that it will be useful
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GeoBot.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.bashtech.geobot;
import java.io.*;
import java.net.URL;
import java.net.URLConnection;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Map;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;


import net.bashtech.geobot.modules.BotModule;

import org.jibble.pircbot.*;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class ReceiverBot extends PircBot {

    static ReceiverBot instance;

    private Pattern[] linkPatterns = new Pattern[3];
	private Pattern[] symbolsPatterns = new Pattern[1];
	private int lastPing = -1;
	private int[] warningTODuration = {10, 60, 600, 86400};
	private String[] warningText = {"first warning (10 sec t/o)", "second warning (1 minute t/o)", "final warning (10 min t/o)", "(24hr timeout)"};
	
	public ReceiverBot(String server, int port){
        ReceiverBot.setInstance(this);
		linkPatterns[0] = Pattern.compile(".*http://.*");
		linkPatterns[1] = Pattern.compile(".*(\\.|\\(dot\\))(com|org|net|tv|ca|xxx|cc|de|eu|fm|gov|info|io|jobs|me|mil|mobi|name|rn|tel|travel|tz|uk|co|us|be|sh|ly|in|gl)(\\s+|/|$).*");
		linkPatterns[2] = Pattern.compile(".*(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\s+|:|/|$).*");	
		
		symbolsPatterns[0] = Pattern.compile(".*(░|░|▓|▀|▄|ส้้้้้้้้้้้้้้้้้้้้|ส็็็็็็็็็็็็็็็|ǝ|ส็็็็็็็็็็็็็็็็็็็็็็็็็|ส้้้้้้้้้|ส็็็็็็็็็็็็็็็็็็็|กิิิิิิิิิิิิิิิิิิิิ|ก้้้้้้้้้้้้้้้้้้้้|กิิิิิิิิิิิิิิิ|▒|█).*");
		this.setName(BotManager.getInstance().getInstance().nick);
		this.setLogin("ReceiverGeoBot");
		
		this.setVerbose(BotManager.getInstance().verboseLogging);
		try {
			this.connect(server, port, BotManager.getInstance().getInstance().password);
		} catch (NickAlreadyInUseException e) {
			System.out.println("ERROR: Nickname already in use - " + this.getNick() + " " + this.getServer());
		} catch (IOException e) {
			System.out.println("ERROR: Unable to connect to server - " + this.getNick() + " " + this.getServer());
		} catch (IrcException e) {
			System.out.println("ERROR: Error connecting to server - " + this.getNick() + " " + this.getServer());
		}
	}

    public static void setInstance(ReceiverBot rb){
        if(instance == null){
            instance = rb;
        }
    }

    public static ReceiverBot getInstance(){
        return instance;
    }
	
	private Channel getChannelObject(String channel){
		Channel channelInfo = null;
		channelInfo = BotManager.getInstance().getChannel(channel);
		return channelInfo;
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
					BotManager.getInstance().addTagAdmin(user);
				if(tag.equalsIgnoreCase("staff"))
					BotManager.getInstance().addTagStaff(user);
//				if(BotManager.getInstance().botMode == 1 && tag.equalsIgnoreCase("subscriber"))
//					channelInfoGlobal.addSubscriber(user);
			}else if(msg[0].equalsIgnoreCase("USERCOLOR")){
				String user = msg[1];
				String color = msg[2];
			}else if(msg[0].equalsIgnoreCase("CLEARCHAT")){
				if(msg.length > 1){
					String user = msg[1];
					if(!BotManager.getInstance().verboseLogging)
						System.out.println("RAW: CLEARCHAT " + user);
				}else{
					if(!BotManager.getInstance().verboseLogging)
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
				if(!BotManager.getInstance().verboseLogging)
					System.out.println("MESSAGE: " + channel + " " + sender + " : " + message);
				
				Channel channelInfo = getChannelObject(channel);
				if(!sender.equalsIgnoreCase(this.getNick()))
					channelInfo.messageCount++; //Inc message count
				
				//Call modules
				for(BotModule b:BotManager.getInstance().getModules()){
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
				if(BotManager.getInstance().isAdmin(sender))
					isAdmin = true;
				if(BotManager.getInstance().isTagAdmin(sender) || BotManager.getInstance().isTagStaff(sender))
					isAdmin = true;
				if(channel.equalsIgnoreCase("#" + sender))
					isOwner = true;
				if(channelInfo.isModerator(sender))
					isOp = true;
				if(channelInfo.isOwner(sender))
					isOwner = true;
				if(channelInfo.isRegular(sender) || channelInfo.isSubscriber(sender))
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
 						BotManager.getInstance().removeChannel(channel);
 				}
 				
 				
				// ********************************************************************************
				// ********************************** Filters *************************************
				// ********************************************************************************
				
 				//Global banned word filter
 				if(!isOp && this.isGlobalBannedWord(message)){
 					this.secondaryTO(channel, sender, 600);
 					System.out.println("NOTICE: Global banned word timeout: " + sender + " in " + channel + " : " + message);
 				}
 				
 				// Voluntary Filters
 				if(channelInfo.useFilters){
					// Cap filter
 					String messageNoWS = message.replaceAll("\\s","");
					int capsNumber = getCapsNumber(messageNoWS);
					int capsPercent = getCapsPercent(messageNoWS);
					if(channelInfo.getFilterCaps() && !(isOp) && message.length() >= channelInfo.getfilterCapsMinCharacters() && capsPercent >= channelInfo.getfilterCapsPercent() && capsNumber >= channelInfo.getfilterCapsMinCapitals()){
						int warningCount = 0;

							channelInfo.incWarningCount(sender, FilterType.CAPS);
							warningCount = channelInfo.getWarningCount(sender, FilterType.CAPS);
							this.secondaryTO(channel, sender, this.getWarningTODuration(warningCount));

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

								channelInfo.incWarningCount(sender, FilterType.LINK);
								warningCount = channelInfo.getWarningCount(sender, FilterType.LINK);
								this.secondaryTO(channel, sender, this.getWarningTODuration(warningCount));
								
							if(channelInfo.checkSignKicks())
								sendMessage(channel, channelInfo.getBullet() +  " " + sender + ", please ask a moderator before posting links - " + this.getWarningText(warningCount));
						}
						
					}
					
					// Length filter
					if(!(isOp || isRegular) && (message.length() > channelInfo.getFilterMax())){
						int warningCount = 0;

								channelInfo.incWarningCount(sender, FilterType.LENGTH);
								warningCount = channelInfo.getWarningCount(sender, FilterType.LENGTH);
								this.secondaryTO(channel, sender, this.getWarningTODuration(warningCount));
								
							if(channelInfo.checkSignKicks())
								sendMessage(channel, channelInfo.getBullet() +  " " + sender + ", please don't spam long messages - " + this.getWarningText(warningCount));

						
					}
					
					// Symbols filter
					if(channelInfo.getFilterSymbols() && !(isOp || isRegular) && this.containsSymbol(message,channelInfo) ){
						int warningCount = 0;
						channelInfo.incWarningCount(sender, FilterType.SYMBOLS);
						warningCount = channelInfo.getWarningCount(sender, FilterType.SYMBOLS);
						this.secondaryTO(channel, sender, this.getWarningTODuration(warningCount));
								
						if(channelInfo.checkSignKicks())
							sendMessage(channel, channelInfo.getBullet() +  " " + sender + ", please don't post spam in the chat - " + this.getWarningText(warningCount));
			
						
					}
					
					//Offensive filter
					if(!isOp && channelInfo.getFilterOffensive()){
						if(channelInfo.isOffensive(message)){
							int warningCount = 0;

								channelInfo.incWarningCount(sender, FilterType.OFFENSIVE);
								warningCount = channelInfo.getWarningCount(sender, FilterType.OFFENSIVE);
								this.secondaryTO(channel, sender, this.getWarningTODuration(warningCount));

						}
					}
					
					//Emote filter
					if(!isOp && channelInfo.getFilterEmotes()){
						if(countEmotes(message) > channelInfo.getFilterEmotesMax()){
							int warningCount = 0;

								channelInfo.incWarningCount(sender, FilterType.EMOTES);
								warningCount = channelInfo.getWarningCount(sender, FilterType.EMOTES);
								this.secondaryTO(channel, sender, this.getWarningTODuration(warningCount));
								
								if(channelInfo.checkSignKicks())
									sendMessage(channel, channelInfo.getBullet() +  " " + sender + ", please don't spam emotes - " + this.getWarningText(warningCount));			
							
						}	
					}
 				}

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
				// ***************************** Raffle Entry *************************************
				// ********************************************************************************
				if(msg[0].equalsIgnoreCase("!raffle") && msg.length == 1){
					System.out.println("DEBUG: Matched command !raffle (user entry)");
						if(channelInfo.raffle != null){
							channelInfo.raffle.enter(sender);
						}
				}
				// ********************************************************************************
				// ******************************* Mode Checks ************************************
				// ********************************************************************************
				
				//Check channel mode.
				if((channelInfo.getMode() == 0 || channelInfo.getMode() == -1) && !isOwner)
					return;
				if(channelInfo.getMode() == 1 && !isOp)
					return;
				
				// ********************************************************************************
				// ********************************* Commands *************************************
				// ********************************************************************************
				
				//Command cooldown check
				if(msg[0].substring(0,1).equalsIgnoreCase("!") && channelInfo.onCooldown(msg[0])){
					System.out.println("DEBUG: Command " + msg[0] + " is on cooldown.");
					if(!isOwner)
						return;
				}
				
				// !ping - All
				if (msg[0].equalsIgnoreCase("!ping") && isOp) {
						System.out.println("DEBUG: Matched command !ping");
						String time = new java.util.Date().toString();
						sendMessage(channel, channelInfo.getBullet() + " Pong sent at " + time + " (" + this.fuseArray(msg, 1) + ")");
						//return;
				}
				
				// !lockouttest - All
				if (msg[0].equalsIgnoreCase("!lockouttest")) {
						System.out.println("DEBUG: Matched command !lockouttest");
						sendMessage(channel, channelInfo.getBullet() + " " + sender + ", your message was received! You are NOT locked out of chat.");
						//return;
				}
				
				// !bothelp - All
				if (msg[0].equalsIgnoreCase("!bothelp")) {
						System.out.println("DEBUG: Matched command !bothelp");
						sendMessage(channel, channelInfo.getBullet() + " Command help is available at http://bashtech.net/twitch/geobot.php");
						//return;
				}
				
				// !viewers - All
				if (msg[0].equalsIgnoreCase("!viewers") || msg[0].equalsIgnoreCase("!lurkers")) {
					System.out.println("DEBUG: Matched command !viewers");
					try {
						sendMessage(channel, channelInfo.getBullet() + " " + this.getStreamList("channel_count", channelInfo) + " viewers (" + this.getStreamList("embed_count", channelInfo) + " from embeds).");
					} catch (Exception e) {
						sendMessage(channel, channelInfo.getBullet() + " Stream is not live.");
					}
					//return;
				}
				
				// !bitrate - All
				if (msg[0].equalsIgnoreCase("!bitrate")) {
					System.out.println("DEBUG: Matched command !bitrate");
					try {
						sendMessage(channel, channelInfo.getBullet() + " Streaming at " + Math.floor(Double.parseDouble(this.getStreamList("video_bitrate", channelInfo))) + " Kbps.");
					} catch (Exception e) {
						sendMessage(channel, channelInfo.getBullet() + " Stream is not live.");
					}
					//return;
				}
				
				// !uptime - All
				if (msg[0].equalsIgnoreCase("!uptime")) {
					System.out.println("DEBUG: Matched command !uptime");
					try {
						String uptime = this.getStreamList("up_time", channelInfo);
						sendMessage(channel, channelInfo.getBullet() + " Streaming for " + this.getTimeStreaming(uptime) + " since " + uptime + " PST.");
					} catch (Exception e) {
						sendMessage(channel, channelInfo.getBullet() + " Error accessing Twitch API.");
					}
				}
				
				// !music - All
				if (msg[0].equalsIgnoreCase("!music") || msg[0].equalsIgnoreCase("!lastfm")) {
					System.out.println("DEBUG: Matched command !music");
					sendMessage(channel, channelInfo.getBullet() + " " + JSONUtil.lastFM(channelInfo.getLastfm()));
				}
				
				// !steam - All
				if (msg[0].equalsIgnoreCase("!steam")) {
					System.out.println("DEBUG: Matched command !steam");
					if(channelInfo.getSteam().length() > 1){

                    if(channelInfo.getSteam().length() > 1){
                        sendMessage(channel, channelInfo.getBullet() + " " + JSONUtil.steam(channelInfo.getSteam(), "all"));
                    }

					}else{
						sendMessage(channel, channelInfo.getBullet() + " Steam ID not set. Do \"!set steam [ID]\" to configure. ID must be in SteamID64 format and profile must be public.");
					}
					
				}
				
				// !game - All
				if (msg[0].equalsIgnoreCase("!game")) {
					System.out.println("DEBUG: Matched command !game");
					String game = this.getGame(channelInfo);
					if(game.length() > 0){
						sendMessage(channel, channelInfo.getBullet() + " Current game: " + game);
					}else{
						sendMessage(channel, channelInfo.getBullet() + " No game set.");
					}
					
				}
				
				// !status - All
				if (msg[0].equalsIgnoreCase("!status")) {
					System.out.println("DEBUG: Matched command !status");
					String status = this.getStatus(channelInfo);
					if(status.length() > 0){
						sendMessage(channel, channelInfo.getBullet() + " " + status);
					}else{
						sendMessage(channel, channelInfo.getBullet() + " Unable to query TwitchTV API.");
					}
				}
				
				// !commands - Op/Regular
				if (msg[0].equalsIgnoreCase("!commands") && (isRegular || isOp)) {
					System.out.println("DEBUG: Matched command !commands");
					sendMessage(channel, channelInfo.getBullet() + " Commands: " + channelInfo.getCommandList());

					//return;
				}
				
				// !throw - All
				if(msg[0].equalsIgnoreCase("!throw") && (channelInfo.checkThrow() || isRegular)){
					System.out.println("DEBUG: Matched command !throw");
					if(msg.length > 1){
						String throwMessage = "";
						for(int i=1;i<msg.length;i++){
							throwMessage += msg[i] + " ";
						}
						sendMessage(channel, "(╯°‿°）╯︵" + throwMessage);
					}
				}
				
				// !flip - All
				if(msg[0].equalsIgnoreCase("!flip") && (channelInfo.checkThrow() || isRegular)){
					System.out.println("DEBUG: Matched command !fli");
					if(msg.length > 1){
						String throwMessage = "";
						for(int i=1;i<msg.length;i++){
							throwMessage += msg[i] + " ";
						}
						//sendMessage(channel, "(╯°‿°）╯︵" + throwMessage);
						sendMessage(channel, throwMessage + " TABLEFLIP");
					}
				}
				
				// !topic
				if(msg[0].equalsIgnoreCase("!topic") && channelInfo.useTopic){
					System.out.println("DEBUG: Matched command !topic");
					if(msg.length < 2 || !isOp){
						if(channelInfo.getTopic().equalsIgnoreCase("")){
							String status = this.getStatus(channelInfo);
							if(status.length() > 0)
								sendMessage(channel, channelInfo.getBullet() + " " + status);
							else
								sendMessage(channel, channelInfo.getBullet() + " Unable to query TwitchTV API.");
							
						}else{
							sendMessage(channel, channelInfo.getBullet() + " Topic: " + channelInfo.getTopic() + " (Set " + channelInfo.getTopicTime() + " ago)");
						}
					}else if(msg.length > 1 && isOp){
						if(msg[1].equalsIgnoreCase("unset")){
							channelInfo.setTopic("");
							sendMessage(channel, channelInfo.getBullet() + " No topic is set.");
						}else{
							channelInfo.setTopic(message.substring(7));
							sendMessage(channel, channelInfo.getBullet() + " Topic: " + channelInfo.getTopic() + " (Set " + channelInfo.getTopicTime() + " ago)");
						}

					}
					//return;
				}
				
				// !link
				if(msg[0].equalsIgnoreCase("!link") && isRegular){
					System.out.println("DEBUG: Matched command !link");
					if(msg.length > 1){
							String rawQuery = message.substring(6);
                        String encodedQuery = "";
                        try {
                            encodedQuery = URLEncoder.encode(rawQuery,"UTF-8");
                        } catch (UnsupportedEncodingException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
							sendMessage(channel, channelInfo.getBullet() + " Link to \"" + rawQuery + "\" -> " + "http://lmgtfy.com/?q=" + encodedQuery);
					}
					//return;
				}
				
				// !commercial
				if(msg[0].equalsIgnoreCase("!commercial")){
					System.out.println("DEBUG: Matched command !commercial");
					if(isOwner){
						String result = channelInfo.runCommercial();
						//sendMessage(channel, channelInfo.getBullet() + " Running a 30 second commercial. Thank you for supporting the channel!");
					}
					//return;
				}
				
				// !command - Ops
 				if(msg[0].equalsIgnoreCase("!command")){
 					System.out.println("DEBUG: Matched command !command");
					if(msg.length < 3 && isOp){
						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!command add/delete [name] [message]\" - Name is the command trigger without \"!\" and message is the response.");
					}else if(msg.length > 2 && isOp){
						if(msg[1].equalsIgnoreCase("add") && msg.length > 3){
							String key = "!" + msg[2];
							String value = fuseArray(msg, 3);
							if(!value.contains(",,")){
								channelInfo.setCommand(key, value);
								sendMessage(channel, channelInfo.getBullet() + " " + channelInfo.getCommand(key));
							}else{
								sendMessage(channel, channelInfo.getBullet() + " Command cannot contain double commas (\",,\").");
							}
								
						}else if(msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")){
							String key = "!" + msg[2];
							channelInfo.removeCommand(key);
							channelInfo.removeRepeatCommand(key);
                            channelInfo.removeScheduledCommand(key);

                            sendMessage(channel, channelInfo.getBullet() + " Command " + key + " removed.");

							}
					}
				}
 				
				// !repeat - Ops
 				if(msg[0].equalsIgnoreCase("!repeat")){
 					System.out.println("DEBUG: Matched command !repeat");
					if(msg.length < 3 && isOp){
						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!repeat add/delete [commandname] [delay in seconds] [message difference - optional]\"");
					}else if(msg.length > 2 && isOp){
						if(msg[1].equalsIgnoreCase("add") && msg.length > 3){
							String key = "!" + msg[2];
							try{
								int delay = Integer.parseInt(msg[3]);
								int difference = 1;
								if(msg.length == 5)
									difference = Integer.parseInt(msg[4]);
									
								if(channelInfo.getCommand(key).equalsIgnoreCase("invalid") || delay < 30){
									//Key not found or delay to short
									sendMessage(channel, channelInfo.getBullet() + " Command not found or delay is less than 30 seconds.");
								}else{
									channelInfo.setRepeatCommand(key, delay, difference);
									sendMessage(channel, channelInfo.getBullet() + " Command " + key + " will repeat every " + delay + " seconds if " + difference + " messages have passed.");
								}							
								
							}catch(Exception ex){
								ex.printStackTrace();
							}
	
						}else if(msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")){
							String key = "!" + msg[2];
							channelInfo.removeRepeatCommand(key);	
							sendMessage(channel, channelInfo.getBullet() + " Command " + key + " will no longer repeat.");

						}
					}
				}

                // !schedule - Ops
                if(msg[0].equalsIgnoreCase("!schedule")){
                    System.out.println("DEBUG: Matched command !schedule");
                    if(msg.length < 3 && isOp){
                        sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!schedule add/delete [commandname] [pattern] [message difference - optional]\"");
                    }else if(msg.length > 2 && isOp){
                        if(msg[1].equalsIgnoreCase("add") && msg.length > 3){
                            String key = "!" + msg[2];
                            try{
                                String pattern = msg[3];
                                if(pattern.equals("hourly"))
                                    pattern = "0 * * * *";
                                else
                                    pattern = pattern.replace("_", " ");

                                int difference = 1;
                                if(msg.length == 5)
                                    difference = Integer.parseInt(msg[4]);

                                if(channelInfo.getCommand(key).equalsIgnoreCase("invalid") || pattern.contains(",,")){
                                    //Key not found or delay to short
                                    sendMessage(channel, channelInfo.getBullet() + " Command not found or invalid pattern.");
                                }else{
                                    channelInfo.setScheduledCommand(key, pattern, difference);
                                    sendMessage(channel, channelInfo.getBullet() + " Command " + key + " will repeat every " + pattern + " if " + difference + " messages have passed.");
                                }

                            }catch(Exception ex){
                                ex.printStackTrace();
                            }

                        }else if(msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")){
                            String key = "!" + msg[2];
                            channelInfo.removeScheduledCommand(key);
                            sendMessage(channel, channelInfo.getBullet() + " Command " + key + " will no longer repeat.");

                        }
                    }
                }


 				
				// !poll - Ops
				if(msg[0].equalsIgnoreCase("!poll") && isOp){
					System.out.println("DEBUG: Matched command !poll");
					if(msg.length < 2){
						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!poll create [option option ... option]\"");
					}else if(msg.length >= 2){
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
                                sendMessage(channel, channelInfo.getBullet() + " " + channelInfo.getPoll().getResultsString());
//								String[] results = channelInfo.getPoll().getResults();
//								for(int c=0;c<results.length;c++){
//									sendMessage(channel, results[c]);
//								}
							}
						
					   }
					}
				}
				
				// !giveaway - Ops
				if((msg[0].equalsIgnoreCase("!giveaway") || msg[0].equalsIgnoreCase("!ga")) && isOp){
					System.out.println("DEBUG: Matched command !giveaway");
					if(msg.length < 2){
						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!giveaway create [max number] [time to run in seconds]\". Time is optional.");
					}else if(msg.length >= 2){
						if(msg[1].equalsIgnoreCase("create")){
							String max = "" + 100;
							if(msg.length > 2){
								max = msg[2];
							}
							channelInfo.setGiveaway(new Giveaway(max));
							if(msg.length > 3 && channelInfo.getGiveaway().isInteger(msg[3])){
								this.startGaTimer(Integer.parseInt(msg[3]),channelInfo);
							}else{
								sendMessage(channel,channelInfo.getBullet() + " Giveaway created. Do !giveaway start' to start." + " Range 1-" + channelInfo.getGiveaway().getMax() + ".");
							}
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
                                sendMessage(channel, channelInfo.getBullet() + " " + channelInfo.getGiveaway().getResultsString());
//								String[] results = channelInfo.getGiveaway().getResults();
//								for(int c=0;c<results.length;c++){
//									sendMessage(channel, results[c]);
//								}
							}else{
								sendMessage(channel, channelInfo.getBullet() + " No giveaway results.");
							}
						
					   }
					}
				}
				
				// !raffle - Ops
				if(msg[0].equalsIgnoreCase("!raffle") && isOp){
					System.out.println("DEBUG: Matched command !raffle");
					if(msg.length >= 2){
						if(msg[1].equalsIgnoreCase("enable")){
							if(channelInfo.raffle == null){
								channelInfo.raffle = new Raffle();
							}
							channelInfo.raffle.setEnabled(true);
							
							sendMessage(channel, channelInfo.getBullet() + " Raffle enabled.");
						}else if(msg[1].equalsIgnoreCase("disable")){
							if(channelInfo.raffle != null){
								channelInfo.raffle.setEnabled(false);
							}
							
							sendMessage(channel, channelInfo.getBullet() + " Raffle disabled.");
						}else if(msg[1].equalsIgnoreCase("reset")){ 
							if(channelInfo.raffle != null){
								channelInfo.raffle.reset();
							}
							
							sendMessage(channel, channelInfo.getBullet() + " Raffle entries cleared.");
						}else if(msg[1].equalsIgnoreCase("count")){ 
							if(channelInfo.raffle != null){
								sendMessage(channel, channelInfo.getBullet() + " Raffle has " + channelInfo.raffle.count() + " entries.");
							}else{
								sendMessage(channel, channelInfo.getBullet() + " Raffle has 0 entries.");
							}	
						}else if(msg[1].equalsIgnoreCase("winner")){
							if(channelInfo.raffle != null){
								sendMessage(channel, channelInfo.getBullet() + " Winner is " + channelInfo.raffle.pickWinner() + "!");
							}else{
								sendMessage(channel, channelInfo.getBullet() + " No raffle history found.");
							}	
						}
					}else{
						if(channelInfo.raffle != null){
							channelInfo.raffle.enter(sender);
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
 				if(isOp){
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
 	 						sendMessage(channel, ".timeout " + msg[1].toLowerCase() + " 1");
 	 					}
 					}
 					
 				}
 				
				// !clear - Ops
				if(msg[0].equalsIgnoreCase("!clear") && isOp){
					System.out.println("DEBUG: Matched command !clear");
					sendMessage(channel, ".clear");
				}
 				
 				// !links - Owner
 				if(msg[0].equalsIgnoreCase("!links") && isOwner){
 					System.out.println("DEBUG: Matched command !links");
 					if(msg.length == 1){
 						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!links on/off\"");
 					}else if(msg.length == 2){
 						if(msg[1].equalsIgnoreCase("on")){
 							channelInfo.setFilterLinks(true);
 							sendMessage(channel, channelInfo.getBullet() + " Link filter: " + channelInfo.getFilterLinks());
 						}else if(msg[1].equalsIgnoreCase("off")){
 							channelInfo.setFilterLinks(false);
 							sendMessage(channel, channelInfo.getBullet() + " Link filter: " + channelInfo.getFilterLinks());
 						}
 					}
 				}
 				
 				// !permit - Allows users to post 1 link
 				if((msg[0].equalsIgnoreCase("!permit") || msg[0].equalsIgnoreCase("!allow")) && channelInfo.getFilterLinks() && channelInfo.useFilters && isOp){
 					System.out.println("DEBUG: Matched command !permit");
 					if(msg.length == 1){
 						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!permit [username]\"");
 					}else if(msg.length > 1){
 						if(!channelInfo.isRegular(msg[1])){
 							channelInfo.permitUser(msg[1]);
 	 						sendMessage(channel, channelInfo.getBullet() + " " + msg[1] + " may now post 1 link.");
 						}else{
 							sendMessage(channel, channelInfo.getBullet() + " " + msg[1] + " is a regular and does not need to be permitted.");
 						}
 					}
 				}
 				
 				
 				// !pd - Owner
 				if(msg[0].equalsIgnoreCase("!pd") && isOwner){
 					System.out.println("DEBUG: Matched command !pd");
 					if(msg.length <= 2){
 						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!pd add/delete [domain]\" and \"!pd list\"");
 					}else if(msg.length  > 2){
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
 				if(msg[0].equalsIgnoreCase("!offensive") && isOwner){
 					System.out.println("DEBUG: Matched command !offensive");
 					if(isOwner)
 						System.out.println("DEBUG: Is owner");
 					if(msg.length == 1){
 						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!offensive on/off\", \"!offsensive add/delete [string to purge]\", \"!offensive list\"");
 					}else if(msg.length > 1){
 						if(msg[1].equalsIgnoreCase("on")){
							channelInfo.setFilterOffensive(true);
							sendMessage(channel, channelInfo.getBullet() + " Offensive word filter is on");
 						}else if(msg[1].equalsIgnoreCase("off")){
							channelInfo.setFilterOffensive(false);
							sendMessage(channel, channelInfo.getBullet() + " Offensive word filter is off");
 						}else if(msg[1].equalsIgnoreCase("list")){
 							String tempList = channelInfo.getBullet() + " Offsenive words: ";
 	 						for(String s:channelInfo.getOffensive()){
 	 							tempList += s + ", ";
 	 						}
 	 						sendMessage(channel, tempList);
 						}else if(msg[1].equalsIgnoreCase("add") && msg.length > 2){
 							String phrase = fuseArray(msg, 2);
 							if(phrase.contains(",,")){
 								sendMessage(channel,channelInfo.getBullet() + " Cannot contain double commas (,,)");
 							}else if(channelInfo.isOffensive(fuseArray(msg, 2))){
 								sendMessage(channel,channelInfo.getBullet() + " Word already exists. " + "(" + phrase + ")");
 							}else{
 								channelInfo.addOffensive(phrase);
 								sendMessage(channel,channelInfo.getBullet() + " Word added. "+ "(" + phrase + ")");
 							}
 						}else if(msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove") && msg.length > 2){
 							String phrase = fuseArray(msg, 2);
 							if(channelInfo.isOffensive(phrase)){
 								channelInfo.removeOffensive(phrase);
 								sendMessage(channel,channelInfo.getBullet() + " Word removed. "+ "(" + phrase + ")");
 							}else{
 								sendMessage(channel,channelInfo.getBullet() + " Word does not exist. "+ "(" + phrase + ")");
 							}
 						}
 					}
 				}
				
 				// !caps - Owner
 				if(msg[0].equalsIgnoreCase("!caps") && isOwner){
 					System.out.println("DEBUG: Matched command !caps");
 					if(msg.length == 1){
 						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!caps on/off\", \"!caps percent/minchars/mincaps [value]\", \"!caps status\"");
 					}else if(msg.length > 1){
 						if(msg[1].equalsIgnoreCase("on")){
 							channelInfo.setFilterCaps(true);
 							sendMessage(channel, channelInfo.getBullet() + " Caps filter: " + channelInfo.getFilterCaps());
 						}else if(msg[1].equalsIgnoreCase("off")){
 							channelInfo.setFilterCaps(false);
 							sendMessage(channel, channelInfo.getBullet() + " Caps filter: " + channelInfo.getFilterCaps());
 						}else if(msg[1].equalsIgnoreCase("percent")){
 							if(msg.length > 2){
 								channelInfo.setfilterCapsPercent(Integer.parseInt(msg[2]));
 	 							sendMessage(channel, channelInfo.getBullet() + " Caps filter percent: " + channelInfo.getfilterCapsPercent());
 							}
 						}else if(msg[1].equalsIgnoreCase("minchars")){
 							if(msg.length > 2 && isInteger(msg[2])){
 								channelInfo.setfilterCapsMinCharacters(Integer.parseInt(msg[2]));
 	 							sendMessage(channel, channelInfo.getBullet() + " Caps filter min characters: " + channelInfo.getfilterCapsMinCharacters());
 							}
 						}else if(msg[1].equalsIgnoreCase("mincaps")){
 							if(msg.length > 2 && isInteger(msg[2])){
 								channelInfo.setfilterCapsMinCapitals(Integer.parseInt(msg[2]));
 	 							sendMessage(channel, channelInfo.getBullet() + " Caps filter min caps: " + channelInfo.getfilterCapsMinCapitals());
 							}
 						}else if(msg[1].equalsIgnoreCase("status")){
 							sendMessage(channel, channelInfo.getBullet() + " Caps filter=" + channelInfo.getFilterCaps() + ", percent=" + channelInfo.getfilterCapsPercent() + ", minchars=" + channelInfo.getfilterCapsMinCharacters() + ", mincaps= " + channelInfo.getfilterCapsMinCapitals());
 						}
 					}
 				}
 				
 				// !emotes - Owner
 				if(msg[0].equalsIgnoreCase("!emotes") && isOwner){
 					System.out.println("DEBUG: Matched command !emotes");
 					if(msg.length == 1){
 						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!emotes on/off\", \"!emotes max [value]\"");
 					}else if(msg.length > 1){
 						if(msg[1].equalsIgnoreCase("on")){
 							channelInfo.setFilterEmotes(true);
 							sendMessage(channel, channelInfo.getBullet() + " Emotes filter: " + channelInfo.getFilterEmotes());
 						}else if(msg[1].equalsIgnoreCase("off")){
 							channelInfo.setFilterEmotes(false);
 							sendMessage(channel, channelInfo.getBullet() + " Emotes filter: " + channelInfo.getFilterEmotes());
 						}else if(msg[1].equalsIgnoreCase("max")){
 							if(msg.length > 2 && isInteger(msg[2])){
 								channelInfo.setFilterEmotesMax(Integer.parseInt(msg[2]));
 	 							sendMessage(channel, channelInfo.getBullet() + " Emotes filter max: " + channelInfo.getFilterEmotesMax());
 							}
 						}else if(msg[1].equalsIgnoreCase("status")){
 							sendMessage(channel, channelInfo.getBullet() + " Emotes filter=" + channelInfo.getFilterEmotes() + ", max=" + channelInfo.getFilterEmotesMax());
 						}
 					}
 				}
 				
 				// !symbols - Owner
 				if(msg[0].equalsIgnoreCase("!symbols") && isOwner){
 					System.out.println("DEBUG: Matched command !symbols");
 					if(msg.length == 1){
 						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!symbols on/off\"");
 					}else if(msg.length > 1){
 						if(msg[1].equalsIgnoreCase("on")){
 							channelInfo.setFilterSymbols(true);
 							sendMessage(channel, channelInfo.getBullet() + " Symbols filter: " + channelInfo.getFilterSymbols());
 						}else if(msg[1].equalsIgnoreCase("off")){
 							channelInfo.setFilterSymbols(false);
 							sendMessage(channel, channelInfo.getBullet() + " Symbols filter: " + channelInfo.getFilterSymbols());
 						}else if(msg[1].equalsIgnoreCase("status")){
 							sendMessage(channel, channelInfo.getBullet() + " Symbols filter=" + channelInfo.getFilterSymbols());
 						}
 					}
 				}
 				
 				// !regular - Owner
 				if(msg[0].equalsIgnoreCase("!regular") && isOwner){
 					System.out.println("DEBUG: Matched command !regular");
 					if(msg.length < 2){
 						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!regular add/delete [name]\", \"!regular list\"");
 					}else if(msg.length  > 2){
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
 				if(msg[0].equalsIgnoreCase("!mod")  && isOwner){
 					System.out.println("DEBUG: Matched command !mod");
 					if(msg.length < 2){
 						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!mod add/delete [name]\", \"!mod list\"");
 					}if(msg.length  > 2){
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
 				if(msg[0].equalsIgnoreCase("!owner") && isOwner){
 					System.out.println("DEBUG: Matched command !owner");
 					if(msg.length < 2){
 						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!owner add/delete [name]\", \"!owner list\"");
 					}if(msg.length  > 2){
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
 				if(msg[0].equalsIgnoreCase("!set") && isOwner){
 					System.out.println("DEBUG: Matched command !set");
					if(msg.length == 1){
						sendMessage(channel, channelInfo.getBullet() + " Syntax: \"!set [option] [value]\". Options: topic, filters, throw, signedkicks, joinsparts, lastfm, steam, mode, chatlogging, maxlength");
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
					}else if(msg[1].equalsIgnoreCase("signedkicks")){
						if(msg[2].equalsIgnoreCase("on")){
							channelInfo.setSignKicks(true);
							sendMessage(channel, channelInfo.getBullet() + " Feature: Sign-kicks is on");
						}else if(msg[2].equalsIgnoreCase("off")){
							channelInfo.setSignKicks(false);
							sendMessage(channel, channelInfo.getBullet() + " Feature: Sign-kicks is off");
						}
					}else if(msg[1].equalsIgnoreCase("joinsparts")){
						sendMessage(channel, channelInfo.getBullet() + " This feature is currently disabled due to issues with TMI.");
						if(msg[2].equalsIgnoreCase("on")){
							//channelInfo.setAnnounceJoinParts(true);
							//sendMessage(channel, channelInfo.getBullet() + " Feature: Join/Part announcing is on");
						}else if(msg[2].equalsIgnoreCase("off")){
							//channelInfo.setAnnounceJoinParts(false);
							//sendMessage(channel, channelInfo.getBullet() + " Feature: Join/Part announcing is off");
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
						}else if(msg[2].equalsIgnoreCase("-1") || msg[2].equalsIgnoreCase("admin")){
							channelInfo.setMode(-1);
							sendMessage(channel, channelInfo.getBullet() + " Special moderation mode activated.");
						}
					}else if(msg[1].equalsIgnoreCase("chatlogging")){
						if(msg[2].equalsIgnoreCase("on")){
//							channelInfo.setLogging(true);
//							sendMessage(channel, channelInfo.getBullet() + " Chat logging is on");
							sendMessage(channel, channelInfo.getBullet() + " You cannot enable chat logging. If you would like it enabled for your channel, please PM bgeorge.");
						}else if(msg[2].equalsIgnoreCase("off")){
							channelInfo.setLogging(false);
							sendMessage(channel, channelInfo.getBullet() + " Chat logging is off");
						}
					}else if(msg[1].equalsIgnoreCase("maxlength")){
						if(msg.length > 2){
							channelInfo.setFilterMax(Integer.parseInt(msg[2]));
							sendMessage(channel, channelInfo.getBullet() + " Max message length set to " + channelInfo.getFilterMax());
						}else{
							sendMessage(channel, channelInfo.getBullet() + " Max message length is " + channelInfo.getFilterMax() + " characters.");
						}
					}
 				}
 				
 				
 				//!modchan - Mod
 				if(msg[0].equalsIgnoreCase("!modchan") && isOp){
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
 				if (msg[0].equalsIgnoreCase("!join") && BotManager.getInstance().publicJoin){
 							System.out.println("DEBUG: Matched command !join");
							sendMessage(channel, channelInfo.getBullet() + " Joining channel #"+ sender +".");
							boolean joinStatus = BotManager.getInstance().addChannel("#" + sender, 2);
							if(joinStatus){
								sendMessage(channel, channelInfo.getBullet() + " Channel #"+ sender +" joined.");
							}else{
								sendMessage(channel, channelInfo.getBullet() + " Already in channel #"+ sender +".");
							}
				}
 				
 				if (msg[0].equalsIgnoreCase("!rejoin")){
 					System.out.println("DEBUG: Matched command !rejoin");
 					if(msg.length > 1 && isAdmin){
 						if(msg[1].contains("#")){
							sendMessage(channel, channelInfo.getBullet() + " Rejoining channel "+ msg[1] +".");
							boolean joinStatus = BotManager.getInstance().rejoinChannel(msg[1]);
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
						boolean joinStatus = BotManager.getInstance().rejoinChannel("#"+sender);
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
 					sendMessage(channel, channelInfo.getBullet() + " Currently in " + BotManager.getInstance().channelList.size() + " channels.");
 					
 					String channelString = "";
 					for (Map.Entry<String, Channel> entry : BotManager.getInstance().channelList.entrySet())
 					{
 						channelString += entry.getValue().getChannel() + ", ";
 					}
 					
 					sendMessage(channel, channelInfo.getBullet() + " Channels: " + channelString);
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-join") && msg.length > 1 && isAdmin) {

 						if(msg[1].contains("#")){
 							sendMessage(channel, channelInfo.getBullet() + " Joining channel "+ msg[1] +".");
 							boolean joinStatus = BotManager.getInstance().addChannel(msg[1],-1);
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
 						BotManager.getInstance().removeChannel(msg[1]);
 						sendMessage(channel, channelInfo.getBullet() + " Channel "+ msg[1] +" parted.");
 					}else{
 						sendMessage(channel, channelInfo.getBullet() + " Invalid channel format. Must be in format #channelname.");
 					}
 						
 				}
 				
// 				if (msg[0].equalsIgnoreCase("!bm-rejoin") && isAdmin) {
// 					sendMessage(channel, channelInfo.getBullet() + " Rejoining all channels.");
// 					BotManager.getInstance().rejoinChannels();
// 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-reconnect") && isAdmin) {
 					sendMessage(channel, channelInfo.getBullet() + " Reconnecting all servers.");
 					BotManager.getInstance().reconnectAllBotsSoft();
 				}
 				 				
 				if (msg[0].equalsIgnoreCase("!bm-global") && isAdmin) {
 					BotManager.getInstance().sendGlobal(message.substring(11), sender);
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-reload") && msg.length > 1 && isAdmin) {
 					if(msg[1].contains("#")){
 						sendMessage(channel, channelInfo.getBullet() + " Reloading channel "+ msg[1]);
 						BotManager.getInstance().reloadChannel(msg[1]);
 						sendMessage(channel, channelInfo.getBullet() + " Channel "+ msg[1] +" reloaded.");
 					}else{
 						sendMessage(channel, channelInfo.getBullet() + " Invalid channel format. Must be in format #channelname.");
 					}
 						
 				}
 				
 				if (msg[0].equalsIgnoreCase("!bm-color") && msg.length > 1 && isAdmin) {
 					sendMessage(channel, ".color " + msg[1]);
 				}
 				
 				
 				if (msg[0].equalsIgnoreCase("!bm-gbtest") && msg.length > 1 && isAdmin) {
 					for(int i=0; i < Integer.parseInt(msg[1]); i++)
 						sendMessage(channel, ".timeout kappa123 1");
 				}
 				
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
    	
		Channel channelInfo = getChannelObject(channel);
    	
		if(channelInfo == null)
			return;
		
		//Call modules
		for(BotModule b:BotManager.getInstance().getModules()){
			b.onJoin(channelInfo, sender, login, hostname);
		}
		
		if(!channelInfo.getAnnounceJoinParts() || this.getNick().equalsIgnoreCase(sender))
			return;
		
		String prefix = "";
		
		if(BotManager.getInstance().isTagStaff(sender))
			prefix = " [Staff] ";
		else if(BotManager.getInstance().isTagAdmin(sender))
			prefix = " [Admin] ";
		
		sendMessage(channel, "> " + prefix + sender + " entered the room.");
    }

    public void onPart(String channel, String sender, String login, String hostname) {	
    	
		Channel channelInfo = getChannelObject(channel);
    	
		if(channelInfo == null)
			return;
		
		//Call modules
		for(BotModule b:BotManager.getInstance().getModules()){
			b.onPart(channelInfo, sender, login, hostname);
		}
		
		if(!channelInfo.getAnnounceJoinParts() || this.getNick().equalsIgnoreCase(sender))
			return;
		
		String prefix = "";
		
		if(BotManager.getInstance().isTagStaff(sender))
			prefix = " [Staff] ";
		else if(BotManager.getInstance().isTagAdmin(sender))
			prefix = " [Admin] ";
		
		sendMessage(channel, "> " + prefix + sender + " left the room.");
    }
    
	@Override
	protected boolean onMessageSend(String target, String message) {
		Channel channelInfo = getChannelObject(target);
		
		if(!BotManager.getInstance().verboseLogging)
			System.out.println("onMessageSend: " + target + " " + getNick() + " : " + message);

		if(channelInfo != null){
			//Call modules
			for(BotModule b:BotManager.getInstance().getModules()){
				b.onSelfMessage(channelInfo, this.getNick(), message);
			}
		}
		
		//Send message to the balancer
		SenderBotBalancer.getInstance().sendMessage(target, message);
	
		return false;
	}

	@Override
    public void onServerPing(String response) {
		super.onServerPing(response);
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
	
    public void log(String line) {
    	super.log(line);
    	
    	if(BotManager.getInstance().useGUI){
    		BotManager.getInstance().getGUI().log(line);
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
	
	private boolean containsSymbol(String message, Channel ch){
		for(Pattern pattern: symbolsPatterns){
			Matcher match = pattern.matcher(message);
			if(match.matches()){
				System.out.println("DEBUG: Symbol match on " + pattern.pattern());	
				return true;
			}
		}

		return false;
	}
	
	private int countEmotes(String message){
		String str = message;
		int count=0;
		for(String findStr: BotManager.getInstance().emoteSet){
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
		for(Pattern reg:BotManager.getInstance().globalBannedWords){
			Matcher match = reg.matcher(message.toLowerCase());
			if(match.matches()){
				System.out.println("DEBUG: Global banned word matched: " + reg.toString());
				return true;
			}
		}
		return false;
	}
	
	private String getWarningText(int count){
		if(count > (warningTODuration.length-1))
			return warningText[warningTODuration.length-1];
		else
			return warningText[count-1];	
	}
	
	private int getWarningTODuration(int count){
		if(count > (warningTODuration.length-1))
			return warningTODuration[warningTODuration.length-1];
		else
			return warningTODuration[count-1];
	}
	
	private void secondaryTO(final String channel, final String name, final int duration){
		Timer timer = new Timer();
		int delay = 1000;
		
		System.out.println("DEBUG: Issuing a timeout on " + name + " in " + channel + " for " + duration);
		
		timer.schedule(new TimerTask()
	       {
	        public void run() {
	        	ReceiverBot.this.sendMessage(channel,".timeout " + name + " " + duration);
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
	
	private String getStreamList(String key, Channel channelInfo) throws Exception{
		URL feedSource = new URL("http://api.justin.tv/api/stream/list.xml?channel=" + channelInfo.getChannel().substring(1));
		URLConnection uc = feedSource.openConnection();
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(uc.getInputStream());
		doc.getDocumentElement().normalize();

		NodeList nList = doc.getElementsByTagName("stream");
		if(nList.getLength() < 1) 
			throw new Exception();
 
		for (int temp = 0; temp < nList.getLength(); temp++) {
 
		   Node nNode = nList.item(temp);
		   if (nNode.getNodeType() == Node.ELEMENT_NODE) {
		      Element eElement = (Element) nNode;
		      
		      return getTagValue(key, eElement);
 
		   }
		}
		
		return "";
	}
	
	public String getTimeStreaming(String uptime){
		DateFormat format = new SimpleDateFormat("EEE MMMM dd HH:mm:ss yyyy");
		format.setTimeZone(java.util.TimeZone.getTimeZone("US/Pacific"));
		try {
			Date then = format.parse(uptime);
			return this.getTimeTilNow(then);
		} catch (ParseException e) {
			e.printStackTrace();
		}
		
		return "Error getting date.";	
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
		
		if(difference > BotManager.getInstance().pingInterval){
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
					ReceiverBot.this.sendMessage(channelInfo.getChannel(), "> Giveaway over.");
				}
			}
        }
	}
	
	private String fuseArray(String[] array, int start){
		String fused = "";
		for(int c = start; c < array.length; c++)
			fused += array[c] + " ";
		
		return fused.trim();
	
	}
	
	public String getTimeTilNow(Date date){
		long difference = (long) (System.currentTimeMillis()/1000) - (date.getTime()/1000);
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
}
