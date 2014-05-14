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

import org.java_websocket.WebSocket;
import org.json.simple.JSONObject;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class Channel {
    public PropertiesFile config;

    private String channel;
    private String twitchname;
    boolean staticChannel;
    private HashMap<String, String> commands = new HashMap<String, String>();
    private HashMap<String, Integer> commandsRestrictions = new HashMap<String, Integer>();
    HashMap<String, RepeatCommand> commandsRepeat = new HashMap<String, RepeatCommand>();
    HashMap<String, ScheduledCommand> commandsSchedule = new HashMap<String, ScheduledCommand>();
    List<Pattern> autoReplyTrigger = new ArrayList<Pattern>();
    List<String> autoReplyResponse = new ArrayList<String>();
    private boolean filterCaps;
    private int filterCapsPercent;
    private int filterCapsMinCharacters;
    private int filterCapsMinCapitals;
    private boolean filterLinks;
    private boolean filterOffensive;
    private boolean filterEmotes;
    private boolean filterSymbols;
    private int filterSymbolsPercent;
    private int filterSymbolsMin;
    private int filterEmotesMax;
    private boolean filterEmotesSingle;
    private int filterMaxLength;
    private String topic;
    private int topicTime;
    private Set<String> regulars = new HashSet<String>();
    private Set<String> subscribers = new HashSet<String>();
    private Set<String> moderators = new HashSet<String>();
    Set<String> tagModerators = new HashSet<String>();
    private Set<String> owners = new HashSet<String>();
    private Set<String> permittedUsers = new HashSet<String>();
    private ArrayList<String> permittedDomains = new ArrayList<String>();
    public boolean useTopic = true;
    public boolean useFilters = true;
    private Poll currentPoll;
    private Giveaway currentGiveaway;
    private boolean enableThrow;
    private boolean signKicks;
    private boolean announceJoinParts;
    private String lastfm;
    private String steamID;
    private int mode; //0: Admin/owner only; 1: Mod Only; 2: Everyone; -1 Special mode to admins to use for channel moderation
    private int bulletInt;
    Raffle raffle;
    public boolean logChat;
    public long messageCount;
    public int commercialLength;
    String clickToTweetFormat;
    private boolean filterColors;
    private boolean filterMe;
    private Set<String> offensiveWords = new HashSet<String>();
    private List<Pattern> offensiveWordsRegex = new LinkedList<Pattern>();
    Map<String, EnumMap<FilterType, Integer>> warningCount;
    Map<String, Long> warningTime;
    private int timeoutDuration;
    private boolean enableWarnings;
    Map<String, Long> commandCooldown;
    Set<WebSocket> wsSubscribers = new HashSet<WebSocket>();
    String prefix;
    String emoteSet;
    boolean subscriberRegulars;
    boolean skipNextCommercial = false;

    private Map<String, Object> defaults = new HashMap<String, Object>();

    public Channel(String name) {
        channel = name;
        config = new PropertiesFile(name + ".properties");
        loadProperties(name);
        warningCount = new HashMap<String, EnumMap<FilterType, Integer>>();
        warningTime = new HashMap<String, Long>();
        commandCooldown = new HashMap<String, Long>();

        twitchname = channel.substring(1);
    }

    public Channel(String name, int mode) {
        this(name);
        setMode(mode);
    }

    public String getChannel() {
        return channel;
    }

    public String getTwitchName() {
        return twitchname;
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String prefix) {
        this.prefix = prefix.charAt(0) + "";

        config.setString("commandPrefix", this.prefix);
    }

    public String getEmoteSet() {
        return emoteSet;
    }

    public void setEmoteSet(String emoteSet) {
        this.emoteSet = emoteSet;

        config.setString("emoteSet", emoteSet);
    }

    public boolean getSubscriberRegulars() {
        return subscriberRegulars;
    }

    public void setSubscriberRegulars(boolean subscriberRegulars) {
        subscribers.clear();

        this.subscriberRegulars = subscriberRegulars;
        config.setBoolean("subscriberRegulars", subscriberRegulars);
    }

    //##############################################################

    public String getCommand(String key) {
        key = key.toLowerCase();

        if (commands.containsKey(key)) {
            return commands.get(key);
        } else {
            return null;
        }
    }

    public void setCommand(String key, String command) {
        key = key.toLowerCase().replaceAll("[^a-zA-Z0-9]", "");
        System.out.println("Key: " + key);
        command = command.replaceAll(",,", "");

        if (key.length() < 1)
            return;

        if (commands.containsKey(key)) {
            commands.remove(key);
            commands.put(key, command);
        } else {
            commands.put(key, command);
        }

        String commandsKey = "";
        String commandsValue = "";

        Iterator itr = commands.entrySet().iterator();

        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            commandsKey += pairs.getKey() + ",";
            commandsValue += pairs.getValue() + ",,";
        }

        config.setString("commandsKey", commandsKey);
        config.setString("commandsValue", commandsValue);

    }

    public void removeCommand(String key) {
        if (commands.containsKey(key)) {
            commands.remove(key);
            commandsRestrictions.remove(key);

            String commandsKey = "";
            String commandsValue = "";

            Iterator itr = commands.entrySet().iterator();

            while (itr.hasNext()) {
                Map.Entry pairs = (Map.Entry) itr.next();
                commandsKey += pairs.getKey() + ",";
                commandsValue += pairs.getValue() + ",,";
            }

            config.setString("commandsKey", commandsKey);
            config.setString("commandsValue", commandsValue);

            saveCommandRestrictions();
        }

    }

    public boolean setCommandsRestriction(String command, int level) {
        command = command.toLowerCase();

        if (!commands.containsKey(command))
            return false;

        commandsRestrictions.put(command, level);

        saveCommandRestrictions();

        return true;
    }

    public boolean checkCommandRestriction(String command, int level) {
        System.out.println("Checking command: " + command + " User level: " + level);
        if (!commandsRestrictions.containsKey(command))
            return true;

        if (level >= commandsRestrictions.get(command))
            return true;

        return false;
    }

    public void saveCommandRestrictions() {
        String commandRestrictionsString = "";

        Iterator itr = commandsRestrictions.entrySet().iterator();

        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            commandRestrictionsString += pairs.getKey() + "|" + pairs.getValue() + ",";
        }

        config.setString("commandRestrictions", commandRestrictionsString);
    }

    public void setRepeatCommand(String key, int delay, int diff) {
        if (commandsRepeat.containsKey(key)) {
            commandsRepeat.get(key).timer.cancel();
            commandsRepeat.remove(key);
            RepeatCommand rc = new RepeatCommand(channel, key, delay, diff, true);
            commandsRepeat.put(key, rc);
        } else {
            RepeatCommand rc = new RepeatCommand(channel, key, delay, diff, true);
            commandsRepeat.put(key, rc);
        }

        writeRepeatCommand();
    }

    public void removeRepeatCommand(String key) {
        if (commandsRepeat.containsKey(key)) {
            commandsRepeat.get(key).timer.cancel();
            commandsRepeat.remove(key);

            writeRepeatCommand();
        }
    }

    public void setRepeatCommandStatus(String key, boolean status) {
        if (commandsRepeat.containsKey(key)) {
            commandsRepeat.get(key).setStatus(status);
            writeRepeatCommand();
        }
    }

    private void writeRepeatCommand() {
        String commandsRepeatKey = "";
        String commandsRepeatDelay = "";
        String commandsRepeatDiff = "";
        String commandsRepeatActive = "";

        Iterator itr = commandsRepeat.entrySet().iterator();

        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            commandsRepeatKey += pairs.getKey() + ",";
            commandsRepeatDelay += ((RepeatCommand) pairs.getValue()).delay + ",";
            commandsRepeatDiff += ((RepeatCommand) pairs.getValue()).messageDifference + ",";
            commandsRepeatActive += ((RepeatCommand) pairs.getValue()).active + ",";
        }

        config.setString("commandsRepeatKey", commandsRepeatKey);
        config.setString("commandsRepeatDelay", commandsRepeatDelay);
        config.setString("commandsRepeatDiff", commandsRepeatDiff);
        config.setString("commandsRepeatActive", commandsRepeatActive);
    }

    public void setScheduledCommand(String key, String pattern, int diff) {
        if (commandsSchedule.containsKey(key)) {
            commandsSchedule.get(key).s.stop();
            commandsSchedule.remove(key);
            ScheduledCommand rc = new ScheduledCommand(channel, key, pattern, diff, true);
            commandsSchedule.put(key, rc);
        } else {
            ScheduledCommand rc = new ScheduledCommand(channel, key, pattern, diff, true);
            commandsSchedule.put(key, rc);
        }

        writeScheduledCommand();


    }

    public void removeScheduledCommand(String key) {
        if (commandsSchedule.containsKey(key)) {
            commandsSchedule.get(key).s.stop();
            commandsSchedule.remove(key);

            writeScheduledCommand();
        }
    }

    public void setScheduledCommandStatus(String key, boolean status) {
        if (commandsSchedule.containsKey(key)) {
            commandsSchedule.get(key).setStatus(status);
            writeScheduledCommand();
        }
    }

    private void writeScheduledCommand() {
        String commandsScheduleKey = "";
        String commandsSchedulePattern = "";
        String commandsScheduleDiff = "";
        String commandsScheduleActive = "";

        Iterator itr = commandsSchedule.entrySet().iterator();

        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            commandsScheduleKey += pairs.getKey() + ",,";
            commandsSchedulePattern += ((ScheduledCommand) pairs.getValue()).pattern + ",,";
            commandsScheduleDiff += ((ScheduledCommand) pairs.getValue()).messageDifference + ",,";
            commandsScheduleActive += ((ScheduledCommand) pairs.getValue()).active + ",,";

        }

        config.setString("commandsScheduleKey", commandsScheduleKey);
        config.setString("commandsSchedulePattern", commandsSchedulePattern);
        config.setString("commandsScheduleDiff", commandsScheduleDiff);
        config.setString("commandsScheduleActive", commandsScheduleActive);
    }

    public String getCommandList() {
        String commandKeys = "";

        Iterator itr = commands.entrySet().iterator();

        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            commandKeys += pairs.getKey() + ", ";
        }

        return commandKeys;

    }

    public void addAutoReply(String trigger, String response) {
        trigger = trigger.replaceAll(",,", "");
        response.replaceAll(",,", "");

        if (!trigger.startsWith("REGEX:")) {
            String[] parts = trigger.replaceFirst("^\\*", "").replaceFirst("\\*$", "").split("\\*");

            //Only apply leading & trailing any if an one was requested
            boolean trailingAny = trigger.endsWith("*");
            if (trigger.startsWith("*"))
                trigger = ".*";
            else
                trigger = "";

            for (int i = 0; i < parts.length; i++) {
                if (parts[i].length() < 1)
                    continue;

                trigger += Pattern.quote(parts[i]);
                if (i != parts.length - 1)
                    trigger += ".*";
            }

            if (trailingAny)
                trigger += ".*";

        } else {
            trigger = trigger.replaceAll("REGEX:", "");
        }

        System.out.println("Final: " + trigger);
        autoReplyTrigger.add(Pattern.compile(trigger, Pattern.CASE_INSENSITIVE));
        autoReplyResponse.add(response);

        saveAutoReply();
    }

    public boolean removeAutoReply(int pos) {
        pos = pos - 1;

        if (pos > autoReplyTrigger.size() - 1)
            return false;

        autoReplyTrigger.remove(pos);
        autoReplyResponse.remove(pos);

        saveAutoReply();

        return true;
    }

    private void saveAutoReply() {
        String triggerString = "";
        String responseString = "";

        for (int i = 0; i < autoReplyTrigger.size(); i++) {
            triggerString += autoReplyTrigger.get(i).toString() + ",,";
            responseString += autoReplyResponse.get(i).toString() + ",,";
        }

        config.setString("autoReplyTriggers", triggerString);
        config.setString("autoReplyResponse", responseString);
    }


    //#####################################################

    public String getTopic() {
        return topic;
    }

    public void setTopic(String s) {
        topic = s;
        config.setString("topic", topic);
        topicTime = (int) (System.currentTimeMillis() / 1000);
        config.setInt("topicTime", topicTime);
    }

    public void updateGame(String game) throws IOException {
        System.out.println(BotManager.putRemoteData("https://api.twitch.tv/kraken/channels/" + this.channel.substring(1), "{\"channel\": {\"game\": \"" + JSONObject.escape(game) + "\"}}"));
    }

    public void updateStatus(String status) throws IOException {
        System.out.println(BotManager.putRemoteData("https://api.twitch.tv/kraken/channels/" + this.channel.substring(1), "{\"channel\": {\"status\": \"" + JSONObject.escape(status) + "\"}}"));
    }

    public String getTopicTime() {
        int difference = (int) (System.currentTimeMillis() / 1000) - topicTime;
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

    //#####################################################

    public int getFilterSymbolsMin() {
        return filterSymbolsMin;
    }

    public int getFilterSymbolsPercent() {
        return filterSymbolsPercent;
    }

    public void setFilterSymbolsMin(int symbols) {
        filterSymbolsMin = symbols;
        config.setInt("filterSymbolsMin", filterSymbolsMin);
    }

    public void setFilterSymbolsPercent(int symbols) {
        filterSymbolsPercent = symbols;
        config.setInt("filterSymbolsPercent", filterSymbolsPercent);
    }

    public boolean getFilterCaps() {
        return filterCaps;
    }

    public int getfilterCapsPercent() {
        return filterCapsPercent;
    }

    public int getfilterCapsMinCharacters() {
        return filterCapsMinCharacters;
    }

    public int getfilterCapsMinCapitals() {
        return filterCapsMinCapitals;
    }

    public void setFilterCaps(boolean caps) {
        filterCaps = caps;
        config.setBoolean("filterCaps", filterCaps);
    }

    public void setfilterCapsPercent(int caps) {
        filterCapsPercent = caps;
        config.setInt("filterCapsPercent", filterCapsPercent);
    }

    public void setfilterCapsMinCharacters(int caps) {
        filterCapsMinCharacters = caps;
        config.setInt("filterCapsMinCharacters", filterCapsMinCharacters);
    }

    public void setfilterCapsMinCapitals(int caps) {
        filterCapsMinCapitals = caps;
        config.setInt("filterCapsMinCapitals", filterCapsMinCapitals);
    }

    public void setFilterLinks(boolean links) {
        filterLinks = links;
        config.setBoolean("filterLinks", links);
    }

    public boolean getFilterLinks() {
        return filterLinks;
    }

    public void setFilterOffensive(boolean option) {
        filterOffensive = option;
        config.setBoolean("filterOffensive", option);
    }

    public boolean getFilterOffensive() {
        return filterOffensive;
    }

    public void setFilterEmotes(boolean option) {
        filterEmotes = option;
        config.setBoolean("filterEmotes", option);
    }

    public boolean getFilterEmotes() {
        return filterEmotes;
    }

    public void setFilterSymbols(boolean option) {
        filterSymbols = option;
        config.setBoolean("filterSymbols", option);
    }

    public boolean getFilterSymbols() {
        return filterSymbols;
    }

    public int getFilterMax() {
        return filterMaxLength;
    }

    public void setFilterMax(int option) {
        filterMaxLength = option;
        config.setInt("filterMaxLength", option);
    }

    public void setFilterEmotesMax(int option) {
        filterEmotesMax = option;
        config.setInt("filterEmotesMax", option);
    }

    public int getFilterEmotesMax() {
        return filterEmotesMax;
    }

    public boolean getFilterEmotesSingle() {
        return filterEmotesSingle;
    }

    public void setFilterEmotesSingle(boolean filterEmotesSingle) {
        this.filterEmotesSingle = filterEmotesSingle;

        config.setBoolean("filterEmotesSingle", filterEmotesSingle);
    }

    public void setAnnounceJoinParts(boolean bol) {
        announceJoinParts = bol;
        config.setBoolean("announceJoinParts", bol);
    }

    public boolean getAnnounceJoinParts() {
        return announceJoinParts;
    }

    public void setFilterColor(boolean option) {
        filterColors = option;
        config.setBoolean("filterColors", option);
    }

    public boolean getFilterColor() {
        return filterColors;
    }

    public void setFilterMe(boolean option) {
        filterMe = option;
        config.setBoolean("filterMe", option);
    }

    public boolean getFilterMe() {
        return filterMe;
    }

    public void setEnableWarnings(boolean option) {
        enableWarnings = option;
        config.setBoolean("enableWarnings", option);
    }

    public boolean getEnableWarnings() {
        return enableWarnings;
    }

    public void setTimeoutDuration(int option) {
        timeoutDuration = option;
        config.setInt("timeoutDuration", option);
    }

    public int getTimeoutDuration() {
        return timeoutDuration;
    }

    //###################################################

    public boolean isRegular(String name) {
        synchronized (regulars) {
            for (String s : regulars) {
                if (s.equalsIgnoreCase(name)) {
                    return true;
                }
            }
        }
        return false;
    }

    public void addRegular(String name) {
        synchronized (regulars) {
            regulars.add(name.toLowerCase());
        }

        String regularsString = "";

        synchronized (regulars) {
            for (String s : regulars) {
                regularsString += s + ",";
            }
        }

        config.setString("regulars", regularsString);
    }

    public void removeRegular(String name) {
        synchronized (regulars) {
            if (regulars.contains(name.toLowerCase()))
                regulars.remove(name.toLowerCase());
        }
        String regularsString = "";

        synchronized (regulars) {
            for (String s : regulars) {
                regularsString += s + ",";
            }
        }

        config.setString("regulars", regularsString);
    }

    public Set<String> getRegulars() {
        return regulars;
    }

    public void permitUser(String name) {
        synchronized (permittedUsers) {
            if (permittedUsers.contains(name.toLowerCase()))
                return;
        }

        synchronized (permittedUsers) {
            permittedUsers.add(name.toLowerCase());
        }
    }

    public boolean linkPermissionCheck(String name) {

        if (this.isRegular(name)) {
            return true;
        }

        synchronized (permittedUsers) {
            if (permittedUsers.contains(name.toLowerCase())) {
                permittedUsers.remove(name.toLowerCase());
                return true;
            }
        }

        return false;
    }

    public boolean isSubscriber(String name) {
        if (subscribers.contains(name.toLowerCase()))
            return true;

//        if (emoteSet.length() > 0)
//            if (BotManager.getInstance().checkEmoteSetMapping(name, emoteSet))
//                return true;
        return false;
    }

    public void addSubscriber(String name) {
        subscribers.add(name.toLowerCase());
    }

    //###################################################

    public boolean isModerator(String name) {
        synchronized (tagModerators) {
            if (tagModerators.contains(name))
                return true;
        }
        synchronized (moderators) {
            if (moderators.contains(name.toLowerCase()))
                return true;
        }

        return false;
    }

    public void addModerator(String name) {
        synchronized (moderators) {
            moderators.add(name.toLowerCase());
        }

        String moderatorsString = "";

        synchronized (moderators) {
            for (String s : moderators) {
                moderatorsString += s + ",";
            }
        }

        config.setString("moderators", moderatorsString);
    }

    public void removeModerator(String name) {
        synchronized (moderators) {
            if (moderators.contains(name.toLowerCase()))
                moderators.remove(name.toLowerCase());
        }

        String moderatorsString = "";

        synchronized (moderators) {
            for (String s : moderators) {
                moderatorsString += s + ",";
            }
        }

        config.setString("moderators", moderatorsString);
    }

    public Set<String> getModerators() {
        return moderators;
    }

    //###################################################


    public boolean isOwner(String name) {
        synchronized (owners) {
            if (owners.contains(name.toLowerCase()))
                return true;
        }

        return false;
    }

    public void addOwner(String name) {
        synchronized (owners) {
            owners.add(name.toLowerCase());
        }

        String ownersString = "";

        synchronized (owners) {
            for (String s : owners) {
                ownersString += s + ",";
            }
        }

        config.setString("owners", ownersString);
    }

    public void removeOwner(String name) {
        synchronized (owners) {
            if (owners.contains(name.toLowerCase()))
                owners.remove(name.toLowerCase());
        }

        String ownersString = "";

        synchronized (owners) {
            for (String s : owners) {
                ownersString += s + ",";
            }
        }

        config.setString("owners", ownersString);
    }

    public Set<String> getOwners() {
        return owners;
    }

    //###################################################

    public void addPermittedDomain(String name) {
        synchronized (permittedDomains) {
            permittedDomains.add(name.toLowerCase());
        }

        String permittedDomainsString = "";

        synchronized (permittedDomains) {
            for (String s : permittedDomains) {
                permittedDomainsString += s + ",";
            }
        }

        config.setString("permittedDomains", permittedDomainsString);
    }

    public void removePermittedDomain(String name) {
        synchronized (permittedDomains) {
            for (int i = 0; i < permittedDomains.size(); i++) {
                if (permittedDomains.get(i).equalsIgnoreCase(name)) {
                    permittedDomains.remove(i);
                }
            }
        }

        String permittedDomainsString = "";

        synchronized (permittedDomains) {
            for (String s : permittedDomains) {
                permittedDomainsString += s + ",";
            }
        }

        config.setString("permittedDomains", permittedDomainsString);
    }

    public boolean isDomainPermitted(String domain) {
        for (String d : permittedDomains) {
            if (d.equalsIgnoreCase(domain)) {
                return true;
            }
        }

        return false;
    }

    public ArrayList<String> getpermittedDomains() {
        return permittedDomains;
    }
    // #################################################

    public void addOffensive(String word) {
        synchronized (offensiveWords) {
            offensiveWords.add(word);
        }

        synchronized (offensiveWordsRegex) {
            if (word.startsWith("REGEX:")) {
                String line = word.substring(6);
                System.out.println("Adding: " + line);
                Pattern tempP = Pattern.compile(line);
                offensiveWordsRegex.add(tempP);
            } else {
                String line = ".*" + Pattern.quote(word) + ".*";
                System.out.println("Adding: " + line);
                Pattern tempP = Pattern.compile(line, Pattern.CASE_INSENSITIVE);
                offensiveWordsRegex.add(tempP);
            }

        }

        String offensiveWordsString = "";


        synchronized (offensiveWords) {
            for (String s : offensiveWords) {
                offensiveWordsString += s + ",,";
            }
        }

        config.setString("offensiveWords", offensiveWordsString);
    }

    public void removeOffensive(String word) {
        synchronized (offensiveWords) {
            if (offensiveWords.contains(word))
                offensiveWords.remove(word);
        }

        String offensiveWordsString = "";
        synchronized (offensiveWords) {
            for (String s : offensiveWords) {
                offensiveWordsString += s + ",,";
            }
        }

        config.setString("offensiveWords", offensiveWordsString);

        synchronized (offensiveWordsRegex) {
            offensiveWordsRegex.clear();

            for (String w : offensiveWords) {
                if (w.startsWith("REGEX:")) {
                    String line = w.substring(6);
                    System.out.println("ReAdding: " + line);
                    Pattern tempP = Pattern.compile(line);
                    offensiveWordsRegex.add(tempP);
                } else {
                    String line = ".*" + Pattern.quote(w) + ".*";
                    System.out.println("ReAdding: " + line);
                    Pattern tempP = Pattern.compile(line);
                    offensiveWordsRegex.add(tempP);
                }
            }
        }
    }

    public void clearBannedPhrases() {
        offensiveWords.clear();
        offensiveWordsRegex.clear();
        config.setString("offensiveWords", "");
    }

    public boolean isBannedPhrase(String phrase) {
        return offensiveWords.contains(phrase);
    }

    public boolean isOffensive(String word) {
        for (Pattern reg : offensiveWordsRegex) {
            Matcher match = reg.matcher(word.toLowerCase());
            if (match.find()) {
                System.out.println("Matched: " + reg.toString());
                return true;
            }
        }

        int severity = config.getInt("banPhraseSeverity");
        if (BotManager.getInstance().banPhraseLists.containsKey(severity)) {
            for (Pattern reg : BotManager.getInstance().banPhraseLists.get(severity)) {
                Matcher match = reg.matcher(word.toLowerCase());
                if (match.find()) {
                    System.out.println("Matched: " + reg.toString());
                    return true;
                }
            }
        }

        return false;
    }

    public Set<String> getOffensive() {
        return offensiveWords;
    }

    // ##################################################

    public void setTopicFeature(boolean setting) {
        this.useTopic = setting;
        config.setBoolean("useTopic", this.useTopic);

    }

    public void setFiltersFeature(boolean setting) {
        this.useFilters = setting;
        config.setBoolean("useFilters", this.useFilters);
    }

    public Poll getPoll() {
        return currentPoll;
    }

    public void setPoll(Poll _poll) {
        currentPoll = _poll;
    }

    public Giveaway getGiveaway() {
        return currentGiveaway;
    }

    public void setGiveaway(Giveaway _gw) {
        currentGiveaway = _gw;
    }

    public boolean checkThrow() {
        return enableThrow;
    }

    public void setThrow(boolean setting) {
        this.enableThrow = setting;
        config.setBoolean("enableThrow", this.enableThrow);
    }

    public boolean checkSignKicks() {
        return signKicks;
    }

    public void setSignKicks(boolean setting) {
        this.signKicks = setting;
        config.setBoolean("signKicks", this.signKicks);
    }

    public void setLogging(boolean option) {
        logChat = option;
        config.setBoolean("logChat", option);
    }

    public boolean getLogging() {
        return logChat;
    }

    public int getCommercialLength() {
        return commercialLength;
    }

    public void setCommercialLength(int commercialLength) {
        this.commercialLength = commercialLength;
        config.setInt("commercialLength", commercialLength);
    }

    // ##################################################

    public boolean checkPermittedDomain(String message) {
        //Allow base domain w/o a path
        if (message.matches(".*(twitch\\.tv|twitchtv\\.com|justin\\.tv)")) {
            System.out.println("INFO: Permitted domain match on jtv/ttv base domain.");
            return true;
        }

        for (String d : permittedDomains) {
            //d = d.replaceAll("\\.", "\\\\.");

            String test = ".*(\\.|^|//)" + Pattern.quote(d) + "(/|$).*";
            if (message.matches(test)) {
                //System.out.println("DEBUG: Matched permitted domain: " + test);
                return true;
            }
        }
        return false;
    }

    // #################################################

    public String getLastfm() {
        return lastfm;
    }

    public void setLastfm(String string) {
        lastfm = string;
        config.setString("lastfm", lastfm);
    }

    // #################################################


    public String getSteam() {
        return steamID;
    }

    public void setSteam(String string) {
        steamID = string;
        config.setString("steamID", steamID);
    }

    // #################################################

    public String getClickToTweetFormat() {
        return clickToTweetFormat;
    }

    public void setClickToTweetFormat(String string) {
        clickToTweetFormat = string;
        config.setString("clickToTweetFormat", clickToTweetFormat);
    }

    public int getWarningCount(String name, FilterType type) {
        if (warningCount.containsKey(name.toLowerCase()) && warningCount.get(name.toLowerCase()).containsKey(type))
            return warningCount.get(name.toLowerCase()).get(type);
        else
            return 0;
    }

    public void incWarningCount(String name, FilterType type) {
        clearWarnings();
        synchronized (warningCount) {
            if (warningCount.containsKey(name.toLowerCase())) {
                if (warningCount.get(name.toLowerCase()).containsKey(type)) {
                    warningCount.get(name.toLowerCase()).put(type, warningCount.get(name.toLowerCase()).get(type) + 1);
                    warningTime.put(name.toLowerCase(), getTime());
                } else {
                    warningCount.get(name.toLowerCase()).put(type, 1);
                    warningTime.put(name.toLowerCase(), getTime());
                }
            } else {
                warningCount.put(name.toLowerCase(), new EnumMap<FilterType, Integer>(FilterType.class));
                warningCount.get(name.toLowerCase()).put(type, 1);
                warningTime.put(name.toLowerCase(), getTime());
            }
        }
    }

    public void clearWarnings() {
        List<String> toRemove = new ArrayList<String>();
        synchronized (warningTime) {
            synchronized (warningCount) {
                long time = getTime();
                for (Map.Entry<String, Long> entry : warningTime.entrySet()) {
                    if ((time - entry.getValue()) > 3600) {
                        toRemove.add((String) entry.getKey());
                    }
                }
                for (String name : toRemove) {
                    warningCount.remove(name);
                    warningTime.remove(name);
                }
            }
        }
    }

    private void registerCommandUsage(String command) {
        synchronized (commandCooldown) {
            System.out.println("DEBUG: Adding command " + command + " to cooldown list");
            commandCooldown.put(command.toLowerCase(), getTime());
        }
    }

    public boolean onCooldown(String command) {
        command = command.toLowerCase();
        if (commandCooldown.containsKey(command)) {
            long lastUse = commandCooldown.get(command);
            if ((getTime() - lastUse) > 30) {
                //Over
                System.out.println("DEBUG: Cooldown for " + command + " is over");
                registerCommandUsage(command);
                return false;
            } else {
                //Not Over
                System.out.println("DEBUG: Cooldown for " + command + " is NOT over");
                return true;
            }
        } else {
            registerCommandUsage(command);
            return false;
        }
    }

    public void reload() {
        BotManager.getInstance().removeChannel(channel);
        BotManager.getInstance().addChannel(channel, mode);
    }

    private void setDefaults() {

        //defaults.put("channel", channel);
        defaults.put("filterCaps", false);
        defaults.put("filterOffensive", true);
        defaults.put("filterCapsPercent", 50);
        defaults.put("filterCapsMinCharacters", 0);
        defaults.put("filterCapsMinCapitals", 6);
        defaults.put("filterLinks", false);
        defaults.put("filterEmotes", false);
        defaults.put("filterSymbols", false);
        defaults.put("filterEmotesMax", 4);
        defaults.put("topic", "");
        defaults.put("commandsKey", "");
        defaults.put("commandsValue", "");
        defaults.put("commandsRepeatKey", "");
        defaults.put("commandsRepeatDelay", "");
        defaults.put("commandsRepeatDiff", "");
        defaults.put("commandsRepeatActive", "");
        defaults.put("commandsScheduleKey", "");
        defaults.put("commandsSchedulePattern", "");
        defaults.put("commandsScheduleDiff", "");
        defaults.put("commandsScheduleActive", "");
        defaults.put("autoReplyTriggers", "");
        defaults.put("autoReplyResponse", "");
        defaults.put("regulars", "");
        defaults.put("moderators", "");
        defaults.put("owners", "");
        defaults.put("useTopic", true);
        defaults.put("useFilters", false);
        defaults.put("enableThrow", true);
        defaults.put("permittedDomains", "");
        defaults.put("signKicks", false);
        defaults.put("topicTime", 0);
        defaults.put("mode", 2);
        defaults.put("announceJoinParts", false);
        defaults.put("lastfm", "");
        defaults.put("steamID", "");
        defaults.put("logChat", false);
        defaults.put("filterMaxLength", 500);
        defaults.put("offensiveWords", "");
        defaults.put("commercialLength", 30);
        defaults.put("filterColors", false);
        defaults.put("filterMe", false);
        defaults.put("staticChannel", false);
        defaults.put("enableWarnings", true);
        defaults.put("timeoutDuration", 600);
        defaults.put("clickToTweetFormat", "Checkout (_CHANNEL_URL_) playing (_GAME_) on @TwitchTV");
        defaults.put("filterSymbolsPercent", 50);
        defaults.put("filterSymbolsMin", 5);
        defaults.put("commandPrefix", "!");
        defaults.put("commandRestrictions", "");
        defaults.put("emoteSet", "");
        defaults.put("subscriberRegulars", false);
        defaults.put("filterEmotesSingle", false);
        defaults.put("subMessage", "(_1_) has subscribed!");
        defaults.put("subscriberAlert", false);
        defaults.put("banPhraseSeverity", 99);
        defaults.put("globalFilter", true);

        Iterator it = defaults.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pairs = (Map.Entry) it.next();
            String key = String.valueOf(pairs.getKey());
            String value = String.valueOf(pairs.getValue());
            if (!config.containsKey(key))
                config.setString(key, value);
        }
    }

    private void loadProperties(String name) {
        setDefaults();

        //channel = config.getString("channel");
        filterCaps = Boolean.parseBoolean(config.getString("filterCaps"));
        filterCapsPercent = Integer.parseInt(config.getString("filterCapsPercent"));
        filterCapsMinCharacters = Integer.parseInt(config.getString("filterCapsMinCharacters"));
        filterCapsMinCapitals = Integer.parseInt(config.getString("filterCapsMinCapitals"));
        filterLinks = Boolean.parseBoolean(config.getString("filterLinks"));
        filterOffensive = Boolean.parseBoolean(config.getString("filterOffensive"));
        filterEmotes = Boolean.parseBoolean(config.getString("filterEmotes"));

        filterSymbols = Boolean.parseBoolean(config.getString("filterSymbols"));
        filterSymbolsPercent = Integer.parseInt(config.getString("filterSymbolsPercent"));
        filterSymbolsMin = Integer.parseInt(config.getString("filterSymbolsMin"));

        filterEmotesMax = Integer.parseInt(config.getString("filterEmotesMax"));
        filterEmotesSingle = Boolean.parseBoolean(config.getString("filterEmotesSingle"));
        //announceJoinParts = Boolean.parseBoolean(config.getString("announceJoinParts"));
        announceJoinParts = false;
        topic = config.getString("topic");
        topicTime = config.getInt("topicTime");
        useTopic = Boolean.parseBoolean(config.getString("useTopic"));
        useFilters = Boolean.parseBoolean(config.getString("useFilters"));
        enableThrow = Boolean.parseBoolean(config.getString("enableThrow"));
        signKicks = Boolean.parseBoolean(config.getString("signKicks"));
        lastfm = config.getString("lastfm");
        steamID = config.getString("steamID");
        logChat = Boolean.parseBoolean(config.getString("logChat"));
        mode = config.getInt("mode");
        filterMaxLength = config.getInt("filterMaxLength");
        commercialLength = config.getInt("commercialLength");
        filterColors = Boolean.parseBoolean(config.getString("filterColors"));
        filterMe = Boolean.parseBoolean(config.getString("filterMe"));
        staticChannel = Boolean.parseBoolean(config.getString("staticChannel"));
        clickToTweetFormat = config.getString("clickToTweetFormat");

        enableWarnings = Boolean.parseBoolean(config.getString("enableWarnings"));
        timeoutDuration = config.getInt("timeoutDuration");
        prefix = config.getString("commandPrefix").charAt(0) + "";
        emoteSet = config.getString("emoteSet");
        subscriberRegulars = config.getBoolean("subscriberRegulars");

        String[] commandsKey = config.getString("commandsKey").split(",");
        String[] commandsValue = config.getString("commandsValue").split(",,");

        for (int i = 0; i < commandsKey.length; i++) {
            if (commandsKey[i].length() > 1) {
                commands.put(commandsKey[i].replaceAll("[^a-zA-Z0-9]", "").toLowerCase(), commandsValue[i]);
            }
        }

        String[] commandR = config.getString("commandRestrictions").split(",");
        for (int i = 0; i < commandR.length; i++) {
            if (commandR[i].length() > 1) {
                String[] parts = commandR[i].split("\\|");
                commandsRestrictions.put(parts[0], Integer.parseInt(parts[1]));
            }
        }

        String[] commandsRepeatKey = config.getString("commandsRepeatKey").split(",");
        String[] commandsRepeatDelay = config.getString("commandsRepeatDelay").split(",");
        String[] commandsRepeatDiff = config.getString("commandsRepeatDiff").split(",");
        String[] commandsRepeatActive = config.getString("commandsRepeatActive").split(",");


        for (int i = 0; i < commandsRepeatKey.length; i++) {
            if (commandsRepeatKey[i].length() > 1) {
                RepeatCommand rc = new RepeatCommand(channel, commandsRepeatKey[i].replaceAll("[^a-zA-Z0-9]", ""), Integer.parseInt(commandsRepeatDelay[i]), Integer.parseInt(commandsRepeatDiff[i]), Boolean.parseBoolean(commandsRepeatActive[i]));
                commandsRepeat.put(commandsRepeatKey[i].replaceAll("[^a-zA-Z0-9]", ""), rc);
            }
        }

        String[] commandsScheduleKey = config.getString("commandsScheduleKey").split(",,");
        String[] commandsSchedulePattern = config.getString("commandsSchedulePattern").split(",,");
        String[] commandsScheduleDiff = config.getString("commandsScheduleDiff").split(",,");
        String[] commandsScheduleActive = config.getString("commandsScheduleActive").split(",,");


        for (int i = 0; i < commandsScheduleKey.length; i++) {
            if (commandsScheduleKey[i].length() > 1) {
                ScheduledCommand rc = new ScheduledCommand(channel, commandsScheduleKey[i].replaceAll("[^a-zA-Z0-9]", ""), commandsSchedulePattern[i], Integer.parseInt(commandsScheduleDiff[i]), Boolean.parseBoolean(commandsScheduleActive[i]));
                commandsSchedule.put(commandsScheduleKey[i].replaceAll("[^a-zA-Z0-9]", ""), rc);
            }
        }

        String[] autoReplyTriggersString = config.getString("autoReplyTriggers").split(",,");
        String[] autoReplyResponseString = config.getString("autoReplyResponse").split(",,");

        for (int i = 0; i < autoReplyTriggersString.length; i++) {
            if (autoReplyTriggersString[i].length() > 0) {
                autoReplyTrigger.add(Pattern.compile(autoReplyTriggersString[i], Pattern.CASE_INSENSITIVE));
                autoReplyResponse.add(autoReplyResponseString[i]);
            }
        }

        String[] regularsRaw = config.getString("regulars").split(",");
        synchronized (regulars) {
            for (int i = 0; i < regularsRaw.length; i++) {
                if (regularsRaw[i].length() > 1) {
                    regulars.add(regularsRaw[i].toLowerCase());
                }
            }
        }

        String[] moderatorsRaw = config.getString("moderators").split(",");
        synchronized (moderators) {
            for (int i = 0; i < moderatorsRaw.length; i++) {
                if (moderatorsRaw[i].length() > 1) {
                    moderators.add(moderatorsRaw[i].toLowerCase());
                }
            }
        }

        String[] ownersRaw = config.getString("owners").split(",");
        synchronized (owners) {
            for (int i = 0; i < ownersRaw.length; i++) {
                if (ownersRaw[i].length() > 1) {
                    owners.add(ownersRaw[i].toLowerCase());
                }
            }
        }

        String[] domainsRaw = config.getString("permittedDomains").split(",");
        synchronized (permittedDomains) {
            for (int i = 0; i < domainsRaw.length; i++) {
                if (domainsRaw[i].length() > 1) {
//					permittedDomains.add(domainsRaw[i].toLowerCase().replaceAll("\\.", "\\\\."));
                    permittedDomains.add(domainsRaw[i].toLowerCase());

                }
            }
        }
        System.out.println(config.getString("offensiveWords"));
        String[] offensiveWordsRaw = config.getString("offensiveWords").split(",,");
        synchronized (offensiveWords) {
            synchronized (offensiveWordsRegex) {
                for (int i = 0; i < offensiveWordsRaw.length; i++) {
                    if (offensiveWordsRaw[i].length() > 1) {
                        String w = offensiveWordsRaw[i];
                        offensiveWords.add(w);
                        if (w.startsWith("REGEX:")) {
                            String line = w.substring(6);
                            System.out.println("Adding: " + line);
                            Pattern tempP = Pattern.compile(line);
                            offensiveWordsRegex.add(tempP);
                        } else {
                            String line = "(?i).*" + Pattern.quote(w) + ".*";
                            System.out.println("Adding: " + line);
                            Pattern tempP = Pattern.compile(line);
                            offensiveWordsRegex.add(tempP);
                        }

                    }
                }
            }

        }

    }

    public void setMode(int mode) {
        this.mode = mode;
        config.setInt("mode", this.mode);

        if (mode == -1) {
            this.setFiltersFeature(true);
            this.setFilterEmotes(false);
            this.setFilterEmotesMax(5);
            this.setFilterSymbols(true);
            this.setFilterCaps(false);
            this.setFilterLinks(false);
            this.setFilterOffensive(true);
            this.setSignKicks(false);
            this.setTopicFeature(false);
            this.setThrow(false);
        }
    }

    public int getMode() {
        return mode;
    }

    private long getTime() {
        return (System.currentTimeMillis() / 1000L);
    }

    public void runCommercial() {
        if (skipNextCommercial) {
            System.out.println("Commercial skipped via command.");
            skipNextCommercial = false;
            return;
        }

        if (JSONUtil.krakenIsLive(getChannel().substring(1))) {
            String dataIn = "";
            dataIn = BotManager.postRemoteDataTwitch("https://api.twitch.tv/kraken/channels/" + getChannel().substring(1) + "/commercial", "length=" + commercialLength, 2);

            System.out.println(dataIn);
        } else {
            System.out.println(getChannel().substring(1) + " is not live. Skipping commercial.");
        }
    }
}
