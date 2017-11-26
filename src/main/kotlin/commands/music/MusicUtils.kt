package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import main.managers
import main.playerManager
import main.spotifyApi
import main.waiter
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.VoiceChannel
import obj.SimpleTrack
import translation.tr
import utils.discord.*
import utils.functionality.Emoji
import utils.functionality.insert
import utils.functionality.limit
import utils.music.LocalTrackObj
import utils.music.TrackDisplay
import java.util.concurrent.LinkedBlockingDeque
import java.util.concurrent.TimeUnit

class AudioPlayerSendHandler(private val audioPlayer: AudioPlayer) : AudioSendHandler {
    private var lastFrame: AudioFrame? = null

    override fun canProvide(): Boolean {
        lastFrame = audioPlayer.provide()
        return lastFrame != null
    }

    override fun provide20MsAudio(): ByteArray {
        return lastFrame!!.data
    }

    override fun isOpus(): Boolean {
        return true
    }
}

class GuildMusicManager(audioPlayerManager: AudioPlayerManager, var channel: TextChannel?, val guild: Guild) {
    val player: AudioPlayer = audioPlayerManager.createPlayer()
    val scheduler: TrackScheduler = TrackScheduler(this, guild)
    val manager = ArdentMusicManager(player)
    internal val sendHandler: AudioPlayerSendHandler get() = AudioPlayerSendHandler(player)

    init {
        player.addListener(scheduler)
    }
}

class ArdentMusicManager(val player: AudioPlayer) {
    var queue = LinkedBlockingDeque<LocalTrackObj>()
    var current: LocalTrackObj? = null

    fun queue(track: LocalTrackObj) {
        if (!player.startTrack(track.track, true)) queue.offer(track)
        else current = track
    }

    fun skipToNextTrack() {
        val track = queue.poll()
        if (track?.track != null) {
            val set: Boolean = track.track!!.position != 0.toLong()
            try {
                player.startTrack(track.track, false)
            } catch (e: Exception) {
                player.startTrack(track.track!!.makeClone(), false)
            }
            if (set && player.playingTrack != null) player.playingTrack.position = track.track!!.position
            current = track
        } else {
            player.startTrack(null, false)
            current = null
        }
    }

    fun resetQueue() {
        this.queue = LinkedBlockingDeque()
    }

    fun addToBeginningOfQueue(track: LocalTrackObj) {
        track.track = track.track?.makeClone()
        if (track.track != null) queue.addFirst(track)
    }

    fun removeAt(num: Int): Boolean {
        val track = queue.toList().getOrNull(num) ?: return false
        queue.removeFirstOccurrence(track)
        return true
    }
}

class TrackScheduler(val guildMusicManager: GuildMusicManager, val guild: Guild) : AudioEventAdapter() {
    var autoplay = true
    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        autoplay = true
        waiter.executor.schedule({
            if (track.position == 0.toLong() && guild.selfMember.voiceState.inVoiceChannel() && !player.isPaused && player.playingTrack != null && player.playingTrack == track) {
                val queue = guildMusicManager.manager.queue.toList()
                guildMusicManager.player.isPaused = false
                guildMusicManager.manager.resetQueue()
                val current = guildMusicManager.manager.current
                guildMusicManager.manager.skipToNextTrack()
                if (current != null) guildMusicManager.manager.queue(current)
                queue.forEach { guildMusicManager.manager.queue(it) }
            }
        }, 5, TimeUnit.SECONDS)
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (guild.audioManager.isConnected) {
            LoggedTrack(guild.id, track.position / 1000.0 / 60.0 / 60.0).insert("musicPlayed")
            if (guildMusicManager.manager.queue.size > 0) {
                guildMusicManager.manager.skipToNextTrack()
            } else if (player.playingTrack == null && guildMusicManager.manager.queue.size == 0 && autoplay
                    && guild.getData().musicSettings.autoplay && guild.selfMember.voiceState.channel != null) {
                try {
                    val current = guildMusicManager.manager.current!!
                    val recommendation: SimpleTrack = when {
                        current.spotifyTrackId != null -> {
                            val spotifyTrack = spotifyApi.tracks.getTrack(current.spotifyTrackId)!!
                            spotifyApi.browse.getRecommendations(seedTracks = listOf(spotifyTrack.id), seedArtists = spotifyTrack.artists.map { it.id })
                        }
                        current.spotifyAlbumId != null -> {
                            val album = spotifyApi.albums.getAlbum(current.spotifyAlbumId)!!
                            spotifyApi.browse.getRecommendations(seedArtists = album.artists.map { it.id }, seedGenres = album.genres)
                        }
                        current.spotifyPlaylistId != null -> {
                            val split = current.spotifyPlaylistId.split(" :: ")
                            val spotifyPlaylist = spotifyApi.playlists.getPlaylist(split[0], split[1])!!
                            spotifyApi.browse.getRecommendations(seedTracks = spotifyPlaylist.tracks.items.limit(10).map { it.track.id })
                        }
                        else -> {
                            val tempTrack = spotifyApi.search.searchTrack("${track.info.title} " +
                                    if (track.info.author.contains("official", true)
                                            || track.info.author.contains("vevo", true)) track.info.author else "").items[0]
                            spotifyApi.browse.getRecommendations(seedTracks = listOf(tempTrack.id), seedArtists = tempTrack.artists.map { it.id })
                        }
                    }.tracks[0]
                    val channel = guildMusicManager.channel ?: guild.defaultChannel ?: guild.textChannels[0]
                    "https://open.spotify.com/track/${recommendation.id}"
                            .loadSpotifyTrack(guild.selfMember, channel, consumerFoundTrack = { audioTrack, trackId ->
                                play(channel, guild.selfMember, LocalTrackObj(guild.selfMember.user.id, guild.selfMember.user.id, current.playlist,
                                        current.spotifyPlaylistId, current.spotifyAlbumId, recommendation.id, audioTrack))
                            })
                } catch (ignored: Exception) {
                    guildMusicManager.channel?.send("Couldn't find this song in the Spotify database, no autoplay available.".tr(guildMusicManager.channel!!.guild))
                }
                return
            }
        }
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        guildMusicManager.manager.skipToNextTrack()
        guildMusicManager.channel?.send("${Emoji.BALLOT_BOX_WITH_CHECK} " + "Oh no! My voice connection got stuck (#blamediscord) - I'll attempt to skip now now - If you encounter this repeatedly, please make me leave then rejoin the channel!".tr(guildMusicManager.channel!!))
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        onException(exception)
    }

    private fun onException(exception: FriendlyException) {
        guildMusicManager.manager.current = null
        guildMusicManager.manager.skipToNextTrack()
        guildMusicManager.channel?.send("I couldn't play that track, sorry :( - Reason: *{0}*".tr(guild, exception.localizedMessage))
    }
}

fun AudioTrack.getDurationString(): String {
    return info.length.getDurationString()
}

fun Long.getDurationString(): String {
    val seconds = (this / 1000).toInt()
    val minutes = seconds / 60
    val hours = minutes / 60
    return "[${String.format("%02d", hours % 60)}:${String.format("%02d", minutes % 60)}:${String.format("%02d", seconds % 60)}]"
}

fun AudioTrack.getCurrentTime(): String {
    val current = position
    val seconds = (current / 1000).toInt()
    val minutes = seconds / 60
    val hours = minutes / 60

    val length = info.length
    val lengthSeconds = (length / 1000).toInt()
    val lengthMinutes = lengthSeconds / 60
    val lengthHours = lengthMinutes / 60

    return "[${String.format("%02d", hours % 60)}:${String.format("%02d", minutes % 60)}:${String
            .format("%02d", seconds % 60)} / ${String.format("%02d", lengthHours % 60)}:${String
            .format("%02d", lengthMinutes % 60)}:${String.format("%02d", lengthSeconds % 60)}]"
}

@Synchronized
fun Guild.getAudioManager(channel: TextChannel?): GuildMusicManager {
    val guildId = id.toLong()
    var musicManager = managers[guildId]
    if (musicManager == null) {
        musicManager = GuildMusicManager(playerManager, channel, this)
        audioManager.sendingHandler = musicManager.sendHandler
        managers.put(guildId, musicManager)
    } else if (channel != null) musicManager.channel = channel
    return musicManager
}

fun VoiceChannel.connect(textChannel: TextChannel?, complain: Boolean = true): Boolean {
    val audioManager = guild.audioManager
    return try {
        audioManager.openAudioConnection(this)
        true
    } catch (e: Throwable) {
        if (complain) textChannel?.send("${Emoji.CROSS_MARK} " + "I can't join the **{0}** voice channel! Reason: *{1}*".tr(textChannel.guild, name, e.localizedMessage))
        false
    }
}

fun play(channel: TextChannel?, member: Member, track: LocalTrackObj) {
    if (!member.guild.audioManager.isConnected) {
        if (member.voiceState.channel != null) member.guild.audioManager.openAudioConnection(member.voiceState.channel)
        else {
            channel?.send("Unable to join voice channel.. Are you sure you're in one?".tr(member.guild))
            return
        }
    }
    member.guild.getAudioManager(channel).manager.queue(track)
}


fun List<LocalTrackObj>.toTrackDisplay(): List<TrackDisplay> {
    val display = mutableListOf<TrackDisplay>()
    forEach { if (it.track != null) display.add(TrackDisplay(it.track!!.info.title, it.track!!.info.author)) }
    return display
}

fun LocalTrackObj.getInfo(guild: Guild): String {
    return "**{0}** by *{1}* {2} - added by **{3}**"
            .tr(guild, track!!.info.title, track!!.info.author, track!!.getCurrentTime(), getUserById(user)?.toFancyString() ?: "Unknown")
}