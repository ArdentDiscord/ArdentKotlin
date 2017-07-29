package commands.manage

import events.Category
import events.Command
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*

class Prefix : Command(Category.MANAGE, "prefix", "view or change your server's prefix for Ardent") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = guild.getData()

        if (arguments.size != 2) {
            channel.send(member, "${Emoji.INFORMATION_SOURCE} The current prefix is **${data.prefix}**\n" +
                    "See a list of all commands by typing **${data.prefix}help** __or__ **ardent help**\n\n" +
                    "Change the current prefix by typing **${data.prefix}prefix set PREFIX_HERE** - Spaces are not allowed")
            return
        }
        if (arguments[0].equals("set", true)) {
            assert(member.hasOverride(channel, false))
            data.prefix = arguments[1]
            data.update()
        }
        else channel.send(member, "${Emoji.NO_ENTRY_SIGN} Type **${data.prefix}prefix** to learn how to use this command")
    }
}

class Clear : Command(Category.MANAGE, "clear", "clear messages in the channel you're sending the command in") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        assert(member.hasOverride(channel))
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

class Tempban : Command(Category.MANAGE, "tempban", "temporarily ban someone", "tban") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0) {
           // channel.send
            return
        }
        assert(member.hasOverride(channel))

    }
}