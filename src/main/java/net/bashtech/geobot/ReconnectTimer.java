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
	private Map<String,GeoBot> botList;
	
	public ReconnectTimer(Map<String, GeoBot> botList2)
	{
		botList = botList2;
	}

	@Override
	public void run() {
		
		//System.out.println("Reconnect timer running...");

		
		for (Map.Entry<String, GeoBot> entry : botList.entrySet())
		{	
			GeoBot b = entry.getValue();
			if(!b.isConnected()){
				try {
					System.out.println("Attempting to reconnet to " + b.getServer() + "...\n");
					b.reconnect();
				} catch (NickAlreadyInUseException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				} catch (IrcException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		
	}

}
