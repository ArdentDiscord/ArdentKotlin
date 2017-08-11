You need to have a text editor or IDE that supports **kotlin** integration: [**IntelliJ Idea**](https://www.jetbrains.com/idea/) is definitely preferred by myself and you should probably use it too!

You'll need API keys from the following: [**Discord**](https://discordapp.com/developers) and [**Twitch**](https://www.twitch.tv/settings/connections)

Finally, you'll need to set up a rethinkdb server, set the admin password, and put the url to the database (could be localhost) and the password for an account called `ardent` that you'll need to create

To test, you'll need to set `val test = false` to `true`. You'll also need a config.txt file in a referenced place, with the format of below
You must be familiar with Git (either command line or through your IDE) to be able to submit a pull request

```
token :: DISCORD_BOT_TOKEN
twitch :: TWITCH_API_KEY
client_secret :: CLIENT_SECRET
password :: RETHINK_DB_PASSWORD
rethinkdb :: YOUR_RETHINK_DB_URL_NO_PORTS
node1 :: NOTNECESSARY
node2 :: NOTNECESSARY
patreon :: NOTNECESSARY
discordbotsorg :: NOTNECESSARY
botsdiscordpw :: NOTNECESSARY
```
