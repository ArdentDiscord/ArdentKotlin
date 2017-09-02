package commands.rpg

import commands.games.BlackjackGame
import events.Category
import events.Command
import main.conn
import main.r
import main.waiter
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*
import java.awt.Color
import java.util.*

class Balance : Command(Category.RPG, "bal", "see someone's balance (or yours)", "balance", "money", "gold") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val actionUser = if (event.message.mentionedUsers.size == 0) event.author else event.message.mentionedUsers[0]
        val prefix = event.guild.getPrefix()
        event.channel.send("**${actionUser.withDiscrim()}**'s balance is *${actionUser.getData().gold}* gold\n" +
                "You can use `${prefix}topserver` or `${prefix}top` to compare your balance to others in your server or globally")
    }
}

class Daily : Command(Category.RPG, "daily", "get a daily stipend of gold") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = event.author.getData()
        if (data.canCollect()) event.channel.send("You got **${data.collect()}** gold today!")
        else event.channel.send("You already got your daily today!")
    }
}

class TriviaStats : Command(Category.RPG, "triviastats", "see your or others' trivia stats") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val user = if (event.message.mentionedUsers.size > 0) event.message.mentionedUsers[0] else event.author
        val triviaData = user.getData().triviaData()
        event.channel.send(event.member.embed("${user.name}'s Trivia Stats", Color.CYAN)
                .setThumbnail("https://pbs.twimg.com/profile_images/526480510747299840/L54TjKO2.jpeg")
                .setDescription("Wins: **${triviaData.wins}**\nLosses: **${triviaData.losses}**\n" +
                        "Questions Correct: **${triviaData.questionsCorrect}** of **${triviaData.questionsWrong + triviaData.questionsCorrect}** _(${triviaData.overallCorrectPercent}%)_\n\n" +
                        "**Percentage Won by Category**: \n${triviaData.percentagesFancy()}"))
    }

}

class ProfileCommand : Command(Category.RPG, "profile", "see your or others' profile") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val profiled = if (event.message.mentionedUsers.size == 0) event.author else event.message.mentionedUsers[0]
        val data = profiled.getData()
        val blackjackData = data.blackjackData()
        val bettingData = data.bettingData()
        val triviaData = data.triviaData()
        val spouse = profiled.getMarriage()
        val embed = event.member.embed("${profiled.withDiscrim()}'s Profile")
                .setThumbnail(profiled.effectiveAvatarUrl)
        if (event.author.isStaff()) embed.addField("Staff Member", "True", true)
        else embed.addField("Patron Level", data.donationLevel.readable, true)
        embed.addField("Money", "**${data.gold}** gold", true)
                .addField("Blackjack Stats", "Wins: **${blackjackData.wins}**\nTies: **${blackjackData.ties}**\nLosses: **${blackjackData.losses}**", true)
                .addField("Betting Stats", "Wins: **${bettingData.wins}**\nLosses: **${bettingData.ties}**\nNet Winnings: **${bettingData.netWinnings}** gold", true)
                .addField("Trivia Stats", "**Use ${event.guild.getPrefix()}triviastats @User**", true)
                .addField("Married To", spouse?.withDiscrim() ?: "Nobody :(", true)
        event.channel.send(embed)
    }
}

class MarryCommand : Command(Category.RPG, "marry", "really fond of someone? make a discord marriage!") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val spouse = event.author.getMarriage()
        if (arguments.size == 0 || event.message.mentionedUsers.size == 0) {
            event.channel.send(if (spouse != null) "You're married to **${spouse.asMention}** ${BlackjackGame.Suit.HEART}" else "You're lonely :( Marry someone by " +
                    "typing **${event.guild.getPrefix()}marry @User**!")
            return
        }
        val proposed = event.message.mentionedUsers[0]
        if (proposed.isBot || proposed.id == event.author.id) {
            event.channel.send("You can't marry a bot or yourself :) Get some friends!")
            return
        }
        when {
            spouse != null -> event.channel.send("We live in the 21st century! No polygamic marriages!")
            proposed.getMarriage() != null -> event.channel.send("This person's already married. Sorry :-(")
            else -> {
                event.channel.send("${proposed.asMention}, ${event.author.asMention} is proposing to you! Do you want to accept? Type `yes` to get married or `no` to break " +
                        "their heart")
                waiter.waitForMessage(Settings(proposed.id, event.channel.id, event.guild.id), { response ->
                    if (response.content.startsWith("ye", true)) {
                        if (event.author.getMarriage() != null || proposed.getMarriage() != null) event.channel.send("Unable to create marriage, one of you was just " +
                                "recently married")
                        else {
                            event.channel.send("Congrats, ${event.author.asMention} & ${proposed.asMention}, you've been married ${BlackjackGame.Suit.HEART}")
                            Marriage(event.author.id, proposed.id).insert("marriages")
                        }
                    } else event.channel.send("Ouch, ${event.author.asMention} just got rejected")
                }, {
                    event.channel.send("${proposed.asMention} didn't answer... Try again later ;)")
                }, time = 45)
            }
        }
    }
}

class DivorceCommand : Command(Category.RPG, "divorce", "marriage not working out? get a divorce!") {
    private val random = Random()
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val marriage = event.author.getMarriageModeled()
        if (marriage != null) {
            event.channel.send("Are you sure you want to go through divorce? Half of your gold could go to your ex-spouse as part of your agreement." +
                    "Type `yes` if you understand and want to go through with the divorce, or `no` to cancel")
            waiter.waitForMessage(Settings(event.author.id, event.channel.id, event.guild.id), { response ->
                if (response.content.startsWith("ye", true)) {
                    r.table("marriages").get(marriage.first.id).delete().runNoReply(conn)
                    if (random.nextBoolean()) {
                        val data = event.author.getData()
                        val spouseData = marriage.second!!.getData()
                        spouseData.gold += data.gold / 2
                        data.gold /= 2
                        data.update()
                        spouseData.update()
                        event.channel.send("Half of your net worth was given to ${marriage.second!!.asMention}")
                    }
                    event.channel.send("You successfully divorced")
                } else event.channel.send("Cancelled divorce, but I'd recommend some couple's therapy")
            }, { event.channel.send("Cancelled divorce, but I'd recommend some couple's therapy") })
        } else event.channel.send("You're not married!")
    }
}

class TopMoney : Command(Category.RPG, "top", "see who has the most money in the Ardent database", "topmoney") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        var page = if (arguments.size > 0) arguments[0].toIntOrNull() ?: 1 else 1
        if (page <= 0) page = 1
        val embed = event.member.embed("Global Money Leaderboards | Page $page")
                .setThumbnail("https://bitcoin.org/img/icons/opengraph.png")
        val builder = StringBuilder()
        val top = r.table("playerData").orderBy().optArg("index", r.desc("gold")).slice(((page - 1) * 10))
                .limit(10).run<Any>(conn).queryAsArrayList(PlayerData::class.java)
        top.forEachIndexed { index, playerData ->
            if (playerData != null && playerData.id.toUser() != null) builder.append("${Emoji.SMALL_BLUE_DIAMOND} **${playerData.id.toUser()!!.withDiscrim()}** " +
                    "*${playerData.gold} gold* (#${index + 1 + ((page - 1) * 10)})\n")
        }
        builder.append("\n*You can see different pages by typing ${event.guild.getPrefix()}top __page_number__*")
        event.channel.send(embed.setDescription(builder.toString()))
    }
}

class TopMoneyServer : Command(Category.RPG, "topserver", "see who has the most money in your server", "topmoneyserver") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val page = if (arguments.size > 0) arguments[0].toIntOrNull() ?: 1 else 1
        val embed = event.member.embed("${event.guild.name}'s Money Leaderboards | Page $page")
                .setThumbnail("https://bitcoin.org/img/icons/opengraph.png")
        val builder = StringBuilder()
        val members = hashMapOf<String, Double>()
        event.guild.playerDatas().forEach { members.put(it.id, it.gold) }
        val top = members.sort().toList() as List<Pair<String, Double>>
        try {
            for (includedMember in ((page - 1) * 10)..(((page - 1) * 10) + 10)) {
                val user = top[includedMember].first.toUser()
                if (user != null) {
                    builder.append("${Emoji.SMALL_BLUE_DIAMOND} **${user.withDiscrim()}** " +
                            "*${top[includedMember].second} gold* (#${includedMember + 1})\n")
                }
            }
        } catch (ignored: Exception) {
        }
        builder.append("\n*You can see different pages by typing ${event.guild.getPrefix()}top __page_number__*")
        event.channel.send(embed.setDescription(builder.toString()))
    }
}
