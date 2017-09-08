package commands.info

import events.Category
import events.Command
import main.factory
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.exceptions.PermissionException
import utils.*
import java.awt.Color
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset

val formatter = DecimalFormat("#,###")

class Ping : Command(Category.BOT_INFO, "ping", "what did you think this command was gonna do?") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val currentTime = System.currentTimeMillis()
        event.channel.sendMessage("I'll calculate my ping to Discord using this message".translateTo(event.guild)).queue({ m ->
            m.editMessage("**Socket Ping**: *${System.currentTimeMillis() - currentTime} milliseconds*")?.queue()
        })
    }
}

class Invite : Command(Category.BOT_INFO, "invite", "get Ardent's invite URL") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("My invite link is <https://ardentbot.com/invite>" +
                " - have fun using Ardent!")
    }
}

class Donate : Command(Category.BOT_INFO, "donate", "learn how to support Ardent and get special perks for it!") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Want to support our work and obtain some perks along the way? Head to <https://ardentbot.com/patreon> to see the different ways " +
                "you could help us out!")
    }
}


class WebPanel : Command(Category.ADMINISTRATE, "webpanel", "administrate the settings for your server", "panel") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Visit our new web panel for an easy way to manage your settings - <https://ardentbot.com/manage/${event.guild.id}>")
    }
}

class Settings : Command(Category.ADMINISTRATE, "settings", "administrate the settings for your server") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Visit our new web panel for an easy way to manage your settings - <https://ardentbot.com/manage/${event.guild.id}>")
    }
}

class About : Command(Category.BOT_INFO, "about", "learn more about Ardent") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val builder = event.member.embed("About the bot and its founders")
        builder.appendDescription("Ardent was originally founded in November 2016 by Adam#9261. It reached over 4,000 servers " +
                "by June, but Adam had to shut it down due to chronic stability issues with the bot and the fact that he was going on " +
                "a language learning program without any internet for nearly two months. When he came back, he decided to recreate Ardent with a " +
                "new focus on modern design, utility, usability, and games. This is the continuation of the original Ardent bot. We hope you like it!")
        event.channel.send(builder)
    }
}

class IamCommand : Command(Category.ADMINISTRATE, "iam", "gives you the role you wish to receive", "iamrole") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = event.guild.getData()
        if (arguments.size == 0) {
            val embed = event.member.embed("Iam List")
            val builder = StringBuilder().append("This is the **autoroles** list for *${event.guild.name}* - to add or delete them, type **/webpanel**\n")
            if (data.iamList.size == 0) builder.append("You don't have any autoroles :(")
            else {
                data.iamList.forEach {
                    val role = it.roleId.toRole(event.guild)
                    if (role != null) builder.append("${Emoji.SMALL_ORANGE_DIAMOND} Typing **${it.name}** will give you the **${role.name}** role\n")
                    else {
                        data.iamList.remove(it)
                    }
                }
                data.update()
                builder.append("\n**Give yourself one of these roles by typing _/iam NAME_**")
            }
            event.channel.send(embed.setDescription(builder.toString()))
            return
        }
        val name = arguments.concat()
        var found = false
        data.iamList.forEach { iterator, current ->
            if (current.name.equals(name, true)) {
                val role = current.roleId.toRole(event.guild)
                if (role == null) {
                    iterator.remove()
                    data.update()
                } else {
                    try {
                        event.guild.controller.addRolesToMember(event.member, role).reason("Ardent Autoroles").queue({
                            event.channel.send("Successfully gave you the **${role.name}** role!")
                        }, {
                            event.channel.send("Failed to give the *${role.name}* role - **please ask an administrator of this server to allow me " +
                                    "to give you roles!**")
                        })
                    } catch (e: PermissionException) {
                        event.channel.send("Failed to give the *${role.name}* role - **please ask an administrator of this server to allow me " +
                                "to give you roles!**")
                    }
                }
                found = true
                return@forEach
            }
        }
        if (!found) event.channel.send("An autorole with that name wasn't found. Please type **${data.prefix}iam** to get a full list")

    }
}

class IamnotCommand : Command(Category.ADMINISTRATE, "iamnot", "removes the role from you that you've been given via /iam", "iamrole") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = event.guild.getData()
        if (arguments.size == 0) {
            event.channel.send("Please type **${data.prefix}iam** to get a full list of available autoroles")
            return
        }
        val name = arguments.concat()
        var found = false
        data.iamList.forEach {
            if (it.name.equals(name, true)) {
                val role = it.roleId.toRole(event.guild)
                if (role == null) {
                    data.iamList.remove(it)
                    data.update()
                } else {
                    if (event.member.roles.contains(role)) {
                        try {
                            event.guild.controller.removeRolesFromMember(event.member, role).reason("Ardent Autoroles - Removal").queue({
                                event.channel.send("Successfully removed the **${role.name}** role!")
                            }, {
                                event.channel.send("Failed to remove *${role.name}* - **please ask an administrator of this server to allow me " +
                                        "to manage roles!**")
                            })
                        } catch (e: Exception) {
                            event.channel.send("Failed to remove *${role.name}* - **please ask an administrator of this server to allow me " +
                                    "to manage roles!**")
                        }
                    } else event.channel.send("You can't remove a role you don't have! :thinking:")
                }
                found = true
                return@forEach
            }
        }
        if (!found) event.channel.send("An autorole with that name wasn't found. Please type **${data.prefix}iam** to get a full list")

    }
}

class Help : Command(Category.BOT_INFO, "help", "can you figure out what this does? it's a grand mystery!", "h") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.selectFromList(event.member, "Ardent | Commands", Category.values().map { "${it.fancyName}: *${it.description}*" }.toMutableList(), { selected, selectionMessage ->
            val category = Category.values()[selected]
            val embed = event.member.embed("${category.fancyName} | Command List", Color.DARK_GRAY)
            factory.commands.filter { it.category == category }.toMutableList().shuffle().forEachIndexed { index, command ->
                embed.appendDescription("\n${if (index % 2 == 0) Emoji.SMALL_BLUE_DIAMOND else Emoji.SMALL_ORANGE_DIAMOND} **${command.name}**: ${command.description}")
                if (command.aliases.isNotEmpty()) {
                    if (command.aliases.size > 1) embed.appendDescription("         __aliases: [${command.aliases.toList().stringify()}]__")
                    else embed.appendDescription("   (__alias: ${command.aliases.toList().stringify()}__)")
                }
            }
            embed.appendDescription("\n\n*Did you know you can also type \"_ardent help_\" along with \"_/help_\" ? You can also change the set prefix for your server!*")
            selectionMessage.editMessage(embed.build()).queue()
        }, failure = {
            event.channel.send("You need to type the number or click the reaction that corresponded to the category you wanted to select :(")
        })
    }
}

class ServerInfo : Command(Category.SERVER_INFO, "serverinfo", "view some basic information about this server", "guildinfo", "si", "gi") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val guild = event.guild
        val data = guild.getData()
        val embed = event.member.embed("Server Info: ${guild.name}")
        embed.addField("Number of users", guild.members.size.toString(), true)
        embed.addField("Online users", guild.members.stream().filter { m -> m.onlineStatus != OnlineStatus.OFFLINE }.count().toString(), true)
        embed.addField("Prefix", data.prefix, true)
        embed.addField("Patron Server", guild.isPatronGuild().toString(), true)
        embed.addField("Owner", guild.owner.withDiscrim(), true)
        embed.addField("Creation Date", guild.creationTime.toLocalDate().toString(), true)
        embed.addField("# of Voice Channels", guild.voiceChannels.size.toString(), true)
        embed.addField("# of Text Channels", guild.textChannels.size.toString(), true)
        embed.addField("# of Roles", guild.roles.size.toString(), true)
        embed.addField("Region", guild.region.getName(), true)
        embed.addField("Verification Level", guild.verificationLevel.toString(), true)
        event.channel.send(embed)
    }
}

class UserInfo : Command(Category.SERVER_INFO, "userinfo", "view cool information about your friends", "whois", "userinfo", "ui") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0) event.channel.send("You need to mention a member!")
        else {
            val mentioned = mentionedUsers[0]
            val mentionedMember = event.guild.getMember(mentioned)
            event.channel.send(event.member.embed("Information about ${mentionedMember.effectiveName}")
                    .setThumbnail(mentioned.avatarUrl)
                    .addField("Name", mentioned.withDiscrim(), true)
                    .addField("Nickname", mentionedMember.nickname ?: "None", true)
                    .addField("Server Join Date", mentionedMember.joinDate.toLocalDate().toString(), true)
                    .addField("Days in Guild", Math.ceil((Instant.now().atOffset(ZoneOffset.UTC).toEpochSecond() -
                            event.member.joinDate.toInstant().atOffset(ZoneOffset.UTC).toEpochSecond() / (60 * 60 * 24)).toDouble())
                            .toString(), true)
                    .addField("Roles", mentionedMember.roles.map { it.name }.concat(), true)
                    .addField("Account Creation", mentioned.creationTime.toLocalDate().toString(), true))
        }
    }
}

class Support : Command(Category.BOT_INFO, "support", "need help? something not working?") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Need help? Something not working? Join our support server @ https://discord.gg/VebBB5z")
    }
}

class GetId : Command(Category.SERVER_INFO, "getid", "get the id of people in your server by mentioning them") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0) event.channel.send("You need to mention some people (or bots)!")
        else {
            mentionedUsers.forEach { event.channel.send("**${it.withDiscrim()}**'s ID: ${it.id}") }
        }
    }
}

class RoleInfo : Command(Category.SERVER_INFO, "roleinfo", "view useful information about roles in this server", "ri") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val role = event.message.getFirstRole(arguments)
        if (role == null) event.channel.send("You need to either mention a role or type its full name!")
        else {
            event.channel.send(event.member.embed("Info about ${role.name}")
                    .setThumbnail(event.guild.iconUrl)
                    .addField("# with role", event.guild.members.filter { it.roles.contains(role) }.count().toString() + " members", true)
                    .addField("Role ID", role.id, true)
                    .addField("Creation Date", (role.creationTime.toEpochSecond() / 1000).readableDate(), true)
                    .addField("Permissions", role.permissions.map { it.getName() }.concat(), true)
            )
        }
    }
}

class WebsiteCommand : Command(Category.BOT_INFO, "website", "get the link for Ardent's cool website") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Check out the pages @ <https://ardentbot.com>")
    }
}

class Status : Command(Category.BOT_INFO, "status", "check realtime statistics about the bot") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send(event.member.embed("Ardent Realtime Status")
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