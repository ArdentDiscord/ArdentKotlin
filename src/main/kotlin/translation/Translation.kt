package translation

import main.conn
import main.r
import utils.queryAsArrayList
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
        }, 0, 2, TimeUnit.MINUTES)
    }

    private fun update() {
        val databaseTranslations = r.table("phrases").run<Any>(conn).queryAsArrayList(ArdentPhraseTranslation::class.java)
        // Primary key is "en" signifying the english translation
        databaseTranslations.forEach { phrase ->
            if (phrase != null) {
                val localPhrase = phrases[phrase.translate(Languages.ENGLISH)]
                if (localPhrase == null) {
                    if (phrase.encoded == null) phrase.encoded = URLEncoder.encode(phrase.english)
                    if (phrase.translate(Languages.ENGLISH) != null) phrases.put(phrase.translate(Languages.ENGLISH)!!, phrase)
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

    fun translate(language: ArdentLanguage): String {
        return translations[language.code] ?: english
    }
}

data class ArdentLanguage(val code: String, val readable: String, val maturity: LanguageMaturity = LanguageMaturity.INFANCY) {
    fun translate(english: String?): String? {
        translationData.phrases.forEach { if (it.key.equals(english, true) || it.value.english == english) return it.value.translate(this)}
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

fun String.toLanguage(): ArdentLanguage? {
    return when (this) {
        "en" -> Languages.ENGLISH
        "fr" -> Languages.FRENCH
        "da" -> Languages.DANISH
        "de" -> Languages.GERMAN
        "hi" -> Languages.HINDI
        "ru" -> Languages.RUSSIAN
        "it" -> Languages.ITALIAN
        "cr" -> Languages.CROATIAN
        "nl" -> Languages.DUTCH
        "ej" -> Languages.EMOJI
        "es" -> Languages.SPANISH
        "po" -> Languages.POLISH
        "zh-TR" -> Languages.MANDARIN_TRADITIONAL
        "zh-SI" -> Languages.MANDARIN_SIMPLIFIED
        "pt-BR" -> Languages.PORTUGESE_BRAZIL
        else -> null
    }?.language
}

fun String.fromLangName(): ArdentLanguage? {
    Languages.values().forEach { if (it.language.readable.equals(this, true)) return it.language }
    return null
}

enum class Languages(val language: ArdentLanguage) {
    ENGLISH(ArdentLanguage("en", "English", LanguageMaturity.DEVELOPMENT)),
    FRENCH(ArdentLanguage("fr", "Français")),
    GERMAN(ArdentLanguage("de", "Deutsch")),
    RUSSIAN(ArdentLanguage("ru", "Russian")),
    DANISH(ArdentLanguage("da", "Dansk")),
    DUTCH(ArdentLanguage("nl", "Nederlands")),
    HINDI(ArdentLanguage("hi", "Hindi")),
    ITALIAN(ArdentLanguage("it", "Italian")),
    CROATIAN(ArdentLanguage("cr", "Croatian")),
    EMOJI(ArdentLanguage("ej", "Emoji")),
    POLISH(ArdentLanguage("po", "Polish")),
    SPANISH(ArdentLanguage("es", "Spanish")),
    MANDARIN_TRADITIONAL(ArdentLanguage("zh-TR", "Traditional Mandarin")),
    MANDARIN_SIMPLIFIED(ArdentLanguage("zh-SI", "Simplified Mandarin")),
    PORTUGESE_BRAZIL(ArdentLanguage("pt-BR", "Português"))
}

enum class LanguageMaturity(val readable: String) {
    INFANCY("Infancy"),
    DEVELOPMENT("In Development"),
    NEEDS_REFINING("Needs Refining"),
    MATURE("Mature")
}
