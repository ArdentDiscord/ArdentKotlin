package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import main.config
import main.playerManager
import main.spotifyApi
import main.youtube
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import translation.tr
import utils.discord.send
import utils.functionality.Emoji
import utils.functionality.log
import utils.functionality.selectFromList
import utils.music.DatabaseMusicPlaylist
import utils.music.LocalTrackObj


// Loading music directly (for use in playing)
val DEFAULT_TRACK_LOAD_HANDLER: (Member, TextChannel, AudioTrack, Boolean, DatabaseMusicPlaylist?) -> Unit =
        { member, channel, track, isQuiet, musicPlaylist ->
            if (!isQuiet) channel.send("${Emoji.BALLOT_BOX_WITH_CHECK} " + "Adding **{0}** by **{1}** to the queue *{2}*...".tr(member.guild, track.info.title, track.info.author, track.getDurationString()))
            play(channel, member, LocalTrackObj(member.user.id, musicPlaylist?.owner ?: member.user.id, musicPlaylist?.toLocalPlaylist(member),
                    musicPlaylist?.spotifyPlaylistId, musicPlaylist?.spotifyAlbumId, null, track))
        }

val DEFAULT_PLAYLIST_LOAD_HANDLER: (Member, TextChannel, AudioPlaylist, Boolean, DatabaseMusicPlaylist?) -> Unit =
        { member, channel, tracksPlaylist, isQuiet, musicPlaylist ->
            if (!isQuiet) channel.send("Loading YouTube playlist **{0}** *{1} tracks*".tr(channel, tracksPlaylist.name, tracksPlaylist.tracks.size))
            tracksPlaylist.tracks.forEach { track ->
                play(channel, member, LocalTrackObj(member.user.id, musicPlaylist?.owner ?: member.user.id, musicPlaylist?.toLocalPlaylist(member),
                        musicPlaylist?.spotifyPlaylistId, musicPlaylist?.spotifyAlbumId, null, track))
            }
        }

fun String.load(member: Member, channel: TextChannel, consumerFoundTrack: (AudioTrack, String?) -> Unit) {
    when {
        this.startsWith("https://open.spotify.com/album/") -> loadSpotifyAlbum(member, channel, consumerFoundTrack)
        this.startsWith("https://open.spotify.com/track/") -> loadSpotifyTrack(member, channel, consumerFoundTrack)
        this.startsWith("https://open.spotify.com/user/") -> loadSpotifyPlaylist(member, channel, consumerFoundTrack)
        else -> loadYoutube(member, channel, consumer = { consumerFoundTrack.invoke(it, null) })
    }
}

fun String.loadYoutube(member: Member, channel: TextChannel, musicPlaylist: DatabaseMusicPlaylist? = null, search: Boolean = false, consumer: ((AudioTrack) -> Unit)? = null) {
    val autoplay = member == member.guild.selfMember
    playerManager.loadItemOrdered(member.guild.getAudioManager(channel), this, object : AudioLoadResultHandler {
        override fun trackLoaded(track: AudioTrack) {
            if (consumer != null) consumer.invoke(track)
            else DEFAULT_TRACK_LOAD_HANDLER(member, channel, track, false, musicPlaylist)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (playlist.isSearchResult) {
                if (consumer != null) {
                    consumer.invoke(playlist.tracks[0])
                    return
                }
                if (autoplay) {
                    val track = playlist.tracks[0]
                    channel.send("${Emoji.BALLOT_BOX_WITH_CHECK} " + "[**Ardent Autoplay**]".tr(channel) + " " + "Adding **{0}** by **{1}** to the queue *{2}*...".tr(member.guild, track.info.title, track.info.author, track.getDurationString()))
                    DEFAULT_TRACK_LOAD_HANDLER(member, channel, track, true, musicPlaylist)
                } else {
                    val selectFrom = mutableListOf<String>()
                    val num: Int = if (playlist.tracks.size >= 7) 7 else playlist.tracks.size
                    (1..num)
                            .map { playlist.tracks[it - 1] }
                            .map { it.info }
                            .mapTo(selectFrom) { "${it.title} by *${it.author}*" }
                    channel.selectFromList(member, "Select Song", selectFrom, { response, selectionMessage ->
                        val track = playlist.tracks[response]
                        DEFAULT_TRACK_LOAD_HANDLER(member, channel, track, false, musicPlaylist)
                    })
                }
            } else {
                DEFAULT_PLAYLIST_LOAD_HANDLER(member, channel, playlist, false, musicPlaylist)
            }
        }

        override fun loadFailed(exception: FriendlyException) {
            if (exception.localizedMessage.contains("Something went wrong") || exception.localizedMessage.contains("503")) {
                val results = this@loadYoutube.removePrefix("ytsearch:").searchYoutubeOfficialApi()
                if (results == null || results.isEmpty()) channel.send("This track wasn't found on YouTube!".tr(member.guild))
                else {
                    if (!autoplay) channel.selectFromList(member, "Select Song", results.map { it.first }.toMutableList(),
                            { response, _ ->
                                "https://www.youtube.com/watch?v=${results[response].second}"
                                        .loadYoutube(member, channel, musicPlaylist, false)
                            })
                    else "https://www.youtube.com/watch?v=${results[0].second}".loadYoutube(member, channel, musicPlaylist, false)
                }
            } else channel.send("Something went wrong :/ **Exception**: {0}".tr(member.guild, exception.localizedMessage))
        }

        override fun noMatches() {
            if (search) {
                if (!autoplay) channel.send("I was unable to find a track with that name. Please try again with a different query")
            } else "ytsearch:${this@loadYoutube}".loadYoutube(member, channel, musicPlaylist, true, consumer = consumer)
        }
    })
}


fun String.loadSpotifyTrack(member: Member, channel: TextChannel, consumerFoundTrack: (AudioTrack, String) -> Unit) {
    try {
        val track = spotifyApi.tracks.getTrack(this.removePrefix("https://open.spotify.com/track/"))
        track?.name?.getSingleTrack(member, channel, { _, _, audioTrack -> consumerFoundTrack.invoke(audioTrack, track.id) },
                false, false)
    } catch (e: Exception) {
        channel.send("You need to specify a valid Spotify track id!".tr(channel.guild))
    }
}

fun String.loadSpotifyAlbum(member: Member, channel: TextChannel, consumerFoundTrack: (AudioTrack, String) -> Unit) {
    val albumId = removePrefix("https://open.spotify.com/album/")
    try {
        val album = spotifyApi.albums.getAlbum(albumId)!!
        channel.sendMessage("Beginning track loading from Spotify album **{0}**... This could take a few minutes!".tr(channel.guild, album.name))
        album.tracks.items.forEach { track ->
            "${track.name} ${track.artists[0].name}"
                    .getSingleTrack(member, channel, { _, _, loaded -> consumerFoundTrack.invoke(loaded, track.id) }, true)

        }
    } catch (e: Exception) {
        channel.send("You provided an invalid album url!".tr(channel.guild))
    }
}


fun String.loadSpotifyPlaylist(member: Member, channel: TextChannel, consumerFoundTrack: (AudioTrack, String) -> (Unit)) {
    val split = removePrefix("https://open.spotify.com/user/").split("/playlist/")
    val user = split.getOrNull(0)
    val playlistId = split.getOrNull(1)
    if (user == null || playlistId == null) channel.send("You provided an invalid playlist url!".tr(channel.guild))
    else {
        try {
            val playlist = spotifyApi.playlists.getPlaylist(user, playlistId)!!
            channel.sendMessage("Beginning track loading from Spotify playlist **{0}**... This could take a few minutes!".tr(channel.guild, playlist.name))
            playlist.tracks.items.forEach { track ->
                "${track.track.name} ${track.track.artists[0].name}"
                        .getSingleTrack(member, channel, { _, _, loaded -> consumerFoundTrack.invoke(loaded, track.track.id) }, true)

            }
        } catch (e: Exception) {
            channel.send("You provided an invalid playlist url!".tr(channel.guild))
        }
    }
}

fun String.getSingleTrack(member: Member, channel: TextChannel, foundConsumer: (Member, TextChannel, AudioTrack) -> (Unit), search: Boolean = false, soundcloud: Boolean = false) {
    val string = this
    playerManager.loadItemOrdered(member.guild.getAudioManager(null), "${if (search) (if (soundcloud) "scsearch:" else "ytsearch:") else ""}$this", object : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException) {
            if (!soundcloud) string.getSingleTrack(member, channel, foundConsumer, true, search)
        }

        override fun trackLoaded(track: AudioTrack) {
            foundConsumer.invoke(member, channel, track)
        }

        override fun noMatches() {
            if (!search) {
                this@getSingleTrack.getSingleTrack(member, channel, foundConsumer, true, false)
            } else this@getSingleTrack.getSingleTrack(member, channel, foundConsumer, true, true)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (playlist.isSearchResult && playlist.tracks.size > 0) foundConsumer.invoke(member, channel, playlist.tracks[0])
        }
    })
}


/**
 * @return [Pair] with first as the title, and second as the video id
 */
fun String.searchYoutubeOfficialApi(): List<Pair<String, String>>? {
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