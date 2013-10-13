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

import java.util.HashSet;
import java.util.Set;


public class Tester {

    public static void main(String[] args) {
        Set<String> staff = new HashSet<String>();
        Set<String> admins = new HashSet<String>();
        Set<String> mods = new HashSet<String>();

        Long chatter_count = JSONUtil.updateTMIUserList("twitch", staff, admins, mods);

        for (String user : staff)
            System.out.println(user);
        for (String user : admins)
            System.out.println(user);
        for (String user : mods)
            System.out.println(user);
    }

}
