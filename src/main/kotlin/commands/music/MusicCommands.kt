package commands.music

import com.sedmelluq.discord.lavaplayer.player.AudioLoadResultHandler
import com.sedmelluq.discord.lavaplayer.tools.FriendlyException
import com.sedmelluq.discord.lavaplayer.track.AudioPlaylist
import com.sedmelluq.discord.lavaplayer.track.AudioTrack
import events.Category
import events.Command
import main.conn
import main.managers
import main.playerManager
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import utils.*

class Radio : Command(Category.MUSIC, "radio", "play a radio station live from a list of provided options. this is a **patron-only** feature", "pr") {
    val stations = hashMapOf(Pair("977hits", "http://19353.live.streamtheworld.com/977_HITS_SC"),
            Pair("Jamendo Lounge", "http://streaming.radionomy.com/JamendoLounge?lang=en-US%2cen%3bq%3d0.8"))

    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            withHelp("start", "view and select from the list of current radio stations")
                    .withHelp("request [name of radio station]", "send a request to the developers to add a specific radio station")
                    .displayHelp(channel, member)
            return
        }
        if (arguments[0].equals("start", true)) {
            val keys = stations.keys.toMutableList()
            channel.selectFromList(member, "Select the radio station that you want to listen to", keys, {
                selection ->
                if (member.isPatron() || guild.isPatronGuild()) {
                    stations.values.toList()[selection].load(member, channel, event.message, radioName = keys[selection])
                } else channel.requires(member, DonationLevel.BASIC)
            })
            return
        }
    }
}

class Play : Command(Category.MUSIC, "play", "play a song by its url or search a song to look it up on youtube", "p") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            channel.send(member, "You need to specify a URL or search query!")
            return
        }
        arguments.concat().load(member, channel, event.message)
    }
}

class Leave : Command(Category.MUSIC, "leave", "makes me leave the voice channel I'm in") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!member.checkSameChannel(channel) || !member.hasOverride(channel, true, djCommand = true)) return
        val manager = guild.getGuildAudioPlayer(channel).scheduler.manager
        manager.resetQueue()
        manager.nextTrack()
        val vc = guild.audioManager.connectedChannel
        guild.audioManager.closeAudioConnection()
        if (vc != null) channel.send(member, "Successfully disconnected from **${vc.name}** ${Emoji.MULTIPLE_MUSICAL_NOTES}")
    }
}

class Pause : Command(Category.MUSIC, "pause", "pause the player... what did you think this was gonna do?") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!member.checkSameChannel(channel) || !member.hasOverride(channel, true, djCommand = true)) return
        val player = guild.getGuildAudioPlayer(channel).player
        if (!player.currentlyPlaying(channel)) return
        player.isPaused = true
        channel.send(member, "Paused the player ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class Resume : Command(Category.MUSIC, "resume", "resumes the player. what did you think this was gonna do?") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!member.checkSameChannel(channel) || !member.hasOverride(channel, true, djCommand = true)) return
        val player = guild.getGuildAudioPlayer(channel).player
        if (!player.currentlyPlaying(channel)) return
        player.isPaused = false
        channel.send(member, "Resumed playback ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class Skip : Command(Category.MUSIC, "skip", "skips the currently playing track") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!member.checkSameChannel(channel) || !member.hasOverride(channel, true, djCommand = true)) return
        val manager = guild.getGuildAudioPlayer(channel)
        if (!manager.player.currentlyPlaying(channel)) return
        val track = manager.scheduler.manager.current!!
        manager.player.playingTrack.position = manager.player.playingTrack.duration - 1
        channel.send(member, "Skipped current track: **${track.track.info.title}** by *${track.track.info.author}* ${track.track.getCurrentTime()} - added by **${track.author.toUser()?.withDiscrim()}**")
    }
}

class Stop : Command(Category.MUSIC, "stop", "stop the player and remove all tracks in the queue") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!member.checkSameChannel(channel) || !member.hasOverride(channel, true, djCommand = true)) return
        val manager = guild.getGuildAudioPlayer(channel)
        manager.player.stopTrack()
        manager.scheduler.manager.resetQueue()
        channel.send(member, "Stopped and reset the player ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class SongUrl : Command(Category.MUSIC, "songlink", "get the link for the currently playing track!", "su") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!member.checkSameChannel(channel)) return
        val player = guild.getGuildAudioPlayer(channel).player
        if (!player.currentlyPlaying(channel)) return
        channel.send(member, "**${player.playingTrack.info.title}** by **${player.playingTrack.info.author}**: ${player.playingTrack.info.uri}")
    }
}

class Shuffle : Command(Category.MUSIC, "shuffle", "shuffle the current queue") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!member.checkSameChannel(channel) || !member.hasOverride(channel, true, djCommand = true)) return
        guild.getGuildAudioPlayer(channel).scheduler.manager.shuffle()
        channel.send(member, "Shuffled the current queue ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class Repeat : Command(Category.MUSIC, "repeat", "repeat the track that's currently playing") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!member.checkSameChannel(channel) || !member.hasOverride(channel, true, djCommand = true)) return
        val manager = guild.getGuildAudioPlayer(channel)
        val player = manager.player
        if (!player.currentlyPlaying(channel)) return
        manager.scheduler.manager.addToBeginningOfQueue(manager.scheduler.manager.current)
        channel.send(member, "Added the current track as Up Next ${Emoji.WHITE_HEAVY_CHECKMARK}")
    }
}

class Playing : Command(Category.MUSIC, "playing", "shows information about the currently playing track", "np") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = guild.getGuildAudioPlayer(channel)
        val player = manager.player
        if (!player.currentlyPlaying(channel)) return
        val track = manager.scheduler.manager.current!!
        channel.send(member, "**${track.track.info.title}** by *${track.track.info.author}* ${track.track.getCurrentTime()} - added by **${track.author.toUser()?.withDiscrim()}**")
    }
}

class Volume : Command(Category.MUSIC, "volume", "see and change the volume of the player") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = guild.getGuildAudioPlayer(channel)
        val player = manager.player
        if (arguments.size == 0) {
            channel.send(member, "The volume of this server's music player is **${player.volume}**% ${Emoji.PUBLIC_ADDRESS_LOUDSPEAKER}")
            channel.send(member, "If the owner of your server or if you are an Ardent patron, type *${guild.getPrefix()}volume percentage_here* to set " +
                    "the volume")
            return
        }
        if (!member.hasDonationLevel(channel, DonationLevel.BASIC)) return
        if (!member.checkSameChannel(channel)) return
        val setTo: Int? = arguments[0].replace("%", "").toIntOrNull()
        if (setTo == null || setTo < 0 || setTo > 100) channel.send(member, "You need to specify a valid percentage. Example: *${guild.getPrefix()}volume 99%")
        else {
            player.volume = setTo
            channel.send(member, "Set player volume to **$setTo**% ${Emoji.PUBLIC_ADDRESS_LOUDSPEAKER}")
        }
    }
}

class Queue : Command(Category.MUSIC, "queue", "see a list of tracks in the queue", "q") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = guild.getGuildAudioPlayer(channel)
        if (manager.scheduler.manager.queue.size == 0) {
            channel.send(member, "${Emoji.MULTIPLE_MUSICAL_NOTES} There are no songs in the queue currently!")
            return
        }
        val embed = embed("Current Queue: ${guild.name}", member)
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
        channel.send(member, embed)
    }
}

class FixMusic : Command(Category.MUSIC, "fixmusic", "fix your music player if something isn't going right") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (member.hasOverride(channel, true, djCommand = true)) {
            val manager = guild.getGuildAudioPlayer(channel)
            val queue = manager.scheduler.manager.queueAsList
            val current = manager.scheduler.manager.current
            managers.remove(guild.idLong)
            val newManager = guild.getGuildAudioPlayer(channel)
            if (current != null) newManager.scheduler.manager.queue(ArdentTrack(current.author, current.channel, current.track.makeClone()))
            queue.forEach { queueMember ->
                newManager.scheduler.manager.queue(ArdentTrack(queueMember.author, queueMember.channel, queueMember.track.makeClone()))
            }
            channel.send(member, "${Emoji.BALLOT_BOX_WITH_CHECK} Successfully reset player")
        }
    }
}

class ClearQueue : Command(Category.MUSIC, "clearqueue", "clear the queue", "cq") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (member.hasOverride(channel, true, djCommand = true)) {
            if (!member.checkSameChannel(channel)) return
            guild.getGuildAudioPlayer(channel).scheduler.manager.resetQueue()
            channel.send(member, "Cleared the queue ${Emoji.BALLOT_BOX_WITH_CHECK}")
        }

    }
}

class RemoveFrom : Command(Category.MUSIC, "removefrom", "remove all the tracks from the mentioned user or users", "rf") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!member.checkSameChannel(channel) || !member.hasOverride(channel, true)) return
        val mentioned = event.message.mentionedUsers
        if (mentioned.size == 0) {
            channel.send(member, "You need to mention at least one person to remove songs from!")
        } else {
            mentioned.forEach { removeFrom -> guild.getGuildAudioPlayer(channel).scheduler.manager.removeFrom(removeFrom) }
            channel.send(member, "Successfully removed any queued tracks from **${mentioned.map { it.withDiscrim() }.toList().stringify()}** ${Emoji.WHITE_HEAVY_CHECKMARK}")
        }
    }
}

fun VoiceChannel.connect(member: Member, textChannel: TextChannel) {
    val guild = member.guild
    val audioManager = guild.audioManager
    try {
        audioManager.openAudioConnection(this)
    } catch (e: Throwable) {
        textChannel.send(member, "${Emoji.CROSS_MARK} I cannot join that voice channel ($name)! Reason: *${e.localizedMessage}*")
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
        textChannel.send(this, "${Emoji.CROSS_MARK} You need to be connected to a voice channel")
        return false
    }
    if (guild.selfMember.voiceState.channel == null) {
        guild.audioManager.closeAudioConnection()
        voiceState.channel.connect(this, textChannel)
        return false
    }
    if (guild.selfMember.voiceState.channel != voiceState.channel) {
        textChannel.send(this, "${Emoji.CROSS_MARK} We need to be connected to the **same** voice channel")
        return false
    }
    return true
}

fun String.load(member: Member, textChannel: TextChannel, message: Message?, search: Boolean = false, radioName: String? = null, autoplay: Boolean = false) {
    if (member.voiceState.channel == null) {
        textChannel.send(member, "${Emoji.CROSS_MARK} You need to be connected to a voice channel")
        return
    }
    val guild = textChannel.guild
    if (!guild.selfMember.voiceState.inVoiceChannel()) {
        member.voiceState.channel.connect(member, textChannel)
    }
    val musicManager = member.guild.getGuildAudioPlayer(textChannel)
    playerManager.loadItemOrdered(musicManager, this, object : AudioLoadResultHandler {
        override fun loadFailed(exception: FriendlyException) {
            textChannel.send(member, "There was an error loading the track: *${exception.localizedMessage}")
        }

        override fun trackLoaded(track: AudioTrack) {
            if (track.info.length > (15 * 60 * 1000) && !member.hasDonationLevel(textChannel, DonationLevel.BASIC)) {
                textChannel.send(member, "${Emoji.NO_ENTRY_SIGN} Sorry, but only servers or members with the **Basic** donation " +
                        "level can play songs longer than 15 minutes")
                return
            }
            if (radioName == null) textChannel.send(member, "${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue...")
            else textChannel.send(member, "${Emoji.MULTIPLE_MUSICAL_NOTES} Starting to play the radio station **$radioName**...")
            play(member, member.guild, member.voiceChannel()!!, musicManager, track, textChannel)
        }

        override fun noMatches() {
            if (search) {
                if (autoplay) {
                    textChannel.send(member, "I was unable to find a related song...")
                }
                else textChannel.send(member, "I was unable to find a track with that name. Please try again with a different query")
            }
            else ("ytsearch: ${this@load}").load(member, textChannel, message, true, autoplay = autoplay)
        }

        override fun playlistLoaded(playlist: AudioPlaylist) {
            if (!playlist.isSearchResult) {
                textChannel.send(member, "${Emoji.BALLOT_BOX_WITH_CHECK} Adding ${playlist.tracks.size} tracks to the queue...")
                try {
                    playlist.tracks.forEach { play(member, member.guild, member.voiceChannel()!!, musicManager, it, textChannel) }
                }
                catch (e: Exception) {
                    textChannel.send(member, "Failed to play tracks: **Error:** ${e.localizedMessage}")
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
                            textChannel.send(member, "${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue... **[Ardent Autoplay]**")
                        }
                        current++
                    }
                }
            }
            else {
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
                    textChannel.send(member, "${Emoji.BALLOT_BOX_WITH_CHECK} Adding **${track.info.title} by ${track.info.author}** to the queue...")
                })
            }
        }
    })
}