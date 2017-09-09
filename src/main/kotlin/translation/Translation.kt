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
                    phrases.put(phrase.translate(Languages.ENGLISH), phrase)
                }
                else {
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

    fun getByEnglish(string: String?): ArdentPhraseTranslation? {
        phrases.forEach { if (it.value.english == string) return it.value}
        return null
    }

    fun get(english: String, command: String): ArdentPhraseTranslation? {
        phrases.forEach { if (it.value.english == english && command == it.value.command) return it.value}
        return null
    }
}

data class ArdentPhraseTranslation(var english: String, val command: String, var encoded: String? = URLEncoder.encode(english), val translations: HashMap<String /* language code */, String /* translated phrase */> = hashMapOf()) {
    fun instantiate(englishPhrase: String): ArdentPhraseTranslation {
        translations.put("en", englishPhrase)
        return this
    }
    fun translate(languages: Languages): String {
        return translate(languages.language)
    }

    fun translate(language: String): String {
        return translate(language.toLanguage() ?: Languages.ENGLISH.language)
    }

    fun translate(language: ArdentLanguage): String {
        return translations.getOrElse(language.code, { translations["en"] ?: "This translation doesn't currently exist" })
    }
}

data class ArdentLanguage(val code: String, val readable: String, val maturity: LanguageMaturity = LanguageMaturity.INFANCY) {
    fun translate(english: String?): String {
        return translationData.getByEnglish(english)?.translate(this) ?: "This translation doesn't exist yet in the database"
    }

    fun getNullTranslations(): MutableList<ArdentPhraseTranslation> {
        val phrases = mutableListOf<ArdentPhraseTranslation>()
        translationData.phrases.forEach { _, u ->  if (!u.translations.containsKey(code) || u.translations[code]?.isEmpty() != false) phrases.add(u) }
        return phrases
    }

    fun getNonNullTranslations(): MutableList<ArdentPhraseTranslation> {
        val phrases = mutableListOf<ArdentPhraseTranslation>()
        translationData.phrases.forEach { _, u ->  if (u.translations.containsKey(code) && u.translations[code]?.isEmpty() == false) phrases.add(u) }
        return phrases
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
        else -> null
    }
}

enum class Languages(val language: ArdentLanguage) {
    ENGLISH(ArdentLanguage("en", "English", LanguageMaturity.DEVELOPMENT)),
    FRENCH(ArdentLanguage("fr", "Français")),
    DANISH(ArdentLanguage("da", "Dansk")),
    GERMAN(ArdentLanguage("de", "Deutsch"))

    ;
}

enum class LanguageMaturity(val readable: String) {
    INFANCY("Infancy"),
    DEVELOPMENT("In Development"),
    NEEDS_REFINING("Needs Refining"),
    MATURE("Mature")
}
