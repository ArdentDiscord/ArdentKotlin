package commands.`fun`

import com.github.vbauer.yta.model.Language
import com.github.vbauer.yta.service.YTranslateApiImpl
import com.mb3364.twitch.api.Twitch
import com.mb3364.twitch.api.handlers.ChannelResponseHandler
import com.mb3364.twitch.api.handlers.StreamResponseHandler
import com.mb3364.twitch.api.handlers.StreamsResponseHandler
import com.mb3364.twitch.api.models.Channel
import com.mb3364.twitch.api.models.Stream
import commands.administrate.staff
import events.Category
import events.Command
import main.config
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import okhttp3.OkHttpClient
import org.jsoup.Jsoup
import utils.*
import java.awt.Color
import java.net.URLEncoder
import java.security.SecureRandom

class Roll : Command(Category.FUN, "roll", "use our customizable system to roll dice") {
    val random = SecureRandom()
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val help = """You can use this command with the following format: **sides**x**amount**, where `amount` represents the amount of times
you want to roll the die, and `sides` represents the amount of sides that this die will have.

**Example**: *${guild.getPrefix()}roll 9x4* - this would roll a 9-sided die 4 times

__Please Note__: You are limited to 999999x10, meaning that at maximum you can roll a **999,999** sided die **10** times"""
        if (arguments.size == 0) channel.send(member, help)
        else {
            val split /* First element is the side count, Second is the amount of rolls */ = arguments[0].split("x")
            if (split.size == 2) {
                val sides = split[0].toIntOrNull()
                val rolls = split[1].toIntOrNull()
                if (sides == null || sides <= 1 || (sides > 999999 && !member.isStaff())) channel.send(member, "${Emoji.HEAVY_MULTIPLICATION_X} You specified an invalid amount of sides. Please retry")
                else if (rolls == null || rolls <= 0 || (rolls > 10 && !member.isStaff())) channel.send(member, "${Emoji.HEAVY_MULTIPLICATION_X} You specified an illegal amount of rolls. Please retry")
                else {
                    val embed = embed("Roll Results", member)
                    (1..rolls)
                            .forEach { embed.appendDescription("Roll **$it**: ${random.nextInt(sides) + 1}\n") }
                    channel.send(member, embed.appendDescription("\nResults gathered from a __$rolls sided die__"))
                }
            } else channel.send(member, "Invalid arguments. Please type ${guild.getPrefix()}roll to see how to use this command")
        }
    }
}

class UrbanDictionary : Command(Category.FUN, "urban", "get search results for your favorite words from urban dictionary", "ud") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) channel.send(member, "${Emoji.HEAVY_MULTIPLICATION_X} Bro, you gotta include a search term")
        else {
            val term = arguments.concat()
            val search = getGson().fromJson(Jsoup.connect("http://api.urbandictionary.com/v0/define?term=${URLEncoder.encode(term)}").ignoreContentType(true).get()
                    .body().text(), UDSearch::class.java)
            if (search.list.isEmpty()) channel.send(member, "There were no results for this term")
            else {
                val result = search.list[0]
                channel.send(member, embed("Urban Dictionary Result: $term", member)
                        .setThumbnail("https://i.gyazo.com/6a40e32928743e68e9006396ee7c2a14.jpg")
                        .setColor(Color.decode("#00B7BE"))
                        .addField("Definition", result.definition.shortenIf(1024), true)
                        .addField("Example", result.example.shortenIf(1024), true)
                        .addField("Thumbs Up", result.thumbs_up.toString(), true)
                        .addField("Thumbs Down", result.thumbs_down.toString(), true)
                        .addField("Author", result.author, true)
                        .addField("Permalink", result.permalink, true)
                )
            }
        }
    }
}

class UnixFortune : Command(Category.FUN, "unixfortune", "in the mood for a unix fortune? us too", "fortune") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val doc = Jsoup.connect("http://motd.ambians.com/quotes" +
                ".php/name/linux_fortunes_random/toc_id/1-1-1").userAgent("Mozilla/5.0 (Windows; U; WindowsNT " +
                "5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").get()!!
        channel.send(member, doc.getElementsByTag("pre")[0].text())
    }
}

class EightBall : Command(Category.FUN, "8ball", "ask the magical 8 ball your future... or something, idfk") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) channel.send(member, "${Emoji.HEAVY_MULTIPLICATION_X} How dare you try to ask the 8-ball an empty question??!!")
        else {
            channel.send(member, getGson().fromJson(Jsoup.connect("https://8ball.delegator.com/magic/JSON/${URLEncoder.encode(arguments.concat())}")
                    .ignoreContentType(true).userAgent("Mozilla/5.0 (Windows; U; WindowsNT " +
                    "5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").get()!!.body().text(), EightBallResult::class.java).magic.answer)
        }
    }
}

class FML : Command(Category.FUN, "fml", "someone's had a shitty day.") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val doc = Jsoup.connect("http://fmylife.com/random").userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; " +
                "rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6").ignoreContentType(true).get()!!
        channel.send(member, doc.getElementsByTag("p")[0].getElementsByTag("a")[0].allElements[0].text())
    }
}

class Translate : Command(Category.FUN, "translate", "translate text to the provided language", "tr") {
    val api = YTranslateApiImpl("trnsl.1.1.20170227T013942Z.6878bfdf518abdf6.a6574733436345112da24eb08e7ee1ef2a0d6a97")
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size < 2) {
            val prefix = guild.getPrefix()
            channel.send(member, """Using the translation command is simple. The format for requesting one is as follows:
**${prefix}translate language_code_here your text goes here**

As follows are the language codes of some languages, but if you don't see the code for the language you want, go to https://ardentbot.com/translation/languages to view a full list.
**English**: en, **French**: fr, **Spanish**: es, **Russian**: ru

**Example**: *${prefix}translate en Bonjour tout le monde!* will return *Hello everyone!*""")
        } else {
            try {
                val code = arguments[0]
                arguments.removeAt(0)
                channel.send(member, api.translationApi().translate(arguments.concat(), Language.of(code)).text()!!)
            } catch (e: Exception) {
                channel.send(member, "You need to include a valid language code! Please visit https://ardentbot.com/translation/languages for a guide")
            }
        }
    }
}

class IsStreaming : Command(Category.FUN, "streaming", "check whether someone is streaming on Twitch and see basic information") {
    val twitch = Twitch()

    init {
        twitch.clientId = config.getValue("twitch")
    }

    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) channel.send(member, "Please include the name of a channel - **Example**: `${guild.getPrefix()}streaming ardentdiscord`")
        else {
            val ch = arguments.concat()
            twitch.channels().get(ch, object : ChannelResponseHandler {
                override fun onSuccess(twitchChannel: Channel) {
                    twitch.streams().get(twitchChannel.name, object : StreamResponseHandler {
                        override fun onSuccess(stream: Stream?) {
                            val embed = embed("${twitchChannel.name} - on Twitch", member)
                                    .addField("Display Name", twitchChannel.displayName, true)
                                    .addField("Twitch Link", "[Click Here](${twitchChannel.url})", true)
                            if (stream != null && stream.isOnline) {
                                embed.setColor(Color.GREEN)
                                        .addField("Currently Streaming", "true", true)
                                        .addField("Streaming Game", stream.game, true)
                                        .addField("Viewers", stream.viewers.format(), true)
                                        .addField("Average FPS", stream.averageFps.toString(), true)
                                embed.setImage(stream.preview.medium)
                            } else {
                                embed.setColor(Color.RED)
                                        .addField("Currently Streaming", "false", true)
                                if (twitchChannel.videoBanner == null) embed.setThumbnail(twitchChannel.logo)
                                else embed.setThumbnail(twitchChannel.videoBanner)
                            }
                            embed.addField("Views", twitchChannel.views.format(), true)
                                    .addField("Followers", twitchChannel.followers.format(), true)
                                    .addField("Creation Date", twitchChannel.createdAt.toGMTString(), true)
                                    .addField("Is Partnered?", twitchChannel.isPartner.toString(), true)
                                    .addField("Mature Content?", twitchChannel.isMature.toString(), true)
                                    .addField("Language", twitchChannel.language, true)

                            channel.send(member, embed)
                        }

                        override fun onFailure(failure: Throwable) {
                            channel.send(member, "Unable to retrieve channel data. **Reason**: ${failure.localizedMessage}")
                        }

                        override fun onFailure(p0: Int, p1: String?, p2: String?) {
                            onFailure(Exception("Unknown API Error"))
                        }
                    })
                }

                override fun onFailure(failure: Throwable) {
                    channel.send(member, "Unable to retrieve channel data. **Reason**: ${failure.localizedMessage}")
                }

                override fun onFailure(p0: Int, p1: String?, p2: String?) {
                    onFailure(Exception("Unknown API Error"))
                }
            })
        }
    }
}