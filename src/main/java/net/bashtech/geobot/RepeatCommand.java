package net.bashtech.geobot;

import java.util.Timer;
import java.util.TimerTask;

public class RepeatCommand {
	String key;
	int delay;
	Timer timer;
	long lastMessageCount;
	int messageDifference;
	
	public RepeatCommand(String _channel, String _key, int _delay, int _messageDifference){
		key = _key;
		delay = _delay; //In seconds
		lastMessageCount = 0;
		messageDifference = _messageDifference;
		
		timer = new Timer();
		int timerDelay = delay * 1000; //In milliseconds
		
		timer.scheduleAtFixedRate(new RepeatCommandTask(_channel, key, messageDifference), timerDelay, timerDelay);
	}
	
	private class RepeatCommandTask extends TimerTask{
		private String key;
		private String channel;
		private int messageDifference;
		
		public RepeatCommandTask(String _channel, String _key, int _messageDifference){
			key = _key;
			channel = _channel;
			messageDifference = _messageDifference;
		}
		
        public void run() {
        	Channel channelInfo = BotManager.getInstance().getChannel(channel);
        	if(channelInfo.messageCount - RepeatCommand.this.lastMessageCount >= messageDifference){
        		String command = channelInfo.getCommand(key);
            	SenderBotBalancer.getInstance().sendMessage(channel, channelInfo.getBullet() + " " + command);
        	}else{
        		System.out.println("DEBUG: No messages received since last send - " + key);
        	}
        	
        	lastMessageCount = channelInfo.messageCount;
        }
	}
}
