package commands.administrate

import commands.games.Connect4Game
import events.Category
import events.Command
import main.config
import main.conn
import main.jdas
import main.r
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.requests.RestAction
import utils.*
import java.awt.Color
import java.util.*
import java.util.concurrent.TimeUnit


class Prefix : Command(Category.ADMINISTRATE, "prefix", "view or change your server's prefix for Ardent") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = event.guild.getData()

        if (arguments.size != 2) {
            event.channel.send("${Emoji.INFORMATION_SOURCE} The current prefix is **${data.prefix}**\n" +
                    "See a list of all commands by typing **${data.prefix}help** __or__ **ardent help**\n\n" +
                    "Change the current prefix by typing **${data.prefix}prefix set PREFIX_HERE** - Spaces are not allowed")
            return
        }
        if (arguments[0].equals("set", true)) {
            if (!event.member.hasOverride(event.textChannel, false)) return
            data.prefix = arguments[1]
            data.update()
            event.channel.send("The prefix has been updated to **${data.prefix}**")
        } else event.channel.send("${Emoji.NO_ENTRY_SIGN} Type **${data.prefix}prefix** to learn how to use this command")
    }
}

class Clear : Command(Category.ADMINISTRATE, "clear", "clear messages in the channel you're sending the command in") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member.hasOverride(event.textChannel)) return
        if (!event.guild.selfMember.hasPermission(event.textChannel, Permission.MESSAGE_MANAGE)) {
            event.channel.send("I need the `Message Manage` permission to be able to delete messages!")
            return
        }
        if (arguments.size == 0) event.channel.send("You need to specify a number of messages - *or mention a member whose messages* to delete!")
        else {
            val mentionedUsers = event.message.mentionedUsers
            if (mentionedUsers.size > 0) {
                event.channel.history.retrievePast(100).queue { messages ->
                    val messagesByUser = mutableListOf<Message>()
                    messages.forEach { m -> if (m.author == mentionedUsers[0]) messagesByUser.add(m) }
                    event.textChannel.deleteMessages(messagesByUser).queue({
                        event.channel.send("Successfully deleted **${messagesByUser.size}** messages from *${mentionedUsers[0].withDiscrim()}*")
                    }, {
                        event.channel.send("Unable to delete messages, please recheck my permissions")
                    })
                }
                return
            }
            val number = arguments[0].toIntOrNull()
            if (number == null || number < 2 || number > 100) event.channel.send("Invalid number specified. Number must be between **2** and **100** messages")
            else {
                event.channel.history.retrievePast(number).queue { messages ->
                    try {
                        event.textChannel.deleteMessages(messages).queue {
                            event.channel.send("Successfully cleared **$number** messages")
                        }
                    } catch (e: Exception) {
                        event.channel.send("Unable to clear messages - ${e.localizedMessage}")
                    }
                }
            }
        }
    }
}

class Tempban : Command(Category.ADMINISTRATE, "tempban", "temporarily ban someone", "tban") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0 || arguments.size < 2) {
            event.channel.send("You need to mention a member and the amount of hours to ban them! **Example**: `${event.guild.getPrefix()}tempban @User 4` - the 4 " +
                    "represents the amount of hours")
            return
        }
        if (!event.member.hasOverride(event.textChannel)) return
        val toBan = event.guild.getMember(mentionedUsers[0])
        if (toBan.hasOverride(event.textChannel, failQuietly = true)) event.channel.send("You don't have permission to ban this person!")
        else if (event.member.canInteract(toBan)) {
            if (event.guild.selfMember.canInteract(toBan)) {
                val hours = arguments[1].toIntOrNull()
                if (hours == null) event.channel.send("You need to provide an amount of hours to ban this user!")
                else {
                    try {
                        event.guild.controller.ban(toBan, 1, "banned by ${event.author.withDiscrim()} for $hours hours").queue({
                            Punishment(toBan.id(), event.author.id, event.guild.id, Punishment.Type.TEMPBAN, System.currentTimeMillis() + (hours * 60 * 60 * 1000)).insert("punishments")
                            event.channel.send("Successfully banned **${toBan.withDiscrim()}** for **$hours** hours")
                        }, {
                            event.channel.send("Failed to ban **${toBan.withDiscrim()}**. Check my permissions..?")
                        })
                    } catch (e: Exception) {
                        event.channel.send("Failed to ban **${toBan.withDiscrim()}**. Check my permissions..?")
                    }
                }
            } else event.channel.send("I don't have permission to ban this user!")
        } else event.channel.send("You cannot ban this person!")

    }
}

class Punishments : Command(Category.ADMINISTRATE, "punishments", "see a list of everyone in this server who currently has a punishment") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val embed = event.member.embed("Punishments in ${event.guild.name}")
        val builder = StringBuilder()
        val punishments = event.guild.punishments()
        if (punishments.size == 0) builder.append("There are no current punishments in this server.")
        else {
            punishments.forEach { punishment ->
                builder.append(" ${Emoji.NO_ENTRY_SIGN} **${punishment?.userId?.toUser()?.withDiscrim()}**: ${punishment?.type} until ${punishment?.expiration?.readableDate()}\n")
            }
        }
        event.channel.send(embed.setColor(Color.RED).setDescription(builder.toString()))
    }
}

class Automessages : Command(Category.ADMINISTRATE, "joinleavemessage", "set join or leave messages for new or leaving members") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("You can manage settings for the **join** and **leave** messages on the web panel @ <https://ardentbot.com/manage/${event.guild.id}>")
    }
}

class Mute : Command(Category.ADMINISTRATE, "mute", "temporarily mute members who abuse their ability to send messages") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0 || arguments.size != 2) event.channel.send("""**Muting**: The first parameter for this command must be a mention of the member you wish to mute (obviously)
The second parameter is the amount of time to mute them for - this can be in minutes, hours, or days.
Type the number you wish (decimals are **not** allowed) and then suffix that with **m** (for minutes), **h** (for hours), or **d** (for days)
**Example**: *${event.guild.getPrefix()}mute @User 3h* - would mute that user for three hours""")
        else {
            if (!event.member.hasOverride(event.textChannel)) return
            val muteMember = event.guild.getMember(mentionedUsers[0])
            if (muteMember.hasOverride(event.textChannel, failQuietly = true) || !event.member.canInteract(muteMember) || !event.guild.selfMember.canInteract(muteMember)) {
                event.channel.send("You don't have permission to mute this member! Make sure that they do not have the `Manage Server` or other elevated " +
                        "permissions")
                return
            }
            val punishments = muteMember.punishments()
            punishments.forEach { punishment ->
                if (punishment != null) {
                    if (punishment.type == Punishment.Type.MUTE) {
                        event.channel.send("**${muteMember.withDiscrim()}** is already muted!")
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
                    event.channel.send("You didn't include a time unit! Type ${event.guild.getPrefix()}mute for help with this command")
                    return
                }
            }
            val number = unparsedTime.toLongOrNull()
            if (number == null) event.channel.send("You specified an invalid number. Type ${event.guild.getPrefix()}mute for help with this command")
            else {
                val unmuteTime = System.currentTimeMillis() + (unit.toMillis(number))
                Punishment(muteMember.id(), event.author.id, event.guild.id, Punishment.Type.MUTE, unmuteTime).insert("punishments")
                event.channel.send("Successfully muted **${muteMember.withDiscrim()}** until ${unmuteTime.readableDate()}")
            }
        }
    }
}

class Unmute : Command(Category.ADMINISTRATE, "unmute", "unmute members who are muted") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0) event.channel.send("Please mention the member you want to unmute!")
        else {
            if (!event.member.hasOverride(event.textChannel)) return
            val unmuteMember = event.guild.getMember(mentionedUsers[0])
            val punishments = unmuteMember.punishments()
            punishments.forEach { punishment ->
                if (punishment != null) {
                    if (punishment.type == Punishment.Type.MUTE) {
                        event.guild.controller.removeRolesFromMember(unmuteMember, event.guild.getRolesByName("muted", true))
                                .reason("Unmuted by ${event.author.withDiscrim()}")
                                .queue({
                                    r.table("punishments").get(punishment.id).delete().runNoReply(conn)
                                    event.channel.send("Successfully unmuted **${unmuteMember.withDiscrim()}**")
                                }, {
                                    event.channel.send("Failed to unmute **${unmuteMember.withDiscrim()}** - Please give me proper permissions to remove roles")
                                })
                        return
                    }
                }
            }
            event.channel.send("This person isn't muted!")
        }
    }
}

class Nono : Command(Category.ADMINISTRATE, "nono", "commands for bot administrators only") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        staff.forEach {
            if (event.author.id == "169904324980244480" || (event.author.id == it.id && it.role == Staff.StaffRole.ADMINISTRATOR)) {
                if (arguments.size == 0) {
                    withHelp("shutdown", "this is really fucking obvious")
                            .withHelp("staff add|remove @User role_name", "^")
                            .withHelp("whitelist", "whitelist your friends (let them have patreon permissions)")
                            .displayHelp(event.textChannel, event.member)
                    return
                }
                when (arguments[0]) {
                    "test" -> {
                        event.channel.send(Connect4Game.GameBoard("1", "2").toString())
                    }
                    "eval" -> {
                        eval(arguments, event)
                    }
                    "shutdown" -> {
                        event.channel.send("Shutting down now!")
                        jdas.forEach { it.shutdown() }
                        System.exit(0)
                    }
                    else -> event.channel.send("You're an idiot")
                }
            }
            return
        }
    }
}

class GiveAll : Command(Category.ADMINISTRATE, "giverole", "give all users who don't have any role, the role you specify", "giveall") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            event.channel.send("You need to type the name of the role that you'd like to give to all members who currently have no roles in this server!")
            return
        }
        if (event.member.hasOverride(event.textChannel)) {
            val query = arguments.concat()
            val results = event.guild.getRolesByName(query, true)
            if (results.size == 0) event.channel.send("No roles with that name were found")
            else {
                event.channel.send("Running... This could take a while.")
                var addedTo = 0
                val role = results[0]
                event.guild.members.forEach { m ->
                    if (m.roles.size < 2) {
                        try {
                            event.guild.controller.addRolesToMember(m, role).queue({
                                addedTo++
                            }, {})
                        } catch (ignored: Exception) {
                        }
                    }
                }
            }
        }
    }
}

fun eval(arguments: MutableList<String>, event: MessageReceivedEvent) {
    arguments.removeAt(0)
    val message = event.message
    val shortcuts = hashMapOf<String, Any>()
    shortcuts.put("jda", message.jda)
    shortcuts.put("event", event)
    shortcuts.put("channel", event.channel)
    shortcuts.put("guild", event.guild)
    shortcuts.put("message", message)
    shortcuts.put("msg", message)
    shortcuts.put("me", event.author)
    shortcuts.put("bot", message.jda.selfUser)
    shortcuts.put("config", config)
    shortcuts.put("jdas", jdas)
    val timeout = 10
    val result = Engine.GROOVY.eval(shortcuts, Collections.emptyList(), Engine.DEFAULT_IMPORTS, timeout, arguments.without(arguments[0]).concat())
    val builder = MessageBuilder()
    if (result.first is RestAction<*>)
        (result.first as RestAction<*>).queue()
    else if (result.first != null && (result.first as String).isNotEmpty())
        builder.appendCodeBlock(result.first.toString(), "")
    if (!result.second.isEmpty() && result.first != null)
        builder.append("\n").appendCodeBlock(result.first as String, "")
    if (!result.third.isEmpty())
        builder.append("\n").appendCodeBlock(result.third, "")
    if (builder.isEmpty)
        event.message.addReaction("✅").queue()
    else
        for (m in builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE, MessageBuilder.SplitPolicy.SPACE, MessageBuilder.SplitPolicy.ANYWHERE))
            event.channel.sendMessage(m).queue()
}