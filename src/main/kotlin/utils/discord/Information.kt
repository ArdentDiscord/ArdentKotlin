package utils.discord

import commands.music.getAudioManager
import main.conn
import main.factory
import main.jdas
import main.managers
import main.r
import net.dv8tion.jda.api.entities.Guild
import net.dv8tion.jda.api.entities.Member
import net.dv8tion.jda.api.entities.Role
import net.dv8tion.jda.api.entities.User
import translation.Language
import translation.LanguageData
import translation.toLanguage
import translation.translationData
import utils.functionality.getProcessCpuLoad
import utils.functionality.increment
import utils.functionality.queryAsArrayList
import utils.music.LocalTrackObj
import java.lang.management.ManagementFactory
import java.util.ArrayList
import java.util.HashMap

val internals: Internals
    get() = Internals()

enum class EventType { LEFT_GUILD, JOINED_GUILD }

data class LoggedTrack(val guildId: String, val position: Double)

data class LoggedCommand(val commandId: String, val userId: String, val executionTime: Long, val readableExecutionTime: String, val id: String = r.uuid().run(conn))

fun getSelfMember(): Member {
    return getGuildById("351220166018727936")!!.selfMember
}

fun Guild.currentTrack(): LocalTrackObj? {
    return getAudioManager(null).manager.current
}

fun Guild.getUsersData(): MutableList<UserData> {
    val data = mutableListOf<UserData>()
    val ids = members.map { it.user.id }
    r.table("users").run<Any>(conn).queryAsArrayList(UserData::class.java).forEach {
        if (it != null && ids.contains(it.id)) data.add(it)
    }
    return data
}

fun getRoleById(id: String?): Role? {
    if (id == null || id.isEmpty()) return null
    jdas.forEach { jda ->
        val role = jda.getRoleById(id)
        if (role != null) return role
    }
    return null
}

fun String.toRole(guild: Guild): Role? {
    return try {
        guild.getRoleById(this)
    } catch (e: Exception) {
        null
    }
}

fun getAllGuilds(): ArrayList<Guild> {
    val guilds = arrayListOf<Guild>()
    jdas.forEach { guilds.addAll(it.guilds) }
    return guilds
}

fun getAllUsers(): MutableList<User> {
    val users = hashSetOf<User>()
    jdas.forEach { users.addAll(it.users) }
    return users.toMutableList()
}

fun mutualGuildsWith(user: User): MutableList<Guild> {
    val servers = mutableListOf<Guild>()
    jdas.forEach { servers.addAll(it.getMutualGuilds(user)) }
    return servers
}

fun User.toFancyString(): String {
    return "$name#$discriminator"
}

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
        roleCount = 0
        channelCount = 0
        voiceCount = 0
        languageStatuses.clear()
        messagesReceived = factory.messagesReceived.get()
        commandsReceived = factory.commandsReceived().toLong()
        commandCount = factory.commands.size
        commandDistribution = factory.commandsById
        users = getAllUsers().size
        cpuUsage = getProcessCpuLoad()

        val totalRam = Runtime.getRuntime().totalMemory() / 1024 / 1024
        ramUsage = Pair(totalRam - Runtime.getRuntime().freeMemory() / 1024 / 1024, totalRam)

        val tempGuilds = getAllGuilds()
        guilds = tempGuilds.size
        tempGuilds.forEach { guild ->
            roleCount += guild.roles.size
            channelCount += guild.textChannels.size
            voiceCount += guild.voiceChannels.size
        }


        loadedMusicPlayers = 0
        queueLength = 0
        managers.forEach { _, u ->
            queueLength += u.manager.queue.size
            if (u.player.playingTrack != null) {
                queueLength++
                loadedMusicPlayers++
            }
        }
        var tempApiCalls = 0L
        jdas.forEach { tempApiCalls += it.responseTotal }
        apiCalls = tempApiCalls

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
            phrase.translations.forEach { key, _ ->
                val lang = key.toLanguage()
                if (lang != null) tempCount.increment(lang)
            }
        }

        languageStatuses.clear()
        tempCount.forEach { lang, phraseCount -> languageStatuses.put(lang, 100 * phraseCount / totalPhrases.toDouble()) }
        val query = r.table("music").run<Any>(conn).queryAsArrayList(LoggedTrack::class.java)
        tracksPlayed = query.size.toLong()
        var tempMusicPlayed = 0.0
        query.forEach { if (it != null) tempMusicPlayed += it.position }
        musicPlayed = tempMusicPlayed
    }
}