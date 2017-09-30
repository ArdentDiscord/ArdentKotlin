package commands.games

import main.conn
import main.r
import net.dv8tion.jda.core.entities.*
import utils.*
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val gamesInLobby = ConcurrentLinkedQueue<Game>()
val activeGames = ConcurrentLinkedQueue<Game>()

/**
 * Abstracted Game features, providing standardized methods for cleanup, startup, and lobbies.
 * @param creator Discord user ID of the user creating the game
 * @param isPublic Should this game be treated as public? (will prompt lobby setup)
 */
abstract class Game(val type: GameType, val channel: TextChannel, val creator: String, val playerCount: Int, var isPublic: Boolean) {
    private val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()!!
    var gameId: Long = 0
    val players = mutableListOf<String>()
    private val creation: Long
    var startTime: Long? = null
    var started = false

    init {
        gameId = type.findNextId()
        players.add(creator)
        this.announceCreation()
        creation = System.currentTimeMillis()
        if (isPublic) {
            displayLobby()
            scheduledExecutor.scheduleAtFixedRate({ displayLobby() }, 60, 47, TimeUnit.SECONDS)
        } else if (type != GameType.BLACKJACK && type != GameType.BETTING && playerCount > 1) {
            channel.send("{0}, use **/gameinvite @User** to invite someone to your game"
                    .tr(channel.guild, creator.toUser()?.asMention ?: "unable to determine creator"))
        }
        scheduledExecutor.scheduleWithFixedDelay({
            if (playerCount == players.size) {
                channel.sendMessage("Starting a game of type **{0}** with **{1}** players ({2})"
                        .tr(channel.guild, type.readable, players.size, players.toUsers()))
                        .queueAfter(2, TimeUnit.SECONDS, { _ -> startEvent() })
                scheduledExecutor.shutdown()
            }
        }, 1, 1, TimeUnit.SECONDS)
        scheduledExecutor.schedule({
            if (gamesInLobby.contains(this)) {
                channel.send("**10** minutes have passed in lobby, so I cancelled the game setup.".tr(channel))
                cancel(creator.toUser()!!, false)
            }
        }, 10, TimeUnit.MINUTES)
    }

    /**
     * Displays the lobby prompt for this game and returns the sent [Message] if not null
     */
    private fun displayLobby(): Message? {
        val prefix = channel.guild.getPrefix()
        val member = channel.guild.selfMember
        val embed = member.embed("${type.readable} Game Lobby", Color.ORANGE)
                .setFooter("Ardent Game Engine By Adam#9261".tr(channel.guild), member.user.avatarUrl)
                .setDescription("This lobby has been active for {0}".tr(channel.guild, ((System.currentTimeMillis() - creation) / 1000).formatMinSec()) + "\n" +
                        "It currently has **{0}** of **{1}** players required to start | {2}".tr(channel.guild, players.size, playerCount, players.toUsers()) + "\n" +
                        "To start, the host can also type *{0}forcestart*".tr(channel.guild, prefix) + "\n\n" +
                        "This game was created by __{0}__".tr(channel.guild, creator.toUser()?.withDiscrim() ?: "unable to determine"))
        var me: Message? = null
        channel.sendMessage(embed.build()).queue { m ->
            channel.send("Join by typing **${prefix}join #$gameId**\n" +
                    "*You can cancel this game by typing ${prefix}cancel*")
            me = m
        }
        return me
    }

    /**
     * Commence game startup, this must be called. Removes pending invites & changes game state
     */
    fun startEvent() {
        if (!started) {
            invites.forEach { i, g -> if (g.gameId == gameId) invites.remove(i) }
            scheduledExecutor.shutdownNow()
            gamesInLobby.remove(this)
            activeGames.add(this)
            startTime = System.currentTimeMillis()
            onStart()
            started = true
        }
    }

    /**
     * Logic to run after the [Game] starts. This <b>cannot</b> be called instead of startEvent()
     */
    abstract fun onStart()

    fun cancel(member: Member) {
        cancel(member.user)
    }

    /**
     * Cancel a game (either ending it or during lobby). This should be called in [Game] logic.
     */
    fun cancel(user: User, complain: Boolean = true) {
        if (gamesInLobby.contains(this) || activeGames.contains(this)) {
            gamesInLobby.remove(this)
            activeGames.remove(this)
            if (complain) channel.send("**{0}** cancelled this game (likely due to no response) or the lobby was open for over 5 minutes ;(".tr(channel.guild, user.withDiscrim()))
            scheduledExecutor.shutdownNow()
        }
    }

    /**
     * Clean up the game, ending it and inserting the provided [GameData] into the database.
     * @param gameData <b>[Game] specific</b> data class that extends the [GameData] class. This is what is inserted and must be serializable.
     */
    fun cleanup(gameData: GameData) {
        if (activeGames.contains(this)) {
            gamesInLobby.remove(this)
            activeGames.remove(this)
            if (r.table("${type.readable}Data").get(gameId).run<Any?>(conn) == null) {
                gameData.id = gameId
                gameData.insert("${type.readable}Data")
            } else {
                val newGameId = type.findNextId()
                gameData.id = gameId
                channel.send("This Game ID has already been inserted into the database. Your new Game ID is **{0}**".tr(channel.guild, newGameId))
                gameData.insert("${type.readable}Data")
            }
            channel.send("Game Data has been successfully inserted into the database. To view the results and statistics for this match, you can go to https://ardentbot.com/games/{0}/{1}".tr(channel.guild, type.name.toLowerCase(), gameId) + "\n\n" +
                    "*Please consider making a small monthly pledge at {0} if you enjoyed this game to support our hosting and development costs".tr(channel.guild, "<https://patreon.com/ardent>") + "\n   - Adam*")
        }
    }

    private fun announceCreation() {
        if (players.size > 1 && isPublic) {
            val prefix = channel.guild.getPrefix()
            if (isPublic) {
                channel.send("You successfully created a **Public {0}** game with ID #__{1}__!\nAnyone in this server can join by typing *{2}minigames join #{1}*".tr(channel.guild, type.readable, gameId, prefix))
            }
        }
    }
}

enum class GameType(val readable: String, val description: String, val id: Int) {
    BLACKJACK("Blackjack", "this is a placeholder", 2),
    TRIVIA("Trivia", "this is a placeholder", 3),
    BETTING("Betting", "this is a placeholder", 4),
    CONNECT_4("Connect_4", "this is a placeholder", 5),
    TIC_TAC_TOE("Tic_Tac_Toe", "this is a placeholder", 7),
    SLOTS("Slots", "this is a placeholder", 6),
    GUESS_THE_NUMBER("Guess_The_Number", "this is a placeholder", 8);

    override fun toString(): String {
        return readable
    }

    /**
     * Generate a game ID, taking care to avoid duplication of an id.
     */
    fun findNextId(): Long {
        val random = Random()
        val number = random.nextInt(99999) + 1
        return if (r.table("${readable}Data").get(number).run<Any?>(conn) == null) number.toLong()
        else findNextId()
    }
}

fun Member.isInGameOrLobby(): Boolean {
    return user.isInGameOrLobby()
}

fun Guild.hasGameType(gameType: GameType): Boolean {
    gamesInLobby.forEach { if (it.type == gameType && it.channel.guild.id == id) return true }
    activeGames.forEach { if (it.type == gameType && it.channel.guild.id == id) return true }
    return false
}

fun User.isInGameOrLobby(): Boolean {
    gamesInLobby.forEach { if (it.players.contains(id)) return true }
    activeGames.forEach { if (it.players.contains(id)) return true }
    return false
}

class TriviaPlayerData(var wins: Int = 0, var losses: Int = 0, var questionsCorrect: Int = 0, var questionsWrong: Int = 0, var overallCorrectPercent: Double = 0.0, var percentageCorrect: HashMap<String, Double> = hashMapOf()) {
    fun percentagesFancy(): String {
        val builder = StringBuilder()
        percentageCorrect.forEach { category, percent -> builder.append("  ${Emoji.SMALL_ORANGE_DIAMOND} $category: *${percent.toInt()}*%\n") }
        return builder.toString()
    }
}

class SlotsPlayerData(wins: Int = 0, losses: Int = 0, var netWinnings: Double = 0.0) : PlayerGameData(wins, losses)

class Connect4PlayerData(wins: Int = 0, losses: Int = 0) : PlayerGameData(wins, losses, 0)

class BlackjackPlayerData(wins: Int = 0, ties: Int = 0, losses: Int = 0) : PlayerGameData(wins, losses, ties)

class BettingPlayerData(wins: Int = 0, losses: Int = 0, var netWinnings: Double = 0.0) : PlayerGameData(wins, losses)

class TicTacToePlayerData(wins: Int = 0, ties: Int = 0, losses: Int = 0) : PlayerGameData(wins, losses, ties)

abstract class PlayerGameData(var wins: Int = 0, var losses: Int = 0, var ties: Int = 0) {
    fun gamesPlayed(): Int {
        return wins + losses
    }
}

class GameDataSlots(gameId: Long, creator: String, startTime: Long, val rounds: List<SlotsGame.Round>) : GameData(gameId, creator, startTime)

class GameDataConnect4(gameId: Long, creator: String, startTime: Long, val winner: String, val loser: String, val game: String) : GameData(gameId, creator, startTime)

class GameDataBetting(gameId: Long, creator: String, startTime: Long, val rounds: List<BetGame.Round>) : GameData(gameId, creator, startTime)

class GameDataBlackjack(gameId: Long, creator: String, startTime: Long, val rounds: List<BlackjackGame.Round>) : GameData(gameId, creator, startTime)

class GameDataTicTacToe(gameId: Long, creator: String, startTime: Long, val playerOne: String, val playerTwo: String, val winner: String?, val game: String) : GameData(gameId, creator, startTime)

class GameDataTrivia(gameId: Long, creator: String, startTime: Long, val winner: String, val losers: List<String>, val scores: Map<String, Int>,
                     val rounds: List<TriviaGame.Round>) : GameData(gameId, creator, startTime) {
    fun sanitize(): SanitizedTrivia {
        val scoresTemp = hashMapOf<String, Int>()
        scores.forEach { t, u -> scoresTemp.put(t.toUser()!!.withDiscrim(), u) }
        val roundsTemp = mutableListOf<SanitizedTriviaRound>()
        rounds.forEach { (winners, losers1, question) ->
            roundsTemp.add(SanitizedTriviaRound(winners.isNotEmpty(),
                    winners.getOrNull(0)?.toUser(), losers1.map { it.toUser() }, question))
        }
        return SanitizedTrivia(creator.toUser()!!, id, winner.toUser()!!, losers.map { it.toUser()!! }, scoresTemp.sort(true).toList() as List<Pair<String, Int>>, roundsTemp)
    }
}

abstract class GameData(var id: Long? = null, val creator: String, val startTime: Long, val endTime: Long = System.currentTimeMillis())