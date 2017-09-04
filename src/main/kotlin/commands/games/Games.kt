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
            channel.send("Because you were broke, the Blackjack Gods took pity on you and gave you **15.0** gold to bet with")
        }
        channel.send("How much would you like to bet, ${user.asMention}? You current have a balance of **${playerData.gold}** gold")
        waiter.waitForMessage(Settings(user.id, channel.id, channel.guild.id), { message ->
            val bet = message.rawContent.toIntOrNull()
            if (bet == null || bet <= 0 || bet > playerData.gold) {
                channel.send("You specified an invalid amount.. resetting the round")
                doRound(user)
            } else {
                val dealerHand = Hand(true).blackjackPlus(2)
                val userHand = Hand().blackjackPlus(1)
                display(dealerHand, userHand, "You've been dealt 1 card. The dealer's second card is hidden. The goal is to get as close as possible to **21**." +
                        " Type `hit` if you'd like to get another card or `stay` to stay at your current amount")
                wait(bet.toDouble(), dealerHand, userHand, user)
            }
        }, { cancel(user) }, silentExpiration = true)
    }

    fun wait(bet: Double, dealerHand: Hand, userHand: Hand, user: User) {
        waiter.waitForMessage(Settings(user.id, channel.id), { response ->
            when (response.content) {
                "hit" -> {
                    userHand.blackjackPlus(1)
                    if (userHand.value() >= 21) displayRoundScore(bet, dealerHand, userHand, user)
                    else {
                        display(dealerHand, userHand, "The dealer's second card is hidden. The goal is to get as close as possible to **21**." +
                                " Type `hit` if you'd like to get another card or `stay` to stay at your current amount")
                        wait(bet, dealerHand, userHand, user)
                    }
                }
                "stay" -> {
                    channel.send("Generating dealer cards...")
                    while (dealerHand.value() < 17) dealerHand.blackjackPlus(1)
                    displayRoundScore(bet, dealerHand, userHand, user)
                }
                else -> {
                    channel.send("You specified an invalid response - please retry")
                    wait(bet, dealerHand, userHand, user)
                    return@waitForMessage
                }
            }
        }, {
            channel.send("${user.asMention}, you didn't specify a response and lost!")
            while (userHand.value() < 21) userHand.blackjackPlus(1)
            displayRoundScore(bet, dealerHand, userHand, user)
            cancel(user)
        }, 15, TimeUnit.SECONDS, silentExpiration = true)
    }

    fun display(dealerHand: Hand, userHand: Hand, message: String, end: Boolean = false) {
        val embed = channel.guild.selfMember.embed("Blackjack | Hand Values")
                .setDescription(message)
                .addField("Your Hand", "$userHand - value (${userHand.value()})", true)
                .addBlankField(true)
        if (dealerHand.cards.size == 2 && !end) embed.addField("Dealer's Hand", "$dealerHand - value (${dealerHand.cards[0].value.representation} + ?)", true)
        else embed.addField("Dealer's Hand", "$dealerHand - value (${dealerHand.value()})", true)
        channel.send(embed)
    }

    fun displayRoundScore(bet: Double, dealerHand: Hand, userHand: Hand, user: User) {
        val result = if (userHand.value() > 21) Result.LOST
        else if (dealerHand.value() > 21) Result.WON
        else if (userHand.value() == dealerHand.value()) Result.TIED
        else if (userHand.value() > dealerHand.value()) Result.WON
        else Result.LOST
        val playerData = user.getData()
        val message = when (result) {
            Result.LOST -> {
                playerData.gold -= bet
                playerData.update()
                "**Sorry, you lost $bet gold!**"

            }
            Result.WON -> {
                playerData.gold += bet
                playerData.update()
                "**Congratulations, you won $bet gold!**"
            }
            Result.TIED -> "**You tied and didn't lose the $bet you bet!**"
        }
        roundResults.add(Round(result, userHand.end(), dealerHand.end(), bet))
        display(dealerHand, userHand, message, true)

        channel.sendMessage("Would you like to go again? Type `yes` to replay or `no` to end the game").queueAfter(2, TimeUnit.SECONDS)
        waiter.waitForMessage(Settings(user.id, channel.id), { response ->
            when (response.content) {
                "yes" -> doRound(user)
                else -> {
                    channel.send("Ending the game and inserting data into the database..")
                    val gameData = GameDataBlackjack(gameId, creator, startTime!!, roundResults)
                    cleanup(gameData)
                }
            }
        }, {
            channel.send("Ending the game and inserting data into the database..")
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
            channel.send("Congrats to ${winnerUser.asMention} for winning with **${winner.second}** points! You'll receive that amount in gold as a prize!\n" +
                    "**Cleaning game up..**")
            val data = winnerUser.getData()
            data.gold += winner.second
            data.update()
            cleanup(GameDataTrivia(gameId, creator, startTime!!, winner.first, players.without(winner.first), sc, rounds))
        } else {
            if (currentRound == (roundTotal - 3)) channel.send("${Emoji.INFORMATION_SOURCE} There are only **3** rounds left!")
            val question = questions[currentRound]
            channel.send(ardent.embed("Trivia | Question ${currentRound + 1} of $roundTotal")
                    .appendDescription("**${question.category}**\n" +
                            "${question.question}\n" +
                            "           **${question.value}** points"))
            var guessed = false
            waiter.gameChannelWait(channel.id, { response ->
                if (players.contains(response.author.id) && !guessed) {
                    if (question.answers.containsEq(response.rawContent.toLowerCase())) {
                        channel.send("${response.author.asMention} guessed the correct answer and got **${question.value}** points!")
                        guessed = true
                        endRound(players.without(response.author.id), question, currentRound, questions, response.author.id)
                        return@gameChannelWait
                    }
                }
            }, {
                if (!guessed) {
                    channel.send("No one got it right! The correct answer was **${question.answers[0]}**")
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
        val embed = ardent.embed("Trivia Scores | ${if (currentRound > 21) "Final Jeopardy" else "Round ${currentRound + 1}"}")
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
        channel.send("How much would you like to bet? You current have **${data.gold} gold**. Type the amount below. You can also bet a **percentage** of your net worth, e.g. *40%*")
        waiter.waitForMessage(Settings(creator, channel.id, channel.guild.id), { message ->
            val content = message.content
            if (content.equals("cancel", true)) {
                channel.send("Cancelling game...")
                cancel(user)
            } else {
                val bet = if (content.contains("%")) content.removeSuffix("%").toDoubleOrNull()?.div(100)?.times(data.gold)?.toInt() else content.toIntOrNull()
                if (bet != null) {
                    if (bet > data.gold || bet <= 0) {
                        channel.send("You specified an invalid bet amount! Please retry or type `cancel` to cancel the game")
                        doRound(user)
                    } else {
                        channel.selectFromList(channel.guild.getMember(user), "What color will the next card I draw be?", mutableListOf("Black", "Red"), { selection, _ ->
                            val suit = BlackjackGame.Hand(false, end = false).generate().suit
                            val won = when (suit) {
                                BlackjackGame.Suit.HEART, BlackjackGame.Suit.DIAMOND -> selection == 1
                                else -> selection == 0
                            }
                            if (won) {
                                data.gold += bet
                                channel.send("Congrats, you won - the suit was $suit! I've added **$bet gold** to your profile - new balance: **${data.gold.format()} gold**")
                            } else {
                                data.gold -= bet
                                channel.send("Sorry, you lost - the suit was $suit :( I've removed **$bet gold** from your profile - new balance: **${data.gold.format()} gold**")
                            }
                            data.update()
                            rounds.add(Round(won, bet.toDouble(), suit))
                            channel.send("Would you like to go again? Type `yes` if so or `no` to end the game")
                            waiter.waitForMessage(Settings(creator, channel.id, channel.guild.id), { continueGameMessage ->
                                if (continueGameMessage.rawContent.startsWith("ye", true)) doRound(user)
                                else {
                                    channel.send("Ending the game and inserting data into the database..")
                                    val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                    cleanup(gameData)
                                }
                            }, {
                                channel.send("Ending the game and inserting data into the database..")
                                val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                cleanup(gameData)
                            })

                        }, failure = {
                            if (rounds.size == 0) {
                                channel.send("Invalid response... Cancelling game now")
                                cancel(user)
                            } else {
                                channel.send("Invalid response... ending the game and inserting data into the database..")
                                val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                cleanup(gameData)
                            }
                        })
                    }
                } else {
                    if (rounds.size == 0) {
                        channel.send("Invalid bet amount... Cancelling game now")
                        cancel(user)
                    } else {
                        channel.send("Invalid bet amount... ending the game and inserting data into the database..")
                        val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                        cleanup(gameData)
                    }
                }
            }
        }, { cancel(user) })
    }

    data class Round(val won: Boolean, val betAmount: Double, val suit: BlackjackGame.Suit)
}

class Connect4Game(channel: TextChannel, creator: String) : Game(GameType.CONNECT_4, channel, creator, 2, false) {
    override fun onStart() {
        val game = GameBoard(players[0], players[1])
        doRound(game, channel.guild.getMemberById(players[0]))
    }

    private fun doRound(game: GameBoard, player: Member, cancelIfExpired: Boolean = false) {
        if (game.full()) {
            channel.send("All tiles have been placed without a winner :thinking: Therefore, I choose ${player.asMention} as the winner! (They will not get any gold for winning this game)")
            val embed = channel.guild.selfMember.embed("Connect 4 | Results", Color.BLUE)
            embed.appendDescription("${player.asMention} has won (technically) in a tied game!\n$game")
            channel.send(embed)
            cleanup(GameDataConnect4(gameId, creator, startTime!!, player.id(), if (player.id() == players[0]) players[1] else players[0], game.toString()))
        } else {
            val embed = channel.guild.selfMember.embed("Connect 4 Game Board", Color.BLUE)
            embed.appendDescription("${player.asMention}, it's your turn ${if (players[0] == player.id()) "\uD83D\uDD34" else "\uD83D\uDD35"} - React to place your chip\n")
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
                            channel.send("An unidentified reaction was received from ${player.asMention}. Retrying the round... If they don't respond this time, the game will be cancelled")
                            doRound(game, player, true)
                        }
                    }
                    message.delete().queue()
                }, {
                    if (cancelIfExpired) cancel(creator.toUser()!!)
                    else {
                        channel.send("No input was received from ${player.asMention}. Retrying the round... If they don't respond this time, the game will be cancelled")
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
            channel.send("${player.asMention}, you specified an invalid column: Please retry.. :(")
            doRound(game, player, false)
        } else {
            val winnerId = game.checkWin(tile)
            if (winnerId == null) doRound(game, if (player.id() == players[0]) channel.guild.getMemberById(players[1]) else channel.guild.getMemberById(players[0]))
            else {
                val winner = channel.guild.getMemberById(winnerId)
                val embed = channel.guild.selfMember.embed("Connect 4 | Results", Color.BLUE)
                embed.appendDescription("${winner.asMention} has won and got **500 gold**!\n")
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
    override fun onStart() {

    }

    class Round {

    }
}

class BetCommand : Command(Category.GAMES, "bet", "bet some money - will you be lucky?") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val member = event.member
        if (member.isInGameOrLobby()) event.channel.send("${member.user.asMention}, You're already in game! You can't create another game!")
        else BetGame(event.textChannel, member.id()).startEvent()
    }
}

class TriviaCommand : Command(Category.GAMES, "trivia", "start a trivia game") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val channel = event.textChannel
        val member = event.member
        if (member.isInGameOrLobby()) channel.send("${member.user.asMention}, You're already in game! You can't create another game!")
        else if (event.guild.hasGameType(GameType.TRIVIA) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one trivia game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at " +
                    "https://ardentbot.com/patreon to start more than one game per type at a time**")
        } else {
            channel.selectFromList(member, "Would you like this game of ${GameType.TRIVIA.readable} to be open to everyone to join?", mutableListOf("Yes", "No"), { public, _ ->
                val isPublic = public == 0
                channel.send("How many players would you like in this game? Type `none` to set the limit as 999 (effectively no limit)")
                waiter.waitForMessage(Settings(member.user.id, channel.id, event.guild.id), { playerCount ->
                    val count = playerCount.content.toIntOrNull() ?: 999
                    if (count == 0 || count == 1) channel.send("Invalid number provided, cancelling setup")
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
        if (member.isInGameOrLobby()) channel.send("${member.user.asMention}, You're already in game! You can't create another game!")
        else if (event.guild.hasGameType(GameType.SLOTS) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one slots game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at " +
                    "https://ardentbot.com/patreon to start more than one game per type at a time**")
        } else {
            SlotsGame(channel, member.id(), 1, false).startEvent()
        }
    }
}

class BlackjackCommand : Command(Category.GAMES, "blackjack", "start games of blackjack") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val member = event.member
        val channel = event.textChannel
        if (member.isInGameOrLobby()) channel.send("${member.user.asMention}, You're already in game! You can't create another game!")
        else if (event.guild.hasGameType(GameType.BLACKJACK) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one blackjack game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at " +
                    "https://ardentbot.com/patreon to start more than one game per type at a time**")
        } else {
            BlackjackGame(channel, member.id(), 1, false).startEvent()
        }
    }
}

class Connect4Command : Command(Category.GAMES, "connect4", "start connect 4 games - inside Discord!") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val member = event.member
        val channel = event.textChannel
        if (member.isInGameOrLobby()) channel.send("${member.user.asMention}, You're already in game! You can't create another game!")
        else if (event.guild.hasGameType(GameType.CONNECT_4) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send("There can only be one Connect 4 game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at " +
                    "https://ardentbot.com/patreon to start more than one game per type at a time**")
        } else {
            val game = Connect4Game(channel, member.id())
            gamesInLobby.add(game)
        }

    }
}

class Games : Command(Category.GAMES, "minigames", "who's the most skilled? play against friends or compete for the leaderboards in these addicting games") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        withHelp("/gamelist", "lists all games that are waiting for players or setting up to start")
                .withHelp("/gameinvite @User", "allows the creator of the game to invite players in the server where it was started")
                .withHelp("/decline invite", "decline a pending invite")
                .withHelp("/joingame #game_id", "join a public game by its id or a game that you were invited to")
                .withHelp("/forcestart", "force start a game")
                .withHelp("/cancel", "cancel the game while it's in setup (for creators)")
                .withHelp("/leavegame", "leave a game or its lobby (this could trigger your resignation from the game if it has already started)")
                .withHelp("/bet", "start a betting game")
                .withHelp("/blackjack", "start a blackjack game")
                .withHelp("/trivia", "start a trivia game")
                .withHelp("/connect4", "start a connect-4 game")

                .displayHelp(event.channel, event.member)
        return
    }
}