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
		
		//System.out.println("Reconnect timer running...");

		
		for (Map.Entry<String, Channel> entry : channelList.entrySet())
		{	
			Bot b = entry.getValue().getBot();
			if(!b.isConnected() || (b.checkStalePing() && BotManager.getInstance().monitorPings)){
				try {
					System.out.println("INFO: Attempting to reconnet to " + b.getServer() + "...\n");
					b.disconnect();
					Thread.currentThread().sleep(20000);
					if(!b.isConnected())
						b.reconnect();
				} catch (NickAlreadyInUseException e) {
					System.out.println("Nickname already in use - " + b.getNick() + " " + b.getServer());
				} catch (IOException e) {
					System.out.println("Unable to connect to server - " + b.getNick() + " " + b.getServer());
				} catch (IrcException e) {
					System.out.println("Error connecting to server - " + b.getNick() + " " + b.getServer());
				} catch (InterruptedException e) {
					System.out.println("Threading execption occured - " + b.getNick() + " " + b.getServer());
				}
			}else{
				//System.out.println("INFO: " + b.getServer() + " is connected.");
			}
		}
		
	}

}
