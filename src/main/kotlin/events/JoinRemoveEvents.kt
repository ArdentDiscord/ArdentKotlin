package events

import main.waiter
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Role
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.guild.GuildJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberJoinEvent
import net.dv8tion.jda.core.events.guild.member.GuildMemberLeaveEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import utils.*
import java.awt.Color

class JoinRemoveEvents {
    @SubscribeEvent
    fun onGuildAdd(e: GuildJoinEvent) {
        val guild = e.guild
        guild.publicChannel.sendMessage("Thanks for adding Ardent! To open an interactive list where you can see our list of commands, please " +
                "type `/help` or `ardent help`. You can change the default prefix, which is a forward slash, by typing `/prefix set prefix_here`, " +
                "but if you forget what you set it as, remember that `ardent help` will always work.\n" +
                "Happy Discording and best wishes from the development team!").queue()
        if (guild.getRolesByName("muted", true).size == 0 && guild.selfMember.hasPermission(guild.publicChannel, Permission.MESSAGE_WRITE)) {
            guild.publicChannel.sendMessage("\n**--------------------------**\n" +
                    "To enable me to issue mutes, you either need to create a role called `Muted` and deny that role permission " +
                    "to send messages, or I can automagically create it for you. Would you like me to create this role? Type **yes** if so or **no** to skip creation.").queue()
            waiter.gameChannelWait(guild.publicChannel.id, { message ->
                if (guild.getMember(message.author).hasOverride(guild.publicChannel)) {
                    if (message.content.equals("no", true)) {
                        guild.publicChannel.sendMessage("Have fun using Ardent! However, keep in mind that you will not be able to use `/mute` without a **Muted** role").queue()
                        return@gameChannelWait
                    }
                    guild.controller.createRole().setColor(Color.PINK).setMentionable(true).setName("Muted").queue({ role ->
                        val succeeded = mutableListOf<TextChannel>()
                        val failed = mutableListOf<TextChannel>()
                        guild.textChannels.forEach { channel ->
                            channel.createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE).queue({
                                succeeded.add(channel)
                            }, {})
                        }
                        if (succeeded.size == 0) {
                            guild.publicChannel.sendMessage("Success! You will need to manually disable role permissions in your channels, but the role was successfully created").queue()
                        } else if (failed.size == 0) {
                            guild.publicChannel.sendMessage("Successfully created role. Try `/help` to see our commands!").queue()
                        } else {
                            guild.publicChannel.sendMessage("I set Permission Overrides for the following channels: ${succeeded.map { it.name }.stringify()}. " +
                                    "You will need to set them manually for other channels. Try `/help` to get started with Ardent!")
                        }
                    }, {
                        guild.publicChannel.sendMessage("Failed to create role due to lack of permission. Keep in mind that you will not be able to use `/mute` without a **Muted** role").queue()
                    })
                } else guild.publicChannel.sendMessage("${message.author.asMention}, you don't have sufficient status in this server to accept or deny this offer!")
            }, time = 30)
        }
    }

    @SubscribeEvent
    fun onMemberJoin(e: GuildMemberJoinEvent) {
        val data = e.guild.getData()
        val joinMessage = data.joinMessage
        if (joinMessage != null) {
            val channel: TextChannel? = e.guild.getTextChannelById(joinMessage.second)
            if (channel != null) {
                if (joinMessage.first /* Message */ != null) {
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
                e.guild.controller.addRolesToMember(e.member, role).reason("Default Role - Automatic Addition")
                        .queue({
                            e.member.user.openPrivateChannel().queue({ channel ->
                                channel.send(e.member, "Added the role **${role.name}** to you in the **${e.guild.name}** server by default")
                            })
                        }, {
                            e.member.user.openPrivateChannel().queue({ channel ->
                                channel.send(e.member, "Unable to give you the default role **${role.name}** in **${e.guild.name}**. Please contact the server " +
                                        "owner or administrators to let them know.")
                            })
                        })
            }
        }
    }

    @SubscribeEvent
    fun onMemberLeave(e: GuildMemberLeaveEvent) {
        val data = e.guild.getData()
        val leaveMessage = data.joinMessage
        if (leaveMessage != null) {
            val channel: TextChannel? = e.guild.getTextChannelById(leaveMessage.second)
            if (channel != null) {
                if (leaveMessage.first /* Message */ != null) {
                    channel.send(e.guild.owner,
                            leaveMessage.first!!.replace("\$username", e.member.withDiscrim())
                                    .replace("\$usermention", e.member.withDiscrim())
                                    .replace("\$membercount", e.guild.members.size.toString())
                                    .replace("\$servername", e.guild.name)
                    )
                }
            }
        }
    }
}