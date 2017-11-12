package commands.music

import events.Category
import events.ExtensibleCommand
import main.waiter
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import translation.tr
import utils.discord.send
import utils.functionality.*
import utils.music.DatabaseMusicPlaylist

class Playlist : ExtensibleCommand(Category.MUSIC, "playlist", "create, delete, or play from your saved playlists", "playlists") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        showHelp(event)
    }

    override fun registerSubcommands() {
        with("create", null, "create a new playlist", { arguments, event ->
            if (arguments.isEmpty()) event.channel.send("You need to include a name for this playlist")
            else {
                val name = arguments.concat()
                event.channel.selectFromList(event.member, "What type of playlist do you want to create?",
                        mutableListOf("Default", "Spotify Playlist or Album", "YouTube Playlist"), { selection, msg ->
                    msg.delete().queue()
                    when (selection) {
                        0 -> {
                            event.channel.send("Successfully created the playlist **{0}**!".tr(event, name))
                            DatabaseMusicPlaylist(genId(6, "musicPlaylists"), event.author.id, name, System.currentTimeMillis(),
                                    null, null, null).insert("musicPlaylists")
                        }
                        1 -> {
                            event.channel.send("Please enter in a Spotify playlist or album url now")
                            waiter.waitForMessage(Settings(event.author.id, event.channel.id, event.guild.id), { reply ->
                                val url = reply.rawContent
                                val playlist: DatabaseMusicPlaylist? = if (url.startsWith("https://open.spotify.com/album/")) {
                                    event.channel.send("Successfully created the playlist **{0}**!".tr(event, name))
                                    DatabaseMusicPlaylist(genId(6, "musicPlaylists"), event.author.id, name, System.currentTimeMillis(),
                                            url.removePrefix("https://open.spotify.com/album/"), null, null)
                                } else if (url.startsWith("https://open.spotify.com/user/")) {
                                    event.channel.send("Successfully created the playlist **{0}**!".tr(event, name))
                                    DatabaseMusicPlaylist(genId(6, "musicPlaylists"), event.author.id, name, System.currentTimeMillis(),
                                            null, url.removePrefix("https://open.spotify.com/user/").split("/playlist/")[1],
                                            null)
                                } else {
                                    event.channel.send("You specified an invalid url. Cancelled playlist setup.".tr(event))
                                    null
                                }
                                playlist?.insert("musicPlaylists")
                            })
                        }
                        else -> {
                            event.channel.send("Please specify a YouTube playlist url now.")
                            waiter.waitForMessage(Settings(event.author.id, event.channel.id, event.guild.id), { reply ->
                                val url = reply.rawContent
                                if (url.startsWith("https://www.youtube.com/watch")) {
                                    event.channel.send("Successfully created the playlist **{0}**!".tr(event, name))
                                    DatabaseMusicPlaylist(genId(6, "musicPlaylists"), event.author.id, name, System.currentTimeMillis(),
                                            null, null, url).insert("musicPlaylists")
                                } else {
                                    event.channel.send("You specified an invalid url. Cancelled playlist setup.".tr(event))
                                }
                            })
                        }
                    }
                }, failure = {
                    event.channel.send("Cancelled playlist creation.".tr(event))
                })
            }
        })
    }
}