package net.bashtech.geobot;

import java.util.ArrayList;
import java.util.Random;

public class Raffle {
	private ArrayList<String> entries;
	private boolean enabled;
	
	public Raffle(){
		entries = new ArrayList<String>();
		enabled = false;
	}
	
	public void enter(String name){
		if(enabled){
			if(!checkIfEntered(name)){
				entries.add(name.toLowerCase());
				System.out.println("DEBUG: Entry accepted for " + name.toLowerCase());
			}else{
				System.out.println("DEBUG: Entry rejected. " + name.toLowerCase() + " is already entered.");
			}
		}else{
			System.out.println("DEBUG: Entry rejected. Raffle not running.");
		}

	}
	
	public void setEnabled(boolean option){
		enabled = option;
	}
	
	public String pickWinner(){
		if(entries.size() < 1)
			return "No users entered";
		
		Random generator = new Random();
		int randomIndex = generator.nextInt(entries.size());
		
		return entries.get(randomIndex);
	}
	
	public void reset(){
		entries.clear();
	}
	
	private boolean checkIfEntered(String name){
		for(String entry : entries){
			if(entry.equalsIgnoreCase(name)){
				return true;
			}
		}
		
		return false;
	}
}
