package net.bashtech.geobot;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;


public class JSONUtil {

//    public static void krakenStreams() throws Exception{
//        JSONParser parser = new JSONParser();
//        Object obj = parser.parse(BotManager.getRemoteContent("https://api.twitch.tv/kraken/streams/mlglol"));
//
//        JSONObject jsonObject = (JSONObject) obj;
//
//        JSONObject stream = (JSONObject)(jsonObject.get("stream"));
//        Long viewers = (Long)stream.get("viewers");
//        System.out.println("Viewers: " + viewers);
//    }

    public static String krakenViewers(String channel) throws Exception{
        try{
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(BotManager.getRemoteContent("https://api.twitch.tv/kraken/streams/mlglol"));

            JSONObject jsonObject = (JSONObject) obj;

            JSONObject stream = (JSONObject)(jsonObject.get("stream"));
            Long viewers = (Long)stream.get("viewers");
            return "Viewers: " + viewers;
        }catch (Exception ex){
            ex.printStackTrace();
            return "Error querying API";
        }

    }

    public static String lastFM(String user){
        String api_key = BotManager.getInstance().LastFMAPIKey;
        try{
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(BotManager.getRemoteContent("http://ws.audioscrobbler.com/2.0/?method=user.getrecenttracks&user=" + user + "&format=json&limit=1&api_key=" + api_key));

            JSONObject jsonObject = (JSONObject) obj;

            JSONObject recenttracks = (JSONObject)(jsonObject.get("recenttracks"));

            if(recenttracks.get("track") instanceof JSONArray){
                JSONArray track = (JSONArray) recenttracks.get("track");

                JSONObject index0 = (JSONObject) track.get(0);
                String trackName = (String) index0.get("name");
                JSONObject artistO = (JSONObject) index0.get("artist");
                String artist = (String) artistO.get("#text");

                return trackName + " by " + artist;

            }else{
                return "No music currently playing";
            }
        }catch (Exception ex){
            ex.printStackTrace();
            return "Error querying API";
        }

    }

    public static String steam(String userID){
        String api_key = BotManager.getInstance().SteamAPIKey;

        try{
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(BotManager.getRemoteContent("http://api.steampowered.com/ISteamUser/GetPlayerSummaries/v0002/?steamids=" + userID + "&key=" + api_key));

            JSONObject jsonObject = (JSONObject) obj;

            JSONObject response = (JSONObject)(jsonObject.get("response"));
            JSONArray players = (JSONArray) response.get("players");

            if(players.size() > 0){
                JSONObject index0 = (JSONObject) players.get(0);
                String profileurl = (String) index0.get("profileurl");
                String gameextrainfo = (String) index0.get("gameextrainfo");
                String gameserverip = (String) index0.get("gameserverip");

                return "Profile: " + JSONUtil.shortenURL(profileurl) + ( gameextrainfo != null ? ", Game: " + gameextrainfo : "") + ( gameserverip != null ? ", Server: " + gameserverip : "");
            }else{
                return "Error querying API";
            }
        }catch (Exception ex){
            ex.printStackTrace();
            return "Error querying API";
        }
    }

    public static String shortenURL(String url){
        String login = BotManager.getInstance().bitlyLogin;
        String api_key = BotManager.getInstance().bitlyAPIKey;

        try{
            String encodedURL = "";
            try {
                encodedURL = URLEncoder.encode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

            JSONParser parser = new JSONParser();
            Object obj = parser.parse(BotManager.getRemoteContent("http://api.bitly.com/v3/shorten?login=" + login + "&apiKey=" + api_key + "&longUrl=" + encodedURL + "&format=json"));

            JSONObject jsonObject = (JSONObject) obj;
            String status_txt = (String) jsonObject.get("status_txt");

            if(status_txt.equalsIgnoreCase("OK")){
                JSONObject data = (JSONObject) jsonObject.get("data");
                String shortenedUrl = (String) data.get("url");
                return shortenedUrl;
            }else{
                return url;
            }
        }catch (Exception ex){
            ex.printStackTrace();
            return url;
        }



    }

}
