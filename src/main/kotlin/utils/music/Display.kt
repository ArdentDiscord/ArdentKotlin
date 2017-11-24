package utils.music

import utils.discord.getMusicLibrary


// Getting information about tracks
fun getUserPlaylistInformation(playlistId: String): DisplayPlaylist? {
    val playlist = getPlaylistById(playlistId) ?: return null
    val display = DisplayPlaylist(playlist.owner, playlist.lastModified, playlist.spotifyAlbumId, playlist.spotifyPlaylistId, playlist.youtubePlaylistUrl)
    playlist.tracks.forEach { display.tracks.add(it.toDisplayTrack(playlist)) }
    return display
}

fun getUserLibrary(id: String): DisplayLibrary? {
    val lib = getMusicLibrary(id) ?: return null
    val display = DisplayLibrary(id, lib.lastModified)
    lib.tracks.forEach { display.tracks.add(it.toDisplayTrack(null, lib)) }
    return display
}