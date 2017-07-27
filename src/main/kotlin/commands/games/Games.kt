package commands.games

import events.Category
import events.Command
import main.waiter
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

val invites = ConcurrentHashMap<String, String>()

class Games : Command(Category.GAMES, "minigames", "who's the most skilled? play against friends or compete for the leaderboards in these addicting games") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            withHelp("list", "lists all games that are waiting for players or setting up to start")
                    .withHelp("invite @User", "allows the creator of the game to invite players in the server where it was started")
                    .withHelp("join #game_id", "join a public game by its id or a game that you were invited to")
                    .withHelp("create", "start a game")
                    .withHelp("forcestart", "force start a game")
                    .withHelp("cancel", "cancel the game while it's in setup (for creators)")
                    .withHelp("leave", "leave a game or its lobby (this could trigger your resignation from the game if it has already started)")
                    .displayHelp(channel, member)
            return
        }
        when (arguments[0]) {
            "create" -> {
                if (member.isInGame()) channel.send(member, "${member.user.asMention}, You're already in game! You can't create another game!")
                else {
                    channel.selectFromList(member, "Which type of game would you like to create?", GameType.values().map { it.readable }.toMutableList(), {
                        selected ->
                        val gameType = GameType.values()[selected]
                        if (guild.hasGameType(gameType)) channel.send(member, "There can only be one of this type of game active at a time in a server!")
                        else {
                            channel.selectFromList(member, "Would you like this game of ${gameType.readable} to be open to everyone to join?", mutableListOf("Yes", "No"), {
                                public ->
                                val isPublic = public == 0
                                channel.send(member, "Are you sure you still want to host this game? Type **yes** if so or **no** if not")
                                waiter.waitForMessage(Settings(1, member.user.id, channel.id, guild.id), {
                                    message ->
                                    if (message.content.startsWith("ye", true)) {
                                        when (gameType) {
                                            GameType.COINFLIP -> {
                                                channel.send(member, "How many players would you like in this game? Type `none` to set the limit as 999 (effectively no limit)")
                                                waiter.waitForMessage(Settings(2, member.user.id, channel.id, guild.id), {
                                                    playerCount ->
                                                    val count = playerCount.content.toIntOrNull() ?: 999
                                                    val game = CoinflipGame(channel, member.user.id, count, isPublic)
                                                    gamesInLobby.add(game)

                                                })
                                            }

                                        }
                                        // TODO("Fill in the other games")
                                    }
                                    else channel.send(member, "Cancelled game setup ${Emoji.SQUARED_OK}")
                                }, 20, TimeUnit.SECONDS)
                            })
                        }
                    })
                }
            }
            "list" -> {
                val embed = embed("Games in Lobby", member)
                val builder = StringBuilder()
                        .append("**Red means that the game is private, Green that it's public and anyone can join**")
                if (gamesInLobby.isEmpty()) builder.append("\n\nThere are no games in lobby right now. You can start one by typing **${guild.getPrefix()}minigames create**")
                else {
                    gamesInLobby.forEach {
                        builder.append("\n\n ")
                        if (it.isPublic) builder.append(Emoji.LARGE_GREEN_CIRCLE)
                        else builder.append(Emoji.LARGE_RED_CIRCLE)
                        builder.append("  **${it.type.readable}** [**${it.players.size}** / **${it.playerCount}**] created by __${it.creator.toUser()!!.withDiscrim()}__")
                    }
                    builder.append("\n\n__Take Note__: You can run only one game of each type at a time in this server")
                    channel.send(member, embed.setDescription(builder.toString()))
                }
            }
            "cancel" -> {

            }
            "start" -> {

            }
            "invite" -> {

            }
        }
    }
}