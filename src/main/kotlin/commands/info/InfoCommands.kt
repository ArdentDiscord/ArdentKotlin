package commands.info

import events.Category
import events.Command
import events.toCategory
import main.factory
import main.jdas
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
import javax.swing.plaf.synth.SynthLookAndFeel.getRegion
import java.nio.file.Files.getOwner
import net.dv8tion.jda.core.OnlineStatus
import sun.misc.MessageUtils
import java.time.Instant
import java.time.ZoneOffset
import java.text.DecimalFormat


val formatter = DecimalFormat("#,###")

class Ping : Command(Category.BOT_INFO, "ping", "what did you think this command was gonna do?") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val currentTime = System.currentTimeMillis()
        event.channel.sendReceive(member, "I'll calculate my ping to Discord using this message")
                ?.editMessage("**Socket Ping**: *${System.currentTimeMillis() - currentTime} milliseconds*")?.queue()
        waiter.waitForMessage(Settings(event.author.id), { message: Message -> println(message.content) })
    }
}

class Invite : Command(Category.BOT_INFO, "invite", "get Ardent's invite URL") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val channelInvite = jdas[0].asBot().getInviteUrl(Permission.MESSAGE_MANAGE, Permission.MANAGE_SERVER, Permission.VOICE_CONNECT, Permission.MANAGE_CHANNEL, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY, Permission.MANAGE_ROLES)
        channel.send(member, "My invite link is $channelInvite - have fun using Ardent!")
    }
}

class Donate : Command(Category.BOT_INFO, "donate", "learn how to support Ardent and get special perks for it!") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Want to support our work and obtain some perks along the way? Head to https://ardentbot.com/support_us to see the different ways " +
                "you could help us out!")
    }
}

class Settings : Command(Category.SERVER_INFO, "settings", "administrate the settings for your server", "s") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            withHelp("current", "see a list of settings along with their current values")
                    .withHelp("trusteveryone true|false", "choose whether to allow everyone to access advanced DJ commands like /skip, /leave, etc.")
                    .withHelp("defaultrole [role name **OR** type `none` to disable]", "set the default role given to users when they join this server")
                    .withHelp("messageparam", "see a list of special parameters you can add to your join or leave message")
                    .withHelp("joinmessage [message **or** `none` to remove]", "set or remove the join message")
                    .withHelp("leavemessage [message **or** `none` to remove]", "set or remove the leave message")
                    .withHelp("messagechannel [channel name **or** `none` to remove]", "set or remove the channel to send join/leave messages to")
                    .withHelp("announcemusic true|false", "set whether you want me to send an info message whenever a new song starts playing")
                    .displayHelp(channel, member)
        } else {
            if (!member.hasOverride(channel)) return
            val data = guild.getData()
            when (arguments[0]) {
                "current" -> {
                    channel.send(member, embed("Settings for ${guild.name}", member)
                            .addField("Can everyone use DJ commands?", data.allowGlobalOverride.toString(), false)
                            .addField("Default Role", data.defaultRole?.toRole(guild)?.name ?: "None", false)
                            .addField("Join Message", data.joinMessage?.first ?: "None", false)
                            .addField("Leave Message", data.leaveMessage?.first ?: "None", false)
                            .addField("Channel for Join/Leave Message", data.joinMessage?.second?.toChannel()?.asMention ?: "None", false)
                            .addField("Add more than 1 song at a time for normal users", (!data.musicSettings.singleSongInQueueForMembers).toString(), false)
                            .addField("Announce Start of New Songs", data.musicSettings.announceNewMusic.toString(), false)
                    )
                }
                "messageparam" -> {
                    channel.send(member, "You can use the following parameters:\n" +
                            "- \$usermention: replaced with a mention of the user leaving/joining\n" +
                            "- \$username: replaced with username#discriminator of the user\n" +
                            "- \$servername: replaced with the server name\n" +
                            "- \$membercount: replaced with the current member count of this server")
                }
                "joinmessage" -> {
                    if (arguments.size == 1) channel.send(member, "You need to include a message or `none` to remove the current message")
                    else {
                        arguments.removeAt(0)
                        val message = arguments.concat()
                        if (message.equals("none", true)) {
                            data.joinMessage = Pair(null, data.joinMessage?.second)
                            channel.send(member, "Removed the set **join message**")
                        }
                        else {
                            data.joinMessage = Pair(message, data.joinMessage?.second)
                            channel.send(member, "Set the join message. Make sure you've set a **receiver channel** where I should send the messages to.")
                        }
                    }
                    data.update()
                }
                "leavemessage" -> {
                    if (arguments.size == 1) channel.send(member, "You need to include a message or `none` to remove the current message")
                    else {
                        arguments.removeAt(0)
                        val message = arguments.concat()
                        if (message.equals("none", true)) {
                            data.leaveMessage = Pair(null, data.leaveMessage?.second)
                            channel.send(member, "Removed the set **leave message**")
                        }
                        else {
                            data.leaveMessage = Pair(message, data.leaveMessage?.second)
                            channel.send(member, "Set the leave message. Make sure you've set a **receiver channel** where I should send the messages to.")
                        }
                    }
                    data.update()
                }
                "messagechannel" -> {
                    if (arguments.size == 1) channel.send(member, "You need to include a channel name or `none` to remove the current channel")
                    else {
                        arguments.removeAt(0)
                        val message = arguments.concat()
                        if (message.equals("none", true)) {
                            data.joinMessage = Pair(data.joinMessage?.first, null)
                            data.leaveMessage = Pair(data.leaveMessage?.first, null)
                            channel.send(member, "Removed the set **receiver channel**")
                        }
                        else {
                            val results = guild.getTextChannelsByName(message, true)
                            if (results.size == 0) channel.send(member, "No channel matched that name! Please check your spelling and try again")
                            else {
                                val setChannel = results[0]
                                data.joinMessage = Pair(data.joinMessage?.first, setChannel.id)
                                data.leaveMessage = Pair(data.leaveMessage?.first, setChannel.id)
                                channel.send(member, "Set the **receiver channel** as ${setChannel.asMention}")
                            }
                        }
                    }
                    data.update()
                }
                "announcemusic" -> {
                    if (arguments.size == 1) channel.send(member, "You need to specify true or false!")
                    else {
                        val change = arguments[1].toBoolean()
                        data.musicSettings.announceNewMusic = change
                        data.update()
                        if (change) channel.send(member, "I will now **announce** the start of songs")
                        else channel.send(member, "I **won't** announce the start of songs")
                    }
                }
                "trusteveryone" -> {
                    if (arguments.size == 1) channel.send(member, "You need to specify true or false!")
                    else {
                        val change = arguments[1].toBoolean()
                        data.allowGlobalOverride = change
                        data.update()
                        if (change) channel.send(member, "Everyone can now use DJ commands")
                        else channel.send(member, "Only elevated users can now use DJ commands")
                    }
                }
                "defaultrole" -> {
                    if (arguments.size == 1) channel.send(member, "You need to specify `none` or type the name of a role")
                    else {
                        if (arguments[1].equals("none", true)) {
                            data.defaultRole = null
                            data.update()
                            channel.send(member, "No default role will be given to new members")
                        } else {
                            val roles = guild.getRolesByName(arguments[1], true)
                            if (roles.size == 0) channel.send(member, "No role with that name was found")
                            else {
                                val role = roles[0]
                                data.defaultRole = role.id
                                data.update()
                                channel.send(member, "**${role.name}** will be given to new members")
                            }
                        }
                    }
                }
            }
        }
    }
}

class About : Command(Category.BOT_INFO, "about", "learn more about Ardent") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val builder = embed("About the bot and its founders", channel.guild.selfMember)
        builder.appendDescription("Ardent was originally founded in November 2016 by Adam#9261. It reached over 4,000 servers " +
                "by June, but Adam had to shut it down due to chronic stability issues with the bot and the fact that he was going on " +
                "a language learning program without any internet for nearly two months. When he came back, he decided to recreate Ardent with a " +
                "new focus on modern design, utility, usability, and games. This is the continuation of the original Ardent bot. We hope you like it!")
        channel.send(member, builder)
    }
}


class Help : Command(Category.BOT_INFO, "help", "can you figure out what this does? it's a grand mystery!", "h") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val categories = Category.values().map { it.toString() }.toMutableList()
        channel.selectFromList(member, "Which category of commands would you like help in?", categories, {
            number ->
            val category = categories[number].toCategory()
            val categoryCommands = factory.commands.filter { it.category == category }.toMutableList().shuffle()
            val embed = embed("${category.fancyName} Commands", member)
                    .appendDescription("*${category.description}*")
            categoryCommands.forEach { command ->
                embed.appendDescription("\n${Emoji.SMALL_ORANGE_DIAMOND} **${command.name}**: ${command.description}")
                if (command.aliases.isNotEmpty()) {
                    embed.appendDescription("\n")
                    if (command.aliases.size > 1) embed.appendDescription("         __aliases: [${command.aliases.toList().stringify()}]__")
                    else embed.appendDescription("         __alias: ${command.aliases.toList().stringify()}__")
                }
            }
            channel.send(member, embed)
        }, "Command count: **${factory.commands.size}**\n" +
                "*Did you know you can also type \"_ardent help_\" along with \"_/help_\" ? You can also change the set prefix for your server!*")
    }
}

class ServerInfo : Command(Category.SERVER_INFO, "serverinfo", "view some basic information about this server", "guildinfo", "si", "gi") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = guild.getData()
        val embed = embed("Server Info: ${guild.name}", member)
        embed.addField("Number of users", guild.members.size.toString(), true)
        embed.addField("Online users", guild.members.stream().filter { m -> m.onlineStatus != OnlineStatus.OFFLINE }.count().toString(), true)
        embed.addField("Prefix", data.prefix, true)
        embed.addField("Patron Server", guild.isPatronGuild().toString(), true)
        embed.addField("Owner", guild.owner.withDiscrim(), true)
        embed.addField("Creation Date", guild.creationTime.toLocalDate().toString(), true)
        embed.addField("Public channel", guild.publicChannel.asMention, true)
        embed.addField("# of Voice Channels", guild.voiceChannels.size.toString(), true)
        embed.addField("# of Text Channels", guild.textChannels.size.toString(), true)
        embed.addField("# of Roles", guild.roles.size.toString(), true)
        embed.addField("Region", guild.region.getName(), true)
        embed.addField("Verification Level", guild.verificationLevel.toString(), true)
        channel.send(member, embed)
    }
}

class UserInfo : Command(Category.SERVER_INFO, "userinfo", "view cool information about your friends", "whois", "userinfo", "ui") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0) channel.send(member, "You need to mention a member!")
        else {
            val mentioned = mentionedUsers[0]
            val mentionedMember = guild.getMember(mentioned)
            channel.send(member, embed("Information about ${mentionedMember.effectiveName}", member)
                    .setThumbnail(mentioned.avatarUrl)
                    .addField("Name", mentioned.withDiscrim(), true)
                    .addField("Nickname", mentionedMember.nickname ?: "None", true)
                    .addField("Server Join Date", mentionedMember.joinDate.toLocalDate().toString(), true)
                    .addField("Days in Guild", Math.ceil((Instant.now().atOffset(ZoneOffset.UTC).toEpochSecond() -
                            member.joinDate.toInstant().atOffset(ZoneOffset.UTC).toEpochSecond() / (60 * 60 * 24)).toDouble())
                            .toString(), true)
                    .addField("Roles", mentionedMember.roles.map { it.name }.concat(), true)
                    .addField("Account Creation", mentioned.creationTime.toLocalDate().toString(), true))
        }
    }
}

class RoleInfo : Command(Category.SERVER_INFO, "roleinfo", "view useful information about roles in this server", "ri") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val role = event.message.getFirstRole(arguments)
        if (role == null) channel.send(member, "You need to either mention a role or type its full name!")
        else {
            channel.send(member, embed("Info about ${role.name}", member)
                    .setThumbnail(guild.iconUrl)
                    .addField("# of people with role", guild.members.filter { it.roles.contains(role) }.count().toString(), true)
                    .addField("Creation Date", role.creationTime.toLocalDateTime().toString(), true)
                    .addField("Hex Color", "#${Integer.toHexString(role.color.rgb).substring(2).toUpperCase()}", true)
                    .addField("Permissions", role.permissions.map { it.getName() }.concat(), true)
            )
        }
    }
}

class Status : Command(Category.BOT_INFO, "status", "check realtime statistics about the bot") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val internals = Internals()
        channel.send(member, embed("Ardent Realtime Status", member)
                .addField("Loaded Commands", internals.commandCount.toString(), true)
                .addField("Messages Received", formatter.format(internals.messagesReceived), true)
                .addField("Commands Received", formatter.format(internals.commandsReceived), true)
                .addField("Servers", formatter.format(internals.guilds), true)
                .addField("Users", formatter.format(internals.users), true)
                .addField("Loaded Music Players", formatter.format(internals.loadedMusicPlayers), true)
                .addField("Queue Length", "${formatter.format(internals.queueLength)} tracks", true)
                .addField("CPU Usage", "${internals.cpuUsage}%", true)
                .addField("RAM Usage", "${internals.ramUsage.first} / ${internals.ramUsage.second} mb", true)
                .addField("Uptime", internals.uptimeFancy, true)
                .addField("Website", "https://ardentbot.com", true))
    }
}