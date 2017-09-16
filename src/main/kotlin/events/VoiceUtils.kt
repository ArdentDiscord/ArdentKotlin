package events

import commands.music.getGuildAudioPlayer
import main.managers
import net.dv8tion.jda.core.events.ShutdownEvent
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceLeaveEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import utils.send
import utils.tr

class VoiceUtils {
    @SubscribeEvent
    fun onVoiceDisconnect(e: GuildVoiceLeaveEvent) {
        val member = e.guild.selfMember
        if (member.voiceState.channel != null && member.voiceState.channel != null && member.voiceState.channel == e.channelLeft && e.channelLeft.members.size == 1) {
            e.guild.audioManager.closeAudioConnection()
            e.guild.getGuildAudioPlayer(null).scheduler.manager.getChannel()?.send("Disconnected from **{0}** because I was left all alone :(".tr(e.guild, e.channelLeft.name))
            managers.remove(e.guild.idLong)
        }
    }
}