package events

import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.GuildLeaveEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import translation.tr
import utils.discord.*
import utils.functionality.Emoji
import utils.functionality.logChannel
import java.util.concurrent.TimeUnit

class JoinRemoveEvents {
    @SubscribeEvent
    fun onGuildAdd(e: GuildJoinEvent) {
        val guild = e.guild
        logChannel?.send("${Emoji.THUMBS_UP.symbol} Joined guild ${guild.name} - ${guild.members.size} members and ${guild.members.filter { it.user.isBot }.size} bots")
        try {
            val ch = guild.getTextChannelById(guild.id) ?: guild.defaultChannel ?: guild.textChannels[0]
            ch.sendMessage(guild.owner.embed("Thanks for adding Ardent!", ch)
                    .appendDescription("If you're new to Ardent, you can read our *Getting Started* page by clicking " +
                            "[here](https://ardentbot.com/getting-started) or see a list of available commands by typing **/help**").build())
                    .queueAfter(3, TimeUnit.SECONDS)
        } catch (ignored: Exception) {
        }
    }

    @SubscribeEvent
    fun onMemberJoin(e: GuildMemberJoinEvent) {
        val data = e.guild.getData()
        val join = data.messageSettings.joinMessage
        if (join != null) {
            if (join.message == null || join.message!!.isEmpty()) return
            getTextChannelById(join.channel)?.send(
                    join.message!!.replace("{username}", e.member.user.toFancyString())
                            .replace("{mention}", e.member.asMention)
                            .replace("{server}", e.guild.name)
                            .replace("{joinplace}", e.guild.members.size.toString()))
        }
        val role = e.guild.getRoleById(data.roleSettings.defaultRole ?: "1") ?: return
        try {
            e.guild.controller.addRolesToMember(e.member, role).reason("Default Role - Automatic Addition").queue()
        } catch (ex: Exception) {
            e.user.openPrivateChannel().queue({ channel ->
                channel.send(("Unable to give you the default role **{0}** in **{1}**. " +
                        "Please contact the server owner or administrators to let them know.").tr(e.guild, role.name, e.guild.name))
            })
        }
    }
}

@SubscribeEvent
fun onMemberLeave(e: GuildMemberLeaveEvent) {
    val data = e.guild.getData()
    val leave = data.messageSettings.leaveMessage ?: return
    if (leave.message == null || leave.message!!.isEmpty()) return
    getTextChannelById(leave.channel)?.send(
            leave.message!!.replace("{username}", e.member.user.toFancyString())
                    .replace("{mention}", e.member.asMention)
                    .replace("{server}", e.guild.name)
                    .replace("{joinplace}", e.guild.members.size.toString()))
}

@SubscribeEvent
fun onGuildLeave(e: GuildLeaveEvent) {
    val guild = e.guild
    guild.owner.user.openPrivateChannel().queue {
        it.send("We're very sad to see you leave ${Emoji.SLIGHTLY_FROWNING_FACE.symbol} If you had any issues that caused you to remove Ardent, you can always join https://discord.gg/sVkfYbX " +
                "and we'd be happy to work with you")
    }
    logChannel?.send("\uD83D\uDC4E Left guild ${guild.name} - ${guild.members.size} members and ${guild.members.filter { it.user.isBot }.size} bots")
}
