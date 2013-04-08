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

import java.util.ArrayList;

public class SenderBotBalancer {
	static SenderBotBalancer _instance;
	
	private int instanceNumber;
	private ArrayList<SenderBot> instances;
	private int position;
    private char bullet[] = {'>','+', '-', '~'};
    private int bulletPos = 0;

    public SenderBotBalancer(){
		setInstance(this);
		instanceNumber = 10;
		position = 0;
		instances = new ArrayList<SenderBot>();
	}
	
	public void setInstanceNumber(int instances){
		instanceNumber = instances;
	}
	
	public void spinUp(){
		for(int i=0; i<instanceNumber; i++){
			instances.add(new SenderBot(BotManager.getInstance().server, BotManager.getInstance().port));
		}
	}
	
	public synchronized void sendMessage(String channel, String message){
		System.out.println("DEBUG: Sending message to instance - " + position);

        message = MessageReplaceParser.parseMessage(channel, message);
        message = getBullet() + " " + message;

		instances.get(position).sendMessage(channel, message);
		
		if(position > (instances.size() - 2))
			position = 0;
		else
			position++;
	}
	
	public ArrayList<SenderBot> getBotInstances(){
		return instances;
	}
	
	public static void setInstance(SenderBotBalancer senderBot){
		if(_instance == null){
			_instance = senderBot;
		}
	}
	
	public static SenderBotBalancer getInstance(){
		return _instance;
	}

    public char getBullet(){
        if(bulletPos == bullet.length)
            bulletPos = 0;

        char rt = bullet[bulletPos];
        bulletPos++;

        return rt;

    }

}
