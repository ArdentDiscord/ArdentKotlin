package events

import main.conn
import main.factory
import main.r
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import org.apache.commons.lang3.exception.ExceptionUtils
import translation.tr
import utils.discord.*
import utils.functionality.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

class CommandFactory {
    val commands = mutableListOf<Command>()
    val executor: ExecutorService = Executors.newCachedThreadPool()
    val commandsById = hashMapOf<String, Int>()
    val commandsByShard = hashMapOf<Int, Int>()
    val messagesReceived = AtomicLong(0)
    val derogatoryTerms = getDerogatoryTerms()
    val ratelimits = ConcurrentHashMap<String, Long>()
    fun commandsReceived(): Int {
        var temp = 0
        commandsById.forEach { temp += it.value }
        return temp
    }

    fun addCommands(vararg inputCommands: Command): CommandFactory {
        inputCommands.forEach { commands.add(it); (it as? ExtensibleCommand)?.registerSubcommands() }
        return this
    }

    @SubscribeEvent
    fun onMessageEvent(event: MessageReceivedEvent) {
        if (event.author.isBot) return
        messagesReceived.getAndIncrement()
        val data = event.guild.getData()
        var foundPrefix: String? = null
        if (event.message.rawContent.startsWith(data.prefixSettings.prefix)) foundPrefix = data.prefixSettings.prefix
        if (foundPrefix == null) {
            foundPrefix = if (!data.prefixSettings.disabledDefaultPrefix && event.message.rawContent.startsWith("/")) "/"
            else if (event.message.rawContent.startsWith("ardent")) "ardent"
            else if (event.message.rawContent.startsWith("<@${event.guild.selfMember.user.id}> ")) "<@${event.guild.selfMember.user.id}> "
            else if (event.message.rawContent.startsWith("<@!${event.guild.selfMember.user.id}> ")) "<@!${event.guild.selfMember.user.id}> "
            else return
        }
        val content = event.message.rawContent.removePrefix(foundPrefix).removePrefix(" ")
        if (content.isEmpty()) return
        commands.forEach { cmd ->
            if (cmd.containsAlias(content.split(" ")[0], event.guild)) {
                val args = when {
                    content.startsWith(cmd.name) -> content.removePrefix(cmd.name)
                    content.startsWith(cmd.name.tr(event.guild)) -> content.removePrefix(cmd.name.tr(event.guild))
                    else -> content.removePrefix(cmd.aliases.filter { content.startsWith(it) }[0])
                }.removeStarting(" ").commandSplit()
                commandsById.increment(cmd.name)
                commandsByShard.increment(event.guild.getShard())
                if (derogatoryTerms.filter { event.author.name.contains(it, true) }.count() > 0) {
                    event.channel.send("Here at Ardent, we accept everyone for who they are. You must to change your username to be able to use any command".tr(event.guild))
                } else {
                    if (!event.member.hasPermission(Permission.MANAGE_SERVER)) {
                        when {
                            data.blacklistSettings.blacklistedChannels.contains(event.textChannel.id) -> {
                                event.channel.send("You're not allowed to use Ardent commands in this channel! Type */blacklist list* to view all blacklisted channels".tr(event.guild))
                                return
                            }
                            data.blacklistSettings.blacklistedUsers.contains(event.author.id) -> {
                                event.author.openPrivateChannel().queue {
                                    it.send("{0}, you're **blacklisted** from using Ardent commands on this server!")
                                }
                                return
                            }
                            else -> event.member.roles.forEach { memberRole ->
                                if (data.blacklistSettings.blacklistedRoles.contains(memberRole.id)) {
                                    event.channel.send("One of your roles, **{0}**, is blacklisted from using Ardent commands. You'll be able to use commands again once you no longer have this role".tr(event.guild, memberRole.name))
                                    return
                                }
                            }
                        }
                    }
                    try {
                        executor.execute {
                            cmd.executeInternal(args, event)
                            r.table("commands").insert(r.json(LoggedCommand(cmd.name, event.author.id, System.currentTimeMillis(), System.currentTimeMillis().readableDate()).toJson())).runNoReply(conn)
                        }
                    } catch (e: Throwable) {
                        e.log()
                        logChannel!!.send("^ Exception thrown in **${event.guild.name}** with command ${cmd.name}")
                        event.channel.send("There was an exception while trying to run this command. Please join {0} and share the following stacktrace:".tr(event.guild, "<https://ardentbot.com/support>") + "\n${ExceptionUtils.getStackTrace(e)}")

                    }
                }
                return
            }
        }
    }
}

data class Subcommand(val englishIdentifier: String, val syntax: String, val description: String? = null,
                      val consumer: (MutableList<String>, MessageReceivedEvent) -> Unit)

abstract class ExtensibleCommand(category: Category, name: String, description: String, vararg aliases: String, ratelimit: Int = 0) : Command(category, name, description, *aliases, ratelimit = ratelimit) {
    val subcommands = mutableListOf<Subcommand>()

    fun with(englishIdentifier: String, syntax: String?, description: String? = null, consumer: (MutableList<String>, MessageReceivedEvent) -> Unit): Command {
        subcommands.add(Subcommand(englishIdentifier, syntax ?: englishIdentifier, description, consumer))
        return this
    }

    abstract fun registerSubcommands()
}

abstract class Command(val category: Category, val name: String, val description: String, vararg val aliases: String, val ratelimit: Int = 0) {
    fun executeInternal(args: MutableList<String>, event: MessageReceivedEvent): Boolean {
        if (event.channelType == ChannelType.PRIVATE) {
            event.author.openPrivateChannel().queue { channel ->
                channel.send("Please use commands inside a Discord server!")
            }
            return false
        } else {
            if (ratelimit != 0) {
                val time = factory.ratelimits[event.author.id]
                if (time != null) {
                    if (time > System.currentTimeMillis()) {
                        event.channel.send("{0}, chill out! You can use this command again in **{1}** seconds".tr(event.guild, event.author.asMention, ((time - System.currentTimeMillis()) / 1000).toInt()))
                        return false
                    } else factory.ratelimits.remove(event.author.id)
                }
                if (!event.author.hasPatronPermission(event.textChannel, PatronLevel.SUPPORTER, true)) factory.ratelimits.put(event.author.id, System.currentTimeMillis() + (1000 * ratelimit))
            }
            if (this is ExtensibleCommand) {
                subcommands.forEach {
                    val identifier = it.englishIdentifier.tr(event.guild)
                    if (args.concat().startsWith(identifier)) {
                        var temp = args.concat().removePrefix(identifier)
                        while (temp.startsWith(" ")) temp = temp.removePrefix(" ")
                        it.consumer.invoke(temp.split(" ").toMutableList().without("").without(" "), event)
                        return true
                    }
                }
            }
            executeBase(args, event)
            return true
        }
    }

    abstract fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent)

    fun showHelp(event: MessageReceivedEvent) {
        val member = event.member
        val channel = event.textChannel
        val data = event.guild.getData()
        val prefixSettings = data.prefixSettings
        val embed = member.embed("How can I use {0}?".tr(event.guild, "${prefixSettings.prefix}$name", command = true), channel)
                .setThumbnail("https://upload.wikimedia.org/wikipedia/commons/f/f6/Lol_question_mark.png")
                .setFooter("This can also be used with: {0}".tr(channel.guild, channel.guild, aliases.toList().stringify()), member.user.avatarUrl)
                .appendDescription("*${description.tr(channel.guild)}*\n")
        if (this is ExtensibleCommand) {
            subcommands.forEach {
                embed.appendDescription("\n" + Emoji.SMALL_BLUE_DIAMOND + "**" + it.syntax.tr(event.guild, subcommand = true)
                        + "**: " + (it.description?.tr(channel) ?: "No selfDescription is available for this subcommand".tr(channel)))
            }
            if (subcommands.size > 0) embed.appendDescription("\n\n**Example**: {0}".tr(channel.guild, "${prefixSettings.prefix}$name ${subcommands[0].syntax.tr(channel)}"))
        }
        embed.appendDescription("\n\nType {PREFIX}help to view a full list of commands".tr(channel.guild))
        channel.send(embed)
    }

    fun containsAlias(arg: String, guild: Guild): Boolean {
        val a = arg.split(" ")[0]
        return a == name.tr(guild) || name.equals(a, true) || aliases.contains(a) || aliases.any { a == it }
    }

    override fun toString(): String {
        return this.toJson()
    }
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
        "Statistics" -> Category.STATISTICS
        else -> Category.BOT_INFO
    }
}

enum class Category(val fancyName: String, val webName: String, val description: String) {
    GAMES("Games", "Games", "Compete against your friends or users around the world in classic and addicting games!"),
    MUSIC("Music", "Music", "Play your favorite tracks or listen to the radio, all inside Discord"),
    BOT_INFO("BotInfo", "Ardent Info", "Curious about the status of Ardent? Want to know how to help us continue development? This is the category for you!"),
    SERVER_INFO("ServerInfo", "Server Info", "Check current information about different aspects of your server"),
    ADMINISTRATE("Administrate", "Administration", "Administrate your server: this category includes commands like warnings and mutes"),
    FUN("Fun", "Fun & Urban", "Bored? Not interested in the games? We have a lot of commands for you to check out here!"),
    RPG("RPG", "RPG", "Need a gambling fix? Want to marry someone? Use this category!"),
    LANGUAGE("Language", "Language", "Want to change your server's data or translate a phrase?"),
    STATISTICS("Statistics", "Ardent Statistics", "Interested in Ardent or how our system's been running?"),
    SETTINGS("Settings", "Settings", "Change Ardent settings like the prefix to customize it to your server!")
    ;

    override fun toString(): String {
        return fancyName
    }

    fun getCommands(): List<Command> {
        return factory.commands.filter { it.category == this }
    }
}