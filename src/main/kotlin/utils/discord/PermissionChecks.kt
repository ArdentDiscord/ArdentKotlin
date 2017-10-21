package utils.discord

import com.sedmelluq.discord.lavaplayer.player.AudioPlayer
import commands.music.connect
import commands.music.getAudioManager
import main.conn
import main.r
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import translation.tr
import utils.functionality.Emoji

fun Guild.checkCurrentlyPlaying(channel: TextChannel): Boolean {
    if (getAudioManager(channel).manager.current != null) return true
    channel.send("${Emoji.HEAVY_MULTIPLICATION_X} " + "There isn't a currently playing track!".tr(channel.guild))
    return false
}

fun Member.hasRole(vararg searchRoles: String): Boolean {
    roles.forEach { memberRole -> if (searchRoles.contains(memberRole.id)) return true }
    return false
}

fun User.isStaff(): Boolean {
    return r.table("staff").filter { r.hashMap("id", id) }.run<Any>(conn) != null
}

fun Member.hasPermission(channel: TextChannel, musicCommand: Boolean = false, failQuietly: Boolean = false): Boolean {
    val has = if (isOwner || hasPermission(channel, Permission.ADMINISTRATOR) || hasPermission(Permission.BAN_MEMBERS)) true
    else {
        if (!musicCommand) false
        else {
            if (voiceState.inVoiceChannel() && guild.selfMember.voiceState.inVoiceChannel()) voiceState.channel.members.size == 2
            else false
        }
    }
    if (!has && !failQuietly) channel.send("You need `Administrator` priviledges in this server to be able to use this command")
    return has
}


fun Member.checkSameChannel(textChannel: TextChannel?, complain: Boolean = true): Boolean {
    if (voiceState.channel == null) {
        textChannel?.send("${Emoji.CROSS_MARK} " + "You need to be connected to a voice channel".tr(textChannel.guild))
        return false
    }
    if (guild.selfMember.voiceState.channel != voiceState.channel) {
        return voiceState.channel.connect(textChannel, complain)
    }
    return true
}