package commands.`fun`

import events.Category
import events.Command
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
                if (sides == null || sides <= 1 || sides > 999999) channel.send(member, "${Emoji.HEAVY_MULTIPLICATION_X} You specified an invalid amount of sides. Please retry")
                else if (rolls == null || rolls <= 0 || rolls > 10) channel.send(member, "${Emoji.HEAVY_MULTIPLICATION_X} You specified an illegal amount of rolls. Please retry")
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