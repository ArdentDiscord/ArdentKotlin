package commands.games

import events.Category
import events.Command
import main.test
import main.waiter
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.entities.User
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


val invites = ConcurrentHashMap<String, Game>()
val questions = mutableListOf<TriviaQuestion>()

class BlackjackGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.BLACKJACK, channel, creator, playerCount, isPublic) {
    private val roundResults = mutableListOf<Round>()
    override fun onStart() {
        val user = players.map { it.toUser()!! }.toList()[0]
        doRound(user)
    }

    fun doRound(user: User) {
        val playerData = user.getData()
        if (playerData.gold == 0.toDouble()) {
            playerData.gold += 15
            playerData.update()
            channel.send("Because you were broke, the Blackjack Gods took pity on you and gave you **15.0** gold to bet with".tr(channel.guild))
        }
        channel.send("How much would you like to bet, {0}? You current have a balance of **{1}** gold".tr(channel.guild, user.asMention, playerData.gold))
        waiter.waitForMessage(Settings(user.id, channel.id, channel.guild.id), { message ->
            val bet = message.rawContent.toIntOrNull()
            if (bet == null || bet <= 0 || bet > playerData.gold) {
                channel.send("You specified an invalid amount.. resetting the round".tr(channel.guild))
                doRound(user)
            } else {
                val dealerHand = Hand(true).blackjackPlus(2)
                val userHand = Hand().blackjackPlus(1)
                display(dealerHand, userHand, "You've been dealt 1 card.".tr(channel.guild) + " " + "The dealer's second card is hidden.".tr(channel.guild) + " " + "The goal is to get as close as possible to **21**. Type `" + "hit".tr(channel.guild) + "` if you'd like to get another card or `" + "stay".tr(channel.guild) + "` to stay at your current amount".tr(channel.guild))
                wait(bet.toDouble(), dealerHand, userHand, user)
            }
        }, { cancel(user) }, silentExpiration = true)
    }

    fun wait(bet: Double, dealerHand: Hand, userHand: Hand, user: User) {
        waiter.waitForMessage(Settings(user.id, channel.id), { response ->
            when (response.content) {
                "hit".tr(channel.guild) -> {
                    userHand.blackjackPlus(1)
                    if (userHand.value() >= 21) displayRoundScore(bet, dealerHand, userHand, user)
                    else {
                        display(dealerHand, userHand, "The dealer's second card is hidden.".tr(channel.guild) + " " + "The goal is to get as close as possible to **21**. Type `" + "hit".tr(channel.guild) + "` if you'd like to get another card or `" + "stay".tr(channel.guild) + "` to stay at your current amount".tr(channel.guild))
                        wait(bet, dealerHand, userHand, user)
                    }
                }
                "stay".tr(channel.guild) -> {
                    channel.send("Generating dealer cards...".tr(channel.guild))
                    while (dealerHand.value() < 17) dealerHand.blackjackPlus(1)
                    displayRoundScore(bet, dealerHand, userHand, user)
                }
                else -> {
                    channel.send("You specified an invalid response - please retry".tr(channel.guild))
                    wait(bet, dealerHand, userHand, user)
                    return@waitForMessage
                }
            }
        }, {
            channel.send("{0}, you didn't specify a response and thus lost!".tr(channel.guild, user.asMention))
            while (userHand.value() < 21) userHand.blackjackPlus(1)
            displayRoundScore(bet, dealerHand, userHand, user)
            cancel(user)
        }, 15, TimeUnit.SECONDS, silentExpiration = true)
    }

    fun display(dealerHand: Hand, userHand: Hand, message: String, end: Boolean = false) {
        val embed = channel.guild.selfMember.embed("Blackjack | Hand Values".tr(channel.guild))
                .setDescription(message)
                .addField("Your Hand".tr(channel.guild), "$userHand (${userHand.value()})", true)
                .addBlankField(true)
        if (dealerHand.cards.size == 2 && !end) embed.addField("Dealer's Hand".tr(channel.guild), "$dealerHand (${dealerHand.cards[0].value.representation} + ?)", true)
        else embed.addField("Dealer's Hand".tr(channel.guild), "$dealerHand (${dealerHand.value()})", true)
        channel.send(embed)
    }

    fun displayRoundScore(bet: Double, dealerHand: Hand, userHand: Hand, user: User) {
        val result = when {
            userHand.value() > 21 -> Result.LOST
            dealerHand.value() > 21 -> Result.WON
            userHand.value() == dealerHand.value() -> Result.TIED
            userHand.value() > dealerHand.value() -> Result.WON
            else -> Result.LOST
        }
        val playerData = user.getData()
        val message = when (result) {
            Result.LOST -> {
                playerData.gold -= bet
                playerData.update()
                "**Sorry, you lost {0} gold!**".tr(channel.guild, bet)

            }
            Result.WON -> {
                playerData.gold += bet
                playerData.update()
                "**Congratulations, you won {0} gold!**".tr(channel.guild, bet)
            }
            Result.TIED -> "**You tied and didn't lose the {0} you bet!**".tr(channel.guild, bet)
        }
        roundResults.add(Round(result, userHand.end(), dealerHand.end(), bet))
        display(dealerHand, userHand, message, true)

        channel.sendMessage("Would you like to go again? Type `" + "yes".tr(channel.guild) + "` to replay or `" + "no".tr(channel.guild) + "` to end the game".tr(channel.guild)).queueAfter(2, TimeUnit.SECONDS)
        waiter.waitForMessage(Settings(user.id, channel.id), { response ->
            when (response.content) {
                "yes".tr(channel.guild) -> doRound(user)
                else -> {
                    val gameData = GameDataBlackjack(gameId, creator, startTime!!, roundResults)
                    cleanup(gameData)
                }
            }
        }, {
            val gameData = GameDataBlackjack(gameId, creator, startTime!!, roundResults)
            cleanup(gameData)
        }, silentExpiration = true)

    }

    enum class Result {
        WON, LOST, TIED;

        override fun toString(): String {
            return when (this) {
                BlackjackGame.Result.WON -> "<font color=\"green\">Won</font>"
                BlackjackGame.Result.LOST -> "<font color=\"red\">Lost</font>"
                BlackjackGame.Result.TIED -> "<font color=\"orange\">Tied</font>"
            }
        }
    }

    class Round(val won: Result, val userHand: Hand, val dealerHand: Hand, val bet: Double)
    class Hand(val dealer: Boolean = false, val cards: MutableList<Card> = mutableListOf(), var end: Boolean = false) {
        val random = Random()
        fun blackjackPlus(cardAmount: Int): Hand {
            (1..cardAmount).forEach { _ ->
                cards.add(generate())
                if (value() > 21) cards.forEach { if (it.value == BlackjackValue.ACE) it.value.representation = 1 }
            }
            return this
        }

        fun end(): Hand {
            end = true
            return this
        }

        fun generate(): Card {
            val card = Card(Suit.values()[random.nextInt(4)], BlackjackValue.values()[random.nextInt(13)])
            if (cards.contains(card)) return generate()
            else return card
        }

        override fun toString(): String {
            if (cards.size == 2 && dealer && !end) return "${cards[0]}, ?"
            else return cards.map { it.toString() }.stringify()
        }

        fun value(): Int {
            var value = 0
            cards.forEach { value += it.value.representation }
            return value
        }
    }

    data class Card(val suit: Suit, val value: BlackjackValue) {

        override fun toString(): String {
            return "$value$suit"
        }
    }

    enum class Suit {
        HEART, SPADE, CLUB, DIAMOND;

        override fun toString(): String {
            return when (this) {
                HEART -> "♥"
                SPADE -> "♠"
                CLUB -> "♣"
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
            return when (this) {
                ACE -> "A"
                KING -> "K"
                QUEEN -> "Q"
                JACK -> "J"
                else -> representation.toString()
            }
        }
    }
}

class TriviaGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.TRIVIA, channel, creator, playerCount, isPublic) {
    val ardent = channel.guild.selfMember!!
    private val rounds = mutableListOf<Round>()
    private val roundTotal = if (test) 4 else 15
    private val questions = roundTotal.getTrivia()
    override fun onStart() {
        doRound(0, questions)
    }

    private fun doRound(currentRound: Int, questions: List<TriviaQuestion>) {
        if (currentRound == roundTotal) {
            val sc = getScores().first.sort(true) as MutableMap<String, Int>
            val winner = sc.toList()[0]
            val winnerUser = winner.first.toUser()!!
            channel.send("Congrats to {0} for winning with **{1}** points! They'll receive that amount in gold as a prize!".tr(channel.guild, winnerUser.asMention, winner.second) + "\n" +
                    "**Cleaning game up..**")
            val data = winnerUser.getData()
            data.gold += winner.second
            data.update()
            cleanup(GameDataTrivia(gameId, creator, startTime!!, winner.first, players.without(winner.first), sc, rounds))
        } else {
            if (currentRound == (roundTotal - 3)) channel.send("${Emoji.INFORMATION_SOURCE} " + "There are only **3** rounds left!".tr(channel.guild))
            val question = questions[currentRound]
            channel.send(ardent.embed("Trivia | Question {0} of {1}".tr(channel.guild, currentRound + 1, roundTotal))
                    .appendDescription("**${question.category}**\n" +
                            "${question.question}\n" +
                            "           " + "**{0}** points".tr(channel.guild, question.value)))
            var guessed = false
            waiter.gameChannelWait(channel.id, { response ->
                if (players.contains(response.author.id) && !guessed) {
                    if (question.answers.containsEq(response.rawContent.toLowerCase())) {
                        channel.send("{0} guessed the correct answer and got **{1}** points!".tr(channel.guild, response.author.asMention, question.value))
                        guessed = true
                        endRound(players.without(response.author.id), question, currentRound, questions, response.author.id)
                        return@gameChannelWait
                    }
                }
            }, {
                if (!guessed) {
                    channel.send("No one got it right! The correct answer was **{0}**".tr(channel.guild, question.answers[0]))
                    endRound(players, question, currentRound, questions)
                    return@gameChannelWait
                }
            }, time = 20)
        }
    }

    private fun endRound(losers: MutableList<String>, question: TriviaQuestion, currentRound: Int, questions: List<TriviaQuestion>, vararg winner: String) {
        rounds.add(Round(winner, losers, question))
        if ((currentRound + 1) % 3 == 0) showScores(currentRound)
        after({ doRound(currentRound + 1, questions) }, 2)
    }

    private fun showScores(currentRound: Int) {
        val embed = ardent.embed("Trivia Scores | Round {0}".tr(channel.guild, currentRound + 1))
        val scores = getScores()
        if (scores.second.size == 0) embed.setDescription("No one has scored yet!")
        else scores.first.forEachIndexed { index, u, score -> embed.appendDescription("[**${index + 1}**]: **${u.toUser()!!.asMention}** *($score points)*\n") }
        channel.send(embed)
    }


    private fun getScores(): Pair<MutableMap<String, Int /* Point values */>, HashMap<String, Int /* Amt of Qs correct */>> {
        val points = hashMapOf<String, Int>()
        val questions = hashMapOf<String, Int>()
        rounds.forEach { (winners, _, q) ->
            if (winners.isNotEmpty()) {
                winners.forEach { winner ->
                    if (points.containsKey(winner)) points.replace(winner, points[winner]!! + q.value)
                    else points.put(winner, q.value)
                    questions.incrementValue(winner)
                }
            }
        }
        players.forEach { points.putIfAbsent(it, 0) }
        return Pair(points.sort(true) as MutableMap<String, Int>, questions)
    }

    data class Round(val winners: Array<out String>, val losers: MutableList<String>, val question: TriviaQuestion) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Round
            if (!Arrays.equals(winners, other.winners)) return false
            if (losers != other.losers) return false
            if (question != other.question) return false
            return true
        }

        override fun hashCode(): Int {
            var result = winners.let { Arrays.hashCode(it) } ?: 0
            result = 31 * result + losers.hashCode()
            result = 31 * result + question.hashCode()
            return result
        }
    }
}

class BetGame(channel: TextChannel, creator: String) : Game(GameType.BETTING, channel, creator, 1, false) {
    val rounds = mutableListOf<Round>()
    override fun onStart() {
        doRound(creator.toUser()!!)
    }

    fun doRound(user: User) {
        val data = user.getData()
        channel.send("How much would you like to bet? You current have **{0} gold**. Type the amount below. You can also bet a **percentage** of your net worth, e.g. *40%*".tr(channel.guild, data.gold))
        waiter.waitForMessage(Settings(creator, channel.id, channel.guild.id), { message ->
            val content = message.content
            if (content.equals("cancel", true)) {
                channel.send("Cancelling game...".tr(channel.guild))
                cancel(user)
            } else {
                val bet = if (content.contains("%")) content.removeSuffix("%").toDoubleOrNull()?.div(100)?.times(data.gold)?.toInt() else content.toIntOrNull()
                if (bet != null) {
                    if (bet > data.gold || bet <= 0) {
                        channel.send("You specified an invalid bet amount! Please retry or type `cancel` to cancel the game".tr(channel.guild))
                        doRound(user)
                    } else {
                        channel.selectFromList(channel.guild.getMember(user), "What color will the next card I draw be?", mutableListOf("Black".tr(channel.guild), "Red".tr(channel.guild)), { selection, _ ->
                            val suit = BlackjackGame.Hand(false, end = false).generate().suit
                            val won = when (suit) {
                                BlackjackGame.Suit.HEART, BlackjackGame.Suit.DIAMOND -> selection == 1
                                else -> selection == 0
                            }
                            if (won) {
                                data.gold += bet
                                channel.send("Congrats, you won - the suit was {0}! I've added **{1} gold** to your profile - new balance: **{2} gold**".tr(channel.guild, suit, bet, data.gold.format()))
                            } else {
                                data.gold -= bet
                                channel.send("Sorry, you lost - the suit was {0} :( I've removed **{1} gold** from your profile - new balance: **{2} gold**".tr(channel.guild, suit, bet, data.gold.format()))
                            }
                            data.update()
                            rounds.add(Round(won, bet.toDouble(), suit))
                            channel.send("Would you like to go again? Type `yes` if so or `no` to end the game".tr(channel.guild))
                            waiter.waitForMessage(Settings(creator, channel.id, channel.guild.id), { continueGameMessage ->
                                if (continueGameMessage.rawContent.startsWith("ye", true)) doRound(user)
                                else {
                                    channel.send("Ending the game and inserting data into the database..".tr(channel.guild))
                                    val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                    cleanup(gameData)
                                }
                            }, {
                                channel.send("Ending the game and inserting data into the database..".tr(channel.guild))
                                val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                cleanup(gameData)
                            })

                        }, failure = {
                            if (rounds.size == 0) {
                                channel.send("Invalid response... Cancelling game now".tr(channel.guild))
                                cancel(user)
                            } else {
                                channel.send("Invalid response... ending the game and inserting data into the database..".tr(channel.guild))
                                val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                cleanup(gameData)
                            }
                        })
                    }
                } else {
                    if (rounds.size == 0) {
                        channel.send("Invalid bet amount... Cancelling game now".tr(channel.guild))
                        cancel(user)
                    } else {
                        channel.send("Invalid bet amount... ending the game and inserting data into the database..".tr(channel.guild))
                        val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                        cleanup(gameData)
                    }
                }
            }
        }, { cancel(user) })
    }

    data class Round(val won: Boolean, val betAmount: Double, val suit: BlackjackGame.Suit)
}

class TicTacToeGame(channel: TextChannel, creator: String) : Game(GameType.TIC_TAC_TOE, channel, creator, 2, false) {
    override fun onStart() {
        doRound(Board(players[0], players[1]), players[0])
    }

    fun doRound(board: Board, player: String, cancelIfExpire: Boolean = false) {
        val member = channel.guild.getMemberById(player)
        channel.sendMessage(member.embed("Tic Tac Toe | Ardent".tr(channel.guild), Color.WHITE)
                .appendDescription("{0}, you're up! Click where you want to place".tr(channel.guild, member.asMention) + "\n\n")
                .appendDescription(board.toString()).build()).queue { message ->
            message.addReaction(Emoji.NORTH_WEST_ARROW.symbol).queue()
            message.addReaction(Emoji.UPWARDS_BLACK_ARROW.symbol).queue()
            message.addReaction(Emoji.NORTHEAST_ARROW.symbol).queue()

            message.addReaction(Emoji.LEFTWARDS_BLACK_ARROW.symbol).queue()
            message.addReaction(Emoji.SMALL_ORANGE_DIAMOND.symbol).queue()
            message.addReaction(Emoji.BLACK_RIGHTWARDS_ARROW.symbol).queue()

            message.addReaction(Emoji.SOUTH_WEST_ARROW.symbol).queue()
            message.addReaction(Emoji.DOWNWARDS_BLACK_ARROW.symbol).queue()
            message.addReaction(Emoji.SOUTHEAST_ARROW.symbol).queue()

            waiter.waitForReaction(Settings(player, channel.id, channel.guild.id, message.id), { messageReaction ->
                val place: Int? = when (messageReaction.emote.name) {
                    Emoji.NORTH_WEST_ARROW.symbol -> 1
                    Emoji.UPWARDS_BLACK_ARROW.symbol -> 2
                    Emoji.NORTHEAST_ARROW.symbol -> 3
                    Emoji.LEFTWARDS_BLACK_ARROW.symbol -> 4
                    Emoji.SMALL_ORANGE_DIAMOND.symbol -> 5
                    Emoji.BLACK_RIGHTWARDS_ARROW.symbol -> 6
                    Emoji.SOUTH_WEST_ARROW.symbol -> 7
                    Emoji.DOWNWARDS_BLACK_ARROW.symbol -> 8
                    Emoji.SOUTHEAST_ARROW.symbol -> 9
                    else -> null
                }
                if (place == null) {
                    channel.send("{0} reacted with an invalid place! They'll have **one** more try or else I'll cancel the game..".tr(channel.guild, member.asMention))
                    if (!cancelIfExpire) doRound(board, player, true)
                    else cancel(member)
                } else {
                    if (board.put(place, player == players[0])) {
                        val winner = board.checkWin()
                        if (winner == null) {
                            if (board.spacesLeft().size > 0) doRound(board, if (player == players[0]) players[1] else players[0])
                            else {
                                channel.sendMessage(member.embed("Tic Tac Toe | Ardent".tr(channel.guild), Color.WHITE)
                                        .appendDescription("Game Over! Sadly, you guys **tied** and nobody won".tr(channel.guild) + " :(\n\n")
                                        .appendDescription(board.toString()).build()).queue()
                                doCleanup(board, null)
                            }
                        } else {
                            channel.sendMessage(member.embed("Tic Tac Toe | Ardent".tr(channel.guild), Color.WHITE)
                                    .appendDescription("Game Over! The amazing {0} has won!".tr(channel.guild, winner.toUser()?.asMention ?: "Unknown") + "\n\n")
                                    .appendDescription(board.toString()).build()).queue()
                            doCleanup(board, winner)
                        }
                    } else {
                        channel.send("{0} reacted with an invalid place! They'll have **one** more try or else I'll cancel the game..".tr(channel.guild, member.asMention))
                        if (!cancelIfExpire) doRound(board, player, true)
                        else cancel(member)
                    }
                }
                message.delete().queue()
            }, {
                if (cancelIfExpire) cancel(member)
                else {
                    channel.send("No input was received from {0}... They have **45** more seconds to play or the game will be automatically cancelled".tr(channel.guild, member.asMention))
                    doRound(board, player, true)
                }
                message.delete().queue()
            }, 45)
        }
    }

    fun doCleanup(board: Board, winner: String?) {
        Thread.sleep(2000)
        cleanup(GameDataTicTacToe(gameId, creator, startTime!!, players[0], players[1], winner, board.toString()))
        val creatorMember = channel.guild.getMemberById(creator)
        channel.selectFromList(creatorMember, "Do you want to start another game?", mutableListOf("Yes".tr(channel.guild), "No".tr(channel.guild)), { selection, selectionMessage ->
            if (selection == 0) {
                channel.send("**Creating game now..**".tr(channel.guild))
                val newGame = TicTacToeGame(channel, creatorMember.id())
                gamesInLobby.add(newGame)
            } else channel.send("You selected not to play another game. **Cleaning up resources..**".tr(channel.guild))
            selectionMessage.delete().queue()
        }, footerText = "Only the creator of the game can do this selection", failure = {})
    }

    data class Board(val playerOne: String, val playerTwo: String, val tiles: Array<String?> = Array(9, { null })) {
        fun put(space: Int, isPlayerOne: Boolean): Boolean {
            return if (space !in 1..9 || tiles[space - 1] != null) false
            else {
                tiles[space - 1] = if (isPlayerOne) playerOne else playerTwo
                true
            }
        }

        fun checkWin(): String? {
            (0..2)
                    .filter { tiles[0 + 3 * it] != null && tiles[0 + 3 * it] == tiles[1 + 3 * it] && tiles[0 + 3 * it] == tiles[2 + 3 * it] }
                    .forEach { return tiles[0 + 3 * it] }
            (0..2)
                    .filter { tiles[it] != null && tiles[it] == tiles[it + 3] && tiles[it] == tiles[it + 6] }
                    .forEach { return tiles[it] }
            if (tiles[0] != null && tiles[0] == tiles[4] && tiles[0] == tiles[8]) return tiles[0]
            if (tiles[2] != null && tiles[2] == tiles[4] && tiles[2] == tiles[6]) return tiles[2]
            return null
        }

        fun spacesLeft(): MutableList<Int> {
            val list = mutableListOf<Int>()
            tiles.forEachIndexed { index, s -> if (s == null) list.add(index) }
            return list
        }

        override fun toString(): String {
            val builder = StringBuilder()
                    .append("▬▬▬▬▬▬▬▬▬▬\n▐ ")
            tiles.forEachIndexed { index, s ->
                builder.append(when (s) {
                    playerOne -> "❌"
                    playerTwo -> "⭕"
                    else -> "⬛"
                })
                if ((index + 1) % 3 == 0) {
                    builder.append("▐ \n▬▬▬▬▬▬▬▬▬▬\n")
                    if (index != 8) builder.append("▐ ")
                } else builder.append(" ║ ")
            }
            return builder.toString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as Board
            if (playerOne != other.playerOne) return false
            if (playerTwo != other.playerTwo) return false
            if (!Arrays.equals(tiles, other.tiles)) return false
            return true
        }

        override fun hashCode(): Int {
            var result = playerOne.hashCode()
            result = 31 * result + playerTwo.hashCode()
            result = 31 * result + Arrays.hashCode(tiles)
            return result
        }
    }

}

class Connect4Game(channel: TextChannel, creator: String) : Game(GameType.CONNECT_4, channel, creator, 2, false) {
    override fun onStart() {
        val game = GameBoard(players[0], players[1])
        doRound(game, channel.guild.getMemberById(players[0]))
    }

    private fun doRound(game: GameBoard, player: Member, cancelIfExpired: Boolean = false) {
        if (game.full()) {
            channel.send("All tiles have been placed without a winner (lol). Therefore, I choose {0} as the winner! (They will not get any gold for winning this game)".tr(channel.guild, player.asMention))
            val embed = channel.guild.selfMember.embed("Connect 4 | Results", Color.BLUE)
            embed.appendDescription("{0} has won (technically) in a tied game".tr(channel.guild, player.asMention) + "\n$game")
            channel.send(embed)
            cleanup(GameDataConnect4(gameId, creator, startTime!!, player.id(), if (player.id() == players[0]) players[1] else players[0], game.toString()))
        } else {
            val embed = channel.guild.selfMember.embed("Connect 4 Game Board", Color.BLUE)
            embed.appendDescription("{0}, it's your turn {1} - React to place your chip".tr(channel.guild, player.asMention, if (players[0] == player.id()) "\uD83D\uDD34" else "\uD83D\uDD35") + "\n")
            embed.appendDescription(game.toString())
            channel.sendMessage(embed.build()).queue({ message ->
                message.addReaction(Emoji.KEYCAP_DIGIT_ONE.symbol).queue()
                message.addReaction(Emoji.KEYCAP_DIGIT_TWO.symbol).queue()
                message.addReaction(Emoji.KEYCAP_DIGIT_THREE.symbol).queue()
                message.addReaction(Emoji.KEYCAP_DIGIT_FOUR.symbol).queue()
                message.addReaction(Emoji.KEYCAP_DIGIT_FIVE.symbol).queue()
                message.addReaction(Emoji.KEYCAP_DIGIT_SIX.symbol).queue()
                message.addReaction(Emoji.KEYCAP_DIGIT_SEVEN.symbol).queue()
                waiter.waitForReaction(Settings(player.id(), channel.id), { messageReaction ->
                    when (messageReaction.emote.name) {
                        Emoji.KEYCAP_DIGIT_ONE.symbol -> place(0, game, player)
                        Emoji.KEYCAP_DIGIT_TWO.symbol -> place(1, game, player)
                        Emoji.KEYCAP_DIGIT_THREE.symbol -> place(2, game, player)
                        Emoji.KEYCAP_DIGIT_FOUR.symbol -> place(3, game, player)
                        Emoji.KEYCAP_DIGIT_FIVE.symbol -> place(4, game, player)
                        Emoji.KEYCAP_DIGIT_SIX.symbol -> place(5, game, player)
                        Emoji.KEYCAP_DIGIT_SEVEN.symbol -> place(6, game, player)
                        else -> {
                            channel.send("An unidentified reaction was received from {0}. Retrying the round... If they don't respond this time, the game will be cancelled".tr(channel.guild, player.asMention))
                            doRound(game, player, true)
                        }
                    }
                    message.delete().queue()
                }, {
                    if (cancelIfExpired) cancel(creator.toUser()!!)
                    else {
                        channel.send("No input was received from {0}. Retrying the round... If they don't respond this time, the game will be cancelled".tr(channel.guild, player.asMention))
                        message.delete().queue()
                        doRound(game, player, true)
                    }
                }, 40, TimeUnit.SECONDS, silentExpiration = true)
            })
        }
    }

    private fun place(column: Int, game: GameBoard, player: Member) {
        val tile = game.put(column, player.id() == players[0])
        if (tile == null) {
            channel.send("{0}, you specified an invalid column: Please retry..".tr(channel.guild, player.asMention))
            doRound(game, player, false)
        } else {
            val winnerId = game.checkWin(tile)
            if (winnerId == null) doRound(game, if (player.id() == players[0]) channel.guild.getMemberById(players[1]) else channel.guild.getMemberById(players[0]))
            else {
                val winner = channel.guild.getMemberById(winnerId)
                val embed = channel.guild.selfMember.embed("Connect 4 | Results".tr(channel.guild), Color.BLUE)
                embed.appendDescription("{0} has won and got **500 gold**!".tr(channel.guild, winner.asMention) + "\n")
                embed.appendDescription(game.toString())
                channel.send(embed)
                val data = winner.data()
                data.gold += 500
                data.update()
                cleanup(GameDataConnect4(gameId, creator, startTime!!, winner.id(), if (winnerId == players[0]) players[1] else players[0], game.toString()))
            }
        }
    }

    data class Column(val game: GameBoard, val number: Int, val tiles: Array<Tile> = Array(6, { Tile(game, number, it) })) {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false

            other as Column

            if (number != other.number) return false
            if (!Arrays.equals(tiles, other.tiles)) return false

            return true
        }

        override fun hashCode(): Int {
            var result = number
            result = 31 * result + Arrays.hashCode(tiles)
            return result
        }
    }

    data class Tile(val game: GameBoard, val x: Int, val y: Int, var possessor: String? = null) {
        override fun toString(): String {
            return when (possessor) {
                null -> "⚪"
                game.playerOne -> "\uD83D\uDD34"
                else -> "\uD83D\uDD35"
            }
        }
    }

    data class GameBoard(val playerOne: String, val playerTwo: String, val state: GameState = GameState.WAITING_PLAYER_ONE) {
        private val grid: List<Column> = listOf(Column(this, 0), Column(this, 1), Column(this, 2), Column(this, 3), Column(this, 4), Column(this, 5), Column(this, 6))

        fun full(): Boolean {
            grid.forEach { grid -> grid.tiles.forEach { tile -> if (tile.possessor == null) return false } }
            return true
        }

        fun put(column: Int, playerOne: Boolean): Tile? {
            if (column !in 0..6) return null
            grid[column].tiles.forEach { tile ->
                if (tile.possessor == null) {
                    tile.possessor = if (playerOne) this.playerOne else playerTwo
                    return tile
                }
            }
            return null
        }

        fun getRow(index: Int): ArrayList<Tile> {
            if (index !in 0..5) throw IllegalArgumentException("Row does not exist at index $index")
            else {
                val row = arrayListOf<Tile>()
                grid.forEach { column -> row.add(column.tiles[index]) }
                return row
            }
        }

        fun getTile(x: Int, y: Int): Tile? {
            return grid.getOrNull(x)?.tiles?.getOrNull(y)
        }

        fun getDiagonal(tile: Tile?, left: Boolean): MutableList<Tile> {
            if (tile == null) return mutableListOf()
            var xStart = tile.x
            var yStart = tile.y
            while (yStart > 0 && if (left) xStart > 0 else xStart < 6) {
                if (left) xStart-- else xStart++
                yStart--
            }
            val tiles = mutableListOf<Tile>()
            var found = true
            while (found) {
                val t = getTile(xStart, yStart)
                if (t == null) found = false
                else {
                    tiles.add(t)
                    if (left) xStart++ else xStart--
                    yStart++
                }
            }
            return tiles
        }

        fun checkWin(tile: Tile): String? {
            val row = getRow(tile.y)
            var counter = 0
            var currentOwner: String? = null
            row.forEach { t ->
                if (currentOwner == t.possessor && currentOwner != null) counter++ else {
                    counter = 1
                    currentOwner = t.possessor
                }
                if (counter == 4) return currentOwner
            }
            counter = 0
            val column = grid[tile.x]
            column.tiles.forEach { t ->
                if (currentOwner == t.possessor && currentOwner != null) counter++ else {
                    counter = 1
                    currentOwner = t.possessor
                }
                if (counter == 4) return currentOwner
            }
            val diagonalLeft = diagonal(tile, true)
            if (diagonalLeft != null) return diagonalLeft
            val diagonalRight = diagonal(tile, false)
            if (diagonalRight != null) return diagonalRight
            return null
        }

        fun diagonal(tile: Tile, direction: Boolean /* True is left, false is right */): String? {
            val tiles = if (direction) getDiagonal(tile, true) else getDiagonal(tile, false)
            var counter = 0
            var currentOwner: String? = null
            tiles.forEach { t ->
                if (currentOwner == t.possessor && currentOwner != null) counter++ else {
                    counter = 1
                    currentOwner = t.possessor
                }
                if (counter == 4) return currentOwner
            }
            return null
        }

        override fun toString(): String {
            val builder = StringBuilder()
            builder.append(" ▬▬▬▬▬▬▬▬▬▬▬ ▬▬▬▬▬▬▬▬▬▬▬")
            builder.append("\n")
            for (rowValue in 5 downTo 0) {
                builder.append("▐ ")
                grid.forEachIndexed { index, it ->
                    builder.append(it.tiles[rowValue])
                    if (index < grid.size - 1) builder.append(" ║ ")
                }
                builder.append("▐\n")
                builder.append(" ▬▬▬▬▬▬▬▬▬▬▬ ▬▬▬▬▬▬▬▬▬▬▬")
                builder.append("\n")
            }
            return builder.toString()
        }
    }

    enum class GameState {
        WAITING_PLAYER_ONE, WAITING_PLAYER_TWO
    }
}


class SlotsGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.SLOTS, channel, creator, playerCount, isPublic) {
    private val rounds = mutableListOf<Round>()
    override fun onStart() {
        doRound(channel.guild.getMemberById(creator))
    }

    private fun doRound(member: Member) {
        val data = member.data()
        channel.send("How much would you like to bet? You currently have **{0}** gold".tr(channel.guild, data.gold))
        waiter.waitForMessage(Settings(member.id(), channel.id, channel.guild.id), { message ->
            val bet = message.rawContent.toIntOrNull()
            if (bet == null || bet <= 0 || bet > data.gold) {
                channel.send("You specified an invalid bet, please retry...".tr(channel.guild))
                doRound(member)
            } else {
                Thread.sleep(500)
                var slots = SlotsGame()
                if (!slots.won()) slots = SlotsGame()
                channel.send(member.embed("Slots Results".tr(channel.guild)).setDescription("${if (slots.won()) "Congrats, you won **{0}** gold".tr(channel.guild, bet) else "Darn, you lost **{0}** gold :(".tr(channel.guild, bet)}\n$slots)"))
                if (slots.won()) data.gold += bet
                else data.gold -= bet
                data.update()
                rounds.add(Round(bet, slots.won(), slots.toString().replace("\n", "<br />")))
                Thread.sleep(1500)
                channel.selectFromList(member, "Do you want to go again?", mutableListOf("Yes".tr(channel.guild), "No".tr(channel.guild)), { goAgainMessage, selectionMessage ->
                    if (goAgainMessage == 0) doRound(member)
                    else finish(member)
                    selectionMessage.delete().queue()
                }, failure = {
                    channel.send("You didn't respond in time, so I'll end the game now".tr(channel.guild))
                    finish(member)
                })
            }
        }, {
            channel.send("You didn't respond in time, so I'll end the game now".tr(channel.guild))
            finish(member)
        })
    }

    private fun finish(member: Member) {
        if (rounds.size == 0) cancel(member)
        else {
            cleanup(GameDataSlots(gameId, creator, startTime!!, rounds))
        }
    }

    data class SlotsGame(val slots: ArrayList<String> = arrayListOf()) {
        init {
            for (i in 1..9) {
                slots.add(when (random.nextInt(4)) {
                    0 -> Emoji.RED_APPLE
                    1 -> Emoji.BANANA
                    2 -> Emoji.FRENCH_FRIES
                    else -> Emoji.STRAWBERRY
                }.symbol)
            }
        }

        fun won(): Boolean {
            val emoji = slots[3]
            return slots[4] == emoji && slots[5] == emoji
        }

        override fun toString(): String {
            val builder = StringBuilder()
            slots.forEachIndexed { index, s ->
                if (index % 3 == 0 && index != 0) builder.append("\n")
                builder.append("$s ")
                if (index == 5) builder.append(Emoji.LEFTWARDS_BLACK_ARROW.symbol)
            }
            return builder.toString()
        }

        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (javaClass != other?.javaClass) return false
            other as SlotsGame
            if (slots != other.slots) return false
            return true
        }

        override fun hashCode(): Int {
            return slots.hashCode()
        }
    }

    data class Round(val bet: Int, val won: Boolean, val game: String)
}

class BetCommand : Command(Category.GAMES, "bet", "bet some money - will you be lucky?") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val member = event.member
        if (member.isInGameOrLobby()) event.channel.send("{0}, You're already in game! You can't create another game!".tr(event, member.asMention))
        else BetGame(event.textChannel, member.id()).startEvent()
    }
}

class TriviaCommand : Command(Category.GAMES, "trivia", "start a trivia game") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val channel = event.textChannel
        val member = event.member
        if (member.isInGameOrLobby()) channel.send("{0}, You're already in game! You can't create another game!".tr(event, member.asMention))
        else if (event.guild.hasGameType(GameType.TRIVIA) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Trivia", "<https://ardentbot.com/patreon>"))
        } else {
            channel.selectFromList(member, "Would you like this game to be open to everyone to join?", mutableListOf("Yes".tr(event), "No".tr(event)), { public, _ ->
                val isPublic = public == 0
                channel.send("How many players would you like in this game? Type `none` to set the limit as 999 (effectively no limit)".tr(event))
                waiter.waitForMessage(Settings(member.user.id, channel.id, event.guild.id), { playerCount ->
                    val count = playerCount.content.toIntOrNull() ?: 999
                    if (count == 0) channel.send("Invalid number provided, cancelling setup".tr(event))
                    else {
                        val game = TriviaGame(channel, member.id(), count, isPublic)
                        gamesInLobby.add(game)
                    }
                })
            })
        }
    }

}

class SlotsCommand : Command(Category.GAMES, "slots", "start slots games") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val member = event.member
        val channel = event.textChannel
        if (member.isInGameOrLobby()) channel.send("{0}, You're already in game! You can't create another game!".tr(event, member.asMention))
        else if (event.guild.hasGameType(GameType.SLOTS) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Slots", "<https://ardentbot.com/patreon>"))
        } else {
            SlotsGame(channel, member.id(), 1, false).startEvent()
        }
    }
}

class BlackjackCommand : Command(Category.GAMES, "blackjack", "start games of blackjack") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val member = event.member
        val channel = event.textChannel
        if (member.isInGameOrLobby()) channel.send("{0}, You're already in game! You can't create another game!".tr(event, member.asMention))
        else if (event.guild.hasGameType(GameType.BLACKJACK) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Blackjack", "<https://ardentbot.com/patreon>"))
        } else {
            BlackjackGame(channel, member.id(), 1, false).startEvent()
        }
    }
}

class Connect4Command : Command(Category.GAMES, "connect4", "start connect 4 games - inside Discord!") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val member = event.member
        val channel = event.textChannel
        if (member.isInGameOrLobby()) channel.send("{0}, You're already in game! You can't create another game!".tr(event, member.asMention))
        else if (event.guild.hasGameType(GameType.CONNECT_4) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Connect 4", "<https://ardentbot.com/patreon>"))
        } else {
            val game = Connect4Game(channel, member.id())
            gamesInLobby.add(game)
        }

    }
}

class TicTacToeCommand : Command(Category.GAMES, "tictactoe", "start tic tac toe games - inside Discord!") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val member = event.member
        val channel = event.textChannel
        if (member.isInGameOrLobby()) channel.send("{0}, You're already in game! You can't create another game!".tr(event, member.asMention))
        else if (event.guild.hasGameType(GameType.TIC_TAC_TOE) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one *{0}* game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at {1} to start more than one game per type at a time**".tr(event, "Tic Tac Toe", "<https://ardentbot.com/patreon>"))
        } else {
            val game = TicTacToeGame(channel, member.id())
            gamesInLobby.add(game)
        }

    }
}