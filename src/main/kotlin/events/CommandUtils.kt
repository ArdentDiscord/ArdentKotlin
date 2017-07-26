package events

import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import utils.*
import java.awt.Color

class CommandFactory {
    val commands = mutableListOf<Command>()

    fun addCommand(command: Command): CommandFactory {
        commands.add(command)
        return this
    }

    @SubscribeEvent
    fun onMessageEvent(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        val args = event.message.rawContent.split(" ").toMutableList()
        val prefix = event.guild.getPrefix()

        when (args[0]) {
            "ardent" -> {
                args.removeAt(0)
            }
            else -> {
                if (args[0].startsWith(prefix)) {
                    args[0] = args[0].replace(prefix, "")
                } else return
            }
        }

        commands.forEach {
            cmd ->
            if (cmd.containsAlias(args[0])) {
                args.removeAt(0)
                cmd.execute(args, event)
                return
            }
        }
    }
}

abstract class Command(val category: Category, val name: String, val description: String, vararg val aliases: String) {
    val help = mutableListOf<Pair<String, String>>()
    fun execute(args: MutableList<String>, event: MessageReceivedEvent) {
        if (event.channelType == ChannelType.PRIVATE)
            event.author.openPrivateChannel().queue {
                channel ->
                channel.send(event.author, "Please use commands inside a Discord server!")
            }
        else execute(event.member, event.textChannel, event.guild, args, event)
    }

    abstract fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent)

    fun withHelp(syntax: String, description: String): Command {
        help.add(Pair(syntax, description))
        return this
    }

    fun displayHelp(channel: TextChannel, member: Member) {
        val embed = embed("How can I use ${channel.guild.getPrefix()}$name ?", member, Color.BLACK)
                .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/f/f6/Lol_question_mark.png")
                .setFooter("Aliases: ${aliases.toList().stringify()}", member.user.avatarUrl)
        embed.appendDescription("description: *$description*\n")
        help.forEach { embed.appendDescription("\n${Emoji.SMALL_BLUE_DIAMOND}**${it.first}**: *${it.second}*") }
        embed.appendDescription("\n\nType ${channel.guild.getPrefix()}help to view a full list of commands")
        channel.send(member, embed)
        help.clear()
    }

    fun containsAlias(arg: String): Boolean {
        return name.equals(arg, true) || aliases.contains(arg)
    }
}

enum class Category(val fancyName : String, val description: String) {
    MUSIC("Music & Radio", "Play your favorite tracks or listen to the radio, all inside Discord"),
    INFO("Bot Information", "Curious about the status of Ardent? Want to know how to help us continue development? This is the category for you!");

    override fun toString(): String {
        return fancyName
    }
}