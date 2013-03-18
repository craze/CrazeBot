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


public class MessageReplaceParser {

    public static String parseMessage(String channel, String message){
        Channel ci = BotManager.getInstance().getChannel(channel);

        if(message.contains("(_GAME_)"))
            message = message.replace("(_GAME_)", JSONUtil.krakenGame(channel.substring(1)));
        if(message.contains("(_STATUS_)"))
            message = message.replace("(_STATUS_)", JSONUtil.krakenStatus(channel.substring(1)));
        if(message.contains("(_VIEWERS_)"))
            message = message.replace("(_VIEWERS_)", JSONUtil.krakenViewers(channel.substring(1)));
        if(message.contains("(_CHATTERS_)"))
            message = message.replace("(_CHATTERS_)", "" + ReceiverBot.getInstance().getUsers(channel).length);
        if(message.contains("(_SONG_)"))
            message = message.replace("(_SONG_)", JSONUtil.lastFM(ci.getLastfm()));
        if(message.contains("(_SONG_)"))
            message = message.replace("(_SONG_)", JSONUtil.lastFM(ci.getLastfm()));
        if(message.contains("(_STEAM_PROFILE_)"))
            message = message.replace("(_STEAM_PROFILE_)", JSONUtil.steam(ci.getSteam(),"profile"));
        if(message.contains("(_STEAM_GAME_)"))
            message = message.replace("(_STEAM_GAME_)", JSONUtil.steam(ci.getSteam(),"game"));
        if(message.contains("(_STEAM_SERVER_)"))
            message = message.replace("(_STEAM_SERVER_)", JSONUtil.steam(ci.getSteam(),"server"));
        if(message.contains("(_STEAM_STORE_)"))
            message = message.replace("(_STEAM_STORE_)", JSONUtil.steam(ci.getSteam(),"store"));

        return message;
    }
}
