package net.bashtech.geobot.modules;

public interface BotModule {
	
	public void onMessage(String channel, String sender, String login, String hostname, String message);
	
	public void onSelfMessage(String channel, String sender, String message);
	
	public void onJoin(String channel, String sender, String login, String hostname);
	
	public void onPart(String channel, String sender, String login, String hostname);

}
