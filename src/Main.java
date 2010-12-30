import java.io.IOException;
import java.util.ArrayList;

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
		ArrayList<GeoBot> botList = new ArrayList<GeoBot>();
		
		
		//Add global channel
		GlobalChannel globalChannel = new GlobalChannel(botList);
		GeoBot globalBot = new GeoBot(globalChannel, true);
		globalBot.setVerbose(true);
		globalBot.connect(globalChannel.getServer(), globalChannel.getPort(), globalChannel.getPassword());
		globalBot.joinChannel(globalChannel.getChannel());
		
		//Add other channels
		for(Channel c:globalChannel.getChannelList()) {
			botList.add(new GeoBot(globalChannel, c));
			botList.get(botList.size() - 1).setVerbose(true);
			botList.get(botList.size() - 1).connect(c.getServer(), c.getPort(), globalChannel.getPassword());
			botList.get(botList.size() - 1).joinChannel(c.getChannel());
		}

	}

}
