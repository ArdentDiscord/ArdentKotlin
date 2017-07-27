package commands.games

import com.sun.org.apache.xpath.internal.operations.Bool
import events.Category
import events.Command
import main.conn
import main.r
import net.dv8tion.jda.core.entities.*
import utils.*
import java.awt.Color
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val gamesInLobby = CopyOnWriteArrayList<Game>()
val activeGames = CopyOnWriteArrayList<Game>()

class CoinflipGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.COINFLIP, channel, creator, playerCount, isPublic) {
    override fun start() {
        // TODO("not implemented") // go implement that
    }
}

class BlackjackGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.BLACKJACK, channel, creator, playerCount, isPublic) {
    override fun start() {
        TODO("not implemented") // go implement that
    }
}

class ConnectFourGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.CONNECT_FOUR, channel, creator, playerCount, isPublic) {
    override fun start() {
        TODO("not implemented") // go implement that
    }
}

class TriviaGame(channel: TextChannel, creator: String, playerCount: Int, isPublic: Boolean) : Game(GameType.TRIVIA, channel, creator, playerCount, isPublic) {
    override fun start() {
        TODO("not implemented") // go implement that
    }
}

abstract class Game(val type: GameType, val channel: TextChannel, val creator: String, val playerCount: Int, var isPublic: Boolean) {
    val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()!!
    var gameId: Long = 0
    val players = mutableListOf<String>()
    val creation = System.currentTimeMillis()
    var startTime: Long? = null

    init {
        gameId = type.findNextId()
        players.add(creator)
        this.announceCreation()
        if (isPublic) {
            displayLobby()
            scheduledExecutor.scheduleAtFixedRate({ displayLobby() }, 25, 47, TimeUnit.SECONDS)
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
                        "It currently has **${players.size}** of **$playerCount** players required to start | ${players.stringify()}\n" +
                        "To start, the host can also type *${prefix}minigames forcestart*)\n\n" +
                        "Join by typing *${prefix}minigames join #$gameId*\n" +
                        "This game was created by __${creator.toUser()?.withDiscrim()}__")
        return channel.sendReceive(channel.guild.selfMember, embed)
    }

    fun startEvent() {
        gamesInLobby.remove(this)
        scheduledExecutor.shutdownNow()
        activeGames.add(this)
        startTime = System.currentTimeMillis()
        start()
    }

    abstract fun start()

    fun cancel(member: Member) {
        gamesInLobby.remove(this)
        scheduledExecutor.shutdownNow()
        channel.send(member, "${member.withDiscrim()} decided to cancel this game setup ;(")
    }

    fun end(gameData: Any) {
        gameData.insert("${type.readable}Data")
        activeGames.remove(this)
    }

    fun announceCreation() {
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

enum class GameType(val readable: String, val description: String, val id: Int) {
    COINFLIP("Coinflip", "this is a placeholder", 1),
    BLACKJACK("Blackjack", "this is a placeholder", 2),
    TRIVIA("Trivia", "this is a placeholder", 3),
    CONNECT_FOUR("Connect-Four", "this is a placeholder", 4);

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

fun Member.isInGame(): Boolean {
    return user.isInGame()
}

fun Guild.hasGameType(gameType: GameType): Boolean {
    gamesInLobby.forEach { if (it.type == gameType) return true }
    activeGames.forEach { if (it.type == gameType) return true }
    return false
}

fun User.isInGame(): Boolean {
    gamesInLobby.forEach { if (it.players.contains(id)) return true }
    activeGames.forEach { if (it.players.contains(id)) return true }
    return false
}

class TriviaPlayerData(var wins: Int = 0, var losses: Int = 0, var questionsCorrect: Int = 0, var questionsWrong: Int = 0)

class GameDataTrivia(val id: Long, val winner: String, val scores: HashMap<String, Int>)