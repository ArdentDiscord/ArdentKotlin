package translation

import main.conn
import main.r
import org.languagetool.JLanguageTool
import org.languagetool.language.*
import utils.queryAsArrayList
import java.net.URLEncoder
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.TimeUnit

val translationData = ArdentTranslationData()

enum class LanguageTools(val languageTool: JLanguageTool) {
    FRENCH(JLanguageTool(org.languagetool.Languages.getLanguageForShortCode("fr"))),
    DANISH(JLanguageTool(Danish())),
    DUTCH(JLanguageTool(Dutch())),
    GERMAN(JLanguageTool(GermanyGerman())),
    RUSSIAN(JLanguageTool(Russian())),
    ITALIAN(JLanguageTool(Italian())),
    SPANISH(JLanguageTool(Spanish()))
}

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
                val localPhrase = phrases[phrase.translate(Languages.ENGLISH)]
                if (localPhrase == null) {
                    if (phrase.encoded == null) phrase.encoded = URLEncoder.encode(phrase.english)
                    else if (phrase.translate(Languages.ENGLISH) != null) phrases.put(phrase.translate(Languages.ENGLISH)!!, phrase)
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

    fun getByEnglish(string: String?): ArdentPhraseTranslation? {
        phrases.forEach { if (it.value.english.equals(string, true)) return it.value }
        return null
    }

    fun get(english: String, command: String): ArdentPhraseTranslation? {
        phrases.forEach { if (it.value.english == english && command == it.value.command) return it.value }
        return null
    }
}

data class ArdentPhraseTranslation(var english: String, val command: String, var encoded: String? = URLEncoder.encode(english), val translations: HashMap<String /* language code */, String /* translated phrase */> = hashMapOf()) {
    fun instantiate(englishPhrase: String): ArdentPhraseTranslation {
        translations.put("en", englishPhrase)
        return this
    }

    fun translate(languages: Languages): String? {
        return translate(languages.language)
    }

    fun translate(language: String): String? {
        return translate(language.toLanguage() ?: Languages.ENGLISH.language)
    }

    fun translate(language: ArdentLanguage): String? {
        return translations[language.code]
    }
}

data class ArdentLanguage(val code: String, val readable: String, val maturity: LanguageMaturity = LanguageMaturity.INFANCY) {
    fun translate(english: String?): String? {
        return translationData.getByEnglish(english)?.translate(this)
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

    fun getChecker(): JLanguageTool? {
        try {
            return when (this) {
                Languages.FRENCH.language -> LanguageTools.FRENCH.languageTool
                Languages.GERMAN.language -> LanguageTools.GERMAN.languageTool
                Languages.DANISH.language -> LanguageTools.DANISH.languageTool
                Languages.DUTCH.language -> LanguageTools.DUTCH.languageTool
                Languages.RUSSIAN.language -> LanguageTools.RUSSIAN.languageTool
                Languages.ITALIAN.language -> LanguageTools.ITALIAN.languageTool
                else -> null
            }
        }
        catch(e: Exception) {
            e.printStackTrace()
        }
        return null
    }

    override fun toString(): String {
        return code
    }
}

fun String.toLanguage(): ArdentLanguage? {
    return when (this) {
        "en" -> Languages.ENGLISH.language
        "fr" -> Languages.FRENCH.language
        "da" -> Languages.DANISH.language
        "de" -> Languages.GERMAN.language
        "hi" -> Languages.HINDI.language
        "ru" -> Languages.RUSSIAN.language
        "it" -> Languages.ITALIAN.language
        "cr" -> Languages.CROATIAN.language
        "nl" -> Languages.DUTCH.language
        "ej" -> Languages.EMOJI.language
        "es" -> Languages.SPANISH.language
        else -> null
    }
}

fun String.fromLangName(): ArdentLanguage? {
    Languages.values().forEach { if (it.language.readable.equals(this, true)) return it.language }
    return null
}

enum class Languages(val language: ArdentLanguage) {
    ENGLISH(ArdentLanguage("en", "English", LanguageMaturity.DEVELOPMENT)),
    FRENCH(ArdentLanguage("fr", "Français")),
    DANISH(ArdentLanguage("da", "Dansk")),
    DUTCH(ArdentLanguage("nl", "Nederlands")),
    GERMAN(ArdentLanguage("de", "Deutsch")),
    HINDI(ArdentLanguage("hi", "Hindi")),
    RUSSIAN(ArdentLanguage("ru", "Russian")),
    ITALIAN(ArdentLanguage("it", "Italian")),
    CROATIAN(ArdentLanguage("cr", "Croatian")),
    EMOJI(ArdentLanguage("ej", "Emoji")),
    SPANISH(ArdentLanguage("es", "Spanish"));
}

enum class LanguageMaturity(val readable: String) {
    INFANCY("Infancy"),
    DEVELOPMENT("In Development"),
    NEEDS_REFINING("Needs Refining"),
    MATURE("Mature")
}
