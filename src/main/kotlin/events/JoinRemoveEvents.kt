package events

import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import utils.getData
import utils.send
import utils.withDiscrim

class JoinRemoveEvents {
    @SubscribeEvent
    fun onGuildAdd(e: GuildJoinEvent) {
        val guild = e.guild
        try {
            guild.publicChannel.sendMessage("Thanks for adding Ardent! To open an interactive list where you can see our list of commands, please " +
                    "type `/help` or `ardent help`. You can change the default prefix, which is a forward slash, by typing `/prefix set prefix_here`, " +
                    "but if you forget what you set it as, remember that `ardent help` will always work.\n" +
                    "Happy Discording and best wishes from the development team!").queue()
        } catch (e: Exception) {
            return
        }
    }

    @SubscribeEvent
    fun onMemberJoin(e: GuildMemberJoinEvent) {
        val data = e.guild.getData()
        val joinMessage = data.joinMessage
        if (joinMessage != null) {
            val channel: TextChannel? = e.guild.getTextChannelById(joinMessage.second ?: "1")
            if (channel != null) {
                if (joinMessage.first /* Message */ != null && !joinMessage.first!!.isEmpty()) {
                    channel.send(e.guild.owner,
                            joinMessage.first!!.replace("\$username", e.member.withDiscrim())
                                    .replace("\$usermention", e.member.asMention)
                                    .replace("\$membercount", e.guild.members.size.toString())
                                    .replace("\$servername", e.guild.name)
                    )
                }
            }
        }
        if (data.defaultRole != null) {
            val role: Role? = e.guild.getRoleById(data.defaultRole)
            if (role != null) {
                try {
                    e.guild.controller.addRolesToMember(e.member, role).reason("Default Role - Automatic Addition")
                            .queue({}, {
                                e.member.user.openPrivateChannel().queue({ channel ->
                                    channel.send(e.member, "Unable to give you the default role **${role.name}** in **${e.guild.name}**. Please contact the server " +
                                            "owner or administrators to let them know.")
                                })
                            })
                } catch (e: Exception) {
                }
            }
        }
    }

    @SubscribeEvent
    fun onMemberLeave(e: GuildMemberLeaveEvent) {
        val data = e.guild.getData()
        val leaveMessage = data.leaveMessage
        if (leaveMessage != null) {
            val channel: TextChannel? = e.guild.getTextChannelById(leaveMessage.second ?: "1")
            if (channel != null) {
                if (leaveMessage.first /* Message */ != null && !leaveMessage.first!!.isEmpty()) {
                    channel.send(e.guild.owner,
                            leaveMessage.first!!.replace("\$username", "**${e.member.withDiscrim()}**")
                                    .replace("\$usermention", "**${e.member.withDiscrim()}**")
                                    .replace("\$membercount", e.guild.members.size.toString())
                                    .replace("\$servername", e.guild.name)
                    )
                }
            }
        }
    }
}