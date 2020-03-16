package utils.discord

import commands.music.connect
import commands.music.getAudioManager
import main.conn
import main.r
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
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
    if (isOwner || hasPermission(channel, Permission.ADMINISTRATOR) || hasPermission(Permission.BAN_MEMBERS)) return true
    else {
        val data = guild.getData()
        if (!musicCommand) return false else {
            if (data.musicSettings.canEveryoneUseAdminCommands || voiceState?.inVoiceChannel() == true && guild.selfMember.voiceState?.inVoiceChannel() == true) voiceState?.channel?.members?.size == 2
            else {
                roles.forEach { role -> if (data.musicSettings.whitelistedRolesForAdminCommands.contains(role.id)) return true }
                val manager = guild.getAudioManager(channel)
                return manager.manager.current?.user == user.id
            }
        }
    }
    if (!failQuietly) channel.send("You need `Administrator` priviledges in this server to be able to use this command")
    return false
}


fun Member.checkSameChannel(textChannel: TextChannel?, complain: Boolean = true): Boolean {
    if (voiceState?.channel == null) {
        textChannel?.send("${Emoji.CROSS_MARK} " + "You need to be connected to a voice channel".tr(textChannel.guild))
        return false
    }
    if (guild.selfMember.voiceState?.channel != voiceState?.channel) {
        return voiceState?.channel?.connect(textChannel, complain) == true
    }
    return true
}