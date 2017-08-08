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

val gamesInLobby = CopyOnWriteArrayList<Game>()
val activeGames = CopyOnWriteArrayList<Game>()

abstract class Game(val type: GameType, val channel: TextChannel, val creator: String, val playerCount: Int, var isPublic: Boolean) {
    val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()!!
    var gameId: Long = 0
    val players = mutableListOf<String>()
    val creation: Long
    var startTime: Long? = null

    init {
        gameId = type.findNextId()
        players.add(creator)
        this.announceCreation()
        creation = System.currentTimeMillis()
        if (isPublic) {
            scheduledExecutor.scheduleAtFixedRate({
                if (((System.currentTimeMillis() - creation) / 1000) > 300 /* Lobby cancels at 5 minutes */) {
                    cancel(creator.toUser()!!)
                } else displayLobby()
            }, 60, 47, TimeUnit.SECONDS)
        }
        scheduledExecutor.scheduleWithFixedDelay({
            if (playerCount == players.size) {
                channel.sendMessage("Starting a game of type **${type.readable}** with **${players.size}** players (${players.toUsers()})")
                        .queueAfter(5, TimeUnit.SECONDS, { _ -> startEvent() })
                scheduledExecutor.shutdown()
            }
        }, 1, 1, TimeUnit.SECONDS)
    }

    fun displayLobby(): Message? {
        val prefix = channel.guild.getPrefix()
        val member = channel.guild.selfMember
        val embed = embed("${type.readable} Game Lobby", member, Color.ORANGE)
                .setFooter("Ardent Game Engine - Adam#9261", member.user.avatarUrl)
                .setDescription("This lobby has been active for ${((System.currentTimeMillis() - creation) / 1000).formatMinSec()}\n" +
                        "It currently has **${players.size}** of **$playerCount** players required to start | ${players.toUsers()}\n" +
                        "To start, the host can also type *${prefix}minigames forcestart*\n\n" +
                        "Join by typing *${prefix}minigames join #$gameId*\n" +
                        "This game was created by __${creator.toUser()?.withDiscrim()}__")
        return channel.sendReceive(channel.guild.selfMember, embed)
    }

    fun startEvent() {
        invites.forEach { i, g -> if (g.gameId == gameId) invites.remove(i) }
        scheduledExecutor.shutdownNow()
        gamesInLobby.remove(this)
        activeGames.add(this)
        startTime = System.currentTimeMillis()
        onStart()
    }

    abstract fun onStart()

    fun cancel(member: Member) {
        cancel(member.user)
    }

    fun cancel(user: User) {
        gamesInLobby.remove(this)
        channel.send(user, "**${user.withDiscrim()}** decided to cancel this game (likely due to no response) or the lobby was open for over 5 minutes ;(")
        scheduledExecutor.shutdownNow()
    }


    fun cleanup(gameData: GameData) {
        val user = creator.toUser()!!
        if (r.table("${type.readable}Data").get(gameId).run<Any?>(conn) == null) {
            gameData.id = gameId
            gameData.insert("${type.readable}Data")
        } else {
            val newGameId = type.findNextId()
            gameData.id = gameId
            channel.send(user, "This Game ID has already been inserted into the database. Your new Game ID is **#$newGameId**")
            gameData.insert("${type.readable}Data")
        }
        channel.send(user, "Game Data has been successfully inserted into the database. To view the results and statistics for this match, " +
                "you can go to https://ardentbot.com/${type.name.toLowerCase()}/$gameId")
        activeGames.remove(this)
    }

    fun announceCreation() {
        if (players.size > 1) {
            val prefix = channel.guild.getPrefix()
            val user = creator.toUser()!!
            if (isPublic) {
                channel.send(user, "You successfully created a **Public ${type.readable}** game with ID #__${gameId}__!\n" +
                        "Anyone in this server can join by typing *${prefix}minigames join #$gameId*")
            } else {
                try {
                    user.openPrivateChannel().queue {
                        privateChannel ->
                        privateChannel.send(user, "You successfully created a **__private__** game of **${type.readable}**. Invite members " +
                                "by typing __${prefix}minigames invite @User__ - Choose wisely, because you can't get rid of them once they've accepted!")
                    }
                } catch(e: Exception) {
                    channel.send(user, "${user.asMention}, you need to allow messages from me! If you don't remember how to invite people, I'd cancel the game")
                }
            }
        }
    }
}

enum class GameType(val readable: String, val description: String, val id: Int) {
    COINFLIP("Coinflip", "this is a placeholder", 1),
    BLACKJACK("Blackjack", "this is a placeholder", 2);
    //TRIVIA("Trivia", "this is a placeholder", 3),
    //CONNECT_FOUR("Connect-Four", "this is a placeholder", 4);

    override fun toString(): String {
        return readable
    }

    fun findNextId(): Long {
        val random = Random()
        val number = random.nextInt(1000000) + 1
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

class TriviaPlayerData(var wins: Int = 0, var losses: Int = 0, var questionsCorrect: Int = 0, var questionsWrong: Int = 0)

// class GameDataTrivia(gameId: Long, creator: String, val winner: String, val scores: HashMap<String, Int>) : GameData(gameId, creator)

class CoinflipPlayerData(wins: Int = 0, losses: Int = 0, var roundsWon: Int = 0, var roundsLost: Int = 0) : PlayerGameData(wins, losses)

class BlackjackPlayerData(wins: Int = 0, ties: Int = 0, losses: Int = 0, var roundsWon: Int, var roundsLost: Int) : PlayerGameData(wins, losses, ties)

abstract class PlayerGameData(var wins: Int = 0, var losses: Int = 0, var ties: Int = 0) {
    fun gamesPlayed(): Int {
        return wins + losses
    }
}

class GameDataBlackjack(gameId: Long, creator: String, startTime: Long, val rounds: List<BlackjackGame.Round>) : GameData(gameId, creator, startTime)

class GameDataCoinflip(gameId: Long, creator: String, startTime: Long, val winner: String, val losers: List<String>, val rounds: List<CoinflipGame.Round>) : GameData(gameId, creator, startTime) {
    fun contains(id: String): Boolean {
        return winner == id || losers.contains(id)
    }
}

abstract class GameData(var id: Long? = null, val creator: String, val startTime: Long, val endTime: Long = System.currentTimeMillis())