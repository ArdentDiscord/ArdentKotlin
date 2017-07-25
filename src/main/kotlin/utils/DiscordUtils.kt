package utils

import com.google.gson.Gson
import net.dv8tion.jda.core.exceptions.PermissionException
import com.rethinkdb.net.Cursor
import com.vdurmont.emoji.EmojiParser
import main.conn
import main.jda
import main.r
import main.version
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.MessageBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.*
import org.json.simple.JSONObject
import utils.Config.ip
import java.awt.Color
import java.util.*
import java.util.ArrayList
import java.util.HashMap

private val random = Random()
private val gsons = listOf(Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson(), Gson())

fun String.toChannel() : TextChannel? {
    return jda?.getTextChannelById(this)
}

fun List<String>.concat() : String {
    val builder = StringBuilder()
    forEach { builder.append("$it ") }
    return builder.removeSuffix(" ").toString()
}

fun Member.hasOverride() : Boolean {
    return hasPermission(Permission.MANAGE_CHANNEL)
}

fun String.getChannel(): TextChannel? {
    return jda!!.getTextChannelById(this)
}

fun Member.withDiscrim(): String {
    return this.user.withDiscrim()
}

fun User.withDiscrim(): String {
    return "$name#$discriminator"
}

fun embed(title: String, member: Member, color: Color = Color.GREEN): EmbedBuilder {
    return EmbedBuilder().setAuthor(title, "https://ardentbot.com", member.guild.selfMember.user.avatarUrl)
            .setColor(color)
            .setFooter("Served ${member.withDiscrim()} with Ardent version $version", member.user.avatarUrl)
}

fun Guild.getData(): GuildData {
    val guildData: GuildData? = asPojo(r.table("guilds").get(this.id).run(conn), GuildData::class.java)
    if (guildData != null) return guildData
    val data = GuildData("/", MusicSettings(true, true), mutableListOf<String>())
    data.insert("guilds")
    return data
}

fun Any.insert(table: String) {
    r.table(table).insert(r.json(getGson().toJson(this))).runNoReply(conn)
}

fun <T> asPojo(map: HashMap<*, *>?, tClass: Class<T>): T? {
    return getGson().fromJson(JSONObject.toJSONString(map), tClass)
}

fun <T> queryAsArrayList(t: Class<T>, o: Any): ArrayList<T?> {
    val cursor = o as Cursor<HashMap<*, *>>
    val tS = ArrayList<T?>()
    cursor.forEach { hashMap -> tS.add(asPojo(hashMap, t)) }
    return tS
}

fun getGson(): Gson {
    return gsons[random.nextInt(gsons.size)]
}

fun MessageChannel.sendReceive(member: Member, message: String): Message? {
    try {
        return this.sendMessage(message).complete()
    } catch (ex: PermissionException) {
        sendFailed(member.user, false)
    }
    return null
}

fun TextChannel.send(member: Member, embedBuilder: EmbedBuilder) {
    send(member.user, embedBuilder)
}

fun TextChannel.send(user: User, embedBuilder: EmbedBuilder) {
    try {
        sendMessage(embedBuilder.build()).queue()
    }
    catch (e : Exception) {
        sendFailed(user, true)
    }
}

fun MessageChannel.send(member: Member, message: String) {
    this.send(member.user, message)
}

fun MessageChannel.send(user: User, message: String) {
    try {
        if (message.length <= 2000) {
            this.sendMessage(message).queue()
            return
        }
        var i = 0
        while (i < message.length) {
            if (i + 2000 <= message.length) {
                this.sendMessage(message.substring(i, i + 2000)).queue()
            } else {
                this.sendMessage(message.substring(i, message.length - 1)).queue()
            }
            i += 2000
        }
    } catch (ex: PermissionException) {
        sendFailed(user, false)
    }
}

fun sendEmbed(embedBuilder: EmbedBuilder, channel: TextChannel, user: User, vararg reactions: String): Message {
    try {
        val message = channel.sendMessage(embedBuilder.build()).complete()
        for (reaction in reactions) {
            message.addReaction(EmojiParser.parseToUnicode(reaction)).queue()
        }
        return message
    } catch (ex: PermissionException) {
        sendFailed(user, true)
    }
    return MessageBuilder().build()
}

fun sendFailed(user: User, embed: Boolean) {
    user.openPrivateChannel().queue { privateChannel ->
        try {
            if (!embed) {
                privateChannel.sendMessage("I don't have permission to type in this channel!").queue()
            } else {
                privateChannel.sendMessage("I don't have permission to send embeds in this channel!").queue()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}

fun Guild.getPrefix(): String {
    return "/"
}

enum class DonationLevels {
    SUBSCRIBER, SUBSCRIBER_PLUS, DIAMOND
}

fun Guild.getDonationLevel(): DonationLevels {
    return DonationLevels.DIAMOND
}