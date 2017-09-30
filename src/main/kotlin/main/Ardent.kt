package main

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.google.api.services.youtube.YouTube
import com.rethinkdb.RethinkDB
import com.rethinkdb.net.Connection
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.http.HttpAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import commands.`fun`.*
import commands.administrate.*
import commands.games.*
import commands.info.*
import commands.info.Settings
import commands.music.*
import commands.rpg.*
import commands.statistics.CommandDistribution
import commands.statistics.MusicInfo
import commands.statistics.MutualGuilds
import commands.statistics.ServerLanguagesDistribution
import events.CommandFactory
import events.JoinRemoveEvents
import events.VoiceUtils
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.hooks.AnnotatedEventManager
import org.apache.commons.io.IOUtils
import translation.LanguageCommand
import translation.Translate
import utils.*
import web.Web
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.concurrent.TimeUnit

val test = true

var hangout: Guild? = null

var r: RethinkDB = RethinkDB.r
var conn: Connection? = null

var jdas = mutableListOf<JDA>()
val waiter = EventWaiter()
val factory = CommandFactory()

var config: Config = if (test) Config("C:\\Users\\Adam\\Desktop\\config.txt") else Config("/root/Ardent/config.txt")

val playerManager = DefaultAudioPlayerManager()
val managers = hashMapOf<Long, GuildMusicManager>()

val spotifyApi = SpotifyAPI.Builder("79d455af5aea45c094c5cea04d167ac1", config.getValue("spotifySecret")).build()

var transport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
var jsonFactory: JacksonFactory = JacksonFactory.getDefaultInstance()

val sheets: Sheets = setupDrive()
val youtube: YouTube = setupYoutube()

fun main(args: Array<String>) {
    val spreadsheet = sheets.spreadsheets().values().get("1qm27kGVQ4BdYjvPSlF0zM64j7nkW4HXzALFNcan4fbs", "A2:D").setKey(config.getValue("google"))
            .execute()
    spreadsheet.getValues().forEach { if (it.getOrNull(1) != null && it.getOrNull(2) != null) questions.add(TriviaQuestion(it[1] as String, (it[2] as String).split("~"), it[0] as String, (it.getOrNull(3) as String?)?.toIntOrNull() ?: 125)) }
    Web()
    val shards = 2
    waiter.executor.execute {
        (1..shards).forEach { sh ->
            jdas.add(JDABuilder(AccountType.BOT)
                    .setCorePoolSize(10)
                    .setGame(Game.of("Try out /lang", "https://twitch.tv/ "))
                    .addEventListener(waiter)
                    .addEventListener(factory)
                    .addEventListener(JoinRemoveEvents())
                    .addEventListener(VoiceUtils())
                    .setEventManager(AnnotatedEventManager())
                    .useSharding(sh - 1, shards)
                    .setToken(config.getValue("token"))
                    .buildBlocking())
        }
        logChannel = "351368131639246848".toChannel()
        hangout = getGuildById("351220166018727936")
    }

    registerAudioSettings()
    addCommands()
    startAdministrativeDaemon()

    waiter.executor.scheduleWithFixedDelay({
        jdas.forEach { jda ->
            jda.presence.game = Game.of(
                    when (random.nextInt(7)) {
                        0 -> "Serving ${internals.guilds.format()} guilds"
                        1 -> "Serving ${internals.users.format()} users"
                        2 -> "${internals.loadedMusicPlayers} servers playing music"
                        3 -> "Fluent in 3 languages"
                        4 -> "Try out /lang"
                        5 -> "${internals.commandCount} available commands"
                        else -> "Adam Approved ${Emoji.COPYRIGHT_SIGN}"
                    }, "https://twitch.tv/ ")
        }
    }, 20, 25, TimeUnit.SECONDS)
    waiter.executor.schedule({ checkQueueBackups() }, 60, TimeUnit.SECONDS)
}

/**
 * Config class represents a text file with the following syntax on each line: KEY :: VALUE
 */
data class Config(val url: String) {
    private val keys: MutableMap<String, String> = hashMapOf()

    init {
        try {
            val keysTemp = IOUtils.readLines(FileReader(File(url)))
            keysTemp.forEach { pair ->
                val keyPair = pair.split(" :: ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (keyPair.size == 2) keys.put(keyPair[0], keyPair[1])
            }
        } catch (e: IOException) {
            println("Unable to load Config, exiting now")
            e.printStackTrace()
            System.exit(1)
        }
        conn = r.connection().timeout(5000).db("ardent").hostname("158.69.214.251").port(28015).user("ardent", keys["rethinkdb"]).connect()
    }

    fun getValue(keyName: String): String {
        return (keys as Map<String, String>).getOrDefault(keyName, "not_available")
    }
}

fun addCommands() {
    factory.addCommands(Play(), Radio(), Stop(), Pause(), Resume(), SongUrl(), Ping(), Help(), Volume(), Playing(), Repeat(),
            Shuffle(), Queue(), RemoveFrom(), Skip(), Prefix(), Leave(), Decline(), InviteToGame(), Gamelist(), LeaveGame(),
            JoinGame(), Cancel(), Forcestart(), Invite(), Settings(), About(), Donate(), UserInfo(), ServerInfo(), RoleInfo(), Roll(),
            UrbanDictionary(), UnixFortune(), EightBall(), FML(), Translate(), IsStreaming(), Status(), Clear(), Tempban(), Automessages(),
            Mute(), Unmute(), Punishments(), Nono(), GiveRoleToAll(), WebsiteCommand(), GetId(), Support(), ClearQueue(), WebPanel(), IamCommand(),
            IamnotCommand(), BlackjackCommand(), Connect4Command(), BetCommand(), TriviaCommand(), TopMoney(), MutualGuilds(), ProfileCommand(),
            MarryCommand(), DivorceCommand(), Daily(), Balance(), AcceptInvitation(), TriviaStats(), RemoveAt(), SlotsCommand(), ArtistSearch(),
            LanguageCommand(), TicTacToeCommand(), CommandDistribution(), GuessTheNumberCommand(), ServerLanguagesDistribution(), MusicInfo(), FastForward(),
            Rewind())
}

/**
 * Procedurally go through the "queue" table and resume playback for guilds.
 */
fun checkQueueBackups() {
    val queues = r.table("queues").run<Any>(conn).queryAsArrayList(QueueModel::class.java)
    queues.forEach {
        if (it != null) {
            val channel = it.channelId?.toChannel()
            val voiceChannel = getVoiceChannelById(it.voiceId)
            if (voiceChannel != null && voiceChannel.members.size > 0) {
                val guild = getGuildById(it.guildId)
                val manager = guild?.getGuildAudioPlayer(channel)
                if (guild != null && manager != null && it.music.size > 0) {
                    voiceChannel.connect(channel)
                    Thread.sleep(1250)
                    channel?.send("**I'm now restoring your queue**... If you appreciate Ardent & its features, take a second and pledge a few dollars at {0} - we'd really appreciate it".tr(guild, "<https://patreon.com/ardent>"))
                    it.music.forEach { trackUri -> trackUri.load(guild.selfMember, null, null, guild = guild) }
                    println("Successfully resumed playback for ${guild.name}")
                }
            }
        }
    }
    r.table("queues").delete().runNoReply(conn)
}

fun registerAudioSettings() {
    playerManager.configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.LOW
    playerManager.registerSourceManager(YoutubeAudioSourceManager())
    playerManager.registerSourceManager(SoundCloudAudioSourceManager())
    playerManager.registerSourceManager(HttpAudioSourceManager())
    AudioSourceManagers.registerRemoteSources(playerManager)
    AudioSourceManagers.registerLocalSource(playerManager)
}

fun setupDrive(): Sheets {
    val builder = Sheets.Builder(transport, jsonFactory, null)
    builder.applicationName = "Ardent"
    return builder.build()
}

fun setupYoutube(): YouTube {
    return YouTube.Builder(transport, jsonFactory, null).setApplicationName("Ardent").build()
}
