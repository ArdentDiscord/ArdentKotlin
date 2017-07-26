package utils

import main.waiter
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.hooks.SubscribeEvent
import java.util.*
import java.util.function.Consumer
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class EventWaiter : EventListener {
    val executor = Executors.newSingleThreadScheduledExecutor()
    val messageEvents = CopyOnWriteArrayList<Pair<Settings, (Message) -> Unit>>()
    val reactionAddEvents = CopyOnWriteArrayList<Pair<Settings, (MessageReaction) -> Unit>>()
    @SubscribeEvent
    fun onEvent(e: Event) {
        when (e) {
            is GuildMessageReceivedEvent -> messageEvents.forEach {
                mE ->
                val settings = mE.first
                if (settings.channel != null && settings.channel != e.channel.id) return
                if (settings.id != null && settings.id != e.author.id) return
                if (settings.guild != null && settings.guild != e.guild.id) return
                mE.second.invoke(e.message)
                messageEvents.remove(mE)
            }
            is MessageReactionAddEvent -> reactionAddEvents.forEach {
                rAE ->
                val settings = rAE.first
                if (settings.channel != null && settings.channel != e.channel.id) return
                if (settings.id != null && settings.id != e.user.id) return
                if (settings.guild != null && settings.guild != e.guild.id) return
                if (settings.message != null && settings.message != e.messageId) return
                rAE.second.invoke(e.reaction)
                reactionAddEvents.remove(rAE)
            }
        }
    }

    fun waitForReaction(settings: Settings, consumer: (MessageReaction) -> Unit, time: Int = 60, unit: TimeUnit = TimeUnit.SECONDS, silentExpiration: Boolean = false): Pair<Settings, (MessageReaction) -> Unit> {
        val pair = Pair(settings, consumer)
        reactionAddEvents.add(pair)
        executor.schedule({
            if (reactionAddEvents.contains(pair)) {
                reactionAddEvents.remove(pair)
                assert(!silentExpiration)
                val channel: TextChannel? = settings.channel?.toChannel()
                channel?.send(channel.guild.selfMember, "You took too long to add a reaction! [${unit.toSeconds(time.toLong())} seconds]")
            }
        }, time.toLong(), unit)
        return pair
    }

    fun waitForMessage(settings: Settings, consumer: (Message) -> Unit, time: Int = 60, unit: TimeUnit = TimeUnit.SECONDS): Pair<Settings, (Message) -> Unit> {
        val pair = Pair(settings, consumer)
        messageEvents.add(pair)
        executor.schedule({
            if (messageEvents.contains(pair)) {
                messageEvents.remove(pair)
                val channel: TextChannel? = settings.channel?.toChannel()
                channel?.send(channel.guild.selfMember, "You took too long to respond! [${unit.toSeconds(time.toLong())} seconds]")
            }
        }, time.toLong(), unit)
        return pair
    }

    fun cancel(pair: Pair<Settings, (Message) -> Unit>) {
        messageEvents.remove(pair)
    }

}

class Settings(val id: String? = null, val channel: String? = null, val guild: String? = null, val message: String? = null)

fun TextChannel.selectFromList(member: Member, title: String, options: MutableList<String>, consumer: (Int) -> Unit, footerText: String? = null): Message? {
    val embed = embed(title, member)
    val builder = StringBuilder()
            .append("**Please type the number corresponding with the choice you want to select**\n\n")
    for ((index, value) in options.iterator().withIndex()) {
        builder.append("${Emoji.SMALL_BLUE_DIAMOND} **${index + 1}**: _${value}_\n")
    }
    if (footerText != null) builder.append("\n$footerText")

    waiter.waitForMessage(Settings(member.user.id, id, guild.id), { message ->
        val option: Int? = message.rawContent.toIntOrNull()?.minus(1)
        if (option == null || (option < 0 || option >= options.size)) {
            send(member, "You sent an invalid reponse; you had to respond with the **number** of an option")
            return@waitForMessage
        }
        consumer.invoke(option)
    })
    return sendReceive(member, embed.setDescription(builder))
}