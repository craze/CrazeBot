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
		
		ArrayList<SenderBot> sblist = SenderBotBalancer.getInstance().getBotInstances();
		for(SenderBot sb : sblist){
			if(!sb.isConnected() || (sb.checkStalePing() && BotManager.getInstance().monitorPings)){
				try {
					System.out.println("INFO: Attempting to reconnect receiver");
					sb.disconnect();
					Thread.currentThread().sleep(20000);
					if(!sb.isConnected())
						sb.reconnect();
				} catch (NickAlreadyInUseException e) {
					System.out.println("Nickname already in use - " + sb.getNick() + " " + sb.getServer());
				} catch (IOException e) {
					System.out.println("Unable to connect to server - " + sb.getNick() + " " + sb.getServer());
				} catch (IrcException e) {
					System.out.println("Error connecting to server - " + sb.getNick() + " " + sb.getServer());
				} catch (InterruptedException e) {
					System.out.println("Threading execption occured - " + sb.getNick() + " " + sb.getServer());
				}
			}
			
		}
		
	}

}
