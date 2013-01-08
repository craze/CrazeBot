package net.bashtech.geobot;

import java.util.Timer;
import java.util.TimerTask;

public class RepeatCommand {
	String key;
	int delay;
	Timer timer;
	
	public RepeatCommand(String _channel, String _key, int _delay){
		key = _key;
		delay = _delay; //In seconds
		
		timer = new Timer();
		int timerDelay = delay * 1000; //In milliseconds
		
		timer.scheduleAtFixedRate(new RepeatCommandTask(_channel, key), timerDelay, timerDelay);

	}
	
	private class RepeatCommandTask extends TimerTask{
		private String key;
		private String channel;
		
		public RepeatCommandTask(String _channel, String _key){
			key = _key;
			channel = _channel;
		}
        public void run() {
        	Channel channelInfo = BotManager.getInstance().getChannel(channel);
        	String command = channelInfo.getCommand(key);
        	SenderBotBalancer.getInstance().sendMessage(channel, channelInfo.getBullet() + " " + command);
        }
	}
}
