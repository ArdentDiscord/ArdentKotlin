package translation

import com.github.vbauer.yta.model.Language
import com.github.vbauer.yta.service.YTranslateApiImpl
import events.Category
import events.Command
import net.dv8tion.jda.core.events.message.MessageReceivedEvent
import utils.*
import java.awt.Color

class LanguageCommand : Command(Category.LANGUAGE, "language", "view or change Ardent's language on this server!", "lang") {
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size == 0 || !arguments[0].equals("set", true)) {
            event.channel.send(event.member.embed("Ardent | Server Language".tr(event), Color.BLUE)
                    .appendDescription("Your server language is **{0}** - You can change it by using {1}lang set **language** - Language list: {2}".tr(event).trReplace(event.guild, event.guild.getLanguage().readable, event.guild.getPrefix(),
                            Languages.values().map {
                                "\n ${Emoji.SMALL_ORANGE_DIAMOND} **${it.language.readable}**: *" + "%.2f".format(internals.languageStatuses[it.language]?.toFloat()) +
                                        "%* " + "done".tr(event)
                            }.stringify())))
        } else {
            if (event.member.hasOverride(event.textChannel)) {
                val lang = arguments.without(arguments[0]).concat().fromLangName()
                if (lang == null) event.channel.send("You specified an invalid language! Remember: You must add accents if your language requires that. Type **{0}lang** to see a language list".tr(event).trReplace(event.guild, event.guild.getPrefix()))
                else {
                    val guildData = event.guild.getData()
                    guildData.language = lang
                    guildData.update()
                    event.channel.send("Successfully updated your language to **{0}**!".tr(event, lang.readable))
                }
            }
        }
    }

    override fun registerSubcommands() {
    }
}

class Translate : Command(Category.LANGUAGE, "translate", "translate text to the provided language", "tr") {
    val api = YTranslateApiImpl("trnsl.1.1.20170227T013942Z.6878bfdf518abdf6.a6574733436345112da24eb08e7ee1ef2a0d6a97")
    override fun executeBase(arguments: MutableList<String>, event: MessageReceivedEvent) {
        if (arguments.size < 2) {
            val prefix = event.guild.getPrefix()
            event.channel.send("""Using the translation command is simple. The format for requesting one is as follows:
**{0}translate language_code_here your text goes here**

As follows are the language codes of some languages, but if you don't see the code for the language you want, go to {1} to view a full list.
**English**: en, **French**: fr, **Spanish**: es, **Russian**: ru

**Example**: *{0}translate en Bonjour tout le monde!* will return *Hello everyone!*""".tr(event, prefix, "<https://ardentbot.com/translation/languages>"))
        } else {
            try {
                val code = arguments[0]
                arguments.removeAt(0)
                event.channel.send(api.translationApi().translate(arguments.concat(), Language.of(code)).text()!!)
            } catch (e: Exception) {
                event.channel.send("You need to include a valid language code! Please visit {0} for a guide".tr(event, "<https://ardentbot.com/translation/languages>"))
            }
        }
    }

    override fun registerSubcommands() {
    }
}