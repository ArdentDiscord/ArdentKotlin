package events

import commands.administrate.administrativeExecutor
import commands.music.getGuildAudioPlayer
import main.managers
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import utils.send
import java.util.concurrent.TimeUnit

class VoiceUtils {
    val pausedConnections = hashMapOf<String, Long>()
    @SubscribeEvent
    fun onVoiceLeave(e: GuildVoiceLeaveEvent) {
        val member = e.guild.selfMember
        if (member.voiceState.channel != null && member.voiceState.channel == e.channelLeft && e.channelLeft.members.size == 1) {
            pausedConnections.put(e.guild.id, System.currentTimeMillis())
            administrativeExecutor.schedule({
                if (pausedConnections.containsKey(e.guild.id)) {
                    pausedConnections.remove(e.guild.id)
                    managers.remove(e.guild.idLong)
                    e.guild.audioManager.closeAudioConnection()
                }
            }, 5, TimeUnit.MINUTES)
            val manager = e.guild.getGuildAudioPlayer(null)
            val player = manager.player
            if (player.playingTrack != null && !player.isPaused) {
                player.isPaused = true
                manager.scheduler.manager.getChannel()?.send(e.guild.selfMember, "Automatically paused because no one else was in the channel")
            }
        }
    }

    @SubscribeEvent
    fun onVoiceJoin(e: GuildVoiceJoinEvent) {
        val member = e.guild.selfMember
        if (member.voiceState.channel != null && member.voiceState.channel == e.channelJoined && e.channelJoined.members.size == 2) {
            if (pausedConnections.containsKey(e.guild.id)) pausedConnections.remove(e.guild.id)
            val manager = e.guild.getGuildAudioPlayer(null)
            val player = manager.player
            if (player.playingTrack != null && player.isPaused) {
                player.isPaused = false
                manager.scheduler.manager.getChannel()?.send(e.guild.selfMember, "Automatically unpaused because someone rejoined my channel")
            }
        }
    }
}