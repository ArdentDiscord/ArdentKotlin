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
        channel.send(member, "The invite link for the bot is $channelInvite")
        try {
            guild.publicChannel.createInvite().setMaxUses(0).setUnique(true).queue { createdInvite ->
                channel.send(member, "The default invite to your server is https://discord.gg/${createdInvite.code}")
            }
        } catch(e: Exception) {
            channel.send(member, "I don't have permissions to view server invites! Please update my permissions!")
        }
    }
}
class Donate : Command(Category.INFO, "donate", "Learn how to support Ardent and get special perks for it!"){
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Want to support Ardent? We need your help! Pledge at https://patreon.com/ardent and receive perks and the " +
                "satisfaction of helping us maintain our bot, used by over 500 people !\n" +
                " You can also donate directly at https://www.paypal.me/ardentbot")
    }
}
class Website : Command(Category.INFO, "website", "Learn how to support Ardent and get special perks for it!"){
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "https://www.ardentbot.com/")
    }
}
class About: Command(Category.INFO, "about","learn more about Ardent"){
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val builder = embed("About Ardent", channel.guild.selfMember)
        builder.appendDescription("Ardent Bot was originally founded by Adam#9261 in Decemeber 2016. It quickly reached over 4,000 servers until Adam had to shut it down due to issues with " +
                "the bot and him leaving for a trip. When he came back, he decided to create a bug free bot with a focus on games. This is the continuation of the original Ardent bot.")
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