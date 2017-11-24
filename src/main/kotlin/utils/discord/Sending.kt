package utils.discord

import com.vdurmont.emoji.EmojiParser
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel
import translation.tr
import utils.functionality.random
import java.awt.Color

fun Member.embed(title: String, channel: MessageChannel, consumer: ((Message) -> Unit)? = null): EmbedWrapper {
    return EmbedWrapper(channel, consumer).setAuthor(title, "https://ardentbot.com", guild.iconUrl)
            .setColor(Color.getHSBColor(random.nextFloat(), random.nextFloat(), 1f))
            .setFooter("Ardent v2 | ardentbot.com".tr(guild), user.effectiveAvatarUrl) as EmbedWrapper

}

fun MessageChannel.send(message: String) {
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
    } catch (ex: Exception) {
    }
}

class EmbedWrapper(val channel: MessageChannel, val consumer: ((Message) -> Unit)?, vararg val reactions: String) : EmbedBuilder() {
    fun appendDescription(string: String, final: Boolean = false): EmbedWrapper {
        if (descriptionBuilder.length + string.length > 2048) {
            channel.send(this, *reactions)
            setDescription("")
        }
        super.appendDescription(string)
        if (final) channel.send(this, *reactions, consumer = consumer)
        return this
    }

    fun send() {
        channel.send(this, *reactions, consumer = consumer)
    }
}

fun MessageChannel.send(embedBuilder: EmbedBuilder, vararg reactions: String, consumer: ((Message) -> (Unit))? = null) {
    if (type == ChannelType.TEXT) {
        val ch = getTextChannelById(id)
        if (ch != null) if (!ch.guild.selfMember.hasPermission(ch, Permission.MESSAGE_EMBED_LINKS)) return
    }
    sendMessage(embedBuilder.build()).queue { message ->
        reactions.forEach { message.addReaction(EmojiParser.parseToUnicode(it)).queue() }
        consumer?.invoke(message)
    }
}
