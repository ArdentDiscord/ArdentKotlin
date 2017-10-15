package utils.discord

import com.vdurmont.emoji.EmojiParser
import net.dv8tion.jda.core.EmbedBuilder
import net.dv8tion.jda.core.Permission
import net.dv8tion.jda.core.entities.ChannelType
import net.dv8tion.jda.core.entities.Message
import net.dv8tion.jda.core.entities.MessageChannel


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

class EmbedWrapper(val embedBuilder: EmbedBuilder, val channel: MessageChannel, vararg val reactions: String, val consumer: ((Message) -> Unit)?) {
    fun appendDescription(string: String, final: Boolean = false) {
        if (embedBuilder.descriptionBuilder.length + string.length > 2048) {
            channel.send(embedBuilder, *reactions, consumer = consumer)
            embedBuilder.setDescription("")
        }
        embedBuilder.appendDescription(string)
        if (final) channel.send(embedBuilder, *reactions, consumer = consumer)
    }
    fun send() {
        channel.send(embedBuilder, *reactions, consumer = consumer)
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
