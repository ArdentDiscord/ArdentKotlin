package commands.info

import events.Category
import events.Command
import main.factory
import main.waiter
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.OnlineStatus
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*
import utils.Settings
import java.awt.Color
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset


val formatter = DecimalFormat("#,###")

class Ping : Command(Category.BOT_INFO, "ping", "what did you think this command was gonna do?") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val currentTime = System.currentTimeMillis()
        event.channel.sendReceive(member, "I'll calculate my ping to Discord using this message")
                ?.editMessage("**Socket Ping**: *${System.currentTimeMillis() - currentTime} milliseconds*")?.queue()
        waiter.waitForMessage(Settings(event.author.id), { message: Message -> println(message.content) })
    }
}

class Invite : Command(Category.BOT_INFO, "invite", "get Ardent's invite URL") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "My invite link is https://discordapp.com/oauth2/authorize?scope=bot&client_id=339101087569281045&permissions=269574192&redirect_uri=https://ardentbot.com/welcome&response_type=code" +
                " - have fun using Ardent!")
    }
}

class Donate : Command(Category.BOT_INFO, "donate", "learn how to support Ardent and get special perks for it!") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Want to support our work and obtain some perks along the way? Head to https://ardentbot.com/support_us to see the different ways " +
                "you could help us out!")
    }
}


class WebPanel : Command(Category.ADMINISTRATE, "webpanel", "administrate the settings for your server", "panel") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Visit our new web panel for an easy way to manage your settings - https://ardentbot.com/manage/${guild.id}")
    }
}

class Settings : Command(Category.ADMINISTRATE, "settings", "administrate the settings for your server") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Visit our new web panel for an easy way to manage your settings - https://ardentbot.com/manage/${guild.id}")
    }
}

class About : Command(Category.BOT_INFO, "about", "learn more about Ardent") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val builder = embed("About the bot and its founders", channel.guild.selfMember)
        builder.appendDescription("Ardent was originally founded in November 2016 by Adam#9261. It reached over 4,000 servers " +
                "by June, but Adam had to shut it down due to chronic stability issues with the bot and the fact that he was going on " +
                "a language learning program without any internet for nearly two months. When he came back, he decided to recreate Ardent with a " +
                "new focus on modern design, utility, usability, and games. This is the continuation of the original Ardent bot. We hope you like it!")
        channel.send(member, builder)
    }
}

class IamCommand : Command(Category.ADMINISTRATE, "iam", "gives you the role you wish to receive", "iamrole") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = guild.getData()
        if (arguments.size == 0) {
            val embed = embed("Iam List", member)
            val builder = StringBuilder().append("This is the **autoroles** list for *${guild.name}* - to add or delete them, type **/webpanel**\n")
            if (data.iamList.size == 0) builder.append("You don't have any autoroles :(")
            else {
                data.iamList.forEach {
                    val role = it.roleId.toRole(guild)
                    if (role != null) builder.append("${Emoji.SMALL_ORANGE_DIAMOND} Typing **${it.name}** will give you the **${role.name}** role\n")
                    else {
                        data.iamList.remove(it)
                    }
                }
                data.update()
                builder.append("\n**Give yourself one of these roles by typing _/iam NAME_**")
            }
            channel.send(member, embed.setDescription(builder.toString()))
            return
        }
        val name = arguments.concat()
        var found = false
        data.iamList.forEach {
            if (it.name.equals(name, true)) {
                val role = it.roleId.toRole(guild)
                if (role == null) {
                    data.iamList.remove(it)
                    data.update()
                } else {
                    guild.controller.addRolesToMember(member, role).reason("Ardent Autoroles").queue({
                        channel.send(member, "Successfully gave you the **${role.name}** role!")
                    }, {
                        channel.send(member, "Failed to give you the *${role.name}* role - **please ask an administrator of this server to allow me " +
                                "to give you roles!**")
                    })
                }
                found = true
                return@forEach
            }
        }
        if (!found) channel.send(member, "An autorole with that name wasn't found. Please type **${data.prefix}iam** to get a full list")
    }
}

class IamnotCommand : Command(Category.ADMINISTRATE, "iamnot", "removes the role from you that you've been given via /iam", "iamrole") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = guild.getData()
        if (arguments.size == 0) {
            channel.send(member, "Please type **${data.prefix}iam** to get a full list of available autoroles")
            return
        }
        val name = arguments.concat()
        var found = false
        data.iamList.forEach {
            if (it.name.equals(name, true)) {
                val role = it.roleId.toRole(guild)
                if (role == null) {
                    data.iamList.remove(it)
                    data.update()
                } else {
                    if (member.roles.contains(role)) {
                        guild.controller.removeRolesFromMember(member, role).reason("Ardent Autoroles - Removal").queue({
                            channel.send(member, "Successfully removed the **${role.name}** role!")
                        }, {
                            channel.send(member, "Failed to remove *${role.name}* - **please ask an administrator of this server to allow me " +
                                    "to manage roles!**")
                        })
                    } else channel.send(member, "You can't remove a role you don't have! :thinking:")
                }
                found = true
                return@forEach
            }
        }
        if (!found) channel.send(member, "An autorole with that name wasn't found. Please type **${data.prefix}iam** to get a full list")

    }
}

class Help : Command(Category.BOT_INFO, "help", "can you figure out what this does? it's a grand mystery!", "h") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val embed: EmbedBuilder
        if (arguments.size == 0) {
            embed = embed("Help | General", member, Color.DARK_GRAY)
            Category.values().forEachIndexed { index, category ->
                embed.appendDescription("${Emoji.SMALL_ORANGE_DIAMOND} ${category.fancyName} [**${index + 1}**]: *${category.description}*\n")
            }
            embed.appendDescription("\n**Type _${guild.getPrefix()}help NUMBER_ to see a category-specific command list**")
        } else {
            var categoryIndex: Int = -1
            val name = arguments.concat()
            val tempIndex = name.toIntOrNull()
            if (tempIndex != null && tempIndex in 1..(Category.values().size )) categoryIndex = tempIndex - 1
            else Category.values().forEachIndexed { index, category -> if (name.equals(category.fancyName, true)) categoryIndex = index }
            if (categoryIndex == -1) {
                channel.send(member, "Unable to find the category you specified... Type **${guild.getPrefix()}help** to see a list")
                return
            } else {
                val category = Category.values()[categoryIndex]
                embed = embed("${category.fancyName} | Command List", member, Color.DARK_GRAY)
                factory.commands.filter { it.category == category }.toMutableList().shuffle().forEach { command ->
                    embed.appendDescription("\n${Emoji.SMALL_BLUE_DIAMOND} **${command.name}**: ${command.description}")
                    if (command.aliases.isNotEmpty()) {
                        embed.appendDescription("\n")
                        if (command.aliases.size > 1) embed.appendDescription("         __aliases: [${command.aliases.toList().stringify()}]__")
                        else embed.appendDescription("         __alias: ${command.aliases.toList().stringify()}__")
                    }
                }
                embed.appendDescription("\n\n*Did you know you can also type \"_ardent help_\" along with \"_/help_\" ? You can also change the set prefix for your server!*")
            }
        }
        channel.send(member, embed)
    }
}

class ServerInfo : Command(Category.SERVER_INFO, "serverinfo", "view some basic information about this server", "guildinfo", "si", "gi") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
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
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
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

class Support : Command(Category.BOT_INFO, "support", "need help? something not working?") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Need help? Something not working? Join our support server @ https://discord.gg/rfGSxNA")
    }
}

class GetId : Command(Category.SERVER_INFO, "getid", "get the id of people in your server by mentioning them") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val mentionedUsers = event.message.mentionedUsers
        if (mentionedUsers.size == 0) channel.send(member, "You need to mention some people (or bots)!")
        else {
            mentionedUsers.forEach { channel.send(member, "**${it.withDiscrim()}**'s ID: ${it.id}") }
        }
    }
}

class RoleInfo : Command(Category.SERVER_INFO, "roleinfo", "view useful information about roles in this server", "ri") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val role = event.message.getFirstRole(arguments)
        if (role == null) channel.send(member, "You need to either mention a role or type its full name!")
        else {
            channel.send(member, embed("Info about ${role.name}", member)
                    .setThumbnail(guild.iconUrl)
                    .addField("# with role", guild.members.filter { it.roles.contains(role) }.count().toString() + " members", true)
                    .addField("Role ID", role.id, true)
                    .addField("Creation Date", (role.creationTime.toEpochSecond() / 1000).readableDate(), true)
                    .addField("Hex Color", "#${Integer.toHexString(role.color.rgb).substring(2).toUpperCase()}", true)
                    .addField("Permissions", role.permissions.map { it.getName() }.concat(), true)
            )
        }
    }
}

class WebsiteCommand : Command(Category.BOT_INFO, "website", "get the link for Ardent's cool website") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        channel.send(member, "Check out the pages @ https://ardentbot.com ")
    }
}

class Status : Command(Category.BOT_INFO, "status", "check realtime statistics about the bot") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
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