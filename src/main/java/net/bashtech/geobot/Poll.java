package net.bashtech.geobot;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class Poll {
	
	private Map<String,Integer> votes;
	private Set<String> voters;
	
	public Poll(String[] options){
		votes = new HashMap<String,Integer>();
		voters = new HashSet<String>();
		
		for(int c=0; c<options.length;c++){
			votes.put(options[c].toLowerCase(), 0);
		}
		
	}
	
	public void vote(String nickname, String option){
		if(voters.contains(nickname.toLowerCase())){
			return;
		}else{
			voters.add(nickname.toLowerCase());
		}
		
		option = option.toLowerCase();
		if(votes.containsKey(option)){
			votes.put(option, votes.get(option) + 1);
		}
	}

}
