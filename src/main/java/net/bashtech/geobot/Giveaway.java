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

import java.util.*;

public class Giveaway {
    // Key: number, Value: name
    private ArrayList<GiveawayEntry> entries;
    private Set<String> names;

    private int secretNumber;

    private boolean isOpen = false;

    private int maxInt;

    private Timer gaTimer;

    public Giveaway(String max) {
        Random rand = new Random();
        entries = new ArrayList<GiveawayEntry>();
        names = new HashSet<String>();
        if (isInteger(max))
            maxInt = Integer.parseInt(max);
        else
            maxInt = 100;

        secretNumber = rand.nextInt(maxInt) + 1;
        System.out.println("DEBUG: Secret number - " + secretNumber);
    }

    public boolean getStatus() {
        return isOpen;
    }

    public void setStatus(boolean status) {
        isOpen = status;
    }

    public int getMax() {
        return maxInt;
    }

    public Timer getTimer() {
        return gaTimer;
    }

    public void setTimer(Timer t) {
        gaTimer = t;
    }

    public void submitEntry(String nickname, String entry) {
        //Check if is numeric
        int entryInt = 0;
        if (!isInteger(entry)) {
            System.out.println("DEBUG: Not integer.");
            return;
        } else {
            try {
                entryInt = Integer.parseInt(entry);
            } catch (NumberFormatException nfe) {
                return;
            }
        }

        if (entryInt > maxInt || entryInt < 1) {
            System.out.println("DEBUG: Out of range.");
            return;
        }

        if (names.contains(nickname.toLowerCase())) {
            System.out.println("DEBUG: Already entered.");
            return;
        } else {
            names.add(nickname.toLowerCase());
        }

        System.out.println("DEBUG: Entry successfull.");
        entries.add(new GiveawayEntry(nickname, entryInt));
    }

    public boolean isInteger(String str) {
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


    public ArrayList<GiveawayEntry> getWinners() {
        ArrayList<GiveawayEntry> winners = new ArrayList<GiveawayEntry>();


        int closest = Integer.MAX_VALUE;
        for (GiveawayEntry e : entries) {
            if (e.getDistance(secretNumber) < closest) {
                closest = e.getDistance(secretNumber);
            }
        }

        for (GiveawayEntry e : entries) {
            if (e.getDistance(secretNumber) == closest) {
                winners.add(e);
            }
        }

        return winners;

    }

    public String[] getResults() {
        ArrayList<GiveawayEntry> winners = getWinners();

        String[] results = new String[winners.size() + 4];
        if (winners.size() > 1)
            results[0] = "> TIE";
        else
            results[0] = "> Winner";
        results[1] = "> -------------";
        results[2] = "> Secret number - " + secretNumber;
        results[3] = "> -------------";
        int c = 4;
        for (GiveawayEntry e : winners) {
            results[c] = "> " + e.nickname + " - " + e.entry;
            c++;
        }
        return results;
    }

    public String getResultsString() {
        ArrayList<GiveawayEntry> winners = getWinners();

        String results = "";
        if (winners.size() > 1)
            results += "Tie";
        else
            results += "Winner";
        results += " ~ Secret number - " + secretNumber + " ~ ";
        int c = 4;
        for (GiveawayEntry e : winners) {
            results += " " + e.nickname + " - " + e.entry + ", ";
            c++;
        }
        return results;
    }

    private class GiveawayEntry {
        String nickname;
        int entry;

        public GiveawayEntry(String _nickname, int _entry) {
            nickname = _nickname;
            entry = _entry;
        }

        public int getDistance(int value) {
            return Math.abs(entry - value);
        }
    }

}
