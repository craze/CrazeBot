import java.io.IOException;

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
		GeoBot bot = new GeoBot();
		
		bot.setVerbose(true);
		
		bot.connect("bgeorge.jtvirc.com", 6667, "passwordgoeshere");
		
		bot.joinChannel("#bgeorge");

	}

}
