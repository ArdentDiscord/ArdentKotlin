package commands.rpg

import commands.games.BlackjackGame
import events.Category
import events.Command
import main.conn
import main.r
import main.waiter
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*
import java.util.*

class Balance : Command(Category.RPG, "bal", "see someone's balance (or yours)", "balance", "money", "gold") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val actionUser = if (event.message.mentionedUsers.size == 0) member.user else event.message.mentionedUsers[0]
        val prefix = guild.getPrefix()
        channel.send(member, "**${actionUser.withDiscrim()}**'s balance is *${actionUser.getData().gold}* gold\n" +
                "You can use __${prefix}topserver__ or __${prefix}top__ to compare your balance to others in your server or globally")
    }
}

class Daily : Command(Category.RPG, "daily", "get a daily stipend of gold") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = member.data()
        if (data.canCollect()) channel.send(member, "You got **${data.collect()}** gold today!")
        else channel.send(member, "You'll be able to use this command at **${data.collectionTime()}**")
    }
}

class ProfileCommand : Command(Category.RPG, "profile", "see your or others' profile") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val profiled = if (event.message.mentionedUsers.size == 0) member.user else event.message.mentionedUsers[0]
        val data = profiled.getData()
        val blackjackData = data.blackjackData()
        val bettingData = data.bettingData()
        val coinflipData = data.coinflipData()
        val spouse = profiled.getMarriage()
        val embed = embed("${profiled.withDiscrim()}'s Profile", member)
                .setThumbnail(profiled.effectiveAvatarUrl)
        if (member.isStaff()) embed.addField("Staff Member", "True", true)
        else embed.addField("Patron Level", data.donationLevel.readable, true)
        embed.addField("Money", "**${data.gold}** gold", true)
                .addField("Blackjack Stats", "Wins: **${blackjackData.wins}**\nTies: **${blackjackData.ties}**\nLosses: **${blackjackData.losses}**", true)
                .addField("Betting Stats", "Wins: **${bettingData.wins}**\nLosses: **${bettingData.ties}**\nNet Winnings: **${bettingData.netWinnings}** gold", true)
                .addField("Coinflip Stats", "Wins: **${coinflipData.wins}**\nLosses: **${coinflipData.losses}**", true)
                .addField("Married To", if (spouse == null) "Nobody :(" else spouse.withDiscrim(), true)
        channel.send(member, embed)
    }
}

class MarryCommand : Command(Category.RPG, "marry", "really fond of someone? make a discord marriage!") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val spouse = member.getMarriage()
        if (arguments.size == 0 || event.message.mentionedUsers.size == 0) {
            channel.send(member, if (spouse != null) "You're married to **${spouse.asMention}** ${BlackjackGame.Suit.HEART}" else "You're lonely :( Marry someone by " +
                    "typing **${guild.getPrefix()}marry @User**!")
            return
        }
        val proposed = event.message.mentionedUsers[0]
        when {
            spouse != null -> channel.send(member, "We live in the 21st century! No polygamic marriages!")
            proposed.getMarriage() != null -> channel.send(member, "This person's already married. Sorry :-(")
            else -> {
                channel.send(member, "${proposed.asMention}, ${member.asMention} is proposing to you! Do you want to accept? Type `yes` to get married or `no` to break " +
                        "their heart")
                waiter.waitForMessage(Settings(proposed.id, channel.id, guild.id), { response ->
                    if (response.content.startsWith("ye", true)) {
                        if (member.getMarriage() != null || proposed.getMarriage() != null) channel.send(member, "Unable to create marriage, one of you was just " +
                                "recently married")
                        else {
                            channel.send(member, "Congrats, ${member.asMention} & ${proposed.asMention}, you've been married ${BlackjackGame.Suit.HEART}")
                            Marriage(member.id(), proposed.id).insert("marriages")
                        }
                    } else channel.send(member, "Ouch, ${member.asMention} just got rejected")
                }, {
                    channel.send(member, "${proposed.asMention} didn't answer... Try again later ;)")
                }, time = 45)
            }
        }
    }
}

class DivorceCommand : Command(Category.RPG, "divorce", "marriage not working out? get a divorce!") {
    private val random = Random()
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val marriage = member.getMarriageModeled()
        if (marriage != null) {
            channel.send(member, "Are you sure you want to go through divorce? Half of your gold could go to your ex-spouse as part of your agreement." +
                    "Type `yes` if you understand and want to go through with the divorce, or `no` to cancel")
            waiter.waitForMessage(Settings(member.id(), channel.id, guild.id), { response ->
                if (response.content.startsWith("ye", true)) {
                    r.table("marriages").get(marriage.first.id).delete().runNoReply(conn)
                    if (random.nextBoolean()) {
                        val data = member.data()
                        val spouseData = marriage.second!!.getData()
                        spouseData.gold += data.gold / 2
                        data.gold /= 2
                        data.update()
                        spouseData.update()
                        channel.send(member, "Half of your net worth was given to ${marriage.second!!.asMention}")
                    }
                    channel.send(member, "You successfully divorced")
                } else channel.send(member, "Cancelled divorce, but I'd recommend some couple's therapy")
            }, { channel.send(member, "Cancelled divorce, but I'd recommend some couple's therapy") })
        } else channel.send(member, "You're not married!")
    }
}

class TopMoney : Command(Category.RPG, "top", "see who has the most money in the Ardent database", "topmoney") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        var page = if (arguments.size > 0) arguments[0].toIntOrNull() ?: 1 else 1
        if (page <= 0) page = 1
        val embed = embed("Global Money Leaderboards | Page $page", member)
                .setThumbnail("https://bitcoin.org/img/icons/opengraph.png")
        val builder = StringBuilder()
        val top = r.table("playerData").orderBy().optArg("index", r.desc("gold")).slice(((page - 1) * 10))
                .limit(10).run<Any>(conn).queryAsArrayList(PlayerData::class.java)
        top.forEachIndexed { index, playerData ->
            if (playerData != null && playerData.id.toUser() != null) builder.append("${Emoji.SMALL_BLUE_DIAMOND} **${playerData.id.toUser()!!.withDiscrim()}** " +
                    "*${playerData.gold} gold* (#${index + 1 + ((page - 1) * 10)})\n")
        }
        builder.append("\n*You can see different pages by typing ${guild.getPrefix()}top __page_number__*")
        channel.send(member, embed.setDescription(builder.toString()))
    }
}

class TopMoneyServer : Command(Category.RPG, "topserver", "see who has the most money in your server", "topmoneyserver") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val page = if (arguments.size > 0) arguments[0].toIntOrNull() ?: 1 else 1
        val embed = embed("${guild.name}'s Money Leaderboards | Page $page", member)
                .setThumbnail("https://bitcoin.org/img/icons/opengraph.png")
        val builder = StringBuilder()
        val members = hashMapOf<String, Double>()
        guild.playerDatas().forEach { members.put(it.id, it.gold) }
        val top = members.sort().toList()
        try {
            for (includedMember in ((page - 1) * 10)..(((page - 1) * 10) + 10)) {
                val user = (top[includedMember].first as String).toUser()
                if (user != null) {
                    builder.append("${Emoji.SMALL_BLUE_DIAMOND} **${user.withDiscrim()}** " +
                            "*${top[includedMember].second.toDouble()} gold* (#${includedMember + 1})\n")
                }
            }
        } catch (ignored: Exception) {
        }
        builder.append("\n*You can see different pages by typing ${guild.getPrefix()}top __page_number__*")
        channel.send(member, embed.setDescription(builder.toString()))

    }
}
