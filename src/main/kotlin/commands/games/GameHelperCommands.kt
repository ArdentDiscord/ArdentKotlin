package commands.games
/*
import events.Category
import events.Command
import main.waiter
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import translation.tr
import utils.*
import utils.discord.embed
import utils.discord.send
import utils.discord.toFancyString
import utils.discord.toUser
import utils.discord.toUsers
import utils.functionality.Emoji
import utils.functionality.Settings
import java.util.concurrent.TimeUnit

class Cancel : Command(Category.GAMES, "cancel", "cancel a currently running game") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        var found = false
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id) {
                found = true
                event.channel.send("${Emoji.HEAVY_EXCLAMATION_MARK_SYMBOL}" +
                        "Are you sure you want to cancel your __{0}__ game? Type **".tr(event, game.type.readable) + "yes".tr(event) + "** if so or **" + "no".tr(event) + "** if you're not sure.".tr(event, game.type.readable) + "\n" +
                        "Current players in lobby: *{0}*".tr(event, game.players.toUsers()))
                waiter.waitForMessage(Settings(event.author.id, event.channel.id, event.guild.id), { message ->
                    if (message.contentRaw.startsWith("ye") || message.contentRaw.startsWith("yes".tr(event))) {
                        game.cancel(event.member!!)
                    } else event.channel.send("${Emoji.BALLOT_BOX_WITH_CHECK} " + "I'll keep the game in lobby".tr(event))
                }, {
                    event.channel.send("You're not the creator of a game in lobby!".tr(event) + " ${Emoji.NO_ENTRY_SIGN}")
                })
            }
        }
        if (!found) event.channel.send("You're not the creator of a game in lobby!".tr(event) + " ${Emoji.NO_ENTRY_SIGN}")
    }
}

class Forcestart : Command(Category.GAMES, "start", "manually start a game", "forcestart") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id && game.channel.guild == event.guild) {
                if (game.players.size == 1 && game.type != GameType.TRIVIA) event.channel.send("You can't force start a game with only **1** person!".tr(event))
                else game.startEvent()
                return
            }
        }
        event.channel.send("You're not the creator of a game in lobby!".tr(event) + " ${Emoji.NO_ENTRY_SIGN}")
    }

    fun registerSubcommands() {
    }
}


class AcceptInvitation : Command(Category.GAMES, "accept", "accept an invitation to a game") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member!!.isInGameOrLobby()) event.channel.send("You can't join another game! You must leave the game you're currently in first".tr(event))
        else {
            gamesInLobby.forEach { game ->
                if (checkInvite(event, game)) return
            }
            event.channel.send("You must be invited by the creator of a game to join!".tr(event))
        }
    }

    fun registerSubcommands() {
    }
}

class JoinGame : Command(Category.GAMES, "join", "join a game in lobby") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 1) {
            val id = arguments[0].replace("#", "").toIntOrNull()
            if (id == null) {
                event.channel.send("You need to include a Game ID! Example: **{0}join #123456**".tr(event, event.guild.getPrefix()))
                return
            }
            gamesInLobby.forEach { game ->
                if (game.channel.guild == event.guild) {
                    if (event.member!!.isInGameOrLobby()) event.channel.send("You can't join another game! You must leave the game you're currently in first".tr(event))
                    else {
                        if (game.isPublic || checkInvite(event, game)) {
                            game.players.add(event.author.id)
                            event.channel.send("**{0}** has joined **{1}**'s game of {2}".tr(event, event.author.toFancyString(), game.creator.toUser()?.toFancyString() ?: "unknown", game.type.readable) + "\n" +
                                    "Current players in lobby: *{0}*".tr(event, game.players.toUsers()))
                        }
                    }
                    return
                }
            }
            event.channel.send("There's not a game in lobby with the ID of **#{0}**".tr(event, id))
        } else event.channel.send("You need to include a Game ID! Example: **{0}join #123456**".tr(event, event.guild.getPrefix()))
    }
}

class LeaveGame : Command(Category.GAMES, "leavegame", "leave a game you're currently queued for", "lg") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id && game.channel.guild == event.guild) {
                event.channel.send("You can't leave the game that you've started! If you want to cancel the game, type **{0}cancel**".tr(event, event.guild.getPrefix()))
                return
            } else if (game.players.contains(event.author.id)) {
                game.players.remove(event.author.id)
                event.channel.send("{0}, you successfully left **{1}**'s game".tr(event, event.author.asMention, game.creator.toUser()?.toFancyString() ?: "unknown"))
                return
            }
        }
        event.channel.send("You're not in a game lobby!".tr(event))
    }
}

class Gamelist : Command(Category.GAMES, "gamelist", "show a list of all currently running games") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val embed = event.member!!.embed("Games in Lobby", event.channel)
        val builder = StringBuilder()
                .append("**" + "Red means that the game is in lobby, Green that it's currently ingame".tr(event) + "**")
        if (gamesInLobby.isEmpty() && activeGames.isEmpty()) event.channel.send("\n\n" + "There are no games in lobby or ingame right now. You can start one though :) Type {0}help to see a list of game commands".tr(event, event.guild.getPrefix()))
        else {
            gamesInLobby.forEach {
                builder.append("\n\n ${Emoji.LARGE_RED_CIRCLE}")
                        .append("  **${it.type.readable}** [**${it.players.size}** / **${it.playerCount}**] " + "created by".tr(event) + " __${it.creator.toUser()?.toFancyString()}__ | ${it.players.toUsers()}")
            }
            activeGames.forEach {
                builder.append("\n\n ${Emoji.LARGE_GREEN_CIRCLE}")
                        .append("  **${it.type.readable}** [**${it.players.size}** / **${it.playerCount}**] " + "created by".tr(event) + " __${it.creator.toUser()?.toFancyString()}__ | ${it.players.toUsers()}")
            }
            builder.append("\n\n" + "__Take Note__: You can run only one game of each type at a time in this server unless you become an Ardent patron".tr(event))
            event.channel.send(embed.setDescription(builder.toString()))
        }
    }
}

class Decline : Command(Category.GAMES, "decline", "decline a pending game invite") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (invites.containsKey(event.author.id)) {
            val game = invites[event.author.id]!!
            event.channel.send("{0} declined an invite to **{1}**'s game of **{2}**".tr(event, event.author.asMention, game.creator.toUser()?.toFancyString() ?: "unknown", game.type.readable))
            invites.remove(event.author.id)
        } else event.channel.send("You don't have a pending invite to decline!".tr(event))
    }
}

class InviteToGame : Command(Category.GAMES, "gameinvite", "invite people to your game!", "ginvite", "gi") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id && game.channel.guild == event.guild) {
                if (game.isPublic) {
                    event.channel.send("You don't need to invite people to a public game! Everyone can join".tr(event))
                    return
                }
                val mentionedUsers = event.message.mentionedUsers
                if (mentionedUsers.size == 0 || mentionedUsers[0].isBot) event.channel.send("You need to mention at least one member to invite them".tr(event))
                else {
                    mentionedUsers.forEach { toInvite ->
                        when {
                            invites.containsKey(toInvite.id) -> event.channel.send("You can't invite a member who already has a pending invite!".tr(event))
                            toInvite.isInGameOrLobby() -> event.channel.send("This person is already in a lobby or ingame!".tr(event))
                            else -> {
                                invites.put(toInvite.id, game)
                                event.channel.send("{0}, you're being invited by {1} to join a game of **{2}**! Type *{3}accept* to accept this invite and join the game or decline by typing *{3}decline*".tr(event, toInvite.asMention, event.member!!.asMention, game.type.readable, event.guild.getPrefix()))
                                val delay = 45
                                waiter.executor.schedule({
                                    if (invites.containsKey(toInvite.id)) {
                                        event.channel.send("{0}, your invite to **{1}**'s game has expired after {2} seconds.".tr(event, toInvite.asMention, game.creator.toUser()?.toFancyString() ?: "unknown", delay))
                                        invites.remove(toInvite.id)
                                    }
                                }, delay.toLong(), TimeUnit.SECONDS)
                            }
                        }
                    }
                }
                return
            }
        }
        event.channel.send("You're not the creator of a game in lobby!".tr(event) + " ${Emoji.NO_ENTRY_SIGN}")
    }
}

fun checkInvite(event: MessageReceivedEvent, game: Game): Boolean {
    return if (invites.containsKey(event.author.id) && invites[event.author.id]!!.gameId == game.gameId) {
        invites.remove(event.author.id)
        game.players.add(event.author.id)
        event.channel.send("**{0}** has joined **{1}**'s game of {2}".tr(event, event.author.toFancyString(), game.creator.toUser()?.toFancyString() ?: "unknown", game.type.readable) + "\n" +
                "Current players in lobby: *{0}*".tr(event, game.players.toUsers()))
        true
    } else false
}
*/