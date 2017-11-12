package commands.info

import events.Category
import events.Command
import events.ExtensibleCommand
import main.factory
import main.waiter
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import translation.tr
import utils.discord.*
import utils.functionality.*
import utils.functionality.Settings
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.stream.Collectors

val formatter = DecimalFormat("#,###")

class Ping : Command(Category.BOT_INFO, "ping", "what did you think this command was gonna do?") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val currentTime = System.currentTimeMillis()
        event.channel.sendMessage("I'll calculate my ping to Discord using this message".tr(event.guild)).queue({ m ->
            m.editMessage("**Ping**: *{0} ms*".tr(event, (System.currentTimeMillis() - currentTime).toString()))?.queue()
        })
    }
}

class Invite : Command(Category.BOT_INFO, "invite", "getWithIndex Ardent's invite URL", "ardent") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("My invite link is {0} - have fun using Ardent!".tr(event, "<https://ardentbot.com/invite>"))
    }
}

class Donate : Command(Category.BOT_INFO, "donate", "learn how to support Ardent and getWithIndex special perks for it!") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Want to support our work and obtain some perks along the way? Head to {0} to see the different ways you could help us out!".tr(event, "<https://ardentbot.com/patreon>"))
    }
}

class Settings : Command(Category.ADMINISTRATE, "settings", "administrate the settings for your server", "manage", "webpanel") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Visit our new web panel for an easy way to manage your settings - {0}".tr(event, "<https://ardentbot.com/manage/${event.guild.id}>"))
    }
}

class About : Command(Category.BOT_INFO, "about", "learn more about Ardent") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val builder = event.member.embed("About the bot and its founders".tr(event), event.channel)
        builder.appendDescription("Ardent was originally founded in November 2016 by Adam#9261. It reached over 4,000 servers " +
                "by June, but Adam had to shut it down due to chronic stability issues with the bot and the fact that he was going on " +
                "a data learning program without any internet for nearly two months. When he came back, he decided to recreate Ardent with a " +
                "new focus on modern design, utility, usability, and games. This is the continuation of the original Ardent bot. We hope you like it!")
        event.channel.send(builder)
    }
}

// IAM
// IAMNOT

class ServerInfo : Command(Category.SERVER_INFO, "serverinfo", "view some basic information about this server", "guildinfo", "si", "gi") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val guild = event.guild
        val data = guild.getData()
        val embed = event.member.embed("Server Info for {0}".tr(event, guild.name), event.channel)
        embed.addField("Number of users".tr(event), guild.members.size.toString(), true)
        embed.addField("Online users".tr(event), guild.members.stream().filter { m -> m.onlineStatus != OnlineStatus.OFFLINE }.count().format(), true)
        embed.addField("Prefix".tr(event), data.prefixSettings.prefix, true)
        embed.addField("Patron Server".tr(event), (if (guild.isPatronGuild()) "Yes" else "No").tr(event), true)
        embed.addField("Owner".tr(event), guild.owner.user.toFancyString(), true)
        embed.addField("Creation Date".tr(event), guild.creationTime.toLocalDate().toString(), true)
        embed.addField("# of Voice Channels".tr(event), guild.voiceChannels.size.toString(), true)
        embed.addField("# of Text Channels".tr(event), guild.textChannels.size.toString(), true)
        embed.addField("# of Roles".tr(event), guild.roles.size.toString(), true)
        embed.addField("Region".tr(event), guild.region.getName(), true)
        embed.addField("Verification Level".tr(event), guild.verificationLevel.toString(), true)
        event.channel.send(embed)
    }
}

class UserInfo : Command(Category.SERVER_INFO, "userinfo", "view cool information about your friends", "whois", "userinfo", "ui") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0) event.channel.send("You need to mention a member!".tr(event))
        else {
            val mentioned = mentionedUsers[0]
            val mentionedMember = event.guild.getMember(mentioned)
            event.channel.send(event.member.embed("Information about {0}".tr(event, mentionedMember.effectiveName), event.channel)
                    .setThumbnail(mentioned.avatarUrl)
                    .addField("Name".tr(event), mentioned.toFancyString(), true)
                    .addField("Nickname".tr(event), mentionedMember.nickname ?: "None".tr(event), true)
                    .addField("Server Join Date".tr(event), mentionedMember.joinDate.toLocalDate().toString(), true)
                    .addField("Days in Guild".tr(event), Math.ceil((Instant.now().atOffset(ZoneOffset.UTC).toEpochSecond() -
                            event.member.joinDate.toInstant().atOffset(ZoneOffset.UTC).toEpochSecond() / (60 * 60 * 24)).toDouble())
                            .toString(), true)
                    .addField("Roles".tr(event), mentionedMember.roles.map { it.name }.concat(), true)
                    .addField("Account Creation Date".tr(event), mentioned.creationTime.toLocalDate().toString(), true))
        }
    }
}

class Support : Command(Category.BOT_INFO, "support", "need help? something not working?") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Need help? Something not working? Join our support server @ {0}".tr(event, "https://discord.gg/VebBB5z"))
    }
}

class GetId : Command(Category.SERVER_INFO, "getid", "getWithIndex the id of people in your server by mentioning them") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0) event.channel.send("You need to mention some people (or bots)!".tr(event))
        else {
            mentionedUsers.forEach { event.channel.send("**{0}**'s ID: {1}".tr(event, it.toFancyString(), it.id)) }
        }
    }
}

class RoleInfo : Command(Category.SERVER_INFO, "roleinfo", "view useful information about roles in this server", "ri") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val role = event.message.getFirstRole(arguments)
        if (role == null) event.channel.send("You need to either mention a role or type its full name!".tr(event))
        else {
            event.channel.send(event.member.embed("Info about {0}".tr(event, role.name), event.channel)
                    .setThumbnail(event.guild.iconUrl)
                    .addField("# with role".tr(event), "{0} members".tr(event, event.guild.members.filter { it.roles.contains(role) }.count().toString()), true)
                    .addField("Role ID".tr(event), role.id, true)
                    .addField("Creation Date".tr(event), (role.creationTime.toEpochSecond() / 1000).readableDate(), true)
                    .addField("Permissions".tr(event), role.permissions.map { it.getName() }.stringify(), true)
            )
        }
    }
}

class WebsiteCommand : Command(Category.BOT_INFO, "website", "getWithIndex the link for Ardent's cool website") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Check out our site @ {0}".tr(event, "<https://ardentbot.com>"))
    }
}

class Status : Command(Category.BOT_INFO, "status", "check realtime statistics about the bot") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val onlyBot = getAllGuilds().filter { it.members.filter { it.user.isBot }.count() == 1 }.count()
        event.channel.send(event.member.embed("Ardent Realtime Status", event.channel)
                .addField("Loaded Commands".tr(event), internals.commandCount.toString(), true)
                .addField("Messages Received".tr(event), formatter.format(internals.messagesReceived), true)
                .addField("Commands Received".tr(event), formatter.format(internals.commandsReceived), true)
                .addField("Servers".tr(event), formatter.format(internals.guilds), true)
                .addField("Users".tr(event), formatter.format(internals.users), true)
                .addField("Loaded Music Players".tr(event), formatter.format(internals.loadedMusicPlayers), true)
                .addField("Queue Length".tr(event), "{0} tracks".tr(event, formatter.format(internals.queueLength)), true)
                .addField("Total Music Played".tr(event), "{0} days".tr(event, (internals.musicPlayed.toFloat() / 24).format()), true)
                .addField("CPU Usage".tr(event), "${internals.cpuUsage}%", true)
                .addField("RAM Usage".tr(event), "${internals.ramUsage.first} / ${internals.ramUsage.second} mb", true)
                .addField("Uptime".tr(event), internals.uptimeFancy, true)
                .addField("Servers w/Ardent as only bot".tr(event), formatter.format(onlyBot) +
                        " (${(onlyBot * 100 / getAllGuilds().size.toFloat()).format()}%)", true)
                .addField("Website".tr(event), "https://ardentbot.com", true))
    }
}