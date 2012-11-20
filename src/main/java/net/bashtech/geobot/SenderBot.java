package net.bashtech.geobot;

import java.io.IOException;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.jibble.pircbot.PircBot;

public class SenderBot extends PircBot {
	private int lastPing = -1;
	
	public SenderBot(String server, int port){
		
		this.setName(BotManager.getInstance().nick);
		this.setLogin("GeoBotSender");
		
		this.setVerbose(BotManager.getInstance().verboseLogging);
		try {
			this.connect(server, port, BotManager.getInstance().password);
		} catch (NickAlreadyInUseException e) {
			System.out.println("ERROR: Nickname already in use - " + this.getNick() + " " + this.getServer());
		} catch (IOException e) {
			System.out.println("ERROR: Unable to connect to server - " + this.getNick() + " " + this.getServer());
		} catch (IrcException e) {
			System.out.println("ERROR: Error connecting to server - " + this.getNick() + " " + this.getServer());
		}
	}
	
	@Override
	public void onDisconnect(){		 
		lastPing = -1;
		try {
			System.out.println("INFO: Internal reconnection: " + this.getServer());
			String[] channels = this.getChannels();
			this.reconnect();
			for(int i=0;i<channels.length;i++){
				this.joinChannel(channels[i]);
			}
		} catch (NickAlreadyInUseException e) {
			System.out.println("ERROR: Nickname already in use - " + this.getNick() + " " + this.getServer());
		} catch (IOException e) {
			System.out.println("ERROR: Unable to connect to server - " + this.getNick() + " " + this.getServer());
		} catch (IrcException e) {
			System.out.println("ERROR: Error connecting to server - " + this.getNick() + " " + this.getServer());
		}
		
	}
	
	@Override
    public void onServerPing(String response) {
		super.onServerPing(response);
		lastPing = (int) (System.currentTimeMillis()/1000);
	}

	public boolean checkStalePing(){
		if(lastPing == -1)
			return false;
		
		int difference = ((int) (System.currentTimeMillis()/1000)) - lastPing;
		
		if(difference > BotManager.getInstance().pingInterval){
			System.out.println("DEBUG: Ping is stale. Last ping= " + lastPing + " Difference= " + difference);
			lastPing = -1;
			return true;
		}
		
		return false;
	}


}
