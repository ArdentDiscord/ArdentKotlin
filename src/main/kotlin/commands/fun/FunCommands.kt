package commands.`fun`

import com.mb3364.twitch.api.Twitch
import com.mb3364.twitch.api.handlers.ChannelResponseHandler
import com.mb3364.twitch.api.handlers.StreamResponseHandler
import com.mb3364.twitch.api.models.Channel
import com.mb3364.twitch.api.models.Stream
import events.Category
import events.Command
import main.config
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import org.jsoup.Jsoup
import utils.*
import java.awt.Color
import java.net.URLEncoder
import java.security.SecureRandom

class Roll : Command(Category.FUN, "roll", "use our customizable system to roll dice") {
    val random = SecureRandom()
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val help = """You can use this command with the following format: **sides**x**amount**, where `amount` represents the amount of times
you want to roll the die, and `sides` represents the amount of sides that this die will have.

**Example**: *{0}roll 9x4* - this would roll a 9-sided die 4 times

__Please Note__: You are limited to 999999x10, meaning that at maximum you can roll a **999,999** sided die **10** times""".tr(event, event.guild.getPrefix())
        if (arguments.size == 0) event.channel.send(help)
        else {
            val split /* First element is the side count, Second is the amount of rolls */ = arguments[0].split("x")
            if (split.size == 2) {
                val sides = split[0].toIntOrNull()
                val rolls = split[1].toIntOrNull()
                if (sides == null || sides <= 1 || (sides > 999999 && !event.author.isStaff())) event.channel.send("${Emoji.HEAVY_MULTIPLICATION_X} " + "You specified an invalid amount of sides. Please retry".tr(event))
                else if (rolls == null || rolls <= 0 || (rolls > 10 && !event.author.isStaff())) event.channel.send("${Emoji.HEAVY_MULTIPLICATION_X} " + "You specified an illegal amount of rolls. Please retry".tr(event))
                else {
                    val embed = event.member.embed("Roll Results".tr(event))
                    (1..rolls)
                            .forEach { embed.appendDescription("Roll **{0}**: {1}".tr(event, it, random.nextInt(sides) + 1) + "\n") }
                    event.channel.send(embed.appendDescription("\n" + "Results gathered from a __{0} sided die__".tr(event, sides)))
                }
            } else event.channel.send("Invalid arguments. Please type {0}roll to see how to use this command".tr(event, event.guild.getPrefix()))
        }
    }

    override fun registerSubcommands() {
    }
}

class UrbanDictionary : Command(Category.FUN, "urban", "get search results for your favorite words from urban dictionary", "ud") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) event.channel.send("${Emoji.HEAVY_MULTIPLICATION_X} " + "You gotta include a search term".tr(event))
        else {
            val term = arguments.concat()
            val search = getGson().fromJson(Jsoup.connect("http://api.urbandictionary.com/v0/define?term=${URLEncoder.encode(term)}").ignoreContentType(true).get()
                    .body().text(), UDSearch::class.java)
            if (search.list.isEmpty()) event.channel.send("There were no results for this term :(".tr(event))
            else {
                val result = search.list[0]
                event.channel.send(event.member.embed("Urban Dictionary Definition for {0}".tr(event, term))
                        .setThumbnail("https://i.gyazo.com/6a40e32928743e68e9006396ee7c2a14.jpg")
                        .setColor(Color.decode("#00B7BE"))
                        .addField("Definition".tr(event), result.definition.shortenIf(1024), true)
                        .addField("Example".tr(event), result.example.shortenIf(1024), true)
                        .addField("Thumbs Up".tr(event), result.thumbs_up.toString(), true)
                        .addField("Thumbs Down".tr(event), result.thumbs_down.toString(), true)
                        .addField("Author".tr(event), result.author, true)
                        .addField("Permalink".tr(event), result.permalink, true)
                )
            }
        }
    }

    override fun registerSubcommands() {
    }
}

class UnixFortune : Command(Category.FUN, "unixfortune", "in the mood for a unix fortune? us too", "fortune") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send(Jsoup.connect("http://motd.ambians.com/quotes.php/name/linux_fortunes_random/toc_id/1-1-1")
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .get().getElementsByTag("pre")[0].text())
    }

    override fun registerSubcommands() {
    }
}

class EightBall : Command(Category.FUN, "8ball", "ask the magical 8 ball your future... or something, idfk") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) event.channel.send("${Emoji.HEAVY_MULTIPLICATION_X} " + "How dare you try to ask the 8-ball an empty question??!!".tr(event))
        else {
            try {
                event.channel.send(getGson().fromJson(Jsoup.connect("https://8ball.delegator.com/magic/JSON/${arguments.concat().encode()}")
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .get().body().text(), EightBallResult::class.java).magic.answer)
            } catch (e: Exception) {
                event.channel.send("You need to ask the 8ball a question!".tr(event.guild))
            }
        }
    }

    override fun registerSubcommands() {
    }
}

class FML : Command(Category.FUN, "fml", "someone's had a shitty day.") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send(Jsoup.connect("http://www.fmylife.com/random")
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .ignoreContentType(true).get()
                .getElementsByTag("p")[0].getElementsByTag("a")[0].allElements[0].text())
    }

    override fun registerSubcommands() {
    }
}

class IsStreaming : Command(Category.FUN, "streaming", "check whether someone is streaming on Twitch and see basic information") {
    val twitch = Twitch()

    init {
        twitch.clientId = config.getValue("twitch")
    }

    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) event.channel.send("Please include the name of a channel - **Example**: `{0}streaming ardentdiscord`".tr(event, event.guild.getPrefix()))
        else {
            val ch = arguments.concat()
            twitch.channels().get(ch, object : ChannelResponseHandler {
                override fun onSuccess(twitchChannel: Channel) {
                    twitch.streams().get(twitchChannel.name, object : StreamResponseHandler {
                        override fun onSuccess(stream: Stream?) {
                            val embed = event.member.embed("{0} on Twitch".tr(event, twitchChannel.name))
                                    .addField("Display Name".tr(event), twitchChannel.displayName, true)
                                    .addField("Twitch Link".tr(event), "[Click Here]({0})".tr(event, twitchChannel.url), true)
                            if (stream != null && stream.isOnline) {
                                embed.setColor(Color.GREEN)
                                        .addField("Currently Streaming".tr(event), "yes".tr(event), true)
                                        .addField("Streaming Game".tr(event), stream.game, true)
                                        .addField("Viewers".tr(event), stream.viewers.format(), true)
                                        .addField("Average FPS".tr(event), stream.averageFps.toString(), true)
                                embed.setImage(stream.preview.medium)
                            } else {
                                embed.setColor(Color.RED)
                                        .addField("Currently Streaming".tr(event), "no".tr(event), true)
                                if (twitchChannel.videoBanner == null) embed.setThumbnail(twitchChannel.logo)
                                else embed.setThumbnail(twitchChannel.videoBanner)
                            }
                            embed.addField("Views".tr(event), twitchChannel.views.format(), true)
                                    .addField("Followers".tr(event), twitchChannel.followers.format(), true)
                                    .addField("Creation Date".tr(event), twitchChannel.createdAt.toGMTString(), true)
                                    .addField("Is Partnered?".tr(event), twitchChannel.isPartner.toString(), true)
                                    .addField("Mature Content?".tr(event), twitchChannel.isMature.toString(), true)
                                    .addField("Language".tr(event), twitchChannel.language, true)

                            event.channel.send(embed)
                        }

                        override fun onFailure(failure: Throwable) {
                            event.channel.send("Unable to retrieve channel data. **Reason**: {0}".tr(event, failure.localizedMessage))
                        }

                        override fun onFailure(p0: Int, p1: String?, p2: String?) {
                            onFailure(Exception("Unknown API Error".tr(event)))
                        }
                    })
                }

                override fun onFailure(failure: Throwable) {
                    event.channel.send("Unable to retrieve channel data. **Reason**: {0}".tr(event, failure.localizedMessage))
                }

                override fun onFailure(p0: Int, p1: String?, p2: String?) {
                    onFailure(Exception("Unknown API Error".tr(event)))
                }
            })
        }
    }

    override fun registerSubcommands() {
    }
}