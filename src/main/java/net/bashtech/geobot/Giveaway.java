package net.bashtech.geobot;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

public class Giveaway {
	// Key: number, Value: name
	private ArrayList<GiveawayEntry> entries;
	private Set<String> names;
	
	private int secretNumber;
	
	private boolean isOpen = false;
	
	public Giveaway(int max){
		Random rand = new Random();
		entries = new ArrayList<GiveawayEntry>();
		names = new HashSet<String>();
		secretNumber = rand.nextInt(max) + 1;
	}
	
	public boolean getStatus(){
		return isOpen;
	}
	
	public void setStatus(boolean status){
		isOpen = status;
	}
	
	public void submitEntry(String nickname, String entry){
		//Check if is numeric
		int entryInt = 0;
		if(!isInteger(entry)){
			System.out.println("DEBUG: Not integer.");
			return;
		}else{
			try{
				entryInt = Integer.parseInt(entry);
			}catch(NumberFormatException nfe){
				return;
			}
		}
		
		if(names.contains(nickname.toLowerCase())){
			return;
		}else{
			names.add(nickname.toLowerCase());
		}
		
		entries.add(new GiveawayEntry(nickname,entryInt));
	}
	
	public static boolean isInteger(String str) {
        if (str == null) {
                return false;
        }
        int length = str.length();
        if (length == 0) {
                return false;
        }
        int i = 0;
        if (str.charAt(0) == '-') {
                if (length == 1) {
                        return false;
                }
                i = 1;
        }
        for (; i < length; i++) {
                char c = str.charAt(i);
                if (c <= '/' || c >= ':') {
                        return false;
                }
        }
        return true;
	}
	
	private int findClosetNumber(){
		
		return -1;
					
	}
	
	public String[] getResults(){
		return null;
//		String[] results = new String[votes.size() + 4];
//		results[0] = "> Poll Results";
//		results[1] = "> -------------";
//		int c = 2;
//		for(Map.Entry<String, Integer> entry: votes.entrySet()){
//			results[c] = "> '" + entry.getKey() + "' - " + entry.getValue();
//			c++;
//		}
//		Map.Entry<String, Integer> most = this.getMostVotes();
//		results[results.length-2] = "> -------------";
//		results[results.length-1] = "> Winner: '" + most.getKey() + "' - " + most.getValue();
//		return  results;		
	}
	
	private class GiveawayEntry{
		String nickname;
		int entry;
		public GiveawayEntry(String _nickname, int _entry){
			nickname = _nickname;
			entry = _entry;
		}
		
		public int getDistance(int value){
			return Math.abs(entry - value);
		}
	}

}
