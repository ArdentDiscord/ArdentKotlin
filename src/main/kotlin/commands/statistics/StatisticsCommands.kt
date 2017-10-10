package commands.statistics

import com.udojava.evalex.Expression
import commands.music.getCurrentTime
import commands.music.getGuildAudioPlayer
import events.Category
import events.Command
import main.*
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import translation.ArdentLanguage
import translation.Languages
import utils.*
import java.awt.Color

class MusicInfo : Command(Category.STATISTICS, "musicinfo", "see how many servers we're currently serving with music", "minfo") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val embed = event.member.embed("Ardent | Music Status".tr(event), Color.MAGENTA)
                .setThumbnail("https://yt3.ggpht.com/-zK8v1xKkZtY/AAAAAAAAAAI/AAAAAAAAAAA/SmyGR2XCwXw/s900-c-k-no-mo-rj-c0xffffff/photo.jpg")
        var total = 0
        managers.forEachIndexed { index, id, manager ->
            val guild = getGuildById(id.toString())
            if (manager.player.playingTrack != null && guild != null) {
                var lengthHours = 0.0
                r.table("musicPlayed").filter(r.hashMap("guildId", guild.id)).run<Any>(conn).queryAsArrayList(PlayedMusic::class.java).forEach {
                    if (it != null) lengthHours += it.position
                }
                embed.appendDescription((if (index % 2 == 0) Emoji.SMALL_ORANGE_DIAMOND else Emoji.SMALL_BLUE_DIAMOND).symbol + " " +
                        ("**{0}**:\n" +
                                "           Now Playing: *{1}* {2}\n" +
                                "           Queue Length: *{3}*\n" +
                                "           Total playback: *{4} hours, {5} minutes*")
                                .replace("{0}", guild.name)
                                .replace("{1}", manager.player.playingTrack.info.title)
                                .replace("{2}", manager.player.playingTrack.getCurrentTime())
                                .replace("{3}", manager.scheduler.manager.queue.size.toString())
                                .replace("{4}", lengthHours.toInt().format())
                                .replace("{5}", lengthHours.toMinutes().format())
                        + "\n")
                total++
            }
        }
        embed.appendDescription("\n" + "**Total Players Active**: {0}".replace("{0}", total.toString()))
        embed.appendDescription("\n\n" + "**" + "Total Music Played".tr(event) + ":** " + "{0} hours, {1} minutes".tr(event, internals.musicPlayed.toInt().format(), internals.musicPlayed.toMinutes()))
        embed.appendDescription("\n" + "**Total Tracks Played**: {0}".tr(event, internals.tracksPlayed.format()))
        event.channel.send(embed)
    }

    override fun registerSubcommands() {
    }
}

class ServerLanguagesDistribution : Command(Category.STATISTICS, "serverlangs", "see how many Ardent servers are using which bot locale", "glangs", "slangs") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        var langs = hashMapOf<ArdentLanguage, Int /* Usages */>()
        val guilds = r.table("guilds").run<Any>(conn).queryAsArrayList(GuildData::class.java)
        guilds.forEach { data ->
            if (data != null) {
                if (data.language == null) {
                    data.language = Languages.ENGLISH.language
                    data.update()
                }
                if (langs.containsKey(data.language!!)) langs.incrementValue(data.language!!)
                else langs.put(data.language!!, 1)
            }
        }
        langs = langs.sort(true) as HashMap<ArdentLanguage, Int>
        val embed = event.member.embed("Ardent | Server Languages".tr(event))
        langs.forEachIndexed { index, l, usages ->
            embed.appendDescription((if (index % 2 == 0) Emoji.SMALL_BLUE_DIAMOND else Emoji.SMALL_ORANGE_DIAMOND).symbol +
                    " **${l.readable}**: *$usages servers* (${"%.2f".format(usages * 100 / guilds.size.toFloat())}%)\n")
        }
        event.channel.send(embed)
    }

    override fun registerSubcommands() {
    }
}

class CalculateCommand : Command(Category.STATISTICS, "calculate", "evaluate a mathematical expression", "calculate", "calc") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) event.channel.send("Please type an expression to evaluate. You can also use functions like **SQRT(NUMBER)** as defined at {0}"
                .tr(event, "<https://github.com/uklimaschewski/EvalEx#supported-functions>"))
        else {
            try {
                event.channel.send(Expression(arguments.concat()).eval().toPlainString())
            }
            catch(e: Exception) {
                event.channel.send("The expression you entered is invalid!".tr(event))
            }
        }
    }

    override fun registerSubcommands() {
    }
}

class ShardInfo : Command(Category.STATISTICS, "shards", "see specific detail about each Ardent shard", "shar", "shard") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val embed = event.member.embed("Ardent | Shard Information")
        jdas.forEach { jda ->
            embed.appendDescription("${Emoji.SMALL_BLUE_DIAMOND} __Shard **${jda.shardInfo.shardId}**__\n" +
                    "       Guilds: *${jda.guilds.size}*\n" +
                    "       Users: *${jda.users.size}*\n" +
                    "       Commands Received: *${factory.commandsByShard[jda.shardInfo.shardId] ?: "None"}*\n" +
                    "       Ping: *${jda.ping}* ms\n\n")
        }
        event.channel.send(embed)
    }

    override fun registerSubcommands() {
    }
}

class CommandDistribution : Command(Category.STATISTICS, "distribution", "see how commands have been distributed on Ardent", "commanddistribution", "cdist") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.sendMessage("Generating a command distribution overview could take up to **2** minutes... I'll delete this message once it's done".tr(event)).queue {
            val isOverall = !arguments.isEmpty()
            val data =
                    if (arguments.size == 0) factory.commandsById.sort(true)
                    else {
                        val temp = hashMapOf<String, Int>()
                        r.table("commands").run<Any>(conn).queryAsArrayList(LoggedCommand::class.java).forEach { if (it != null) if (temp.containsKey(it.commandId)) temp.incrementValue(it.commandId) else temp.put(it.commandId, 1) }
                        temp.sort(true)
                    }
            val embed = event.member.embed((if (isOverall) "Ardent | Lifetime Command Distribution" else "Ardent | Current Session Command Distribution").tr(event))
            embed.setThumbnail("https://www.wired.com/wp-content/uploads/blogs/magazine/wp-content/images/18-05/st_thompson_statistics_f.jpg")
            var total = 0
            data.forEach { total += it.value }
            embed.appendDescription("Data generated from **{0}** individual entries".tr(event, total.format()))
            val iterator = data.iterator().withIndex()
            while (iterator.hasNext()) {
                val current = iterator.next()
                if (embed.descriptionBuilder.length >= 1900) {
                    embed.appendDescription("\n\n" + "Type *{0}distribution all* to see the total command distribution".tr(event, event.guild.getPrefix()))
                    event.channel.send(embed)
                    embed.setDescription("")
                } else {
                    embed.appendDescription("\n   " + (if (current.index % 2 == 0) Emoji.SMALL_ORANGE_DIAMOND else Emoji.SMALL_BLUE_DIAMOND).symbol
                            + " " + "{0}: **{1}**% ({2} commands) - [**{3}**]".tr(event, current.value.key, "%.2f".format(current.value.value * 100 / total.toFloat()), current.value.value.format(), "#" + (current.index + 1).toString()))
                }
            }
            if (embed.descriptionBuilder.isNotEmpty()) {
                embed.appendDescription("\n\n" + "Type *{0}distribution all* to see the total command distribution".tr(event, event.guild.getPrefix()))
                event.channel.send(embed)
            }
            it.delete().queue()
        }
    }

    override fun registerSubcommands() {
    }
}

class GetGuilds : Command(Category.STATISTICS, "guilds", "get a hastebin paste of servers", "servers") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val builder = StringBuilder().append("Ardent Server Data, collected at ${System.currentTimeMillis().readableDate()}\n\n")
        guilds().sortedByDescending { it.members.size }.forEach { guild -> builder.append("${guild.name} - ${guild.members.size} members & ${guild.botSize()} bots\n") }
        event.channel.send("Click the following link to see server data:".tr(event) + " ${paste(builder.toString().removeSuffix("\n"))}")
    }

    override fun registerSubcommands() {
    }
}

class MutualGuilds : Command(Category.STATISTICS, "mutualguilds", "get a list of servers I'm in with a specified user", "mutualservers") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val user = if (event.message.mentionedUsers.size == 0) event.author else event.message.mentionedUsers[0]
        if (user.id == "339101087569281045") event.channel.send("Nice try :-)".tr(event))
        val embed = event.member.embed("Ardent | Mutual Servers with ${user.name}")
        getMutualGuildsWith(user).forEachIndexed { index, guild ->
            if (embed.descriptionBuilder.length < 1900) embed.appendDescription("${(if (index % 2 == 0) Emoji.SMALL_ORANGE_DIAMOND else Emoji.SMALL_BLUE_DIAMOND).symbol} " +
                    "**${guild.name}** - *${guild.members.size}* members, *${guild.members.filter { it.user.isBot }.count() * 100 / guild.members.size}*% bots\n")
            else {
                if (!embed.descriptionBuilder.endsWith("...")) embed.appendDescription("...")
            }
        }
        event.channel.send(embed)
    }

    override fun registerSubcommands() {
    }
}

class AudioAnalysisCommand : Command(Category.STATISTICS, "trackanalysis", "see an audio feature analysis for tracks", "audioanalysis") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        showHelp(event)
    }

    override fun registerSubcommands() {
        with("current", null, "see an analysis for the currently playing track", { arguments, event ->
            val playing = event.guild.getGuildAudioPlayer(event.textChannel).player.playingTrack
            if (playing == null) event.channel.send("There isn't a currently playing track..".tr(event))
            else {
                val embed = getAnalysis(playing.info.title, event)
                if (embed != null) event.channel.send(embed)
            }
        })
        with("search", null, "see an analysis for the specified search term", { arguments, event ->
            if (arguments.size == 0) event.channel.send("You need a search term!".tr(event))
            else {
                val embed = getAnalysis(arguments.concat(), event)
                if (embed != null) event.channel.send(embed)
            }
        })
    }

    private fun getAnalysis(trackName: String, event: MessageReceivedEvent): EmbedBuilder? {
        try {
            val track = spotifyApi.search.searchTrack(trackName, 1).items[0]
            val features = spotifyApi.tracks.getAudioFeatures(track.id)
            return event.member.embed("Audio Analysis | {0}".tr(event, track.name))
                    .addField("Acousticness", features.acousticness.times(100).format() + "%", true)
                    .addField("Energy", features.energy.times(100).format() + "%", true)
                    .addField("Liveness", features.liveness.times(100).format() + "%", true)
                    .addField("Danceability", features.danceability.times(100).format() + "%", true)
                    .addField("Instrumentalness", features.instrumentalness.times(100).format() + "%", true)
                    .addField("Loudness", features.loudness.times(100).format() + "%", true)
                    .addField("Speechiness", features.speechiness.times(100).format() + "%", true)
                    .addField("Valence", features.valence.times(100).format() + "%", true)
                    .addField("Tempo", features.tempo.format() + " bpm", true)
                    .addField("Duration", features.duration_ms.toLong().formatMinSec(), true)
                    .addField("Track Link", "https://open.spotify.com/track/${features.id}", true)
                    .addField("Analysis URL", features.analysis_url, true)

        } catch (e: Exception) {
            event.channel.send("A track wasn't found by the name of **{0}**!".tr(event, trackName))
        }
        return null
    }
}