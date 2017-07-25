package music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import main.Category
import main.Command
import main.playerManager
import main.waiter
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import utils.*

class Radio : Command(Category.MUSIC, "radio", "play a radio station live from a list of provided options", "pr") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            withHelp("start", "start the selection for radio playback")
                    .withHelp("request [name of radio station]", "send a request to the developers to add a specific radio station")
                    .displayHelp(channel, member)
        }
    }
}

class Play : Command(Category.MUSIC, "play", "play a song by its url or search a song to look it up on youtube", "p") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (member.voiceState.channel == null) {
            event.channel.send(member, "${Emoji.CROSS_MARK} lol, you gotta be connected to a voice channel")
            return
        }
        arguments.concat().load(member, member.voiceState.channel, channel, event.message)
    }
}


fun VoiceChannel.connect(member: Member, textChannel: TextChannel) {
    val guild = member.guild
    val audioManager = guild.audioManager
    if (audioManager.isConnected) {
        textChannel.send(member, "${Emoji.CROSS_MARK} I'm already connected to a voice channel")
        return
    }
    try {
        audioManager.openAudioConnection(this)
        textChannel.send(member, "${Emoji.BALLOT_BOX_WITH_CHECK} I successfully joined **$name**!")
    } catch (e: PermissionException) {
        textChannel.send(member, "${Emoji.CROSS_MARK} I cannot join that voice channel ($name)! Reason: *${e.localizedMessage}*")
    }
}

fun play(member: Member, guild: Guild, channel: VoiceChannel, musicManager: GuildMusicManager, track: AudioTrack, textChannel: TextChannel) {
    if (!guild.audioManager.isAttemptingToConnect && !guild.audioManager.isConnected) {
        channel.connect(member, textChannel)
    }
    musicManager.scheduler.manager.addToQueue(ArdentTrack(member.user.id, textChannel.id, track))
}

fun String.load(member: Member, channel: VoiceChannel, textChannel: TextChannel, message: Message, search: Boolean = false) {
    val musicManager = getGuildAudioPlayer(member.guild, textChannel)
    val data = member.guild.getData()
    if (data.musicSettings.singleSongInQueueForMembers && !member.hasOverride()) {
        musicManager.scheduler.manager.queueAsList.forEach { track ->
            if (track.author == member.user.id) {
                textChannel.send(member, "${Emoji.CROSS_MARK} You can only queue **1** song at a time, per the rules set by your administrators")
                return
            }
        }
    }
    playerManager.loadItemOrdered(musicManager, this, object : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException) {
            textChannel.send(member, "There was an error loading the track: *${exception.localizedMessage}")
        }

        override fun trackLoaded(track: AudioTrack) {
            textChannel.send(member, "${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue...")
            play(member, member.guild, channel, musicManager, track, textChannel)
        }

        override fun noMatches() {
            if (search) textChannel.send(member, "I was unable to find a track with that name. Please try again with a different query")
            else ("ytsearch: ${this@load}").load(member, channel, textChannel, message, true)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (!playlist.isSearchResult) {
                textChannel.send(member, "${Emoji.BALLOT_BOX_WITH_CHECK} Adding ${playlist.tracks.size} tracks to the queue...")
                playlist.tracks.forEach { play(member, member.guild, channel, musicManager, it, textChannel) }
                return
            }
            val selectFrom = mutableListOf<String>()
            val num: Int
            if (playlist.tracks.size >= 5) num = 5
            else num = playlist.tracks.size
            (1..num)
                    .map { playlist.tracks[it - 1] }
                    .map { it.info }
                    .mapTo(selectFrom) { "${it.title} by *${it.author}*" }
            textChannel.selectFromList(member, "Select Song", selectFrom, { response ->
                val track = playlist.tracks[response]
                play(member, member.guild, channel, musicManager, track, textChannel)
                textChannel.send(member, "${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue...")
            })

        }
    })
}