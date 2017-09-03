package commands.games

import main.conn
import main.r
import net.dv8tion.jda.core.entities.*
import utils.*
import java.awt.Color
import java.util.*
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.collections.HashMap

val gamesInLobby = CopyOnWriteArrayList<Game>()
val activeGames = CopyOnWriteArrayList<Game>()

/**
 * Abstracted Game features, providing standardized methods for cleanup, startup, and lobbies.
 * @param creator Discord user ID of the user creating the game
 * @param isPublic Should this game be treated as public? (will prompt lobby setup)
 */
abstract class Game(val type: GameType, val channel: TextChannel, val creator: String, val playerCount: Int, var isPublic: Boolean) {
    val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()!!
    var gameId: Long = 0
    val players = mutableListOf<String>()
    private val creation: Long
    var startTime: Long? = null

    init {
        gameId = type.findNextId()
        players.add(creator)
        this.announceCreation()
        creation = System.currentTimeMillis()
        if (isPublic) {
            displayLobby()
            scheduledExecutor.scheduleAtFixedRate({
                if (((System.currentTimeMillis() - creation) / 1000) > 300 /* Lobby cancels at 5 minutes */) {
                    cancel(creator.toUser()!!)
                } else displayLobby()
            }, 60, 47, TimeUnit.SECONDS)
        } else if (type != GameType.BLACKJACK && type != GameType.BETTING){
            channel.send("${creator.toUser()!!.asMention}, use **/gameinvite @User** to invite someone to your game")
        }
        scheduledExecutor.scheduleWithFixedDelay({
            if (playerCount == players.size) {
                channel.sendMessage("Starting a game of type **${type.readable}** with **${players.size}** players (${players.toUsers()})")
                        .queueAfter(5, TimeUnit.SECONDS, { _ -> startEvent() })
                scheduledExecutor.shutdown()
            }
        }, 1, 1, TimeUnit.SECONDS)
    }

    /**
     * Displays the lobby prompt for this game and returns the sent [Message] if not null
     */
    private fun displayLobby(): Message? {
        val prefix = channel.guild.getPrefix()
        val member = channel.guild.selfMember
        val embed = member.embed("${type.readable} Game Lobby", Color.ORANGE)
                .setFooter("Ardent Game Engine - Adam#9261", member.user.avatarUrl)
                .setDescription("This lobby has been active for ${((System.currentTimeMillis() - creation) / 1000).formatMinSec()}\n" +
                        "It currently has **${players.size}** of **$playerCount** players required to start | ${players.toUsers()}\n" +
                        "To start, the host can also type *${prefix}minigames forcestart*\n\n" +
                        "This game was created by __${creator.toUser()?.withDiscrim()}__")
        val m = channel.sendReceive(embed)
        channel.send("Join by typing **${prefix}join #$gameId**\n" +
                "*You can cancel this game by typing ${prefix}cancel*")
        return m
    }

    /**
     * Commence game startup, this must be called. Removes pending invites & changes game state
     */
    fun startEvent() {
        invites.forEach { i, g -> if (g.gameId == gameId) invites.remove(i) }
        scheduledExecutor.shutdownNow()
        gamesInLobby.remove(this)
        activeGames.add(this)
        startTime = System.currentTimeMillis()
        onStart()
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
    fun cancel(user: User) {
        gamesInLobby.remove(this)
        activeGames.remove(this)
        channel.send("**${user.withDiscrim()}** cancelled this game (likely due to no response) or the lobby was open for over 5 minutes ;(")
        scheduledExecutor.shutdownNow()
    }

    /**
     * Clean up the game, ending it and inserting the provided [GameData] into the database.
     * @param gameData <b>[Game] specific</b> data class that extends the [GameData] class. This is what is inserted and must be serializable.
     */
    fun cleanup(gameData: GameData) {
        activeGames.remove(this)
        if (r.table("${type.readable}Data").get(gameId).run<Any?>(conn) == null) {
            gameData.id = gameId
            gameData.insert("${type.readable}Data")
        } else {
            val newGameId = type.findNextId()
            gameData.id = gameId
            channel.send("This Game ID has already been inserted into the database. Your new Game ID is **#$newGameId**")
            gameData.insert("${type.readable}Data")
        }
        channel.send("Game Data has been successfully inserted into the database. To view the results and statistics for this match, " +
                "you can go to https://ardentbot.com/games/${type.name.toLowerCase()}/$gameId")
    }

    private fun announceCreation() {
        if (players.size > 1 && isPublic) {
            val prefix = channel.guild.getPrefix()
            if (isPublic) {
                channel.send("You successfully created a **Public ${type.readable}** game with ID #__${gameId}__!\n" +
                        "Anyone in this server can join by typing *${prefix}minigames join #$gameId*")
            }
        }
    }
}

enum class GameType(val readable: String, val description: String, val id: Int) {
    BLACKJACK("Blackjack", "this is a placeholder", 2),
    TRIVIA("Trivia", "this is a placeholder", 3),
    BETTING("Betting", "this is a placeholder", 4),
    CONNECT_4("Connect_4", "this is a placeholder", 5)
    ;
    //CONNECT_FOUR("Connect-Four", "this is a placeholder", 4);

    override fun toString(): String {
        return readable
    }

    /**
     * Generate a game ID, taking care to avoid duplication of an id.
     */
    fun findNextId(): Long {
        val random = Random()
        val number = random.nextInt(99999) + 1
        if (r.table("${readable}Data").get(number).run<Any?>(conn) == null) return number.toLong()
        else return findNextId()
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
        percentageCorrect.forEach { category, percent ->  builder.append("  ${Emoji.SMALL_ORANGE_DIAMOND} $category: *$percent*%\n")}
        return builder.toString()
    }
}

class BlackjackPlayerData(wins: Int = 0, ties: Int = 0, losses: Int = 0) : PlayerGameData(wins, losses, ties)

class BettingPlayerData(wins: Int = 0, losses: Int = 0, var netWinnings: Double = 0.0) : PlayerGameData(wins, losses)

abstract class PlayerGameData(var wins: Int = 0, var losses: Int = 0, var ties: Int = 0) {
    fun gamesPlayed(): Int {
        return wins + losses
    }
}

class GameDataConnect4(gameId: Long, creator: String, startTime: Long, val winner: String, val loser: String, val game: String) : GameData(gameId, creator, startTime)

class GameDataBetting(gameId: Long, creator: String, startTime: Long, val rounds: List<BetGame.Round>) : GameData(gameId, creator, startTime)

class GameDataBlackjack(gameId: Long, creator: String, startTime: Long, val rounds: List<BlackjackGame.Round>) : GameData(gameId, creator, startTime)

class GameDataTrivia(gameId: Long, creator: String, startTime: Long, val winner: String, val losers: List<String>, val scores: Map<String, Int>,
                     val rounds: List<TriviaGame.Round>) : GameData(gameId, creator, startTime) {
    fun sanitize(): SanitizedTrivia {
        val scoresTemp = hashMapOf<String, Int>()
        scores.forEach { t, u -> scoresTemp.put(t.toUser()!!.withDiscrim(), u) }
        val roundsTemp = mutableListOf<SanitizedTriviaRound>()
        rounds.forEach { r -> roundsTemp.add(SanitizedTriviaRound(r.winners.isNotEmpty(),
                r.winners.getOrNull(0)?.toUser(), r.losers.map { it.toUser() }, r.question)) }
        return SanitizedTrivia(creator.toUser()!!, id, winner.toUser()!!, losers.map { it.toUser()!! }, scoresTemp.sort(true).toList() as List<Pair<String, Int>>, roundsTemp)
    }
}

abstract class GameData(var id: Long? = null, val creator: String, val startTime: Long, val endTime: Long = System.currentTimeMillis())