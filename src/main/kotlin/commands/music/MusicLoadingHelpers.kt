package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import main.config
import main.playerManager
import main.spotifyApi
import main.youtube
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import obj.Album
import obj.Playlist
import utils.*
import utils.discord.send
import utils.music.LocalTrackObj

val teenParty = mutableListOf<AudioTrack>()
val todaysTopHits = mutableListOf<AudioTrack>()
val rapCaviar = mutableListOf<AudioTrack>()
val powerGaming = mutableListOf<AudioTrack>()
val usTop50 = mutableListOf<AudioTrack>()
val vivaLatino = mutableListOf<AudioTrack>()
val hotCountry = mutableListOf<AudioTrack>()

fun VoiceChannel.connect(textChannel: TextChannel?, complain: Boolean = true): Boolean {
    val audioManager = guild.audioManager
    return try {
        audioManager.openAudioConnection(this)
        true
    } catch (e: Throwable) {
        if (complain) textChannel?.send("${Emoji.CROSS_MARK} " + "I cannot join that voice channel ({0})! Reason: *{1}*".tr(textChannel.guild, name, e.localizedMessage))
        false
    }
}

fun play(channel: TextChannel?, member: Member, track: LocalTrackObj) {
    if (!member.guild.audioManager.isConnected) {
        if (member.voiceState.channel != null) member.guild.audioManager.openAudioConnection(member.voiceChannel())
        else return
    }
    member.guild.musicManager(channel).manager.queue(track)
}

fun Member.checkSameChannel(textChannel: TextChannel?, complain: Boolean = true): Boolean {
    if (voiceState.channel == null) {
        textChannel?.send("${Emoji.CROSS_MARK} " + "You need to be connected to a voice channel".tr(textChannel.guild))
        return false
    }
    if (guild.selfMember.voiceState.channel != voiceState.channel) {
        return voiceState.channel.connect(textChannel, complain)
    }
    return true
}

// Loading Music into bot
fun String.loadSpotifyPlaylist(member: Member, channel: TextChannel, consumerFoundTrack: (String, AudioTrack) -> (Unit)) {
    val split = removePrefix("https://open.spotify.com/user/").split("/playlist/")
    val user = split.getOrNull(0)
    val playlistId = split.getOrNull(1)
    if (user == null || playlistId == null) channel.send("You provided an invalid playlist url!".tr(channel.guild))
    else {
        try {
            val playlist = spotifyApi.playlists.getPlaylist(user, playlistId)!!
            playlist.tracks.items.forEach { track ->
                "${track.track.name} ${track.track.artists[0].name}"
                        .getSingleTrack(channel.guild, { loadedTrack -> consumerFoundTrack.invoke(playlist.name, loadedTrack) }, true)
                channel.sendMessage("Beginning track loading from Spotify playlist **{0}**... This could take a few minutes. I'll add progress bars as the playlist is processed".tr(channel.guild, ((playlist as? Playlist)?.name) ?: (playlist as Album).name))
                        .queue { message ->
                            var percentage = 0.0
                            var current = 0
                            val items = hashMapOf<String, String>()
                            playlist.tracks.items.forEach { items.put(it.track.name, it.track.artists[0].name) }
                            items.forEach { playlistTrack ->
                                "${playlistTrack.key} ${playlistTrack.value}".getSingleTrack(member.guild, { foundTrack ->
                                    current++
                                    val ardentTrack = LocalTrackObj(member.id(), channel.id, foundTrack)
                                    play(channel, member, ardentTrack)
                                    if (current / items.size.toDouble() >= percentage && percentage <= 1) {
                                        percentage += 0.1
                                        when (percentage) {
                                            0.1 -> message.addReaction(Emoji.KEYCAP_DIGIT_ONE.symbol).queue()
                                            0.2 -> message.addReaction(Emoji.KEYCAP_DIGIT_TWO.symbol).queue()
                                            0.3 -> message.addReaction(Emoji.KEYCAP_DIGIT_THREE.symbol).queue()
                                            0.4 -> message.addReaction(Emoji.KEYCAP_DIGIT_FOUR.symbol).queue()
                                            0.5 -> message.addReaction(Emoji.KEYCAP_DIGIT_FIVE.symbol).queue()
                                            0.6 -> message.addReaction(Emoji.KEYCAP_DIGIT_SIX.symbol).queue()
                                            0.7 -> message.addReaction(Emoji.KEYCAP_DIGIT_SEVEN.symbol).queue()
                                            0.8 -> message.addReaction(Emoji.KEYCAP_DIGIT_EIGHT.symbol).queue()
                                            0.9 -> message.addReaction(Emoji.KEYCAP_DIGIT_NINE.symbol).queue()
                                            1.0 -> message.addReaction(Emoji.KEYCAP_TEN.symbol).queue()
                                        }
                                    }
                                    if (current / items.size.toDouble() == 1.0) {
                                        message.addReaction(Emoji.HEAVY_CHECK_MARK.symbol).queue()
                                    }
                                    Thread.sleep(500)
                                })
                            }
                        }
            }
    } catch (e: Exception) {
        channel.send("You provided an invalid playlist url!".tr(channel.guild))
    }
}
}

fun String.getSingleTrack(guild: Guild, foundConsumer: (AudioTrack) -> (Unit), search: Boolean = false, soundcloud: Boolean = false) {
    val string = this
    playerManager.loadItemOrdered(guild.musicManager(null), "${if (search) (if (soundcloud) "scsearch:" else "ytsearch:") else ""}$this", object : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException) {
            if (!soundcloud) string.getSingleTrack(guild, foundConsumer, true, search)
        }

        override fun trackLoaded(track: AudioTrack) {
            foundConsumer.invoke(track)
        }

        override fun noMatches() {
            if (!search) {
                this@getSingleTrack.getSingleTrack(guild, foundConsumer, true, false)
            } else this@getSingleTrack.getSingleTrack(guild, foundConsumer, true, true)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (playlist.isSearchResult && playlist.tracks.size > 0) foundConsumer.invoke(playlist.tracks[0])
        }
    })
}

fun String.load(member: Member, channel: TextChannel?, search: Boolean = false, autoplay: Boolean = false) {
    if (this.startsWith(" ")) this.removePrefix(" ").load(member, channel, search, autoplay)
    else if (member.checkSameChannel(channel)) {
        if (this.contains("spotify.com")) {
            when {
                this.startsWith("https://open.spotify.com/track/") -> {
                    val tr = spotifyApi.tracks.getTrack(this.removePrefix("https://open.spotify.com/track/"))
                    if (tr != null) {
                        tr.name.load(member, channel, search, autoplay)
                        return
                    }
                }
                this.startsWith("https://open.spotify.com/user/") -> {
                    this.loadSpotifyPlaylist(channel
                            ?: member.guild.musicManager(null).scheduler.channel
                            ?: member.guild.textChannels[0], member)
                    return
                }
                this.startsWith("https://open.spotify.com/album/") -> {
                    val album = spotifyApi.albums.getAlbum(this.removePrefix("https://open.spotify.com/album/"))
                    if (album != null) {
                        album.tracks.items.forEach { it.name.load(member, channel, true, false) }
                        return
                    }
                }
            }
            channel?.send("We couldn't find the item you were looking for. Please recheck the link you're providing".tr(member.guild))
        } else {
            playerManager.loadItemOrdered(member.guild.musicManager(channel), this@load, object : AudioLoadResultHandler {
                override fun playlistLoaded(playlist: AudioPlaylist) {
                    when (playlist.isSearchResult) {
                        false -> {
                            channel?.send("${Emoji.BALLOT_BOX_WITH_CHECK} " + "Adding {0} tracks to the queue...".tr(member.guild, playlist.tracks.size))
                            playlist.tracks.forEach { track -> play(channel, member, ArdentTrack(member.id(), channel?.id, track)) }
                        }
                        true -> {
                            if (autoplay) {
                                val track = playlist.tracks[0]
                                channel?.send("${Emoji.BALLOT_BOX_WITH_CHECK} " + "Adding **{0}** by **{1}** to the queue *{2}*..."
                                        .tr(member.guild, track.info.title, track.info.author, track.getDurationFancy())
                                        + " " + "**[Ardent Autoplay]**".tr(member.guild))
                                play(channel, member, ArdentTrack(member.id(), channel?.id, track))
                            } else if (channel != null) {
                                val selectFrom = mutableListOf<String>()
                                val num: Int = if (playlist.tracks.size >= 7) 7 else playlist.tracks.size
                                (1..num)
                                        .map { playlist.tracks[it - 1] }
                                        .map { it.info }
                                        .mapTo(selectFrom) { "${it.title} by *${it.author}*" }
                                channel.selectFromList(member, "Select Song", selectFrom, { response, selectionMessage ->
                                    val track = playlist.tracks[response]
                                    play(channel, member, ArdentTrack(member.id(), channel.id, track))
                                    channel.send("${Emoji.BALLOT_BOX_WITH_CHECK} " + "Adding **{0}** by **{1}** to the queue *{2}*..."
                                            .tr(member.guild, track.info.title, track.info.author, track.getDurationFancy()))
                                    selectionMessage.delete().queue()
                                })
                            }
                        }
                    }
                }

                override fun trackLoaded(track: AudioTrack) {
                    if (track.info.length > (15 * 60 * 1000) && !member.hasDonationLevel(channel ?: member.guild.textChannels[0], DonationLevel.BASIC)) {
                        (channel ?: member.guild.textChannels[0]).send("${Emoji.NO_ENTRY_SIGN} " + "Sorry, but only servers or members with the **Basic** donation level can play songs longer than 15 minutes".tr(member.guild))
                    } else {
                        channel?.send("${Emoji.BALLOT_BOX_WITH_CHECK} " + "Adding **{0} by {1}** to the queue...".tr(member.guild, track.info.title, track.info.author))
                        play(channel, member, ArdentTrack(member.id(), channel?.id, track))
                    }
                }

                override fun noMatches() {
                    if (search) {
                        if (autoplay) channel?.send("I was unable to find a related song...")
                        else channel?.send("I was unable to find a track with that name. Please try again with a different query")
                    } else "ytsearch:${this@load}".load(member, channel, true, autoplay)
                }

                override fun loadFailed(exception: FriendlyException) {
                    if (exception.localizedMessage.contains("Something went wrong") || exception.localizedMessage.contains("503")) {
                        val results = this@load.removePrefix("ytsearch:").searchYoutubeOfficial()
                        if (results == null || results.isEmpty()) channel?.send("This track wasn't found on YouTube!".tr(member.guild))
                        else {
                            if (!autoplay && channel != null) channel.selectFromList(member, "Select Song", results.map { it.first }.toMutableList(), { response, _ -> "https://www.youtube.com/watch?v=${results[response].second}".load(member, channel, false, autoplay) })
                            else "https://www.youtube.com/watch?v=${results[0].second}".load(member, channel, false, autoplay)
                        }
                    } else if (!this@load.contains("spotify")) channel?.send("Something went wrong :/ **Exception**: {0}".tr(member.guild, exception.localizedMessage))
                }
            })
        }
    }
}

/**
 * @return [Pair] with first as the title, and second as the video id
 */
fun String.searchYoutubeOfficial(): List<Pair<String, String>>? {
    return try {
        val search = youtube.search().list("id,snippet")
        search.q = this
        search.key = config.getValue("google")
        search.fields = "items(id/videoId,snippet/title)"
        search.maxResults = 7
        val response = search.execute()
        val items = response.items ?: return null
        items.filter { it != null }.map { Pair(it?.snippet?.title ?: "unavailable", it?.id?.videoId ?: "none") }
    } catch (e: Exception) {
        e.log()
        null
    }
}