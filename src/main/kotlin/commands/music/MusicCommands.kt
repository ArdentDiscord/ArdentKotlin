package commands.music

import events.Category
import events.Command
import main.spotifyApi
import main.waiter
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import obj.Market
import utils.*

class Radio : Command(Category.MUSIC, "radio", "play a spotify playlist or radio station live from a list of provided options. this is a **patron-only** feature", "pr") {
    private val stations = mutableListOf(
            "Spotify **Today's Top Hits**",
            "Spotify **Rap Caviar**",
            "Spotify **Power Gaming**",
            "Spotify **Teen Party**",
            "Spotify **USA Top 50**",
            "Spotify **Viva Latino Top 50**",
            "Spotify **Hot Country**"
    )

    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.channel.requires(event.member, DonationLevel.BASIC)) {
            event.channel.send("Thanks for supporting Ardent! Enjoy this ever-expanding list of popular Spotify playlists that you might enjoy listening to".tr(event))
            if (event.member.voiceChannel() == null) event.channel.send("You need to be in a voice channel!".tr(event))
            else {
                event.channel.selectFromList(event.member, "Select the playlist that you want to listen to", stations, { selection, _ ->
                    when (selection) {
                        0 -> "https://open.spotify.com/user/spotify/playlist/37i9dQZF1DXcBWIGoYBM5M"
                        1 -> "https://open.spotify.com/user/spotify/playlist/37i9dQZF1DX0XUsuxWHRQd"
                        2 -> "https://open.spotify.com/user/spotify/playlist/37i9dQZF1DX6taq20FeuKj"
                        3 -> "https://open.spotify.com/user/spotify/playlist/37i9dQZF1DX1N5uK98ms5p"
                        4 -> "https://open.spotify.com/user/spotifycharts/playlist/37i9dQZEVXbLRQDuF5jeBp"
                        5 -> "https://open.spotify.com/user/spotify/playlist/37i9dQZF1DX10zKzsJ2jva"
                        6 -> "https://open.spotify.com/user/spotify/playlist/37i9dQZF1DX1lVhptIYRda"
                        else -> ""
                    }.getSpotifyPlaylist(event.textChannel, event.member)
                })
                return
            }
        }
    }

    override fun registerSubcommands() {
    }
}

class ArtistSearch : Command(Category.MUSIC, "searchartist", "search top songs by the authors' names", "artistsearch", "artist", "searchauthor", "author") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.voiceChannel() == null) {
            event.channel.send("You need to be in a voice channel to use this command!".tr(event))
            return
        }
        val artistName = arguments.concat()
        if (artistName.isEmpty()) event.channel.send("You need to include an author!".tr(event))
        else {
            if (event.channel.requires(event.member, DonationLevel.BASIC)) {
                try {
                    val searchResults = spotifyApi.search.searchArtist(artistName, 7)
                    val options = searchResults
                            .items
                            .asSequence()
                            .sortedByDescending { it.popularity }
                    event.channel.selectFromList(event.member, "Select the artist you want", options.map { "${it.name} *Popularity: ${it.popularity}*" }.toMutableList(), { option, selectionMessage ->
                        selectionMessage.delete().queue()
                        val artist = options.toList()[option]
                        val tracks = spotifyApi.artists.getArtistTopTracks(artist.id, Market.US).tracks
                        if (tracks.isEmpty()) event.channel.send("No tracks were found with that author :(".tr(event))
                        else {
                            val embed = event.member.embed("Top tracks for {0}".tr(event, artist.name))
                                    .appendDescription("**Select reactions corresponding to the choices you want to add those songs to the queue**".tr(event))
                            var current = 0
                            while (current < 7 && tracks.size > current) {
                                embed.appendDescription("\n${Emoji.SMALL_ORANGE_DIAMOND} [**${current + 1}**] ${tracks[current].name}")
                                current++
                            }
                            event.channel.sendMessage(embed.build()).queue { embedMessage ->
                                for (x in 1..(if (tracks.size > 7) 7 else tracks.size)) {
                                    embedMessage.addReaction(when (x) {
                                        1 -> Emoji.KEYCAP_DIGIT_ONE
                                        2 -> Emoji.KEYCAP_DIGIT_TWO
                                        3 -> Emoji.KEYCAP_DIGIT_THREE
                                        4 -> Emoji.KEYCAP_DIGIT_FOUR
                                        5 -> Emoji.KEYCAP_DIGIT_FIVE
                                        6 -> Emoji.KEYCAP_DIGIT_SIX
                                        else -> Emoji.KEYCAP_DIGIT_SEVEN
                                    }.symbol).queue()
                                }
                                val songsToQueue = mutableListOf<String>()
                                waiter.gameReactionWait(embedMessage, { author, messageReaction ->
                                    if (author.id == event.author.id) {
                                        when (messageReaction.emote.name) {
                                            Emoji.KEYCAP_DIGIT_ONE.symbol -> songsToQueue.addIfNotExists(tracks[0].name)
                                            Emoji.KEYCAP_DIGIT_TWO.symbol -> songsToQueue.addIfNotExists(tracks[1].name)
                                            Emoji.KEYCAP_DIGIT_THREE.symbol -> songsToQueue.addIfNotExists(tracks[2].name)
                                            Emoji.KEYCAP_DIGIT_FOUR.symbol -> songsToQueue.addIfNotExists(tracks[3].name)
                                            Emoji.KEYCAP_DIGIT_FIVE.symbol -> songsToQueue.addIfNotExists(tracks[4].name)
                                            Emoji.KEYCAP_DIGIT_SIX.symbol -> songsToQueue.addIfNotExists(tracks[5].name)
                                            Emoji.KEYCAP_DIGIT_SEVEN.symbol -> songsToQueue.addIfNotExists(tracks[6].name)
                                        }
                                    }
                                }, {
                                    if (songsToQueue.size == 0) event.channel.send("You didn't select any songs!".tr(event))
                                    else {
                                        event.channel.send("**Adding __{0}__ songs to the queue by __{1}__!**".tr(event, songsToQueue.size, artist.name))
                                        songsToQueue.forEach {
                                            it.getSingleTrack(event.guild, { foundTrack ->
                                                play(event.textChannel, event.member, ArdentTrack(event.author.id, event.channel.id, foundTrack))
                                            })
                                        }
                                    }
                                })
                            }
                        }
                    })
                } catch (e: Exception) {
                    event.channel.send("You provided an invalid author name :(".tr(event))
                }
            }
        }
    }

    override fun registerSubcommands() {
    }
}

class RemoveAt : Command(Category.MUSIC, "removeat", "remove a song from the queue by typing its place", "remove") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.hasOverride(event.textChannel, true, djCommand = true)) {
            if (!event.member.checkSameChannel(event.textChannel)) return
            if (event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.removeAt(arguments.getOrNull(0)?.toIntOrNull()?.minus(1))) {
                event.channel.send("Removed song from the queue".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
            } else event.channel.send("Failed to remove track... check the number in the queue?".tr(event))
        }
    }

    override fun registerSubcommands() {
    }
}

class Play : Command(Category.MUSIC, "play", "play a song by its url or search a song to look it up on youtube", "p") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) event.channel.send("You need to specify a URL or search query!".tr(event))
        else arguments.concat().load(event.member, event.textChannel)
    }

    override fun registerSubcommands() {
    }
}

class Leave : Command(Category.MUSIC, "leave", "makes me leave the voice channel I'm in", "disconnect") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val guild = event.guild
        val manager = guild.getGuildAudioPlayer(event.textChannel).scheduler.manager
        guild.audioManager.closeAudioConnection()
        manager.resetQueue()
        manager.nextTrack()
        if (guild.audioManager.connectedChannel != null) event.channel.send("Successfully disconnected from **{0}**".tr(event, guild.audioManager.connectedChannel.name) + " ${Emoji.MULTIPLE_MUSICAL_NOTES}")
    }

    override fun registerSubcommands() {
    }
}

class Pause : Command(Category.MUSIC, "pause", "pause the player... what did you think this was gonna do?") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        pause(event, true)
    }

    override fun registerSubcommands() {
    }
}

class Resume : Command(Category.MUSIC, "resume", "resumes the player. what did you think this was gonna do?") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        pause(event, false)
    }

    override fun registerSubcommands() {
    }
}

fun pause(event: MessageReceivedEvent, value: Boolean) {
    if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
    val player = event.guild.getGuildAudioPlayer(event.textChannel).player
    if (!player.currentlyPlaying(event.textChannel)) return
    player.isPaused = value
    if (!value) event.channel.send("Resumed playback".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
    else event.channel.send("Paused playback".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
}

class Skip : Command(Category.MUSIC, "skip", "skips the currently playing track") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        if (!manager.player.currentlyPlaying(event.textChannel)) return
        val track = manager.scheduler.manager.current!!
        event.channel.send("Skipped current track: **{0}** by *{1}* {2} - added by **{3}**".tr(event, track.track.info.title, track.track.info.author, track.track.getCurrentTime(), track.author.toUser()?.withDiscrim() ?: "unable to determine"))
        manager.player.playingTrack.position = manager.player.playingTrack.duration - 1
    }

    override fun registerSubcommands() {
    }
}

class Stop : Command(Category.MUSIC, "stop", "stop the player and remove all tracks in the queue") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        manager.scheduler.autoplay = false
        manager.scheduler.manager.resetQueue()
        manager.player.stopTrack()
        event.channel.send("Stopped and reset the player".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }

    override fun registerSubcommands() {
    }
}

class SongUrl : Command(Category.MUSIC, "songlink", "get the link for the currently playing track!", "su") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel)) return
        val player = event.guild.getGuildAudioPlayer(event.textChannel).player
        if (!player.currentlyPlaying(event.textChannel)) return
        event.channel.send("**{0}** by **{1}**: {2}".tr(event, player.playingTrack.info.title, player.playingTrack.info.author, player.playingTrack.info.uri))
    }

    override fun registerSubcommands() {
    }
}

class Shuffle : Command(Category.MUSIC, "shuffle", "shuffle the current queue") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.shuffle()
        event.channel.send("Shuffled the current queue".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }

    override fun registerSubcommands() {
    }
}

class Repeat : Command(Category.MUSIC, "repeat", "repeat the track that's currently playing") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (!player.currentlyPlaying(event.textChannel)) return
        manager.scheduler.manager.addToBeginningOfQueue(manager.scheduler.manager.current)
        event.channel.send("Added the current track to the front of the queue".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }

    override fun registerSubcommands() {
    }
}

class FastForward : Command(Category.MUSIC, "fastforward", "fast forward a certain amount of time in a track", "ff") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val time = arguments.getOrNull(0)?.toIntOrNull() ?: -4
        if (manager.player.currentlyPlaying(event.textChannel)) {
            val current = manager.player.playingTrack
            when {
                time <= 0 -> event.channel.send("You need to type a valid number of seconds to skip forward!".tr(event))
                (current.position + (time * 1000)) > current.duration -> {
                    event.channel.send("Time specified was greater than song duration, skipping now..".tr(event))
                    manager.scheduler.manager.nextTrack()
                }
                else -> {
                    event.channel.send("${Emoji.BLK_RGT_POINT_DBL_TRIANGLE} " + "Fast Forwarding **{0}** by **{1}** seconds".tr(event, current.info.title, time))
                    current.position += (time * 1000)
                }
            }
        }
    }

    override fun registerSubcommands() {
    }
}

class Rewind : Command(Category.MUSIC, "rewind", "rewind a track by a specified amount of time", "rw") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val time = arguments.getOrNull(0)?.toIntOrNull() ?: -4
        if (manager.player.currentlyPlaying(event.textChannel)) {
            val current = manager.player.playingTrack
            when {
                time <= 0 -> event.channel.send("You need to type a valid number of seconds to rewind!".tr(event))
                (current.position - (time * 1000)) < 0 -> {
                    event.channel.send("Rewinding past song beginning, restarting now..".tr(event))
                    current.position = 0
                }
                else -> {
                    event.channel.send("${Emoji.BLK_LFT_POINT_DBL_TRIANGLE} " + "Rewinding **{0}** by **{1}** seconds".tr(event, current.info.title, time))
                    current.position -= (time * 1000)
                }
            }
        }
    }

    override fun registerSubcommands() {
    }
}

class Playing : Command(Category.MUSIC, "playing", "shows information about the currently playing track", "np") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (!player.currentlyPlaying(event.textChannel)) return
        val track = manager.scheduler.manager.current!!
        val info = track.track.info
        event.channel.send("**{0}** by *{1}* {2} - added by **{3}**".tr(event, info.title, info.author, track.track.getCurrentTime(), track.author.toUser()!!.withDiscrim()))
    }

    override fun registerSubcommands() {
    }
}

class Volume : Command(Category.MUSIC, "volume", "see and change the volume of the player") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (arguments.size == 0) {
            event.channel.send("The volume of this server's music player is **{0}**%".tr(event, player.volume) + " ${Emoji.PUBLIC_ADDRESS_LOUDSPEAKER}")
            event.channel.send("If the owner of your server or if you are an Ardent patron, type *{0}volume percentage_here* to set the volume".tr(event, event.guild.getPrefix()))
        } else {
            if (!event.member.hasDonationLevel(event.textChannel, DonationLevel.BASIC)) return
            if (!event.member.checkSameChannel(event.textChannel)) return
            if (!event.member.hasOverride(event.textChannel, true, djCommand = true)) return
            val setTo: Int? = arguments[0].replace("%", "").toIntOrNull()
            if (setTo == null || setTo < 0 || setTo > 100) event.channel.send("You need to specify a valid percentage. Example: *{0}volume 99%".tr(event, event.guild.getPrefix()))
            else {
                player.volume = setTo
                event.channel.send("Set player volume to **{0}**%".tr(event, setTo) + " ${Emoji.PUBLIC_ADDRESS_LOUDSPEAKER}")
            }
        }
    }

    override fun registerSubcommands() {
    }
}

class Queue : Command(Category.MUSIC, "queue", "see a list of tracks in the queue", "q") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        if (manager.scheduler.manager.queue.size == 0) {
            event.channel.send("${Emoji.MULTIPLE_MUSICAL_NOTES} " + "There aren't any songs in the queue at the moment!".tr(event))
            return
        }
        val embed = event.member.embed("Current Queue for {0}".tr(event, event.guild.name))
                .appendDescription("*This shows the state of the queue at the current moment*".tr(event)).appendDescription("\n")
        val remaining = manager.scheduler.manager.queue.size - 10
        val show = if (remaining > 0) 9 else remaining + 9
        val queue = manager.scheduler.manager.queueAsList
        (0..show).forEach { number ->
            val track = queue[number]
            embed.appendDescription("\n [**${number + 1}**] : **${track.track.info.title}** by *${track.track.info.author}*\n${track.track.getCurrentTime()} - added by __${track.author.toUser()?.withDiscrim()}__")
        }
        if (remaining > 0) embed.appendDescription("\n\n   " + "*...and {0} tracks after*".tr(event, remaining))
        event.channel.send(embed)
    }

    override fun registerSubcommands() {
    }
}

class ClearQueue : Command(Category.MUSIC, "clearqueue", "clear the queue", "cq") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.hasOverride(event.textChannel, true, djCommand = true)) {
            if (!event.member.checkSameChannel(event.textChannel)) return
            val scheduler = event.guild.getGuildAudioPlayer(event.textChannel).scheduler
            scheduler.autoplay = false
            scheduler.manager.resetQueue()
            event.channel.send("Cleared the queue".tr(event) + " ${Emoji.BALLOT_BOX_WITH_CHECK}")
        }
    }

    override fun registerSubcommands() {
    }
}

class RemoveFrom : Command(Category.MUSIC, "removefrom", "remove all the tracks from the mentioned user or users", "rf") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true)) return
        val mentioned = event.message.mentionedUsers
        if (mentioned.size == 0) {
            event.channel.send("You need to mention at least one person to remove songs from!".tr(event))
        } else {
            mentioned.forEach { removeFrom -> event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.removeFrom(removeFrom) }
            event.channel.send("Successfully removed any queued tracks from **{0}**".tr(event, mentioned.map { it.withDiscrim() }.toList().stringify()) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
        }
    }

    override fun registerSubcommands() {
    }
}