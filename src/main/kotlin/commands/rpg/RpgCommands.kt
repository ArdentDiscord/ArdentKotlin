package commands.rpg

import events.Category
import events.Command
import events.ExtensibleCommand
import main.conn
import main.hostname
import main.r
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import translation.Language
import translation.fromLangName
import translation.toLanguage
import translation.tr
import utils.discord.*
import utils.functionality.*

class Balance : Command(Category.RPG, "bal", "see someone's balance (or yours)", "balance", "money", "gold") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val actionUser = if (event.message.mentionedUsers.size == 0) event.author else event.message.mentionedUsers[0]
        event.channel.send("**{0}** has *{1}* gold\nYou can use `/topserver` or `/top` to compare your balance to others in your server or globally"
                .tr(event, actionUser.toFancyString(), actionUser.getData().gold))
    }
}


class Daily : Command(Category.RPG, "daily", "get a daily stipend of gold") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        val data = event.author.getData()
        if (data.canCollect()) event.channel.send("You got **{0}** gold today!".tr(event, data.collect()))
        else event.channel.send("You already got your daily today!".tr(event) + " " + "You'll next be able to redeem it at **{0}**".tr(event, data.collectionTime()))
    }
}


class ProfileCommand : ExtensibleCommand(Category.RPG, "profile", "see and edit profiles - **type /profile @User to see someone's profile**") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0) showHelp(event)
        else {
            val profiled = if (event.message.mentionedUsers.size == 0) event.author else event.message.mentionedUsers[0]
            val data = profiled.getData()
            val embed = event.member!!.embed("${profiled.toFancyString()}'s Profile", event.textChannel)
            embed.setThumbnail("https://robohash.org/${profiled.id}")
            if (profiled.isStaff()) embed.addField("Special Permissions", "Staff Member", true)
            else embed.addField("Patron Level", getPatronLevel(profiled.id)?.readable ?: "None", true)
            embed.addField("About", data.selfDescription ?: "No description available", true)
            embed.addField("Speaks", data.languagesSpoken.map { it.fromLangName()?.readable }.stringify(), true)
            embed.addField("Gender", data.gender.display, true)
            embed.addField("Money", "**${data.gold}** gold", true)
            event.channel.send(embed)

            event.channel.send("View this profile on our site at {0}".tr(event, "$hostname/profile/${profiled.id}"))
        }
    }

    override fun registerSubcommands() {
        with("removelang", "removelang [language name]", "remove a language that you don't speak from your profile", { arguments, event ->
            if (arguments.size == 0 || arguments[0].fromLangName() == null) event.channel.send("Invalid language specified. Available: *{0}*"
                    .tr(event, Language.values().map { it.data.readable }.stringify()))
            else {
                val lang = arguments[0].fromLangName()!!
                val data = event.author.getData()
                val before = data.languagesSpoken.size
                data.languagesSpoken.removeIf { it == lang.readable }
                if (before == data.languagesSpoken.size) event.channel.send(Emoji.HEAVY_MULTIPLICATION_X.symbol + " " + "That language isn't on your profile!".tr(event))
                else {
                    event.channel.send(Emoji.HEAVY_CHECK_MARK.symbol + " " + "Successfully removed *{0}* from your spoken languages".tr(event, lang.readable))
                    data.update()
                }
            }
        })
        with("addlang", "addlang [language name]", "add a language that you speak to your profile", { arguments, event ->
            if (arguments.size == 0 || arguments[0].fromLangName() == null) event.channel.send("Invalid language specified. Available: *{0}*"
                    .tr(event, Language.values().map { it.data.readable }.stringify()))
            else {
                val lang = arguments[0].fromLangName()!!
                val data = event.author.getData()
                data.languagesSpoken.putIfNotThere(lang.readable)
                data.update()
                event.channel.send(Emoji.HEAVY_CHECK_MARK.symbol + " " + "Successfully added *{0}* to your spoken languages".tr(event, lang.readable))
            }
        })
        with("about", "about [about me]", "add an About Me to your profile", { arguments, event ->
            if (arguments.size == 0) showHelp(event)
            else {
                val data = event.author.getData()
                data.selfDescription = arguments.concat()
                data.update()
                event.channel.send(Emoji.HEAVY_CHECK_MARK.symbol + " " + "Successfully set your About Me as *{0}*".tr(event, arguments.concat()))
            }
        })
        with("gender", "gender [male/female/other]", "specify your gender for your profile", { arguments, event ->
            if (arguments.size == 0) showHelp(event)
            else {
                val data = event.author.getData()
                when (arguments[0]) {
                    "male" -> {
                        data.gender = UserData.Gender.MALE
                        event.channel.send(Emoji.HEAVY_CHECK_MARK.symbol + " " + "Successfully set your gender as {0}".tr(event, UserData.Gender.MALE.display))
                    }
                    "female" -> {
                        data.gender = UserData.Gender.FEMALE
                        event.channel.send(Emoji.HEAVY_CHECK_MARK.symbol + " " + "Successfully set your gender as {0}".tr(event, UserData.Gender.FEMALE.display))
                    }
                    "other" -> {
                        data.gender = UserData.Gender.UNDEFINED
                        event.channel.send(Emoji.HEAVY_CHECK_MARK.symbol + " " + "Successfully set your gender as {0}".tr(event, UserData.Gender.UNDEFINED.display))
                    }
                }
                data.update()
            }
        })
    }
}


class TopMoney : ExtensibleCommand(Category.RPG, "top", "see who has the most money in the Ardent database", "topmoney", "topserver") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        showHelp(event)
    }

    override fun registerSubcommands() {
        with("global", null, null, { arguments, event ->
            var page = if (arguments.size > 0) arguments[0].toIntOrNull() ?: 1 else 1
            if (page <= 0) page = 1
            val embed = event.member!!.embed("Global Money Leaderboards | Page {0}".tr(event, page), event.textChannel)
                    .setThumbnail("https://bitcoin.org/img/icons/opengraph.png")
            val builder = StringBuilder()
            val top = r.table("users").orderBy().optArg("index", r.desc("gold")).slice(((page - 1) * 10))
                    .limit(10).run<Any>(conn).queryAsArrayList(UserData::class.java)
            top.forEachIndexed { index, data ->
                if (data != null && getUserById(data.id) != null) builder.append("${Emoji.SMALL_BLUE_DIAMOND} **${getUserById(data.id)!!.toFancyString()}** " +
                        "*${data.gold} gold* (#${index + 1 + ((page - 1) * 10)})\n")
            }
            builder.append("\n").append("*You can see different pages by typing /top __page number__*".tr(event))
            event.channel.send(embed.setDescription(builder.toString()))
        })
        with("server", null, null, { arguments, event ->
            val page = if (arguments.size > 0) arguments[0].toIntOrNull() ?: 1 else 1
            val embed = event.member!!.embed("{0}'s Money Leaderboards | Page {1}".tr(event, event.guild.name, page), event.textChannel)
                    .setThumbnail("https://bitcoin.org/img/icons/opengraph.png")
            val builder = StringBuilder()
            val members = hashMapOf<String, Double>()
            event.guild.getUsersData().forEach { members.put(it.id, it.gold) }
            val top = members.sort().toList()
            try {
                for (includedMember in ((page - 1) * 10)..(((page - 1) * 10) + 10)) {
                    val user = getUserById(top[includedMember].first)
                    if (user != null) {
                        builder.append("${Emoji.SMALL_BLUE_DIAMOND} **${user.toFancyString()}** " +
                                "*${top[includedMember].second} gold* (#${includedMember + 1})\n")
                    }
                }
            } catch (ignored: Exception) {
            }
            builder.append("\n").append("*You can see different pages by typing /topserver __page number__*".tr(event))
            event.channel.send(embed.setDescription(builder.toString()))
        })
    }
}