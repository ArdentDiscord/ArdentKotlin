package commands.games

import com.sun.org.apache.xpath.internal.operations.Bool
import events.Category
import events.Command
import main.factory
import main.waiter
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*
import java.security.SecureRandom
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

val invites = ConcurrentHashMap<String, Game>()

class BlackjackGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.BLACKJACK, channel, creator, playerCount, isPublic) {
    val roundResults = mutableListOf<Round>()
    override fun onStart() {
        val ardent = channel.guild.selfMember
        val user = players.map { it.toUser()!! }.toList()[0]


    }
    fun doRound(user: User) {

    }

    class Round(val won: Boolean, val userHand: Hand, val dealerHand: Hand)
    class Hand(val cards: MutableList<Card>) {
        val random = Random()
        fun with(cardAmount: Int) : Hand {
            cards.add(generate())
            cards.add(generate())
            return this
        }
        fun generate() : Card {
            val card = Card(Suit.values()[random.nextInt(4)], BlackjackValue.values()[random.nextInt(13)])
            if (cards.contains(card)) return generate()
            else return card
        }
    }
    data class Card(val suit: Suit, val value: BlackjackValue) {
        override fun toString(): String {
            return "$suit$value"
        }
    }
    enum class Suit {
        HEART, SPADE, CLUB, DIAMOND;
        override fun toString(): String {
            return when (this) {
                HEART -> "❤"
                SPADE -> "♠"
                CLUB -> "♧"
                DIAMOND -> "♦"
            }
        }
    }
    enum class BlackjackValue(var representation: Int) {
        TWO(2),
        THREE(3),
        FOUR(4),
        FIVE(5),
        SIX(6),
        SEVEN(7),
        EIGHT(8),
        NINE(9),
        TEN(10),
        JACK(10),
        QUEEN(10),
        KING(10),
        ACE(11);

        override fun toString(): String {
            return when(this) {
                ACE -> "A"
                KING -> "K"
                QUEEN -> "Q"
                JACK -> "J"
                else -> representation.toString()
            }
        }
    }
}

class CoinflipGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.COINFLIP, channel, creator, playerCount, isPublic) {
    override fun onStart() {
        val ardent = channel.guild.selfMember
        val users = players.map { it.toUser()!! }.toList()
        channel.send(ardent, "This is a **5** round game where the person who guesses the side the most times wins.\nIf there's a tie, the top player will flip a coin and if they guess correctly, they win. " +
                "If not, the second place player will win.")
        val results = mutableListOf<Round>()

        val doRound = {
            round: Int ->
            val roundResults = Round()
            val rand = SecureRandom()
            channel.send(channel.guild.selfMember, "${users.map { it.asMention }.concat()}, you're up in round **$round** of **5**. Type `heads` now to guess **Heads** or `tails` to guess **Tails** - you have __15__ seconds!")
            factory.executor.execute {
                val answered = mutableListOf<String>()
                waiter.gameChannelWait(channel.id, { message ->
                    val it = message.author
                    if (players.contains(it.id) && !answered.contains(it.id)) {
                        val content = message.content
                        val isHeads: Boolean
                        when {
                            content.startsWith("he", true) -> isHeads = true
                            content.startsWith("ta", true) -> isHeads = false
                            else -> {
                                channel.send(it, "${it.asMention} didn't type **heads** or **tails** - Heads will be assumed")
                                isHeads = true
                            }
                        }
                        if (rand.nextBoolean() == isHeads) {
                            roundResults.winners.add(it.id)
                            channel.send(it, "Congrats, ${it.asMention} guessed correctly!")
                        } else {
                            channel.send(it, "Sadly, ${it.asMention} didn't guess correctly ;(")
                            roundResults.losers.add(it.id)
                        }
                        answered.add(it.id)
                    }
                })
            }

            Thread.sleep(15000)
            val missedRound = mutableListOf<String>()
            users.forEach {
                if (!roundResults.losers.contains(it.id) && !roundResults.winners.contains(it.id)) {
                    roundResults.losers.add(it.id)
                    missedRound.add(it.id)
                }
            }
            if (missedRound.size > 0) channel.send(channel.guild.selfMember, "The following user(s) missed this round and therefore lost: ${missedRound.toUsers()}")
            roundResults

        }
        (1..5).forEach { round ->
            results.add(doRound.invoke(round))
            displayCurrentResults(results)
        }
        val scores = results.mapScores()
        var winner: String? = null
        val values = scores.values.toIntArray()
        if (values[0] == values[1]) {
            val tiedPlayers = scores.keys.toList().subList(0, 2)
            val first = tiedPlayers[0].toUser()!!
            val second = tiedPlayers[1].toUser()!!
            channel.send(channel.guild.selfMember, "There's a tie between **${first.withDiscrim()}** and **${second.withDiscrim()}**!\n" +
                    "${first.asMention}, to prove the true winner, decide whether the next coin will land `heads` or `tails` - Choose wisely - you have 20 seconds!")
            waiter.waitForMessage(Settings(first.id, channel.id, channel.guild.id), { message ->
                val content = message.content
                val isHeads: Boolean
                when {
                    content.startsWith("he", true) -> isHeads = true
                    content.startsWith("ta") -> isHeads = false
                    else -> {
                        channel.send(first, "You didn't type **heads** or **tails** - Heads will be assumed")
                        isHeads = true
                    }
                }
                if (SecureRandom().nextBoolean() == isHeads) {
                    winner = first.id
                } else {
                    winner = second.id
                }
                channel.send(first, "Loading...")
            }, {
                channel.send(first, "Bad time to go AFK! You lose because of inactivity :(")
                winner = second.id
            }, time = 20)
            Thread.sleep(20000)
        } else {
            winner = scores.keys.toList()[0]
        }
        val winnerUser = winner!!.toUser()!!
        channel.send(channel.guild.selfMember, "Congratulations to **${winnerUser.withDiscrim()}** for winning with __${scores[winner!!]}__ correct guesses; You win **500** gold")
        val playerData = winnerUser.getData()
        playerData.gold += 500
        playerData.update()
        val gameData = GameDataCoinflip(gameId, creator, startTime!!, winner!!, players.without(winner!!), results)
        cleanup(gameData)
    }

    fun displayCurrentResults(results: MutableList<Round>) {
        val embed = embed("Current Results", channel.guild.selfMember)
        val scores = results.mapScores()
        scores.iterator().withIndex().forEach { (index, value) ->
            embed.appendDescription(" #${index.plus(1)} **${value.key.toUser()!!.withDiscrim()}**: *${value.value} correct guesses*\n")
        }
        embed.appendDescription("\n__Current Round__: **${results.size}**")
        channel.send(creator.toUser()!!, embed)
    }

    class Round(val winners: MutableList<String> = mutableListOf(), val losers: MutableList<String> = mutableListOf())
}


class Games : Command(Category.GAMES, "minigames", "who's the most skilled? play against friends or compete for the leaderboards in these addicting games") {
    val inviteManager: ScheduledExecutorService = Executors.newSingleThreadScheduledExecutor()
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) {
            withHelp("list", "lists all games that are waiting for players or setting up to start")
                    .withHelp("invite @User", "allows the creator of the game to invite players in the server where it was started")
                    .withHelp("decline invite", "decline a pending invite")
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
                if (member.isInGameOrLobby()) channel.send(member, "${member.user.asMention}, You're already in game! You can't create another game!")
                else {
                    channel.selectFromList(member, "Which type of game would you like to create?", GameType.values().map { it.readable }.toMutableList(), {
                        selected ->
                        val gameType = GameType.values()[selected]
                        if (guild.hasGameType(gameType) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
                            channel.send(member, "There can only be one of this type of game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at " +
                                    "https://ardentbot.com/support_us to start more than one game per type at a time**")
                        } else {
                            when (gameType) {
                                GameType.COINFLIP -> {
                                    channel.selectFromList(member, "Would you like this game of ${gameType.readable} to be open to everyone to join?", mutableListOf("Yes", "No"), {
                                        public ->
                                        val isPublic = public == 0
                                        channel.send(member, "How many players would you like in this game? Type `none` to set the limit as 999 (effectively no limit)")
                                        waiter.waitForMessage(Settings(member.user.id, channel.id, guild.id), {
                                            playerCount ->
                                            val count = playerCount.content.toIntOrNull() ?: 999
                                            val game = CoinflipGame(channel, member.id(), count, isPublic)
                                            gamesInLobby.add(game)

                                        })
                                    })
                                }
                                GameType.BLACKJACK -> {
                                    channel.send(member, "This is a solo game; it'll start in a second!")
                                    BlackjackGame(channel, member.id(), 1, false).startEvent()
                                }
                            }
                            // TODO("Fill in the other games")

                        }
                    })
                }
            }
            "list" -> {
                val embed = embed("Games in Lobby", member)
                val builder = StringBuilder()
                        .append("**Red means that the game is private, Green that it's public and anyone can join**")
                if (gamesInLobby.isEmpty()) channel.send(member, "\n\nThere are no games in lobby right now. You can start one by typing **${guild.getPrefix()}minigames create**")
                else {
                    gamesInLobby.forEach {
                        builder.append("\n\n ")
                        if (it.isPublic) builder.append(Emoji.LARGE_GREEN_CIRCLE)
                        else builder.append(Emoji.LARGE_RED_CIRCLE)
                        builder.append("  **${it.type.readable}** [**${it.players.size}** / **${it.playerCount}**] created by __${it.creator.toUser()!!.withDiscrim()}__ | ${it.players.toUsers()}")
                    }
                    builder.append("\n\n__Take Note__: You can run only one game of each type at a time in this server")
                    channel.send(member, embed.setDescription(builder.toString()))
                }
            }
            "cancel" -> {
                gamesInLobby.forEach { game ->
                    if (game.creator == member.id()) {
                        channel.send(member, "${Emoji.HEAVY_EXCLAMATION_MARK_SYMBOL}" +
                                "Are you sure you want to cancel your __${game.type.readable}__ game? Type **yes** if so or **no** if you're not sure.\n" +
                                "Current players in lobby: *${game.players.toUsers()}*")
                        waiter.waitForMessage(Settings(member.id(), channel.id, guild.id), { message ->
                            if (message.content.startsWith("ye", true)) {
                                game.cancel(member)
                            } else channel.send(member, "${Emoji.BALLOT_BOX_WITH_CHECK} I'll keep the game in lobby")
                        })
                        return
                    }
                }
                channel.send(member, "You're not the creator of a game that's in lobby! ${Emoji.NO_ENTRY_SIGN}")
            }
            "forcestart" -> {
                gamesInLobby.forEach { game ->
                    if (game.creator == member.id() && game.channel.guild == guild) {
                        if (game.players.size == 1 && game.type != GameType.BLACKJACK) channel.send(member, "You can't force start a game with only **1** person!")
                        else {
                            game.startEvent()
                        }
                        return
                    }
                }
                channel.send(member, "You're not the creator of a game that's in lobby! ${Emoji.NO_ENTRY_SIGN}")
            }
            "join" -> {
                if (arguments.size == 2) {
                    val id = arguments[1].replace("#", "").toIntOrNull()
                    if (id == null) {
                        channel.send(member, "You need to include a Game ID! Example: **${guild.getPrefix()}minigames join #123456**")
                        return
                    }
                    gamesInLobby.forEach { game ->
                        if (game.channel.guild == guild) {
                            if (member.isInGameOrLobby()) channel.send(member, "You can't join another game! You must leave the game you're currently in first")
                            else {
                                if (game.isPublic) {
                                    game.players.add(member.id())
                                    channel.send(member, "**${member.withDiscrim()}** has joined **${game.creator.toUser()!!.withDiscrim()}**'s game of ${game.type.readable}\n" +
                                            "Players in lobby: *${game.players.toUsers()}*")
                                } else {
                                    if (invites.containsKey(member.id()) && invites[member.id()]!!.gameId == game.gameId) {
                                        invites.remove(member.id())
                                        game.players.add(member.id())
                                        channel.send(member, "**${member.withDiscrim()}** has joined **${game.creator.toUser()!!.withDiscrim()}**'s *private* game of ${game.type.readable}\n" +
                                                "Players in lobby: *${game.players.toUsers()}*")
                                    } else channel.send(member, "You must be invited by the creator of this game to join this __private__ game!")
                                }
                            }
                            return
                        }
                    }
                    channel.send(member, "There's not a game in lobby with the ID of **#$id**")
                } else channel.send(member, "You need to include a Game ID! Example: **${guild.getPrefix()}minigames join #123456**")
            }
            "leave" -> {
                gamesInLobby.forEach { game ->
                    if (game.creator == member.id() && game.channel.guild == guild) {
                        channel.send(member, "You can't leave the game that you've started! If you want to cancel the game, type **${guild.getPrefix()}minigames " +
                                "cancel**")
                        return
                    } else if (game.players.contains(member.id())) {
                        game.players.remove(member.id())
                        channel.send(member, "${member.asMention}, you successfully left **${game.creator.toUser()!!.withDiscrim()}**'s game")
                        return
                    }
                }
                channel.send(member, "You're not in a game lobby!")
            }
            "invite" -> {
                gamesInLobby.forEach { game ->
                    if (game.creator == member.id() && game.channel.guild == guild) {
                        if (game.isPublic) {
                            channel.send(member, "You don't need to invite people to a public game, as everyone can join")
                            return
                        }
                        val mentionedUsers = event.message.mentionedUsers
                        if (mentionedUsers.size == 0) channel.send(member, "You need to mention at least one member to invite them")
                        else {
                            mentionedUsers.forEach { toInvite ->
                                if (invites.containsKey(toInvite.id)) channel.send(member, "You can't invite a member who already has a pending invite!")
                                else if (toInvite.isInGameOrLobby()) channel.send(member, "This person is already in a lobby or ingame!")
                                else {
                                    invites.put(toInvite.id, game)
                                    channel.send(member, "${toInvite.asMention}, you're being invited by ${member.asMention} to join a __${if (game.isPublic) "public" else "private"}__ game of " +
                                            "**${game.type.readable}**! Type *${guild.getPrefix()}minigames join #${game.gameId}* to accept this invite and join the game " +
                                            "or decline by typing *${guild.getPrefix()}minigames decline*")
                                    val delay = 45
                                    inviteManager.schedule({
                                        if (invites.containsKey(toInvite.id)) {
                                            channel.send(member, "${toInvite.asMention}, your invite to **${game.creator.toUser()!!.withDiscrim()}**'s game has expired after $delay seconds.")
                                            invites.remove(toInvite.id)
                                        }
                                    }, delay.toLong(), TimeUnit.SECONDS)
                                }
                            }
                        }
                        return
                    }
                }
                channel.send(member, "You're not the creator of a game that's in lobby! ${Emoji.NO_ENTRY_SIGN}")
            }
            "decline" -> {
                if (invites.containsKey(member.id())) {
                    val game = invites[member.id()]!!
                    channel.send(member, "${member.asMention} declined an invite to **${game.creator.toUser()!!.withDiscrim()}**'s game of **${game.type.readable}**")
                    invites.remove(member.id())
                } else channel.send(member, "You don't have a pending invite to decline!")
            }
        }
    }
}