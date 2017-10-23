package events

import commands.music.getAudioManager
import main.managers
import main.waiter
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import translation.tr
import utils.discord.getData
import utils.discord.send
import java.util.concurrent.TimeUnit

class VoiceUtils {
    @SubscribeEvent
    fun onVoiceDisconnect(e: GuildVoiceLeaveEvent) {
        val member = e.guild.selfMember
        waiter.executor.schedule({
            if (member.voiceState.channel != null && member.voiceState.channel != null && member.voiceState.channel == e.channelLeft && e.channelLeft.members.size == 1) {
                if (!e.guild.getData().musicSettings.stayInChannel) {
                    e.guild.audioManager.closeAudioConnection()
                    e.guild.getAudioManager(null).channel?.send("Disconnected from **{0}** because I was left all alone :(".tr(e.guild, e.channelLeft.name))
                    e.guild.getAudioManager(null).channel?.send("Do you need me to stay in this voice channel? You can enable it at {0}".tr(e.guild, "<https://ardentbot.com/manage/${e.guild.id}>"))
                    managers.remove(e.guild.idLong)
                } else e.guild.getAudioManager(null).player.isPaused = true
            }
        }, 15, TimeUnit.SECONDS)
    }

    @SubscribeEvent
    fun onVoiceConnect(e: GuildVoiceJoinEvent) {
        if (e.member != e.guild.selfMember) {
            if (e.channelJoined.members.size == 2 && e.guild.getData().musicSettings.stayInChannel) {
                e.guild.getAudioManager(null).player.isPaused = false
            }
        }
    }
}