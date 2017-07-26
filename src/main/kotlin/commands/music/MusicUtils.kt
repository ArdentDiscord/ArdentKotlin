package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import com.sedmelluq.discord.lavaplayer.track.playback.AudioFrame
import net.dv8tion.jda.core.audio.AudioSendHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioTrackEndReason
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.entities.MessageChannel
import com.sedmelluq.discord.lavaplayer.player.event.AudioEventAdapter
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import java.util.concurrent.LinkedBlockingQueue
import java.time.Instant
import net.dv8tion.jda.core.entities.TextChannel
import com.sedmelluq.discord.lavaplayer.player.AudioPlayerManager
import jdk.nashorn.internal.objects.NativeDate
import main.managers
import main.playerManager
import net.dv8tion.jda.core.entities.Guild
import utils.embed
import utils.getChannel
import utils.getData
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

class ArdentTrack(val author: String, val channel: String, val track: AudioTrack) {
    private val votedToSkip = ArrayList<String>()

    fun addSkipVote(user: User): Boolean {
        if (votedToSkip.contains(user.id)) return false
        votedToSkip.add(user.id)
        return true
    }
}


class GuildMusicManager(manager: AudioPlayerManager, channel: TextChannel?) {
    val scheduler: TrackScheduler
    internal val player: AudioPlayer = manager.createPlayer()

    init {
        scheduler = TrackScheduler(player, channel)
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
            player.startTrack(track.track, false)
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
        assert(track != null)
        queue.addFirst(track)
    }
}

class TrackScheduler(player: AudioPlayer, var channel: TextChannel?) : AudioEventAdapter() {
    var manager: ArdentMusicManager = ArdentMusicManager(player, channel?.id)

    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        if (channel!!.guild.getData().musicSettings.announceNewMusic) {
            val builder = embed("Now Playing: ${track.info.title}", channel!!.guild.selfMember)
            builder.setThumbnail("https://s-media-cache-ak0.pinimg.com/736x/69/96/5c/69965c2849ec9b7148a5547ce6714735.jpg")
            builder.addField("Title", track.info.title, true)
                    .addField("Author", track.info.author, true)
                    .addField("Duration", track.getDurationFancy(), true)
                    .addField("URL", track.info.uri, true)
                    .addField("Is Stream", track.info.isStream.toString(), true)
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        if (endReason.mayStartNext) {
            manager.nextTrack()
        }
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        manager.nextTrack()
        onException(FriendlyException("Track got stuck", FriendlyException.Severity.COMMON, Exception()))
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        onException(exception)
    }


    private fun onException(exception: FriendlyException) {
        manager.current = null
        manager.nextTrack()
        try {
            manager.getChannel()?.sendMessage("I wasn't able to play that track, skipping... **Reason: **${exception.localizedMessage}")?.queue()
        } catch (ex: Exception) {
            ex.printStackTrace()
        }
    }
}

fun AudioTrack.getDurationFancy(): String {
    val length = info.length
    val seconds = (length / 1000).toInt()
    val minutes = seconds / 60
    val hours = minutes / 60
    if (NativeDate.getHours(this) < 0) return "[Live Stream]"
    else return "[${String.format("%02d", hours % 60)}:${String.format("%02d", minutes % 60)}:${String.format("%02d", seconds % 60)}]"
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
        musicManager = GuildMusicManager(playerManager, channel)
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