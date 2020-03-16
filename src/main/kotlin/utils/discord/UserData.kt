package utils.discord

import main.conn
import main.r
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import translation.tr
import utils.functionality.*
import utils.music.DatabaseMusicLibrary
import utils.music.DatabaseMusicPlaylist
import java.util.concurrent.TimeUnit

fun User.isAdministrator(channel: TextChannel, complain: Boolean = false): Boolean {
    if (getStaffLevel(id) == StaffRole.ADMINISTRATOR)
        if (complain) channel.send("${Emoji.HEAVY_MULTIPLICATION_X} " + "You need to be an **Ardent Administrator** to use this command!".tr(channel))
    return false
}

fun TextChannel.requires(user: User, requiredLevel: PatronLevel, failQuietly: Boolean = false): Boolean {
    if (guild.isPatronGuild()) return true
    else send("${Emoji.CROSS_MARK} " + "{0}, This command requires that you or the owner of this server have a donation level of **{0}** to be able to use it".tr(guild, user.asMention, requiredLevel.readable))
    return false
}

fun User.hasPatronPermission(channel: TextChannel, donationLevel: PatronLevel, failQuietly: Boolean = false): Boolean {
    val patronLevel = getPatronLevel(id)
    return if (patronLevel != null && patronLevel.level >= donationLevel.level) true
    else channel.requires(this, donationLevel, failQuietly)
}

fun User.getData(): UserData {
    var data = asPojo(r.table("users").get(id).run(conn), UserData::class.java)
    if (data != null) return data
    data = UserData(id, 25.0, 0L, null, UserData.Gender.UNDEFINED, mutableListOf("English"), connectedAccounts = ConnectedAccounts())
    data.insert("users")
    return data
}

data class ConnectedAccounts(var spotifyId: String? = null)

class UserData(val id: String, var gold: Double = 50.0, var collected: Long = 0, var selfDescription: String?,
               var gender: Gender, val languagesSpoken: MutableList<String>, val reminders: MutableList<Reminder> = mutableListOf(),
               val connectedAccounts: ConnectedAccounts) {
    fun canCollect(): Boolean {
        return ((System.currentTimeMillis() - collected) / 1000) > 86399
    }

    fun collectionTime(): String {
        return (collected + TimeUnit.DAYS.toMillis(1)).readableDate()
    }

    fun collect(): Int {
        val amount = random.nextInt(500) + 1
        gold += amount
        collected = System.currentTimeMillis() + (1000 * 60 * 24)
        update()
        return amount
    }

    fun update(blocking: Boolean = false) {
        if (!blocking) r.table("users").get(id).update(r.json(gson.toJson(this))).runNoReply(conn)
        else r.table("users").get(id).update(r.json(gson.toJson(this))).run<Any>(conn)
    }

    enum class Gender(val display: String) { MALE("♂"), FEMALE("♀"), UNDEFINED("Not Specified") }
}

fun getMusicLibrary(id: String): DatabaseMusicLibrary {
    var library = asPojo(r.table("musicLibraries").get(id).run(conn), DatabaseMusicLibrary::class.java)
    if (library == null) {
        library = DatabaseMusicLibrary(id, mutableListOf())
        library.update("musicLibraries", id)
    }
    return library
}

fun getPlaylists(id: String): List<DatabaseMusicPlaylist> {
    val playlists = mutableListOf<DatabaseMusicPlaylist>()
    r.table("musicPlaylists").filter { r.hashMap("owner", id) }.run<Any>(conn).queryAsArrayList(DatabaseMusicPlaylist::class.java)
            .forEach { if (it != null) playlists.add(it) }
    return playlists
}

data class StaffMember(val id: String, var role: StaffRole)

enum class StaffRole(val readable: String, val level: Int) {
    TRANSLATOR("Translator", 0), STAFF("Moderator", 1), ADMINISTRATOR("Administrator", 2);

    fun hasPermission(role: StaffRole): Boolean {
        return level >= role.level
    }
}

fun getStaffLevel(id: String): StaffRole? {
    return asPojo(r.table("staff").filter(r.hashMap("id", id)).run(conn), StaffMember::class.java)?.role
}

fun getPatronLevel(id: String): PatronLevel? {
    return asPojo(r.table("patrons").filter(r.hashMap("id", id)).run(conn), Patron::class.java)?.level
}

fun User.hasStaffLevel(role: StaffRole, channel: TextChannel? = null, complain: Boolean = true): Boolean {
    return if (getStaffLevel(id) == role) true
    else {
        if (complain) channel?.send("You need the `{0}` staff role to be able to use this command!".tr(channel, role.readable))
        false
    }
}

enum class PatronLevel(val readable: String, val level: Int) { SUPPORTER("Supporter", 1), PREMIUM("Premium", 3), SPONSOR("Sponsor", 7) }


data class Patron(val id: String, val level: PatronLevel)

/*
fun UserData.getBlackjackData(): BlackjackPlayerData {
    val data = BlackjackPlayerData()
    r.table("BlackjackData").run<Any>(conn).queryAsArrayList(GameDataBlackjack::class.java).forEach { game ->
        if (game != null && game.creator == id) {
            game.rounds.forEach { round ->
                when (round.won) {
                    BlackjackGame.Result.TIED -> data.ties++
                    BlackjackGame.Result.WON -> data.wins++
                    BlackjackGame.Result.LOST -> data.losses++
                }
            }
        }
    }
    return data
}

fun UserData.getConnect4Data(): Connect4PlayerData {
    val data = Connect4PlayerData()
    r.table("Connect_4Data").run<Any>(conn).queryAsArrayList(GameDataConnect4::class.java).forEach { game ->
        if (game != null && (game.loser == id || game.winner == id)) {
            if (game.winner == id) data.wins++
            else data.losses++
        }
    }
    return data
}

fun UserData.getTicTacToeData(): TicTacToePlayerData {
    val data = TicTacToePlayerData()
    r.table("Tic_Tac_ToeData").run<Any>(conn).queryAsArrayList(GameDataTicTacToe::class.java).forEach { game ->
        if (game != null && (game.playerOne == id || game.playerTwo == id)) {
            if (game.winner == null) data.ties++
            else {
                if (game.winner == id) data.wins++
                else data.losses++
            }
        }
    }
    return data
}

fun UserData.getBettingData(): BettingPlayerData {
    val data = BettingPlayerData()
    r.table("BettingData").run<Any>(conn).queryAsArrayList(GameDataBetting::class.java).forEach { game ->
        if (game != null && game.creator == id) {
            game.rounds.forEach { round ->
                if (round.won) {
                    data.wins++
                    data.netWinnings += round.betAmount
                } else {
                    data.losses++
                    data.netWinnings -= round.betAmount
                }
            }
        }
    }
    return data
}

fun UserData.getTriviaData(): TriviaPlayerData {
    val correctByCategory = hashMapOf<String, Pair<Int, Int>>()
    val data = TriviaPlayerData()
    r.table("TriviaData").run<Any>(conn).queryAsArrayList(GameDataTrivia::class.java).forEach { game ->
        if (game != null && (game.winner == id || game.losers.contains(id))) {
            if (game.winner == id) data.wins++
            else data.losses++
            game.rounds.forEach { round ->
                val currentQuestion = round.question
                if (!correctByCategory.containsKey(currentQuestion.category)) correctByCategory.put(currentQuestion.category, Pair(0, 0))
                if (round.winners.contains(id)) {
                    data.questionsCorrect++
                    correctByCategory.replace(currentQuestion.category, Pair(correctByCategory[currentQuestion.category]!!.first + 1, correctByCategory[currentQuestion.category]!!.second))
                } else {
                    data.questionsWrong++
                    correctByCategory.replace(currentQuestion.category, Pair(correctByCategory[currentQuestion.category]!!.first, correctByCategory[currentQuestion.category]!!.second + 1))
                }
            }
        }
    }
    correctByCategory.forEach { category, (first, second) -> data.percentageCorrect.put(category, first.toDouble() / (first + second).toDouble() * 100) }
    data.overallCorrectPercent = (data.questionsCorrect.toDouble() / (data.questionsCorrect + data.questionsWrong).toDouble()) * 100.0
    return data
}

*/