package net.bashtech.geobot;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Scanner;


public class JSONTest {
    public static void main(String[] args) {


/*        System.out.println("tal: " + JSONTest.krakenIsLive("talutha"));
        System.out.println("bwana: " + JSONTest.krakenIsLive("bwana"));
        System.out.println("lirik: " + JSONTest.krakenIsLive("lirik"));
        System.out.println("dansgaming: " + JSONTest.krakenIsLive("dansgaming"));
        System.out.println("giantwaffle: " + JSONTest.krakenIsLive("giantwaffle"));

        System.out.println("bgeorge" + JSONUtil.krakenChannelExist("bgeorge"));
        System.out.println("bwana" + JSONUtil.krakenChannelExist("bwana"));
        System.out.println("dylanlive" + JSONUtil.krakenChannelExist("dylanlive"));
        System.out.println("communitychat" + JSONUtil.krakenChannelExist("communitychat"));*/

    }


    public static String putRemoteData(String urlString, String postData){
        String krakenOAuthToken = "";

        URL url;
        HttpURLConnection conn;

        try{
            url=new URL(urlString);

            conn=(HttpURLConnection)url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("PUT");


            conn.setFixedLengthStreamingMode(postData.getBytes().length);
            conn.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
            conn.setRequestProperty("Authorization", "OAuth " + krakenOAuthToken);

            PrintWriter out = new PrintWriter(conn.getOutputStream());
            out.print(postData);
            out.close();

            String response= "";

            Scanner inStream = new Scanner(conn.getInputStream());

            while(inStream.hasNextLine())
                response+=(inStream.nextLine());

            System.out.println(conn.getResponseCode());
            System.out.println(response);
            return response;

        }
        catch(MalformedURLException ex){
            ex.printStackTrace();
        }
        catch(IOException ex){
            ex.printStackTrace();
        }

        return "";
    }

    public static boolean krakenIsLive(String channel){
        try{
            JSONParser parser = new JSONParser();
            Object obj = parser.parse(BotManager.getRemoteContent("https://api.twitch.tv/kraken/streams/" + channel));

            JSONObject jsonObject = (JSONObject) obj;

            JSONObject stream = (JSONObject)(jsonObject.get("stream"));

            if(stream != null)
                return true;
            else
                return false;
        }catch (Exception ex){
            ex.printStackTrace();
            return false;
        }

    }

}