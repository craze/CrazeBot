/*
 * Copyright 2012 Andrew Bashore
 * This file is part of GeoBot.
 * 
 * GeoBot is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 * 
 * GeoBot is distributed in the hope that it will be useful
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with GeoBot.  If not, see <http://www.gnu.org/licenses/>.
*/

package net.bashtech.geobot;
import java.io.IOException;
import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;


public class Main {

	public static void main(String[] args) throws NickAlreadyInUseException, IOException, IrcException {
		String propertiesFile = "global.properties";
		if(args.length > 0){
			propertiesFile = args[0];
		}
		
		BotManager bm = new BotManager(propertiesFile);
	}

}
