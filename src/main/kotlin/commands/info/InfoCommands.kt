package commands.info

import events.Category
import events.Command
import events.toCategory
import main.factory
import main.jda
import main.waiter
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import utils.*

class Ping : Command(Category.INFO, "ping", "what did you think this command was gonna do?") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val currentTime = System.currentTimeMillis()
        event.channel.sendReceive(member, "I'll calculate my ping to Discord using this message")
                ?.editMessage("**Socket Ping**: *${System.currentTimeMillis() - currentTime} milliseconds*")?.queue()
        waiter.waitForMessage(Settings(event.author.id), { message: Message -> println(message.content) })
    }
}

class Invite : Command(Category.INFO, "invite", "Get the invite link for the bot") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val channelInvite = jda!!.asBot().getInviteUrl(Permission.MESSAGE_MANAGE, Permission.MANAGE_SERVER, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY)
        channel.send(member, "My invite link is $channelInvite - have fun using Ardent!")
    }
}
class Donate : Command(Category.INFO, "donate", "Learn how to support Ardent and get special perks for it!"){
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Want to support our work and obtain some perks along the way? Head to https://ardentbot.com/support_us to see the different ways " +
                "you could help us out!")
    }
}
class Settings : Command(Category.INFO, "settings", "Learn how to support Ardent and get special perks for it!", "website"){
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Manage the settings for this server at https://www.ardentbot.com/manage/${guild.id} - while you're there, be sure to check out " +
                "the rest of our website!")
    }
}
class About: Command(Category.INFO, "about","learn more about Ardent"){
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val builder = embed("About the bot and its founders", channel.guild.selfMember)
        builder.appendDescription("Ardent was originally founded in November 2016 by ${jda!!.asBot().applicationInfo.complete().owner.withDiscrim()}. It reached over 4,000 servers " +
                "by June, but Adam had to shut it down due to chronic stability issues with the bot and the fact that he was going on " +
                "a language learning program without any internet for nearly two months. When he came back, he decided to recreate Ardent with a " +
                "new focus on modern design, utility, usability, and games. This is the continuation of the original Ardent bot. We hope you like it!")
        channel.send(member, builder)
    }
}


class Help : Command(Category.INFO, "help", "can you figure out what this does? it's a grand mystery!", "h") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val categories = Category.values().map { it.toString() }.toMutableList().shuffle()
        channel.selectFromList(member, "Which category of commands do you need help in?", categories, {
            number ->
            val category = categories[number].toCategory()
            val categoryCommands = factory.commands.filter { it.category == category }.toMutableList().shuffle()
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