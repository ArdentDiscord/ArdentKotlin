package utils

import commands.games.*
import main.*
import net.dv8tion.jda.core.entities.User
import obj.SpotifyPublicUser
import translation.*
import java.lang.management.ManagementFactory
import java.util.*
import java.util.concurrent.TimeUnit


data class SanitizedTriviaRound(val hasWinner: Boolean, val winner: User?, val losers: List<User?>, val question: TriviaQuestion)
data class SanitizedTrivia(val creator: User?, val id: Long?, val winner: User?, val losers: List<User?>, val scores: List<Pair<String, Int>>, val rounds: List<SanitizedTriviaRound>)

data class SanitizedGame(val user: String, val endTime: String, val type: String, val url: String)

data class SimpleLoggedEvent(val guildId: String, val eventType: EventType, val time: Long = System.currentTimeMillis(), val id: String = r.uuid().run(conn))


enum class DonationLevel(val readable: String, val level: Int) {
    NONE("None", 1), SUPPORTER("Supporter", 2), BASIC("Basic", 3), INTERMEDIATE("Intermediate", 4), EXTREME("Extreme", 5);

    override fun toString(): String {
        return readable
    }
}

enum class EventType { LEFT_GUILD, JOINED_GUILD }

data class PlayedMusic(val guildId: String, val position: Double)

data class TriviaQuestion(val question: String, val answers: List<String>, val category: String, val value: Int)

data class Marriage(var userOne: String, var userTwo: String, val id: String = r.uuid().run(conn))
data class Patron(var id: String, var donationLevel: DonationLevel)

data class Reminder(val userId: String, val creation: Long, val end: Long, val content: String, val repeat: Boolean = false, val id: String = r.uuid().run(conn))
data class SpecialPerson(var id: String, var backer: String)
data class Announcement(val date: String, val dateLong: Long, val writer: User, val content: String)
data class AnnouncementModel(var date: Long, var writer: String, var content: String) {
    fun toAnnouncement(): Announcement {
        return Announcement(date.readableDate(), date, writer.toUser()!!, content)
    }
}

data class QueueModel(val guildId: String, val voiceId: String, val channelId: String?, val music: MutableList<String /* URI */>)
data class ProofreadPhrase(val original: ArdentPhraseTranslation, val phrase: String, val hasChecker: Boolean, val suggestions: MutableList<String> = mutableListOf(), var suggestionString: String = "", var hasSuggestions: Boolean = true)
data class GuildData(val id: String, var prefix: String?, var musicSettings: MusicSettings, var advancedPermissions: MutableList<String>, var iamList: MutableList<Iam> = mutableListOf(),
                     var joinMessage: Pair<String?, String? /* Message then Channel ID */>? = null, var leaveMessage: Pair<String?, String?>? = null,
                     var defaultRole: String? = null, var allowGlobalOverride: Boolean = false, var languageData: LanguageData?, var blacklistedUsers: MutableList<String>?,
                     var blacklistedRoles: MutableList<String>?, var blacklistedChannels: MutableList<String>?)

data class Iam(var name: String, var roleId: String)
data class MusicSettings(var announceNewMusic: Boolean = false, var singleSongInQueueForMembers: Boolean = false, var membersCanMoveBot: Boolean = true,
                         var membersCanSkipSongs: Boolean = false, var autoQueueSongs: Boolean = false, var stayInChannel: Boolean? = false)

data class UDSearch(val tags: List<String>, val result_type: String, val list: List<UDResult>, val sounds: List<String>)

data class UDResult(val definition: String, val permalink: String, val thumbs_up: Int, val author: String, val word: String,
                    val defid: String, val current_vote: String, val example: String, val thumbs_down: Int)

data class EightBallResult(val magic: Magic)
data class Magic /* The name was not my choice...... */(val question: String, val answer: String, val type: String)

class Punishment(val userId: String, val punisherId: String, val guildId: String, val type: Type, val expiration: Long, val start: Long = System.currentTimeMillis(), val id: String = r.uuid().run(conn)) {
    enum class Type {
        TEMPBAN, MUTE;

        override fun toString(): String {
            return if (this == TEMPBAN) "temp-banned" else "muted"
        }
    }
}

data class UserProfile(val id: String, val points: Int = 0, val likes: UserLikes, val gender: Int = 0, val accounts: ConnectedAccounts, val userPlaylists: List<UserPlaylist>) {
    fun getLevel(): Int {
        var level = 1
        var tempPoints = points
        while (tempPoints > 0) {
            if (tempPoints - (level * 1000) >= 0) {
                level++
                tempPoints -= level * 1000
            }
        }
        return level
    }
}

data class UserLikes(val food: Emoji? = null, val sport: Emoji? = null, val languageData: LanguageData = Language.ENGLISH.data)
data class ConnectedAccounts(val spotify: SpotifyPublicUser? = null)
data class UserPlaylist(val tracks: List<String /* URL */>)

class PlayerData(val id: String, var donationLevel: DonationLevel, var gold: Double = 50.0, var collected: Long = 0, val reminders: MutableList<Reminder> = mutableListOf()) {
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

    fun blackjackData(): BlackjackPlayerData {
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

    fun connect4Data(): Connect4PlayerData {
        val data = Connect4PlayerData()
        r.table("Connect_4Data").run<Any>(conn).queryAsArrayList(GameDataConnect4::class.java).forEach { game ->
            if (game != null && (game.loser == id || game.winner == id)) {
                if (game.winner == id) data.wins++
                else data.losses++
            }
        }
        return data
    }

    fun ticTacToeData(): TicTacToePlayerData {
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


    fun bettingData(): BettingPlayerData {
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

    fun slotsData(): SlotsPlayerData {
        val data = SlotsPlayerData()
        r.table("SlotsData").run<Any>(conn).queryAsArrayList(GameDataSlots::class.java).forEach { game ->
            if (game != null && game.creator == id) {
                game.rounds.forEach { round ->
                    if (round.won) {
                        data.wins++
                        data.netWinnings += round.bet
                    } else {
                        data.losses++
                        data.netWinnings -= round.bet
                    }
                }
            }
        }
        return data
    }

    fun triviaData(): TriviaPlayerData {
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
}

data class LoggedCommand(val commandId: String, val userId: String, val executionTime: Long, val readableExecutionTime: String, val id: String = r.uuid().run(conn))

class Internals {
    var messagesReceived: Long = 0
    var commandsReceived: Long = 0
    var commandCount: Int = 0
    var commandDistribution: HashMap<String, Int> = hashMapOf()
    var guilds: Int = 0
    var users: Int = 0
    var cpuUsage: Double = 0.0
    var ramUsage: Pair<Long /* Used RAM in MB */, Long /* Available RAM in MB */> = Pair(0, 0)
    var roleCount: Long = 0
    var channelCount: Long = 0
    var voiceCount: Long = 0
    var loadedMusicPlayers: Int = 0
    var queueLength: Int = 0
    var uptime: Long = 0
    var uptimeFancy: String = ""
    var apiCalls: Long = 0
    var musicPlayed: Double = 0.0
    var tracksPlayed: Long = 0
    val languageStatuses = hashMapOf<LanguageData, Double>()

    init {
        waiter.executor.scheduleWithFixedDelay({
            loadedMusicPlayers = 0
            queueLength = 0
            apiCalls = 0
            roleCount = 0
            channelCount = 0
            voiceCount = 0
            languageStatuses.clear()
            messagesReceived = factory.messagesReceived.get()
            commandsReceived = factory.commandsReceived().toLong()
            commandCount = factory.commands.size
            commandDistribution = factory.commandsById
            guilds = utils.guilds().size
            users = users().size
            cpuUsage = getProcessCpuLoad()
            val totalRam = Runtime.getRuntime().totalMemory() / 1024 / 1024
            ramUsage = Pair(totalRam - Runtime.getRuntime().freeMemory() / 1024 / 1024, totalRam)
            guilds().forEach { guild ->
                roleCount += guild.roles.size
                channelCount += guild.textChannels.size
                voiceCount += guild.voiceChannels.size
            }
            managers.forEach { _, u ->
                queueLength += u.scheduler.manager.queue.size
                if (u.player.playingTrack != null) {
                    queueLength++
                    loadedMusicPlayers++
                }
            }
            jdas.forEach { apiCalls += it.responseTotal }
            uptime = ManagementFactory.getRuntimeMXBean().uptime
            val seconds = (uptime / 1000) % 60
            val minutes = (uptime / (1000 * 60)) % 60
            val hours = (uptime / (1000 * 60 * 60)) % 24
            val days = (uptime / (1000 * 60 * 60 * 24))
            val builder = StringBuilder()
            if (days == 1.toLong()) builder.append("$days day, ")
            else if (days > 1.toLong()) builder.append("$days days, ")

            if (hours == 1.toLong()) builder.append("$hours hour, ")
            else if (hours > 1.toLong()) builder.append("$hours hours, ")

            if (minutes == 1.toLong()) builder.append("$minutes minute, ")
            else if (minutes > 1.toLong()) builder.append("$minutes minutes, ")

            if (seconds == 1.toLong()) builder.append("$minutes second")
            else builder.append("$seconds seconds")
            uptimeFancy = builder.toString()

            val totalPhrases = translationData.phrases.size
            val tempCount = hashMapOf<LanguageData, Int>()
            Language.values().forEach { tempCount.put(it.data, 0) }
            translationData.phrases.forEach { _, phrase ->
                phrase.translations.forEach { key, value ->
                    val lang = key.toLanguage()
                    if (lang != null) tempCount.incrementValue(lang)
                }
            }

            tempCount.forEach { lang, phraseCount -> languageStatuses.put(lang, 100 * phraseCount / totalPhrases.toDouble()) }

            val query = r.table("musicPlayed").run<Any>(conn).queryAsArrayList(PlayedMusic::class.java)
            tracksPlayed = query.size.toLong()
            var tempMusicPlayed = 0.0
            query.forEach { if (it != null) tempMusicPlayed += it.position }
            musicPlayed = tempMusicPlayed
        }, 0, 3, TimeUnit.SECONDS)
    }
}