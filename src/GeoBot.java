import java.io.IOException;

import org.jibble.pircbot.*;

public class GeoBot extends PircBot {
	private boolean isGlobalChannel = false;
	private GlobalChannel globalChannel;
	private Channel channelInfo;
	
	public GeoBot(GlobalChannel g, Channel c){
		globalChannel = g;
		channelInfo = c;
		this.setName("GiantZombie");
	}
	
	public GeoBot(GlobalChannel g, boolean gCheck){
		isGlobalChannel = gCheck;
		globalChannel = g;
		this.setName("GiantZombie");
	}
	
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
			//Ignore commands from bot
			if(sender.equalsIgnoreCase(this.getName())){
				return;
			}
		
			String[] msg = split(message);
			System.out.println("Command= "  + msg[0]);

			
			if(!isGlobalChannel){
				//Normal channel stuff
				// !time - All
				if (message.equalsIgnoreCase("!time")) {
						String time = new java.util.Date().toString();
						sendMessage(channel, sender + ": The time is now " + time);
						//return;
				}
				
				// !clear - Ops
				if(message.equalsIgnoreCase("!clear") && matchUser(sender, channel).isOp()){
					this.sendMessage(channel, "/clear");
					//return;
				}
				
				// !topic
				if(msg[0].equalsIgnoreCase("!topic")){
					if(msg.length == 1 || !matchUser(sender, channel).isOp()){
						this.sendMessage(channel, "> Topic: " + channelInfo.getTopic());
					}else if(msg.length > 1 && matchUser(sender, channel).isOp()){
						channelInfo.setTopic(message.substring(7));
						this.sendMessage(channel, "> Topic: " + channelInfo.getTopic());
					}
					//return;
				}
				
				// !set - Sets commands
 				if(msg[0].equalsIgnoreCase("!set")){
					if(msg.length == 1 && matchUser(sender, channel).isOp()){
						System.out.println("!set w/o" );
						this.sendMessage(channel, "> !set !command ^string");
					}else if(msg.length > 2 && matchUser(sender, channel).isOp()){
						System.out.println("!set w/" );
						String key = msg[1];
						String value = message.substring(message.indexOf("^") + 1, message.length() - 1);
						channelInfo.setCommand(key, value);
						this.sendMessage(channel, "> " + channelInfo.getCommand(key));
					}
					//return;
				}
				
				// Cap filter
				if(channelInfo.getFilterCaps() && countCapitals(message) > channelInfo.getFilterCapsLimit() && !matchUser(sender, channel).isOp()){
					this.kick(channel, sender);
					this.unBan(channel,sender + "!" + sender + "@*.*");
				}
				
				if(message.substring(0,1).equalsIgnoreCase("!") && !channelInfo.getCommand(message).equalsIgnoreCase("invalid")){
					sendMessage(channel, "> " + channelInfo.getCommand(message));
				}
	
			}else{
				//Global channel stuff
				if (msg[0].equalsIgnoreCase("!join") && msg.length > 1 && matchUser(sender, channel).isOp()) {
					try {
						globalChannel.addChannel(split(message)[1]);
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
				
				if (split(message)[0].equalsIgnoreCase("!leave") && split(message).length > 1 && matchUser(sender, channel).isOp()) {
						globalChannel.removeChannel(split(message)[1]);

				}
			}
			
			
			
	}
	
	public User matchUser(String nick, String channel){
		User[] userList = this.getUsers(channel);
		
		for(int i = 0; i < userList.length; i++){
			if(userList[i].equals(nick)){
				return userList[i];
			}
		}
		return null;
		
	}
	
//#################################################################################
	
	public int countCapitals(String s){
		int caps = 0;
		int max = 0;
		//boolean con = true;
		for (int i=0; i<s.length(); i++)
		{
			if (Character.isUpperCase(s.charAt(i))){
					caps++;
			}else{
				if(caps > 0 && caps > max)
						max = caps;
				caps = 0;
			}
		}
		if(caps > max)
			return caps;
		else
			return max;
	}
	
	private String[] split(String s){
		return s.split(" ");
	}
	
	public Channel getChannel(){
		return channelInfo;
	}

}
