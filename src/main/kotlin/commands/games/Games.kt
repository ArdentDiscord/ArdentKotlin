package commands.games

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
import java.util.concurrent.TimeUnit

val invites = ConcurrentHashMap<String, Game>()

class BlackjackGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.BLACKJACK, channel, creator, playerCount, isPublic) {
    val roundResults = mutableListOf<Round>()
    override fun onStart() {
        val user = players.map { it.toUser()!! }.toList()[0]
        doRound(user)
    }

    fun doRound(user: User) {
        val playerData = user.getData()
        if (playerData.gold == 0.toDouble()) {
            playerData.gold += 15
            playerData.update()
            channel.send(user, "Because you were broke, the Blackjack Gods took pity on you and gave you **15.0** gold to bet with")
        }
        channel.send(user, "How much would you like to bet, ${user.asMention}? You current have a balance of **${playerData.gold}** gold")
        waiter.waitForMessage(Settings(user.id, channel.id, channel.guild.id), { message ->
            val bet = message.rawContent.toIntOrNull()
            if (bet == null || bet <= 0 || bet > playerData.gold) {
                channel.send(user, "You specified an invalid amount.. resetting the round")
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
                    channel.send(user, "Generating dealer cards...")
                    while (dealerHand.value() < 17) dealerHand.blackjackPlus(1)
                    displayRoundScore(bet, dealerHand, userHand, user)
                }
                else -> {
                    channel.send(user, "You specified an invalid response - please retry")
                    wait(bet, dealerHand, userHand, user)
                    return@waitForMessage
                }
            }
        }, {
            channel.send(user, "${user.asMention}, you didn't specify a response and lost!")
            while (userHand.value() < 21) userHand.blackjackPlus(1)
            displayRoundScore(bet, dealerHand, userHand, user)
            cancel(user)
        }, 15, TimeUnit.SECONDS, silentExpiration = true)
    }

    fun display(dealerHand: Hand, userHand: Hand, message: String, end: Boolean = false) {
        val embed = embed("Blackjack | Hand Values", channel.guild.selfMember)
                .setDescription(message)
                .addField("Your Hand", "$userHand - value (${userHand.value()})", true)
                .addBlankField(true)
        if (dealerHand.cards.size == 2 && !end) embed.addField("Dealer's Hand", "$dealerHand - value (${dealerHand.cards[0].value.representation} + ?)", true)
        else embed.addField("Dealer's Hand", "$dealerHand - value (${dealerHand.value()})", true)
        channel.send(channel.guild.selfMember, embed)
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
                    channel.send(user, "Ending the game and inserting data into the database..")
                    val gameData = GameDataBlackjack(gameId, creator, startTime!!, roundResults)
                    cleanup(gameData)
                }
            }
        }, {
            channel.send(user, "Ending the game and inserting data into the database..")
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
    private val roundTotal = 21
    private val questions = roundTotal.getTrivia()
    override fun onStart() {
        channel.send(ardent, "**Trivia is starting..** There will be __${roundTotal}__ questions, followed by Final Jeopardy. You'll have **20** seconds to " +
                "complete each round")
        doRound(0, questions)
    }

    fun doRound(currentRound: Int, questions: List<TriviaQuestion>) {
        if (currentRound == 20) doFinalJeopardy()
        else {
            val question = questions[currentRound]
            channel.send(ardent, embed("Trivia | Question ${currentRound + 1}", ardent)
                    .appendDescription("**${question.category.getCategoryName()}**\n" +
                            "${question.question}\n" +
                            "           **${question.value}** points"))
            var guessed = false
            waiter.gameChannelWait(channel.id, { response ->
                if (response.rawContent.equals(question.answer, true) || "a ${response.rawContent}".equals(question.answer, true)) {
                    channel.send(ardent, "${response.author.asMention} guessed the correct answer and got **${question.value}** points!")
                    guessed = true
                    endRound(response.author.id, players.without(response.author.id), question, currentRound, questions)
                    return@gameChannelWait
                }
            }, {
                if (!guessed) {
                    channel.send(ardent, "No one got it right! The correct answer was **${question.answer}**")
                    endRound(null, players, question, currentRound, questions)
                    return@gameChannelWait
                }
            }, time = 20)
        }
    }

    private fun endRound(winner: String?, losers: MutableList<String>, question: TriviaQuestion, currentRound: Int, questions: List<TriviaQuestion>) {
        rounds.add(Round(winner, losers, question))
        showScores(currentRound)
        doRound(currentRound + 1, questions)
    }

    fun showScores(currentRound: Int) {
        val embed = embed("Trivia Scores | Round ${currentRound + 1}", ardent)
        val scores = getScores()
        if (scores.second.size == 0) embed.setDescription("No one has scored yet!")
        else scores.first.forEachIndexed { index, u, score -> embed.appendDescription("[**$index**]: **${u.toUser()!!.asMention}** *($score points)*") }
        channel.send(ardent, embed)
    }

    private fun doFinalJeopardy() {
        val scores = getScores().first
    }

    fun getScores(): Pair<MutableMap<String, Int /* Point values */>, HashMap<String, Int /* Amt of Qs correct */>> {
        val points = hashMapOf<String, Int>()
        val questions = hashMapOf<String, Int>()
        rounds.forEach { (winner, _, q) ->
            if (winner != null) {
                if (points.containsKey(winner)) points.replace(winner, points[winner]!! + q.value)
                else points.put(winner, q.value)
                questions.incrementValue(winner)
            }
        }
        return Pair(points.sort(true) as MutableMap<String, Int>, questions)
    }

    data class FinalJeopardy(val winners: HashMap<String, Int>, val losers: HashMap<String, Int>)
    data class Round(val winner: String?, val losers: MutableList<String>, val question: TriviaQuestion)
}

class CoinflipGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.COINFLIP, channel, creator, playerCount, isPublic) {
    override fun onStart() {
        val ardent = channel.guild.selfMember
        val users = players.map { it.toUser()!! }.toList()
        channel.send(ardent, "This is a **5** round game where the person who guesses the side the most times wins.\nIf there's a tie, the top player will flip a coin and if they guess correctly, they win. " +
                "If not, the second place player will win.")
        val results = mutableListOf<Round>()

        val doRound = { round: Int ->
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
        if (values.size > 1 && values[0] == values[1]) {
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
        if (users.size > 1) {
            val winnerUser = winner!!.toUser()!!
            channel.send(channel.guild.selfMember, "Congratulations to **${winnerUser.withDiscrim()}** for winning with __${scores[winner!!]}__ correct guesses; You win **150** gold")
            val playerData = winnerUser.getData()
            playerData.gold += 150
            playerData.update()
        }
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

class BetGame(channel: TextChannel, creator: String) : Game(GameType.BETTING, channel, creator, 1, false) {
    val rounds = mutableListOf<Round>()
    override fun onStart() {
        doRound(creator.toUser()!!)
    }

    fun doRound(user: User) {
        val data = user.getData()
        channel.send(user, "How much would you like to bet? You current have **${data.gold} gold**. Type the amount below. You can also bet a **percentage** of your net worth, e.g. *40%*")
        waiter.waitForMessage(Settings(creator, channel.id, channel.guild.id), { message ->
            val content = message.content
            if (content.equals("cancel", true)) {
                channel.send(user, "Cancelling game...")
                cancel(user)
            } else {
                val bet = if (content.contains("%")) content.removeSuffix("%").toDoubleOrNull()?.div(100)?.times(data.gold)?.toInt() else content.toIntOrNull()
                if (bet != null) {
                    if (bet > data.gold || bet <= 0) {
                        channel.send(user, "You specified an invalid bet amount! Please retry or type `cancel` to cancel the game")
                        doRound(user)
                    } else {
                        channel.selectFromList(channel.guild.getMember(user), "What color will the next card I draw be?", mutableListOf("Black", "Red"), { selection ->
                            val suit = BlackjackGame.Hand(false, end = false).generate().suit
                            val won = when (suit) {
                                BlackjackGame.Suit.HEART, BlackjackGame.Suit.DIAMOND -> selection == 1
                                else -> selection == 0
                            }
                            if (won) {
                                data.gold += bet
                                channel.send(user, "Congrats, you won - the suit was $suit! I've added **$bet gold** to your profile - new balance: **${data.gold.format()} gold**")
                            } else {
                                data.gold -= bet
                                channel.send(user, "Sorry, you lost - the suit was $suit :( I've removed **$bet gold** from your profile - new balance: **${data.gold.format()} gold**")
                            }
                            data.update()
                            rounds.add(Round(won, bet.toDouble(), suit))
                            channel.send(user, "Would you like to go again? Type `yes` if so or `no` to end the game")
                            waiter.waitForMessage(Settings(creator, channel.id, channel.guild.id), { continueGameMessage ->
                                if (continueGameMessage.rawContent.startsWith("ye", true)) doRound(user)
                                else {
                                    channel.send(user, "Ending the game and inserting data into the database..")
                                    val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                    cleanup(gameData)
                                }
                            }, {
                                channel.send(user, "Ending the game and inserting data into the database..")
                                val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                cleanup(gameData)
                            })

                        }, failure = {
                            if (rounds.size == 0) {
                                channel.send(user, "Invalid response... Cancelling game now")
                                cancel(user)
                            } else {
                                channel.send(user, "Invalid response... ending the game and inserting data into the database..")
                                val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                cleanup(gameData)
                            }
                        })
                    }
                } else {
                    if (rounds.size == 0) {
                        channel.send(user, "Invalid bet amount... Cancelling game now")
                        cancel(user)
                    } else {
                        channel.send(user, "Invalid bet amount... ending the game and inserting data into the database..")
                        val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                        cleanup(gameData)
                    }
                }
            }
        }, { cancel(user) })
    }

    data class Round(val won: Boolean, val betAmount: Double, val suit: BlackjackGame.Suit)
}

class BetCommand : Command(Category.GAMES, "bet", "bet some money - will you be lucky?") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (member.isInGameOrLobby()) channel.send(member, "${member.user.asMention}, You're already in game! You can't create another game!")
        else BetGame(channel, member.id()).startEvent()
    }
}

class TriviaCommand : Command(Category.GAMES, "trivia", "start a trivia game") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (member.isInGameOrLobby()) channel.send(member, "${member.user.asMention}, You're already in game! You can't create another game!")
        else if (guild.hasGameType(GameType.TRIVIA) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send(member, "There can only be one trivia game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at " +
                    "https://ardentbot.com/patreon to start more than one game per type at a time**")
        } else {
            channel.selectFromList(member, "Would you like this game of ${GameType.TRIVIA.readable} to be open to everyone to join?", mutableListOf("Yes", "No"), { public ->
                val isPublic = public == 0
                channel.send(member, "How many players would you like in this game? Type `none` to set the limit as 999 (effectively no limit)")
                waiter.waitForMessage(Settings(member.user.id, channel.id, guild.id), { playerCount ->
                    val count = playerCount.content.toIntOrNull() ?: 999
                    val game = TriviaGame(channel, member.id(), count, isPublic)
                    gamesInLobby.add(game)

                })
            })
        }
    }

}

class BlackjackCommand : Command(Category.GAMES, "blackjack", "start games of blackjack") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (member.isInGameOrLobby()) channel.send(member, "${member.user.asMention}, You're already in game! You can't create another game!")
        else if (guild.hasGameType(GameType.BLACKJACK) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send(member, "There can only be one blackjack game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at " +
                    "https://ardentbot.com/patreon to start more than one game per type at a time**")
        } else {
            BlackjackGame(channel, member.id(), 1, false).startEvent()
        }
    }
}

class CoinflipCommand : Command(Category.GAMES, "coinflip", "start games of coinflip (it's fun we promise)") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (member.isInGameOrLobby()) channel.send(member, "${member.user.asMention}, You're already in game! You can't create another game!")
        else if (guild.hasGameType(GameType.COINFLIP) && !member.hasDonationLevel(channel, DonationLevel.INTERMEDIATE, failQuietly = true)) {
            channel.send(member, "There can only be one coinflip game active at a time in a server!. **Pledge $5 a month or buy the Intermediate rank at " +
                    "https://ardentbot.com/patreon to start more than one game per type at a time**")
        } else {
            channel.selectFromList(member, "Would you like this game of ${GameType.COINFLIP.readable} to be open to everyone to join?", mutableListOf("Yes", "No"), { public ->
                val isPublic = public == 0
                channel.send(member, "How many players would you like in this game? Type `none` to set the limit as 999 (effectively no limit)")
                waiter.waitForMessage(Settings(member.user.id, channel.id, guild.id), { playerCount ->
                    val count = playerCount.content.toIntOrNull() ?: 999
                    val game = CoinflipGame(channel, member.id(), count, isPublic)
                    gamesInLobby.add(game)

                })
            })
        }
    }
}

class Games : Command(Category.GAMES, "minigames", "who's the most skilled? play against friends or compete for the leaderboards in these addicting games") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        withHelp("/gamelist", "lists all games that are waiting for players or setting up to start")
                .withHelp("/gameinvite @User", "allows the creator of the game to invite players in the server where it was started")
                .withHelp("/decline invite", "decline a pending invite")
                .withHelp("/joingame #game_id", "join a public game by its id or a game that you were invited to")
                .withHelp("/forcestart", "force start a game")
                .withHelp("/cancel", "cancel the game while it's in setup (for creators)")
                .withHelp("/leavegame", "leave a game or its lobby (this could trigger your resignation from the game if it has already started)")
                .withHelp("/bet", "start a betting game")
                .withHelp("/blackjack", "start a blackjack game")
                .withHelp("/coinflip", "start a coinflip game")
                .displayHelp(channel, member)
        return
    }
}