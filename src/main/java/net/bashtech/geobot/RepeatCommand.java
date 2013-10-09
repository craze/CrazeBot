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

import java.util.Timer;
import java.util.TimerTask;

public class RepeatCommand {
	String key;
	int delay;
	Timer timer;
	long lastMessageCount;
	int messageDifference;
    boolean active;
    TimerTask task;
    String channel;

    public RepeatCommand(String _channel, String _key, int _delay, int _messageDifference, boolean _active){
		key = _key;
		delay = _delay; //In seconds
		lastMessageCount = 0;
		messageDifference = _messageDifference;
        active = _active;
        channel = _channel;
		
		timer = new Timer();

        if(active)
		    timer.scheduleAtFixedRate( new RepeatCommandTask(channel, key, messageDifference), (delay * 1000), (delay * 1000));

	}

    public void setStatus(boolean status){
        if(status == active)
            return;
        else if(status == true){
            System.out.println("creating scheduler");
            timer = null;
            timer = new Timer();
            timer.scheduleAtFixedRate( new RepeatCommandTask(channel, key, messageDifference), (delay * 1000), (delay * 1000));
            active = true;
        }else if(status == false){
           timer.cancel();
            active = false;
        }
    }
	
	private class RepeatCommandTask extends TimerTask{
		private String key;
		private String channel;
		private int messageDifference;
		
		public RepeatCommandTask(String _channel, String _key, int _messageDifference){
			key = _key;
			channel = _channel;
			messageDifference = _messageDifference;
		}
		
        public void run() {
        	Channel channelInfo = BotManager.getInstance().getChannel(channel);
        	if(channelInfo.messageCount - RepeatCommand.this.lastMessageCount >= messageDifference){
        		String command = channelInfo.getCommand(key);
            	SenderBotBalancer.getInstance().sendMessage(channel, command);
            	
            	if(key.equalsIgnoreCase("!commercial"))
            		channelInfo.runCommercial();
        	}else{
        		//System.out.println("DEBUG: No messages received since last send - " + key);
        	}
        	
        	lastMessageCount = channelInfo.messageCount;
        }
	}
}
