package commands.`fun`

import com.mb3364.twitch.api.Twitch
import com.mb3364.twitch.api.handlers.ChannelResponseHandler
import com.mb3364.twitch.api.handlers.StreamResponseHandler
import com.mb3364.twitch.api.models.Channel
import com.mb3364.twitch.api.models.Stream
import events.Category
import events.Command
import main.config
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import org.json.JSONObject
import org.jsoup.Jsoup
import translation.tr
import utils.discord.embed
import utils.discord.send
import utils.functionality.*
import utils.web.EightBallResult
import utils.web.UrbanDictionarySearch
import java.awt.Color
import java.net.URLEncoder

class Meme : Command(Category.FUN, "gif", "get a random meme from giphy", "meme", ratelimit = 5) {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send(JSONObject(Jsoup.connect("https://api.giphy.com/v1/gifs/random").data("api_key", config.getValue("giphy"))
                .ignoreContentType(true).get().body().text()).getJSONObject("data").getString("image_url"))
    }

}

class UrbanDictionary : Command(Category.FUN, "urban", "get search results for your favorite words from urban dictionary", "ud") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) event.channel.send("${Emoji.HEAVY_MULTIPLICATION_X} " + "You gotta include a search term".tr(event))
        else {
            val term = arguments.concat()
            val search = gson.fromJson(Jsoup.connect("http://api.urbandictionary.com/v0/define?term=${URLEncoder.encode(term)}").ignoreContentType(true).get()
                    .body().text(), UrbanDictionarySearch::class.java)
            if (search.list.isEmpty()) event.channel.send("There were no results for this term :(".tr(event))
            else {
                val result = search.list[0]
                event.channel.send(event.member!!.embed("Urban Dictionary Definition for {0}".tr(event, term), event.textChannel)
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
}

class UnixFortune : Command(Category.FUN, "unixfortune", "in the mood for a unix fortune? us too", "fortune") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send(Jsoup.connect("http://motd.ambians.com/quotes.php/name/linux_fortunes_random/toc_id/1-1-1")
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .get().getElementsByTag("pre")[0].text())
    }
}

class EightBall : Command(Category.FUN, "8ball", "ask the magical 8 ball your future... or something, idfk") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) event.channel.send("${Emoji.HEAVY_MULTIPLICATION_X} " + "How dare you try to ask the 8-ball an empty question??!!".tr(event))
        else {
            try {
                event.channel.send(gson.fromJson(Jsoup.connect("https://8ball.delegator.com/magic/JSON/${arguments.concat().encode()}")
                        .ignoreContentType(true)
                        .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                        .get().body().text(), EightBallResult::class.java).magic.answer)
            } catch (e: Exception) {
                event.channel.send("Type a valid question, you must!".tr(event.guild))
            }
        }
    }
}

class FML : Command(Category.FUN, "fml", "someone else has had a bad day.") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.send(Jsoup.connect("http://www.fmylife.com/random")
                .userAgent("Mozilla/5.0 (Windows; U; WindowsNT 5.1; en-US; rv1.8.1.6) Gecko/20070725 Firefox/2.0.0.6")
                .ignoreContentType(true).get()
                .getElementsByTag("p")[0].getElementsByTag("a")[0].allElements[0].text())
    }
}

class IsStreaming : Command(Category.FUN, "streaming", "check whether someone is streaming on Twitch and see basic information") {
    val twitch = Twitch()

    init {
        twitch.clientId = config.getValue("twitch")
    }

    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) event.channel.send("Please include the name of a channel - **Example**: `/streaming ardentdiscord`".tr(event))
        else {
            val ch = arguments.concat()
            twitch.channels().get(ch, object : ChannelResponseHandler {
                override fun onSuccess(twitchChannel: Channel) {
                    twitch.streams().get(twitchChannel.name, object : StreamResponseHandler {
                        override fun onSuccess(stream: Stream?) {
                            val embed = event.member!!.embed("{0} on Twitch".tr(event, twitchChannel.name), event.textChannel)
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
}