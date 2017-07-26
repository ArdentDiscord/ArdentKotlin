package commands.manage

import events.Category
import events.Command
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
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