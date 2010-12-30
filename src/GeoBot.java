import org.jibble.pircbot.*;

public class GeoBot extends PircBot {
	
	
	public GeoBot(){
		this.setName("GiantZombie");
	}
	
	public void onMessage(String channel, String sender, String login, String hostname, String message) {
			// !time - All
			if (message.equalsIgnoreCase("!time")) {
					String time = new java.util.Date().toString();
					sendMessage(channel, sender + ": The time is now " + time);
			}
			
			// !clear - Ops
			if(message.equalsIgnoreCase("!clear") && matchUser(sender, channel).isOp()){
				this.sendMessage(channel, "/clear");
				
			}
			
			if(countCapitals(message) > 3 && !matchUser(sender, channel).isOp()){
				this.kick(channel, sender);
				this.unBan(channel,sender + "!" + sender + "@*.*");
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

}
