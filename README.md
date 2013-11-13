##  Summary

Geobot is a Java-based IRC bot based on the
[PIRC](http://www.jibble.org/pircbot.php) framework designed for [Twitch](http://twitch.tv).

##  Features

  * Advanced moderation filers
  * Twitch API integration
  * Custom Triggers and Auto replies
  * Open Source!

##  Issues/Feature Requests

Open a [new issue](https://bitbucket.org/bashtech/geobot/issues) on BitBucket.

##  Commands

###  Note about prefixes

This documentation uses the default command prefix of `!`. This can be changed on the channel level using: `!set prefix`.

### Syntax
  * `<>` - Denotes required arguments
  * `[]` - Denotes optional arguments
  * `option1|option2`- Denotes basic toggles.

###  General Channel

  * `!join` - Request bot to join your channel 
  * `!rejoin` - Force bot to attempt to rejoin your channel 
  * `!leave` - Owner - Removes the bot from channel 

  * `!topic [new topic]` - Displays and sets the topic. If no topic is set, Twitch channel title will be returned.
  * `!bitrate` - The current broadcast bitrate.  
  * `!resolution` or `!res` - Displays source broadcast resolution.
  * `!viewers` - Displays the number of Twitch viewers. 
  * `!uptime` - Displays stream starting time and length of time streamed. 
  * `!music` - Displays what you are currently listening to on Last.fm. See `!set lastfm` to set account info.
  * `!steam` - Display Steam profile, current game and server See `!set steam` to set account info.
  * `!bothelp` - Displays link to bot help documents 
  * `!commercial` - Runs a 30 second commercial. You must contact the bot maintainer to follow your channel with the bot's account and add the bot as a channel editor. This command is also supported in `!repeat` and `!schedule`.
  * `!game [new game]` - Displays the current Twitch game. Optional - specify a new game to set (must be channel editor).
  * `!status [new status]` - Displays the current Twitch status. Optional - specify a new status to set (must be channel editor).
  * `!followme` - Request the bot follow your Twitch account. Can only be done in your own channel.


### Fun 

  * `!throw <object>` - Throws object 

###  Custom, Custom Repeat, Custom Scheduled, Auto-Replies

#### Triggers
Custom commands aka triggers provide frequently requested information in your channel.  

  * `!command add <name> <text>` - Creates an info command (!name) 
  * `!command delete <name>` - Removes info command (!name)
  * `!command restrict <name> <everyone|regulars|mods|owner>` - Sets access level for triggers.

#### Repeat
The repeat command will repeat a custom trigger every X amount of seconds passed. Message difference allows you to prevent spamming an inactive channel. It requires Y amount of messages have passed in the channel since the last iteration of the message. The default is 1 so at least one message will need to have been sent in the channel in order for the repeat to trigger.  

  * `!repeat add <name> <delay in seconds> [message difference]` - Sets a command to repeat.
  * `!repeat delete <name>` - Stops repetition of a command and discards info.
  * `!repeat on|off <name>` - Enables/disables repetition of a command but keeps info.
  * `!repeat list` - Lists commands that will be repeated.

#### Schedule 
Schedule is similar to repeat but is designed to repeat at specific times such as 5pm, hourly (on the hour), semihourly (on 0:30), etc. `pattern` accepts: hourly, semihourly, and [crontab syntax**](http://i.imgur.com/j4t8CcM.png). **Replace spaces in crontab syntax with _ (underscore)**


  * `!schedule add <name> <pattern> [message difference]` - Schedules a command.
  * `!schedule delete <name>` - Removes scheduled command and discards info.
  * `!schedule on/off <name>` - Enables/disables scheduled command but keeps info.
  * `!schedule list` - Lists schedules commands.

Examples:

 * `!schedule add youtube hourly 0` - This will repeat the `!youtube` command every hour on the hour.
 * `!schedule add ip *_*_*_*_* 0` - This will repeat the `!ip` command every minute.
 * `!schedule add texture *_5_*_*_* 0` - This will repeat the `!texture` at 5am every day.

#### Auto-Replies
Autoreplies are like custom triggers but do not require a command to be typed. The bot will check all messages for the specified pattern and reply with the response if found. **Responses have a 30 second cooldown**

 * `!autoreply list` - Lists current autoreplies
 * `!autoreply add [patten] [response]` - Adds an autoreply triggered by \*pattern\* with the desired response. Use * to denote wildcards and _ to denote spaces in the pattern.
 * `!autoreply remove [number]` - Removes the autoreply with that index number. Do !autoreply list for those values.

Example:

`!autoreply add *what*texture* The broadcaster is using Sphax.` will respond with: `The broadcaster is using Sphax.` if a message similar to: `What texture pack is this?` is typed.

###  Moderation

#### Shortcuts
  * `+m` - Slow mode on. 
  * `-m` - Slow mode off. 
  * `+f` - Followers only mode on. 
  * `-f` - Followers only mode off. 
  * `+s` - Subscribers only mode off. 
  * `-s` - Subscribers only mode off. 
  * `+b [user]` - Bans a user. 
  * `-b [user]` - Unbans a user. 
  * `+k [user]` - Timeouts a user. 
  * `+p [user]` - Purges a user.  
  * `!clear` - Clears chat.

### Filters

 * `!filter on|off` - Toggles all filters.
 * `!filter status` - Displays status of all filter options.
 * `!filter me on|off` - Toggle the action AKA /me filter.
 * `!filter enablewarnings on|off` - Toggles a 10sec warning timeout for first offenses.
 * `!filter timeoutduration <seconds>` - Sets timeout duration for filter timeouts. 
 * `!filter displaywarnings on|off` - Toggle a message warning and announcing filter timeouts.
 * `!filter messagelength <num of char>` - Sets the maximum allowed character length for a message.

As of Nov. 10, 2013, filter options have been moved to sub commands of `!filter`. For example, `!caps` can now be found as `!filter caps`.

#### Links
  * `links on|off` - Toggles link filtering on and off.
  * `pd add|delete <domain>` - Configures permanently permitted domains. 
  * `pd list` - Lists domains that are allowed to bypass link filter.


  * `!permit <name>` - Permits a user to post 1 link. **This is NOT a sub command!**
  
#### Caps
  * `caps on|off` - Toggle cap filtering on and off. 

Filtered messages must match all three of the below settings:

  * `caps percent <int (0-100)>` - >= this percentage of caps per line. 
  * `caps mincaps <int>` - >= this number of caps per line. 
  * `caps minchars <int>` - total characters per line must be >= this. 
  * `caps status` - Displays the current values. 
  
#### Banned phrases
  * `banphrase on|off` - Turns filter on/off.
  * `banphrase list` - Lists filtered words.
  * `banphrase add <phrase>` - Adds string to filter - Accepts direct regular expressions (Prefix with REGEX:).
  * `banphrase delete <phrase>` - Removes string from filter.

#### Symbols
Covers ASCII symbols, unicode classes for box drawings, block elements and geometric shapes also select other spammed characters.

  * `symbols on|off` - Turns symbols filter on/off.  
  
Filtered messages must match both of the below settings:

  * `symbols percent <int (0-100)>` - >= percentage of symbols.
  * `symbols min <int>` - >= number of symbols per line.
  * `symbols status` - Displays the current values.

#### Emotes

Limits Twitch **global** emotes.

  * `emotes on|off` - Owner - Toggle emote spam filtering on and off. 
  * `emotes max <int>` - Max number of emotes allowed.
  * `emotes single on|off` - Toggles filter for single emote messages.

### Settings
  * `!set <option> [parameters]`
  * **Options**: 
    * `topic on|off` - Enables the !topic command.
    * `throw on|off` - Enables the !throw command.
    * `lastfm <username|off>` - Sets username to use with !music command.
    * `steam <ID>` - Sets your Steam ID. Must be in [SteamID64](http://steamidconverter.com/) format and profile must be public. 
    * `mode <(0/owner)|(1/mod)|(2,everyone)|(-1, "Admin mode")>` - Sets the minimum access to use any bot commands. 
    * `commerciallength <30|60|90|120|150|180>` - Length of commercials to run with.
    * `tweet <message>` - Format for Click to tweet message.
    * `prefix <character>` - Sets the command prefix. Default is "!"
    * `emoteset <set id>` - Sets the emote_set for of the subscription product for this channel. (Used to determine subscriber status for regulars)
    * `subscriberregulars on|off` - Treats subscribers a regulars. `emoteset` must be defined first.
    * `subscriberalerts on|off` - Toggle chat alert when a new user subs.
    * `subscriberalerts message <message>` - Message to be displayed when new user subs. Use `(_1_)` to insert the new subscriber's name.

### User Levels

Consists of Owners, Mods, and Regulars. Owner have permission to you all channel bot commands. Mods have permission to use moderation related commands. Regulars are immune to the link filter. **Mods are optional if you only wish to use Twitch mod status**

  * `!owner|mod|regular list` - Lists users in that group.
  * `!owner|mod|regular add|remove <name>` - Adds/removes a user from the group.

###  Poll, Giveaway, Raffle

#### Poll 
  * `!poll create <option 1> <option 2> ... [option n]` - Creates a new poll with specified options.
  * `!poll start|stop` - Starts or stops the poll 
  * `!poll results` - Displays poll results
  * `!vote <option]>` -  Votes in the poll.
  
#### Giveaway
  * `!giveaway create <max-number> [duration]` - Creates a number-selection based giveaway with numbers from 1 - max. Duration is an optional value in seconds after which the giveaway will stop. Specifying a duration will auto-start the giveaway and stop will not need to be executed.
  * `!giveaway start|stop` - Starts or stops the giveaway entry 
  * `!giveaway results` - Displays winner(s) 
  * `!ga` - Alias for `!giveaway`
  
#### Raffle
  * `!raffle` - Enters the raffle. 
  * `!raffle enable|disable` - Enables entries in the raffle. 
  * `!raffle reset` - Clears entries. 
  * `!raffle count` - Displays number of entries. 
  * `!raffle winner` - Picks a winner. 

### String Replacement

Adding dynamic data to bot message is also supported via string substitutions. Almost any response from the bot will accept a replacement. The following substitutions are available:

  * `(_GAME_)` : Twitch game.
  * `(_STATUS_)` or `(_JTV_STATUS_)`: Channel status.
  * `(_VIEWERS_)` or `(_JTV_VIEWERS_)` : Viewer count.
  * `(_STEAM_PROFILE_)` : Link to Steam profile (Steam account must be configured).
  * `(_STEAM_GAME_)` : Steam game (Steam account must be configured).
  * `(_STEAM_SERVER_)` : Server you are playing on with a compatible (ie SteamWorks) game (Steam account must be configured).
  * `(_STEAM_STORE_)` : Link to Steam store for the game you are playing (Steam account must be configured).
  * `(_SONG_)` : Scrobbled Last.fm track name and artist (Last.fm account must be configured).
  * `(_BOT_HELP_)` : Bot's help message. See bothelpMessage in global.properties..
  * `(_TWEET_URL_)` : Click to tweet URL See !set tweet.
  * `(_USER_)` : Nick of the user requesting a trigger or triggering an autoreply.

  Example:
  `!command add info I am (_STEAM_PROFILE_) and I'm playing (_STEAM_GAME_) on (_STEAM_SERVER_) listening to (_SONG_)`

  Output:
  `I am http://bit.ly/yoursteamprofile and I'm playing ARMA III on 127.0.0.1:2602 listening to Wings of Destiny by David Saulesco`

###  Admin

Admin nicks are defined in global.properties. Twitch Admins and Staff also have access.

  * `!admin join [#channelname]` - Joins channelname. (Note: Forces mode level -1)
  * `!admin part [#channelname]` - Leaves channelname