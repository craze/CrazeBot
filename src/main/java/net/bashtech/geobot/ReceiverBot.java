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

import org.apache.commons.lang3.StringUtils;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.awt.*;
import java.io.*;
import java.net.URLEncoder;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ReceiverBot extends PircBot {
    static Logger LOGGER_D = LoggerFactory.getLogger("debugLogger");
    static Logger LOGGER_R = LoggerFactory.getLogger("recordLogger");


    static ReceiverBot instance;
    Timer joinCheck;
    Random random = new Random();
    private Pattern[] linkPatterns = new Pattern[4];
    private Pattern[] symbolsPatterns = new Pattern[2];
    private int lastPing = -1;
    private char bullet[] = {'>', '+', '-', '~'};
    private int bulletPos = 0;
    private int countToNewColor = BotManager.getInstance().randomNickColorDiff;
    private Pattern twitchnotifySubscriberPattern = Pattern.compile("^([a-z_]+) just subscribed!$", Pattern.CASE_INSENSITIVE);
    private Pattern banNoticePattern = Pattern.compile("^You are permanently banned from talking in ([a-z_]+).$", Pattern.CASE_INSENSITIVE);
    private Pattern toNoticePattern = Pattern.compile("^You are banned from talking in ([a-z_]+) for (?:[0-9]+) more seconds.$", Pattern.CASE_INSENSITIVE);
    private Pattern vinePattern = Pattern.compile(".*(vine|4).*(4|vine).*Google.*", Pattern.CASE_INSENSITIVE);

    private Set<String> joinedChannels = new HashSet<String>();

    public ReceiverBot(String server, int port) {
        ReceiverBot.setInstance(this);
        linkPatterns[0] = Pattern.compile(".*http://.*", Pattern.CASE_INSENSITIVE);
        linkPatterns[1] = Pattern.compile(".*https://.*", Pattern.CASE_INSENSITIVE);
        linkPatterns[2] = Pattern.compile(".*[-A-Za-z0-9]+\\s?(\\.|\\(dot\\))\\s?(ac|ad|ae|aero|af|ag|ai|al|am|an|ao|aq|ar|as|asia|at|au|aw|ax|az|ba|bb|bd|be|bf|bg|bh|bi|biz|bj|bm|bn|bo|br|bs|bt|bv|bw|by|bz|ca|cat|cc|cd|cf|cg|ch|ci|ck|cl|cm|cn|co|com|coop|cr|cu|cv|cw|cx|cy|cz|de|dj|dk|dm|do|dz|ec|edu|ee|eg|er|es|et|eu|fi|fj|fk|fm|fo|fr|ga|gb|gd|ge|gf|gg|gh|gi|gl|gm|gn|gov|gp|gq|gr|gs|gt|gu|gw|gy|hk|hm|hn|hr|ht|hu|id|ie|il|im|in|info|int|io|iq|ir|is|it|je|jm|jo|jobs|jp|ke|kg|kh|ki|km|kn|kp|kr|kw|ky|kz|la|lb|lc|li|lk|lr|ls|lt|lu|lv|ly|ma|mc|md|me|mg|mh|mil|mk|ml|mm|mn|mo|mobi|mp|mq|mr|ms|mt|mu|museum|mv|mw|mx|my|mz|na|name|nc|ne|net|nf|ng|ni|nl|no|np|nr|nu|nz|om|org|pa|pe|pf|pg|ph|pk|pl|pm|pn|post|pr|pro|ps|pt|pw|py|qa|re|ro|rs|ru|rw|sa|sb|sc|sd|se|sg|sh|si|sj|sk|sl|sm|sn|so|sr|st|su|sv|sx|sy|sz|tc|td|tel|tf|tg|th|tj|tk|tl|tm|tn|to|tp|tr|travel|tt|tv|tw|tz|ua|ug|uk|us|uy|uz|va|vc|ve|vg|vi|vn|vu|wf|ws|xxx|ye|yt|za|zm|zw)(\\W|$).*", Pattern.CASE_INSENSITIVE);
        linkPatterns[3] = Pattern.compile(".*(([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])\\.){3}([0-9]|[1-9][0-9]|1[0-9]{2}|2[0-4][0-9]|25[0-5])(\\s+|:|/|$).*");

        symbolsPatterns[0] = Pattern.compile("(\\p{InPhonetic_Extensions}|\\p{InLetterlikeSymbols}|\\p{InDingbats}|\\p{InBoxDrawing}|\\p{InBlockElements}|\\p{InGeometricShapes}|\\p{InHalfwidth_and_Fullwidth_Forms}|つ|°|ຈ|░|▀|▄|̰̦̮̠ę̟̹ͦͯͯ́ͮ̊̐͌̉͑ͨ̊́́̚|U̶̧ͩͭͧ͊̅̊ͥͩ̿̔̔ͥ͌ͬ͊͋ͬ҉|Ọ̵͇̖̖|A̴͍̥̳̠̞̹ͩ̋̆ͤͅ|E̡̛͚̺̖̪͈̲̻̠̰̳̐̿)");
        symbolsPatterns[1] = Pattern.compile("[!-/:-@\\[-`{-~]");

        this.setName(BotManager.getInstance().getInstance().nick);
        this.setLogin("ReceiverGeoBot");
        this.setMessageDelay(0);

        this.setVerbose(BotManager.getInstance().verboseLogging);
        try {
            this.connect(server, port, BotManager.getInstance().getInstance().password);
        } catch (NickAlreadyInUseException e) {
            logMain("[ERROR] Nickname already in use - " + this.getNick() + " " + this.getServer());
        } catch (IOException e) {
            logMain("[ERROR] Unable to connect to server - " + this.getNick() + " " + this.getServer());
        } catch (IrcException e) {
            logMain("[ERROR] Error connecting to server - " + this.getNick() + " " + this.getServer());
        }

    }

    public static ReceiverBot getInstance() {
        return instance;
    }

    public static void setInstance(ReceiverBot rb) {
        if (instance == null) {
            instance = rb;
        }
    }

    private static String getTagValue(String sTag, Element eElement) {
        NodeList nlList = eElement.getElementsByTagName(sTag).item(0).getChildNodes();

        Node nValue = (Node) nlList.item(0);

        return nValue.getNodeValue();
    }

    private Channel getChannelObject(String channel) {
        Channel channelInfo = null;
        channelInfo = BotManager.getInstance().getChannel(channel);
        return channelInfo;
    }

    @Override
    protected void onDeop(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
        recipient = recipient.replace(":", "");
        this.getChannelObject(channel).tagModerators.remove(recipient);
    }

    @Override
    protected void onOp(String channel, String sourceNick, String sourceLogin, String sourceHostname, String recipient) {
        recipient = recipient.replace(":", "");
        this.getChannelObject(channel).tagModerators.add(recipient);
    }

    @Override
    protected void onConnect() {
        //Force TMI to send USERCOLOR AND SPECIALUSER messages.
        //this.sendRawLine("TWITCHCLIENT 3");
        this.sendRawLine("CAP REQ :twitch.tv/tags twitch.tv/commands");
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message) {
        if (!message.startsWith("USERCOLOR") && !message.startsWith("EMOTESET") && !message.startsWith("SPECIALUSER") && !message.startsWith("HISTORYEND") && !message.startsWith("CLEARCHAT") && !message.startsWith("Your color"))
            LOGGER_D.debug("RB PM: " + sender + " " + message);

        Matcher m = banNoticePattern.matcher(message);
        if (m.matches()) {
            String channel = "#" + m.group(1);
            BotManager.getInstance().log("SB: Detected ban in " + channel + ". Parting..");
            BotManager.getInstance().removeChannel(channel);
        }

        m = toNoticePattern.matcher(message);
        if (m.matches()) {
            String channel = "#" + m.group(1);
            BotManager.getInstance().log("SB: Detected timeout in " + channel + ". Parting..");
            BotManager.getInstance().removeChannel(channel);
        }

        if (sender.equals("jtv"))
            onAdministrativeMessage(message, null);
    }

    @Override
    protected void onAction(String sender, String login, String hostname, String target, String action) {
        this.onMessage(target, sender, login, hostname, "/me " + action, null);
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message, String tags) {
        LOGGER_D.debug("Tags: " + tags);
        Map<String, String> tagMap = mapTags(tags);
        Iterator it = tagMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            System.out.println("'" + pairs.getKey() + "' = '" + pairs.getValue() + "'");
        }
        onChannelMessage(channel, sender, message, tagMap);
    }

    @Override
    protected void onUserState(String channel, String tags) {
        LOGGER_D.debug("Got USERSTATE '" + tags + "' for " + channel);

        Map<String, String> tagMap = mapTags(tags);

        Iterator it = tagMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            System.out.println("'" + pairs.getKey() + "' = '" + pairs.getValue() + "'");
        }
    }

    protected void onChannelMessage(String channel, String sender, String message, Map<String, String> tags) {
        Channel channelInfo = getChannelObject(channel);
        String twitchName = channelInfo.getTwitchName();
        String prefix = channelInfo.getPrefix();

        //Handle future administrative messages from JTV
        if (sender.equals("jtv")) {
            onAdministrativeMessage(message, channelInfo);
            return;
        }

        if (!BotManager.getInstance().verboseLogging)
            logMain("MSG: " + channel + " " + sender + " : " + message);

        if (!sender.equalsIgnoreCase(this.getNick()))
            channelInfo.messageCount++; //Inc message count

        //Ignore messages from self.
        if (sender.equalsIgnoreCase(this.getNick())) {
            //System.out.println("Message from bot");
            return;
        }

        //Handle twitchnotify
        if (sender.equals("twitchnotify")) {
            Matcher m = twitchnotifySubscriberPattern.matcher(message);
            if (m.matches()) {
                onNewSubscriber(channelInfo, m.group(1));
            }
        }

        //Split message on spaces.
        String[] msg = message.trim().split(" ");

        // ********************************************************************************
        // ****************************** User Ranks **************************************
        // ********************************************************************************

        boolean isAdmin = false;
        boolean isOwner = false;
        boolean isOp = false;
        boolean isRegular = false;
        int accessLevel = 0;

        //Check for user level based on other factors.
        String v3_user_type = (tags.get("user_type") != null ? tags.get("user_type") : "user");
        String v3_subscriber = (tags.get("subscriber") != null ? tags.get("subscriber") : "0");

        LOGGER_D.debug("v3 user_type = " + v3_user_type);

        if (BotManager.getInstance().isAdmin(sender))
            isAdmin = true;
        if (BotManager.getInstance().isTagAdmin(sender) || BotManager.getInstance().isTagStaff(sender) || BotManager.getInstance().isTagGlobalMod(sender) || v3_user_type.equals("admin") || v3_user_type.equals("staff") || v3_user_type.equals("global_mod"))
            isAdmin = true;
        if (channel.equalsIgnoreCase("#" + sender))
            isOwner = true;
        if (channelInfo.isModerator(sender) || v3_user_type.equals("mod"))
            isOp = true;
        if (channelInfo.isOwner(sender))
            isOwner = true;
        if (channelInfo.isRegular(sender) || (channelInfo.subscriberRegulars && (channelInfo.isSubscriber(sender) || v3_subscriber.equals("1"))))
            isRegular = true;

        if (isRegular)
            LOGGER_D.debug(sender + " is a regular");

        //Give users all the ranks below them
        if (isAdmin) {
            log("" + sender + " is admin.");
            isOwner = true;
            isOp = true;
            isRegular = true;
            accessLevel = 99;
        } else if (isOwner) {
            log("" + sender + " is owner.");
            isOp = true;
            isRegular = true;
            accessLevel = 3;
        } else if (isOp) {
            log("" + sender + " is op.");
            isRegular = true;
            accessLevel = 2;
        } else if (isRegular) {
            log("" + sender + " is regular.");
            accessLevel = 1;
        }


        //!{botname} command
        if (msg[0].equalsIgnoreCase(prefix + this.getName())) {
            if (msg.length >= 2) {

                String[] newMsg = new String[msg.length - 1];
                for (int i = 1; i < msg.length; i++) {
                    newMsg[i - 1] = msg[i];
                }
                msg = newMsg;
                msg[0] = prefix + msg[0];

                message = fuseArray(msg, 0);
            }
        }

        //Impersonation command
        if (isAdmin && msg[0].equalsIgnoreCase(prefix + "imp")) {
            if (msg.length >= 3) {
                channelInfo = getChannelObject("#" + msg[1]);
                twitchName = channelInfo.getTwitchName();

                String[] newMsg = new String[msg.length - 2];
                for (int i = 2; i < msg.length; i++) {
                    newMsg[i - 2] = msg[i];
                }
                msg = newMsg;

                message = fuseArray(msg, 0);
                send(channel, "Impersonating channel " + channelInfo.getChannel() + " with command: " + message);
                LOGGER_D.debug("Impersonating channel " + channelInfo.getChannel() + " with command: " + message);
            }

        }


        //!leave - Owner
        if ((msg[0].equalsIgnoreCase(prefix + "leave") || msg[0].equalsIgnoreCase(prefix + "remove") || msg[0].equalsIgnoreCase(prefix + "part")) && isOwner) {
            send(channel, "Leaving channel " + channelInfo.getChannel() + ".");
            BotManager.getInstance().removeChannel(channelInfo.getChannel());
            return;
        }


        // ********************************************************************************
        // ********************************** Filters *************************************
        // ********************************************************************************

        //Global banned word filter
        if (!isOp && channelInfo.config.getBoolean("globalFilter") && this.isGlobalBannedWord(message)) {
            //this.secondaryBan(channel, sender, FilterType.GLOBALBAN);
            this.secondaryTO(channel, sender, 86400, FilterType.GLOBALBAN, message);
            logMain("GLOBALBAN: Global banned word timeout: " + sender + " in " + channel + " : " + message);
            logGlobalBan(channel, sender, message);
            return;
        }

        // Voluntary Filters
        if (channelInfo.useFilters) {

            if (!isRegular) {
                String normalMessage = org.apache.commons.lang3.StringUtils.stripAccents(message);
                Matcher m = vinePattern.matcher(normalMessage.replaceAll(" ", ""));
                if (m.find()) {
                    logMain("VINEBAN: " + sender + " in " + channel + " : " + message);
                    this.secondaryBan(channel, sender, FilterType.VINE);
                    logGlobalBan(channel, sender, message);
                    return;
                }
            }

            //Me filter
            if (channelInfo.getFilterMe() && !isRegular) {
                if (msg[0].equalsIgnoreCase("/me") || message.startsWith("\u0001ACTION")) {
                    int warningCount = 0;

                    channelInfo.incWarningCount(sender, FilterType.ME);
                    warningCount = channelInfo.getWarningCount(sender, FilterType.ME);
                    this.secondaryTO(channel, sender, this.getTODuration(warningCount, channelInfo), FilterType.ME, message);

                    if (channelInfo.checkSignKicks())
                        send(channel, sender + ", /me is not allowed in this channel - " + this.getTimeoutText(warningCount, channelInfo));

                    return;

                }

            }

            // Cap filter
            if (channelInfo.getFilterCaps() && !isRegular) {
                String messageNoWS = message.replaceAll("\\s", "");
                int capsNumber = getCapsNumber(messageNoWS);
                double capsPercent = ((double) capsNumber / messageNoWS.length()) * 100;
                if (channelInfo.getFilterCaps() && !(isRegular) && message.length() >= channelInfo.getfilterCapsMinCharacters() && capsPercent >= channelInfo.getfilterCapsPercent() && capsNumber >= channelInfo.getfilterCapsMinCapitals()) {
                    int warningCount = 0;

                    channelInfo.incWarningCount(sender, FilterType.CAPS);
                    warningCount = channelInfo.getWarningCount(sender, FilterType.CAPS);
                    this.secondaryTO(channel, sender, this.getTODuration(warningCount, channelInfo), FilterType.CAPS, message);

                    if (channelInfo.checkSignKicks())
                        send(channel, sender + ", please don't shout or talk in all caps - " + this.getTimeoutText(warningCount, channelInfo));

                    return;
                }
            }

            // Link filter
            if (channelInfo.getFilterLinks() && !(isRegular) && this.containsLink(message, channelInfo)) {
                boolean result = channelInfo.linkPermissionCheck(sender);
                int warningCount = 0;
                if (result) {
                    send(channel, "Link permitted. (" + sender + ")");
                } else {

                    channelInfo.incWarningCount(sender, FilterType.LINK);
                    warningCount = channelInfo.getWarningCount(sender, FilterType.LINK);
                    this.secondaryTO(channel, sender, this.getTODuration(warningCount, channelInfo), FilterType.LINK, message);

                    if (channelInfo.checkSignKicks())
                        send(channel, sender + ", please ask a moderator before posting links - " + this.getTimeoutText(warningCount, channelInfo));
                    return;
                }

            }

            // Length filter
            if (!(isRegular) && (message.length() > channelInfo.getFilterMax())) {
                int warningCount = 0;

                channelInfo.incWarningCount(sender, FilterType.LENGTH);
                warningCount = channelInfo.getWarningCount(sender, FilterType.LENGTH);
                this.secondaryTO(channel, sender, this.getTODuration(warningCount, channelInfo), FilterType.LENGTH, message);

                if (channelInfo.checkSignKicks())
                    send(channel, sender + ", please don't spam long messages - " + this.getTimeoutText(warningCount, channelInfo));

                return;
            }

            // Symbols filter
            if (channelInfo.getFilterSymbols() && !(isRegular)) {
                String messageNoWS = message.replaceAll("\\s", "");
                int count = getSymbolsNumber(messageNoWS);
                double percent = (double) count / messageNoWS.length();

                if (count > channelInfo.getFilterSymbolsMin() && (percent * 100 > channelInfo.getFilterSymbolsPercent())) {
                    int warningCount = 0;
                    channelInfo.incWarningCount(sender, FilterType.SYMBOLS);
                    warningCount = channelInfo.getWarningCount(sender, FilterType.SYMBOLS);
                    this.secondaryTO(channel, sender, this.getTODuration(warningCount, channelInfo), FilterType.SYMBOLS, message);

                    if (channelInfo.checkSignKicks())
                        send(channel, sender + ", please don't spam symbols - " + this.getTimeoutText(warningCount, channelInfo));

                    return;
                }
            }

            //Offensive filter
            if (!isRegular && channelInfo.getFilterOffensive()) {
                boolean isOffensive = channelInfo.isOffensive(message);
                if (isOffensive) {
                    int warningCount = 0;

                    channelInfo.incWarningCount(sender, FilterType.OFFENSIVE);
                    warningCount = channelInfo.getWarningCount(sender, FilterType.OFFENSIVE);
                    this.secondaryTO(channel, sender, this.getTODuration(warningCount, channelInfo), FilterType.OFFENSIVE, message);

                    if (channelInfo.checkSignKicks())
                        send(channel, sender + ", disallowed word or phrase - " + this.getTimeoutText(warningCount, channelInfo));

                    return;
                }
            }

            //Emote filter
            if (!isRegular && channelInfo.getFilterEmotes()) {
                String emote_tag = null;
                if (tags.containsKey("emotes"))
                    emote_tag = tags.get("emotes");

                int count_emotes = 0;
                count_emotes = StringUtils.countMatches(emote_tag, "-");
                if (count_emotes > channelInfo.getFilterEmotesMax()) {
                    int warningCount = 0;

                    channelInfo.incWarningCount(sender, FilterType.EMOTES);
                    warningCount = channelInfo.getWarningCount(sender, FilterType.EMOTES);
                    this.secondaryTO(channel, sender, this.getTODuration(warningCount, channelInfo), FilterType.EMOTES, message);

                    if (channelInfo.checkSignKicks())
                        send(channel, sender + ", please don't spam emotes - " + this.getTimeoutText(warningCount, channelInfo));

                    return;

                }

                if (channelInfo.getFilterEmotesSingle() && checkSingleEmote(message, emote_tag)) {
                    int warningCount = 0;

                    channelInfo.incWarningCount(sender, FilterType.EMOTES);
                    warningCount = channelInfo.getWarningCount(sender, FilterType.EMOTES);
                    this.secondaryTO(channel, sender, this.getTODuration(warningCount, channelInfo), FilterType.EMOTES, message);

                    if (channelInfo.checkSignKicks())
                        send(channel, sender + ", single emote messages are not allowed - " + this.getTimeoutText(warningCount, channelInfo));

                    return;

                }

            }

        }

        // ********************************************************************************
        // ***************************** Poll Voting **************************************
        // ********************************************************************************
        if (msg[0].equalsIgnoreCase(prefix + "vote")) {
            log("Matched command !vote (user entry)");
            if (channelInfo.getPoll() != null && channelInfo.getPoll().getStatus() && msg.length > 1) {
                channelInfo.getPoll().vote(sender, msg[1]);
                return;
            }
        }
        // ********************************************************************************
        // ***************************** Giveaway Voting **********************************
        // ********************************************************************************
        if (channelInfo.getGiveaway() != null && channelInfo.getGiveaway().getStatus()) {
            //Giveaway is open and accepting entries.
            channelInfo.getGiveaway().submitEntry(sender, msg[0]);
        }


        // ********************************************************************************
        // ***************************** Raffle Entry *************************************
        // ********************************************************************************
        if (msg[0].equalsIgnoreCase(prefix + "raffle") && msg.length == 1) {
            log("Matched command !raffle (user entry)");
            if (channelInfo.raffle != null) {
                channelInfo.raffle.enter(sender);
                return;
            }
        }
        // ********************************************************************************
        // ******************************* Mode Checks ************************************
        // ********************************************************************************

        //Check channel mode.
        if ((channelInfo.getMode() == 0 || channelInfo.getMode() == -1) && !isOwner)
            return;
        if (channelInfo.getMode() == 1 && !isOp)
            return;

        // ********************************************************************************
        // ********************************* Commands *************************************
        // ********************************************************************************

        // ********************************************************************************
        // ***************************** Info/Catch-all Command ***************************
        // ********************************************************************************

        if (msg[0].substring(0, 1).equalsIgnoreCase(prefix)) {
            String command = msg[0].substring(1);
            String value = channelInfo.getCommand(command);
            if (value != null) {
                log("Matched command " + msg[0]);
                if (msg.length > 1 && isOwner) {
                    String updatedMessage = fuseArray(msg, 1);
                    if (!updatedMessage.contains(",,")) {
                        channelInfo.setCommand(command, updatedMessage);
                        send(channel, "Command updated.");
                    } else {
                        send(channel, "Command cannot contain double commas (\",,\").");
                    }
                } else {
                    if (channelInfo.checkCommandRestriction(command, accessLevel))
                        send(channel, sender, value);
                }
            }
        }

        //Command cooldown check
        if (msg[0].substring(0, 1).equalsIgnoreCase("!") && channelInfo.onCooldown(msg[0])) {
            if (!isOp)
                return;
        }

        // !ping - All
        if (msg[0].equalsIgnoreCase(prefix + "ping") && isOp) {
            log("Matched command !ping");
            String time = new java.util.Date().toString();
            send(channel, "Pong sent at " + time + " (" + this.fuseArray(msg, 1) + ")");
            return;
        }

        // !lockouttest - All
        if (msg[0].equalsIgnoreCase(prefix + "lockouttest")) {
            log("Matched command !lockouttest");
            send(channel, sender + ", your message was received! You are NOT locked out of chat.");
            return;
        }

        // !bothelp - All
        if (msg[0].equalsIgnoreCase(prefix + "bothelp")) {
            log("Matched command !bothelp");
            send(channel, BotManager.getInstance().bothelpMessage);
            return;
        }

        // !viewers - All
        if ((msg[0].equalsIgnoreCase(prefix + "viewers") || msg[0].equalsIgnoreCase(prefix + "lurkers"))) {
            log("Matched command !viewers");
            try {
                send(channel, JSONUtil.krakenViewers(twitchName) + " viewers.");
            } catch (Exception e) {
                send(channel, "Stream is not live.");
            }
            return;
        }

        // !uptime - All
        if (msg[0].equalsIgnoreCase(prefix + "uptime")) {
            log("Matched command !uptime");
            try {
                String uptime = JSONUtil.krakenCreated_at(channelInfo.getTwitchName());
                send(channel, this.getTimeStreaming(uptime));
            } catch (Exception e) {
                send(channel, "Error accessing Twitch API.");
            }
            return;
        }

        // !music - All
        if (msg[0].equalsIgnoreCase(prefix + "music") || msg[0].equalsIgnoreCase(prefix + "lastfm")) {
            log("Matched command !music");
            send(channel, "Now playing: " + JSONUtil.lastFM(channelInfo.getLastfm()));
        }

        // !steam - All
        if (msg[0].equalsIgnoreCase("!steam")) {
            log("Matched command !steam");
            if (channelInfo.getSteam().length() > 1) {

                if (channelInfo.getSteam().length() > 1) {
                    send(channel, JSONUtil.steam(channelInfo.getSteam(), "all"));
                }

            } else {
                send(channel, "Steam ID not set. Do \"!set steam [ID]\" to configure. ID must be in SteamID64 format and profile must be public.");
            }
            return;
        }

        // !game - All
        if (msg[0].equalsIgnoreCase(prefix + "game")) {
            log("Matched command !game");
            if (isOwner && msg.length > 1) {
                String game = this.fuseArray(msg, 1);
                game.trim();
                if (game.equals("-"))
                    game = "";
                try {
                    channelInfo.updateGame(game);
                    send(channel, "Game update sent.");
                } catch (Exception ex) {
                    send(channel, "Error updating game. Did you add me as an editor?");
                }

            } else {
                String game = JSONUtil.krakenGame(twitchName);
                if (game.length() > 0) {
                    send(channel, "Current game: " + game);
                } else {
                    send(channel, "No game set.");
                }
            }
            return;
        }

        // !status - All
        if (msg[0].equalsIgnoreCase(prefix + "status")) {
            log("Matched command !status");
            if (isOwner && msg.length > 1) {
                String status = this.fuseArray(msg, 1);
                status.trim();
                try {
                    channelInfo.updateStatus(status);
                    send(channel, "Status update sent.");
                } catch (Exception ex) {
                    send(channel, "Error updating status. Did you add me as an editor?");
                }
            } else {
                String status = "";
                status = JSONUtil.krakenStatus(twitchName);
                if (status.length() > 0) {
                    send(channel, status);
                } else {
                    send(channel, "Unable to query API.");
                }
            }
            return;
        }

        // !followme - Owner
        if (msg[0].equalsIgnoreCase(prefix + "followme") && isOwner) {
            log("Matched command !followme");
            BotManager.getInstance().followChannel(twitchName);
            send(channel, "Follow update sent.");
            return;
        }

        // !properties - Owner
        if (msg[0].equalsIgnoreCase(prefix + "properties") && isOwner) {
            log("Matched command !properties");
            send(channel, JSONUtil.getChatProperties(channelInfo.getTwitchName()));
            return;
        }

        // !commands - Op/Regular
        if (msg[0].equalsIgnoreCase(prefix + "commands") && isOp) {
            log("Matched command !commands");
            send(channel, "Commands: " + channelInfo.getCommandList());
            return;
        }

        // !throw - All
        if (msg[0].equalsIgnoreCase(prefix + "throw") && (channelInfo.checkThrow() || isRegular)) {
            log("Matched command !throw");
            if (msg.length > 1) {
                String throwMessage = "";
                for (int i = 1; i < msg.length; i++) {
                    throwMessage += msg[i] + " ";
                }
                send(channel, "(╯°□°）╯︵" + throwMessage);
            }
            return;
        }

        // !topic
        if (msg[0].equalsIgnoreCase(prefix + "topic") && channelInfo.useTopic) {
            log("Matched command !topic");
            if (msg.length < 2 || !isOp) {
                if (channelInfo.getTopic().equalsIgnoreCase("")) {
                    String status = "";
                    status = JSONUtil.krakenStatus(twitchName);

                    if (status.length() > 0)
                        send(channel, status);
                    else
                        send(channel, "Unable to query API.");
                } else {
                    send(channel, "Topic: " + channelInfo.getTopic() + " (Set " + channelInfo.getTopicTime() + " ago)");
                }
            } else if (msg.length > 1 && isOp) {
                if (msg[1].equalsIgnoreCase("unset")) {
                    channelInfo.setTopic("");
                    send(channel, "No topic is set.");
                } else {
                    channelInfo.setTopic(message.substring(7));
                    send(channel, "Topic: " + channelInfo.getTopic() + " (Set " + channelInfo.getTopicTime() + " ago)");
                }

            }
            return;
        }

        // !link
        if (msg[0].equalsIgnoreCase(prefix + "link") && isRegular) {
            log("Matched command !link");
            if (msg.length > 1) {
                String rawQuery = message.substring(6);
                String encodedQuery = "";
                try {
                    encodedQuery = URLEncoder.encode(rawQuery, "UTF-8");
                } catch (UnsupportedEncodingException e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                String url = "http://lmgtfy.com/?q=" + encodedQuery;
                send(channel, "Link to \"" + rawQuery + "\" -> " + JSONUtil.shortenURL(url));
            }
            return;
        }

        // !commercial
        if (msg[0].equalsIgnoreCase(prefix + "commercial")) {
            log("Matched command !commercial");
            if (isOwner) {
                channelInfo.runCommercial();
                //send(channel, "Running a 30 second commercial. Thank you for supporting the channel!");
            }
            return;
        }

        // !skipcommercial
        if (msg[0].equalsIgnoreCase(prefix + "skipcommercial")) {
            log("Matched command !skipcommercial");
            if (isOwner) {
                channelInfo.skipNextCommercial = true;
                send(channel, "The next commercial command will be ignored. If you have the command overridden with a custom command it will still be displayed.");
            }
            return;
        }

        // !command - Ops
        if (msg[0].equalsIgnoreCase(prefix + "command") && isOwner) {
            log("Matched command !command");
            if (msg.length < 3) {
                send(channel, "Syntax: \"!command add/delete [name] [message]\" - Name is the command trigger without \"!\" and message is the response.");
            } else if (msg.length > 2) {
                if (msg[1].equalsIgnoreCase("add") && msg.length > 3) {
                    String key = msg[2].replaceAll("[^a-zA-Z0-9]", "");
                    String value = fuseArray(msg, 3);
                    if (!value.contains(",,")) {
                        channelInfo.setCommand(key, value);
                        send(channel, "Command added/updated.");
                    } else {
                        send(channel, "Command cannot contain double commas (\",,\").");
                    }

                } else if (msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")) {
                    String key = msg[2];
                    channelInfo.removeCommand(key);
                    channelInfo.removeRepeatCommand(key);
                    channelInfo.removeScheduledCommand(key);

                    send(channel, "Command " + key + " removed.");

                } else if (msg[1].equalsIgnoreCase("restrict") && msg.length >= 4) {
                    String command = msg[2];
                    String levelStr = msg[3].toLowerCase();
                    int level = 0;
                    if (channelInfo.getCommand(command) != null) {
                        if (levelStr.equalsIgnoreCase("owner") || levelStr.equalsIgnoreCase("owners"))
                            level = 3;
                        if (levelStr.equalsIgnoreCase("mod") || levelStr.equalsIgnoreCase("mods"))
                            level = 2;
                        if (levelStr.equalsIgnoreCase("regular") || levelStr.equalsIgnoreCase("regulars"))
                            level = 1;
                        if (levelStr.equalsIgnoreCase("everyone"))
                            level = 0;

                        if (channelInfo.setCommandsRestriction(command, level))
                            send(channel, prefix + command + " restricted to " + levelStr + " only.");
                        else
                            send(channel, "Error setting restriction.");
                    } else {
                        send(channel, "Command does not exist.");
                    }
                }
            }
            return;
        }

        // !repeat - Ops
        if (msg[0].equalsIgnoreCase(prefix + "repeat") && isOwner) {
            log("Matched command !repeat");
            if (msg.length < 3) {
                if (msg.length > 1 && msg[1].equalsIgnoreCase("list")) {
                    String commandsRepeatKey = "";

                    Iterator itr = channelInfo.commandsRepeat.entrySet().iterator();

                    while (itr.hasNext()) {
                        Map.Entry pairs = (Map.Entry) itr.next();
                        RepeatCommand rc = (RepeatCommand) pairs.getValue();
                        commandsRepeatKey += pairs.getKey() + " [" + (rc.active == true ? "ON" : "OFF") + "]" + ", ";
                    }
                    send(channel, "Repeating commands: " + commandsRepeatKey);
                } else {
                    send(channel, "Syntax: \"!repeat add/delete [commandname] [delay in seconds] [message difference - optional]\"");
                }
            } else if (msg.length > 2) {
                if (msg[1].equalsIgnoreCase("add") && msg.length > 3) {
                    String key = msg[2];
                    try {
                        int delay = Integer.parseInt(msg[3]);
                        int difference = 1;
                        if (msg.length == 5)
                            difference = Integer.parseInt(msg[4]);

                        if (channelInfo.getCommand(key) == null || delay < 30) {
                            //Key not found or delay to short
                            send(channel, "Command not found or delay is less than 30 seconds.");
                        } else {
                            channelInfo.setRepeatCommand(key, delay, difference);
                            send(channel, "Command " + key + " will repeat every " + delay + " seconds if " + difference + " messages have passed.");
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                } else if (msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")) {
                    String key = msg[2];
                    channelInfo.removeRepeatCommand(key);
                    send(channel, "Command " + key + " will no longer repeat.");

                } else if (msg[1].equalsIgnoreCase("on") || msg[1].equalsIgnoreCase("off")) {
                    String key = msg[2];
                    if (msg[1].equalsIgnoreCase("on")) {
                        channelInfo.setRepeatCommandStatus(key, true);
                        send(channel, "Repeat command " + key + " has been enabled.");
                    } else if (msg[1].equalsIgnoreCase("off")) {
                        channelInfo.setRepeatCommandStatus(key, false);
                        send(channel, "Repeat command " + key + " has been disabled.");
                    }

                }
            }
            return;
        }

        // !schedule - Ops
        if (msg[0].equalsIgnoreCase(prefix + "schedule") && isOwner) {
            log("Matched command !schedule");
            if (msg.length < 3) {
                if (msg.length > 1 && msg[1].equalsIgnoreCase("list")) {
                    String commandsScheduleKey = "";

                    Iterator itr = channelInfo.commandsSchedule.entrySet().iterator();

                    while (itr.hasNext()) {
                        Map.Entry pairs = (Map.Entry) itr.next();
                        ScheduledCommand sc = (ScheduledCommand) pairs.getValue();
                        commandsScheduleKey += pairs.getKey() + " [" + (sc.active == true ? "ON" : "OFF") + "]" + ", ";
                    }
                    send(channel, "Scheduled commands: " + commandsScheduleKey);
                } else {
                    send(channel, "Syntax: \"!schedule add/delete/on/off [commandname] [pattern] [message difference - optional]\"");
                }
            } else if (msg.length > 2) {
                if (msg[1].equalsIgnoreCase("add") && msg.length > 3) {
                    String key = msg[2];
                    try {
                        String pattern = msg[3];
                        if (pattern.equals("hourly"))
                            pattern = "0 * * * *";
                        else if (pattern.equals("semihourly"))
                            pattern = "0,30 * * * *";
                        else
                            pattern = pattern.replace("_", " ");

                        int difference = 1;
                        if (msg.length == 5)
                            difference = Integer.parseInt(msg[4]);

                        if (channelInfo.getCommand(key) == null || pattern.contains(",,")) {
                            //Key not found or delay to short
                            send(channel, "Command not found or invalid pattern.");
                        } else {
                            channelInfo.setScheduledCommand(key, pattern, difference);
                            send(channel, "Command " + key + " will repeat every " + pattern + " if " + difference + " messages have passed.");
                        }

                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }

                } else if (msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")) {
                    String key = msg[2];
                    channelInfo.removeScheduledCommand(key);
                    send(channel, "Command " + key + " will no longer repeat.");

                } else if (msg[1].equalsIgnoreCase("on") || msg[1].equalsIgnoreCase("off")) {
                    String key = msg[2];
                    if (msg[1].equalsIgnoreCase("on")) {
                        channelInfo.setScheduledCommandStatus(key, true);
                        send(channel, "Scheduled command " + key + " has been enabled.");
                    } else if (msg[1].equalsIgnoreCase("off")) {
                        channelInfo.setScheduledCommandStatus(key, false);
                        send(channel, "Scheduled command " + key + " has been disabled.");
                    }

                }
            }
            return;
        }

        // !autoreply - Ops
        if (msg[0].equalsIgnoreCase(prefix + "autoreply") && isOp) {
            log("Matched command !autoreply");
            if (msg.length < 3) {
                if (msg.length > 1 && msg[1].equalsIgnoreCase("list")) {
                    for (int i = 0; i < channelInfo.autoReplyTrigger.size(); i++) {
                        String cleanedTrigger = channelInfo.autoReplyTrigger.get(i).toString().replaceAll("\\.\\*", "*").replaceAll("\\\\Q", "").replaceAll("\\\\E", "");
                        send(channel, "[" + (i + 1) + "] " + cleanedTrigger + " ---> " + channelInfo.autoReplyResponse.get(i));
                    }
                } else {
                    send(channel, "Syntax: \"!autoreply add/delete/list [pattern] [response]\"");
                }
            } else if (msg.length > 2) {
                if (msg[1].equalsIgnoreCase("add") && msg.length > 3) {
                    String pattern = msg[2].replaceAll("_", " ");
                    String response = fuseArray(msg, 3);

                    channelInfo.addAutoReply(pattern, response);
                    send(channel, "Autoreply added.");
                } else if ((msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")) && msg.length > 2) {
                    if (Main.isInteger(msg[2])) {
                        int pos = Integer.parseInt(msg[2]);

                        if (channelInfo.removeAutoReply(pos))
                            send(channel, "Autoreply removed.");
                        else
                            send(channel, "Autoreply not found. Are you sure you have the correct number?");
                    }
                }
            }
            return;
        }


        // !poll - Ops
        if (msg[0].equalsIgnoreCase(prefix + "poll") && isOp) {
            log("Matched command !poll");
            if (msg.length < 2) {
                send(channel, "Syntax: \"!poll create [option option ... option]\"");
            } else if (msg.length >= 2) {
                if (msg[1].equalsIgnoreCase("create")) {
                    String[] options = new String[msg.length - 2];
                    int oc = 0;
                    for (int c = 2; c < msg.length; c++) {
                        options[oc] = msg[c];
                        oc++;
                    }
                    channelInfo.setPoll(new Poll(options));
                    send(channel, "Poll created. Do '!poll start' to start voting.");
                } else if (msg[1].equalsIgnoreCase("start")) {
                    if (channelInfo.getPoll() != null) {
                        if (channelInfo.getPoll().getStatus()) {
                            send(channel, "Poll is alreay running.");
                        } else {
                            channelInfo.getPoll().setStatus(true);
                            send(channel, "Poll started. Type: !vote <option> to start voting.");
                        }
                    }
                } else if (msg[1].equalsIgnoreCase("stop")) {
                    if (channelInfo.getPoll() != null) {
                        if (channelInfo.getPoll().getStatus()) {
                            channelInfo.getPoll().setStatus(false);
                            send(channel, "Poll stopped.");
                        } else {
                            send(channel, "Poll is not running.");
                        }
                    }
                } else if (msg[1].equalsIgnoreCase("results")) {
                    if (channelInfo.getPoll() != null) {
                        send(channel, channelInfo.getPoll().getResultsString());
//								String[] results = channelInfo.getPoll().getResults();
//								for(int c=0;c<results.length;c++){
//									send(channel, results[c]);
//								}
                    }

                }
            }
            return;
        }

        // !giveaway - Ops
        if ((msg[0].equalsIgnoreCase(prefix + "giveaway") || msg[0].equalsIgnoreCase("!ga")) && isOp) {
            log("Matched command !giveaway");
            if (msg.length < 2) {
                send(channel, "Syntax: \"!giveaway create [max number] [time to run in seconds]\". Time is optional.");
            } else if (msg.length >= 2) {
                if (msg[1].equalsIgnoreCase("create")) {
                    String max = "" + 100;
                    if (msg.length > 2) {
                        max = msg[2];
                    }
                    channelInfo.setGiveaway(new Giveaway(max));
                    if (msg.length > 3 && channelInfo.getGiveaway().isInteger(msg[3])) {
                        this.startGaTimer(Integer.parseInt(msg[3]), channelInfo);
                    } else {
                        send(channel, "Giveaway created. Do !giveaway start' to start." + " Range 1-" + channelInfo.getGiveaway().getMax() + ".");
                    }
                } else if (msg[1].equalsIgnoreCase("start")) {
                    if (channelInfo.getGiveaway() != null) {
                        if (channelInfo.getGiveaway().getStatus()) {
                            send(channel, "Giveaway is alreay running.");
                        } else {
                            channelInfo.getGiveaway().setStatus(true);
                            send(channel, "Giveaway started.");
                        }
                    }
                } else if (msg[1].equalsIgnoreCase("stop")) {
                    if (channelInfo.getGiveaway() != null) {
                        if (channelInfo.getGiveaway().getStatus()) {
                            channelInfo.getGiveaway().setStatus(false);
                            send(channel, "Giveaway stopped.");
                        } else {
                            send(channel, "Giveaway is not running.");
                        }
                    }
                } else if (msg[1].equalsIgnoreCase("results")) {
                    if (channelInfo.getGiveaway() != null) {
                        send(channel, channelInfo.getGiveaway().getResultsString());
//								String[] results = channelInfo.getGiveaway().getResults();
//								for(int c=0;c<results.length;c++){
//									send(channel, results[c]);
//								}
                    } else {
                        send(channel, "No giveaway results.");
                    }

                }
            }
            return;
        }

        // !raffle - Ops
        if (msg[0].equalsIgnoreCase(prefix + "raffle") && isOp) {
            log("Matched command !raffle");
            if (msg.length >= 2) {
                if (msg[1].equalsIgnoreCase("enable")) {
                    if (channelInfo.raffle == null) {
                        channelInfo.raffle = new Raffle();
                    }
                    channelInfo.raffle.setEnabled(true);

                    send(channel, "Raffle enabled.");
                } else if (msg[1].equalsIgnoreCase("disable")) {
                    if (channelInfo.raffle != null) {
                        channelInfo.raffle.setEnabled(false);
                    }

                    send(channel, "Raffle disabled.");
                } else if (msg[1].equalsIgnoreCase("reset")) {
                    if (channelInfo.raffle != null) {
                        channelInfo.raffle.reset();
                    }

                    send(channel, "Raffle entries cleared.");
                } else if (msg[1].equalsIgnoreCase("count")) {
                    if (channelInfo.raffle != null) {
                        send(channel, "Raffle has " + channelInfo.raffle.count() + " entries.");
                    } else {
                        send(channel, "Raffle has 0 entries.");
                    }
                } else if (msg[1].equalsIgnoreCase("winner")) {
                    if (channelInfo.raffle != null) {
                        send(channel, "Winner is " + channelInfo.raffle.pickWinner() + "!");
                    } else {
                        send(channel, "No raffle history found.");
                    }
                }
            } else {
                if (channelInfo.raffle != null) {
                    channelInfo.raffle.enter(sender);
                }
            }
            return;
        }

        // !random - Ops
        if (msg[0].equalsIgnoreCase(prefix + "random") && isRegular) {
            log("Matched command !random");
            if (msg.length >= 2) {
                if (msg[1].equalsIgnoreCase("coin")) {
                    Random rand = new Random();
                    boolean coin = rand.nextBoolean();
                    if (coin == true)
                        send(channel, "Heads!");
                    else
                        send(channel, "Tails!");
                }
            }
            return;
        }

        // ********************************************************************************
        // ***************************** Moderation Commands ******************************
        // ********************************************************************************

        //Moderation commands - Ops
        if (isOp) {
            if (msg[0].equalsIgnoreCase("+m")) {
                sendCommand(channel, ".slow");
            }
            if (msg[0].equalsIgnoreCase("-m")) {
                sendCommand(channel, ".slowoff");
            }
            if (msg[0].equalsIgnoreCase("+s")) {
                sendCommand(channel, ".subscribers");
            }
            if (msg[0].equalsIgnoreCase("-s")) {
                sendCommand(channel, ".subscribersoff");
            }
            if (msg.length > 0) {
                if (msg[0].equalsIgnoreCase("+b")) {
                    sendCommand(channel, ".ban " + msg[1].toLowerCase());
                }
                if (msg[0].equalsIgnoreCase("-b")) {
                    sendCommand(channel, ".unban " + msg[1].toLowerCase());
                    sendCommand(channel, ".timeout " + msg[1].toLowerCase() + " 1");
                }
                if (msg[0].equalsIgnoreCase("+k")) {
                    sendCommand(channel, ".timeout " + msg[1].toLowerCase());
                }
                if (msg[0].equalsIgnoreCase("+p")) {
                    sendCommand(channel, ".timeout " + msg[1].toLowerCase() + " 1");
                }
            }

        }

        // !clear - Ops
        if (msg[0].equalsIgnoreCase(prefix + "clear") && isOp) {
            log("Matched command !clear");
            sendCommand(channel, ".clear");
            return;
        }

        //Filters
        if (msg[0].equalsIgnoreCase(prefix + "filter") && isOwner) {
            if (msg.length < 2) {
                send(channel, "Syntax: !filter <option> [sub options]. Options: on/off, status, me, ignoresubs, enablewarnings, timeoutduration, displaywarnings, messagelength, links, pd, banphrase, caps, emotes, and symbols.");
                return;
            }

            //Shift down a notch
            String[] newMsg = new String[msg.length - 1];
            for (int i = 1; i < msg.length; i++) {
                newMsg[i - 1] = msg[i];
            }
            msg = newMsg;

            //Global disable
            if (msg[0].equalsIgnoreCase("on")) {
                channelInfo.setFiltersFeature(true);
                send(channel, "Feature: Filters is on");
                return;
            } else if (msg[0].equalsIgnoreCase("off")) {
                channelInfo.setFiltersFeature(false);
                send(channel, "Feature: Filters is off");
                return;
            }

            if (msg[0].equalsIgnoreCase("status")) {
                send(channel, "Global: " + channelInfo.useFilters);
                send(channel, "Enable warnings: " + channelInfo.getEnableWarnings());
                send(channel, "Timeout duration: " + channelInfo.getTimeoutDuration());
                send(channel, "Display warnings: " + channelInfo.checkSignKicks());
                send(channel, "Max message length: " + channelInfo.getFilterMax());
                send(channel, "Ignore Subs: " + channelInfo.getSubscriberRegulars());
                send(channel, "Me: " + channelInfo.getFilterMe());
                send(channel, "Links: " + channelInfo.getFilterLinks());
                send(channel, "Banned phrases: " + channelInfo.getFilterOffensive() + " ~ severity=" + channelInfo.config.getInt("banPhraseSeverity"));
                send(channel, "Caps: " + channelInfo.getFilterCaps() + " ~ percent=" + channelInfo.getfilterCapsPercent() + ", minchars=" + channelInfo.getfilterCapsMinCharacters() + ", mincaps=" + channelInfo.getfilterCapsMinCapitals());
                send(channel, "Emotes: " + channelInfo.getFilterEmotes() + " ~ max=" + channelInfo.getFilterEmotesMax() + ", single=" + channelInfo.getFilterEmotesSingle());
                send(channel, "Symbols: " + channelInfo.getFilterSymbols() + " ~ percent=" + channelInfo.getFilterSymbolsPercent() + ", min=" + channelInfo.getFilterSymbolsMin());
            }

            if (msg[0].equalsIgnoreCase("me") && msg.length == 2) {
                if (msg[1].equalsIgnoreCase("on")) {
                    channelInfo.setFilterMe(true);
                    send(channel, "Feature: /me filter is on");
                } else if (msg[1].equalsIgnoreCase("off")) {
                    channelInfo.setFilterMe(false);
                    send(channel, "Feature: /me filter is off");
                }
                return;
            }

            if (msg[0].equalsIgnoreCase("enablewarnings") && msg.length == 2) {
                if (msg[1].equalsIgnoreCase("on")) {
                    channelInfo.setEnableWarnings(true);
                    send(channel, "Feature: Timeout warnings are on");
                } else if (msg[1].equalsIgnoreCase("off")) {
                    channelInfo.setEnableWarnings(false);
                    send(channel, "Feature: Timeout warnings are off");
                }
            }

            if (msg[0].equalsIgnoreCase("ignoresubs") && msg.length == 2) {
                if (msg[1].equalsIgnoreCase("on")) {
                    channelInfo.setSubscriberRegulars(true);
                    send(channel, "Subscribers are now immune to filters.");
                } else if (msg[1].equalsIgnoreCase("off")) {
                    channelInfo.setSubscriberRegulars(false);
                    send(channel, "Subscribers are no longer immune to filters.");
                }

            }

            if (msg[0].equalsIgnoreCase("timeoutduration") && msg.length == 2) {
                if (Main.isInteger(msg[1])) {
                    int duration = Integer.parseInt(msg[1]);
                    channelInfo.setTimeoutDuration(duration);
                    send(channel, "Timeout duration is " + channelInfo.getTimeoutDuration());
                } else {
                    send(channel, "You must specify an integer for the duration");
                }
            }

            if (msg[0].equalsIgnoreCase("displaywarnings") && msg.length == 2) {
                if (msg[1].equalsIgnoreCase("on")) {
                    channelInfo.setSignKicks(true);
                    send(channel, "Feature: Display warnings is on");
                } else if (msg[1].equalsIgnoreCase("off")) {
                    channelInfo.setSignKicks(false);
                    send(channel, "Feature: Display warnings is off");
                }
            }

            if (msg[0].equalsIgnoreCase("messagelength") && msg.length == 2) {
                if (Main.isInteger(msg[1])) {
                    channelInfo.setFilterMax(Integer.parseInt(msg[1]));
                    send(channel, "Max message length set to " + channelInfo.getFilterMax());
                } else {
                    send(channel, "Must be an integer.");
                }
            }


            // !links - Owner
            if (msg[0].equalsIgnoreCase("links")) {
                log("Matched command !links");
                if (msg.length == 1) {
                    send(channel, "Syntax: \"!links on/off\"");
                } else if (msg.length == 2) {
                    if (msg[1].equalsIgnoreCase("on")) {
                        channelInfo.setFilterLinks(true);
                        send(channel, "Link filter: " + channelInfo.getFilterLinks());
                    } else if (msg[1].equalsIgnoreCase("off")) {
                        channelInfo.setFilterLinks(false);
                        send(channel, "Link filter: " + channelInfo.getFilterLinks());
                    }
                }
                return;
            }

            // !pd - Owner
            if (msg[0].equalsIgnoreCase("pd")) {
                log("Matched command !pd");
                if (msg.length == 1) {
                    send(channel, "Syntax: \"!pd add/delete [domain]\" and \"!pd list\"");
                } else if (msg.length > 2) {
                    if (msg[1].equalsIgnoreCase("add")) {
                        if (channelInfo.isDomainPermitted(msg[2])) {
                            send(channel, "Domain already exists. " + "(" + msg[2] + ")");
                        } else {
                            channelInfo.addPermittedDomain(msg[2]);
                            send(channel, "Domain added. " + "(" + msg[2] + ")");
                        }
                    } else if (msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")) {
                        if (channelInfo.isDomainPermitted(msg[2])) {
                            channelInfo.removePermittedDomain(msg[2]);
                            send(channel, "Domain removed. " + "(" + msg[2] + ")");
                        } else {
                            send(channel, "Domain does not exist. " + "(" + msg[2] + ")");
                        }
                    }
                } else if (msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner) {
                    String tempList = "Permitted domains: ";
                    for (String s : channelInfo.getpermittedDomains()) {
                        tempList += s + ", ";
                    }
                    send(channel, tempList);
                }
                return;
            }

            // !banphrase - Owner
            if (msg[0].equalsIgnoreCase("banphrase")) {
                log("Matched command !banphrase");
                if (isOwner)
                    log("Is owner");
                if (msg.length == 1) {
                    send(channel, "Syntax: \"!banphrase on/off\", \"!banphrase add/delete [string to purge]\", \"!banphrase list\"");
                } else if (msg.length > 1) {
                    if (msg[1].equalsIgnoreCase("on")) {
                        channelInfo.setFilterOffensive(true);
                        send(channel, "Ban phrase filter is on");
                    } else if (msg[1].equalsIgnoreCase("off")) {
                        channelInfo.setFilterOffensive(false);
                        send(channel, "Ban phrase filter is off");
                    } else if (msg[1].equalsIgnoreCase("clear")) {
                        channelInfo.clearBannedPhrases();
                        send(channel, "Banned phrases cleared.");
                    } else if (msg[1].equalsIgnoreCase("list")) {
                        String tempList = "Banned phrases words: ";
                        for (String s : channelInfo.getOffensive()) {
                            tempList += s + ", ";
                        }
                        send(channel, tempList);
                    } else if (msg[1].equalsIgnoreCase("add") && msg.length > 2) {
                        String phrase = fuseArray(msg, 2);
                        if (phrase.contains(",,")) {
                            send(channel, "Cannot contain double commas (,,)");
                        } else if (channelInfo.isBannedPhrase(fuseArray(msg, 2))) {
                            send(channel, "Word already exists. " + "(" + phrase + ")");
                        } else {
                            if (phrase.startsWith("REGEX:") && !isAdmin) {
                                send(channel, "You must have Admin status to add regex phrases.");
                                return;
                            }
                            channelInfo.addOffensive(phrase);
                            send(channel, "Word added. " + "(" + phrase + ")");
                        }
                    } else if (msg[1].equalsIgnoreCase("severity")) {
                        if (msg.length > 2 && Main.isInteger(msg[2])) {
                            int severity = Integer.parseInt(msg[2]);
                            channelInfo.config.setInt("banPhraseSeverity", severity);

                            send(channel, "Severity set to " + channelInfo.config.getInt("banPhraseSeverity"));
                        } else {
                            send(channel, "Severity is " + channelInfo.config.getInt("banPhraseSeverity"));
                        }
                    } else if (msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove") && msg.length > 2) {
                        String phrase = fuseArray(msg, 2);
                        channelInfo.removeOffensive(phrase);
                        send(channel, "Word removed. " + "(" + phrase + ")");
                    }
                }
                return;
            }

            // !caps - Owner
            if (msg[0].equalsIgnoreCase("caps")) {
                log("Matched command !caps");
                if (msg.length == 1) {
                    send(channel, "Syntax: \"!caps on/off\", \"!caps percent/minchars/mincaps [value]\", \"!caps status\"");
                } else if (msg.length > 1) {
                    if (msg[1].equalsIgnoreCase("on")) {
                        channelInfo.setFilterCaps(true);
                        send(channel, "Caps filter: " + channelInfo.getFilterCaps());
                    } else if (msg[1].equalsIgnoreCase("off")) {
                        channelInfo.setFilterCaps(false);
                        send(channel, "Caps filter: " + channelInfo.getFilterCaps());
                    } else if (msg[1].equalsIgnoreCase("percent")) {
                        if (msg.length > 2) {
                            channelInfo.setfilterCapsPercent(Integer.parseInt(msg[2]));
                            send(channel, "Caps filter percent: " + channelInfo.getfilterCapsPercent());
                        }
                    } else if (msg[1].equalsIgnoreCase("minchars")) {
                        if (msg.length > 2 && Main.isInteger(msg[2])) {
                            channelInfo.setfilterCapsMinCharacters(Integer.parseInt(msg[2]));
                            send(channel, "Caps filter min characters: " + channelInfo.getfilterCapsMinCharacters());
                        }
                    } else if (msg[1].equalsIgnoreCase("mincaps")) {
                        if (msg.length > 2 && Main.isInteger(msg[2])) {
                            channelInfo.setfilterCapsMinCapitals(Integer.parseInt(msg[2]));
                            send(channel, "Caps filter min caps: " + channelInfo.getfilterCapsMinCapitals());
                        }
                    } else if (msg[1].equalsIgnoreCase("status")) {
                        send(channel, "Caps filter=" + channelInfo.getFilterCaps() + ", percent=" + channelInfo.getfilterCapsPercent() + ", minchars=" + channelInfo.getfilterCapsMinCharacters() + ", mincaps=" + channelInfo.getfilterCapsMinCapitals());
                    }
                }
                return;
            }

            // !emotes - Owner
            if (msg[0].equalsIgnoreCase("emotes")) {
                log("Matched command !emotes");
                if (msg.length == 1) {
                    send(channel, "Syntax: \"!emotes on/off\", \"!emotes max [value]\", \"!emotes single on/off\"");
                } else if (msg.length > 1) {
                    if (msg[1].equalsIgnoreCase("on")) {
                        channelInfo.setFilterEmotes(true);
                        send(channel, "Emotes filter: " + channelInfo.getFilterEmotes());
                    } else if (msg[1].equalsIgnoreCase("off")) {
                        channelInfo.setFilterEmotes(false);
                        send(channel, "Emotes filter: " + channelInfo.getFilterEmotes());
                    } else if (msg[1].equalsIgnoreCase("max")) {
                        if (msg.length > 2 && Main.isInteger(msg[2])) {
                            channelInfo.setFilterEmotesMax(Integer.parseInt(msg[2]));
                            send(channel, "Emotes filter max: " + channelInfo.getFilterEmotesMax());
                        }
                    } else if (msg[1].equalsIgnoreCase("status")) {
                        send(channel, "Emotes filter=" + channelInfo.getFilterEmotes() + ", max=" + channelInfo.getFilterEmotesMax() + ", single=" + channelInfo.getFilterEmotesSingle());
                    } else if (msg[1].equalsIgnoreCase("single") && msg.length > 2) {
                        if (msg[2].equalsIgnoreCase("on")) {
                            channelInfo.setFilterEmotesSingle(true);
                            send(channel, "Single Emote filter: " + channelInfo.getFilterEmotesSingle());
                        } else if (msg[2].equalsIgnoreCase("off")) {
                            channelInfo.setFilterEmotesSingle(false);
                            send(channel, "Single Emote filter: " + channelInfo.getFilterEmotesSingle());
                        }
                    }
                }
                return;
            }

            // !symbols - Owner
            if (msg[0].equalsIgnoreCase("symbols")) {
                log("Matched command !symbols");
                if (msg.length == 1) {
                    send(channel, "Syntax: \"!symbols on/off\", \"!symbols percent/min [value]\", \"!symbols status\"");
                } else if (msg.length > 1) {
                    if (msg[1].equalsIgnoreCase("on")) {
                        channelInfo.setFilterSymbols(true);
                        send(channel, "Symbols filter: " + channelInfo.getFilterSymbols());
                    } else if (msg[1].equalsIgnoreCase("off")) {
                        channelInfo.setFilterSymbols(false);
                        send(channel, "Symbols filter: " + channelInfo.getFilterSymbols());
                    } else if (msg[1].equalsIgnoreCase("percent")) {
                        if (msg.length > 2 && Main.isInteger(msg[2])) {
                            channelInfo.setFilterSymbolsPercent(Integer.parseInt(msg[2]));
                            send(channel, "Symbols filter percent: " + channelInfo.getFilterSymbolsPercent());
                        }
                    } else if (msg[1].equalsIgnoreCase("min")) {
                        if (msg.length > 2 && Main.isInteger(msg[2])) {
                            channelInfo.setFilterSymbolsMin(Integer.parseInt(msg[2]));
                            send(channel, "Symbols filter min symbols: " + channelInfo.getFilterSymbolsMin());
                        }
                    } else if (msg[1].equalsIgnoreCase("status")) {
                        send(channel, "Symbols filter=" + channelInfo.getFilterSymbols() + ", percent=" + channelInfo.getFilterSymbolsPercent() + ", min=" + channelInfo.getFilterSymbolsMin());
                    }
                }
                return;
            }


            return;
        }

        // !permit - Allows users to post 1 link
        if ((msg[0].equalsIgnoreCase(prefix + "permit") || msg[0].equalsIgnoreCase(prefix + "allow")) && channelInfo.getFilterLinks() && channelInfo.useFilters && isOp) {
            log("Matched command !permit");
            if (msg.length == 1) {
                send(channel, "Syntax: \"!permit [username]\"");
            } else if (msg.length > 1) {
                if (!channelInfo.isRegular(msg[1])) {
                    channelInfo.permitUser(msg[1]);
                    send(channel, msg[1] + " may now post 1 link.");
                } else {
                    send(channel, msg[1] + " is a regular and does not need to be permitted.");
                }
            }
            return;
        }

        // !regular - Owner
        if (msg[0].equalsIgnoreCase(prefix + "regular") && isOwner) {
            log("Matched command !regular");
            if (msg.length < 2) {
                send(channel, "Syntax: \"!regular add/delete [name]\", \"!regular list\"");
            } else if (msg.length > 2) {
                if (msg[1].equalsIgnoreCase("add")) {
                    if (channelInfo.isRegular(msg[2])) {
                        send(channel, "User already exists." + "(" + msg[2] + ")");
                    } else {
                        channelInfo.addRegular(msg[2]);
                        send(channel, "User added. " + "(" + msg[2] + ")");
                    }
                } else if (msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")) {
                    if (channelInfo.isRegular(msg[2])) {
                        channelInfo.removeRegular(msg[2]);
                        send(channel, "User removed." + "(" + msg[2] + ")");
                    } else {
                        send(channel, "User does not exist. " + "(" + msg[2] + ")");
                    }
                }
            } else if (msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner) {
                String tempList = "Regulars: ";
                for (String s : channelInfo.getRegulars()) {
                    tempList += s + ", ";
                }
                send(channel, tempList);
            }
            return;
        }

        // !mod - Owner
        if (msg[0].equalsIgnoreCase(prefix + "mod") && isOwner) {
            log("Matched command !mod");
            if (msg.length < 2) {
                send(channel, "Syntax: \"!mod add/delete [name]\", \"!mod list\"");
            }
            if (msg.length > 2) {
                if (msg[1].equalsIgnoreCase("add")) {
                    if (channelInfo.isModerator(msg[2])) {
                        send(channel, "User already exists. " + "(" + msg[2] + ")");
                    } else {
                        channelInfo.addModerator(msg[2]);
                        send(channel, "User added. " + "(" + msg[2] + ")");
                    }
                } else if (msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")) {
                    if (channelInfo.isModerator(msg[2])) {
                        channelInfo.removeModerator(msg[2]);
                        send(channel, "User removed. " + "(" + msg[2] + ")");
                    } else {
                        send(channel, "User does not exist. " + "(" + msg[2] + ")");
                    }
                }
            } else if (msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner) {
                String tempList = "Moderators: ";
                for (String s : channelInfo.getModerators()) {
                    tempList += s + ", ";
                }
                send(channel, tempList);
            }
            return;
        }

        // !owner - Owner
        if (msg[0].equalsIgnoreCase(prefix + "owner") && isOwner) {
            log("Matched command !owner");
            if (msg.length < 2) {
                send(channel, "Syntax: \"!owner add/delete [name]\", \"!owner list\"");
            }
            if (msg.length > 2) {
                if (msg[1].equalsIgnoreCase("add")) {
                    if (channelInfo.isOwner(msg[2])) {
                        send(channel, "User already exists. " + "(" + msg[2] + ")");
                    } else {
                        channelInfo.addOwner(msg[2]);
                        send(channel, "User added. " + "(" + msg[2] + ")");
                    }
                } else if (msg[1].equalsIgnoreCase("delete") || msg[1].equalsIgnoreCase("remove")) {
                    if (channelInfo.isOwner(msg[2])) {
                        channelInfo.removeOwner(msg[2]);
                        send(channel, "User removed. " + "(" + msg[2] + ")");
                    } else {
                        send(channel, "User does not exist. " + "(" + msg[2] + ")");
                    }
                }
            } else if (msg.length > 1 && msg[1].equalsIgnoreCase("list") && isOwner) {
                String tempList = "Owners: ";
                for (String s : channelInfo.getOwners()) {
                    tempList += s + ", ";
                }
                send(channel, tempList);
            }
            return;
        }

        // !set - Owner
        if (msg[0].equalsIgnoreCase(prefix + "set") && isOwner) {
            log("Matched command !set");
            if (msg.length == 1) {
                send(channel, "Syntax: \"!set [option] [value]\". Options: topic, filters, throw, signedkicks, joinsparts, lastfm, steam, mode, chatlogging, maxlength");
            } else if (msg[1].equalsIgnoreCase("topic")) {
                if (msg[2].equalsIgnoreCase("on")) {
                    channelInfo.setTopicFeature(true);
                    send(channel, "Feature: Topic is on");
                } else if (msg[2].equalsIgnoreCase("off")) {
                    channelInfo.setTopicFeature(false);
                    send(channel, "Feature: Topic is off");
                }

            } else if (msg[1].equalsIgnoreCase("throw")) {
                if (msg[2].equalsIgnoreCase("on")) {
                    channelInfo.setThrow(true);
                    send(channel, "Feature: !throw is on");
                } else if (msg[2].equalsIgnoreCase("off")) {
                    channelInfo.setThrow(false);
                    send(channel, "Feature: !throw is off");
                }
            } else if (msg[1].equalsIgnoreCase("lastfm")) {
                if (msg[2].equalsIgnoreCase("off")) {
                    channelInfo.setLastfm("");
                    send(channel, "Feature: Lastfm is off.");
                } else {
                    channelInfo.setLastfm(msg[2]);
                    send(channel, "Feature: Lastfm user set to " + msg[2]);
                }
            } else if (msg[1].equalsIgnoreCase("steam")) {
                if (msg[2].equalsIgnoreCase("off")) {
                    channelInfo.setSteam("");
                    send(channel, "Feature: Steam is off.");
                } else {
                    channelInfo.setSteam(msg[2]);
                    send(channel, "Feature: Steam id set to " + msg[2]);
                }
            } else if (msg[1].equalsIgnoreCase("mode")) {
                if (msg.length < 3) {
                    send(channel, "Mode set to " + channelInfo.getMode() + "");
                } else if ((msg[2].equalsIgnoreCase("0") || msg[2].equalsIgnoreCase("owner")) && isOwner) {
                    channelInfo.setMode(0);
                    send(channel, "Mode set to admin/owner only.");
                } else if (msg[2].equalsIgnoreCase("1") || msg[2].equalsIgnoreCase("mod")) {
                    channelInfo.setMode(1);
                    send(channel, "Mode set to admin/owner/mod only.");
                } else if (msg[2].equalsIgnoreCase("2") || msg[2].equalsIgnoreCase("everyone")) {
                    channelInfo.setMode(2);
                    send(channel, "Mode set to everyone.");
                } else if (msg[2].equalsIgnoreCase("-1") || msg[2].equalsIgnoreCase("admin")) {
                    channelInfo.setMode(-1);
                    send(channel, "Special moderation mode activated.");
                }
            } else if (msg[1].equalsIgnoreCase("commerciallength")) {
                if (msg.length > 2) {
                    int cLength = Integer.parseInt(msg[2]);
                    if (cLength == 30 || cLength == 60 || cLength == 90 || cLength == 120 || cLength == 150 || cLength == 180) {
                        channelInfo.setCommercialLength(cLength);
                        send(channel, "Commercial length is set to " + channelInfo.getCommercialLength() + " seconds.");
                    }
                } else {
                    send(channel, "Commercial length is " + channelInfo.getCommercialLength() + " seconds.");
                }
            } else if (msg[1].equalsIgnoreCase("tweet")) {
                if (msg.length < 3) {
                    send(channel, "ClickToTweet format: " + channelInfo.getClickToTweetFormat());
                } else {
                    String format = fuseArray(msg, 2);
                    if (!format.contains("(_TWEET_URL_)")) {
                        channelInfo.setClickToTweetFormat(format);
                        send(channel, "ClickToTweet format: " + channelInfo.getClickToTweetFormat());
                    } else {
                        send(channel, "_TWEET_URL_ is not allowed.");
                    }

                }
            } else if (msg[1].equalsIgnoreCase("prefix")) {
                if (msg.length > 2) {
                    if (msg[2].length() > 1) {
                        send(channel, "Prefix may only be 1 character.");
                    } else {
                        channelInfo.setPrefix(msg[2]);
                        send(channel, "Command prefix is " + channelInfo.getPrefix());
                    }
                } else {
                    send(channel, "Command prefix is " + channelInfo.getPrefix());
                }
            } else if (msg[1].equalsIgnoreCase("emoteset") && msg.length > 2) {
                channelInfo.setEmoteSet(msg[2]);
                send(channel, "Emote set ID set to " + channelInfo.getEmoteSet());
            } else if (msg[1].equalsIgnoreCase("subscriberregulars")) {
                if (msg[2].equalsIgnoreCase("on")) {
                    channelInfo.setSubscriberRegulars(true);
                    send(channel, "Subscribers will now be treated as regulars.");
                } else if (msg[2].equalsIgnoreCase("off")) {
                    channelInfo.setSubscriberRegulars(false);
                    send(channel, "Subscribers will no longer be treated as regulars.");
                }
            } else if (msg[1].equalsIgnoreCase("subscriberalerts")) {
                if (msg.length < 3) {
                    send(channel, "Subscriber alerts: " + channelInfo.config.getBoolean("subscriberAlert"));
                    send(channel, "Subscriber alert message: " + channelInfo.config.getString("subMessage"));
                } else if (msg[2].equalsIgnoreCase("on")) {
                    channelInfo.config.setBoolean("subscriberAlert", true);
                    send(channel, "Subscriber alerts enabled.");
                } else if (msg[2].equalsIgnoreCase("off")) {
                    channelInfo.config.setBoolean("subscriberAlert", false);
                    send(channel, "Subscriber alerts disabled.");
                } else if (msg[2].equalsIgnoreCase("message") && msg.length > 3) {
                    channelInfo.config.setString("subMessage", fuseArray(msg, 3));
                    send(channel, "Subscriber alert message set to: " + channelInfo.config.getString("subMessage"));
                }
            }
            return;
        }


        //!modchan - Mod
        if (msg[0].equalsIgnoreCase(prefix + "modchan") && isOwner) {
            log("Matched command !modchan");
            if (channelInfo.getMode() == 2) {
                channelInfo.setMode(1);
                send(channel, "Mode set to admin/owner/mod only.");
            } else if (channelInfo.getMode() == 1) {
                channelInfo.setMode(2);
                send(channel, "Mode set to everyone.");
            } else {
                send(channel, "Mode can only be changed by bot admin.");
            }
            return;
        }


        //!join
        if (msg[0].equalsIgnoreCase(prefix + "join")) {
            log("Matched command !join");

            if (!BotManager.getInstance().publicJoin) {
                send(channel, "Public joining is disabled at this time.");
                return;
            }

            if (JSONUtil.krakenChannelExist(sender)) {
                send(channel, "Joining channel #" + sender + ".");
                boolean joinStatus = BotManager.getInstance().addChannel("#" + sender, 2);
                if (joinStatus) {
                    send(channel, "Channel #" + sender + " joined.");
                } else {
                    send(channel, "Already in channel #" + sender + ".");
                }
            } else {
                send(channel, "Unable to join " + sender + ". This could be because your channel is on Justin.tv and not Twitch. If you are sure your channel is on Twitch, try again later.");
            }
            return;
        }

        if (msg[0].equalsIgnoreCase(prefix + "rejoin")) {
            log("Matched command !rejoin");
            if (msg.length > 1 && isAdmin) {
                if (msg[1].contains("#")) {
                    send(channel, "Rejoining channel " + msg[1] + ".");
                    boolean joinStatus = BotManager.getInstance().rejoinChannel(msg[1]);
                    if (joinStatus) {
                        send(channel, "Channel " + msg[1] + " rejoined.");
                    } else {
                        send(channel, "Bot is not assigned to channel " + msg[1] + ".");
                    }

                } else {
                    send(channel, "Invalid channel format. Must be in format #channelname.");
                }
            } else {
                send(channel, "Rejoining channel #" + sender + ".");
                boolean joinStatus = BotManager.getInstance().rejoinChannel("#" + sender);
                if (joinStatus) {
                    send(channel, "Channel #" + sender + " rejoined.");
                } else {
                    send(channel, "Bot is not assigned to channel #" + sender + ".");
                }
            }
            return;
        }

        // ********************************************************************************
        // **************************** Administration Commands ***************************
        // ********************************************************************************

        if (msg[0].equalsIgnoreCase(prefix + "admin") && isAdmin && msg.length > 1) {
            if (msg[1].equalsIgnoreCase("channels")) {
                send(channel, "Currently in " + BotManager.getInstance().channelList.size() + " channels.");
                String channelString = "";
                for (Map.Entry<String, Channel> entry : BotManager.getInstance().channelList.entrySet()) {
                    channelString += entry.getValue().getChannel() + ", ";
                }
                send(channel, "Channels: " + channelString);
                return;
            } else if (msg[1].equalsIgnoreCase("join") && msg.length > 2) {
                if (msg[2].contains("#")) {
                    String toJoin = msg[2];
                    int mode = 0;
                    if (msg.length > 3 && Main.isInteger(msg[3]))
                        mode = Integer.parseInt(msg[3]);
                    send(channel, "Joining channel " + toJoin + " with mode (" + mode + ").");
                    boolean joinStatus = BotManager.getInstance().addChannel(toJoin, mode);
                    if (joinStatus) {
                        send(channel, "Channel " + toJoin + " joined.");
                    } else {
                        send(channel, "Already in channel " + toJoin + ".");
                    }

                } else {
                    send(channel, "Invalid channel format. Must be in format #channelname.");
                }
                return;
            } else if (msg[1].equalsIgnoreCase("part") && msg.length > 2) {
                if (msg[2].startsWith("#")) {
                    String toPart = msg[2];
                    send(channel, "Channel " + toPart + " parting...");
                    BotManager.getInstance().removeChannel(toPart);
                    send(channel, "Channel " + toPart + " parted.");
                } else {
                    send(channel, "Invalid channel format. Must be in format #channelname.");
                }
                return;
            } else if (msg[1].equalsIgnoreCase("reconnect")) {
                send(channel, "Reconnecting all servers.");
                BotManager.getInstance().reconnectAllBotsSoft();
                return;
            } else if (msg[1].equalsIgnoreCase("reload") && msg.length > 2) {
                if (msg[2].startsWith("#")) {
                    String toReload = msg[2];
                    send(channel, "Reloading channel " + toReload);
                    BotManager.getInstance().reloadChannel(toReload);
                    send(channel, "Channel " + toReload + " reloaded.");
                } else {
                    send(channel, "Invalid channel format. Must be in format #channelname.");
                }
                return;
            } else if (msg[1].equalsIgnoreCase("clone") && msg.length > 3) {
                if (msg[2].startsWith("#") && msg[3].startsWith("#")) {
                    String src = msg[2];
                    String dest = msg[3];

                    try {
                        BotManager.getInstance().cloneConfig(src, dest);
                    } catch (IOException ioE) {
                        ioE.printStackTrace();
                        send(channel, "An IO exception occurred running this command and thus it was not successful.");
                    }

                    send(channel, "Channel " + src + " has been cloned to " + dest);
                    BotManager.getInstance().reloadChannel(dest);
                    send(channel, "Attempting to reload " + dest);
                } else {
                    send(channel, "Invalid channel format. Must be in format #channelname.");
                }
                return;
            } else if (msg[1].equalsIgnoreCase("color") && msg.length > 2) {
                sendCommand(channel, ".color " + msg[2]);
                send(channel, "Color set to " + msg[2]);
                return;
            } else if (msg[1].equalsIgnoreCase("loadfilter")) {
                BotManager.getInstance().loadGlobalBannedWords();
                BotManager.getInstance().loadBanPhraseList();
                send(channel, "Global banned filter reloaded.");
                return;
            } else if (msg[1].equalsIgnoreCase("spam")) {
                if (msg.length > 3 && Main.isInteger(msg[2])) {
                    String toSpam = fuseArray(msg, 3);
                    for (int i = 0; i < Integer.parseInt(msg[2]); i++)
                        send(channel, toSpam + " " + (i + 1));
                    return;
                }
            }
        }

        // ********************************************************************************
        // *********************************** Auto Reply *********************************
        // ********************************************************************************
        for (int i = 0; i < channelInfo.autoReplyTrigger.size(); i++) {
            Matcher m = channelInfo.autoReplyTrigger.get(i).matcher(message);
            if (m.matches()) {

                if (!channelInfo.onCooldown(channelInfo.autoReplyTrigger.get(i).toString()))
                    send(channel, sender, channelInfo.autoReplyResponse.get(i));
            }
        }
    }

    protected void onAdministrativeMessage(String message, Channel channelinfo) {
        //System.out.println("ADM MSG: " + message);

        String[] msg = message.trim().split(" ");

        if (msg.length > 0) {
            if (msg[0].equalsIgnoreCase("SPECIALUSER")) {
                String user = msg[1];
                String tag = msg[2];

                if (tag.equalsIgnoreCase("admin"))
                    BotManager.getInstance().addTagAdmin(user);
                if (tag.equalsIgnoreCase("staff"))
                    BotManager.getInstance().addTagStaff(user);
                if (tag.equalsIgnoreCase("global_mod"))
                    BotManager.getInstance().addTagGlobalMod(user);
                if (tag.equalsIgnoreCase("subscriber") && channelinfo != null)
                    channelinfo.addSubscriber(user);
            } else if (msg[0].equalsIgnoreCase("USERCOLOR")) {
                String user = msg[1];
                String color = msg[2];
            }
        }
    }

    protected void onNewSubscriber(Channel channel, String username) {
        LOGGER_D.debug("New subscriber in " + channel.getTwitchName() + " " + username);
        if (channel.config.getBoolean("subscriberAlert")) {
            String msgFormat = channel.config.getString("subMessage");
            send(channel.getChannel(), null, msgFormat, new String[]{username});
        }
    }

    private Map<String, String> mapTags(String rawTags) {
        Map<String, String> tags = new HashMap<String, String>();

        StringTokenizer tokenizer = new StringTokenizer(rawTags);

        while (tokenizer.hasMoreTokens()) {
            String tag = tokenizer.nextToken(";");
            if (tag.contains("=")) {
                String[] parts = tag.split("=");
                tags.put(parts[0], (parts.length == 2 ? parts[1] : null));
            } else {
                tags.put(tag, null);
            }
        }

        return tags;
    }

    private void setRandomNickColor() {
        if (!BotManager.getInstance().randomNickColor)
            return;

        countToNewColor--;

        if (countToNewColor == 0) {
            countToNewColor = BotManager.getInstance().randomNickColorDiff;
            Color newColor = new Color(Color.HSBtoRGB(random.nextFloat(), 1.0f, 0.65f));
            String hexColor = String.format("#%06X", (0xFFFFFF & newColor.getRGB()));
            sendCommand("#" + getNick(), ".color " + hexColor);
        }

    }

    @Override
    public void onDisconnect() {
        lastPing = -1;
        try {
            LOGGER_D.debug("Internal reconnection: " + this.getServer());
            String[] channels = this.getChannels();
            this.reconnect();
            for (int i = 0; i < channels.length; i++) {
                this.joinChannel(channels[i]);
            }
        } catch (NickAlreadyInUseException e) {
            logMain("[ERROR] Nickname already in use - " + this.getNick() + " " + this.getServer());
        } catch (IOException e) {
            logMain("[ERROR] Unable to connect to server - " + this.getNick() + " " + this.getServer());
        } catch (IrcException e) {
            logMain("[ERROR] Error connecting to server - " + this.getNick() + " " + this.getServer());
        }

    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostname) {
        Channel channelInfo = getChannelObject(channel);

        if (channelInfo == null)
            return;

        if (this.getNick().equalsIgnoreCase(sender)) {
            synchronized (joinedChannels) {
                //log("Got self join for " + channel);
                if (!joinedChannels.contains(channel)) {
                    LOGGER_D.debug("Got self join on " + channel);
                    LOGGER_D.debug("Got JOIN for " + joinedChannels.size() + " " + this.getChannels().length);
                    joinedChannels.add(channel);
                }
            }
        }
    }

    public void send(String target, String sender, String message) {
        send(target, sender, message, null);
    }

    public void send(String target, String message) {
        send(target, null, message, null);
    }

    public void send(String target, String sender, String message, String[] args) {
        //    Channel channelInfo = getChannelObject(target);

        if (!BotManager.getInstance().verboseLogging)
            logMain("SEND: " + target + " " + getNick() + " : " + message);

        message = MessageReplaceParser.parseMessage(target, sender, message, args);
        boolean useBullet = true;

        if (message.startsWith("/me "))
            useBullet = false;

        //Split if message > X characters
        List<String> chunks = Main.splitEqually(message, 500);
        int c = 1;
        for (String chunk : chunks) {
            sendMessage(target, (useBullet ? getBullet() + " " : "") + (chunks.size() > 1 ? "[" + c + "] " : "") + chunk);
            c++;
            useBullet = true;
        }

        setRandomNickColor();
    }

    public void sendCommand(String target, String message) {
        sendMessage(target, message);
    }

    @Override
    public void onServerPing(String response) {
        super.onServerPing(response);
        lastPing = (int) (System.currentTimeMillis() / 1000);
    }

    public void log(String line) {
        if (this.getVerbose()) {
            logMain(System.currentTimeMillis() + " " + line);
        }
    }

    public void logMain(String line) {
        BotManager.getInstance().log(line);
    }

    public void startJoinCheck() {

        joinCheck = new Timer();

        int delay = 120000;

        joinCheck.scheduleAtFixedRate(new TimerTask() {
            public void run() {
                LOGGER_D.debug("Running joined list comparison");
                //String[] currentChanList = ReceiverBot.this.getChannels();
                for (Map.Entry<String, Channel> entry : BotManager.getInstance().channelList.entrySet()) {
                    boolean inList = false;
                    if (joinedChannels.contains(entry.getValue().getChannel()))
                        inList = true;

//                    for (String c : currentChanList) {
//                        if (entry.getValue().getChannel().equals(c))
//                            inList = true;
//                    }

                    if (!inList) {
                        LOGGER_D.debug(entry.getValue().getChannel() + " is not in the joined list.");
                        ReceiverBot.this.joinChannel(entry.getValue().getChannel());
                    }

                }
            }
        }, delay, delay);

    }

    private int getSymbolsNumber(String s) {
        int symbols = 0;
        for (Pattern p : symbolsPatterns) {
            Matcher m = p.matcher(s);
            while (m.find())
                symbols += 1;
        }
        return symbols;
    }

    private int getCapsNumber(String s) {
        int caps = 0;
        for (int i = 0; i < s.length(); i++) {
            if (Character.isUpperCase(s.charAt(i))) {
                caps++;
            }
        }

        return caps;
    }

    private boolean containsLink(String message, Channel ch) {
        String[] splitMessage = message.toLowerCase().split(" ");
        for (String m : splitMessage) {
            for (Pattern pattern : linkPatterns) {
                //System.out.println("Checking " + m + " against " + pattern.pattern());
                Matcher match = pattern.matcher(m);
                if (match.matches()) {
                    log("Link match on " + pattern.pattern());
                    if (ch.checkPermittedDomain(m))
                        return false;
                    else
                        return true;
                }
            }
        }
        return false;
    }

    private int countEmotes(String message) {
        String str = message;
        int count = 0;
        for (String findStr : BotManager.getInstance().emoteSet) {
            int lastIndex = 0;
            while (lastIndex != -1) {

                lastIndex = str.indexOf(findStr, lastIndex);

                if (lastIndex != -1) {
                    count++;
                    lastIndex += findStr.length();
                }
            }
        }
        return count;
    }

    private boolean checkSingleEmote(String message, String emote_tags) {
        if (emote_tags == null)
            return false;

        String[] emotes = emote_tags.split("/");

        if (emotes.length > 1)
            return false;

        for (String e : emotes) {
            if (e.contains(","))
                continue;

            String id = e.substring(0, e.indexOf(":"));
            int first_char = Integer.parseInt(e.substring(e.indexOf(":") + 1, e.indexOf("-")));
            int last_char = Integer.parseInt(e.substring(e.indexOf("-") + 1));

            if (first_char == 0 && last_char == message.length() - 1)
                return true;
        }


        return false;
    }

    public boolean isGlobalBannedWord(String message) {
        for (Pattern reg : BotManager.getInstance().globalBannedWords) {
            Matcher match = reg.matcher(message.toLowerCase());
            if (match.matches()) {
                log("Global banned word matched: " + reg.toString());
                return true;
            }
        }
        return false;
    }

    private String getTimeoutText(int count, Channel channel) {
        if (channel.getEnableWarnings()) {
            if (count > 1) {
                return "temp ban";
            } else {
                return "warning";
            }
        } else {
            return "temp ban";
        }
    }

    private int getTODuration(int count, Channel channel) {
        if (channel.getEnableWarnings()) {
            if (count > 1) {
                return channel.getTimeoutDuration();
            } else {
                return 10;
            }
        } else {
            return channel.getTimeoutDuration();
        }
    }

    private void secondaryTO(final String channel, final String name, final int duration, FilterType type, String message) {


        String line = "FILTER: Issuing a timeout on " + name + " in " + channel + " for " + type.toString() + " (" + duration + ")";
        logMain(line);
        line = "FILTER: Affected Message: " + message;
        logMain(line);
        ReceiverBot.this.sendCommand(channel, ".timeout " + name + " " + duration);

        int iterations = BotManager.getInstance().multipleTimeout;

        for (int i = 0; i < iterations; i++) {
            Timer timer = new Timer();
            int delay = 500 * i;
            timer.schedule(new TimerTask() {
                public void run() {
                    ReceiverBot.this.sendCommand(channel, ".timeout " + name + " " + duration);
                }
            }, delay);
        }


        //Send to subscribers
        Channel channelInfo = getChannelObject(channel);
        if (BotManager.getInstance().wsEnabled)
            BotManager.getInstance().ws.sendToSubscribers(line, channelInfo);

    }

    private void secondaryBan(final String channel, final String name, FilterType type) {
        String line = "Issuing a ban on " + name + " in " + channel + " for " + type.toString();
        logMain(line);

        int iterations = BotManager.getInstance().multipleTimeout;
        for (int i = 0; i < iterations; i++) {
            Timer timer = new Timer();
            int delay = 1000 * i;
            timer.schedule(new TimerTask() {
                public void run() {
                    ReceiverBot.this.sendCommand(channel, ".ban " + name);
                }
            }, delay);
        }


        //Send to subscribers
        Channel channelInfo = getChannelObject(channel);
        if (BotManager.getInstance().wsEnabled)
            BotManager.getInstance().ws.sendToSubscribers(line, channelInfo);

    }

    private void startGaTimer(int seconds, Channel channelInfo) {
        if (channelInfo.getGiveaway() != null) {
            channelInfo.getGiveaway().setTimer(new Timer());
            int delay = seconds * 1000;

            if (!channelInfo.getGiveaway().getStatus()) {
                channelInfo.getGiveaway().setStatus(true);
                send(channelInfo.getChannel(), "> Giveaway started. (" + seconds + " seconds)");
            }

            channelInfo.getGiveaway().getTimer().schedule(new giveawayTimer(channelInfo), delay);
        }
    }



    public String getTimeStreaming(String uptime) {
        uptime = uptime.replace("Z", "UTC");
        DateFormat format = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss");
        format.setTimeZone(java.util.TimeZone.getTimeZone("UTC"));
        try {
            Date then = format.parse(uptime);
            return "Streaming for " + this.getTimeTilNow(then) + " since " + uptime + ".";
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return "An error occurred or stream is offline";
    }

    public boolean checkStalePing() {
        if (lastPing == -1)
            return false;

        int difference = ((int) (System.currentTimeMillis() / 1000)) - lastPing;

        if (difference > BotManager.getInstance().pingInterval) {
            log("Ping is stale. Last ping= " + lastPing + " Difference= " + difference);
            lastPing = -1;
            return true;
        }

        return false;
    }

    private String fuseArray(String[] array, int start) {
        String fused = "";
        for (int c = start; c < array.length; c++)
            fused += array[c] + " ";

        return fused.trim();

    }

    public String getTimeTilNow(Date date) {
        long difference = (long) (System.currentTimeMillis() / 1000) - (date.getTime() / 1000);
        String returnString = "";

        if (difference >= 86400) {
            int days = (int) (difference / 86400);
            returnString += days + "d ";
            difference -= days * 86400;
        }
        if (difference >= 3600) {
            int hours = (int) (difference / 3600);
            returnString += hours + "h ";
            difference -= hours * 3600;
        }

        int seconds = (int) (difference / 60);
        returnString += seconds + "m";
        difference -= seconds * 60;


        return returnString;
    }

    public void logGlobalBan(String channel, String sender, String message) {
        String line = sender + "," + channel + ",\"" + message + "\"\n";

        //System.out.print(line);
        try {
            BufferedWriter out = new BufferedWriter(new OutputStreamWriter(new FileOutputStream("globalbans.csv", true), "UTF-8"));
            out.write(line);
            out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public char getBullet() {
        if (bulletPos == bullet.length)
            bulletPos = 0;

        char rt = bullet[bulletPos];
        bulletPos++;

        return rt;

    }

    private class giveawayTimer extends TimerTask {
        private Channel channelInfo;

        public giveawayTimer(Channel channelInfo2) {
            super();
            channelInfo = channelInfo2;
        }

        public void run() {
            if (channelInfo.getGiveaway() != null) {
                if (channelInfo.getGiveaway().getStatus()) {
                    channelInfo.getGiveaway().setStatus(false);
                    ReceiverBot.this.send(channelInfo.getChannel(), "> Giveaway over.");
                }
            }
        }
    }
}
