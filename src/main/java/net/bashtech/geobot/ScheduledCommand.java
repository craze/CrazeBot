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

import it.sauronsoftware.cron4j.Scheduler;

public class ScheduledCommand {
    String key;
    String pattern;
    Scheduler s;
    long lastMessageCount;
    int messageDifference;
    boolean active;

    public ScheduledCommand(String _channel, String _key, String _pattern, int _messageDifference, boolean _active){
        key = _key;
        pattern = _pattern; //In seconds
        lastMessageCount = 0;
        messageDifference = _messageDifference;
        active = _active;

        s = new Scheduler();
        System.out.println("Scheduling " + key + " on " + pattern);
        s.schedule(pattern, new ScheduledCommandTask(_channel, key, messageDifference));
        if(active)
            s.start();
    }

    public void setStatus(boolean status){
        if(status == active)
            return;
        else if(status == true){
            s.start();
            active = true;
        }else if(status == false){
            s.stop();
            active = false;
        }
    }

    private class ScheduledCommandTask implements Runnable{
        private String key;
        private String channel;
        private int messageDifference;

        public ScheduledCommandTask(String _channel, String _key, int _messageDifference){
            key = _key;
            channel = _channel;
            messageDifference = _messageDifference;
        }

        public void run() {
            Channel channelInfo = BotManager.getInstance().getChannel(channel);
            if(channelInfo.messageCount - ScheduledCommand.this.lastMessageCount >= messageDifference){
                String command = channelInfo.getCommand(key);
                ReceiverBot.getInstance().sendMessage(channel, command);

                if(key.equalsIgnoreCase("!commercial"))
                    channelInfo.runCommercial();
            }else{
                //System.out.println("DEBUG: No messages received since last send - " + key);
            }

            lastMessageCount = channelInfo.messageCount;
        }
    }
}
