package games

import main.conn
import main.r
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import utils.*
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit

val gamesInLobby = ConcurrentHashMap<Game, String? /* ID of the lobby message for joining */>()
val activeGames = CopyOnWriteArrayList<Game>()

abstract class Coinflip

abstract class Game(val type : GameType, val channel: TextChannel, val creator : String, val playerCount : Int) {
    val scheduledExecutor = Executors.newSingleThreadScheduledExecutor()
    var gameId : Long = 0
    val players = mutableListOf<String>()
    val creation = System.currentTimeMillis()
    var startTime : Long? = null
    init {
        val message = displayLobby()
        gamesInLobby.put(this, message?.id)
        scheduledExecutor.scheduleWithFixedDelay({
            if (playerCount == players.size) {
                scheduledExecutor.shutdownNow()
                channel.sendMessage("Starting a game of type **${type.readable}** with **${players.size}** players (${players.concat()})")
                        .queueAfter(5, TimeUnit.SECONDS, { _ -> startEvent()})
            }
        }, 1, 1, TimeUnit.SECONDS)
        scheduledExecutor.scheduleAtFixedRate({ displayLobby() }, 20, 20, TimeUnit.SECONDS)
    }

    fun displayLobby() : Message? {
        val member = channel.guild.selfMember
        val embed = embed("${type.readable} Game Lobby", member, Color.ORANGE)
                .setFooter("Ardent GameAPI by Adam#9261 & Suit ✈#6566", member.user.avatarUrl)
                .setDescription("This lobby has been active for ${(System.currentTimeMillis() - creation) / 1000} seconds and currently has " +
                        "**${players.size}** of **$playerCount** required\n\n" +
                        "This game was created by ${creator.toUser()?.withDiscrim()}")
        val returned = channel.sendReceive(channel.guild.selfMember, embed)
        if (gamesInLobby.contains(this)) gamesInLobby.replace(this, returned?.id)
        return returned
    }

    fun startEvent() {
        gameId = type.findNextId()
        gamesInLobby.remove(this)
        scheduledExecutor.shutdownNow()
        activeGames.add(this)
        start()
    }

    abstract fun start()

    fun cancel(member: Member) {
        gamesInLobby.remove(this)
        scheduledExecutor.shutdownNow()
        channel.send(member, "${member.withDiscrim()} decided to cancel this game setup ;(")
    }

    fun end(gameData : Any) {
        gameData.insert("${type.readable}Data")
        activeGames.remove(this)
    }
}

fun GameType.findNextId() : Long {
    val random = Random()
    val number = random.nextInt(1000000) + 1
    if (r.table("${readable}Data").get(number).run<Any?>(conn) != null) return number.toLong()
    else return findNextId()
}

enum class GameType(val readable : String, val id : Int) {
    COINFLIP("Coinflip", 1),
    BLACKJACK("Blackjack", 2),
    TRIVIA("Trivia", 3),
    CONNECT_FOUR("Connect-Four", 4);

    override fun toString(): String {
        return readable
    }
}

class TriviaPlayerData(var wins : Int = 0, var losses : Int = 0, var questionsCorrect : Int = 0, var questionsWrong : Int = 0)

class GameDataTrivia(val id : Long, val winner : String, val scores : HashMap<String, Int>)