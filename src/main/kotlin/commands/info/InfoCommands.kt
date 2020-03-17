package commands.info

import events.Category
import events.Command
import events.ExtensibleCommand
import main.factory
import main.hostname
import net.dv8tion.jda.api.OnlineStatus
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import translation.tr
import utils.discord.*
import utils.functionality.*
import java.text.DecimalFormat
import java.time.Instant
import java.time.ZoneOffset
import java.util.stream.Collectors

val formatter = DecimalFormat("#,###")

class Help : Command(Category.BOT_INFO, "help", "see a list of commands you can use", "h") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size > 0) {
            val name = arguments.concat()
            factory.commands.forEach {
                if (it.name.equals(name, true)) {
                    val embed = event.member!!.embed("Ardent | {0}".tr(event, it.name), event.textChannel)
                            .setThumbnail("https://previews.123rf.com/images/jianghaistudio/jianghaistudio1408/jianghaistudio140800205/30309384-Abstract-image-with-question-mark-made-of-gears-Stock-Vector.jpg")
                            .appendDescription("**" + "Command Description:".tr(event) + "** *" + it.description.tr(event) + "*")
                    embed.appendDescription("\n\n")
                    if (it !is ExtensibleCommand) embed.appendDescription("**" + "There are no subcommands registered for this command".tr(event) + "**")
                    else {
                        embed.appendDescription("**" + "Subcommands:".tr(event) + "**\n")
                        it.subcommands.forEach { subcommand ->
                            embed.appendDescription("${Emoji.SMALL_ORANGE_DIAMOND}  __${subcommand.syntax.tr(event)}__: ${(subcommand.description ?: "No description is available for this subcommand").tr(event)}\n")
                        }
                    }
                    if (it.aliases.isNotEmpty()) embed.appendDescription("\n\n**" + "Aliases:" + "** ${it.aliases.toList().stringify()}")
                    event.channel.send(embed)
                    return
                }
            }
            event.channel.send("No command was found with name **{0}** - showing default help menu instead".tr(event, name) + " " + Emoji.HEAVY_MULTIPLICATION_X.symbol)
            Thread.sleep(1500)
        }
        val embed = event.member!!.embed("Ardent | Command List", event.textChannel)
        Category.values().forEach { category ->
            embed.appendDescription("**" + category.fancyName.tr(event) + "** :  " +
                    category.getCommands().toMutableList().shuffle().map { "`" + it.name.tr(event) + "`" }.stream().collect(Collectors.joining("    ")) +
                    "\n\n")
        }
        embed.appendDescription("__" + "To see detailed information about a command, type */help command name*" + "__".tr(event))
        event.channel.send(embed)
    }
}

class Ping : Command(Category.BOT_INFO, "ping", "what did you think this command was gonna do?") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val currentTime = System.currentTimeMillis()
        event.channel.sendMessage("I'll calculate my ping to Discord using this message".tr(event.guild)).queue({ m ->
            m.editMessage("**Ping**: *{0} ms*".tr(event, (System.currentTimeMillis() - currentTime).toString()))?.queue()
        })
    }
}

class Invite : Command(Category.BOT_INFO, "invite", "get Ardent's invite URL", "ardent") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("My invite link is {0} - have fun using Ardent!".tr(event, "<$hostname/invite>"))
    }
}

class Donate : Command(Category.BOT_INFO, "donate", "learn how to support Ardent and get special perks for it!") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Want to support our work and obtain some perks along the way? Head to {0} to see the different ways you could help us out!".tr(event, "<$hostname/patreon>"))
    }
}

class About : Command(Category.BOT_INFO, "about", "learn more about Ardent") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val builder = event.member!!.embed("About the bot and its founders".tr(event), event.channel)
        builder.appendDescription("Ardent was originally founded in November 2016 by Adam#9261. It reached over 4,000 servers " +
                "by June, but Adam had to shut it down due to chronic stability issues with the bot and the fact that he was going on " +
                "a data learning program without any internet for nearly two months. When he came back, he decided to recreate Ardent with a " +
                "new focus on modern design, utility, usability, and games. This is the continuation of the original Ardent bot. We hope you like it!")
        event.channel.send(builder)
    }
}

// IAM


class IamCommand : Command(Category.ADMINISTRATE, "iam", "gives you the role you wish to receive", "iamrole") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = event.guild.getData()
        if (arguments.size == 0) {
            val embed = event.member!!.embed("Iam List".tr(event), event.channel)
            val builder = StringBuilder().append("This is the **autoroles** list for *{0}* - to add or delete them, type **/settings**".tr(event, event.guild.name) + "\n")
            if (data.roleSettings.autoroles.size == 0) builder.append("This server doesn't have any autoroles :(".tr(event))
            else {
                val iterator = data.roleSettings.autoroles.iterator()
                while (iterator.hasNext()) {
                    val it = iterator.next()
                    val role = it.role.toRole(event.guild)
                    if (role != null) builder.append("{0} Typing **{1}** will give you the **{2}** role".tr(event, Emoji.SMALL_ORANGE_DIAMOND.symbol, it.name, role.name)).append("\n")
                    else iterator.remove()
                }
                data.update()
                builder.append("\n").append("**Give yourself one of these roles by typing _/iam NAME_**".tr(event))
                builder.append("\n\n").append("**Tip**: You can create autoroles using */settings*".tr(event))
            }
            var curr = 0
            while (curr < builder.length) {
                event.channel.send(embed.setDescription(builder.substring(curr, if ((curr + 2048) <= builder.length) curr + 2048 else builder.length)))
                curr += 2048
            }
        } else {
            var found = false
            data.roleSettings.autoroles.forEach { iterator, current ->
                if (current.name.replace(" ", "").equals(arguments.concat().replace(" ", ""), true)) {
                    val role = current.role.toRole(event.guild)
                    if (role == null) {
                        iterator.remove()
                        data.update()
                    } else {
                        try {
                            event.guild.addRoleToMember(event.member!!, role).reason("Ardent Autoroles").queue({
                                event.channel.send("Successfully gave you the **{0}** role!".tr(event, role.name))
                            }, {
                                event.channel.send("Failed to give the *{0}* role - **Please ask an administrator of this server to allow me to give you roles!**".tr(event, role.name))
                            })
                        } catch (e: Throwable) {
                            event.channel.send("Failed to give the *{0}* role - **Please ask an administrator of this server to allow me to give you roles!**".tr(event, role.name))
                        }
                    }
                    found = true
                    return@forEach
                }
            }
            if (!found) event.channel.send("An autorole with that name wasn't found. Please type */iam* to get a full list".tr(event))
        }
    }
}

class IamnotCommand : Command(Category.ADMINISTRATE, "iamnot", "removes the role from you that you've been given via /iam", "iamrole") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = event.guild.getData()
        if (arguments.size == 0) {
            event.channel.send("Please type **/iam** to get a full list of available autoroles".tr(event))
            return
        }
        val name = arguments.concat()
        val iterator = data.roleSettings.autoroles.iterator()
        while (iterator.hasNext()) {
            val it = iterator.next()
            if (it.name.equals(name, true)) {
                val role = it.role.toRole(event.guild)
                if (role == null) {
                    iterator.remove()
                    data.update()
                } else {
                    if (event.member!!.roles.contains(role)) {
                        try {
                            event.guild.removeRoleFromMember(event.member!!, role).reason("Ardent Autoroles - Removal".tr(event)).queue({
                                event.channel.send("Successfully removed the **{0}** role!".tr(event, role.name))
                            }, {
                                event.channel.send("Failed to remove *{0}* - **please ask an administrator of this server to allow me to manage roles!**".tr(event, role.name))
                            })
                        } catch (e: Exception) {
                            event.channel.send("Failed to remove *{0}* - **please ask an administrator of this server to allow me to manage roles!**".tr(event, role.name))
                        }
                    } else event.channel.send("You can't remove a role you don't have!".tr(event))
                }
                return
            }
        }
        event.channel.send("An autorole with that name wasn't found. Please type **/iam** to get a full list".tr(event))
    }
}

// IAMNOT

class ServerInfo : Command(Category.SERVER_INFO, "serverinfo", "view some basic information about this server", "guildinfo", "si", "gi") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val guild = event.guild
        val data = guild.getData()
        val embed = event.member!!.embed("Server Info for {0}".tr(event, guild.name), event.channel)
        embed.addField("Number of users".tr(event), guild.members.size.toString(), true)
        embed.addField("Online users".tr(event), guild.members.stream().filter { m -> m.onlineStatus != OnlineStatus.OFFLINE }.count().format(), true)
        embed.addField("Prefix".tr(event), data.prefixSettings.prefix, true)
        embed.addField("Patron Server".tr(event), (if (guild.isPatronGuild()) "Yes" else "No").tr(event), true)
        embed.addField("Owner".tr(event), guild.owner?.user?.toFancyString(), true)
        embed.addField("Creation Date".tr(event), guild.timeCreated.toLocalDate().toString(), true)
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
            val mentionedMember = event.guild.getMember(mentioned)!!
            event.channel.send(event.member!!.embed("Information about {0}".tr(event, mentionedMember.effectiveName), event.channel)
                    .setThumbnail(mentioned.avatarUrl)
                    .addField("Name".tr(event), mentioned.toFancyString(), true)
                    .addField("Nickname".tr(event), mentionedMember?.nickname ?: "None".tr(event), true)
                    .addField("Server Join Date".tr(event), mentionedMember.timeJoined.toLocalDate().toString(), true)
                    .addField("Days in Guild".tr(event), Math.ceil((Instant.now().atOffset(ZoneOffset.UTC).toEpochSecond() -
                            event.member!!.timeJoined.toInstant().atOffset(ZoneOffset.UTC).toEpochSecond() / (60 * 60 * 24)).toDouble())
                            .toString(), true)
                    .addField("Roles".tr(event), mentionedMember.roles.map { it.name }.concat(), true)
                    .addField("Account Creation Date".tr(event), mentioned.timeCreated.toLocalDate().toString(), true))
        }
    }
}

class Support : Command(Category.BOT_INFO, "support", "need help? something not working?") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Need help? Something not working? Join our support server @ {0}".tr(event, "https://discord.gg/VebBB5z"))
    }
}

class GetId : Command(Category.SERVER_INFO, "getid", "get the id of people in your server by mentioning them") {
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
            event.channel.send(event.member!!.embed("Info about {0}".tr(event, role.name), event.channel)
                    .setThumbnail(event.guild.iconUrl)
                    .addField("# with role".tr(event), "{0} members".tr(event, event.guild.members.filter { it.roles.contains(role) }.count().toString()), true)
                    .addField("Role ID".tr(event), role.id, true)
                    .addField("Creation Date".tr(event), (role.timeCreated.toEpochSecond() / 1000).readableDate(), true)
                    .addField("Permissions".tr(event), role.permissions.map { it.getName() }.stringify(), true)
            )
        }
    }
}

class WebsiteCommand : Command(Category.BOT_INFO, "website", "get the link for Ardent's cool website") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("Check out our site @ {0}".tr(event, "<$hostname>"))
    }
}

class Status : Command(Category.BOT_INFO, "status", "check realtime statistics about the bot") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val onlyBot = getAllGuilds().filter { it.members.filter { it.user.isBot }.count() == 1 }.count()
        event.channel.send(event.member!!.embed("Ardent Realtime Status", event.channel)
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
                .addField("Website".tr(event), hostname, true))
    }
}