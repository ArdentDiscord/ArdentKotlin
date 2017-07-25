package main

import utils.GuildMusicManager
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import utils.getPrefix
import utils.send

class CommandFactory {
    val commands = mutableListOf<Command>()

    fun addCommand(command: Command) {
        commands.add(command)
    }

    @SubscribeEvent
    fun onMessageEvent(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        val args = event.message.rawContent.split(" ").toMutableList()
        val prefix = event.guild.getPrefix()

        when (args[0]) {
            "route" -> {
                args[0] = args[0].replace("route", "")
            }
            else -> {
                if (args[0].startsWith(prefix)) {
                    args[0] = args[0].replace(prefix, "")
                }
                else return
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

abstract class Command(val category : Category, val name: String, val description: String, vararg val aliases: String) {
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

    fun withHelp(syntax: String, description: String) {
        help.add(Pair(syntax, description))
    }

    fun displayHelp() {
        TODO("later")
    }

    fun containsAlias(arg: String): Boolean {
        return name.equals(arg, true) || aliases.contains(arg)
    }
}

enum class Category {
    MUSIC, INFO
}