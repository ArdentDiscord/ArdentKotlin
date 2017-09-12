package commands.games

import events.Category
import events.Command
import main.waiter
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

class Cancel : Command(Category.GAMES, "cancel", "cancel a currently running game") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id) {
                event.channel.send("${Emoji.HEAVY_EXCLAMATION_MARK_SYMBOL}" +
                        "Are you sure you want to cancel your __${game.type.readable}__ game? Type **yes** if so or **no** if you're not sure.\n" +
                        "Current players in lobby: *${game.players.toUsers()}*")
                waiter.waitForMessage(Settings(event.author.id, event.channel.id, event.guild.id), { message ->
                    if (message.content.startsWith("ye", true)) {
                        game.cancel(event.member)
                    } else event.channel.send("${Emoji.BALLOT_BOX_WITH_CHECK} I'll keep the game in lobby")
                })
                return
            }
        }
        event.channel.send("You're not the creator of a game that's in lobby! ${Emoji.NO_ENTRY_SIGN}")
    }
}

class Forcestart : Command(Category.GAMES, "start", "manually start a game", "forcestart") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id && game.channel.guild == event.guild) {
                if (game.players.size == 1 && game.type != GameType.TRIVIA) event.channel.send("You can't force start a game with only **1** person!")
                else {
                    game.startEvent()
                }
                return
            }
        }
        event.channel.send("You're not the creator of a game that's in lobby! ${Emoji.NO_ENTRY_SIGN}")
    }
}


class AcceptInvitation : Command(Category.GAMES, "accept", "accept an invitation to a game") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (event.member.isInGameOrLobby()) event.channel.send("You can't join another game! You must leave the game you're currently in first")
        else {
            gamesInLobby.forEach { game ->
                if (checkInvite(event, game)) return
            }
            event.channel.send("You must be invited by the creator of this game to join this game!")
        }
    }
}

class JoinGame : Command(Category.GAMES, "join", "join a game in lobby") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 1) {
            val id = arguments[0].replace("#", "").toIntOrNull()
            if (id == null) {
                event.channel.send("You need to include a Game ID! Example: **${event.guild.getPrefix()}join #123456**")
                return
            }
            gamesInLobby.forEach { game ->
                if (game.channel.guild == event.guild) {
                    if (event.member.isInGameOrLobby()) event.channel.send("You can't join another game! You must leave the game you're currently in first")
                    else {
                        if (game.isPublic || checkInvite(event, game)) {
                            game.players.add(event.author.id)
                            event.channel.send("**${event.author.withDiscrim()}** has joined **${game.creator.toUser()!!.withDiscrim()}**'s game of ${game.type.readable}\n" +
                                    "Players in lobby: *${game.players.toUsers()}*")
                        }
                    }
                    return
                }
            }
            event.channel.send("There's not a game in lobby with the ID of **#$id**")
        } else event.channel.send("You need to include a Game ID! Example: **${event.guild.getPrefix()}join #123456**")
    }
}

class LeaveGame : Command(Category.GAMES, "leavegame", "leave a game you're currently queued for", "lg") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id && game.channel.guild == event.guild) {
                event.channel.send("You can't leave the game that you've started! If you want to cancel the game, type **${event.guild.getPrefix()}cancel**")
                return
            } else if (game.players.contains(event.author.id)) {
                game.players.remove(event.author.id)
                event.channel.send("${event.author.asMention}, you successfully left **${game.creator.toUser()!!.withDiscrim()}**'s game")
                return
            }
        }
        event.channel.send("You're not in a game lobby!")
    }
}

class Gamelist : Command(Category.GAMES, "gamelist", "show a list of all currently running games") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val embed = event.member.embed("Games in Lobby")
        val builder = StringBuilder()
                .append("**Red means that the game is private, Green that it's public and anyone can join**")
        if (gamesInLobby.isEmpty()) event.channel.send("\n\nThere are no games in lobby right now. You can start one by typing **${event.guild.getPrefix()}minigames** and seeing what the command is for each specific game")
        else {
            gamesInLobby.forEach {
                builder.append("\n\n ")
                if (it.isPublic) builder.append(Emoji.LARGE_GREEN_CIRCLE)
                else builder.append(Emoji.LARGE_RED_CIRCLE)
                builder.append("  **${it.type.readable}** [**${it.players.size}** / **${it.playerCount}**] created by __${it.creator.toUser()?.withDiscrim()}__ | ${it.players.toUsers()}")
            }
            builder.append("\n\n__Take Note__: You can run only one game of each type at a time in this server")
            event.channel.send(embed.setDescription(builder.toString()))
        }
    }
}

class Decline : Command(Category.GAMES, "decline", "decline a pending game invite") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (invites.containsKey(event.author.id)) {
            val game = invites[event.author.id]!!
            event.channel.send("${event.author.asMention} declined an invite to **${game.creator.toUser()!!.withDiscrim()}**'s game of **${game.type.readable}**")
            invites.remove(event.author.id)
        } else event.channel.send("You don't have a pending invite to decline!")
    }
}

class InviteToGame : Command(Category.GAMES, "gameinvite", "invite people to your game!", "ginvite", "gi") {
    private val inviteManager: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        gamesInLobby.forEach { game ->
            if (game.creator == event.author.id && game.channel.guild == event.guild) {
                if (game.isPublic) {
                    event.channel.send("You don't need to invite people to a public game, as everyone can join")
                    return
                }
                val mentionedUsers = event.message.mentionedUsers
                if (mentionedUsers.size == 0 || mentionedUsers[0].isBot) event.channel.send("You need to mention at least one member to invite them")
                else {
                    mentionedUsers.forEach { toInvite ->
                        when {
                            invites.containsKey(toInvite.id) -> event.channel.send("You can't invite a member who already has a pending invite!")
                            toInvite.isInGameOrLobby() -> event.channel.send("This person is already in a lobby or ingame!")
                            else -> {
                                invites.put(toInvite.id, game)
                                event.channel.send("${toInvite.asMention}, you're being invited by ${event.member.asMention} to join a __${if (game.isPublic) "public" else "private"}__ game of " +
                                        "**${game.type.readable}**! Type *${event.guild.getPrefix()}accept* to accept this invite and join the game " +
                                        "or decline by typing *${event.guild.getPrefix()}decline*")
                                val delay = 45
                                inviteManager.schedule({
                                    if (invites.containsKey(toInvite.id)) {
                                        event.channel.send("${toInvite.asMention}, your invite to **${game.creator.toUser()!!.withDiscrim()}**'s game has expired after $delay seconds.")
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
        event.channel.send("You're not the creator of a game that's in lobby! ${Emoji.NO_ENTRY_SIGN}")
    }
}

fun checkInvite(event: MessageReceivedEvent, game: Game): Boolean {
    return if (invites.containsKey(event.author.id) && invites[event.author.id]!!.gameId == game.gameId) {
        invites.remove(event.author.id)
        game.players.add(event.author.id)
        event.channel.send("**${event.author.withDiscrim()}** has joined **${game.creator.toUser()!!.withDiscrim()}**'s game of ${game.type.readable}\n" +
                "Players in lobby: *${game.players.toUsers()}*")
        true
    } else false
}