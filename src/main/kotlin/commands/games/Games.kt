package commands.games
/*
import events.Category
import events.Category.GAMES
import events.Command
import main.test
import main.waiter
import net.dv8tion.jda.api.entities.TextChannel
import net.dv8tion.jda.api.entities.User
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import net.dv8tion.jda.api.events.message.guild.GuildMessageReceivedEvent
import translation.tr
import utils.*
import utils.discord.getData
import utils.discord.send
import utils.discord.toUser
import utils.functionality.*
import java.awt.Color
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit


val invites = ConcurrentHashMap<String, Game>()
val questions = mutableListOf<TriviaQuestion>()

class BetGame(channel: TextChannel, creator: String) : Game(GameType.BETTING, channel, creator, 1, false) {
    private val rounds = mutableListOf<Round>()
    override fun onStart() {
        doRound(creator.toUser()!!)
    }

    private fun doRound(user: User) {
        val data = user.getData()
        channel.send("bet.how_much").apply(data.gold)
        waiter.waitForMessage(Settings(user.id, channel.id), { message ->
            val content = message.contentRaw.removePrefix("/bet ").replace("bet ", "")
            if (content.equals("cancel", true) ){
                channel.send("games.cancel")
                cancel(user)
            } else {
                val bet = if (content.contains("%")) content.removeSuffix("%").toDoubleOrNull()?.div(100)?.times(data.gold)?.toInt() else content.toIntOrNull()
                if (bet != null) {
                    if (bet > data.gold || bet <= 0) {
                        channel.send("bet.invalid_amount")
                        doRound(user)
                        return@waitForMessage
                    } else {
                        channel.selectFromList(channel.guild.getMember(user)!!, "bet.embed_title",
                                mutableListOf("color.black"), "color.red", { selection, _ ->
                            val suit = BlackjackGame.Hand(false, end = false).generate().suit
                            val won = when (suit) {
                                BlackjackGame.Suit.HEART, BlackjackGame.Suit.DIAMOND -> selection == 1
                                else -> selection == 0
                            }
                            if (won) {
                                data.gold += bet
                                channel.send("Congrats, you won - the suit was []! I've added **[] gold** to your profile - new balance: **[] gold**".apply(suit, bet, data.gold.format()))
                            } else {
                                data.gold -= bet
                                channel.send("Sorry, you lost - the suit was [] :( I've removed **[] gold** from your profile - new balance: **[] gold**".apply(suit, bet, data.gold.format()))
                            }
                            .database.update(data)
                            rounds.add(Round(won, bet.toDouble(), suit))
                            channel.send("bet.go_again")

                            Sender.waitForMessage({ it.author.id == user.id && it.guild.id == channel.guild.id && it.channel.id == channel.id }, {
                                if (it.message.contentRaw.startsWith("y", true)) doRound(user)
                                else {
                                    channel.send("bet.end")
                                    val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                    cleanup(gameData)
                                }
                            }, {
                                channel.send("bet.end")
                                val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                cleanup(gameData)
                            })

                        }, failure = {
                            if (rounds.size == 0) {
                                channel.send("games.invalid_response_cancel")
                                cancel(user)
                            } else {
                                channel.send("games.invalid_response_end_game")
                                val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                                cleanup(gameData)
                            }
                        } )
                    }
                } else {
                    if (rounds.size == 0) {
                        channel.send("games.invalid_response_cancel")
                        cancel(user)
                    } else {
                        channel.send("games.invalid_response_end_game")
                        val gameData = GameDataBetting(gameId, creator, startTime!!, rounds)
                        cleanup(gameData)
                    }
                }
            }
        }, { cancel(user) }, time = 20)
    }

    data class Round(val won: Boolean, val betAmount: Double, val suit: BlackjackGame.Suit)

}

class BetCommand : Command(GAMES, "bet", "placeholder") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val member = event.member
        if (member!!.isInGameOrLobby()) event.channel.send("games.already_in_game".tr(event, member.asMention))
        else BetGame(event.textChannel, member.user.id).startEvent()
    }
}
*/