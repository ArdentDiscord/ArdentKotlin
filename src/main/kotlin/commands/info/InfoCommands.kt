package commands.info

import events.Category
import events.Command
import main.factory
import main.waiter
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*

class Ping : Command(Category.INFO, "ping", "what did you think this command was gonna do?") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val currentTime = System.currentTimeMillis()
        event.channel.sendReceive(member, "I'll calculate my ping to Discord using this message")
                ?.editMessage("**Socket Ping**: *${System.currentTimeMillis() - currentTime} milliseconds*")?.queue()
        waiter.waitForMessage(Settings(event.author.id), { message: Message -> println(message.content) })
    }
}

class Help : Command(Category.INFO, "help", "can you figure out what this does? it's a grand mystery!", "h") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val categories = Category.values().map { it.toString() }.toMutableList()
        channel.selectFromList(member, "Which category of commands do you need help in?", categories, {
            number ->
            val category = Category.values()[number]
            val categoryCommands = factory.commands.filter { it.category == category }
            val embed = embed("${category.fancyName} Commands", member)
                    .appendDescription("*${category.description}*")
            categoryCommands.forEach { command ->
                embed.appendDescription("\n${Emoji.SMALL_ORANGE_DIAMOND} **${command.name}**: *${command.description}*")
                if (command.aliases.isNotEmpty()) {
                    embed.appendDescription("\n")
                    if (command.aliases.size > 1) embed.appendDescription("         __aliases: [${command.aliases.toList().stringify()}]__")
                    else embed.appendDescription("         __alias: ${command.aliases.toList().stringify()}__")
                }
            }
            channel.send(member, embed)
        }, "__*Did you know you can also type \"_ardent help_\" along with \"_/help_\" ? You can also change the set prefix for your server!*__")
    }
}