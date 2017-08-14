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
import net.dv8tion.jda.core.audio.AudioSendHandler
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import utils.*
import java.time.Instant
import java.util.*
import java.util.concurrent.LinkedBlockingDeque

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

class ArdentTrack(val author: String, val channel: String, var track: AudioTrack) {
    private val votedToSkip = ArrayList<String>()

    fun addSkipVote(user: User): Boolean {
        if (votedToSkip.contains(user.id)) return false
        votedToSkip.add(user.id)
        return true
    }
}


class GuildMusicManager(manager: AudioPlayerManager, channel: TextChannel?, guild: Guild) {
    val scheduler: TrackScheduler
    internal val player: AudioPlayer = manager.createPlayer()

    init {
        scheduler = TrackScheduler(player, channel, guild)
        player.addListener(scheduler)
    }

    internal val sendHandler: AudioPlayerSendHandler get() = AudioPlayerSendHandler(player)
}

class ArdentMusicManager(val player: AudioPlayer, var textChannel: String? = null, var lastPlayedAt: Instant? = null) {
    var queue = LinkedBlockingDeque<ArdentTrack>()
    var current: ArdentTrack? = null

    fun getChannel(): TextChannel? {
        if (textChannel == null) return null
        return textChannel!!.getChannel()
    }

    fun setChannel(channel: TextChannel?) {
        textChannel = channel?.id
    }

    val isTrackCurrentlyPlaying: Boolean get() = current != null

    fun queue(track: ArdentTrack) {
        if (!player.startTrack(track.track, true)) queue.offer(track)
        else current = track
    }

    fun nextTrack() {
        val track = queue.poll()
        if (track != null) {
            val set: Boolean = track.track.position != 0.toLong()
            try {
                player.startTrack(track.track, false)
            } catch (e: Exception) {
                player.startTrack(track.track.makeClone(), false)
            }
            if (set && player.playingTrack != null) player.playingTrack.position = track.track.position
            current = track
        } else {
            player.startTrack(null, false)
            current = null
        }
    }

    fun addToQueue(track: ArdentTrack) {
        queue(track)
        lastPlayedAt = Instant.now()
    }

    fun resetQueue() {
        this.queue = LinkedBlockingDeque<ArdentTrack>()
    }

    fun shuffle() {
        val tracks = ArrayList<ArdentTrack>()
        tracks.addAll(queue)
        Collections.shuffle(tracks)
        queue = LinkedBlockingDeque(tracks)
    }

    fun removeFrom(user: User): Int {
        var count = 0
        val iterator = queue.iterator()
        while (iterator.hasNext()) {
            if (iterator.next().author == user.id) {
                count++
                iterator.remove()
            }
        }
        return count
    }

    val queueAsList: MutableList<ArdentTrack> get() = queue.toMutableList()

    fun addToBeginningOfQueue(track: ArdentTrack?) {
        if (track == null) return
        track.track = track.track.makeClone()
        queue.addFirst(track)
    }
}

class TrackScheduler(player: AudioPlayer, var channel: TextChannel?, val guild: Guild) : AudioEventAdapter() {
    var manager: ArdentMusicManager = ArdentMusicManager(player, channel?.id)

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        if (channel!!.guild.getData().musicSettings.announceNewMusic) {
            val builder = embed("Now Playing: ${track.info.title}", guild.selfMember)
            builder.setThumbnail("https://s-media-cache-ak0.pinimg.com/736x/69/96/5c/69965c2849ec9b7148a5547ce6714735.jpg")
            builder.addField("Title", track.info.title, true)
                    .addField("Author", track.info.author, true)
                    .addField("Duration", track.getDurationFancy(), true)
                    .addField("URL", track.info.uri, true)
                    .addField("Is Stream", track.info.isStream.toString(), true)
            channel?.send(guild.selfMember, builder)
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (manager.queue.size == 0 && guild.getData().musicSettings.autoQueueSongs) {
            val songSearch = spotifyApi.searchTracks(track.info.title.rmCharacters("()").rmCharacters("[]").replace("ft.", "").replace("feat", "").replace("feat.", "")).build()
            try {
                val get = songSearch.get()
                if (get.items.size == 0) {
                    channel?.send(guild.selfMember, "Couldn't find this song in the Spotify database, no autoplay available.")
                    return
                }
                val songId = get.items[0].id
                spotifyApi.getRecommendations().tracks(mutableListOf(songId)).build().get()[0].name.load(guild.selfMember,
                        channel ?: guild.publicChannel, null, false, autoplay = true)
            } catch(e: Exception) {}
        } else manager.nextTrack()
    }

    fun String.rmCharacters(characterSymbol: String): String {
        if (characterSymbol.contains("[]")) {
            return this.replace("\\s*\\[[^\\]]*\\]\\s*".toRegex(), " ")
        } else if (characterSymbol.contains("{}")) {
            return this.replace("\\s*\\{[^\\}]*\\}\\s*".toRegex(), " ")
        } else {
            return this.replace("\\s*\\([^\\)]*\\)\\s*".toRegex(), " ")
        }
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        val ch = guild.audioManager.connectedChannel
        guild.audioManager.closeAudioConnection()
        ch.connect(guild.selfMember, channel!!)
        guild.getGuildAudioPlayer(channel).scheduler.manager.nextTrack()
        channel?.send(guild.selfMember, "${Emoji.BALLOT_BOX_WITH_CHECK} The player got stuck... attempting to skip now now (this is Discord's fault)")

    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        onException(exception)
    }


    private fun onException(exception: FriendlyException) {
        manager.current = null
        manager.nextTrack()
        try {
            manager.getChannel()?.sendMessage("I wasn't able to play that track, skipping... **Reason: **${exception.localizedMessage}")?.queue()
        } catch (e: Exception) {
            e.log()
        }
    }
}

fun AudioTrack.getDurationFancy(): String {
    val length = info.length
    val seconds = (length / 1000).toInt()
    val minutes = seconds / 60
    val hours = minutes / 60
    return "[${String.format("%02d", hours % 60)}:${String.format("%02d", minutes % 60)}:${String.format("%02d", seconds % 60)}]"
}

fun ArrayList<ArdentTrack>.getDuration(): String {
    val length: Long = this.map { it.track.duration }.sum()
    val seconds = (length / 1000).toInt()
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

@Synchronized fun Guild.getGuildAudioPlayer(channel: TextChannel?): GuildMusicManager {
    val guildId = id.toLong()
    var musicManager = managers[guildId]
    if (musicManager == null) {
        musicManager = GuildMusicManager(playerManager, channel, this)
        audioManager.sendingHandler = musicManager.sendHandler
        managers.put(guildId, musicManager)
    } else {
        val ardentMusicManager = musicManager.scheduler.manager
        if (ardentMusicManager.getChannel() == null) {
            ardentMusicManager.setChannel(channel)
        }
    }
    return musicManager
}