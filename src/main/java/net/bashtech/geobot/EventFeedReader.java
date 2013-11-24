package net.bashtech.geobot;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;

public class EventFeedReader implements Runnable {
    JSONParser parser;

    EventFeedReader() {
        parser = new JSONParser();
    }

    public void run() {
        while (true)
            this.readStream();
    }

    public void readStream() {
        try {
            URL url = new URL(BotManager.getInstance().eventFeedURL);
            URLConnection urlConn = url.openConnection();
            urlConn.setDoOutput(true);
            urlConn.setReadTimeout(1000 * 5);
            BufferedReader in = new BufferedReader(
                    new InputStreamReader(urlConn.getInputStream()));

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                if (inputLine.length() > 0 && !inputLine.startsWith("event:") && inputLine.length() > 8) {
                    JSONObject jo = this.parseMessage(inputLine);
                    String room = (String) jo.get("room");
                    if (BotManager.getInstance().checkChannel(room)) {
                        System.out.println(inputLine);
                        String nick = (String) jo.get("nick");
                        String message = (String) jo.get("body");
                        BotManager.getInstance().receiverBot.onChannelMessage(room, nick, message);
                    }

                } else if (inputLine.startsWith(":ping")) {
                    System.out.println(inputLine);
                }
            }
            in.close();
            System.out.println("################ Connection Lost ################");

            //in.close();
        } catch (Exception e) {
            System.out.println("################ Connection Interrupted ################");
        }

    }

    public JSONObject parseMessage(String rawEvent) {
        rawEvent = rawEvent.substring(6);

        try {
            Object obj = parser.parse(rawEvent);
            JSONObject jsonObject = (JSONObject) obj;
            return jsonObject;
        } catch (ParseException e) {
            return null;
        }
    }
}