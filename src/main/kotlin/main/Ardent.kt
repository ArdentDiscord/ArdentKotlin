package main

import com.rethinkdb.RethinkDB
import com.rethinkdb.net.Connection
import net.dv8tion.jda.core.AccountType
import net.dv8tion.jda.core.JDA
import net.dv8tion.jda.core.JDABuilder
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import net.dv8tion.jda.core.hooks.AnnotatedEventManager
import com.sedmelluq.discord.lavaplayer.source.AudioSourceManagers
import com.sedmelluq.discord.lavaplayer.source.soundcloud.SoundCloudAudioSourceManager
import com.sedmelluq.discord.lavaplayer.source.youtube.YoutubeAudioSourceManager
import com.sedmelluq.discord.lavaplayer.player.AudioConfiguration
import com.sedmelluq.discord.lavaplayer.player.DefaultAudioPlayerManager
import commands.music.*
import events.Category
import events.Command
import events.CommandFactory
import events.VoiceUtils
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.TextChannel
import utils.*


val version = 1.001

var r = RethinkDB.r
var conn: Connection? = null

var jda: JDA? = null
val waiter = EventWaiter()
val factory = CommandFactory()

val config = Config("C:\\Users\\Adam\\Desktop\\config.txt")

val playerManager = DefaultAudioPlayerManager()
val managers = hashMapOf<Long, GuildMusicManager>()

fun main(args: Array<String>) {
    jda = JDABuilder(AccountType.BOT)
            .setCorePoolSize(10)
            .addEventListener(waiter)
            .addEventListener(factory)
            .addEventListener(VoiceUtils())
            .setEventManager(AnnotatedEventManager())
            .setToken(config.getValue("token"))
            .buildBlocking()

    playerManager.configuration.resamplingQuality = AudioConfiguration.ResamplingQuality.LOW
    playerManager.registerSourceManager(YoutubeAudioSourceManager())
    playerManager.registerSourceManager(SoundCloudAudioSourceManager())

    AudioSourceManagers.registerRemoteSources(playerManager)
    AudioSourceManagers.registerLocalSource(playerManager)

    factory.addCommand(object : Command(Category.INFO, "ping", "what did you think this command was gonna do?") {
        override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
            val currentTime = System.currentTimeMillis()
            event.channel.sendReceive(member, "I'll calculate my ping to Discord using this message")
                    ?.editMessage("**Socket Ping**: *${System.currentTimeMillis() - currentTime} milliseconds*")?.queue()
            waiter.waitForMessage(Settings(event.author.id), { message: Message -> println(message.content) })
        }
    })

    factory.addCommand(Play())
            .addCommand(Radio())
            .addCommand(Stop())
            .addCommand(Pause())
            .addCommand(Resume())
            .addCommand(SongUrl())
}