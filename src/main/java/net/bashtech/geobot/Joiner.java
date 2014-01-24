package net.bashtech.geobot;

import java.util.Map;

public class Joiner implements Runnable {

    private Map<String, Channel> channels;

    public Joiner(Map<String, Channel> channels) {
        this.channels = channels;
    }

    public void run() {
        int count = 0;
        for (Map.Entry<String, Channel> entry : channels.entrySet()) {
            String channel = entry.getValue().getChannel();
            BotManager.getInstance().log("BM: Joining channel " + channel);
            BotManager.getInstance().receiverBot.joinChannel(channel);

            count++;

            if (count > 15) {
                count = 0;
                try {
                    Thread.sleep(5000);
                } catch (InterruptedException e) {
                    e.printStackTrace();  //To change body of catch statement use File | Settings | File Templates.
                }
            }
        }
        BotManager.getInstance().receiverBot.startJoinCheck();
    }
}
