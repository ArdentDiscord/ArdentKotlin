package commands.rpg

import events.Category
import events.Command
import main.conn
import main.r
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.Member
import net.dv8tion.jda.core.entities.TextChannel
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*

class TopMoney : Command(Category.RPG, "top", "see who has the most money in the Ardent database", "topmoney") {
    override fun execute(member: Member, channel: TextChannel, guild: Guild, arguments: MutableList<String>, event: MessageReceivedEvent) {
        val page = if (arguments.size > 0) arguments[0].toIntOrNull() ?: 1 else 1
        val embed = embed("Global Money Leaderboards | Page $page", member)
                .setThumbnail("https://bitcoin.org/img/icons/opengraph.png")
        val builder = StringBuilder()
        val top = r.table("playerData").orderBy().optArg("index", r.desc("gold")).slice(((page - 1) * 10))
                .limit(10).run<Any>(conn).queryAsArrayList(PlayerData::class.java)
        top.forEachIndexed { index, playerData ->
            if (playerData != null && playerData.id.toUser() != null) builder.append("${Emoji.SMALL_BLUE_DIAMOND} **${playerData.id.toUser()!!.withDiscrim()}** " +
                    "*${playerData.gold} gold* (#${index + 1 + (page * 10)})\n")
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
