package commands.statistics

import events.Category
import events.Command
import main.conn
import main.factory
import main.r
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import translation.ArdentLanguage
import translation.Languages
import utils.*

class ServerLanguagesDistribution : Command(Category.STATISTICS, "serverlangs", "see how many Ardent servers are using which bot locale", "glangs", "slangs") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        var langs = hashMapOf<ArdentLanguage, Int /* Usages */>()
        val guilds = r.table("guilds").run<Any>(conn).queryAsArrayList(GuildData::class.java)
        guilds.forEach { data ->
            if (data != null) {
                if (data.language == null) {
                    data.language = Languages.ENGLISH.language
                    data.update()
                }
                if (langs.containsKey(data.language!!)) langs.incrementValue(data.language!!)
                else langs.put(data.language!!, 1)
            }
        }
        langs = langs.sort(true) as HashMap<ArdentLanguage, Int>
        val embed = event.member.embed("Ardent | Server Languages".tr(event))
        langs.forEachIndexed { index, l, usages ->
            embed.appendDescription((if (index % 2 == 0) Emoji.SMALL_BLUE_DIAMOND else Emoji.SMALL_ORANGE_DIAMOND).symbol +
                    " **${l.readable}**: *${usages} servers* (${"%.2f".format(usages * 100 / guilds.size.toFloat())}%)\n")
        }
        event.channel.send(embed)
    }
}

class CommandDistribution : Command(Category.STATISTICS, "distribution", "see how commands have been distributed on Ardent", "commanddistribution", "cdist") {
    override fun execute(arguments: MutableList<String>, event: MessageReceivedEvent) {
        event.channel.sendMessage("Generating a command distribution overview could take up to **2** minutes... I'll delete this message once it's done".tr(event)).queue {
            val isOverall = !arguments.isEmpty()
            val data =
                    if (arguments.size == 0) factory.commandsById.sort(true)
                    else {
                        val temp = hashMapOf<String, Int>()
                        r.table("commands").run<Any>(conn).queryAsArrayList(LoggedCommand::class.java).forEach { if (it != null) if (temp.containsKey(it.commandId)) temp.incrementValue(it.commandId) else temp.put(it.commandId, 1) }
                        temp.sort(true)
                    } as HashMap<String, Int>
            val embed = event.member.embed((if (isOverall) "Ardent | Lifetime Command Distribution" else "Ardent | Current Session Command Distribution").tr(event))
            embed.setThumbnail("https://www.wired.com/wp-content/uploads/blogs/magazine/wp-content/images/18-05/st_thompson_statistics_f.jpg")
            var total = 0
            data.forEach { total += it.value }
            embed.appendDescription("Data generated from **{0}** individual entries".tr(event, total.format()))
            val iterator = data.iterator().withIndex()
            while (iterator.hasNext()) {
                val current = iterator.next()
                if (embed.descriptionBuilder.length >= 1900) {
                    embed.appendDescription("\n\n" + "Type *{0}distribution all* to see the total command distribution".tr(event, event.guild.getPrefix()))
                    event.channel.send(embed)
                    embed.setDescription("")
                } else {
                    embed.appendDescription("\n   " + (if (current.index % 2 == 0) Emoji.SMALL_ORANGE_DIAMOND else Emoji.SMALL_BLUE_DIAMOND).symbol
                            + " " + "{0}: **{1}**% ({2} commands) - [**{3}**]".tr(event, name, "%.2f".format(current.value.value * 100 / total.toFloat()), current.value.value.format(), "#" + (current.index + 1).toString()))
                }
            }
            if (embed.descriptionBuilder.isNotEmpty()) {
                embed.appendDescription("\n\n" + "Type *{0}distribution all* to see the total command distribution".tr(event, event.guild.getPrefix()))
                event.channel.send(embed)
            }
            it.delete().queue()
        }
    }
}