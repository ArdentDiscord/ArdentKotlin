package utils

import main.factory
import main.waiter
import net.dv8tion.jda.core.entities.*
import net.dv8tion.jda.core.events.Event
import net.dv8tion.jda.core.events.message.guild.GuildMessageReceivedEvent
import net.dv8tion.jda.core.events.message.react.MessageReactionAddEvent
import net.dv8tion.jda.core.hooks.SubscribeEvent
import java.util.*
import java.util.concurrent.ConcurrentLinkedDeque
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


class EventWaiter : EventListener {
    val executor = Executors.newScheduledThreadPool(50)
    val gameEvents = CopyOnWriteArrayList<Triple<String, Long, Pair<((Message) -> Unit) /* ID of channel, Long MS of expiration */, (() -> Unit)?>>>()
    val messageEvents = CopyOnWriteArrayList<Pair<Settings, (Message) -> Unit>>()
    val reactionAddEvents = CopyOnWriteArrayList<Pair<Settings, (MessageReaction) -> Unit>>()

    @SubscribeEvent
    fun onEvent(e: Event) {
        factory.executor.execute {
            when (e) {
                is GuildMessageReceivedEvent -> {
                    if (e.author.isBot) return@execute
                    messageEvents.forEach { mE ->
                        var cont: Boolean = true
                        val settings = mE.first
                        if (settings.channel != null && settings.channel != e.channel.id) cont = false
                        if (settings.id != null && settings.id != e.author.id) cont = false
                        if (settings.guild != null && settings.guild != e.guild.id) cont = false
                        if (cont) {
                            mE.second.invoke(e.message)
                            messageEvents.remove(mE)
                        }
                    }
                    gameEvents.forEach { game ->
                        if (e.channel.id == game.first) {
                            if (System.currentTimeMillis() < game.second) {
                                factory.executor.execute { game.third.first(e.message) }
                            } else gameEvents.remove(game)
                        }
                    }
                }
                is MessageReactionAddEvent -> reactionAddEvents.forEach { rAE ->
                    val settings = rAE.first
                    var cont = true
                    if (settings.channel != null && settings.channel != e.channel.id) cont = false
                    else if (settings.id != null && settings.id != e.user.id) cont = false
                    else if (settings.guild != null && settings.guild != e.guild.id) cont = false
                    else if (settings.message != null && settings.message != e.messageId) cont = false
                    if (cont) {
                        rAE.second.invoke(e.reaction)
                        reactionAddEvents.remove(rAE)
                    }
                }
            }
        }
    }

    fun waitForReaction(settings: Settings, consumer: (MessageReaction) -> Unit, expirationConsumer: (() -> Unit)? = null, time: Int = 60, unit: TimeUnit = TimeUnit.SECONDS, silentExpiration: Boolean = false): Pair<Settings, (MessageReaction) -> Unit> {
        val pair = Pair(settings, consumer)
        reactionAddEvents.add(pair)
        executor.schedule({
            if (reactionAddEvents.contains(pair)) {
                reactionAddEvents.remove(pair)
                if (expirationConsumer != null) expirationConsumer.invoke()
                else {
                    if (!silentExpiration) {
                        val channel: TextChannel? = settings.channel?.toChannel()
                        channel?.send("You took too long to add a reaction! [${unit.toSeconds(time.toLong())} seconds]")
                    }
                }
            }
        }, time.toLong(), unit)
        return pair
    }

    fun gameChannelWait(channel: String, consumer: (Message) -> Unit, expirationConsumer: (() -> Unit)? = null, time: Int = 10, unit: TimeUnit = TimeUnit.SECONDS) {
        val game = Triple(channel, System.currentTimeMillis() + unit.toMillis(time.toLong()), Pair(consumer, expirationConsumer))
        gameEvents.add(game)
        executor.schedule({
            if (gameEvents.contains(game)) game.third.second?.invoke()
        }, time.toLong(), unit)
    }

    fun waitForMessage(settings: Settings, consumer: (Message) -> Unit, expiration: (() -> Unit)? = null, time: Int = 20, unit: TimeUnit = TimeUnit.SECONDS, silentExpiration: Boolean = false) {
        val pair = Pair(settings, consumer)
        messageEvents.add(pair)
        executor.schedule({
            if (messageEvents.contains(pair)) {
                messageEvents.remove(pair)
                val channel: TextChannel? = settings.channel?.toChannel()
                if (expiration == null && !silentExpiration) channel?.send("You took too long to respond! [${unit.toSeconds(time.toLong())} seconds]")
                expiration?.invoke()
            }
        }, time.toLong(), unit)
    }

    fun cancel(pair: Pair<Settings, (Message) -> Unit>) {
        messageEvents.remove(pair)
        // TODO("add expiration consumer invocation here")
    }

}

data class Settings(val id: String? = null, val channel: String? = null, val guild: String? = null, val message: String? = null)

fun MessageChannel.selectFromList(member: Member, title: String, options: MutableList<String>, consumer: (Int) -> Unit, footerText: String? = null, failure: (() -> Unit)? = null) {
    val embed = member.embed(title)
    val builder = StringBuilder()
    for ((index, value) in options.iterator().withIndex()) {
        builder.append("${Emoji.SMALL_BLUE_DIAMOND} **${index + 1}**: $value\n")
    }
    if (footerText != null) builder.append("\n$footerText\n")
    builder.append("\n__Please select the number corresponding with the choice that you'd like to select__\n")
    sendMessage(embed.setDescription(builder).build()).queue { message ->
        for (x in 1..options.size) {
            message.addReaction(when (x) {
                1 -> Emoji.KEYCAP_DIGIT_ONE
                2 -> Emoji.KEYCAP_DIGIT_TWO
                3 -> Emoji.KEYCAP_DIGIT_THREE
                4 -> Emoji.KEYCAP_DIGIT_FOUR
                5 -> Emoji.KEYCAP_DIGIT_FIVE
                6 -> Emoji.KEYCAP_DIGIT_SIX
                7 -> Emoji.KEYCAP_DIGIT_SEVEN
                8 -> Emoji.KEYCAP_DIGIT_EIGHT
                9 -> Emoji.KEYCAP_DIGIT_NINE
                10 -> Emoji.KEYCAP_TEN
                else -> Emoji.HEAVY_CHECK_MARK
            }.symbol).queue()
        }
        waiter.waitForReaction(Settings(member.user.id, id, member.guild.id), { messageReaction ->
            val chosen = when (messageReaction.emote.name) {
                Emoji.KEYCAP_DIGIT_ONE.symbol -> 1
                Emoji.KEYCAP_DIGIT_TWO.symbol -> 2
                Emoji.KEYCAP_DIGIT_THREE.symbol -> 3
                Emoji.KEYCAP_DIGIT_FOUR.symbol -> 4
                Emoji.KEYCAP_DIGIT_FIVE.symbol -> 5
                Emoji.KEYCAP_DIGIT_SIX.symbol -> 6
                Emoji.KEYCAP_DIGIT_SEVEN.symbol -> 7
                Emoji.KEYCAP_DIGIT_EIGHT.symbol -> 8
                Emoji.KEYCAP_DIGIT_NINE.symbol -> 9
                Emoji.KEYCAP_TEN.symbol -> 10
                else -> 69999999
            } - 1
            if (chosen in 0..(options.size - 1)) consumer.invoke(chosen)
            else send("You specified an invalid reaction, cancelling selection")
        }, time = 25, silentExpiration = false)
    }
}