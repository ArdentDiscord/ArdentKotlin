package commands.rpg

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
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val actionUser = if (event.message.mentionedUsers.size == 0) event.author else event.message.mentionedUsers[0]
        val prefix = event.guild.getPrefix()
        event.channel.send("**{0}**'s balance is *{1}* gold\nYou can use `{2}topserver` or `{2}top` to compare your balance to others in your server or globally".tr(event, actionUser.withDiscrim(), actionUser.getData().gold.toString(), prefix))
    }

    override fun registerSubcommands() {
    }
}

class Daily : Command(Category.RPG, "daily", "get a daily stipend of gold") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = event.author.getData()
        if (data.canCollect()) event.channel.send("You got **{0}** gold today!".tr(event, data.collect()))
        else event.channel.send("You already got your daily today!".tr(event) + " " + "You'll next be able to redeem it at **{0}**".tr(event, data.collectionTime()))
    }

    override fun registerSubcommands() {
    }
}

class TriviaStats : Command(Category.RPG, "triviastats", "see your or others' trivia stats") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val user = if (event.message.mentionedUsers.size > 0) event.message.mentionedUsers[0] else event.author
        val triviaData = user.getData().triviaData()
        event.channel.send(event.member.embed("${user.name}'s Trivia Stats", Color.CYAN)
                .setThumbnail("https://pbs.twimg.com/profile_images/526480510747299840/L54TjKO2.jpeg")
                .setDescription("Wins: **${triviaData.wins}**\nLosses: **${triviaData.losses}**\n" +
                        "Questions Correct: **${triviaData.questionsCorrect}** of **${triviaData.questionsWrong + triviaData.questionsCorrect}** _(${triviaData.overallCorrectPercent.toInt()}%)_\n\n" +
                        "**Percentage Won by Category**: \n${triviaData.percentagesFancy()}"))
    }

    override fun registerSubcommands() {
    }
}

class ProfileCommand : Command(Category.RPG, "profile", "see your or others' profile") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val profiled = if (event.message.mentionedUsers.size == 0) event.author else event.message.mentionedUsers[0]
        val data = profiled.getData()
        val blackjackData = data.blackjackData()
        val bettingData = data.bettingData()
        val slotsData = data.slotsData()
        val connect4Data = data.connect4Data()
        val ticTacToeData = data.ticTacToeData()
        val spouse = profiled.getMarriage()
        val embed = event.member.embed("${profiled.withDiscrim()}'s Profile")
                .setThumbnail("https://robohash.org/${profiled.id}")
        if (profiled.isStaff()) embed.addField("Ardent Status", "Staff Member", true)
        else embed.addField("Patron Level", data.donationLevel.readable, true)
        embed.addField("Money", "**${data.gold}** gold", true)
                .addField("Blackjack Stats", "Wins: **${blackjackData.wins}**\nTies: **${blackjackData.ties}**\nLosses: **${blackjackData.losses}**", true)
                .addField("Betting Stats", "Wins: **${bettingData.wins}**\nLosses: **${bettingData.ties}**\nNet Winnings: **${bettingData.netWinnings}** gold", true)
                .addField("Slots Stats", "Wins: **${slotsData.wins}**\nLosses: **${slotsData.losses}**\nNet Winnings: **${slotsData.netWinnings}** gold", true)
                .addField("Connect 4 Stats", "Wins: **${connect4Data.wins}**\nLosses: **${connect4Data.losses}**", true)
                .addField("Tic Tac Toe Stats", "Wins: **${ticTacToeData.wins}**\nTies: **${ticTacToeData.ties}**\nLosses: **${ticTacToeData.losses}**", true)
                .addField("Trivia Stats", "**Use ${event.guild.getPrefix()}triviastats @User**", true)
                .addField("Married To", spouse?.withDiscrim() ?: "Forever a bachelor :)", true)
        event.channel.send(embed)
    }

    override fun registerSubcommands() {
    }
}

class MarryCommand : Command(Category.RPG, "marry", "really fond of someone? make a discord marriage!") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val spouse = event.author.getMarriage()
        if (arguments.size == 0 || event.message.mentionedUsers.size == 0) {
            event.channel.send(if (spouse != null) "You're married to **{0}** ♥".tr(event, spouse.asMention) else "You're lonely :( Marry someone by typing **{0}marry @User**!".tr(event, event.guild.getPrefix()))
            return
        }
        val proposed = event.message.mentionedUsers[0]
        if (proposed.isBot || proposed.id == event.author.id) {
            event.channel.send("You can't marry a bot or yourself :) Get some friends!".tr(event))
            return
        }
        when {
            spouse != null -> event.channel.send("We live in the 21st century! No polygamic marriages!".tr(event))
            proposed.getMarriage() != null -> event.channel.send("This person's already married. Sorry :-(".tr(event))
            else -> {
                event.channel.send("{0}, {1} is proposing to you! Do you want to accept? Type `yes` to get married or `no` to break their heart".tr(event, proposed.asMention, event.author.asMention))
                waiter.waitForMessage(Settings(proposed.id, event.channel.id, event.guild.id), { response ->
                    if (response.content.startsWith("ye", true)) {
                        if (event.author.getMarriage() != null || proposed.getMarriage() != null) event.channel.send("Unable to create marriage, one of you was just recently married".tr(event))
                        else {
                            event.channel.send("Congrats, {0} and {1}, you've been married ♥".tr(event, event.author.asMention, proposed.asMention))
                            Marriage(event.author.id, proposed.id).insert("marriages")
                        }
                    } else event.channel.send("Ouch, {0} just got rejected".tr(event, event.author.asMention))
                }, {
                    event.channel.send("{0} didn't answer... Try again later ;)".tr(event, proposed.asMention))
                }, time = 45)
            }
        }
    }

    override fun registerSubcommands() {
    }
}

class DivorceCommand : Command(Category.RPG, "divorce", "marriage not working out? get a divorce!") {
    private val random = Random()
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val marriage = event.author.getMarriageModeled()
        if (marriage != null) {
            event.channel.send("Are you sure you want to go through divorce? Half of your gold could go to your ex-spouse as part of your agreement.\n\nType `yes` if you understand and want to go through with the divorce, or `no` to cancel".tr(event))
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
                        event.channel.send("Half of your net worth was given to {0}".tr(event, marriage.second!!.asMention))
                    }
                    event.channel.send("You successfully divorced".tr(event))
                } else event.channel.send("Cancelled divorce, but I'd recommend some couple's therapy".tr(event))
            }, { event.channel.send("Cancelled divorce, but I'd recommend some couple's therapy".tr(event)) })
        } else event.channel.send("You're not married!".tr(event))
    }

    override fun registerSubcommands() {
    }
}

class TopMoney : Command(Category.RPG, "top", "see who has the most money in the Ardent database", "topmoney", "topserver") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        showHelp(event)
    }

    override fun registerSubcommands() {
        with("global", null, null, { arguments, event ->
            var page = if (arguments.size > 0) arguments[0].toIntOrNull() ?: 1 else 1
            if (page <= 0) page = 1
            val embed = event.member.embed("Global Money Leaderboards | Page {0}".tr(event, page))
                    .setThumbnail("https://bitcoin.org/img/icons/opengraph.png")
            val builder = StringBuilder()
            val top = r.table("playerData").orderBy().optArg("index", r.desc("gold")).slice(((page - 1) * 10))
                    .limit(10).run<Any>(conn).queryAsArrayList(PlayerData::class.java)
            top.forEachIndexed { index, playerData ->
                if (playerData != null && playerData.id.toUser() != null) builder.append("${Emoji.SMALL_BLUE_DIAMOND} **${playerData.id.toUser()!!.withDiscrim()}** " +
                        "*${playerData.gold} gold* (#${index + 1 + ((page - 1) * 10)})\n")
            }
            builder.append("\n").append("*You can see different pages by typing {0}top __page_number__*".tr(event, event.guild.getPrefix()))
            event.channel.send(embed.setDescription(builder.toString()))
        })
        with("server", null, null, { arguments, event ->
            val page = if (arguments.size > 0) arguments[0].toIntOrNull() ?: 1 else 1
            val embed = event.member.embed("{0}'s Money Leaderboards | Page {1}".tr(event, event.guild.name, page))
                    .setThumbnail("https://bitcoin.org/img/icons/opengraph.png")
            val builder = StringBuilder()
            val members = hashMapOf<String, Double>()
            event.guild.playerDatas().forEach { members.put(it.id, it.gold) }
            val top = members.sort().toList()
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
            builder.append("\n").append("*You can see different pages by typing {0}topserver __page_number__*".tr(event, event.guild.getPrefix()))
            event.channel.send(embed.setDescription(builder.toString()))
        })
    }
}