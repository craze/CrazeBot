##  Summary

Geobot is a Java-based IRC bot based on the
[PIRC](http://www.jibble.org/pircbot.php) framework designed Twitch.


For more information about the Ackbot implementation, please see
[Ackbot](http://bashtech.net/wiki/Ackbot).

##  Features

  * Link filtering w/ permitted domains
  * Highly configurable caps filtering
  * Custom info triggers
  * Topic / Status commands 
  * Viewers and bitrate commands
  * Poll and random number giveaways 

##  Issues/Feature Requests

PM bgeorge on Twitch

##  Commands

###  General Channel

  * !topic [new topic] - All/Mods - Displays and sets the topic. If no topic is set, Twitch channel title will be returned 
  * !viewers - All - Displays the number of viewers 
  * !bitrate - All - The current broadcast bitrate 
  * !uptime - All - Stream starting time and length of time streamed 
  * !music - All - Displays what you are currently listening to on Last.fm 
  * !steam - All - Display Steam profile, current game and server 
  * !bothelp - All - Displays link bot help documents 
  * !commercial - Owner - Runs a 30 second commercial. You must contact the bot maintainer to follow your channel with the bot's account and add the bot as a channel editor first. This command is also supported in !repeat.  

### Fun 

  * !throw [object] - All - Throws object 

###  Moderation and Modes - Mods 


  * +m - Slow mode on 
  * -m - Slow mode off 
  * +f - Followers only mode on 
  * -f - Followers only mode off 
  * +s - Subscribers only mode off 
  * -s - Subscribers only mode off 
  * +b [user] - Bans a user 
  * -b [user] - Unbans a user 
  * +k [user] - Timeouts a user 
  * +p [user] - Purges a user 

  * !clear - Clears chat 
  * !modchan - Toggle command access between mod only and everyone 

###  Custom and Custom Repeat

  * !command add [name] [text] - Ops - Creates an info command (!name) 
  * !command delete [name] - Ops - Removes info command (!name) 
  
  
  * !repeat add [name] [delay in seconds] [message difference - optional] 
  * !repeat delete [name] 

The repeat command will repeat a custom trigger every X amount of seconds passed. Message difference allows you to prevent spamming an inactive channel. It requires Y amount of messages have passed in the channel since the last iteration of the message. The default is 1 so at least one message will need to have been sent in the channel in order for the repeat to trigger.

###  Settings

  * !links on/off - Owner - Toggles link filtering on and off 
  * !permit [name] - Mods - Permits name to post 1 link 
  * !pd add/delete/list - Owner - Configures permanently permitted domains 
  
  
  * !caps on/off - Owner - Toggle cap filtering on and off 

Filtered messages must match all three of the below settings:

  * !caps percent [int (0-100)] - >= this percentage of caps per line 
  * !caps mincaps [int] - >= this number of caps per line 
  * !caps minchars [int] - total characters per line must be >= this 
  * !caps status - Displays the current values 
  
  
  * !offensive on/off - Turns filter on/off 
  * !offensive list - Lists filtered words 
  * !offensive add [word string] - Adds string to filter 
  * !offensive delete [word string] - Removes string from filter 
  
  
  * !emotes on/off - Owner - Toggle emote spam filtering on and off 
  * !emotes max [int] - Max number of emotes allowed 

  * !set [option] on/off - Owner 
    * topic - Enables the !topic command 
    * filters - Global toggle for cap and link filters 
    * throw - Enables the !throw command 
    * signedkicks - Enables announcing cap and link kicks 
    * joinsparts - Enables announcing users joining and leaving the channel 
    * lastfm [username/off] - Sets username to use with !music command 
    * steam [ID] - Sets your Steam ID. Must be in [SteamID64](http://steamidconverter.com/) format and profile must be public 
    * mode [(0/owner),(1/mod),(2,everyone),(-1, "Admin mode")] - Sets the minimum access to use any bot commands. Options are either owner, mod, everyone, or "Admin mode". Specifying no argument will display current setting. Also see !modchan 
    * chatlogging - Enables logging of chat conversations. Logs are stored at [http://btx.me/ackbot](http://btx.me/ackbot) and are publicly visible 
  
  
  * !regular add/delete/list [name] - Owner - Adds a "regular". Regulars don't need permission to post links 
  * !mod add/delete/list [name] - Owner - Adds a "moderator". Moderators have access to all the bot commands the same way mods/ops do. Sometimes the bot does not recognize peoples op status do to issues with their IRC implementation so this is a way to explicitly add them. Channel owner automatically gets this status 
  * !owner add/delete/list [name] - Owner - Gives a user owner permissions on a channel 

###  Poll, Giveaway, Raffle

  * !poll create [vote options] - Ops - Creates a new poll with specified options. (ex "!poll create pie cake") 
  * !poll start/stop - Ops - Starts or stops the poll 
  * !poll results - Ops - Displays poll results 
  
  
  * !giveaway create max-number [duration] - Ops - Creates a number-selection based giveaway with numbers from 1 - max. Duration is an optional value in seconds after which the giveaway will stop. Specifying a duration will auto-start the giveaway and stop will not need to be executed 
  * !giveaway start/stop - Ops - Starts or stops the giveaway entry 
  * !giveaway results - Ops - Displays winner(s) 
  * (!ga - Alias for !giveaway) 
  
  
  * !raffle - Enters sender in the raffle. 
  * !raffle enable/disable - Enables entries in the raffle. 
  * !raffle reset - Clears entries. 
  * !raffle count - Displays number of entries. 
  * !raffle winner - Picks a winner. 

  
### Random Generators

  * !random user - Selects a random user from the userlist (Due to quirks with Twitch chat, this is not a recommended why of picking user) 

### Bot General

  * !join - Request bot to join your channel 
  * !rejoin - Force bot to attempt to rejoin your channel 
  * !leave - Owner - Removes the bot from channel 

###  Admin

Admin nicks are defined in global.properties. Twitch Admins and Staff also have access.

  * !bm-join [#channelname] - Joins channelname  
  * !bm-leave [#channelname] - Leaves channelname   
  * !bm-rejoin - Rejoin all channels   
  * !bm-reconnect - Disconnects all bots and allows them to "auto-reconnect" 
  * !bm-global [message] - Sends a message to all channel the bot is in

### String Replacement

Adding dynamic data to bot message is also supported via string substitutions. The following substitutions are available:

  * (\_GAME\_) : Twitch game
  * (\_STATUS\_) : Twitch status
  * (\_VIEWERS\_) : Viewer count
  * (\_CHATTERS\_) : Number of users in chat
  * (\_STEAM\_PROFILE\_) : Link to Steam profile (Steam account must be configured)
  * (\_STEAM\_GAME\_) : Steam game (Steam account must be configured)
  * (\_STEAM\_SERVER\_) : Server you are playing on with a compatible (ie SteamWorks) game (Steam account must be configured)
  * (\_STEAM\_STORE\_) : Link to Steam store for the game you are playing (Steam account must be configured)
  * (\_SONG\_) : Scrobbled Last.fm track name and artist (Last.fm account must be configured)

  Example:
  !command add info I am (\_PROFILE\_) and I'm playing (\_STEAM\_GAME\_) on (\_STEAM\_SERVER\_) listening to (\_SONG\_)

  Output:
  I am http://bit.ly/321nds and I'm playing FTL: Faster Than Light on (none) listening to Wings of Destiny by David Saulesco