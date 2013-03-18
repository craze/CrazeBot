##  Installation

###  Installing from source

Downloading source code (Source access currently unavailable but will be back soon.)

    git clone git@bitbucket.org:bashtech/geobot.git

Compile with maven

    cd GeoBot

    mvn install

Copy jar to desired location

    cp target/GeoBot.jar ~/mybot/.


Install [JSON Simple](http://bashtech.net/twitch/ackbot/json-simple-1.1.jar) to '~/mybot/lib/json-simple-1.1.jar'


###  Running

The bot can be run from console with the command:

    java -jar Geobot.jar

or double click the Jar to open the logging GUI.

When starting the bot for the first time, run and quit the bot once to
generate the initial _global.properties_ file. Next edit _global.properties_
and set the server, and add a channel to channelList followed by a comma and
add yourself to the adminList.

    
    server=yourchannel.jtvirc.com 
    channelList=#yourchanel 
    adminList=yournick 

Next start the bot again and it will generate a _#yourchannel.properties_
file.

After that property is set the bot should now join your channel.

Also, note that all toggle-able features are off by default. You must use
the `!set` command to enable them. This includes topic,
filters, etc.