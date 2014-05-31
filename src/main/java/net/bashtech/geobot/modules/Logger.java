package net.bashtech.geobot.modules;

import net.bashtech.geobot.Channel;
import org.jibble.pircbot.Colors;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Logger implements BotModule {

    //Make these customizable later.


    String outDir = "logs";

    // ****************************

    private static final Pattern urlPattern = Pattern.compile("(?i:\\b((http|https|ftp|irc)://[^\\s]+))");
    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
    private static final SimpleDateFormat TIME_FORMAT = new SimpleDateFormat("H:mm");

    public static final String GREEN = "irc-green";
    public static final String BLACK = "irc-black";
    public static final String BROWN = "irc-brown";
    public static final String NAVY = "irc-navy";
    public static final String BRICK = "irc-brick";
    public static final String RED = "irc-red";

    public Logger() {
        System.out.println("MODULE: Logger loaded.");
        File logDir = new File(outDir);
        if (!logDir.exists()) {
            logDir.mkdir();
        }
    }

    public void append(String color, String line, String channel) {

        line = Colors.removeFormattingAndColors(line);

        line = line.replaceAll("&", "&amp;");
        line = line.replaceAll("<", "&lt;");
        line = line.replaceAll(">", "&gt;");

        Matcher matcher = urlPattern.matcher(line);
        line = matcher.replaceAll("<a href=\"$1\" rel=\"nofollow\">$1</a>");


        try {
            Date now = new Date();
            String date = DATE_FORMAT.format(now);
            String time = TIME_FORMAT.format(now);
            File chanenlDir = new File(outDir + "/" + (channel.substring(1, channel.length())));
            if (!chanenlDir.exists()) {
                chanenlDir.mkdir();
            }
            File file = new File(chanenlDir.toString(), date + ".log");
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file, true), "UTF-8"));
            String entry = "<span class=\"irc-date\">[" + time + "]</span> <span class=\"" + color + "\">" + line + "</span><br />";
            writer.write(entry);
            writer.newLine();
            writer.flush();
            writer.close();
        } catch (IOException e) {
            System.out.println("Could not write to log: " + e);
        }
    }

    @Override
    public void onMessage(Channel channel, String sender, String login,
                          String hostname, String message) {
        if (channel.logChat)
            append(BLACK, "<" + sender + "> " + message, channel.getChannel());
    }

    @Override
    public void onJoin(Channel channel, String sender, String login,
                       String hostname) {
        //append(GREEN, "* " + sender + " (" + login + "@" + hostname + ") has joined " + channel,channel);
    }

    @Override
    public void onPart(Channel channel, String sender, String login,
                       String hostname) {
        //append(GREEN, "* " + sender + " (" + login + "@" + hostname + ") has left " + channel,channel);

    }

    @Override
    public void onSelfMessage(Channel channel, String sender, String message) {
        if (channel.logChat)
            append(BLACK, "<" + sender + "> " + message, channel.getChannel());

    }


}
