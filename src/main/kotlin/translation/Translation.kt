package translation

import main.beta
import main.conn
import main.r
import main.test
import net.dv8tion.jda.core.entities.Guild
import net.dv8tion.jda.core.entities.TextChannel
import utils.discord.getData
import utils.discord.send
import utils.functionality.insert
import utils.functionality.logChannel
import utils.functionality.queryAsArrayList
import utils.functionality.trReplace
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

val translationData = ArdentTranslationData()

class ArdentTranslationData(val phrases: ConcurrentHashMap<String, ArdentPhraseTranslation> = ConcurrentHashMap(), executor: ScheduledExecutorService = Executors.newScheduledThreadPool(5)) {
    init {
        executor.scheduleAtFixedRate({
            update()
        }, 0, 1, TimeUnit.MINUTES)
    }

    private fun update() {
        val databaseTranslations = r.table("phrases").run<Any>(conn).queryAsArrayList(ArdentPhraseTranslation::class.java)
        // Primary key is "en" signifying the english translation
        databaseTranslations.forEach { phrase ->
            if (phrase != null) {
                val localPhrase = phrases[phrase.translate(Language.ENGLISH)]
                if (localPhrase == null) {
                    if (phrase.encoded == null) phrase.encoded = URLEncoder.encode(phrase.english)
                    if (phrase.translate(Language.ENGLISH) != null) phrases.put(phrase.translate(Language.ENGLISH)!!, phrase)
                } else {
                    phrase.translations.forEach { languageCode, translation ->
                        if (localPhrase.translations[languageCode] != translation) {
                            if (localPhrase.encoded == null) localPhrase.encoded = URLEncoder.encode(localPhrase.english)
                            if (!localPhrase.translations.containsKey(languageCode)) localPhrase.translations.put(languageCode, translation)
                            else localPhrase.translations.replace(languageCode, translation)
                        }
                    }
                }
            }
        }
    }

    fun getByEncoded(string: String?): ArdentPhraseTranslation? {
        phrases.forEach { if (it.value.encoded == string) return it.value }
        return null
    }

    fun get(english: String): ArdentPhraseTranslation? {
        phrases.forEach { if (it.value.english == english) return it.value }
        return null
    }
}

data class ArdentPhraseTranslation(var english: String, var command: Boolean = false, var subcommand: Boolean = false, var translationTip: String? = null, var encoded: String? = URLEncoder.encode(english), val translations: HashMap<String /* data code */, String /* translated phrase */> = hashMapOf()) {
    init {
        translations.put("en", english)
    }

    fun translate(language: Language): String? {
        return translate(language.data)
    }

    fun translate(languageData: LanguageData): String {
        return translations[languageData.code] ?: english
    }
}

data class LanguageData(val code: String, val readable: String, val maturity: LanguageMaturity = LanguageMaturity.INFANCY) {
    fun translate(english: String?): String? {
        translationData.phrases.forEach { if (it.key == english || it.value.english == english) return it.value.translate(this)}
        return null
    }

    fun getTranslations(): List<ArdentPhraseTranslation> {
        return translationData.phrases.map { it.value }
    }

    fun getNullTranslations(): MutableList<ArdentPhraseTranslation> {
        val phrases = mutableListOf<ArdentPhraseTranslation>()
        translationData.phrases.forEach { _, u -> if (!u.translations.containsKey(code) || u.translations[code]?.isEmpty() != false) phrases.add(u) }
        return phrases
    }

    fun getNonNullTranslations(): MutableList<ArdentPhraseTranslation> {
        val phrases = mutableListOf<ArdentPhraseTranslation>()
        translationData.phrases.forEach { _, u -> if (u.translations.containsKey(code) && u.translations[code]?.isEmpty() == false) phrases.add(u) }
        return phrases
    }

    override fun toString(): String {
        return code
    }
}

fun String.toLanguage(): LanguageData? {
    return when (this) {
        "en" -> Language.ENGLISH
        "fr" -> Language.FRENCH
        "da" -> Language.DANISH
        "de" -> Language.GERMAN
        "hi" -> Language.HINDI
        "ru" -> Language.RUSSIAN
        "it" -> Language.ITALIAN
        "cr" -> Language.CROATIAN
        "nl" -> Language.DUTCH
        "ej" -> Language.EMOJI
        "es" -> Language.SPANISH
        "po" -> Language.POLISH
        "zh-TR" -> Language.MANDARIN_TRADITIONAL
        "zh-SI" -> Language.MANDARIN_SIMPLIFIED
        "pt-BR" -> Language.PORTUGESE_BRAZIL
        else -> null
    }?.data
}



fun String.tr(languageData: LanguageData, vararg new: Any, command: Boolean = false, subcommand: Boolean = false): String {
    return languageData.translate(this)?.trReplace(languageData, *new) ?: translationDoesntExist(languageData, *new, command, subcommand)
}

fun String.translationDoesntExist(languageData: LanguageData, vararg new: Any, command: Boolean = false, subcommand: Boolean = false): String {
    if (!test && !beta) {
        val phrase = ArdentPhraseTranslation(this, command, subcommand)
        translationData.phrases.put(this, phrase)
        if (r.table("phrases").filter(r.hashMap("english", this)).count().run<Long>(conn) == 0.toLong()) {
            phrase.insert("phrases")
            logChannel!!.send("```Translation for the following doesn't exist and was automatically inserted into the database: $this```")
        }
    }
    return this.trReplace(languageData, *new)
}

fun String.tr(textChannel: TextChannel, vararg new: Any): String {
    return tr(textChannel.guild, *new)
}

fun String.tr(guild: Guild, vararg new: Any): String {
    return tr(guild.getData().languageSettings.getLanguage(), *new)
}


fun String.fromLangName(): LanguageData? {
    Language.values().forEach { if (it.data.readable.equals(this, true)) return it.data }
    return null
}

enum class Language(val data: LanguageData) {
    ENGLISH(LanguageData("en", "English", LanguageMaturity.DEVELOPMENT)),
    FRENCH(LanguageData("fr", "Français")),
    GERMAN(LanguageData("de", "Deutsch")),
    RUSSIAN(LanguageData("ru", "Russian")),
    DANISH(LanguageData("da", "Dansk")),
    DUTCH(LanguageData("nl", "Nederlands")),
    HINDI(LanguageData("hi", "Hindi")),
    ITALIAN(LanguageData("it", "Italian")),
    CROATIAN(LanguageData("cr", "Croatian")),
    EMOJI(LanguageData("ej", "Emoji")),
    POLISH(LanguageData("po", "Polish")),
    SPANISH(LanguageData("es", "Spanish")),
    MANDARIN_TRADITIONAL(LanguageData("zh-TR", "Traditional Mandarin")),
    MANDARIN_SIMPLIFIED(LanguageData("zh-SI", "Simplified Mandarin")),
    PORTUGESE_BRAZIL(LanguageData("pt-BR", "Português"))
}

enum class LanguageMaturity(val readable: String) {
    INFANCY("Infancy"),
    DEVELOPMENT("In Development"),
    NEEDS_REFINING("Needs Refining"),
    MATURE("Mature")
}
