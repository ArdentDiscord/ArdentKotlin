package utils.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import commands.music.*
import main.conn
import main.r
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import translation.tr
import utils.discord.send
import utils.functionality.asPojo

data class DatabaseMusicLibrary(val id: String, var tracks: MutableList<DatabaseTrackObj>, var lastModified: Long = System.currentTimeMillis())

data class DatabaseMusicPlaylist(val id: String, val owner: String, var name: String, var lastModified: Long, var spotifyAlbumId: String?,
                                 val spotifyPlaylistId: String?, val youtubePlaylistUrl: String?, val tracks: MutableList<DatabaseTrackObj>? = null) {
    fun toLocalPlaylist(member: Member): LocalPlaylist {
        return LocalPlaylist(member, this)
    }
}

data class DatabaseTrackObj(val owner: String, val addedAt: Long, val playlistId: String?, val title: String, val author: String, val url: String) {
    fun toDisplayTrack(musicPlaylist: DatabaseMusicPlaylist? = null, lib: DatabaseMusicLibrary? = null): DisplayTrack {
        if (musicPlaylist == null && lib == null) throw IllegalArgumentException("Lib or playlist must be present to convert to display track")
        return DisplayTrack(musicPlaylist?.owner ?: lib!!.id, musicPlaylist?.spotifyAlbumId, null,
                null, title, author)
    }
}

data class LocalTrackObj(val user: String, val owner: String, val playlist: LocalPlaylist?, val spotifyPlaylistId: String?, val spotifyAlbumId: String?, val spotifyTrackId: String?, var track: AudioTrack?, var url: String? = track?.info?.uri) {
    fun getUri(): String? {
        return when {
            spotifyPlaylistId != null -> "https://open.spotify.com/user/${spotifyPlaylistId.split(" ")[0]}/playlist/${spotifyPlaylistId.split(" ")[1]}"
            spotifyAlbumId != null -> "https://open.spotify.com/album/$spotifyAlbumId"
            spotifyTrackId != null -> "https://open.spotify.com/track/$spotifyTrackId"
            url != null -> url
            track != null -> track!!.info.uri
            else -> null
        }
    }
}

data class LinkedPlaylist(val user: String, val playlistId: String)

data class LocalPlaylist(val member: Member, val playlist: DatabaseMusicPlaylist) {
    fun isSpotify(): Boolean = playlist.spotifyAlbumId != null || playlist.spotifyPlaylistId != null
    fun loadTracks(channel: TextChannel, member: Member) {
        if (playlist.tracks == null) return
        else {
            when {
                playlist.spotifyAlbumId != null -> playlist.spotifyAlbumId!!.loadSpotifyAlbum(this.member, channel, { audioTrack, id ->
                    play(channel, member, LocalTrackObj(member.user.id, member.user.id, this, null, playlist.spotifyAlbumId, id, audioTrack))
                })
                playlist.spotifyPlaylistId != null -> playlist.spotifyPlaylistId.loadSpotifyPlaylist(this.member, channel, { audioTrack, id ->
                    play(channel, member, LocalTrackObj(member.user.id, member.user.id,this,playlist.spotifyPlaylistId, null, id, audioTrack))
                })
                playlist.youtubePlaylistUrl != null -> {
                    playlist.youtubePlaylistUrl.loadYoutube(member, channel, playlist)
                }
                else -> {
                    channel.send("Now loading local playlist **{0}** with **{1}** tracks".tr(channel, playlist.name, playlist.tracks.size))
                    playlist.tracks.forEach { track ->
                        track.url.loadYoutube(member, channel, playlist, false, { found ->
                            DEFAULT_TRACK_LOAD_HANDLER(member, channel, found, true, playlist)
                        })
                    }
                }
            }
        }
    }
}

data class DisplayTrack(val owner: String, val playlistId: String?, val spotifyAlbumId: String?, val spotifyTrackId: String?,
                        val title: String, val author: String)

data class DisplayPlaylist(val owner: String, val lastModified: Long, val spotifyAlbumId: String?, val spotifyPlaylistId: String?,
                           val youtubePlaylistUrl: String?, val tracks: MutableList<DisplayTrack> = mutableListOf())

data class DisplayLibrary(val owner: String, val lastModified: Long, val tracks: MutableList<DisplayTrack> = mutableListOf())

data class ServerQueue(val voiceId: String, val channelId: String?, val tracks: List<String>)

fun getPlaylistById(id: String): DatabaseMusicPlaylist? {
    return asPojo(r.table("musicPlaylists").filter(r.hashMap("id", id)).run(conn), DatabaseMusicPlaylist::class.java)
}

fun getMusicLibrary(id: String): DatabaseMusicLibrary? {
    return asPojo(r.table("musicLibraries").filter(r.hashMap("id", id)).run(conn), DatabaseMusicLibrary::class.java)
}