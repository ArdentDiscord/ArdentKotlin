package events

import commands.music.getGuildAudioPlayer
import main.managers
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import utils.send

class VoiceUtils {
    @SubscribeEvent
    fun onVoiceDisconnect(e: GuildVoiceLeaveEvent) {
        val member = e.guild.selfMember
        if (member.voiceState.channel != null && member.voiceState.channel != null && member.voiceState.channel == e.channelLeft && e.channelLeft.members.size == 1) {
            e.guild.audioManager.closeAudioConnection()
            e.guild.getGuildAudioPlayer(null).scheduler.manager.getChannel()?.send(e.member, "Disconnected from **${e.channelLeft.name}** because I was left all " +
                    "alone :(")
            managers.remove(e.guild.idLong)
        }
    }
}