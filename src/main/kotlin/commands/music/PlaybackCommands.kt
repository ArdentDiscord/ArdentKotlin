package commands.music

import events.Category
import events.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import translation.tr
import utils.discord.*
import utils.functionality.Emoji
import utils.functionality.concat

class Play : Command(Category.MUSIC, "play", "play songs, playlists, or your personal music library!", "p") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            event.channel.send("You can search or play single tracks from Youtube or Spotify by typing */play [search or url]*.".tr(event) +
                    "Or, type */mylibrary play* to play your personal music library or */playlist [playlist id] play* to play one of your playlists.".tr(event))
        } else {
            arguments.concat().load(event.member, event.textChannel)
        }
    }
}

class Skip : Command(Category.MUSIC, "skip", "skips the currently playing track") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasPermission(event.textChannel, true)) return
        val manager = event.guild.getAudioManager(event.textChannel)
        if (manager.manager.current == null) return
        val track = manager.manager.current!!
        event.channel.send("Skipped current track: **{0}** by *{1}* {2} - added by **{3}**"
                .tr(event, track.track!!.info.title, track.track!!.info.author, track.track!!.getCurrentTime(), getUserById(track.user)?.toFancyString()
                        ?: "unable to determine track owner"))
        manager.player.playingTrack.position = manager.player.playingTrack.duration - 1
    }
}

class Resume : Command(Category.MUSIC, "resume", "resumes the music player") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasPermission(event.textChannel, musicCommand = true)) return
        val audioManager = event.guild.getAudioManager(event.textChannel)
        when {
            audioManager.player.playingTrack == null -> event.channel.send("There isn't a playing track!".tr(event))
            !audioManager.player.isPaused -> event.channel.send("The player isn't paused!".tr(event))
            else -> {
                audioManager.player.isPaused = false
                event.channel.send("Resumed playback".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
            }
        }
    }
}

class Pause : Command(Category.MUSIC, "pause", "pause the music player") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel) || !event.member.hasPermission(event.textChannel, musicCommand = true)) return
        val audioManager = event.guild.getAudioManager(event.textChannel)
        when {
            audioManager.player.playingTrack == null -> event.channel.send("There isn't a playing track!".tr(event))
            audioManager.player.isPaused -> event.channel.send("The player is already paused!".tr(event))
            else -> {
                audioManager.player.isPaused = true
                event.channel.send("Paused playback".tr(event) + " ${Emoji.WHITE_HEAVY_CHECKMARK}")
            }
        }
    }
}


class SongUrl : Command(Category.MUSIC, "songlink", "get the link for the currently playing track!", "su") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.checkSameChannel(event.textChannel)) return
        val player = event.guild.getAudioManager(event.textChannel).player
        if (player.playingTrack == null) event.channel.send("There isn't a playing track!")
        else event.channel.send("**{0}** by **{1}**: {2}"
                .tr(event, player.playingTrack.info.title, player.playingTrack.info.author, player.playingTrack.info.uri))
    }
}


class Playing : Command(Category.MUSIC, "playing", "shows information about the currently playing track", "np") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val manager = event.guild.getAudioManager(event.textChannel)
        val player = manager.player
        if (player.playingTrack == null) event.channel.send("There isn't a playing track!")
        else {
            val track = manager.manager.current!!
            event.channel.send(track.getInfo(event.guild))
            if (manager.player.isPaused) event.channel.send(Emoji.INFORMATION_SOURCE.symbol + " " + "The player is currently paused".tr(event))
        }
    }
}

class Queue : Command(Category.MUSIC, "queue", "shows information about the current queue", "q") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val embed = event.member.embed("Current Music Queue", event.textChannel)
        val audioManager = event.guild.getAudioManager(event.textChannel)
        if (audioManager.manager.current == null) {
            embed.appendDescription(Emoji.INFORMATION_SOURCE.symbol + " " + "There aren't any currently playing tracks!".tr(event))
            embed.appendDescription("\n\n" + "You can view the queue online by clicking [here]({0})".tr(event, "https://ardentbot.com/music/queue/${event.guild.id}"))
        } else {
            if (audioManager.manager.queue.size == 0) {
                embed.appendDescription("There are no songs in the queue!".tr(event))
                embed.appendDescription("\n\n" + "You can view the queue online by clicking [here]({0})".tr(event, "https://ardentbot.com/music/queue/${event.guild.id}"))
            }
            else {
                var current = 1
                audioManager.manager.queue.stream().limit(10).forEachOrdered {
                    embed.appendDescription("**$current**: " + it.getInfo(event.guild) + "\n")
                    current++
                }
                if (audioManager.manager.queue.size > 10) embed.appendDescription("View the entire queue by clicking [here]({0})".tr(event, "https://ardentbot.com/music/queue/${event.guild.id}"))
            }
        }
        embed.send()
    }
}

class ClearQueue : Command(Category.MUSIC, "clearqueue", "clears all songs from the queue", "cq") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.hasPermission(event.textChannel, true) && event.member.checkSameChannel(event.textChannel)) {
            val audioManager = event.guild.getAudioManager(event.textChannel)
            audioManager.scheduler.autoplay = false
            audioManager.manager.resetQueue()
            event.channel.send(Emoji.BALLOT_BOX_WITH_CHECK.symbol + " " + "Successfully cleared the queue".tr(event))
        }
    }
}

class Repeat : Command(Category.MUSIC, "repeat", "repeat the track that's currently playing") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.checkSameChannel(event.textChannel) && event.member.hasPermission(event.textChannel, true)) {
            val audioManager = event.guild.getAudioManager(event.textChannel)
            if (audioManager.manager.current != null) {
                audioManager.manager.addToBeginningOfQueue(audioManager.manager.current!!)
                event.channel.send(Emoji.WHITE_HEAVY_CHECKMARK.symbol + " " + "Added the current track to the front of the queue".tr(event))
            } else event.channel.send("There isn't a currently playing track!".tr(event))
        }
    }
}

class RemoveFrom : Command(Category.MUSIC, "removefrom", "remove all the tracks from the mentioned user or users", "rf") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.hasPermission(event.textChannel, true) && event.member.checkSameChannel(event.textChannel)) {
            val mentioned = event.message.mentionedUsers
            if (mentioned.size == 0 || mentioned.size > 1) event.channel.send("You must mention one user!".tr(event))
            else {
                val audioManager = event.guild.getAudioManager(event.textChannel)
                val sizeBefore = audioManager.manager.queue.size
                audioManager.manager.queue.removeIf { it.user == mentioned[0].id }
                event.channel.send(Emoji.WHITE_HEAVY_CHECKMARK.symbol + " " + "Successfully removed **{0}** queued tracks from **{1}**"
                        .tr(event, audioManager.manager.queue.size - sizeBefore, mentioned[0].toFancyString()))
            }
        }
    }
}

class Volume : Command(Category.MUSIC, "volume", "see and change the volume of the player") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val audioManager = event.guild.getAudioManager(event.textChannel)
        if (arguments.size == 0) event.channel.send(Emoji.PUBLIC_ADDRESS_LOUDSPEAKER.symbol + " " + "The volume of your server's music player is **{0}**%. Change it using */volume [percent from 1 to 100 here]*".tr(event, audioManager.player.volume))
        else {
            if (event.member.hasPermission(event.textChannel, true) && event.member.checkSameChannel(event.textChannel)) {
                val volume = arguments[0].replace("%", "").toIntOrNull()
                if (volume == null || volume < 0 || volume > 100) {
                    event.channel.send(Emoji.HEAVY_MULTIPLICATION_X.symbol + " " + "You need to specify a valid percentage!")
                } else {
                    audioManager.player.volume = volume
                    event.channel.send(Emoji.PUBLIC_ADDRESS_LOUDSPEAKER.symbol + " " + "Successfully set Ardent volume to **{0}**%".tr(event, volume))
                }
            }
        }
    }
}

class GoTo : Command(Category.MUSIC, "goto", "use this command to go to a certain point in the currently playing track") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) event.channel.send("Use this command to go to specific points in a track. For example, to go to **1 minute, 23 seconds** into a track, use */goto 1:23*")
        else {
            val split = arguments[0].split(":")
            val minutes = split.getOrNull(0)?.toIntOrNull()
            val seconds = split.getOrNull(1)?.toIntOrNull()
            if (split.size != 2 || minutes == null || seconds == null) event.channel.send("Use this command to go to specific points in a track. For example, to go to **1 minute, 23 seconds** into a track, use */goto 1:23*")
            else {
                val audioManager = event.guild.getAudioManager(event.textChannel)
                if (audioManager.manager.current == null) event.channel.send("There isn't a currently playing track!".tr(event))
                else if (event.member.hasPermission(event.textChannel, true) && event.member.checkSameChannel(event.textChannel)) {
                    if (minutes * 60 + seconds < 0 || minutes * 60 + seconds > audioManager.player.playingTrack.duration / 1000) {
                        event.channel.send("You entered an invalid position!".tr(event))
                    } else {
                        audioManager.player.playingTrack.position = ((minutes * 60 + seconds) * 1000).toLong()
                        event.channel.send(Emoji.BALLOT_BOX_WITH_CHECK.symbol + " " + "Went to **{0}** in the track".tr(event, arguments[0]))
                    }
                }
            }
        }
    }
}