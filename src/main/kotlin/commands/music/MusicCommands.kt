package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import events.Category
import events.Command
import main.playerManager
import main.spotifyApi
import main.waiter
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import obj.BadRequestException
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

    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            withHelp("start", "view and select from a curated list of Spotify playlists", event)
                    .withHelp("artist [Artist Name]", "select tracks from an author", event)
                    .displayHelp(event.textChannel, event.member)
            return
        }
        if (arguments[0].equals("start", true)) {
            if (event.member.voiceChannel() == null) event.channel.send("You need to be in a voice channel!".tr(event))
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

class ArtistSearch : Command(Category.MUSIC, "searchartist", "search top songs by the authors' names", "artistsearch", "artist", "searchauthor", "author") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
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
                } catch (e: BadRequestException) {
                    event.channel.send("You provided an invalid author name :(".tr(event))
                }
            }
        }
    }
}

class RemoveAt : Command(Category.MUSIC, "removeat", "remove a song from the queue by typing its place", "remove") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.hasOverride(event.textChannel, true, djCommand = true)) {
            if (!event.member.checkSameChannel(event.textChannel)) return
            if (event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.removeAt(arguments.getOrNull(0)?.toIntOrNull()?.minus(1))) {
                event.channel.send("Removed song from the queue".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
            } else event.channel.send("Failed to remove track... check the number in the queue?".tr(event))
        }
    }
}

class Play : Command(Category.MUSIC, "play", "play a song by its url or search a song to look it up on youtube", "p") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            event.channel.send("You need to specify a URL or search query!".tr(event))
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
        if (vc != null) event.channel.send("Successfully disconnected from **{0}**".tr(event, vc.name) + " ${Emoji.MULTIPLE_MUSICAL_NOTES}")
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
    if (!value) event.channel.send("Resumed playback".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
    else event.channel.send("Paused playback".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")

}

class Skip : Command(Category.MUSIC, "skip", "skips the currently playing track") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        if (!manager.player.currentlyPlaying(event.textChannel)) return
        val track = manager.scheduler.manager.current!!
        manager.player.playingTrack.position = manager.player.playingTrack.duration - 1
        event.channel.send("Skipped current track: **{0}** by *{1}* {2} - added by **{3}**".tr(event, track.track.info.title, track.track.info.author, track.track.getCurrentTime(), track.author.toUser()?.withDiscrim() ?: "unable to determine"))
    }
}

class Stop : Command(Category.MUSIC, "stop", "stop the player and remove all tracks in the queue") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        manager.scheduler.autoplay = false
        manager.scheduler.manager.resetQueue()
        manager.player.stopTrack()
        event.channel.send("Stopped and reset the player".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class SongUrl : Command(Category.MUSIC, "songlink", "get the link for the currently playing track!", "su") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel)) return
        val player = event.guild.getGuildAudioPlayer(event.textChannel).player
        if (!player.currentlyPlaying(event.textChannel)) return
        event.channel.send("**{0}** by **{1}**: {2}".tr(event, player.playingTrack.info.title, player.playingTrack.info.author, player.playingTrack.info.uri))
    }
}

class Shuffle : Command(Category.MUSIC, "shuffle", "shuffle the current queue") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.shuffle()
        event.channel.send("Shuffled the current queue".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class Repeat : Command(Category.MUSIC, "repeat", "repeat the track that's currently playing") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (!player.currentlyPlaying(event.textChannel)) return
        manager.scheduler.manager.addToBeginningOfQueue(manager.scheduler.manager.current)
        event.channel.send("Added the current track to the front of the queue".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class FastForward : Command(Category.MUSIC, "fastforward", "fast forward a certain amount of time in a track", "ff") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
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
}

class Rewind : Command(Category.MUSIC, "rewind", "rewind a track by a specified amount of time", "rw") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
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
}

class Playing : Command(Category.MUSIC, "playing", "shows information about the currently playing track", "np") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (!player.currentlyPlaying(event.textChannel)) return
        val track = manager.scheduler.manager.current!!
        val info = track.track.info
        event.channel.send("**{0}** by *{1}* {2} - added by **{3}**".tr(event, info.title, info.author, track.track.getCurrentTime(), track.author.toUser()!!.withDiscrim()))
    }
}

class Volume : Command(Category.MUSIC, "volume", "see and change the volume of the player") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (arguments.size == 0) {
            event.channel.send("The volume of this server's music player is **{0}**%".tr(event, player.volume) + " ${Emoji.PUBLIC_ADDRESS_LOUDSPEAKER}")
            event.channel.send("If the owner of your server or if you are an Ardent patron, type *{0}volume percentage_here* to set the volume".tr(event, event.guild.getPrefix()))
            return
        }
        if (!event.member.hasDonationLevel(event.textChannel, DonationLevel.BASIC)) return
        if (!event.member.checkSameChannel(event.textChannel)) return
        val setTo: Int? = arguments[0].replace("%", "").toIntOrNull()
        if (setTo == null || setTo < 0 || setTo > 100) event.channel.send("You need to specify a valid percentage. Example: *{0}volume 99%".tr(event, event.guild.getPrefix()))
        else {
            player.volume = setTo
            event.channel.send("Set player volume to **{0}**%".tr(event, setTo) + " ${Emoji.PUBLIC_ADDRESS_LOUDSPEAKER}")
        }
    }
}

class Queue : Command(Category.MUSIC, "queue", "see a list of tracks in the queue", "q") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        if (manager.scheduler.manager.queue.size == 0) {
            event.channel.send("${Emoji.MULTIPLE_MUSICAL_NOTES} " + "There aren't any songs in the queue at the moment!".tr(event))
            return
        }
        val embed = event.member.embed("Current Queue for {0}".tr(event, event.guild.name))
                .appendDescription("*This shows the state of the queue at the current moment*".tr(event)).appendDescription("\n")
        val remaining = manager.scheduler.manager.queue.size - 10
        val show: Int
        show = if (remaining > 0) 9
        else remaining + 9
        val queue = manager.scheduler.manager.queueAsList
        (0..show).forEach { number ->
            val track = queue[number]
            embed.appendDescription("\n [**${number + 1}**] : **${track.track.info.title}** by *${track.track.info.author}*\n${track.track.getCurrentTime()} - added by __${track.author.toUser()?.withDiscrim()}__")
        }
        if (remaining > 0) embed.appendDescription("\n\n   " + "*...and {0} tracks after*".tr(event, remaining))
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
            event.channel.send("Cleared the queue".tr(event) + " ${Emoji.BALLOT_BOX_WITH_CHECK}")
        }
    }
}

class RemoveFrom : Command(Category.MUSIC, "removefrom", "remove all the tracks from the mentioned user or users", "rf") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true)) return
        val mentioned = event.message.mentionedUsers
        if (mentioned.size == 0) {
            event.channel.send("You need to mention at least one person to remove songs from!".tr(event))
        } else {
            mentioned.forEach { removeFrom -> event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.removeFrom(removeFrom) }
            event.channel.send("Successfully removed any queued tracks from **{0}**".tr(event, mentioned.map { it.withDiscrim() }.toList().stringify()) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
        }
    }
}

fun VoiceChannel.connect(textChannel: TextChannel?) {
    val audioManager = guild.audioManager
    try {
        audioManager.openAudioConnection(this)
    } catch (e: Throwable) {
        textChannel?.send("${Emoji.CROSS_MARK} " + "I cannot join that voice channel ({0})! Reason: *{1}*".tr(textChannel.guild, name, e.localizedMessage))
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
        textChannel.send("${Emoji.CROSS_MARK} " + "You need to be connected to a voice channel".tr(textChannel.guild))
        return false
    }
    if (guild.selfMember.voiceState.channel == null) {
        guild.audioManager.closeAudioConnection()
        voiceState.channel.connect(textChannel)
        return false
    }
    if (guild.selfMember.voiceState.channel != voiceState.channel) {
        textChannel.send("${Emoji.CROSS_MARK} " + "We need to be connected to the **same** voice channel".tr(textChannel.guild))
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
        textChannel?.send("${Emoji.CROSS_MARK} " + "You need to be connected to a voice channel".tr(member.guild))
        return
    }
    if (guild != null && !guild.selfMember.voiceState.inVoiceChannel()) {
        member.voiceState.channel.connect(textChannel)
    }
    val musicManager = member.guild.getGuildAudioPlayer(textChannel)
    if (this.contains("spotify")) {
        try {
            val tr = spotifyApi.tracks.getTrack(this.removePrefix("https://open.spotify.com/track/"))
            tr.name.load(member, textChannel, message, search, radioName, autoplay)
        } catch (e: BadRequestException) {
            if (textChannel != null && member.hasDonationLevel(textChannel, DonationLevel.INTERMEDIATE, false)) {
                this.getSpotifyPlaylist(textChannel, member)
                return
            }
        }
    }
    playerManager.loadItemOrdered(musicManager, this, object : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException) {
            if (exception.message?.contains("Something went wrong when looking up the track") == true) {
                val results = this@load.removePrefix("ytsearch: ").searchYoutubeOfficial()
                if (results == null || results.isEmpty()) textChannel?.send("This track wasn't found on YouTube!".tr(member.guild))
                else {
                    textChannel?.selectFromList(member, "Select Song", results.map { it.first }.toMutableList(), { response, _ ->
                        "https://www.youtube.com/watch?v=${results[response].second}".load(member, textChannel, message, false, guild = guild)
                    })
                }
            } else {
                textChannel?.send("Something went wrong :/ **Exception**: {0}".tr(member.guild, exception.localizedMessage))
            }
        }

        override fun trackLoaded(track: AudioTrack) {
            if (textChannel != null && track.info.length > (15 * 60 * 1000) && !member.hasDonationLevel(textChannel, DonationLevel.BASIC)) {
                textChannel.send("${Emoji.NO_ENTRY_SIGN} " + "Sorry, but only servers or members with the **Basic** donation level can play songs longer than 15 minutes".tr(member.guild))
                return
            }
            if (radioName == null) textChannel?.send("${Emoji.BALLOT_BOX_WITH_CHECK} " + "Adding **{0} by {1}** to the queue...".tr(member.guild, track.info.title, track.info.author))
            else textChannel?.send("${Emoji.MULTIPLE_MUSICAL_NOTES} " + "Starting to play the radio station **{0}**...".tr(member.guild, radioName))
            play(member, member.guild, member.voiceChannel()!!, musicManager, track, textChannel)
        }

        override fun noMatches() {
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
                textChannel?.selectFromList(member, "Select Song", selectFrom, { response, selectionMessage ->
                    selectionMessage.delete().queue()
                    val track = playlist.tracks[response]
                    play(member, member.guild, member.voiceChannel()!!, musicManager, track, textChannel)
                    textChannel.send("${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue...")
                })
            }
        }
    })
}