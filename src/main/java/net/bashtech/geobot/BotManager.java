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

import net.bashtech.geobot.gui.BotGUI;
import net.bashtech.geobot.modules.BotModule;
import net.bashtech.geobot.modules.Logger;
import org.java_websocket.WebSocketImpl;

import java.io.*;
import java.net.*;
import java.util.*;
import java.util.regex.Pattern;

public class BotManager {

    static BotManager instance;
    // API KEYS
    public String bitlyAPIKey;
    public String bitlyLogin;
    public String LastFMAPIKey;
    public String SteamAPIKey;
    public String krakenOAuthToken;
    public String krakenClientID;
    String nick;
    String server;
    int port;
    String password;
    String localAddress;
    boolean publicJoin;
    boolean monitorPings;
    int pingInterval;
    boolean useGUI;
    BotGUI gui;
    Map<String, Channel> channelList;
    Set<String> admins;
    List<String> emoteSet;
    List<Pattern> globalBannedWords;
    boolean verboseLogging;
    ReceiverBot receiverBot;
    String bothelpMessage;
    boolean ignoreHistory;
    WSServer ws;
    boolean wsEnabled;
    int wsPort;
    String wsAdminPassword;
    boolean twitchChannels;
    private PropertiesFile config;
    private Set<BotModule> modules;
    private Set<String> tagAdmins;
    private Set<String> tagStaff;
    int multipleTimeout;
    // ********
    private String _propertiesFile;

    Map<String, Set<String>> emoteSetMapping;


    public BotManager(String propertiesFile) {
        BotManager.setInstance(this);
        _propertiesFile = propertiesFile;
        channelList = new HashMap<String, Channel>();
        admins = new HashSet<String>();
        modules = new HashSet<BotModule>();
        tagAdmins = new HashSet<String>();
        tagStaff = new HashSet<String>();
        emoteSet = new LinkedList<String>();
        globalBannedWords = new LinkedList<Pattern>();
        emoteSetMapping = new HashMap<String, Set<String>>();

        loadGlobalProfile();

        if (useGUI) {
            gui = new BotGUI();
        }

        //Start WebSocket server
        if (wsEnabled) {
            WebSocketImpl.DEBUG = false;
            ws = null;
            try {
                ws = new WSServer(wsPort);
                ws.start();
                System.out.println("WebSocket server started on port: " + ws.getPort());
            } catch (UnknownHostException e) {
                e.printStackTrace();
            }
        }


        receiverBot = new ReceiverBot(server, port);
        List<String> outdatedChannels = new LinkedList<String>();
        for (Map.Entry<String, Channel> entry : channelList.entrySet()) {
            String channel = entry.getValue().getChannel();
            if (!JSONUtil.krakenOutdatedChannel(channel.substring(1)) || receiverBot.getNick().equalsIgnoreCase(channel.substring(1)) || entry.getValue().staticChannel) {
                log("BM: Joining channel " + channel);
                receiverBot.joinChannel(channel);
            } else {
                outdatedChannels.add(channel);
            }

        }

        //Remove outdatedChannels
        for (String channel : outdatedChannels) {
            log("BM: Removing channel: " + channel);
            this.removeChannel(channel);
        }

        //Start timer to check for bot disconnects
        Timer reconnectTimer = new Timer();
        reconnectTimer.scheduleAtFixedRate(new ReconnectTimer(channelList), 30 * 1000, 30 * 1000);

        // Load modules
        this.registerModule(new Logger());
    }

    public static BotManager getInstance() {
        return instance;
    }

    public static void setInstance(BotManager bm) {
        if (instance == null) {
            instance = bm;
        }
    }

    public static String getRemoteContent(String urlString) {
        String dataIn = "";
        try {
            URL url = new URL(urlString);
            //System.out.println("DEBUG: Getting data from " + url.toString());
            URLConnection conn = url.openConnection();
            BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
            String inputLine;
            while ((inputLine = in.readLine()) != null)
                dataIn += inputLine;
            in.close();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return dataIn;
    }

    public static String getRemoteContentTwitch(String urlString, int krakenVersion) {
        String dataIn = "";
        try {
            URL url = new URL(urlString);
            //System.out.println("DEBUG: Getting data from " + url.toString());
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
//            conn.setConnectTimeout(connectTimeout);
//            conn.setReadTimeout(socketTimeout);

            if (BotManager.getInstance().krakenClientID.length() > 0)
                conn.setRequestProperty("Client-ID", BotManager.getInstance().krakenClientID);

            conn.setRequestProperty("Accept", "application/vnd.twitchtv.v" + krakenVersion + "+json");

            try {
                BufferedReader in = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                String inputLine;
                while ((inputLine = in.readLine()) != null)
                    dataIn += inputLine;
                in.close();
            } catch (IOException exerr) {
                String inputLine;

                InputStream errorStream = conn.getErrorStream();
                if (errorStream != null) {
                    BufferedReader inE = new BufferedReader(new InputStreamReader(errorStream));
                    while ((inputLine = inE.readLine()) != null)
                        dataIn += inputLine;
                    inE.close();
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        return dataIn;
    }

    public static String postRemoteDataTwitch(String urlString, String postData, int krakenVersion) {
        URL url;
        HttpURLConnection conn;

        try {
            url = new URL(urlString);

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");


            conn.setFixedLengthStreamingMode(postData.getBytes().length);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Accept", "application/vnd.twitchtv.v" + krakenVersion + "+json");
            conn.setRequestProperty("Authorization", "OAuth " + BotManager.getInstance().krakenOAuthToken);
            conn.setRequestProperty("Client-ID", BotManager.getInstance().krakenClientID);

            PrintWriter out = new PrintWriter(conn.getOutputStream());
            out.print(postData);
            out.close();

            String response = "";

            Scanner inStream = new Scanner(conn.getInputStream());

            while (inStream.hasNextLine())
                response += (inStream.nextLine());

            System.out.println(conn.getResponseCode());
            System.out.println(response);
            return response;

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }

        return "";
    }

    public static String putRemoteData(String urlString, String postData) throws IOException {
        URL url;
        HttpURLConnection conn;

        try {
            url = new URL(urlString);

            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");


            conn.setFixedLengthStreamingMode(postData.getBytes().length);
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Accept", "application/vnd.twitchtv.v2+json");
            conn.setRequestProperty("Authorization", "OAuth " + BotManager.getInstance().krakenOAuthToken);
            conn.setRequestProperty("Client-ID", BotManager.getInstance().krakenClientID);

            PrintWriter out = new PrintWriter(conn.getOutputStream());
            out.print(postData);
            out.close();

            String response = "";

            Scanner inStream = new Scanner(conn.getInputStream());

            while (inStream.hasNextLine())
                response += (inStream.nextLine());

            System.out.println(conn.getResponseCode());
            System.out.println(response);
            return response;

        } catch (MalformedURLException ex) {
            ex.printStackTrace();
        }

        return "";
    }

    private void loadGlobalProfile() {
        config = new PropertiesFile(_propertiesFile);

        log("BM: Reading global file > " + _propertiesFile);
        try {
            config.load();
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!config.keyExists("nick")) {
            config.setString("nick", "");
        }
        if (!config.keyExists("server")) {
            config.setString("server", "");
        }

        if (!config.keyExists("password")) {
            config.setString("password", "");
        }

        if (!config.keyExists("port")) {
            config.setInt("port", 6667);
        }

        if (!config.keyExists("channelList")) {
            config.setString("channelList", "");
        }

        if (!config.keyExists("adminList")) {
            config.setString("adminList", "");
        }

        if (!config.keyExists("publicJoin")) {
            config.setBoolean("publicJoin", false);
        }

        if (!config.keyExists("monitorPings")) {
            config.setBoolean("monitorPings", false);
        }

        if (!config.keyExists("pingInterval")) {
            config.setInt("pingInterval", 350);
        }

        if (!config.keyExists("useGUI")) {
            config.setBoolean("useGUI", false);
        }

        if (!config.keyExists("localAddress")) {
            config.setString("localAddress", "");
        }

        if (!config.keyExists("verboseLogging")) {
            config.setBoolean("verboseLogging", false);
        }

        if (!config.keyExists("bothelpMessage")) {
            config.setString("bothelpMessage", "http://bashtech.net/twitch/geobot.php");
        }

        // API KEYS

        if (!config.keyExists("bitlyAPIKey")) {
            config.setString("bitlyAPIKey", "");
        }

        if (!config.keyExists("bitlyLogin")) {
            config.setString("bitlyLogin", "");
        }

        if (!config.keyExists("LastFMAPIKey")) {
            config.setString("LastFMAPIKey", "");
        }

        if (!config.keyExists("SteamAPIKey")) {
            config.setString("SteamAPIKey", "");
        }

        if (!config.keyExists("krakenOAuthToken")) {
            config.setString("krakenOAuthToken", "");
        }

        if (!config.keyExists("krakenClientID")) {
            config.setString("krakenClientID", "");
        }

        if (!config.keyExists("wsPort")) {
            config.setInt("wsPort", 8887);
        }

        if (!config.keyExists("wsEnabled")) {
            config.setBoolean("wsEnabled", false);
        }

        if (!config.keyExists("wsAdminPassword")) {
            config.setString("wsAdminPassword", "");
        }

        if (!config.keyExists("twitchChannels")) {
            config.setBoolean("twitchChannels", true);
        }

        if (!config.keyExists("ignoreHistory")) {
            config.setBoolean("ignoreHistory", true);
        }

        if (!config.keyExists("multipleTimeout")) {
            config.setInt("multipleTimeout", 3);
        }
        // ********

        nick = config.getString("nick");
        server = config.getString("server");
        port = Integer.parseInt(config.getString("port"));
        localAddress = config.getString("localAddress");
        password = config.getString("password");
        useGUI = config.getBoolean("useGUI");
        monitorPings = config.getBoolean("monitorPings");
        pingInterval = config.getInt("pingInterval");
        publicJoin = config.getBoolean("publicJoin");
        verboseLogging = config.getBoolean("verboseLogging");
        bothelpMessage = config.getString("bothelpMessage");

        wsEnabled = config.getBoolean("wsEnabled");
        wsPort = config.getInt("wsPort");
        wsAdminPassword = config.getString("wsAdminPassword");

        twitchChannels = config.getBoolean("twitchChannels");
        ignoreHistory = config.getBoolean("ignoreHistory");
        multipleTimeout = config.getInt("multipleTimeout");

        // API KEYS

        bitlyAPIKey = config.getString("bitlyAPIKey");
        bitlyLogin = config.getString("bitlyLogin");
        LastFMAPIKey = config.getString("LastFMAPIKey");
        SteamAPIKey = config.getString("SteamAPIKey");
        krakenOAuthToken = config.getString("krakenOAuthToken");
        krakenClientID = config.getString("krakenClientID");


        // ********

        for (String s : config.getString("channelList").split(",")) {
            System.out.println("DEBUG: Adding channel " + s);
            if (s.length() > 1) {
                channelList.put(s.toLowerCase(), new Channel(s));
            }
        }

        for (String s : config.getString("adminList").split(",")) {
            if (s.length() > 1) {
                admins.add(s.toLowerCase());
            }
        }

        loadEmotes();
        loadGlobalBannedWords();

        if (server.length() < 1) {
            System.exit(1);
        }
    }

    public synchronized Channel getChannel(String channel) {
        if (channelList.containsKey(channel.toLowerCase())) {

            return channelList.get(channel.toLowerCase());
        } else {
            return null;
        }
    }

    public synchronized boolean addChannel(String name, int mode) {
        if (channelList.containsKey(name.toLowerCase())) {
            System.out.println("INFO: Already in channel " + name);
            return false;
        }
        Channel tempChan = new Channel(name.toLowerCase(), mode);

        channelList.put(name.toLowerCase(), tempChan);

        log("BM: Joining channel " + tempChan.getChannel());
        receiverBot.joinChannel(tempChan.getChannel());

        log("BM: Joined channel " + tempChan.getChannel());

        writeChannelList();

        return true;
    }

    public synchronized void removeChannel(String name) {
        if (!channelList.containsKey(name.toLowerCase())) {
            log("BM: Not in channel " + name);
            return;
        }

        Channel tempChan = channelList.get(name.toLowerCase());

        //Stop timers
        System.out.println("Stopping timers");
        Iterator itr = tempChan.commandsRepeat.entrySet().iterator();
        while (itr.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr.next();
            ((RepeatCommand) pairs.getValue()).setStatus(false);
        }
        Iterator itr2 = tempChan.commandsSchedule.entrySet().iterator();
        while (itr2.hasNext()) {
            Map.Entry pairs = (Map.Entry) itr2.next();
            ((ScheduledCommand) pairs.getValue()).setStatus(false);
        }

        receiverBot.partChannel(name.toLowerCase());
        channelList.remove(name.toLowerCase());

        writeChannelList();
    }

    public synchronized void reloadChannel(String name) {
        if (!channelList.containsKey(name.toLowerCase())) {
            log("BM: Not in channel " + name);
            return;
        }

        channelList.get(name.toLowerCase()).reload();
    }

    public boolean rejoinChannel(String name) {
        if (!channelList.containsKey(name.toLowerCase())) {
            log("BM: Not in channel " + name);
            return false;
        }

        Channel tempChan = channelList.get(name.toLowerCase());
        receiverBot.partChannel(tempChan.getChannel());
        receiverBot.joinChannel(tempChan.getChannel());


        return true;
    }

    public synchronized void reconnectAllBotsSoft() {
        receiverBot.disconnect();
    }

    private synchronized void writeChannelList() {
        String channelString = "";
        for (Map.Entry<String, Channel> entry : channelList.entrySet()) {
            channelString += entry.getKey() + ",";
        }

        config.setString("channelList", channelString);
    }

    public void registerModule(BotModule module) {
        modules.add(module);
    }

    public Set<BotModule> getModules() {
        return modules;
    }

    public BotGUI getGUI() {
        return gui;
    }

    public String getLocalAddress() {
        return localAddress;
    }

    public void sendGlobal(String message, String sender) {
        for (Map.Entry<String, Channel> entry : channelList.entrySet()) {
            Channel tempChannel = (Channel) entry.getValue();
            if (tempChannel.getMode() == 0)
                continue;

            String globalMsg = "> Global: " + message + " (from " + sender + " to " + tempChannel.getChannel() + ")";
            ReceiverBot.getInstance().sendMessage(tempChannel.getChannel(), globalMsg);
        }
    }

    public boolean isAdmin(String nick) {
        if (admins.contains(nick.toLowerCase()))
            return true;
        else
            return false;
    }

    public boolean isTagAdmin(String name) {
        return tagAdmins.contains(name.toLowerCase());
    }

    public boolean isTagStaff(String name) {
        return tagStaff.contains(name.toLowerCase());
    }

    public void addTagAdmin(String name) {
        tagAdmins.add(name.toLowerCase());
    }

    public void addTagStaff(String name) {
        tagStaff.add(name.toLowerCase());
    }

    private void loadEmotes() {
        emoteSet.clear();
        File f = new File("emotes.cfg");
        if (!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        try {
            Scanner in = new Scanner(f, "UTF-8");

            while (in.hasNextLine()) {
                emoteSet.add(in.nextLine().trim());
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void loadGlobalBannedWords() {
        globalBannedWords.clear();
        File f = new File("globalbannedwords.cfg");
        if (!f.exists())
            try {
                f.createNewFile();
            } catch (IOException e1) {
                // TODO Auto-generated catch block
                e1.printStackTrace();
            }
        try {
            Scanner in = new Scanner(f, "UTF-8");

            while (in.hasNextLine()) {
                String line = in.nextLine().replace("\uFEFF", "");
                if (line.length() > 0) {

                    if (line.startsWith("REGEX:"))
                        line = line.replaceAll("REGEX:", "");
                    else
                        line = ".*" + Pattern.quote(line) + ".*";

                    System.out.println(line);
                    Pattern tempP = Pattern.compile(line, Pattern.CASE_INSENSITIVE);
                    globalBannedWords.add(tempP);
                }
            }
        } catch (FileNotFoundException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }

    }

    public void followChannel(String channel) {
        try {
            System.out.println(BotManager.putRemoteData("https://api.twitch.tv/kraken/users/" + this.nick + "/follows/channels/" + channel, ""));
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public void addSubBySet(String username, String setID) {
        if (emoteSetMapping.containsKey(setID)) {
            emoteSetMapping.get(setID).add(username);
        } else {
            emoteSetMapping.put(setID, new HashSet<String>());
        }
    }

    public boolean checkEmoteSetMapping(String username, String setID) {
        if (emoteSetMapping.containsKey(setID)) {
            if (emoteSetMapping.get(setID).contains(username))
                return true;
        }

        return false;
    }

    public void log(String line) {
        System.out.println(line);

        if (wsEnabled && !line.startsWith("MSG:") && !line.startsWith("SEND:"))
            ws.sendToAdmin(line);

        if (useGUI) {
            getGUI().log(line);
        }
    }

}