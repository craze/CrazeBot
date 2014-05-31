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

import java.util.ArrayList;
import java.util.Random;

public class Raffle {
    private ArrayList<String> entries;
    private boolean enabled;

    public Raffle() {
        entries = new ArrayList<String>();
        enabled = false;
    }

    public void enter(String name) {
        if (enabled) {
            if (!checkIfEntered(name)) {
                entries.add(name.toLowerCase());
                System.out.println("DEBUG: Entry accepted for " + name.toLowerCase());
            } else {
                System.out.println("DEBUG: Entry rejected. " + name.toLowerCase() + " is already entered.");
            }
        } else {
            System.out.println("DEBUG: Entry rejected. Raffle not running.");
        }

    }

    public void setEnabled(boolean option) {
        enabled = option;
    }

    public String pickWinner() {
        if (entries.size() < 1)
            return "No users entered";

        Random generator = new Random();
        int randomIndex = generator.nextInt(entries.size());

        return entries.get(randomIndex);
    }

    public void reset() {
        entries.clear();
    }

    public int count() {
        return entries.size();
    }

    private boolean checkIfEntered(String name) {
        for (String entry : entries) {
            if (entry.equalsIgnoreCase(name)) {
                return true;
            }
        }

        return false;
    }
}
