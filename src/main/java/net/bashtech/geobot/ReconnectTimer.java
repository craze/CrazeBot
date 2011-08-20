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
	private Map<String,Bot> botList;
	
	public ReconnectTimer(Map<String, Bot> botList2)
	{
		botList = botList2;
	}

	@Override
	public void run() {
		
		System.out.println("Reconnect timer running...");

		
		for (Map.Entry<String, Bot> entry : botList.entrySet())
		{	
			Bot b = entry.getValue();
			if(!b.isConnected() || (b.checkStalePing() && BotManager.getInstance().monitorPings)){
				try {
					System.out.println("INFO: Attempting to reconnet to " + b.getServer() + "...\n");
					b.disconnect();
					Thread.currentThread().sleep(20000);
					if(!b.isConnected())
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
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}else{
				System.out.println("INFO: " + b.getServer() + " is connected.");
			}
		}
		
	}

}
