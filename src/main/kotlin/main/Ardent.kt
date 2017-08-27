package main

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.http.HttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.services.sheets.v4.Sheets
import com.rethinkdb.RethinkDB
import com.rethinkdb.net.Connection
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.wrapper.spotify.Api
import commands.`fun`.*
import commands.administrate.*
import commands.games.*
import commands.info.*
import commands.music.*
import commands.music.Queue
import commands.rpg.*
import events.CommandFactory
import events.JoinRemoveEvents
import events.VoiceUtils
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.hooks.AnnotatedEventManager
import org.apache.commons.io.IOUtils
import utils.EventWaiter
import utils.TriviaQuestion
import utils.getGuildById
import utils.logChannel
import web.Web
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*

val test = false

var hangout: Guild? = null

var r: RethinkDB = RethinkDB.r
var conn: Connection? = null

var jdas = mutableListOf<JDA>()
val waiter = EventWaiter()
val factory = CommandFactory()

var config: Config = if (test) Config("C:\\Users\\Adam\\Desktop\\config.txt") else Config("/root/Ardent/config.txt")

val playerManager = DefaultAudioPlayerManager()
val managers = hashMapOf<Long, GuildMusicManager>()

val spotifyApi: Api = Api.builder().clientId("79d455af5aea45c094c5cea04d167ac1").clientSecret(config.getValue("spotifySecret"))
        .redirectURI("https://ardentbot.com").build()

val shards = 2

val DATA_STORE_DIR = File(System.getProperty("user.home"), ".credentials/sheets.googleapis.com.json")
var transport: HttpTransport = GoogleNetHttpTransport.newTrustedTransport()
var jsonFactory: JacksonFactory = JacksonFactory.getDefaultInstance()

val sheets: Sheets = setupDrive()

fun main(args: Array<String>) {
    val spreadsheet = sheets.spreadsheets().values().get("1qm27kGVQ4BdYjvPSlF0zM64j7nkW4HXzALFNcan4fbs", "A2:D").setKey(config.getValue("google"))
            .execute()
    spreadsheet.getValues().forEach { if (it.getOrNull(1) != null) questions.add(TriviaQuestion(it[1] as String, (it[2] as String).split("~"), it[0] as String, (it.getOrNull(3) as String?)?.toIntOrNull() ?: 50)) }
    Web()
    for (sh in 1..shards) {
        jdas.add(JDABuilder(AccountType.BOT)
                .setCorePoolSize(10)
                .setGame(Game.of("Try out /trivia!", "https://twitch.tv/ "))
                .addEventListener(waiter)
                .addEventListener(factory)
                .addEventListener(JoinRemoveEvents())
                .addEventListener(VoiceUtils())
                .setEventManager(AnnotatedEventManager())
                .useSharding(sh - 1, shards)
                .setToken(config.getValue("token"))
                .buildBlocking())
    }

    hangout = getGuildById("351220166018727936")

    jdas.forEach {
        val logCh: TextChannel? = it.getTextChannelById("351368131639246848")
        if (logCh != null) logChannel = logCh
    }
    playerManager.configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.LOW
    playerManager.registerSourceManager(YoutubeAudioSourceManager())
    playerManager.registerSourceManager(SoundCloudAudioSourceManager())
    playerManager.useRemoteNodes(config.getValue("node1"), config.getValue("node2"))
    AudioSourceManagers.registerRemoteSources(playerManager)
    AudioSourceManagers.registerLocalSource(playerManager)

    addCommands()
    startAdministrativeDaemon()
    println("Successfully set up. Essentially ready to receive commands (daemon commencement could delay this a few seconds)!")
}

data class Config(val url: String) {
    private val keys: MutableMap<String, String>

    init {
        keys = HashMap<String, String>()
        try {
            val keysTemp = IOUtils.readLines(FileReader(File(url)))
            keysTemp.forEach { pair ->
                val keyPair = pair.split(" :: ".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
                if (keyPair.size == 2) keys.put(keyPair[0], keyPair[1])
            }
        } catch (e: IOException) {
            println("Unable to load Config....")

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
    factory.addCommand(Play())
            .addCommand(Radio())
            .addCommand(Stop())
            .addCommand(Pause())
            .addCommand(Resume())
            .addCommand(SongUrl())
            .addCommand(Ping())
            .addCommand(Help())
            .addCommand(Volume())
            .addCommand(Playing())
            .addCommand(Repeat())
            .addCommand(Shuffle())
            .addCommand(Queue())
            .addCommand(RemoveFrom())
            .addCommand(Skip())
            .addCommand(Prefix())
            .addCommand(Leave())
            .addCommand(Games())
            .addCommand(Decline())
            .addCommand(InviteToGame())
            .addCommand(Gamelist())
            .addCommand(LeaveGame())
            .addCommand(JoinGame())
            .addCommand(Cancel())
            .addCommand(Forcestart())
            .addCommand(Invite())
            .addCommand(Settings())
            .addCommand(About())
            .addCommand(Donate())
            .addCommand(UserInfo())
            .addCommand(ServerInfo())
            .addCommand(RoleInfo())
            .addCommand(Roll())
            .addCommand(UrbanDictionary())
            .addCommand(UnixFortune())
            .addCommand(EightBall())
            .addCommand(FML())
            .addCommand(Translate())
            .addCommand(IsStreaming())
            .addCommand(Status())
            .addCommand(Clear())
            .addCommand(Tempban())
            .addCommand(Automessages())
            .addCommand(Mute())
            .addCommand(Unmute())
            .addCommand(Punishments())
            .addCommand(Nono())
            .addCommand(GiveAll())
            .addCommand(WebsiteCommand())
            .addCommand(GetId())
            .addCommand(Support())
            .addCommand(ClearQueue())
            .addCommand(WebPanel())
            .addCommand(IamCommand())
            .addCommand(IamnotCommand())
            .addCommand(BlackjackCommand())
            .addCommand(BetCommand())
            .addCommand(TriviaCommand())
            .addCommand(TopMoney())
            .addCommand(TopMoneyServer())
            .addCommand(ProfileCommand())
            .addCommand(MarryCommand())
            .addCommand(DivorceCommand())
            .addCommand(Daily())
            .addCommand(Balance())
            .addCommand(AcceptInvitation())
            .addCommand(TriviaStats())
}

fun setupDrive(): Sheets {
    val builder = Sheets.Builder(transport, jsonFactory, null)
    builder.applicationName = "Ardent"
    return builder.build()
}