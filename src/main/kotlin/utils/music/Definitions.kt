package utils.music

import com.sedmelluq.discord.lavaplayer.track.AudioTrack

data class MusicLibrary(val userId: String, var tracks: List<DatabaseTrackObj>, var lastModified: Long = System.currentTimeMillis())

data class MusicPlaylist(val id: String, val owner: String, var name: String, var lastModified: Long, var spotifyAlbumId: String?,
                         val spotifyPlaylistId: String?, val youtubePlaylistId: String, val tracks: List<DatabaseTrackObj>? = null)

data class DatabaseTrackObj(val owner: String, val addedAt: Long, val playlistId: String?, val url: String)

data class LocalTrackObj(val user: String, val owner: String, val playlistId: String?, val spotifyId: String?, var track: AudioTrack) {

}

data class LocalPlaylist(val user: String, val playlist: MusicPlaylist) {
    fun isSpotify(): Boolean = playlist.spotifyAlbumId != null || playlist.spotifyPlaylistId != null
    fun getYoutubePlaylist(): String? = playlist.youtubePlaylistId
    fun getSpotifyPlaylist(): String? = playlist.spotifyPlaylistId
    fun getSpotifyAlbum(): String? = playlist.spotifyAlbumId
    fun getTracks(): List<LocalTrackObj>? {
        if (playlist.tracks == null) return null
        else {
            val localTracks = mutableListOf<LocalTrackObj>()
            playlist.tracks.forEach { track -> track. }
        }
    }
}