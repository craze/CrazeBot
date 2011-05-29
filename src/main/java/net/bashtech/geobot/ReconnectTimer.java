package net.bashtech.geobot;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;

public class ReconnectTimer extends TimerTask{
	private ArrayList<GeoBot> botList;
	
	public ReconnectTimer(ArrayList<GeoBot> bots)
	{
		botList = bots;
	}

	@Override
	public void run() {
		
		//System.out.println("Reconnect timer running...");

		
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
