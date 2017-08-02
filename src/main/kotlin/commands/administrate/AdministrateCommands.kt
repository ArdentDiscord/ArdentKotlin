package commands.administrate

import events.Category
import events.Command
import main.conn
import main.jdas
import main.r
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*
import java.awt.Color
import java.time.Instant
import java.util.*
import java.util.concurrent.TimeUnit

class Prefix : Command(Category.ADMINISTRATE, "prefix", "view or change your server's prefix for Ardent") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = guild.getData()

        if (arguments.size != 2) {
            channel.send(member, "${Emoji.INFORMATION_SOURCE} The current prefix is **${data.prefix}**\n" +
                    "See a list of all commands by typing **${data.prefix}help** __or__ **ardent help**\n\n" +
                    "Change the current prefix by typing **${data.prefix}prefix set PREFIX_HERE** - Spaces are not allowed")
            return
        }
        if (arguments[0].equals("set", true)) {
            if (!member.hasOverride(channel, false)) return
            data.prefix = arguments[1]
            data.update()
        } else channel.send(member, "${Emoji.NO_ENTRY_SIGN} Type **${data.prefix}prefix** to learn how to use this command")
    }
}

class Clear : Command(Category.ADMINISTRATE, "clear", "clear messages in the channel you're sending the command in") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!member.hasOverride(channel)) return
        if (!guild.selfMember.hasPermission(channel, Permission.MESSAGE_MANAGE)) {
            channel.send(member, "I need the `Message Manage` permission to be able to delete messages!")
            return
        }
        if (arguments.size == 0) channel.send(member, "You need to specify a number of messages - *or mention a member whose messages* to delete!")
        else {
            val mentionedUsers = event.message.mentionedUsers
            if (mentionedUsers.size > 0) {
                channel.history.retrievePast(100).queue { messages ->
                    val messagesByUser = mutableListOf<Message>()
                    messages.forEach { m -> if (m.author == mentionedUsers[0]) messagesByUser.add(m) }
                    channel.deleteMessages(messagesByUser).queue {
                        channel.send(member, "Successfully deleted **${messagesByUser.size}** messages from *${mentionedUsers[0].withDiscrim()}*")
                    }
                }
                return
            }
            var number = arguments[0].toIntOrNull()
            if (number == null || number < 2 || number > 100) channel.send(member, "Invalid number specified. Number must be between **2** and **99** messages")
            else {
                number++
                channel.history.retrievePast(number).queue { messages ->
                    channel.deleteMessages(messages).queue {
                        channel.send(member, "Successfully cleared **$number** messages")
                    }
                }
            }
        }
    }
}

class Tempban : Command(Category.ADMINISTRATE, "tempban", "temporarily ban someone", "tban") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0 || arguments.size < 2) {
            channel.send(member, "You need to mention a member and the amount of hours to ban them! **Example**: `${guild.getPrefix()}tempban @User 4` - the 4 " +
                    "represents the amount of hours")
            return
        }
        if (!member.hasOverride(channel)) return
        val toBan = guild.getMember(mentionedUsers[0])
        if (toBan.hasOverride(channel, failQuietly = true)) channel.send(member, "You don't have permission to ban this person!")
        else if (member.canInteract(toBan)) {
            if (guild.selfMember.canInteract(toBan)) {
                val hours = arguments[1].toIntOrNull()
                if (hours == null) channel.send(member, "You need to provide an amount of hours to ban this user!")
                else {
                    try {
                        guild.controller.ban(toBan, 1, "banned by ${member.withDiscrim()} for $hours hours").queue({
                            Punishment(toBan.id(), member.id(), guild.id, Punishment.Type.TEMPBAN, System.currentTimeMillis() + (hours * 60 * 60 * 1000)).insert("punishments")
                            channel.send(member, "Successfully banned **${toBan.withDiscrim()}** for **$hours** hours")
                        }, {
                            channel.send(member, "Failed to ban **${toBan.withDiscrim()}**. Check my permissions..?")
                        })
                    } catch(e: Exception) {
                        channel.send(member, "Failed to ban **${toBan.withDiscrim()}**. Check my permissions..?")
                    }
                }
            } else channel.send(member, "I don't have permission to ban this user!")
        } else channel.send(member, "You cannot ban this person!")

    }
}

class Punishments : Command(Category.ADMINISTRATE, "punishments", "see a list of everyone in this server who currently has a punishment") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val embed = embed("Punishments in ${guild.name}", member)
        val builder = StringBuilder()
        val punishments = guild.punishments()
        if (punishments.size == 0) builder.append("There are no current punishments in this server.")
        else {
            punishments.forEach { punishment ->
                builder.append(" ${Emoji.NO_ENTRY_SIGN} **${punishment?.userId?.toUser()?.withDiscrim()}**: ${punishment?.type} until ${punishment?.expiration?.readableDate()}\n")
            }
        }
        channel.send(member, embed.setColor(Color.RED).setDescription(builder.toString()))
    }
}

class Automessages : Command(Category.ADMINISTRATE, "joinleavemessage", "set join or leave messages for new or leaving members") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "You can manage settings for the **join** and **leave** messages on the web panel: ${guild.panelUrl()}")
    }
}

class DefaultRole : Command(Category.ADMINISTRATE, "defaultrole", "set a default role that new members will receive when they join") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "You can manage settings for the default role on the web panel: ${guild.panelUrl()}")
    }
}

class Mute : Command(Category.ADMINISTRATE, "mute", "temporarily mute members who abuse their ability to send messages") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0 || arguments.size != 2) channel.send(member,
                """**Muting**: The first parameter for this command must be a mention of the member you wish to mute (obviously)
The second parameter is the amount of time to mute them for - this can be in minutes, hours, or days.
Type the number you wish (decimals are **not** allowed) and then suffix that with **m** (for minutes), **h** (for hours), or **d** (for days)
**Example**: *${guild.getPrefix()}mute @User 3h* - would mute that user for three hours""")
        else {
            if (!member.hasOverride(channel)) return
            val muteMember = guild.getMember(mentionedUsers[0])
            if (muteMember.hasOverride(channel, failQuietly = true) || !member.canInteract(muteMember) || !guild.selfMember.canInteract(muteMember)) {
                channel.send(member, "You don't have permission to mute this member! Make sure that they do not have the `Manage Server` or other elevated " +
                        "permissions")
                return
            }
            val punishments = muteMember.punishments()
            punishments.forEach { punishment ->
                if (punishment != null) {
                    if (punishment.type == Punishment.Type.MUTE) {
                        channel.send(member, "**${muteMember.withDiscrim()}** is already muted!")
                        return
                    }
                }
            }
            var unparsedTime = arguments[1]
            val unit: TimeUnit
            when {
                unparsedTime.endsWith("m") -> {
                    unit = TimeUnit.MINUTES
                    unparsedTime = unparsedTime.removeSuffix("m")
                }
                unparsedTime.endsWith("h") -> {
                    unit = TimeUnit.HOURS
                    unparsedTime = unparsedTime.removeSuffix("h")
                }
                unparsedTime.endsWith("d") -> {
                    unit = TimeUnit.DAYS
                    unparsedTime = unparsedTime.removeSuffix("d")
                }
                else -> {
                    channel.send(member, "You didn't include a time unit! Type ${guild.getPrefix()}mute for help with this command")
                    return
                }
            }
            val number = unparsedTime.toLongOrNull()
            if (number == null) channel.send(member, "You specified an invalid number. Type ${guild.getPrefix()}mute for help with this command")
            else {
                val mutedRoles = guild.getRolesByName("muted", true)
                if (mutedRoles.size == 0) {
                    channel.send(member, "There isn't a `Muted` role in this server! I'll try to create one automatically, but this is necessary.")
                    guild.controller.createRole().setColor(Color.PINK).setMentionable(true).setName("Muted").queue({ role ->
                        val succeeded = mutableListOf<TextChannel>()
                        val failed = mutableListOf<TextChannel>()
                        guild.textChannels.forEach { ch -> ch.createPermissionOverride(role).setDeny(Permission.MESSAGE_WRITE).queue({}, {}) }
                        if (succeeded.size == 0) {
                            guild.publicChannel.sendMessage("Success! You will need to manually disable this role's ability to send messages in your channels, but the role was successfully created").queue()
                        } else if (failed.size == 0) {
                            guild.publicChannel.sendMessage("Successfully created role. Try `/help` to see our commands!").queue()
                        } else {
                            guild.publicChannel.sendMessage("I set Permission Overrides for the following channels: ${succeeded.map { it.name }.stringify()}. " +
                                    "You will need to set them manually for other channels. Try `/help` to get started with Ardent!")
                            mutedRoles.add(role)
                        }
                    }, {
                        guild.publicChannel.sendMessage("Failed to create role due to lack of permission, and therefore failed to mute." +
                                " Keep in mind that you will not be able to use `/mute` without a **Muted** role").queue()
                    })
                }
                if (mutedRoles.size == 1) {
                    guild.controller.addRolesToMember(muteMember, mutedRoles[0]).reason("Muted by ${member.withDiscrim()}").queue({
                        val unmuteTime = System.currentTimeMillis() + (unit.toMillis(number))
                        Punishment(muteMember.id(), member.id(), guild.id, Punishment.Type.MUTE, unmuteTime).insert("punishments")
                        channel.send(member, "Successfully muted **${muteMember.withDiscrim()}** until ${unmuteTime.readableDate()}")
                    }, {
                        channel.send(member, "Failed to add the `Muted` role to **${muteMember.withDiscrim()}**. Please update my permissions and retry1")
                    })
                }
            }
        }
    }
}

class Unmute : Command(Category.ADMINISTRATE, "unmute", "unmute members who are muted") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0) channel.send(member, "Please mention the member you want to unmute!")
        else {
            if (!member.hasOverride(channel)) return
            val unmuteMember = guild.getMember(mentionedUsers[0])
            val punishments = unmuteMember.punishments()
            punishments.forEach { punishment ->
                if (punishment != null) {
                    if (punishment.type == Punishment.Type.MUTE) {
                        guild.controller.removeRolesFromMember(unmuteMember, guild.getRolesByName("muted", true))
                                .reason("Unmuted by ${member.withDiscrim()}")
                                .queue({
                                    r.table("punishments").get(punishment.uuid).delete().runNoReply(conn)
                                    channel.send(member, "Successfully unmuted **${unmuteMember.withDiscrim()}**")
                                }, {
                                    channel.send(member, "Failed to unmute **${unmuteMember.withDiscrim()}** - Please give me proper permissions to remove roles")
                                })
                        return
                    }
                }
            }
            channel.send(member, "This person isn't muted!")
        }
    }
}

class Nono : Command(Category.ADMINISTRATE, "nono", "commands for bot administrators only") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        staff.forEach {
            if (member.id() == it.id && it.role == Staff.StaffRole.ADMINISTRATOR) {
                if (arguments.size == 0) channel.send(member, "no")
                else {
                    when (arguments[0]) {
                        "shutdown" -> {
                            jdas.forEach { it.shutdown() }
                            channel.send(member, "Shut down JDA instances, exiting...")
                            System.exit(0)
                        }
                        "staff" -> {
                            if (arguments.size == 3) {
                                val type = arguments[1]
                                val roleName = arguments[3]
                                val roles = Staff.StaffRole.values().map { it.name.toLowerCase() }
                                if (roles.contains(roleName)) {
                                    if (event.message.mentionedUsers.size == 0) {
                                        channel.send(member, "no")
                                        return
                                    }
                                    val user = event.message.mentionedUsers[0]
                                    if (type == "add") {
                                        if (r.table("staff").get(user.id).run<Any>(conn) != null) {
                                            r.table("staff").get(user.id).update(r.hashMap("role", roleName.toUpperCase())).runNoReply(conn)
                                        } else r.table("staff").insert(r.json(getGson().toJson(Staff(user.id, Staff.StaffRole.valueOf(roleName.toUpperCase()))))).runNoReply(conn)
                                    } else if (type == "remove") {
                                        r.table("staff").get(user.id).delete().runNoReply(conn)
                                    } else {
                                        channel.send(member, "no")
                                        return
                                    }
                                    channel.send(member, "updated database")
                                } else channel.send(member, "/nono staff add|remove @User roleName")
                            } else channel.send(member, "/nono staff remove|add @User roleName")
                        }
                        else -> channel.send(member, "You're an idiot")
                    }
                }
                return@forEach
            }
        }
    }
}

class GiveAll : Command(Category.ADMINISTRATE, "giveall", "give all users who don't have any role, the role you specify") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            channel.send(member, "You need to type the name of the role that you'd like to give to all members who currently have no roles in this server!")
            return
        }
        if (member.hasOverride(channel)) {
            val query = arguments.concat()
            val results = guild.getRolesByName(query, true)
            if (results.size == 0) channel.send(member, "No roles with that name were found")
            else {
                channel.send(member, "Running... This could take a while.")
                var addedTo = 0
                val role = results[0]
                guild.members.forEach { m ->
                    if (m.roles.size < 2) {
                        try {
                            guild.controller.addRolesToMember(m, role).queue({
                                addedTo++
                            }, {})
                        } catch(ignored: Exception) {
                        }
                    }
                }
            }
        }
    }
}