package net.bashtech.geobot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;

public class ReconnectTimer extends TimerTask{
	GeoBot globalBot;
	private ArrayList<GeoBot> botList;
	
	public ReconnectTimer(GeoBot global, ArrayList<GeoBot> bots)
	{
		globalBot = global;
		botList = bots;
	}

	@Override
	public void run() {
		
		//System.out.println("Reconnect timer running...");
		
		if(!globalBot.isConnected()){
			try {
				System.out.println("Attempting to reconnet globalBot...");
				globalBot.reconnect();
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
		
		for(GeoBot b:botList){
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
