package main

import Web
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
import commands.games.questions
import commands.info.*
import commands.music.*
import events.CommandFactory
import events.JoinRemoveEvents
import events.VoiceUtils
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.hooks.AnnotatedEventManager
import okhttp3.OkHttpClient
import org.apache.commons.io.IOUtils
import translation.LanguageCommand
import translation.Translate
import translation.tr
import utils.discord.getGuildById
import utils.discord.getTextChannelById
import utils.discord.getVoiceChannelById
import utils.discord.send
import utils.functionality.EventWaiter
import utils.functionality.TriviaQuestion
import utils.functionality.logChannel
import utils.functionality.queryAsArrayList
import utils.music.LocalTrackObj
import utils.music.ServerQueue
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

val test = true
var beta = true

var hangout: Guild? = null

var r: RethinkDB = RethinkDB.r
var conn: Connection? = null

var config: Config = if (test) Config("C:\\Users\\Adam\\Desktop\\config.txt") else Config("/root/Ardent/config.txt")

var jdas = mutableListOf<JDA>()
val waiter = EventWaiter()
val factory = CommandFactory()

val playerManager = DefaultAudioPlayerManager()
val managers = ConcurrentHashMap<Long, GuildMusicManager>()

val spotifyApi = SpotifyAPI.Builder("79d455af5aea45c094c5cea04d167ac1", config.getValue("spotifySecret")).build()

var transport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
var jsonFactory: JacksonFactory = JacksonFactory.getDefaultInstance()

val sheets: Sheets = setupDrive()
val youtube: YouTube = setupYoutube()

val shards = 2

val httpClient = OkHttpClient()

fun main(args: Array<String>) {
    val spreadsheet = sheets.spreadsheets().values().get("1qm27kGVQ4BdYjvPSlF0zM64j7nkW4HXzALFNcan4fbs", "A2:D").setKey(config.getValue("google"))
            .execute()
    spreadsheet.getValues().forEach { if (it.getOrNull(1) != null && it.getOrNull(2) != null) questions.add(TriviaQuestion(it[1] as String, (it[2] as String).split("~"), it[0] as String, (it.getOrNull(3) as String?)?.toIntOrNull() ?: 125)) }
    Web()
    (1..shards).forEach { sh ->
        jdas.add(JDABuilder(AccountType.BOT)
                .setCorePoolSize(10)
                .setGame(Game.of("Starting up... 418 I'm a teapot"))
                .addEventListener(waiter)
                .addEventListener(factory)
                .addEventListener(JoinRemoveEvents())
                .addEventListener(VoiceUtils())
                .setEventManager(AnnotatedEventManager())
                .useSharding(sh - 1, shards)
                .setToken(config.getValue("token"))
                .buildBlocking())
    }

    logChannel = getTextChannelById("351368131639246848")
    hangout = getGuildById("351220166018727936")

    playerManager.configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.LOW
    playerManager.registerSourceManager(YoutubeAudioSourceManager())
    playerManager.registerSourceManager(SoundCloudAudioSourceManager())
    playerManager.registerSourceManager(HttpAudioSourceManager())
    AudioSourceManagers.registerRemoteSources(playerManager)
    AudioSourceManagers.registerLocalSource(playerManager)

    val administrativeDaemon = AdministrativeDaemon()
    administrativeExecutor.scheduleAtFixedRate(administrativeDaemon, 15, 30, TimeUnit.SECONDS)

    addCommands()

    waiter.executor.schedule({ checkQueueBackups() }, 45, TimeUnit.SECONDS)
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
            conn = r.connection().timeout(3000).db("ardent_v2").hostname("158.69.214.251").port(28015).user("ardent", keys["rethinkdb"]).connect()
        } catch (e: IOException) {
            println("Unable to load Config, exiting now")
            e.printStackTrace()
            System.exit(1)
        }
    }

    fun getValue(keyName: String): String {
        return (keys as Map<String, String>).getOrDefault(keyName, "not_available")
    }
}

fun addCommands() {
    factory.addCommands(Ping(), /* Help(), */
            Invite(), Settings(), About(), Donate(), UserInfo(), ServerInfo(), RoleInfo(),
            UrbanDictionary(), UnixFortune(), EightBall(), FML(), Translate(), IsStreaming(), Status(), Clear(), Automessages(),
            AdministratorCommand(), GiveRoleToAll(), WebsiteCommand(), GetId(), Support(), /* IamCommand(), IamnotCommand(), */
            LanguageCommand(), Blacklist(), Meme())

    // Game Helper Commands
    // factory.addCommands(Decline(), InviteToGame(), Gamelist(), LeaveGame(), JoinGame(), Cancel(), Forcestart(), AcceptInvitation())

    // Statistics Commands
    /*factory.addCommands(CommandDistribution(), ServerLanguagesDistribution(), MusicInfo(), AudioAnalysisCommand(), GetGuilds(),  ShardInfo(), CalculateCommand(),
            MutualGuilds())*/

    // Music Commands
    factory.addCommands(Playlist())
    /* factory.addCommands(Play(), Radio(), Stop(), Pause(), Resume(), SongUrl(), Volume(), Playing(), Repeat(),
            Shuffle(), Queue(), RemoveFrom(), Skip(), Prefix(), Leave(), ClearQueue(), RemoveAt(), ArtistSearch(), FastForward(),
            Rewind()) */

    // Game Commands
    // factory.addCommands(BlackjackCommand(), Connect4Command(), BetCommand(), TriviaCommand(), TicTacToeCommand())

    // RPG Commands
    // factory.addCommands( TopMoney(),ProfileCommand(), MarryCommand(), DivorceCommand(), Daily(), Balance(), TriviaStats())
}

fun checkQueueBackups() {
    val queues = r.table("savedQueues").run<Any>(conn).queryAsArrayList(ServerQueue::class.java)
    queues.forEach { queue ->
        if (queue == null || queue.tracks.isEmpty()) return
        val channel = getVoiceChannelById(queue.voiceId) ?: return
        val textChannel = getTextChannelById(queue.channelId) ?: return
        if (channel.members.size > 1 || (channel.members.size == 1 && channel.members[0] == channel.guild.selfMember)) {
            val manager = channel.guild.getAudioManager(textChannel)
            if (manager.channel != null) {
                if (channel.guild.selfMember.voiceState.channel != channel) channel.connect(textChannel)
                textChannel.send(("**Restarting playback...**... Check out {0} for other cool features we offer in Ardent **Premium**").tr(channel.guild, "<https://ardentbot.com/premium>"))
                queue.tracks.forEach { trackUrl ->
                    trackUrl.load(channel.guild.selfMember, textChannel, { audioTrack, id ->
                        play(manager.channel, channel.guild.selfMember, LocalTrackObj(channel.guild.selfMember.user.id, channel.guild.selfMember.user.id, null, null, null, id, audioTrack))
                    })
                }
                logChannel?.send("Resumed playback in `${channel.guild.name}` - channel `${channel.name}`")
            }
        }
    }
    r.table("savedQueues").delete().runNoReply(conn)
}

fun setupDrive(): Sheets {
    val builder = Sheets.Builder(transport, jsonFactory, null)
    builder.applicationName = "Ardent"
    return builder.build()
}

fun setupYoutube(): YouTube {
    return YouTube.Builder(transport, jsonFactory, null).setApplicationName("Ardent").build()
}
