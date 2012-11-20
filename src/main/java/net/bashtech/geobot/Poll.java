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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Poll {
	
	private Map<String,Integer> votes;
	private Set<String> voters;
	
	private boolean isOpen = false;
	
	public Poll(String[] options){
		votes = new HashMap<String,Integer>();
		voters = new HashSet<String>();
		
		for(int c=0; c<options.length;c++){
			votes.put(options[c].toLowerCase(), 0);
			System.out.println("DEBUG: added " + options[c].toLowerCase());
		}
				
	}
	
	public void vote(String nickname, String option){
		if(voters.contains(nickname.toLowerCase())){
			System.out.println("DEBUG: already voted.");
			return;
		}else{
			voters.add(nickname.toLowerCase());
		}
		
		option = option.toLowerCase();
		if(votes.containsKey(option)){
			votes.put(option, votes.get(option) + 1);
			System.out.println("DEBUG: Vote registered.");
		}
	}
	
	public boolean getStatus(){
		return isOpen;
	}
	
	public void setStatus(boolean status){
		isOpen = status;
	}
	
	private Map.Entry<String, Integer> getMostVotes(){
		
		Map.Entry<String, Integer> most = null;
		
		for(Map.Entry<String, Integer> entry: votes.entrySet()){
			if(most == null){
				most = entry;
			}else{
				if(entry.getValue().intValue() > most.getValue().intValue()){
					most = entry;
				}
			}
		}
		
		return most;
		
	}
	
	public String[] getResults(){
		String[] results = new String[votes.size() + 4];
		results[0] = "> Poll Results";
		results[1] = "> -------------";
		int c = 2;
		for(Map.Entry<String, Integer> entry: votes.entrySet()){
			results[c] = "> '" + entry.getKey() + "' - " + entry.getValue();
			c++;
		}
		Map.Entry<String, Integer> most = this.getMostVotes();
		results[results.length-2] = "> -------------";
		results[results.length-1] = "> Winner: '" + most.getKey() + "' - " + most.getValue();
		return  results;
	}

}
