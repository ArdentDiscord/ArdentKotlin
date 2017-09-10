package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.wrapper.spotify.exceptions.BadRequestException
import events.Category
import events.Command
import main.playerManager
import main.spotifyApi
import main.waiter
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
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

    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            withHelp("start", "view and select from a curated list of Spotify playlists", event)
                    .withHelp("artist [Artist Name]", "select tracks from an author", event)
                    .displayHelp(event.textChannel, event.member)
            return
        }
        if (arguments[0].equals("start", true)) {
            if (event.member.voiceChannel() == null) event.channel.send("You need to be in a voice channel!")
            else {
                event.channel.selectFromList(event.member, "Select the playlist that you want to listen to", stations, { selection, _ ->
                    if (event.channel.requires(event.member, DonationLevel.BASIC)) {
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
                    }
                })
                return
            }
        }
    }
}

class ArtistSearch : Command(Category.MUSIC, "searchartist", "search top songs by the authors' names", "artistsearch", "artist") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.voiceChannel() == null) {
            event.channel.send("You need to be in a voice channel to use this command!")
            return
        }
        val artistName = arguments.concat()
        if (artistName.isEmpty()) event.channel.send("You need to include an author!")
        else {
            if (event.channel.requires(event.member, DonationLevel.BASIC)) {
                try {
                    val searchResults = spotifyApi.searchArtists(artistName).build().get().items
                    if (searchResults.size == 0) {
                        event.channel.send("No author name was found with that query :(")
                        println(artistName)
                    } else {
                        val options = searchResults
                                .asSequence()
                                .sortedByDescending { it.popularity }
                                .toMutableList().limit(7)
                        event.channel.selectFromList(event.member, "Select the artist you want", options.map { "${it.name} *Popularity: ${it.popularity}*" }.toMutableList(), { option, selectionMessage ->
                            selectionMessage.delete().queue()
                            val artist = options[option]
                            val tracks = spotifyApi.getTopTracksForArtist(artist.id, "us").build().get()
                            if (tracks.size == 0) event.channel.send("No tracks were found with that author :(")
                            else {
                                val embed = event.member.embed("Top tracks for ${artist.name}")
                                        .appendDescription("**Select reactions corresponding to the choices you want to add those songs to the queue**")
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
                                        if (songsToQueue.size == 0) event.channel.send("You didn't select any songs!")
                                        else {
                                            event.channel.send("**Adding __${songsToQueue.size}__ songs to the queue by __${artist.name}__!**")
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
                    }
                } catch (e: BadRequestException) {
                    event.channel.send("You provided an invalid author name :(")
                }
            }
        }
    }
}

class RemoveAt : Command(Category.MUSIC, "removeat", "remove a song from the queue by typing its place", "remove") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.hasOverride(event.textChannel, true, djCommand = true)) {
            if (!event.member.checkSameChannel(event.textChannel)) return
            if (event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.removeAt(arguments.getOrNull(0)?.toIntOrNull())) {
                event.channel.send("Removed song from the queue :thumbs_up:")
            } else event.channel.send("Failed to remove track... check the number in the queue?")
        }
    }
}

class Play : Command(Category.MUSIC, "play", "play a song by its url or search a song to look it up on youtube", "p") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            event.channel.send("You need to specify a URL or search query!")
            return
        }
        arguments.concat().load(event.member, event.textChannel, event.message)
    }
}

class Leave : Command(Category.MUSIC, "leave", "makes me leave the voice channel I'm in") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val guild = event.guild
        val manager = guild.getGuildAudioPlayer(event.textChannel).scheduler.manager
        guild.audioManager.closeAudioConnection()
        manager.resetQueue()
        val vc = guild.audioManager.connectedChannel
        if (vc != null) event.channel.send("Successfully disconnected from **${vc.name}** ${Emoji.MULTIPLE_MUSICAL_NOTES}")
    }
}

class Pause : Command(Category.MUSIC, "pause", "pause the player... what did you think this was gonna do?") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        pause(event, true)
    }
}

class Resume : Command(Category.MUSIC, "resume", "resumes the player. what did you think this was gonna do?") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        pause(event, false)
    }
}

fun pause(event: MessageReceivedEvent, value: Boolean) {
    if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
    val player = event.guild.getGuildAudioPlayer(event.textChannel).player
    if (!player.currentlyPlaying(event.textChannel)) return
    player.isPaused = value
    if (!value) event.channel.send("Resumed playback ${Emoji.WHITE_HEAVY_CHECKMARK}")
    else event.channel.send("Paused playback ${Emoji.WHITE_HEAVY_CHECKMARK}")

}

class Skip : Command(Category.MUSIC, "skip", "skips the currently playing track") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        if (!manager.player.currentlyPlaying(event.textChannel)) return
        val track = manager.scheduler.manager.current!!
        manager.player.playingTrack.position = manager.player.playingTrack.duration - 1
        event.channel.send("Skipped current track: **${track.track.info.title}** by *${track.track.info.author}* ${track.track.getCurrentTime()} - added by **${track.author.toUser()?.withDiscrim()}**")
    }
}

class Stop : Command(Category.MUSIC, "stop", "stop the player and remove all tracks in the queue") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        manager.scheduler.autoplay = false
        manager.player.stopTrack()
        manager.scheduler.manager.resetQueue()
        event.channel.send("Stopped and reset the player ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class SongUrl : Command(Category.MUSIC, "songlink", "get the link for the currently playing track!", "su") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel)) return
        val player = event.guild.getGuildAudioPlayer(event.textChannel).player
        if (!player.currentlyPlaying(event.textChannel)) return
        event.channel.send("**${player.playingTrack.info.title}** by **${player.playingTrack.info.author}**: ${player.playingTrack.info.uri}")
    }
}

class Shuffle : Command(Category.MUSIC, "shuffle", "shuffle the current queue") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.shuffle()
        event.channel.send("Shuffled the current queue ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class Repeat : Command(Category.MUSIC, "repeat", "repeat the track that's currently playing") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (!player.currentlyPlaying(event.textChannel)) return
        manager.scheduler.manager.addToBeginningOfQueue(manager.scheduler.manager.current)
        event.channel.send("Added the current track as Up Next ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class Playing : Command(Category.MUSIC, "playing", "shows information about the currently playing track", "np") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (!player.currentlyPlaying(event.textChannel)) return
        val track = manager.scheduler.manager.current!!
        event.channel.send("**${track.track.info.title}** by *${track.track.info.author}* ${track.track.getCurrentTime()} - added by **${track.author.toUser()?.withDiscrim()}**")
    }
}

class Volume : Command(Category.MUSIC, "volume", "see and change the volume of the player") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (arguments.size == 0) {
            event.channel.send("The volume of this server's music player is **${player.volume}**% ${Emoji.PUBLIC_ADDRESS_LOUDSPEAKER}")
            event.channel.send("If the owner of your server or if you are an Ardent patron, type *${event.guild.getPrefix()}volume percentage_here* to set " +
                    "the volume")
            return
        }
        if (!event.member.hasDonationLevel(event.textChannel, DonationLevel.BASIC)) return
        if (!event.member.checkSameChannel(event.textChannel)) return
        val setTo: Int? = arguments[0].replace("%", "").toIntOrNull()
        if (setTo == null || setTo < 0 || setTo > 100) event.channel.send("You need to specify a valid percentage. Example: *${event.guild.getPrefix()}volume 99%")
        else {
            player.volume = setTo
            event.channel.send("Set player volume to **$setTo**% ${Emoji.PUBLIC_ADDRESS_LOUDSPEAKER}")
        }
    }
}

class Queue : Command(Category.MUSIC, "queue", "see a list of tracks in the queue", "q") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        if (manager.scheduler.manager.queue.size == 0) {
            event.channel.send("${Emoji.MULTIPLE_MUSICAL_NOTES} There aren't any songs in the queue at the moment!")
            return
        }
        val embed = event.member.embed("Current Queue: ${event.guild.name}")
                .appendDescription("*This shows the state of the queue at the current moment*\n")
        val remaining = manager.scheduler.manager.queue.size - 10
        val show: Int
        show = if (remaining > 0) 9
        else remaining + 9
        val queue = manager.scheduler.manager.queueAsList
        (0..show).forEach { number ->
            val track = queue[number]
            embed.appendDescription("\n [**${number + 1}**] : **${track.track.info.title}** by *${track.track.info.author}*\n${track.track.getCurrentTime()} - added by __${track.author.toUser()?.withDiscrim()}__")
        }
        if (remaining > 0) embed.appendDescription("\n\n   *...and $remaining tracks after*")
        event.channel.send(embed)
    }
}

class ClearQueue : Command(Category.MUSIC, "clearqueue", "clear the queue", "cq") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.hasOverride(event.textChannel, true, djCommand = true)) {
            if (!event.member.checkSameChannel(event.textChannel)) return
            val scheduler = event.guild.getGuildAudioPlayer(event.textChannel).scheduler
            scheduler.autoplay = false
            scheduler.manager.resetQueue()
            event.channel.send("Cleared the queue ${Emoji.BALLOT_BOX_WITH_CHECK}")
        }
    }
}

class RemoveFrom : Command(Category.MUSIC, "removefrom", "remove all the tracks from the mentioned user or users", "rf") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true)) return
        val mentioned = event.message.mentionedUsers
        if (mentioned.size == 0) {
            event.channel.send("You need to mention at least one person to remove songs from!")
        } else {
            mentioned.forEach { removeFrom -> event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.removeFrom(removeFrom) }
            event.channel.send("Successfully removed any queued tracks from **${mentioned.map { it.withDiscrim() }.toList().stringify()}** ${Emoji.WHITE_HEAVY_CHECKMARK}")
        }
    }
}

fun VoiceChannel.connect(textChannel: TextChannel?) {
    val audioManager = guild.audioManager
    try {
        audioManager.openAudioConnection(this)
    } catch (e: Throwable) {
        textChannel?.send("${Emoji.CROSS_MARK} I cannot join that voice channel ($name)! Reason: *${e.localizedMessage}*")
    }
}

fun play(channel: TextChannel, member: Member, track: ArdentTrack) {
    if (!member.guild.audioManager.isConnected) {
        if (member.voiceState.channel != null) member.guild.audioManager.openAudioConnection(member.voiceChannel())
        else return
    }
    member.guild.getGuildAudioPlayer(channel).scheduler.manager.addToQueue(track)
}

fun play(member: Member, guild: Guild, channel: VoiceChannel, musicManager: GuildMusicManager, track: AudioTrack, textChannel: TextChannel?) {
    if (!guild.audioManager.isConnected) {
        guild.audioManager.openAudioConnection(channel)
    }
    musicManager.scheduler.manager.addToQueue(ArdentTrack(member.user.id, textChannel?.id, track))
}

fun Member.checkSameChannel(textChannel: TextChannel): Boolean {
    if (voiceState.channel == null) {
        textChannel.send("${Emoji.CROSS_MARK} You need to be connected to a voice channel")
        return false
    }
    if (guild.selfMember.voiceState.channel == null) {
        guild.audioManager.closeAudioConnection()
        voiceState.channel.connect(textChannel)
        return false
    }
    if (guild.selfMember.voiceState.channel != voiceState.channel) {
        textChannel.send("${Emoji.CROSS_MARK} We need to be connected to the **same** voice channel")
        return false
    }
    return true
}

fun String.getSingleTrack(guild: Guild, foundConsumer: (AudioTrack) -> (Unit), soundcloud: Boolean = false) {
    val string = this
    playerManager.loadItemOrdered(guild.getGuildAudioPlayer(null), "${if (soundcloud) "scsearch" else "ytsearch"}:$this", object : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException) {
            string.getSingleTrack(guild, foundConsumer, true)
        }

        override fun trackLoaded(track: AudioTrack) {
            foundConsumer.invoke(track)
        }

        override fun noMatches() {}
        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (playlist.isSearchResult) foundConsumer.invoke(playlist.tracks[0])
        }
    })
}

fun String.load(member: Member, textChannel: TextChannel?, message: Message?, search: Boolean = false, radioName: String? = null, autoplay: Boolean = false, guild: Guild? = null) {
    if (member.voiceState.channel == null) {
        textChannel?.send("${Emoji.CROSS_MARK} You need to be connected to a voice channel")
        return
    }
    if (guild != null && !guild.selfMember.voiceState.inVoiceChannel()) {
        member.voiceState.channel.connect(textChannel)
    }
    val musicManager = member.guild.getGuildAudioPlayer(textChannel)
    if (this.contains("spotify")) {
        try {
            val tr = spotifyApi.getTrack(this.removePrefix("https://open.spotify.com/track/")).build().get()
            if (tr != null && tr.name != null) {
                tr.name.load(member, textChannel, message, search, radioName, autoplay)
                return
            } else {
                textChannel?.send("You specified an invalid url.. Please try again after checking the link")
                return
            }
        } catch (e: BadRequestException) {
            if (textChannel != null && member.hasDonationLevel(textChannel, DonationLevel.INTERMEDIATE, false)) {
                this.getSpotifyPlaylist(textChannel, member)
                return
            }
        }
    }
    playerManager.loadItemOrdered(musicManager, this, object : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException) {
            textChannel?.send("There was an error loading the track: *${exception.localizedMessage}")
        }

        override fun trackLoaded(track: AudioTrack) {
            if (textChannel != null && track.info.length > (15 * 60 * 1000) && !member.hasDonationLevel(textChannel, DonationLevel.BASIC)) {
                textChannel.send("${Emoji.NO_ENTRY_SIGN} Sorry, but only servers or members with the **Basic** donation " +
                        "level can play songs longer than 15 minutes")
                return
            }
            if (radioName == null) textChannel?.send("${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue...")
            else textChannel?.send("${Emoji.MULTIPLE_MUSICAL_NOTES} Starting to play the radio station **$radioName**...")
            play(member, member.guild, member.voiceChannel()!!, musicManager, track, textChannel)
        }

        override fun noMatches() {
            println("No matches for $this")
            if (search) {
                if (autoplay) {
                    textChannel?.send("I was unable to find a related song...")
                } else textChannel?.send("I was unable to find a track with that name. Please try again with a different query")
            } else ("ytsearch: ${this@load}").load(member, textChannel, message, true, autoplay = autoplay)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (!playlist.isSearchResult) {
                textChannel?.send("${Emoji.BALLOT_BOX_WITH_CHECK} Adding ${playlist.tracks.size} tracks to the queue...")
                try {
                    playlist.tracks.forEach { play(member, member.guild, member.voiceChannel()!!, musicManager, it, textChannel) }
                } catch (e: Exception) {
                    textChannel?.send("Failed to play tracks: **Error:** ${e.localizedMessage}")
                }
                return
            }
            if (autoplay) {
                if (playlist.tracks.size > 0) {
                    var cont = true
                    var current = 0
                    while (cont) {
                        val track = playlist.tracks[current]
                        if (track.duration < 20 * 1000 * 60) {
                            cont = false
                            play(member, member.guild, member.voiceChannel()!!, musicManager, track, textChannel)
                            textChannel?.send("${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue... **[Ardent Autoplay]**")
                        }
                        current++
                    }
                }
            } else {
                val selectFrom = mutableListOf<String>()
                val num: Int = if (playlist.tracks.size >= 7) 7
                else playlist.tracks.size
                (1..num)
                        .map { playlist.tracks[it - 1] }
                        .map { it.info }
                        .mapTo(selectFrom) { "${it.title} by *${it.author}*" }
                textChannel?.selectFromList(member, "Select Song", selectFrom, { response, _ ->
                    val track = playlist.tracks[response]
                    play(member, member.guild, member.voiceChannel()!!, musicManager, track, textChannel)
                    textChannel.send("${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue...")
                })
            }
        }
    })
}