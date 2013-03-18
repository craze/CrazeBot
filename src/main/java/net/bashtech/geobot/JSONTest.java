package net.bashtech.geobot;


import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;


public class JSONTest {
    public static void main(String[] args) {

        JSONParser parser = new JSONParser();

        try {
              JSONTest.krakenStreams();


//            long age = (Long) jsonObject.get("age");
//            System.out.println(age);
//
//            // loop array
//            JSONArray msg = (JSONArray) jsonObject.get("messages");
//            Iterator<String> iterator = msg.iterator();
//            while (iterator.hasNext()) {
//                System.out.println(iterator.next());
//            }

        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public static void krakenStreams() throws Exception{
        JSONParser parser = new JSONParser();
        Object obj = parser.parse(BotManager.getRemoteContent("https://api.twitch.tv/kraken/streams/mlglol"));

        JSONObject jsonObject = (JSONObject) obj;

        JSONObject stream = (JSONObject)(jsonObject.get("stream"));
        Long viewers = (Long)stream.get("viewers");
        System.out.println("Viewers: " + viewers);
    }



}