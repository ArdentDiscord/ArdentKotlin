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
import javax.swing.plaf.synth.SynthLookAndFeel.getRegion
import java.nio.file.Files.getOwner
import net.dv8tion.jda.core.OnlineStatus
import org.apache.commons.lang3.CharSetUtils.count
import sun.misc.MessageUtils
import java.time.Instant
import java.time.ZoneOffset
import java.text.DecimalFormat




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
        val channelInvite = jda!!.asBot().getInviteUrl(Permission.MESSAGE_MANAGE, Permission.MANAGE_SERVER, Permission.VOICE_CONNECT, Permission.VOICE_SPEAK, Permission.MESSAGE_EMBED_LINKS, Permission.MESSAGE_HISTORY)
        channel.send(member, "My invite link is $channelInvite - have fun using Ardent!")
    }
}

class Donate : Command(Category.BOT_INFO, "donate", "learn how to support Ardent and get special perks for it!") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Want to support our work and obtain some perks along the way? Head to https://ardentbot.com/support_us to see the different ways " +
                "you could help us out!")
    }
}

class Settings : Command(Category.SERVER_INFO, "settings", "manage the settings for your server using our shiny new web panel", "website") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Manage the settings for this server at https://www.ardentbot.com/manage/${guild.id} - while you're there, be sure to check out " +
                "the rest of our website!")
    }
}

class About : Command(Category.BOT_INFO, "about", "learn more about Ardent") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val builder = embed("About the bot and its founders", channel.guild.selfMember)
        builder.appendDescription("Ardent was originally founded in November 2016 by ${jda!!.asBot().applicationInfo.complete().owner.withDiscrim()}. It reached over 4,000 servers " +
                "by June, but Adam had to shut it down due to chronic stability issues with the bot and the fact that he was going on " +
                "a language learning program without any internet for nearly two months. When he came back, he decided to recreate Ardent with a " +
                "new focus on modern design, utility, usability, and games. This is the continuation of the original Ardent bot. We hope you like it!")
        channel.send(member, builder)
    }
}


class Help : Command(Category.BOT_INFO, "help", "can you figure out what this does? it's a grand mystery!", "h") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val categories = Category.values().map { it.toString() }.toMutableList().shuffle()
        channel.selectFromList(member, "Which category of commands would you like help in?", categories, {
            number ->
            val category = categories[number].toCategory()
            val categoryCommands = factory.commands.filter { it.category == category }.toMutableList()
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
    var formatter = DecimalFormat("#,###")
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val internals = Internals()
        channel.send(member, embed("Ardent Realtime Status", member)
                .addField("Loaded Commands", internals.commandCount.toString(), true)
                .addField("Messages Received", formatter.format(internals.messagesReceived), true)
                .addField("Commands Received", formatter.format(internals.commandsReceived), true)
                .addField("Servers", formatter.format(internals.guilds.size), true)
                .addField("Users", formatter.format(internals.users), true)
                .addField("Loaded Music Players", formatter.format(internals.loadedMusicPlayers), true)
                .addField("Queue Length", "${formatter.format(internals.queueLength)} tracks", true)
                .addField("CPU Usage", "${internals.cpuUsage}%", true)
                .addField("RAM Usage", "${internals.ramUsage.first} / ${internals.ramUsage.second} mb", true)
                .addField("Uptime", internals.uptimeFancy, true)
                .addField("Website", "https://ardentbot.com", true))
    }
}