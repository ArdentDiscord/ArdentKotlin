package events

import main.conn
import main.r
import main.test
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import org.apache.commons.lang3.exception.ExceptionUtils
import translation.Languages
import utils.*
import java.awt.Color
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class CommandFactory {
    val commands = mutableListOf<Command>()
    val executor: ExecutorService = Executors.newCachedThreadPool()
    val commandsById = hashMapOf<String, Int>()
    val messagesReceived = AtomicLong(0)

    fun commandsReceived(): Int {
        var temp = 0
        commandsById.forEach { temp += it.value }
        return temp
    }

    fun addCommands(vararg inputCommands: Command): CommandFactory {
        inputCommands.forEach { commands.add(it) }
        return this
    }

    @SubscribeEvent
    fun onMessageEvent(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        event.guild.getData()
        var cont = true
        event.guild.punishments().forEach { punishment ->
            if (punishment != null && punishment.userId == event.author.id) {
                try {
                    event.message.delete().reason("This user is muted").queue()
                    cont = false
                } catch (ignored: Exception) {
                }
            }
        }
        messagesReceived.getAndIncrement()
        if (cont) {
            val member = event.member
            var args = event.message.rawContent.split(" ").toMutableList()
            val prefix = event.guild.getPrefix()
            when {
                args[0].startsWith(prefix) -> args[0] = args[0].replace(prefix, "")
                args[0].startsWith("/") && !test -> args[0] = args[0].replace("/", "")
                args[0] == "ardent" && !test -> args.removeAt(0)
                args[0] == "test" && test -> args.removeAt(0)
                else -> return
            }
            commands.forEach { cmd ->
                val arg = args.concat()
                if (cmd.containsAlias(arg, event.guild)) {
                    when {
                        arg.startsWith(cmd.name) -> args = arg.removePrefix(cmd.name).split(" ").toMutableList()
                        arg.startsWith(cmd.name.tr(event.guild)) -> args = arg.removePrefix(cmd.name.tr(event.guild)).split(" ").toMutableList()
                        else -> {
                            cmd.aliases.forEach { a ->
                                if (arg.startsWith(a)) args = arg.removePrefix(a).split(" ").toMutableList()
                            }
                        }
                    }
                    (0..(args.size - 1))
                            .filter { args[it].isEmpty() }
                            .forEach { args.removeAt(it) }
                    commandsById.incrementValue(cmd.name)
                    val name = event.author.name
                    if (name.contains("faggot", true) || name.contains("nigger") || name.contains("nigga")) {
                        event.channel.send("Here at Ardent, we hate derogatory and discriminatory statements. Thus, {0}, you need to change your username to be able to use any command".tr(event.guild, event.author.asMention))
                    } else {
                        executor.execute {
                            try {
                                val data = member.data()
                                data.gold += 2
                                data.update()
                                cmd.executeInternal(args, event)
                                r.table("commands").insert(r.json(getGson().toJson(
                                        LoggedCommand(cmd.name, event.author.id, System.currentTimeMillis(), System.currentTimeMillis().readableDate())))).runNoReply(conn)
                            } catch (e: Throwable) {
                                e.log()
                                event.channel.send("There was an exception while trying to run this command. Please join {0} and share the following stacktrace:".tr(event.guild, "<https://ardentbot.com/support>") + "\n${ExceptionUtils.getStackTrace(e)}")
                            }
                        }
                    }
                    return
                }
            }
        }
    }
}

abstract class Command(val category: Category, val name: String, val description: String, vararg val aliases: String) {
    val help = mutableListOf<Pair<String, String>>()
    fun executeInternal(args: MutableList<String>, event: MessageReceivedEvent) {
        if (event.channelType == ChannelType.PRIVATE)
            event.author.openPrivateChannel().queue { channel ->
                channel.send("Please use commands inside a Discord server!".tr(Languages.ENGLISH.language))
            }
        else execute(args, event)
    }

    abstract fun execute(arguments: MutableList<String>, event: MessageReceivedEvent)

    fun withHelp(syntax: String, description: String, event: MessageReceivedEvent): Command {
        help.add(Pair(syntax.tr(event), description.tr(event)))
        return this
    }

    fun displayHelp(channel: TextChannel, member: Member) {
        val prefix = member.guild.getPrefix()
        val embed = member.embed("How can I use {0}?".tr(channel.guild).trReplace(channel.guild, "$prefix$name"), Color.BLACK)
                .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/f/f6/Lol_question_mark.png")
                .setFooter("Aliases: {0}".tr(channel.guild).trReplace(channel.guild, aliases.toList().stringify()), member.user.avatarUrl)
                .appendDescription("*${description.tr(channel.guild)}*\n")
        help.forEach { embed.appendDescription("\n${Emoji.SMALL_BLUE_DIAMOND}**${it.first.tr(channel.guild)}**: *${it.second.tr(channel.guild)}*") }
        if (help.size > 0) embed.appendDescription("\n\n**Example**: {0}".tr(channel.guild).trReplace(channel.guild, "$prefix$name ${help[0].first}"))
        embed.appendDescription("\n\nType {0}help to view a full list of commands".tr(channel.guild).trReplace(channel.guild, member.guild.getPrefix()))
        channel.send(embed)
        help.clear()
    }

    fun containsAlias(arg: String, guild: Guild): Boolean {
        val a = arg.split(" ")[0]
        return a == name.tr(guild) || name.equals(a, true) || aliases.contains(a) || aliases.any {a == it }
    }

    override fun toString(): String {
        return Model(category, name, description, aliases).toJson()
    }

    private class Model(val category: Category, val name: String, val description: String, val aliases: Array<out String>)
}

fun String.toCategory(): Category {
    return when (this) {
        "Music" -> Category.MUSIC
        "BotInfo" -> Category.BOT_INFO
        "ServerInfo" -> Category.SERVER_INFO
        "Administrate" -> Category.ADMINISTRATE
        "Games" -> Category.GAMES
        "Fun" -> Category.FUN
        "RPG" -> Category.RPG
        "Language" -> Category.LANGUAGE
        else -> Category.BOT_INFO
    }
}

enum class Category(val fancyName: String, val description: String) {
    GAMES("Games", "Compete against your friends or users around the world in classic and addicting games!"),
    MUSIC("Music", "Play your favorite tracks or listen to the radio, all inside Discord"),
    BOT_INFO("BotInfo", "Curious about the status of Ardent? Want to know how to help us continue development? This is the category for you!"),
    SERVER_INFO("ServerInfo", "Check current information about different aspects of your server"),
    ADMINISTRATE("Administrate", "Administrate your server: this category includes commands like warnings and mutes"),
    FUN("Fun", "Bored? Not interested in the games? We have a lot of commands for you to check out here!"),
    RPG("RPG", "Need a gambling fix? Want to marry someone? Use this category!"),
    LANGUAGE("Language", "Want to change your server's language or translate a phrase?")
    ;

    override fun toString(): String {
        return fancyName
    }
}