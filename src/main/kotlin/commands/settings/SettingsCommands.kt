package commands.settings

import events.Category
import events.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import translation.tr
import utils.discord.getData
import utils.discord.send
import utils.functionality.Emoji

class Prefix : Command(Category.SETTINGS, "prefix", "view or change your server's prefix for Ardent") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = event.guild.getData()
        if (arguments.size != 2 || arguments.getOrNull(0)?.equals("set", true) != true) {
            event.channel.send("${Emoji.INFORMATION_SOURCE} " + "The current prefix is **{PREFIX}**\nYou can change it by typing **{PREFIX}prefix set PREFIX_HERE** - Spaces are not allowed".tr(event))
            return
        } else {
            if (!event.member.hasPermission(event.textChannel)) return
            data.prefixSettings.prefix = arguments[1]
            data.update(true)
            event.channel.send("The prefix has been updated to **{0}**".tr(event, data.prefixSettings.prefix))
        }
    }
}