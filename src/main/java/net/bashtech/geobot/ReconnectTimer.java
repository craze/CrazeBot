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

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;

public class ReconnectTimer extends TimerTask{
	private Map<String,Channel> channelList;
	
	public ReconnectTimer(Map<String, Channel> channelList2)
	{
		channelList = channelList2;
	}

	@Override
	public void run() {
		
		ReceiverBot rb = BotManager.getInstance().receiverBot;
		if(!rb.isConnected() || (rb.checkStalePing() && BotManager.getInstance().monitorPings)){
			try {
				System.out.println("INFO: Attempting to reconnect receiver");
				rb.disconnect();
				Thread.currentThread().sleep(20000);
				if(!rb.isConnected())
					rb.reconnect();
			} catch (NickAlreadyInUseException e) {
				System.out.println("Nickname already in use - " + rb.getNick() + " " + rb.getServer());
			} catch (IOException e) {
				System.out.println("Unable to connect to server - " + rb.getNick() + " " + rb.getServer());
			} catch (IrcException e) {
				System.out.println("Error connecting to server - " + rb.getNick() + " " + rb.getServer());
			} catch (InterruptedException e) {
				System.out.println("Threading execption occured - " + rb.getNick() + " " + rb.getServer());
			}
		}

	}

}
