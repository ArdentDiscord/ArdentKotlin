package translation

import com.github.vbauer.yta.service.YTranslateApiImpl
import events.Category
import events.Command
import main.hostname
import net.dv8tion.jda.api.events.message.MessageReceivedEvent
import utils.discord.embed
import utils.discord.getData
import utils.discord.internals
import utils.discord.send
import utils.functionality.Emoji
import utils.functionality.concat
import utils.functionality.stringify
import utils.functionality.without

class LanguageCommand : Command(Category.LANGUAGE, "data", "view or change Ardent's data on this server!", "lang") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0 || !arguments[0].equals("set", true)) {
            event.channel.send(event.member!!.embed("Ardent | Server Language".tr(event.guild), event.textChannel)
                    .appendDescription("Your server data is **{0}** - You can change it by using _/lang set **data**_ - Language list: {2}"
                            .tr(event, event.guild, event.guild.getData().languageSettings.getLanguage().readable,
                                    Language.values().map {
                                        "\n ${Emoji.SMALL_ORANGE_DIAMOND} **${it.data.readable}**: *" +
                                                "%.2f".format(internals.languageStatuses[it.data]?.toFloat()) +
                                                "%* " + "done".tr(event)
                                    }.stringify())))
        } else {
            if (event.member!!.hasPermission(event.textChannel)) {
                val lang = arguments.without(arguments[0]).concat().fromLangName()
                if (lang == null) {
                    event.channel.send("You specified an invalid language! **NOTE**: You must add accents if your data requires them!".tr(event))
                } else {
                    val guildData = event.guild.getData()
                    guildData.languageSettings.language = lang.code
                    guildData.update()
                    event.channel.send("Successfully updated your data to **{0}**!".tr(event, lang.readable))
                }
            }
        }
    }
}

class Translate : Command(Category.LANGUAGE, "translate", "translate text to the provided data", "tr") {
    val api = YTranslateApiImpl("trnsl.1.1.20170227T013942Z.6878bfdf518abdf6.a6574733436345112da24eb08e7ee1ef2a0d6a97")
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size < 2) {
            event.channel.send("""Using the translation command is simple. The format for requesting one is as follows:
**/translate language_code_here your text goes here**

As follows are the data codes of some languages, but if you don't see the code for the data you want, go to {0} to view a full list.
**English**: en, **French**: fr, **Spanish**: es, **Russian**: ru

**Example**: *{0}translate en Bonjour tout le monde!* will return *Hello everyone!*""".tr(event, "<$hostname/translation/languages>"))
        } else {
            try {
                val code = arguments[0]
                arguments.removeAt(0)
                event.channel.send(api.translationApi().translate(arguments.concat(), com.github.vbauer.yta.model.Language.of(code)).text()!!)
            } catch (e: Exception) {
                event.channel.send("You need to include a valid data code! Please visit {0} for a guide".tr(event, "<$hostname/translation/languages>"))
            }
        }
    }
}