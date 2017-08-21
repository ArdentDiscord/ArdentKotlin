package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import events.Category
import events.Command
import main.managers
import main.playerManager
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*

class Radio : Command(Category.MUSIC, "radio", "play a radio station live from a list of provided options. this is a **patron-only** feature", "pr") {
    private val stations = hashMapOf(Pair("977hits", "http://19353.live.streamtheworld.com/977_HITS_SC"),
            Pair("Jamendo Lounge", "http://streaming.radionomy.com/JamendoLounge?lang=en-US%2cen%3bq%3d0.8"),
            Pair("Radio Rock Mix", "http://streaming.hotmixradio.fr/hotmixradio-rock-128.mp3?lang=en-US%2cen%3bq%3d0.8%2cru%3bq%3d0.6"))

    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            withHelp("start", "view and select from the list of current radio stations")
                    .withHelp("request [name of radio station]", "send a request to the developers to add a specific radio station")
                    .displayHelp(event.channel, event.member)
            return
        }
        if (arguments[0].equals("start", true)) {
            val keys = stations.keys.toMutableList()
            event.channel.selectFromList(event.member, "Select the radio station that you want to listen to", keys, { selection ->
                if (event.member.isPatron() || event.guild.isPatronGuild()) {
                    stations.values.toList()[selection].load(event.member, event.textChannel, event.message, radioName = keys[selection])
                } else event.channel.requires(event.member, DonationLevel.BASIC)
            })
            return
        }
    }
}

class Play : Command(Category.MUSIC, "play", "play a song by its url or search a song to look it up on youtube", "p") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            event.channel.send("You need to specify a URL or search query!")
            return
        }
        arguments.concat().load(event.member, event.textChannel, event.message)
    }
}

class Leave : Command(Category.MUSIC, "leave", "makes me leave the voice channel I'm in") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val guild = event.guild
        val manager = guild.getGuildAudioPlayer(event.textChannel).scheduler.manager
        manager.resetQueue()
        manager.nextTrack()
        val vc = guild.audioManager.connectedChannel
        guild.audioManager.closeAudioConnection()
        if (vc != null) event.channel.send("Successfully disconnected from **${vc.name}** ${Emoji.MULTIPLE_MUSICAL_NOTES}")
    }
}

class Pause : Command(Category.MUSIC, "pause", "pause the player... what did you think this was gonna do?") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        pause(event, true)
    }
}

class Resume : Command(Category.MUSIC, "resume", "resumes the player. what did you think this was gonna do?") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        pause(event, false)
    }
}

fun pause(event: MessageReceivedEvent, value: Boolean) {
    if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
    val player = event.guild.getGuildAudioPlayer(event.textChannel).player
    if (!player.currentlyPlaying(event.textChannel)) return
    player.isPaused = value
    if (!value) event.channel.send("Resumed playback ${Emoji.WHITE_HEAVY_CHECKMARK}")
    else event.channel.send("Paused playback ${Emoji.WHITE_HEAVY_CHECKMARK}")

}

class Skip : Command(Category.MUSIC, "skip", "skips the currently playing track") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        if (!manager.player.currentlyPlaying(event.textChannel)) return
        val track = manager.scheduler.manager.current!!
        manager.player.playingTrack.position = manager.player.playingTrack.duration - 1
        event.channel.send("Skipped current track: **${track.track.info.title}** by *${track.track.info.author}* ${track.track.getCurrentTime()} - added by **${track.author.toUser()?.withDiscrim()}**")
    }
}

class Stop : Command(Category.MUSIC, "stop", "stop the player and remove all tracks in the queue") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        manager.player.stopTrack()
        manager.scheduler.manager.resetQueue()
        event.channel.send("Stopped and reset the player ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class SongUrl : Command(Category.MUSIC, "songlink", "get the link for the currently playing track!", "su") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel)) return
        val player = event.guild.getGuildAudioPlayer(event.textChannel).player
        if (!player.currentlyPlaying(event.textChannel)) return
        event.channel.send("**${player.playingTrack.info.title}** by **${player.playingTrack.info.author}**: ${player.playingTrack.info.uri}")
    }
}

class Shuffle : Command(Category.MUSIC, "shuffle", "shuffle the current queue") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.shuffle()
        event.channel.send("Shuffled the current queue ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class Repeat : Command(Category.MUSIC, "repeat", "repeat the track that's currently playing") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true, djCommand = true)) return
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (!player.currentlyPlaying(event.textChannel)) return
        manager.scheduler.manager.addToBeginningOfQueue(manager.scheduler.manager.current)
        event.channel.send("Added the current track as Up Next ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class Playing : Command(Category.MUSIC, "playing", "shows information about the currently playing track", "np") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (!player.currentlyPlaying(event.textChannel)) return
        val track = manager.scheduler.manager.current!!
        event.channel.send("**${track.track.info.title}** by *${track.track.info.author}* ${track.track.getCurrentTime()} - added by **${track.author.toUser()?.withDiscrim()}**")
    }
}

class Volume : Command(Category.MUSIC, "volume", "see and change the volume of the player") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        val player = manager.player
        if (arguments.size == 0) {
            event.channel.send("The volume of this server's music player is **${player.volume}**% ${Emoji.PUBLIC_ADDRESS_LOUDSPEAKER}")
            event.channel.send("If the owner of your server or if you are an Ardent patron, type *${event.guild.getPrefix()}volume percentage_here* to set " +
                    "the volume")
            return
        }
        if (!event.member.hasDonationLevel(event.textChannel, DonationLevel.BASIC)) return
        if (!event.member.checkSameChannel(event.textChannel)) return
        val setTo: Int? = arguments[0].replace("%", "").toIntOrNull()
        if (setTo == null || setTo < 0 || setTo > 100) event.channel.send("You need to specify a valid percentage. Example: *${event.guild.getPrefix()}volume 99%")
        else {
            player.volume = setTo
            event.channel.send("Set player volume to **$setTo**% ${Emoji.PUBLIC_ADDRESS_LOUDSPEAKER}")
        }
    }
}

class Queue : Command(Category.MUSIC, "queue", "see a list of tracks in the queue", "q") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getGuildAudioPlayer(event.textChannel)
        if (manager.scheduler.manager.queue.size == 0) {
            event.channel.send("${Emoji.MULTIPLE_MUSICAL_NOTES} There are no songs in the queue currently!")
            return
        }
        val embed = event.member.embed("Current Queue: ${event.guild.name}")
                .appendDescription("*This shows the state of the queue at the current moment*\n")
        val remaining = manager.scheduler.manager.queue.size - 10
        val show: Int
        if (remaining > 0) show = 9
        else show = remaining + 9
        val queue = manager.scheduler.manager.queueAsList
        (0..show).forEach { number ->
            val track = queue[number]
            embed.appendDescription("\n [**${number + 1}**] : **${track.track.info.title}** by *${track.track.info.author}*\n${track.track.getCurrentTime()} - added by __${track.author.toUser()?.withDiscrim()}__")
        }
        if (remaining > 0) embed.appendDescription("\n\n   *...and $remaining tracks after*")
        event.channel.send(embed)
    }
}

class FixMusic : Command(Category.MUSIC, "fixmusic", "fix your music player if something isn't going right") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.hasOverride(event.textChannel, true, djCommand = true)) {
            val manager = event.guild.getGuildAudioPlayer(event.textChannel)
            val queue = manager.scheduler.manager.queueAsList
            val current = manager.scheduler.manager.current
            managers.remove(event.guild.idLong)
            val newManager = event.guild.getGuildAudioPlayer(event.textChannel)
            if (current != null) newManager.scheduler.manager.queue(ArdentTrack(current.author, current.channel, current.track.makeClone()))
            queue.forEach { queueMember ->
                newManager.scheduler.manager.queue(ArdentTrack(queueMember.author, queueMember.channel, queueMember.track.makeClone()))
            }
            event.channel.send("${Emoji.BALLOT_BOX_WITH_CHECK} Successfully reset player")
        }
    }
}

class ClearQueue : Command(Category.MUSIC, "clearqueue", "clear the queue", "cq") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.hasOverride(event.textChannel, true, djCommand = true)) {
            if (!event.member.checkSameChannel(event.textChannel)) return
            event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.resetQueue()
            event.channel.send("Cleared the queue ${Emoji.BALLOT_BOX_WITH_CHECK}")
        }

    }
}

class RemoveFrom : Command(Category.MUSIC, "removefrom", "remove all the tracks from the mentioned user or users", "rf") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasOverride(event.textChannel, true)) return
        val mentioned = event.message.mentionedUsers
        if (mentioned.size == 0) {
            event.channel.send("You need to mention at least one person to remove songs from!")
        } else {
            mentioned.forEach { removeFrom -> event.guild.getGuildAudioPlayer(event.textChannel).scheduler.manager.removeFrom(removeFrom) }
            event.channel.send("Successfully removed any queued tracks from **${mentioned.map { it.withDiscrim() }.toList().stringify()}** ${Emoji.WHITE_HEAVY_CHECKMARK}")
        }
    }
}

fun VoiceChannel.connect(member: Member, textChannel: TextChannel) {
    val guild = member.guild
    val audioManager = guild.audioManager
    try {
        audioManager.openAudioConnection(this)
    } catch (e: Throwable) {
        textChannel.send("${Emoji.CROSS_MARK} I cannot join that voice channel ($name)! Reason: *${e.localizedMessage}*")
    }
}

fun play(member: Member, guild: Guild, channel: VoiceChannel, musicManager: GuildMusicManager, track: AudioTrack, textChannel: TextChannel) {
    if (!guild.audioManager.isConnected) {
        guild.audioManager.openAudioConnection(channel)
    }
    musicManager.scheduler.manager.addToQueue(ArdentTrack(member.user.id, textChannel.id, track))
}

fun Member.checkSameChannel(textChannel: TextChannel): Boolean {
    if (voiceState.channel == null) {
        textChannel.send("${Emoji.CROSS_MARK} You need to be connected to a voice channel")
        return false
    }
    if (guild.selfMember.voiceState.channel == null) {
        guild.audioManager.closeAudioConnection()
        voiceState.channel.connect(this, textChannel)
        return false
    }
    if (guild.selfMember.voiceState.channel != voiceState.channel) {
        textChannel.send("${Emoji.CROSS_MARK} We need to be connected to the **same** voice channel")
        return false
    }
    return true
}

fun String.load(member: Member, textChannel: TextChannel, message: Message?, search: Boolean = false, radioName: String? = null, autoplay: Boolean = false) {
    if (member.voiceState.channel == null) {
        textChannel.send("${Emoji.CROSS_MARK} You need to be connected to a voice channel")
        return
    }
    val guild = textChannel.guild
    if (!guild.selfMember.voiceState.inVoiceChannel()) {
        member.voiceState.channel.connect(member, textChannel)
    }
    val musicManager = member.guild.getGuildAudioPlayer(textChannel)
    playerManager.loadItemOrdered(musicManager, this, object : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException) {
            textChannel.send("There was an error loading the track: *${exception.localizedMessage}")
        }

        override fun trackLoaded(track: AudioTrack) {
            if (track.info.length > (15 * 60 * 1000) && !member.hasDonationLevel(textChannel, DonationLevel.BASIC)) {
                textChannel.send("${Emoji.NO_ENTRY_SIGN} Sorry, but only servers or members with the **Basic** donation " +
                        "level can play songs longer than 15 minutes")
                return
            }
            if (radioName == null) textChannel.send("${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue...")
            else textChannel.send("${Emoji.MULTIPLE_MUSICAL_NOTES} Starting to play the radio station **$radioName**...")
            play(member, member.guild, member.voiceChannel()!!, musicManager, track, textChannel)
        }

        override fun noMatches() {
            if (search) {
                if (autoplay) {
                    textChannel.send("I was unable to find a related song...")
                } else textChannel.send("I was unable to find a track with that name. Please try again with a different query")
            } else ("ytsearch: ${this@load}").load(member, textChannel, message, true, autoplay = autoplay)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (!playlist.isSearchResult) {
                textChannel.send("${Emoji.BALLOT_BOX_WITH_CHECK} Adding ${playlist.tracks.size} tracks to the queue...")
                try {
                    playlist.tracks.forEach { play(member, member.guild, member.voiceChannel()!!, musicManager, it, textChannel) }
                } catch (e: Exception) {
                    textChannel.send("Failed to play tracks: **Error:** ${e.localizedMessage}")
                }
                return
            }
            if (autoplay) {
                if (playlist.tracks.size > 0) {
                    var cont = true
                    var current = 0
                    while (cont) {
                        val track = playlist.tracks[current]
                        if (track.duration < 20 * 1000 * 60) {
                            cont = false
                            play(member, member.guild, member.voiceChannel()!!, musicManager, track, textChannel)
                            textChannel.send("${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue... **[Ardent Autoplay]**")
                        }
                        current++
                    }
                }
            } else {
                val selectFrom = mutableListOf<String>()
                val num: Int
                if (playlist.tracks.size >= 7) num = 7
                else num = playlist.tracks.size
                (1..num)
                        .map { playlist.tracks[it - 1] }
                        .map { it.info }
                        .mapTo(selectFrom) { "${it.title} by *${it.author}*" }
                textChannel.selectFromList(member, "Select Song", selectFrom, { response ->
                    val track = playlist.tracks[response]
                    play(member, member.guild, member.voiceChannel()!!, musicManager, track, textChannel)
                    textChannel.send("${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue...")
                })
            }
        }
    })
}