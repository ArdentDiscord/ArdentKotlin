package commands.music

import events.Category
import events.ExtensibleCommand
import main.conn
import main.r
import main.waiter
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import translation.tr
import utils.discord.*
import utils.functionality.*
import utils.music.DatabaseMusicPlaylist
import utils.music.getPlaylistById
import java.util.stream.Collectors

class MyMusicLibrary : ExtensibleCommand(Category.MUSIC, "mylibrary", "reset, or play from your personal music library", "mymusic") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        showHelp(event)
    }

    override fun registerSubcommands() {
        with("reset", null, "reset your music library from scratch", { arguments, event ->
            event.channel.send(Emoji.INFORMATION_SOURCE.symbol + " " + "Are you sure you want to reset your library? Type **yes** to continue".tr(event))
            waiter.waitForMessage(Settings(event.author.id, event.channel.id, event.guild.id), { message ->
                if (message.rawContent.startsWith("ye")) {
                    val library = getMusicLibrary(event.author.id)
                    library.tracks = mutableListOf()
                    library.update("musicLibraries", event.author.id)
                    event.channel.send(Emoji.BALLOT_BOX_WITH_CHECK.symbol + " " + "Successfully reset your music library")
                } else event.channel.send("**Yes** wasn't provided, so I cancelled the reset")
            })
        })

        with("play", null, "play from your personal music library", { arguments, event ->
            val library = getMusicLibrary(event.author.id)
            if (library.tracks.size == 0) event.channel.send(Emoji.HEAVY_MULTIPLICATION_X.symbol + " " + "You don't have any tracks in your music library! Add some at {0}".tr(event, "https://ardentbot.com/profile/${event.author.id}"))
            else {
                event.channel.send("Started loading **{0}** tracks from your music library..".tr(event, library.tracks.size))
                library.load(event.member, event.textChannel)
            }
        })
    }

}

class Playlist : ExtensibleCommand(Category.MUSIC, "playlist", "create, delete, or play from your saved playlists", "playlists") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        showHelp(event)
    }

    override fun registerSubcommands() {
        with("play", "play [playlist id]", "start playback of a playlist", { arguments, event ->
            val playlist: DatabaseMusicPlaylist? = asPojo(r.table("musicPlaylists").get(arguments.getOrElse(0, { "" })).run(conn), DatabaseMusicPlaylist::class.java)
            if (playlist == null) event.channel.send("You need to specify a valid playlist id!".tr(event))
            else {
                event.channel.send("Loading tracks from playlist **{0}**..".tr(event, playlist.name))
                playlist.toLocalPlaylist(event.member).loadTracks(event.textChannel, event.member)
            }
        })

        with("list", "list @User", "see the mentioned user's playlists", { arguments, event ->
            val user = event.message.mentionedUsers.getOrElse(0, { event.author })
            val embed = event.member.embed("{0} | Music Playlists".tr(event, user.toFancyString()), event.textChannel)
            val playlists = getPlaylists(user.id)
            if (playlists.isEmpty()) embed.appendDescription("This user doesn't have any playlists! Create one by typing */playlist create [name]*".tr(event))
            else {
                playlists.forEachIndexed { index, playlist ->
                    embed.appendDescription(index.getListEmoji() + " " +
                            "[**{0}**](https://ardentbot.com/music/playlist/{1}) - ID: *{2}* - Last Modified at *{3}*"
                                    .tr(event, playlist.name, playlist.id, playlist.id, playlist.lastModified.readableDate()) + "\n")
                }
                embed.appendDescription("Play a playlist using */playlist play [playlist id]*")
            }
            embed.send()
        })

        with("view", "view [playlist id]", "see an overview of the specified playlist", { arguments, event ->
            val playlist: DatabaseMusicPlaylist? = asPojo(r.table("musicPlaylists").get(arguments.getOrElse(0, { "" })).run(conn), DatabaseMusicPlaylist::class.java)
            if (playlist == null) event.channel.send("You need to specify a valid playlist id!".tr(event))
            else {
                event.channel.send("To see track information for, or modify **{0}** *by {1}*, go to {2} - id: *{3}*".tr(event, playlist.name, getUserById(playlist.owner)?.toFancyString() ?: "Unknown", "https://ardentbot.com/music/playlist/${playlist.id}", playlist.id))
            }
        })
        with("delete", "delete [playlist id]", "delete one of your playlists", { arguments, event ->
            val playlist: DatabaseMusicPlaylist? = asPojo(r.table("musicPlaylists").get(arguments.getOrElse(0, { "" })).run(conn), DatabaseMusicPlaylist::class.java)
            if (playlist == null) event.channel.send("You need to specify a valid playlist id!".tr(event))
            else {
                if (playlist.owner != event.author.id) event.channel.send("You need to be the owner of this playlist in order to delete it!")
                else {
                    event.channel.selectFromList(event.member, "Are you sure you want to delete the playlist **{0}** [{1} tracks]? This is **unreversable**".tr(event, playlist.name, playlist.tracks.size), mutableListOf("Yes", "No"), { selection, m ->
                        if (selection == 0) {
                            r.table("musicPlaylists").get(playlist.id).delete().runNoReply(conn)
                            event.channel.send(Emoji.BALLOT_BOX_WITH_CHECK.symbol + " " + "Deleted the playlist **{0}**".tr(event, playlist.name))
                        } else event.channel.send(Emoji.BALLOT_BOX_WITH_CHECK.symbol + " " + "Cancelled playlist deletion..".tr(event))
                        m.delete()
                    }, translatedTitle = true)
                }
            }
        })
        with("create", null, "create a new playlist", { arguments, event ->
            if (arguments.isEmpty()) event.channel.send("You need to include a name for this playlist")
            else {
                val name = arguments.concat()
                event.channel.selectFromList(event.member, "What type of playlist do you want to create?",
                        mutableListOf("Default", "Spotify Playlist or Album", "Clone someone's playlist", "YouTube Playlist"), { selection, msg ->
                    msg.delete().queue()
                    when (selection) {
                        0 -> {
                            event.channel.send("Successfully created the playlist **{0}**!".tr(event, name))
                            DatabaseMusicPlaylist(genId(6, "musicPlaylists"), event.author.id, name, System.currentTimeMillis(),
                                    null, null, null, tracks = mutableListOf()).insert("musicPlaylists")
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
                                            null, url.removePrefix("https://open.spotify.com/user/")
                                            .split("/playlist/").stream().collect(Collectors.joining("||")), null)
                                } else {
                                    event.channel.send("You specified an invalid url. Cancelled playlist setup.".tr(event))
                                    null
                                }
                                playlist?.insert("musicPlaylists")
                            })
                        }
                        2 -> {
                            event.channel.send("Please enter in an Ardent playlist id or url")
                            waiter.waitForMessage(Settings(event.author.id, event.channel.id, event.guild.id), { reply ->
                                val url = reply.rawContent.replace("https://ardentbot.com/music/playlist/", "")
                                val playlist = getPlaylistById(url)
                                if (playlist == null) event.channel.send("You specified an invalid playlist. Please try again")
                                else {
                                    playlist.copy(name = name, owner = event.author.id).insert("musicPlaylists")
                                    event.channel.send("Successfully cloned **{0}**!".tr(event, playlist.name))
                                }
                            })
                        }
                        else -> {
                            event.channel.send("Please specify a YouTube playlist url now.")
                            waiter.waitForMessage(Settings(event.author.id, event.channel.id, event.guild.id), { reply ->
                                val url = reply.rawContent
                                if (url.startsWith("https://www.youtube.com/watch")) {
                                    event.channel.send("Successfully created the playlist **{0}**!".tr(event, name))
                                    DatabaseMusicPlaylist(genId(6, "musicPlaylists"), event.author.id, name, System.currentTimeMillis(),
                                            null, null, url, tracks = mutableListOf()).insert("musicPlaylists")
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