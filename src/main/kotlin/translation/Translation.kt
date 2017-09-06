package translation

private val translationData = ArdentTranslationData()

class ArdentTranslationData(val phrases: HashMap<String, ArdentPhraseTranslation> = hashMapOf()) {
    init {
        update()
    }

    private fun update() {

    }
}

data class ArdentPhraseTranslation(private val translations: HashMap<String /* language code */, String /* translated phrase */>) {
    fun translate(language: ArdentLanguage): String {
        return translations.getOrElse(language.code, { translations["en"] ?: "This translation doesn't currently exist" })
    }
}

data class ArdentLanguage(val code: String, val readable: String, val maturity: LanguageMaturity = LanguageMaturity.INFANCY) {
    fun translate(english: String): String {
        return translationData.phrases[english]?.translate(this) ?: "This translation doesn't exist yet in the database"
    }

    override fun toString(): String {
        return code
    }
}

enum class LanguageMaturity(val readable: String) {
    INFANCY("Infancy"),
    DEVELOPMENT("In Development"),
    NEEDS_REFINING("Needs Refining"),
    MATURE("Mature")
}