package main

import com.rethinkdb.RethinkDB
import com.rethinkdb.net.Connection
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import commands.`fun`.*
import commands.administrate.*
import commands.games.Games
import commands.info.*
import commands.music.*
import commands.music.Queue
import events.CommandFactory
import events.JoinRemoveEvents
import events.VoiceUtils
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Game
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.hooks.AnnotatedEventManager
import org.apache.commons.io.IOUtils
import utils.EventWaiter
import utils.logChannel
import web.Web
import java.io.File
import java.io.FileReader
import java.io.IOException
import java.util.*

val test = true

var r = RethinkDB.r
var conn: Connection? = null

var jdas = mutableListOf<JDA>()
val waiter = EventWaiter()
val factory = CommandFactory()

var config: Config = if (test) Config("C:\\Users\\Adam\\Desktop\\config.txt") else Config("/root/Ardent/config.txt")

val playerManager = DefaultAudioPlayerManager()
val managers = hashMapOf<Long, GuildMusicManager>()

val shards = 2

fun main(args: Array<String>) {
    Web()
    for (sh in 1..shards) {
        jdas.add(JDABuilder(AccountType.BOT)
                .setCorePoolSize(10)
                .setGame(Game.of("/help ! <3", "https://twitch.tv/ "))
                .addEventListener(waiter)
                .addEventListener(factory)
                .addEventListener(JoinRemoveEvents())
                .addEventListener(VoiceUtils())
                .setEventManager(AnnotatedEventManager())
                .useSharding(sh - 1, shards)
                .setToken(config.getValue("token"))
                .buildBlocking())
    }
    jdas.forEach {
        val logCh: TextChannel? = it.getTextChannelById("345226134532784129")
        if (logCh != null) logChannel = logCh
    }
    playerManager.configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.LOW
    playerManager.registerSourceManager(YoutubeAudioSourceManager())
    playerManager.registerSourceManager(SoundCloudAudioSourceManager())
    playerManager.useRemoteNodes(config.getValue("node1"), config.getValue("node2"))
    AudioSourceManagers.registerRemoteSources(playerManager)
    AudioSourceManagers.registerLocalSource(playerManager)

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
            .addCommand(FixMusic())
            .addCommand(Nono())
            .addCommand(GiveAll())
            .addCommand(WebsiteCommand())
            .addCommand(GetId())
            .addCommand(Support())

    startAdministrativeDaemon()
    println("Successfully set up. Essentially ready to receive commands (daemon commencement could delay this a few seconds)!")
}

class Config(url: String) {
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
