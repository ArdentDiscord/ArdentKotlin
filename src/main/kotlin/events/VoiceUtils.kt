package events

import commands.music.musicManager
import main.managers
import main.waiter
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import utils.getData
import utils.send
import utils.tr
import java.util.concurrent.TimeUnit

class VoiceUtils {
    @SubscribeEvent
    fun onVoiceDisconnect(e: GuildVoiceLeaveEvent) {
        val member = e.guild.selfMember
        waiter.executor.schedule({
            if (member.voiceState.channel != null && member.voiceState.channel != null && member.voiceState.channel == e.channelLeft && e.channelLeft.members.size == 1) {
                if (e.guild.getData().musicSettings.stayInChannel == false) {
                    e.guild.audioManager.closeAudioConnection()
                    e.guild.musicManager(null).scheduler.channel?.send("Disconnected from **{0}** because I was left all alone :(".tr(e.guild, e.channelLeft.name))
                    e.guild.musicManager(null).scheduler.channel?.send("Need me to stay in this voice channel? You can enable it at {0}".tr(e.guild, "<https://ardentbot.com/manage/${e.guild.id}>"))
                    managers.remove(e.guild.idLong)
                }
                else e.guild.musicManager(null).player.isPaused = true
            }
        }, 15, TimeUnit.SECONDS)
    }

    @SubscribeEvent
    fun onVoiceConnect(e: GuildVoiceJoinEvent) {
        if (e.member != e.guild.selfMember) {
            if (e.channelJoined.members.size == 2 && e.guild.getData().musicSettings.stayInChannel == true) e.guild.musicManager(null).player.isPaused = false
        }
    }
}