package commands.administrate

import com.ardentbot.Eval
import commands.music.getAudioManager
import events.Category
import events.Command
import events.ExtensibleCommand
import main.config
import main.hostname
import main.jdas
import main.managers
import net.dv8tion.jda.api.MessageBuilder
import net.dv8tion.jda.api.Permission
import net.dv8tion.jda.api.entities.Message
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.requests.RestAction
import translation.tr
import utils.discord.StaffRole
import utils.discord.getGuildById
import utils.discord.getGuildsByName
import utils.discord.hasPermission
import utils.discord.hasStaffLevel
import utils.discord.send
import utils.discord.toFancyString
import utils.functionality.concat
import utils.music.ServerQueue
import java.util.concurrent.TimeUnit

class Clear : Command(Category.ADMINISTRATE, "clear", "clear messages in the channel you're sending the command in") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (!event.member!!.hasPermission(event.textChannel)) return
        if (!event.guild.selfMember.hasPermission(event.textChannel, Permission.MESSAGE_MANAGE)) {
            event.channel.send("I need the `Message Manage` permission to be able to delete messages!".tr(event))
            return
        }
        if (arguments.size == 0) event.channel.send("You need to specify a number of messages - *or mention a member whose messages* to delete!".tr(event))
        else {
            val mentionedUsers = event.message.mentionedUsers
            if (mentionedUsers.size > 0) {
                event.channel.history.retrievePast(100).queue { messages ->
                    val messagesByUser = mutableListOf<Message>()
                    messages.forEach { m -> if (m.author == mentionedUsers[0]) messagesByUser.add(m) }
                    event.textChannel.deleteMessages(messagesByUser).queue({
                        event.channel.send("Successfully deleted **{0}** messages from *{1}*".tr(event, messagesByUser.size, mentionedUsers[0].toFancyString()))
                    }, {
                        event.channel.send("Unable to delete messages, please recheck my permissions".tr(event))
                    })
                }
                return
            }
            val number = arguments[0].toIntOrNull()
            if (number == null || number < 2 || number > 100) event.channel.send("Invalid number specified. Number must be between **2** and **100** messages".tr(event))
            else {
                event.channel.history.retrievePast(number).queue { messages ->
                    try {
                        event.textChannel.deleteMessages(messages).queue {
                            event.channel.send("Successfully cleared **{0}** messages".tr(event, number))
                        }
                    } catch (e: Exception) {
                        event.channel.send("Unable to clear messages - {0}".tr(event, e.localizedMessage))
                    }
                }
            }
        }
    }
}

class Automessages : Command(Category.ADMINISTRATE, "joinmessage", "set join or leave messages for new or leaving members") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send("You can manage **join** and **leave** messages on our web panel at {0}".tr(event, "<$hostname/manage/${event.guild.id}>"))
    }
}

class AdministratorCommand : ExtensibleCommand(Category.ADMINISTRATE, "admin", "commands for bot administrators only") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        showHelp(event)
    }

    override fun registerSubcommands() {
        with("eval", null, "evaluate code", { arguments, event ->
            if (event.author.hasStaffLevel(StaffRole.ADMINISTRATOR, event.textChannel, true)) eval(arguments, event)
        })

        with("inv", "inv server-name", "generate a temporary invite to investigate bot abuses", { arguments, event ->
            if (event.author.hasStaffLevel(StaffRole.ADMINISTRATOR, event.textChannel, true)) {
                val guilds = getGuildsByName(arguments.concat(), true)
                guilds.forEach { guild ->
                    try {
                        guild.retrieveInvites().queue { invites ->
                            if (invites.size > 0) event.channel.send("Found invite https://discord.gg/${invites[0].code} for guild with id **${guild.id}**")
                            else "hi dad".toInt()
                        }
                    } catch (e: Exception) {
                        try {
                            (guild.defaultChannel
                                    ?: guild.textChannels[0]).createInvite().setMaxUses(1).setTemporary(true).setMaxAge(5L, TimeUnit.MINUTES)
                                    .reason("Temporary Invite - Testing").queue { invite -> event.channel.send("Generated invite https://discord.gg/${invite.code} for id **${guild.id}**") }

                        } catch (e: Exception) {
                            event.channel.send("Cannot retrieve invite for guild with ID **${guild.id}** - owner is ${guild.owner?.asMention}")
                        }
                    }
                }
                if (guilds.size == 0) event.channel.send("No guild found with that name")
            }
        })

        with("shutdown", null, "shut down the bot and begin update process", { arguments, event ->
            if (event.author.hasStaffLevel(StaffRole.ADMINISTRATOR, event.textChannel, true)) {
                managers.forEach {
                    try {
                        val guild = getGuildById(it.key.toString())
                        if (guild?.selfMember?.voiceState?.channel != null) {
                            val manager = guild.getAudioManager(null)
                            if (manager.channel != null && manager.player.playingTrack != null || manager.manager.queue.size > 0) {
                                val tracks = mutableListOf<String>()
                                if (it.value.player.playingTrack != null) tracks.add(it.value.player.playingTrack.info.uri)
                                manager.manager.queue.forEach { song -> if (song.getUri() != null) tracks.add(song.getUri()!!) }
                                ServerQueue(guild.selfMember.voiceState!!.channel!!.id, manager.channel!!.id, tracks)
                                manager.channel!!.send("Ardent will be down for a few minutes to update, but don't fear! Your music queue has been preserved and playback will resume after a few minutes".tr(guild))
                            }
                        }
                    } catch (ignored: Exception) {
                    }
                }
                //gamesInLobby.forEach { game -> game.channel.send("Ardent is **updating** - Your game data will not be saved :(".tr(game.channel)) }
                // activeGames.forEach { game -> game.channel.send("Ardent is **updating** - Your game data will not be saved :(".tr(game.channel)) }
                event.channel.sendMessage("Shutting down").queue {
                    jdas.forEach { it.shutdown() }
                    System.exit(0)
                }
            }
        })
    }
}

class GiveRoleToAll : Command(Category.ADMINISTRATE, "giverole", "give all users who don't have any role, the role you specify", "giveall") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            event.channel.send("You need to type the name of the role that you'd like to give to all members who currently have no roles in this server!".tr(event))
            return
        }
        if (event.member!!.hasPermission(event.textChannel)) {
            val query = arguments.concat()
            val results = event.guild.getRolesByName(query, true)
            if (results.size == 0) event.channel.send("No roles with that name were found".tr(event))
            else {
                event.channel.send("Running... This could take a while".tr(event))
                event.guild.members.forEach { m -> if (m.roles.size < 2) event.guild.addRoleToMember(m, results[0]).queue() }
            }
        }
    }
}

class Blacklist : ExtensibleCommand(Category.ADMINISTRATE, "blacklist", "blacklist roles or members from using Ardent, or disallow commands in certain channels") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        showHelp(event)
    }

    override fun registerSubcommands() {
        /*with("add", "add [user mention, channel mention or name, or role mention or name]", "add the specified user, channel, or role to the blacklist", { arguments, event ->
            if (arguments.size == 0) event.channel.send("You can mention a user to blacklist them. You can also mention **or** type the name of a role or channel to add it to the blacklist.".tr(event))
            else {
                if (event.member!!.hasOverride(event.textChannel, failQuietly = false)) {
                    val data = event.guild.getData()
                    if (event.message.mentionedUsers.size > 0) {
                        val blacklistUser = event.message.mentionedUsers[0]
                        when {
                            blacklistUser.isBot -> event.channel.send("You can't blacklist a bot!".tr(event))
                            blacklistUser.id == event.author.id -> event.channel.send("You can't blacklist yourself, silly!".tr(event))
                            blacklistUser.isStaff() || event.guild.getMember(blacklistUser).hasOverride(event.textChannel, failQuietly = true) -> {
                                event.channel.send("This permission either has `Manage Server` or is an Ardent staff member, so you can't mute them!")
                            }
                            data.blacklistedUsers?.contains(blacklistUser.id) == true -> event.channel.send("This user is already blacklisted!".tr(event))
                            else -> {
                                data.blacklistedUsers!!.add(blacklistUser.id)
                                data.update()
                                event.channel.send("${Emoji.BALLOT_BOX_WITH_CHECK.symbol} " + "Successfully blacklisted {0} from using Ardent".tr(event, blacklistUser.asMention))
                            }
                        }
                    } else {
                        val blacklistRole: Role? = event.message.mentionedRoles.getOrNull(0) ?: event.guild.getRolesByName(arguments.concat(), true).getOrNull(0)
                        if (blacklistRole == null) {
                            val channel = event.message.mentionedChannels.getOrNull(0) ?: event.guild.getTextChannelsByName(arguments.concat(), true).getOrNull(0)
                            if (channel != null) {
                                if (data.blacklistedChannels?.contains(channel.id) == true) event.channel.send("This channel is already blacklisted!".tr(event))
                                else {
                                    data.blacklistedChannels!!.add(channel.id)
                                    data.update()
                                    event.channel.send("${Emoji.BALLOT_BOX_WITH_CHECK.symbol} " + "Members can no longer use Ardent commands in **{0}**".tr(event, channel.name))
                                }
                            } else event.channel.send("You need to specify a valid user, channel, or role to blacklist!".tr(event))
                        } else if (blacklistRole.hasPermission(Permission.MANAGE_SERVER) || blacklistRole.hasPermission(Permission.ADMINISTRATOR)) event.channel.send("You can't blacklist a role that has the `Manage Server` permission!".tr(event))
                        else if (data.blacklistedRoles?.contains(blacklistRole.id) == true) event.channel.send("This role is already blacklisted!".tr(event))
                        else {
                            data.blacklistedRoles!!.add(blacklistRole.id)
                            data.update()
                            event.channel.send("${Emoji.BALLOT_BOX_WITH_CHECK.symbol} " + "Successfully blacklisted members with the **{0}** role from using Ardent".tr(event, blacklistRole.name))
                        }
                    }
                }
            }
        })

        with("remove", "remove [user mention, channel mention or name, or role mention or name]", "removes the specified user, channel, or role from the blacklist", { arguments, event ->
            if (arguments.size == 0) event.channel.send("You can mention a user to unblacklist them. You can also mention **or** type the name of a role to remove it from the blacklist".tr(event))
            else {
                if (event.member!!.hasOverride(event.textChannel, failQuietly = false)) {
                    val data = event.guild.getData()
                    val unblacklistUser = event.message.mentionedUsers.getOrNull(0)
                    if (unblacklistUser != null) {
                        if (data.blacklistedUsers?.contains(unblacklistUser.id) == false) event.channel.send("{0} isn't blacklisted!".tr(event, unblacklistUser.asMention))
                        else {
                            data.blacklistedUsers?.remove(unblacklistUser.id)
                            data.update()
                            event.channel.send("${Emoji.BALLOT_BOX_WITH_CHECK.symbol} " + "Unblacklisted {0}".tr(event, unblacklistUser.asMention))
                        }
                    } else {
                        val role = event.message.mentionedRoles.getOrNull(0) ?: event.guild.getRolesByName(arguments.concat(), true).getOrNull(0)
                        if (role != null) {
                            if (data.blacklistedRoles?.contains(role.id) == false) event.channel.send("The **{0}** role isn't blacklisted!".tr(event, role.name) + " ${Emoji.THINKING_FACE.symbol}")
                            else {
                                data.blacklistedRoles?.remove(role.id)
                                data.update()
                                event.channel.send("${Emoji.BALLOT_BOX_WITH_CHECK.symbol} " + "Unblacklisted the **{0}** role".tr(event, role.name))
                            }
                        } else {
                            val channel = event.message.mentionedChannels.getOrNull(0) ?: event.guild.getTextChannelsByName(arguments.concat(), true).getOrNull(0)
                            if (channel != null) {
                                if (data.blacklistedChannels?.contains(channel.id) == false) event.channel.send("**{0}** isn't blacklisted!".tr(event, channel.name))
                                else {
                                    data.blacklistedChannels?.remove(channel.id)
                                    data.update()
                                    event.channel.send("${Emoji.BALLOT_BOX_WITH_CHECK.symbol} " + "Members can now use commands in **{0}**".tr(event, channel.name))

                                }
                            } else event.channel.send("You didn't specify a valid user, channel, or role")
                        }
                    }
                }
            }
        })

        with("list", null, "view a list of currently blacklisted users and roles", { _, event ->
            val data = event.guild.getData()
            val embed = event.member!!.embed("Ardent | Server Blacklists".tr(event))
            if (data.blacklistedChannels?.isEmpty() == false) {
                embed.appendDescription("**Blacklisted Channels**".tr(event))
                val iterator = data.blacklistedChannels!!.iterator()
                while (iterator.hasNext()) {
                    val channelId = iterator.next()
                    val channel = event.guild.getTextChannelById(channelId)
                    if (channel == null) {
                        iterator.remove()
                        data.update()
                    } else embed.appendDescription("\n${Emoji.SMALL_ORANGE_DIAMOND} **" + channel.name + "**")
                }
            } else embed.appendDescription("*There are no blacklisted channels in this server!*".tr(event))

            embed.appendDescription("\n\n")

            if (data.blacklistedRoles?.isEmpty() == false) {
                embed.appendDescription("**Blacklisted Roles**".tr(event))
                val iterator = data.blacklistedRoles!!.iterator()
                while (iterator.hasNext()) {
                    val roleId = iterator.next()
                    val role = event.guild.getRoleById(roleId)
                    if (role == null) {
                        iterator.remove()
                        data.update()
                    } else embed.appendDescription("\n${Emoji.SMALL_BLUE_DIAMOND} " + "**{0}** [{1} members]"
                            .tr(event, role.name, event.guild.members.filter { it.roles.contains(role) }.count()))
                }
            } else embed.appendDescription("*There are no blacklisted roles in this server!*".tr(event))

            embed.appendDescription("\n\n")

            if (data.blacklistedUsers?.isEmpty() == false) {
                embed.appendDescription("**Blacklisted Users**".tr(event))
                val iterator = data.blacklistedUsers!!.iterator()
                while (iterator.hasNext()) {
                    val userId = iterator.next()
                    val member = event.guild.getMemberById(userId)
                    if (member == null) {
                        iterator.remove()
                        data.update()
                    } else embed.appendDescription("\n${Emoji.SMALL_ORANGE_DIAMOND} " + member.withDiscrim())
                }
            } else embed.appendDescription("*There are no blacklisted users in this server!*".tr(event))
            event.channel.send(embed)
        })*/
    }
}

fun eval(arguments: MutableList<String>, event: MessageReceivedEvent) {
    val message = event.message
    val shortcuts = hashMapOf<String, Any>()
    shortcuts.put("jda", message.jda)
    shortcuts.put("event", event)
    shortcuts.put("channel", event.channel)
    shortcuts.put("guild", event.guild)
    shortcuts.put("message", message)
    shortcuts.put("msg", message)
    shortcuts.put("me", event.author)
    shortcuts.put("bot", message.jda.selfUser)
    shortcuts.put("config", config)
    shortcuts.put("jdas", jdas)
    try {
        val result = Eval.evalJda(arguments.concat(), shortcuts, 10)
        val builder = MessageBuilder()
        if (result.first is RestAction<*>) (result.first as RestAction<*>).queue()
        else if (result.first != null && (result.first as String).isNotEmpty()) builder.appendCodeBlock(result.first.toString(), "")
        if (!result.second.isEmpty() && result.first != null) builder.append("\n").appendCodeBlock(result.first as String, "")
        if (!result.third.isEmpty()) builder.append("\n").appendCodeBlock(result.third, "")
        if (builder.isEmpty) event.message.addReaction("✅").queue()
        else for (m in builder.buildAll(MessageBuilder.SplitPolicy.NEWLINE, MessageBuilder.SplitPolicy.SPACE, MessageBuilder.SplitPolicy.ANYWHERE)) event.channel.send(m.contentRaw)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}