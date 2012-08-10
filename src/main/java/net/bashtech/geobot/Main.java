package net.bashtech.geobot;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Timer;

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
		String propertiesFile = "global.properties";
		if(args.length > 0){
			propertiesFile = args[0];
		}
		
		BotManager bm = new BotManager(propertiesFile);
	}

}
