package net.bashtech.geobot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;


public class Main {
	
	/**
	 * @param args
	 * @throws IrcException 
	 * @throws IOException 
	 * @throws NickAlreadyInUseException 
	 */

	public static void main(String[] args) throws NickAlreadyInUseException, IOException, IrcException {
		System.out.println("INFO: Staring up.");
		ArrayList<GeoBot> botList = new ArrayList<GeoBot>();
		
		
		//Add global channel
		System.out.println("INFO: Adding global channel...");
		GlobalChannel globalChannel = new GlobalChannel(botList);
		GeoBot globalBot = new GeoBot(globalChannel, true);
		
		//Add other channels
		System.out.println("INFO: Adding other channels...");
		for(Channel c:globalChannel.getChannelList()) {
			botList.add(new GeoBot(globalChannel, c));
		}
		
		
		//Start reconnect timer
		
//		Timer reconnectTimer = new Timer();
//		reconnectTimer.scheduleAtFixedRate(new ReconnectTimer(globalBot, botList), 30 * 1000, 30 * 1000);
//		System.out.println("Reconnect timer scheduled.");
	}

}
