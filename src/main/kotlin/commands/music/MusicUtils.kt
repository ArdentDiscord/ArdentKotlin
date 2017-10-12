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

data class ArdentTrack(val author: String, val channel: String?, var track: AudioTrack) {
    private val votedToSkip = ArrayList<String>()

    fun addSkipVote(user: User): Boolean {
        TODO()
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
        return textChannel!!.toChannel()
    }

    fun setChannel(channel: TextChannel?) {
        textChannel = channel?.id
    }

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

    fun removeAt(num: Int?): Boolean {
        if (num == null) return false
        val track = queue.toList().getOrNull(num) ?: return false
        queue.removeFirstOccurrence(track)
        return true
    }
}

class TrackScheduler(player: AudioPlayer, var channel: TextChannel?, val guild: Guild) : AudioEventAdapter() {
    var manager: ArdentMusicManager = ArdentMusicManager(player, channel?.id)
    var autoplay = true
    override fun onTrackStart(player: AudioPlayer, track: AudioTrack) {
        waiterExecutor.schedule({
            if (track.position == 0.toLong() && guild.selfMember.voiceState.inVoiceChannel() && !player.isPaused && player.playingTrack != null && player.playingTrack == track) {
                val queue = mutableListOf<ArdentTrack>(manager.current ?: ArdentTrack(guild.selfMember.id(), channel?.id, track))
                queue.addAll(manager.queue)
                player.isPaused = false
                manager.resetQueue()
                manager.nextTrack()
                queue.forEach { manager.queue(it) }
            }
        }, 5, TimeUnit.SECONDS)
        autoplay = true
        if (channel?.guild?.getData()?.musicSettings?.announceNewMusic == true) {
            if (guild.selfMember.voiceChannel() != null) {
                val builder = guild.selfMember.embed("Now Playing: {0}".tr(channel!!.guild, track.info.title))
                builder.setThumbnail("https://s-media-cache-ak0.pinimg.com/736x/69/96/5c/69965c2849ec9b7148a5547ce6714735.jpg")
                builder.addField("Title".tr(channel!!.guild), track.info.title, true)
                        .addField("Author".tr(channel!!.guild), track.info.author, true)
                        .addField("Duration".tr(channel!!.guild), track.getDurationFancy(), true)
                        .addField("URL".tr(channel!!.guild), track.info.uri, true)
                        .addField("Is Stream".tr(channel!!.guild), track.info.isStream.toString(), true)
                channel?.send(builder)
            } else {
                player.stopTrack()
            }
        }
    }

    override fun onTrackEnd(player: AudioPlayer, track: AudioTrack, endReason: AudioTrackEndReason) {
        try {
            if (guild.audioManager.isConnected) {
                if (track.position != 0L) PlayedMusic(guild.id ?: "unknown", track.position / 1000.0 / 60.0 / 60.0).insert("musicPlayed")
                if (track.position != 0L && player.playingTrack == null && manager.queue.size == 0 && guild.getData().musicSettings.autoQueueSongs && guild.selfMember.voiceChannel() != null && autoplay) {
                    try {
                        val get = spotifyApi.search.searchTrack(track.info.title.rmCharacters("()").rmCharacters("[]")
                                .replace("ft.", "").replace("feat", "")
                                .replace("feat.", ""))
                        if (get.items.isEmpty()) {
                            (channel ?: manager.getChannel())?.send("Couldn't find this song in the Spotify database, no autoplay available.".tr(channel!!.guild))
                            return
                        }
                        val recommendation = spotifyApi.browse.getRecommendations(seedTracks = listOf(get.items[0].id), limit = 1).tracks[0]
                        println("${recommendation.name} by ${recommendation.artists[0].name}")
                        "${recommendation.name} by ${recommendation.artists[0].name}".load(guild.selfMember, channel ?: guild.defaultChannel)
                    } catch (ignored: Exception) {
                        ignored.printStackTrace()
                    }
                } else manager.nextTrack()
            }
        } catch (e: Exception) {
        }
    }

    private fun String.rmCharacters(characterSymbol: String): String {
        return when {
            characterSymbol.contains("[]") -> this.replace("\\s*\\[[^\\]]*\\]\\s*".toRegex(), " ")
            characterSymbol.contains("{}") -> this.replace("\\s*\\{[^\\}]*\\}\\s*".toRegex(), " ")
            else -> this.replace("\\s*\\([^\\)]*\\)\\s*".toRegex(), " ")
        }
    }

    override fun onTrackStuck(player: AudioPlayer, track: AudioTrack, thresholdMs: Long) {
        guild.getGuildAudioPlayer(channel).scheduler.manager.nextTrack()
        channel?.send("${Emoji.BALLOT_BOX_WITH_CHECK} " + "The player got stuck... attempting to skip now now (this is Discord's fault) - If you encounter this multiple times, please type {0}leave".tr(channel!!.guild, guild.getPrefix()))
    }

    override fun onTrackException(player: AudioPlayer, track: AudioTrack, exception: FriendlyException) {
        onException(exception)
    }


    private fun onException(exception: FriendlyException) {
        manager.current = null
        manager.nextTrack()
        try {
            manager.getChannel()?.sendMessage("I wasn't able to play that track, skipping... **Reason: **{0}".tr(manager.getChannel()!!.guild, exception.localizedMessage))?.queue()
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
fun Guild.getGuildAudioPlayer(channel: TextChannel?): GuildMusicManager {
    val guildId = id.toLong()
    var musicManager = managers[guildId]
    if (musicManager == null) {
        musicManager = GuildMusicManager(playerManager, channel, this)
        audioManager.sendingHandler = musicManager.sendHandler
        managers.put(guildId, musicManager)
    } else {
        if (channel != null) {
            musicManager.scheduler.channel = channel
            musicManager.scheduler.manager.setChannel(channel)
        }
    }
    return musicManager
}