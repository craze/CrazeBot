package net.bashtech.geobot;

import java.util.ArrayList;

public class SenderBotBalancer {
	static SenderBotBalancer _instance;
	
	private int instanceNumber;
	private ArrayList<SenderBot> instances;
	private int position;
	
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
		instances.get(position).sendMessage(channel, message);
		
		if(position==9)
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

}
