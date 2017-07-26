package commands.manage

import events.Category
import events.Command
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent

class Prefix : Command(Category.MANAGE, "prefix", "view or change your server's prefix for Ardent") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        TODO("lazy")
    }
}